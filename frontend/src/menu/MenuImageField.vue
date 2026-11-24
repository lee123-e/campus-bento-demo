<script setup>
import { onUnmounted, ref, useId, watch } from 'vue'

defineProps({
  label: { type: String, required: true },
  storedPreview: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['error'])
const file = defineModel({ default: null })
const fieldId = useId()
const fileField = ref(null)
const localPreview = ref('')

function revokePreview() {
  if (localPreview.value) {
    URL.revokeObjectURL(localPreview.value)
    localPreview.value = ''
  }
}

watch(file, selected => {
  revokePreview()
  if (selected) {
    localPreview.value = URL.createObjectURL(selected)
  } else if (fileField.value) {
    fileField.value.value = ''
  }
})

function selectImage(event) {
  const selected = event.target.files[0]
  if (!selected) return
  if (!['image/jpeg', 'image/png'].includes(selected.type) || selected.size > 5 * 1024 * 1024) {
    event.target.value = ''
    file.value = null
    emit('error', '请选择不超过 5 MB 的 JPG 或 PNG 图片。')
    return
  }
  file.value = selected
  emit('error', '')
}

onUnmounted(revokePreview)
</script>

<template>
  <div class="field photo-field">
    <label :for="fieldId">{{ label }}</label>
    <div class="photo-preview">
      <img v-if="localPreview || storedPreview" :src="localPreview || storedPreview" :alt="label + '预览'" />
      <span v-else>选择一张照片</span>
    </div>
    <input :id="fieldId" ref="fileField" type="file" accept="image/jpeg,image/png"
           :disabled="disabled" :aria-describedby="fieldId + '-help'" @change="selectImage" />
    <small :id="fieldId + '-help'">JPG 或 PNG，最大 5 MB。</small>
  </div>
</template>

<style scoped>
.photo-field { align-content: start; }
.photo-preview { display: grid; place-items: center; min-height: 8rem; aspect-ratio: 16 / 10; overflow: hidden; border-radius: .6rem; background: #f4eee6; color: var(--muted); }
.photo-preview img { width: 100%; height: 100%; object-fit: cover; }
.photo-field small { color: var(--muted); }
.field input[type="file"] { min-height: auto; padding: .45rem; font-size: .875rem; }

@media (max-width: 600px) {
  .photo-preview { max-height: 13rem; }
}
</style>
