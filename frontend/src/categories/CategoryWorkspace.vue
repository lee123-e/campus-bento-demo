<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { adminFetch } from '../adminApi.js'

const emit = defineEmits(['session-expired'])
const types = [{ value: 1, label: '菜品分类' }, { value: 2, label: '套餐分类' }]
const filters = [{ value: '', label: '全部' }, ...types]
const pageSize = 10
const filter = ref('')
const page = ref(1)
const total = ref(0)
const rows = ref([])
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const loading = ref(true)
const listError = ref('')
const feedback = ref('')
let listRequest

const editor = ref(null)
const deletionDialog = ref(null)
const form = reactive({ id: null, type: 1, name: '', sort: 0 })
const deletion = ref(null)
const saving = ref(false)
const dialogError = ref('')

function typeLabel(value) {
  return types.find(type => type.value === value).label
}

async function loadCategories() {
  listRequest?.abort()
  const request = new AbortController()
  listRequest = request
  loading.value = true
  listError.value = ''

  const query = new URLSearchParams({ page: page.value, size: pageSize })
  if (filter.value) {
    query.set('type', filter.value)
  }

  try {
    const response = await adminFetch('/categories?' + query, { signal: request.signal })
    if (response.status === 401) {
      emit('session-expired')
      return
    }
    if (!response.ok) {
      throw new Error('分类加载失败，请重试。')
    }
    const result = await response.json()
    if (request.signal.aborted) {
      return
    }
    total.value = result.total
    if (page.value > pageCount.value) {
      page.value = pageCount.value
      await loadCategories()
      return
    }
    rows.value = result.items
  } catch {
    if (!request.signal.aborted) {
      listError.value = '分类加载失败，请重试。'
    }
  } finally {
    if (listRequest === request) {
      loading.value = false
    }
  }
}

function selectFilter(value) {
  filter.value = value
  page.value = 1
  loadCategories()
}

function changePage(value) {
  page.value = value
  loadCategories()
}

function showEditor(row) {
  Object.assign(form, row ?? { id: null, type: filter.value || 1, name: '', sort: 0 })
  dialogError.value = ''
  editor.value.showModal()
}

function showDeletion(row) {
  deletion.value = row
  dialogError.value = ''
  deletionDialog.value.showModal()
}

function preventDismissWhileSaving(event) {
  if (saving.value) {
    event.preventDefault()
  }
}

async function runMutation(path, options, dialog, successMessage) {
  saving.value = true
  dialogError.value = ''
  feedback.value = ''

  try {
    const response = await adminFetch(path, options)
    if (response.status === 401) {
      dialog.close()
      emit('session-expired')
      return false
    }
    if (!response.ok) {
      const problem = await response.json().catch(() => null)
      dialogError.value = [404, 409].includes(response.status) && problem?.detail
        ? problem.detail
        : '操作未完成，请检查输入后重试。'
      return false
    }
    dialog.close()
    feedback.value = successMessage
    return true
  } catch {
    dialogError.value = '无法连接店铺服务，请稍后重试。'
    return false
  } finally {
    saving.value = false
  }
}

async function saveCategory() {
  const id = form.id
  const body = { name: form.name, sort: form.sort }
  if (!id) {
    body.type = form.type
  }
  const saved = await runMutation(
    id ? '/categories/' + id : '/categories',
    { method: id ? 'PUT' : 'POST', body },
    editor.value,
    id ? '分类已更新。' : '分类已新增。',
  )
  if (!saved) {
    return
  }
  if (!id) {
    if (filter.value) {
      filter.value = form.type
    }
    page.value = 1
  }
  await loadCategories()
}

async function deleteCategory() {
  const removed = await runMutation(
    '/categories/' + deletion.value.id,
    { method: 'DELETE' },
    deletionDialog.value,
    '分类已删除。',
  )
  if (removed) {
    await loadCategories()
  }
}

onMounted(loadCategories)
onUnmounted(() => listRequest?.abort())
</script>

<template>
  <section class="categories" aria-labelledby="categories-heading">
    <div class="category-heading">
      <div>
        <h1 id="categories-heading">分类管理</h1>
        <p class="description">维护菜品和套餐的分类。排序值越小，显示越靠前。</p>
      </div>
      <button type="button" class="primary-button add-category" @click="showEditor()">新增分类</button>
    </div>

    <div class="category-list">
      <div class="category-toolbar">
        <div class="category-filters" role="group" aria-label="按分类类型筛选">
          <button v-for="option in filters" :key="option.label" type="button"
                  :aria-pressed="filter === option.value" @click="selectFilter(option.value)">
            {{ option.label }}
          </button>
        </div>
        <p v-if="!listError && !loading" class="category-count">共 {{ total }} 个分类</p>
      </div>

      <p v-if="feedback" class="category-feedback" role="status">{{ feedback }}</p>
      <p v-if="loading" class="list-message" role="status">正在加载分类…</p>
      <div v-else-if="listError" class="list-message">
        <p role="alert">{{ listError }}</p>
        <button class="quiet-button retry-button" type="button" @click="loadCategories">重试</button>
      </div>
      <div v-else-if="!rows.length" class="list-message">
        <p class="empty-title">{{ filter ? '暂无' + typeLabel(filter) : '还没有分类' }}</p>
        <p>添加分类后，就能在这里维护名称和排序。</p>
      </div>
      <template v-else>
        <div class="category-table-scroll">
          <table class="category-table">
            <thead>
              <tr>
                <th scope="col">分类名称</th>
                <th scope="col">类型</th>
                <th scope="col" class="sort-cell">排序</th>
                <th scope="col" class="updated-cell">更新时间</th>
                <th scope="col" class="actions-cell">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <th scope="row">{{ row.name }}</th>
                <td><span class="category-type" :class="{ bundle: row.type === 2 }">{{ typeLabel(row.type) }}</span></td>
                <td class="sort-cell">{{ row.sort }}</td>
                <td class="updated-cell">
                  <time :datetime="row.updatedAt">{{ row.updatedAt.replace('T', ' ').slice(0, 16) }}</time>
                </td>
                <td class="actions-cell">
                  <button type="button" class="row-action" :aria-label="'编辑' + row.name"
                          @click="showEditor(row)">编辑</button>
                  <button type="button" class="row-action delete-action" :aria-label="'删除' + row.name"
                          @click="showDeletion(row)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <nav class="pagination" aria-label="分类分页">
          <span>第 {{ page }} / {{ pageCount }} 页</span>
          <button class="quiet-button" type="button" :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
          <button class="quiet-button" type="button" :disabled="page === pageCount" @click="changePage(page + 1)">下一页</button>
        </nav>
      </template>
    </div>

    <dialog ref="editor" class="category-dialog" aria-labelledby="category-editor-title" @cancel="preventDismissWhileSaving">
      <h2 id="category-editor-title">{{ form.id ? '编辑分类' : '新增分类' }}</h2>
      <form @submit.prevent="saveCategory">
        <div class="field">
          <label for="category-type">分类类型</label>
          <select id="category-type" v-model.number="form.type" :disabled="!!form.id || saving">
            <option v-for="option in types" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </div>
        <div class="field">
          <label for="category-name">分类名称</label>
          <input id="category-name" v-model.trim="form.name" maxlength="40" required :disabled="saving" />
        </div>
        <div class="field">
          <label for="category-sort">排序值</label>
          <input id="category-sort" v-model.number="form.sort" type="number" min="0" max="2147483647"
                 step="1" required :disabled="saving" />
        </div>
        <p v-if="dialogError" class="notice" role="alert">{{ dialogError }}</p>
        <div class="dialog-actions">
          <button type="button" class="quiet-button" :disabled="saving" @click="editor.close()">取消</button>
          <button type="submit" class="primary-button" :disabled="saving || !form.name">
            {{ saving ? '保存中…' : '保存分类' }}
          </button>
        </div>
      </form>
    </dialog>

    <dialog ref="deletionDialog" class="category-dialog" aria-labelledby="category-delete-title"
            @cancel="preventDismissWhileSaving">
      <h2 id="category-delete-title">删除分类</h2>
      <p class="description">确认删除「{{ deletion?.name }}」？</p>
      <p v-if="dialogError" class="notice" role="alert">{{ dialogError }}</p>
      <div class="dialog-actions">
        <button type="button" class="quiet-button" autofocus :disabled="saving" @click="deletionDialog.close()">取消</button>
        <button type="button" class="primary-button" :disabled="saving" @click="deleteCategory">
          {{ saving ? '删除中…' : '确认删除' }}
        </button>
      </div>
    </dialog>
  </section>
</template>

<style scoped>
.category-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 1rem 2rem;
  margin-bottom: 2rem;
}

.add-category { flex-shrink: 0; margin: 0; }

.category-list {
  border: 1px solid #e5ded4;
  border-radius: .85rem;
  background: #fffdf9;
}

.category-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .75rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e5ded4;
}

.category-filters { display: flex; flex-wrap: wrap; gap: .25rem; }

.category-filters button {
  min-height: 2.5rem;
  padding: .35rem .7rem;
  border: 0;
  border-radius: .45rem;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font-size: .9rem;
}

.category-filters button:hover { background: #f4eee6; }

.category-filters button[aria-pressed="true"] {
  background: #f5e7df;
  color: #862b1d;
  font-weight: 700;
}

.category-count { color: var(--muted); font-size: .875rem; white-space: nowrap; }

.category-feedback {
  margin: .85rem 1.25rem 0;
  color: #375041;
  font-size: .9375rem;
}

.list-message { padding: 3.5rem 1.25rem; color: var(--muted); text-align: center; }
.empty-title { margin-bottom: .4rem; color: #25211e; font-weight: 650; }
.retry-button { margin-top: .7rem; }

.category-table-scroll { overflow-x: auto; }
.category-table { width: 100%; border-collapse: collapse; text-align: left; }

.category-table th, .category-table td {
  padding: .9rem 1.25rem;
  border-bottom: 1px solid #ece6dc;
}

.category-table thead {
  background: #f7f3ed;
  color: #60554b;
  font-size: .875rem;
  white-space: nowrap;
}

.category-table tbody th { overflow-wrap: anywhere; font-weight: 650; }
.category-table tbody tr:hover { background: #fcf8f1; }

.category-type {
  display: inline-block;
  padding: .2rem .55rem;
  border-radius: .35rem;
  background: #eaf0e8;
  color: #355341;
  font-size: .8125rem;
  white-space: nowrap;
}

.category-type.bundle { background: #f5e9dd; color: #795032; }

.sort-cell, .updated-cell { font-variant-numeric: tabular-nums; }
.updated-cell { color: var(--muted); font-size: .875rem; white-space: nowrap; }
.actions-cell { text-align: right; white-space: nowrap; }

.row-action {
  min-height: 2.25rem;
  padding: .25rem .4rem;
  border: 0;
  border-radius: .35rem;
  background: transparent;
  color: #624330;
  font-size: .875rem;
  cursor: pointer;
}

.row-action:hover { background: #f2ece4; }
.delete-action { color: #a13625; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: end;
  gap: .5rem;
  padding: .9rem 1.25rem;
  color: var(--muted);
  font-size: .875rem;
}

.pagination span { margin-inline-end: .5rem; }
.pagination button:disabled { cursor: default; }

.category-dialog {
  width: min(100% - 2rem, 27rem);
  max-height: calc(100svh - 2rem);
  padding: 1.6rem;
  border: 1px solid #e5ded4;
  border-radius: .875rem;
  background: #fffdf9;
  color: #25211e;
  box-shadow: 0 1rem 3rem #33201926;
}

.category-dialog::backdrop { background: #33201965; }
.category-dialog h2 { margin: 0; font-size: 1.4rem; }
.category-dialog form { margin-top: 1.25rem; }
.category-dialog .notice { margin-top: .25rem; }

.category-dialog select {
  width: 100%;
  min-height: 3.15rem;
  padding: .65rem .85rem;
  border: 1px solid #9a8c7f;
  border-radius: .55rem;
  background: #fffdf9;
  color: inherit;
  font: inherit;
}

.category-dialog select:disabled { background: #f4efe8; color: var(--muted); }

.dialog-actions { display: flex; justify-content: end; gap: .65rem; margin-top: .7rem; }
.dialog-actions .primary-button { margin: 0; }

@media (max-width: 680px) {
  .category-heading { flex-wrap: wrap; align-items: start; }
  .category-toolbar { flex-wrap: wrap; padding: .75rem; }
  .category-table th, .category-table td { padding: .65rem .55rem; }
  .updated-cell { display: none; }
  .category-type { padding-inline: .3rem; }
  .row-action { padding-inline: .25rem; }
  .pagination { padding-inline: .65rem; }
}
</style>
