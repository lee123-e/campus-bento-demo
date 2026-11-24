<script setup>
import { reactive, ref } from 'vue'
import MenuImageField from '../menu/MenuImageField.vue'
import { menuImageUrl, requestMenuData } from '../menu/menuApi.js'
import SetmealDishPicker from './SetmealDishPicker.vue'

const props = defineProps({ categories: { type: Array, required: true } })
const emit = defineEmits(['saved', 'session-expired'])
const dialog = ref(null)
const editorOpen = ref(false)
const image = ref(null)
const storedPreview = ref('')
const error = ref('')
const saving = ref(false)
const form = reactive({ id: null, name: '', categoryId: '', price: '', description: '', items: [] })

function open(detail, defaultCategoryId) {
  const setmeal = detail?.setmeal
  Object.assign(form, {
    id: setmeal?.id ?? null,
    name: setmeal?.name ?? '',
    categoryId: setmeal?.categoryId ?? defaultCategoryId ?? props.categories[0]?.id ?? '',
    price: setmeal?.price ?? '',
    description: detail?.description ?? '',
    items: detail?.items.map(item => ({ ...item })) ?? [],
  })
  image.value = null
  storedPreview.value = setmeal ? menuImageUrl('/setmeals', setmeal) : ''
  error.value = ''
  editorOpen.value = true
  dialog.value.showModal()
}

function closeEditor() {
  editorOpen.value = false
  image.value = null
}

function expireSession() {
  dialog.value?.close()
  emit('session-expired')
}

async function save() {
  if (saving.value) return
  error.value = ''
  if (!form.items.length) {
    error.value = '请至少选择一道菜品。'
    return
  }
  if (!form.id && !image.value) {
    error.value = '请先选择套餐图片。'
    return
  }

  const body = new FormData()
  body.append('setmeal', new Blob([JSON.stringify({
    name: form.name,
    categoryId: form.categoryId,
    price: form.price,
    description: form.description,
    items: form.items.map(({ dishId, quantity }) => ({ dishId, quantity })),
  })], { type: 'application/json' }))
  if (image.value) body.append('image', image.value)

  saving.value = true
  try {
    await requestMenuData(form.id ? '/setmeals/' + form.id : '/setmeals', { method: form.id ? 'PUT' : 'POST', body })
    dialog.value.close()
    emit('saved', { created: !form.id })
  } catch (failure) {
    if (failure.status === 401) expireSession()
    else error.value = failure.message || '套餐保存失败，请稍后重试。'
  } finally {
    saving.value = false
  }
}

function preventCancel(event) {
  if (saving.value) event.preventDefault()
}

defineExpose({ open })
</script>

<template>
  <dialog ref="dialog" class="setmeal-editor" aria-labelledby="setmeal-editor-heading"
          @cancel="preventCancel" @close="closeEditor">
    <h2 id="setmeal-editor-heading">{{ form.id ? '编辑套餐' : '新增套餐' }}</h2>
    <form class="editor-form" :aria-busy="saving" @submit.prevent="save">
      <div class="editor-grid">
        <div class="editor-inputs">
          <div class="field">
            <label for="setmeal-name">套餐名称</label>
            <input id="setmeal-name" v-model.trim="form.name" maxlength="60" required autofocus :disabled="saving" />
          </div>
          <div class="field">
            <label for="setmeal-category">套餐分类</label>
            <select id="setmeal-category" v-model.number="form.categoryId" required :disabled="saving">
              <option value="" disabled>请选择套餐分类</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
            </select>
          </div>
          <div class="field">
            <label for="setmeal-price">套餐价（元）</label>
            <input id="setmeal-price" v-model.number="form.price" type="number" min="0.01"
                   max="99999999.99" step="0.01" required :disabled="saving" />
          </div>
        </div>
        <MenuImageField v-model="image" label="套餐图片" :stored-preview="storedPreview"
                        :disabled="saving" @error="error = $event" />
      </div>
      <div class="field">
        <label for="setmeal-description">简介</label>
        <textarea id="setmeal-description" v-model.trim="form.description" maxlength="500" rows="2" :disabled="saving"></textarea>
      </div>
      <SetmealDishPicker v-if="editorOpen" v-model="form.items" :disabled="saving" @session-expired="expireSession" />
      <p v-if="error" class="notice" role="alert">{{ error }}</p>
      <div class="editor-actions">
        <button class="quiet-button" type="button" :disabled="saving" @click="dialog.close()">取消</button>
        <button class="primary-button" type="submit" :disabled="saving || !form.name">{{ saving ? '保存中…' : '保存套餐' }}</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.setmeal-editor { width: min(100% - 2rem, 47rem); max-height: calc(100svh - 2rem); padding: clamp(1rem, 4vw, 2rem); border: 1px solid #e5ded4; border-radius: .9rem; background: #fffdf9; color: #25211e; }
.setmeal-editor::backdrop { background: #33201965; }
.setmeal-editor h2 { margin: 0; }
.editor-form { gap: 1.1rem; margin-top: 1.15rem; }
.editor-grid { display: grid; grid-template-columns: 1fr .92fr; gap: 1.25rem; }
.editor-inputs { display: grid; align-content: start; gap: .85rem; }
.editor-inputs select, .editor-form textarea { width: 100%; min-height: 3.15rem; padding: .65rem .85rem; border: 1px solid #9a8c7f; border-radius: .55rem; background: #fffdf9; color: inherit; font: inherit; }
.editor-form textarea { resize: vertical; }
.editor-actions { display: flex; justify-content: end; gap: .65rem; }
.editor-actions .primary-button { margin: 0; }

@media (max-width: 600px) {
  .editor-grid { grid-template-columns: 1fr; }
}
</style>
