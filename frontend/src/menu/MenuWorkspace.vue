<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { menuImageUrl, requestMenuData } from './menuApi.js'

const props = defineProps({
  entityLabel: { type: String, required: true },
  unit: { type: String, required: true },
  summary: { type: String, required: true },
  resource: { type: String, required: true },
  categoryType: { type: Number, required: true },
  itemEditor: { type: Object, required: true },
})
const emit = defineEmits(['session-expired'])
const pageSize = 10
const money = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' })
const editor = ref(null)
const deleteDialog = ref(null)
const categories = ref([])
const categoriesError = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const search = ref('')
const filterCategory = ref('')
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const selected = ref([])
const allSelected = computed(() => rows.value.length > 0 && selected.value.length === rows.value.length)
const partlySelected = computed(() => selected.value.length > 0 && !allSelected.value)
const loading = ref(true)
const working = ref(false)
const listError = ref('')
const actionError = ref('')
const notice = ref('')
const deletion = ref([])
let listRequest
let nameFilter = ''

async function loadCategories() {
  categoriesError.value = ''
  try {
    categories.value = await requestMenuData('/categories/options?type=' + props.categoryType)
  } catch (error) {
    if (error.status === 401) emit('session-expired')
    else categoriesError.value = props.entityLabel + '分类加载失败。'
  }
}

async function loadItems() {
  listRequest?.abort()
  const request = new AbortController()
  listRequest = request
  loading.value = true
  listError.value = ''
  selected.value = []
  const query = new URLSearchParams({ page: page.value, size: pageSize })
  if (nameFilter) query.set('name', nameFilter)
  if (filterCategory.value) query.set('categoryId', filterCategory.value)
  try {
    const result = await requestMenuData(props.resource + '?' + query, { signal: request.signal })
    if (request.signal.aborted) return
    total.value = result.total
    if (page.value > pageCount.value) {
      page.value = pageCount.value
      await loadItems()
      return
    }
    rows.value = result.items
  } catch (error) {
    if (!request.signal.aborted) {
      if (error.status === 401) emit('session-expired')
      else listError.value = props.entityLabel + '列表加载失败。'
    }
  } finally {
    if (listRequest === request) loading.value = false
  }
}

function applyFilter() {
  nameFilter = search.value
  page.value = 1
  loadItems()
}

function changePage(value) {
  page.value = value
  loadItems()
}

function selectPage(event) {
  selected.value = event.target.checked ? rows.value.map(row => row.id) : []
}

function createItem() {
  editor.value.open(null, filterCategory.value || undefined)
}

async function editItem(row) {
  working.value = true
  actionError.value = ''
  try {
    const detail = await requestMenuData(props.resource + '/' + row.id)
    editor.value?.open(detail)
  } catch (error) {
    showActionError(error)
  } finally {
    working.value = false
  }
}

function showActionError(error) {
  if (error.status === 401) emit('session-expired')
  else actionError.value = error.message || '操作失败，请重试。'
}

async function runAction(path, options, message) {
  if (working.value) return
  working.value = true
  actionError.value = ''
  notice.value = ''
  try {
    await requestMenuData(path, options)
    notice.value = message
    await loadItems()
  } catch (error) {
    showActionError(error)
  } finally {
    working.value = false
  }
}

function setSaleStatus(ids, onSale) {
  return runAction(props.resource + '/sale-status', { method: 'PUT', body: { ids, onSale } },
    '所选' + props.entityLabel + (onSale ? '已启售。' : '已停售。'))
}

function confirmDelete(items) {
  deletion.value = items
  deleteDialog.value.showModal()
}

function removeItems() {
  deleteDialog.value.close()
  return runAction(props.resource, { method: 'DELETE', body: { ids: deletion.value.map(item => item.id) } },
    '所选' + props.entityLabel + '已删除。')
}

function onSaved({ created }) {
  actionError.value = ''
  notice.value = props.entityLabel + (created ? '已保存为停售。' : '已更新。')
  if (created) {
    search.value = ''
    nameFilter = ''
    filterCategory.value = ''
    page.value = 1
  }
  loadItems()
}

onMounted(() => {
  loadCategories()
  loadItems()
})
onUnmounted(() => listRequest?.abort())
</script>

<template>
  <section class="menu-workspace" aria-labelledby="menu-heading">
    <div class="menu-heading">
      <div>
        <h1 id="menu-heading">{{ entityLabel }}管理</h1>
        <p class="description">{{ summary }}</p>
      </div>
      <button type="button" class="primary-button" :disabled="!categories.length || working" @click="createItem">新增{{ entityLabel }}</button>
    </div>

    <p v-if="categoriesError" class="notice" role="alert">
      {{ categoriesError }}
      <button type="button" class="text-button" @click="loadCategories">重试</button>
    </p>
    <p v-else-if="!categories.length && !loading" class="notice">
      新增{{ entityLabel }}前，请先在<a href="#categories">分类管理</a>添加{{ entityLabel }}分类。
    </p>

    <div class="menu-panel">
      <form class="menu-filter" @submit.prevent="applyFilter">
        <label class="search-field">
          <span>{{ entityLabel }}名称</span>
          <input type="search" v-model.trim="search" maxlength="60" :placeholder="'输入' + entityLabel + '名称'" />
        </label>
        <label class="category-field">
          <span>分类</span>
          <select v-model.number="filterCategory" @change="applyFilter">
            <option value="">全部分类</option>
            <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <button class="quiet-button search-button" type="submit">搜索</button>
      </form>

      <div class="menu-actions">
        <div class="selection-summary">
          <span class="result-count" role="status">共 {{ total }} {{ unit }}<span v-if="selected.length">，已选 {{ selected.length }} {{ unit }}</span></span>
          <span v-if="!selected.length && rows.length && !loading && !listError" class="selection-hint">勾选左侧{{ entityLabel }}后可批量操作。</span>
        </div>
        <div class="batch-buttons" role="group" :aria-label="entityLabel + '批量操作'">
          <button class="quiet-button" type="button" :disabled="!selected.length || working"
                  @click="setSaleStatus(selected, true)">批量启售</button>
          <button class="quiet-button" type="button" :disabled="!selected.length || working"
                  @click="setSaleStatus(selected, false)">批量停售</button>
          <button class="quiet-button delete-button" type="button" :disabled="!selected.length || working"
                  @click="confirmDelete(rows.filter(row => selected.includes(row.id)))">批量删除</button>
        </div>
      </div>
      <p v-if="actionError" class="notice list-feedback" role="alert">{{ actionError }}</p>
      <p v-if="notice" class="success list-feedback" role="status">{{ notice }}</p>

      <p v-if="loading" class="list-state" role="status">正在加载{{ entityLabel }}…</p>
      <div v-else-if="listError" class="list-state">
        <p role="alert">{{ listError }}</p>
        <button class="quiet-button" type="button" @click="loadItems">重试</button>
      </div>
      <p v-else-if="!rows.length" class="list-state">{{ (nameFilter || filterCategory ? '没有找到符合条件的' : '还没有') + entityLabel + '。' }}</p>
      <template v-else>
        <div class="menu-table-scroll">
          <table class="menu-table">
            <thead>
              <tr>
                <th scope="col" class="check-cell"><input type="checkbox" :aria-label="'选择本页所有' + entityLabel" :checked="allSelected" :indeterminate="partlySelected" @change="selectPage" /></th>
                <th scope="col">{{ entityLabel }}</th>
                <th scope="col">分类</th>
                <th scope="col" class="price-cell">售价</th>
                <th scope="col">状态</th>
                <th scope="col" class="updated-cell">更新时间</th>
                <th scope="col" class="row-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in rows" :key="item.id">
                <td class="check-cell"><input v-model="selected" type="checkbox" :value="item.id" :aria-label="'选择' + item.name" /></td>
                <th scope="row">
                  <span class="menu-identity">
                    <img :src="menuImageUrl(resource, item)" alt="" loading="lazy" width="48" height="42" />
                    <span>{{ item.name }}</span>
                  </span>
                </th>
                <td>{{ item.categoryName ?? '—' }}</td>
                <td class="price-cell">{{ money.format(item.price) }}</td>
                <td><span class="sale-status" :class="{ active: item.onSale }">{{ item.onSale ? '启售' : '停售' }}</span></td>
                <td class="updated-cell"><time :datetime="item.updatedAt">{{ item.updatedAt.replace('T', ' ').slice(0, 16) }}</time></td>
                <td class="row-actions">
                  <button type="button" class="text-button" :disabled="working || !categories.length"
                          :aria-label="'编辑' + item.name" @click="editItem(item)">编辑</button>
                  <button type="button" class="text-button" :disabled="working" :aria-label="(item.onSale ? '停售' : '启售') + item.name"
                          @click="setSaleStatus([item.id], !item.onSale)">{{ item.onSale ? '停售' : '启售' }}</button>
                  <button type="button" class="text-button delete-button" :disabled="working" :aria-label="'删除' + item.name"
                          @click="confirmDelete([item])">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <nav class="pagination" :aria-label="entityLabel + '分页'">
          <span>第 {{ page }} / {{ pageCount }} 页</span>
          <button type="button" class="quiet-button" :disabled="page === 1 || working" @click="changePage(page - 1)">上一页</button>
          <button type="button" class="quiet-button" :disabled="page === pageCount || working" @click="changePage(page + 1)">下一页</button>
        </nav>
      </template>
    </div>

    <component :is="itemEditor" ref="editor" :categories="categories" @saved="onSaved" @session-expired="emit('session-expired')" />
    <dialog ref="deleteDialog" class="delete-dialog" aria-labelledby="menu-delete-heading">
      <h2 id="menu-delete-heading">删除{{ entityLabel }}</h2>
      <p class="description">{{ deletion.length === 1 ? '确认删除「' + deletion[0].name + '」？' : '确认删除所选的 ' + deletion.length + ' ' + unit + entityLabel + '？' }}</p>
      <p class="description">{{ entityLabel }}需在停售后才能删除。</p>
      <div class="delete-actions">
        <button type="button" class="quiet-button" autofocus @click="deleteDialog.close()">取消</button>
        <button type="button" class="primary-button" @click="removeItems">确认删除</button>
      </div>
    </dialog>
  </section>
</template>

<style scoped>
.menu-heading { display: flex; align-items: end; justify-content: space-between; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.6rem; }
.menu-heading .primary-button, .delete-actions .primary-button { margin: 0; }
.menu-workspace > .notice { margin-bottom: .9rem; }
.menu-workspace a { color: var(--accent); }
.menu-panel { border: 1px solid #e5ded4; border-radius: .85rem; background: #fffdf9; }
.menu-filter { display: flex; align-items: end; flex-wrap: wrap; gap: .65rem; margin: 0; padding: 1.1rem 1.25rem; border-bottom: 1px solid #e5ded4; }
.menu-filter label { display: grid; gap: .35rem; font-size: .875rem; color: var(--muted); }
.search-field { width: min(100%, 17rem); }
.category-field { width: min(100%, 12rem); }
.menu-filter input, .menu-filter select { min-height: 2.75rem; width: 100%; padding: .5rem .65rem; border: 1px solid #9a8c7f; border-radius: .55rem; background: #fffdf9; color: #25211e; font: inherit; }
.menu-actions { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: .65rem; padding: .85rem 1.25rem; }
.selection-summary { display: flex; flex-wrap: wrap; align-items: baseline; gap: .25rem .75rem; }
.result-count, .selection-hint { color: var(--muted); font-size: .875rem; }
.batch-buttons { display: flex; flex-wrap: wrap; gap: .4rem; }
.batch-buttons button { min-height: 2.25rem; padding: .35rem .6rem; font-size: .875rem; }
.batch-buttons button:disabled { cursor: default; }
.delete-button { color: var(--accent); }
.list-feedback { margin: .4rem 1.25rem .8rem; }
.success { color: #365640; font-size: .9375rem; }
.list-state { display: grid; justify-items: center; gap: .65rem; padding: 3rem 1rem; color: var(--muted); }
.menu-table-scroll { overflow-x: auto; }
.menu-table { width: 100%; min-width: 37rem; border-collapse: collapse; text-align: left; }
.menu-table th, .menu-table td { padding: .7rem .8rem; border-bottom: 1px solid #ece6dc; }
.menu-table thead { background: #f7f3ed; color: #60554b; font-size: .875rem; white-space: nowrap; }
.menu-table tbody tr:hover { background: #fcf8f1; }
.menu-table .check-cell { padding-inline: 1rem .25rem; width: 2rem; }
.check-cell input { width: 1rem; height: 1rem; accent-color: var(--accent); }
.menu-identity { display: flex; align-items: center; gap: .65rem; font-weight: 650; overflow-wrap: anywhere; }
.menu-identity img { flex: 0 0 3rem; object-fit: cover; border-radius: .38rem; background: #f4eee6; }
.price-cell, .updated-cell { font-variant-numeric: tabular-nums; white-space: nowrap; }
.updated-cell { color: var(--muted); font-size: .875rem; }
.sale-status { display: inline-block; padding: .2rem .5rem; border-radius: .35rem; background: #eee9e2; color: #62544a; font-size: .8125rem; white-space: nowrap; }
.sale-status.active { background: #eaf0e8; color: #355341; }
.row-actions { text-align: right; white-space: nowrap; }
.text-button { min-height: 2.25rem; padding: .25rem .45rem; border: 0; border-radius: .35rem; background: transparent; cursor: pointer; font-size: .875rem; }
.text-button:hover:not(:disabled) { background: #f2ece4; }
.pagination { display: flex; align-items: center; justify-content: end; gap: .5rem; padding: .8rem 1.25rem; font-size: .875rem; color: var(--muted); }

.delete-dialog { width: min(100% - 2rem, 27rem); padding: 1.5rem; border: 1px solid #e5ded4; border-radius: .85rem; background: #fffdf9; color: inherit; }
.delete-dialog::backdrop { background: #33201965; }
.delete-dialog h2 { margin: 0; font-size: 1.4rem; }
.delete-actions { display: flex; justify-content: end; gap: .65rem; margin-top: 1.25rem; }

@media (max-width: 760px) {
  .menu-table { min-width: 0; }
  .menu-table .updated-cell { display: none; }
  .menu-table th, .menu-table td { padding-inline: .45rem; }
  .menu-table .check-cell { padding-inline: .7rem .15rem; }
  .menu-identity { gap: .4rem; }
  .menu-table .row-actions { white-space: normal; }
  .row-actions .text-button { white-space: nowrap; }
  .search-field { flex: 1 1 9rem; width: auto; max-width: 17rem; min-width: 0; }
  .category-field { flex: 1 1 6rem; width: auto; max-width: 12rem; min-width: 0; }
  .menu-filter, .menu-actions, .pagination { padding-inline: .8rem; }
}
</style>
