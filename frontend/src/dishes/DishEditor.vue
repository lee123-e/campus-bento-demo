<script setup>
import { reactive, ref } from 'vue'
import MenuImageField from '../menu/MenuImageField.vue'
import { menuImageUrl, requestMenuData } from '../menu/menuApi.js'

const props = defineProps({
  categories: { type: Array, required: true },
})
const emit = defineEmits(['saved', 'session-expired'])
const dialog = ref(null)
const selectedImage = ref(null)
const storedPreview = ref('')
const editorError = ref('')
const saving = ref(false)
const form = reactive({ id: null, name: '', categoryId: '', price: '', description: '', flavors: [] })
const customFlavor = '__custom__'
const flavorPresets = [
  { name: '辣度', options: ['不辣', '微辣', '中辣', '重辣'] },
  { name: '甜度', options: ['无糖', '微糖', '半糖', '全糖'] },
  { name: '温度', options: ['热', '常温', '少冰', '去冰'] },
  { name: '忌口', options: ['不加葱', '不加香菜', '不加蒜'] },
  { name: '饭量', options: ['少饭', '标准', '加饭'] },
]
let nextFlavorKey = 1

function open(detail, defaultCategoryId) {
  const dish = detail?.dish
  Object.assign(form, {
    id: dish?.id ?? null,
    name: dish?.name ?? '',
    categoryId: dish?.categoryId ?? defaultCategoryId ?? props.categories[0]?.id ?? '',
    price: dish?.price ?? '',
    description: detail?.description ?? '',
    flavors: detail?.flavors.map(toEditableFlavor) ?? [],
  })
  selectedImage.value = null
  editorError.value = ''
  storedPreview.value = dish ? menuImageUrl('/dishes', dish) : ''
  dialog.value.showModal()
}

function toEditableFlavor(group) {
  return {
    key: nextFlavorKey++,
    preset: flavorPresets.some(preset => preset.name === group.name) ? group.name : customFlavor,
    name: group.name,
    options: [...group.options],
    showCustomOption: false,
    draftOption: '',
  }
}

function addFlavor() {
  form.flavors.push({ ...toEditableFlavor({ name: '', options: [] }), preset: '' })
}

function changeFlavorPreset(group, preset) {
  group.preset = preset
  group.name = preset === customFlavor ? '' : preset
  group.options = []
  group.showCustomOption = false
  group.draftOption = ''
}

function availableOptions(group) {
  const preset = flavorPresets.find(item => item.name === group.preset)
  return preset?.options.filter(option => !group.options.includes(option)) ?? []
}

function addFlavorOption(group, option) {
  if (group.options.length >= 15) return

  if (group.options.some(existing => existing.toLowerCase() === option.toLowerCase())) {
    editorError.value = '这个选项已经添加。'
    return
  }
  group.options.push(option)
  group.draftOption = ''
  editorError.value = ''
}

function selectFlavorOption(group, event) {
  const select = event.target
  if (select.value === customFlavor) {
    group.showCustomOption = true
  } else if (select.value) {
    addFlavorOption(group, select.value)
  }
  select.value = ''
}

async function save() {
  if (saving.value) {
    return
  }
  editorError.value = ''
  const flavors = form.flavors.map(({ name, options }) => ({ name, options }))
  if (flavors.some(group => !group.options.length)) {
    editorError.value = '每个口味分组至少选择或添加一个选项。'
    return
  }
  if (!form.id && !selectedImage.value) {
    editorError.value = '请先选择菜品图片。'
    return
  }

  const body = new FormData()
  body.append('dish', new Blob([JSON.stringify({
    name: form.name,
    categoryId: form.categoryId,
    price: form.price,
    description: form.description,
    flavors,
  })], { type: 'application/json' }))
  if (selectedImage.value) {
    body.append('image', selectedImage.value)
  }

  saving.value = true
  try {
    const path = form.id ? '/dishes/' + form.id : '/dishes'
    await requestMenuData(path, { method: form.id ? 'PUT' : 'POST', body })
    dialog.value.close()
    emit('saved', { created: !form.id })
  } catch (error) {
    if (error.status === 401) {
      dialog.value.close()
      emit('session-expired')
    } else {
      editorError.value = error.message || '保存失败，请稍后重试。'
    }
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
  <dialog ref="dialog" class="dish-editor" aria-labelledby="dish-editor-title"
          @cancel="preventCancel" @close="selectedImage = null">
    <h2 id="dish-editor-title">{{ form.id ? '编辑菜品' : '新增菜品' }}</h2>
    <form class="editor-form" @submit.prevent="save">
      <div class="editor-grid">
        <div class="editor-inputs">
          <div class="field">
            <label for="dish-name">菜品名称</label>
            <input id="dish-name" v-model.trim="form.name" maxlength="60" required autofocus :disabled="saving" />
          </div>
          <div class="field">
            <label for="dish-category">菜品分类</label>
            <select id="dish-category" v-model.number="form.categoryId" required :disabled="saving">
              <option value="" disabled>请选择菜品分类</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
            </select>
          </div>
          <div class="field">
            <label for="dish-price">售价（元）</label>
            <input id="dish-price" v-model.number="form.price" type="number" min="0.01"
                   max="99999999.99" step="0.01" required :disabled="saving" />
          </div>
        </div>
        <MenuImageField v-model="selectedImage" label="菜品图片" :stored-preview="storedPreview"
                        :disabled="saving" @error="editorError = $event" />
      </div>
      <div class="field">
        <label for="dish-description">简介</label>
        <textarea id="dish-description" v-model.trim="form.description" maxlength="500"
                  rows="2" :disabled="saving"></textarea>
      </div>
      <section class="flavors" aria-labelledby="dish-flavors-heading">
        <div class="flavor-heading">
          <h3 id="dish-flavors-heading">口味选项</h3>
          <button class="quiet-button" type="button" :disabled="saving || form.flavors.length >= 12" @click="addFlavor">添加口味</button>
        </div>
        <fieldset v-for="(group, index) in form.flavors" :key="group.key" class="flavor-row" :disabled="saving">
          <legend>口味 {{ index + 1 }}</legend>
          <div class="field">
            <label :for="'flavor-group-' + group.key">口味分组</label>
            <select :id="'flavor-group-' + group.key" :value="group.preset" required
                    @change="changeFlavorPreset(group, $event.target.value)">
              <option value="" disabled>选择分组</option>
              <option v-for="preset in flavorPresets" :key="preset.name" :value="preset.name">{{ preset.name }}</option>
              <option :value="customFlavor">自定义分组</option>
            </select>
            <template v-if="group.preset === customFlavor">
              <label :for="'flavor-name-' + group.key">自定义分组名称</label>
              <input :id="'flavor-name-' + group.key" v-model.trim="group.name" maxlength="40" required />
            </template>
          </div>
          <div class="field">
            <template v-if="group.preset !== customFlavor">
              <label :for="'flavor-option-' + group.key">添加选项</label>
              <select :id="'flavor-option-' + group.key" :disabled="!group.preset || group.options.length >= 15"
                      @change="selectFlavorOption(group, $event)">
                <option value="">选择一个选项</option>
                <option v-for="option in availableOptions(group)" :key="option">{{ option }}</option>
                <option :value="customFlavor">自定义选项…</option>
              </select>
            </template>
            <template v-if="group.preset === customFlavor || group.showCustomOption">
              <label :for="'custom-option-' + group.key">自定义选项</label>
              <div class="custom-option">
                <input :id="'custom-option-' + group.key" v-model.trim="group.draftOption" maxlength="40"
                       :disabled="group.options.length >= 15"
                       @keydown.enter.prevent="group.draftOption && addFlavorOption(group, group.draftOption)" />
                <button class="quiet-button" type="button" :disabled="!group.draftOption || group.options.length >= 15"
                        @click="addFlavorOption(group, group.draftOption)">添加</button>
              </div>
            </template>
            <div v-if="group.options.length" class="flavor-options" role="group" aria-label="已选口味">
              <button v-for="option in group.options" :key="option" class="flavor-option" type="button"
                      :aria-label="'移除选项 ' + option" @click="group.options = group.options.filter(value => value !== option)">
                {{ option }} <span aria-hidden="true">×</span>
              </button>
            </div>
          </div>
          <button class="quiet-button remove-flavor" type="button" @click="form.flavors.splice(index, 1)">移除分组</button>
        </fieldset>
      </section>
      <p v-if="editorError" class="notice" role="alert">{{ editorError }}</p>
      <div class="editor-actions">
        <button class="quiet-button" type="button" :disabled="saving" @click="dialog.close()">取消</button>
        <button class="primary-button" type="submit" :disabled="saving || !form.name">{{ saving ? '保存中…' : '保存菜品' }}</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.dish-editor {
  width: min(100% - 2rem, 44rem);
  max-height: calc(100svh - 2rem);
  padding: clamp(1rem, 4vw, 2rem);
  border: 1px solid #e5ded4;
  border-radius: .9rem;
  background: #fffdf9;
  color: #25211e;
}
.dish-editor::backdrop { background: #33201965; }
.dish-editor h2, .dish-editor h3 { margin: 0; }
.editor-form { gap: 1.2rem; margin-top: 1.25rem; }
.editor-grid { display: grid; grid-template-columns: 1fr .92fr; gap: 1.25rem; }
.editor-inputs { display: grid; align-content: start; gap: .85rem; }
.field select, .field textarea { width: 100%; min-height: 3.15rem; padding: .65rem .85rem; border: 1px solid #9a8c7f; border-radius: .55rem; background: #fffdf9; color: inherit; font: inherit; }
.field textarea { resize: vertical; }
.flavor-heading, .editor-actions { display: flex; align-items: center; justify-content: space-between; gap: .75rem; }
.flavor-heading h3 { font-size: 1rem; }
.flavor-row { display: grid; grid-template-columns: minmax(0, .85fr) minmax(0, 1.4fr) auto; align-items: start; gap: .65rem; margin: .75rem 0 0; padding: .65rem; border: 1px solid #e5ded4; border-radius: .65rem; }
.flavor-row legend { color: var(--muted); }
.remove-flavor { margin-top: 1.83rem; min-height: 3.15rem; padding-inline: .65rem; white-space: nowrap; }
.custom-option { display: flex; gap: .4rem; }
.custom-option .quiet-button { flex-shrink: 0; }
.flavor-options { display: flex; flex-wrap: wrap; gap: .35rem; }
.flavor-option { padding: .3rem .6rem; border: 1px solid #e5ded4; border-radius: .45rem; background: #f4eee6; color: #624330; cursor: pointer; overflow-wrap: anywhere; }
.flavor-option:hover { background: #f5e7df; }
.editor-actions { justify-content: end; }
.editor-actions .primary-button { margin: 0; }

@media (max-width: 600px) {
  .editor-grid, .flavor-row { grid-template-columns: 1fr; }
  .remove-flavor { justify-self: start; margin-top: 0; min-height: 2.75rem; }
}
</style>
