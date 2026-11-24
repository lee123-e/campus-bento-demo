<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { requestMenuData } from '../menu/menuApi.js'

defineProps({ disabled: Boolean })
const emit = defineEmits(['session-expired'])
const selected = defineModel({ type: Array, required: true })
const categories = ref([])
const candidates = ref([])
const name = ref('')
const categoryId = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const error = ref('')
const pageSize = 5
const lastPage = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const money = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' })
let request
let appliedName = ''

function isSelected(id) {
  return selected.value.some(item => item.dishId === id)
}

function add(dish) {
  selected.value = [...selected.value, {
    dishId: dish.id, name: dish.name, price: dish.price, onSale: dish.onSale, quantity: 1,
  }]
}

function remove(id) {
  selected.value = selected.value.filter(item => item.dishId !== id)
}

async function loadCategories() {
  try {
    categories.value = await requestMenuData('/categories/options?type=1')
  } catch (failure) {
    if (failure.status === 401) emit('session-expired')
  }
}

async function loadDishes() {
  request?.abort()
  const current = new AbortController()
  request = current
  loading.value = true
  error.value = ''
  const query = new URLSearchParams({ page: page.value, size: pageSize })
  if (appliedName) query.set('name', appliedName)
  if (categoryId.value) query.set('categoryId', categoryId.value)
  try {
    const result = await requestMenuData('/dishes?' + query, { signal: current.signal })
    if (current.signal.aborted) return
    candidates.value = result.items
    total.value = result.total
  } catch (failure) {
    if (current.signal.aborted) return
    if (failure.status === 401) emit('session-expired')
    else error.value = '菜品列表加载失败，请重试。'
  } finally {
    if (request === current) loading.value = false
  }
}

function search() {
  appliedName = name.value
  page.value = 1
  loadDishes()
}

function changePage(next) {
  page.value = next
  loadDishes()
}

onMounted(() => {
  loadCategories()
  loadDishes()
})
onUnmounted(() => request?.abort())
</script>

<template>
  <section class="picker" aria-labelledby="setmeal-dishes-heading">
    <div class="picker-header">
      <h3 id="setmeal-dishes-heading">套餐菜品</h3>
      <span>{{ selected.length }} 种已选</span>
    </div>
    <div v-if="selected.length" class="selected-dishes">
      <div v-for="item in selected" :key="item.dishId" class="selected-dish">
        <div class="dish-name">
          <strong>{{ item.name }}</strong>
          <span>{{ money.format(item.price) }}<span v-if="!item.onSale"> · 停售</span></span>
        </div>
        <label class="quantity">
          <span>份数</span>
          <input v-model.number="item.quantity" type="number" min="1" max="99" step="1" required
                 :aria-label="item.name + '份数'" :disabled="disabled" />
        </label>
        <button class="quiet-button" type="button" :aria-label="'移除' + item.name" :disabled="disabled" @click="remove(item.dishId)">移除</button>
      </div>
    </div>
    <p v-else class="picker-empty">从下方添加菜品，再填写每道菜的份数。</p>

    <div class="dish-search">
      <label class="field">
        <span>查找菜品</span>
        <input v-model.trim="name" type="search" maxlength="60" placeholder="输入菜品名称" @keydown.enter.prevent="search" />
      </label>
      <label class="field">
        <span>菜品分类</span>
        <select v-model.number="categoryId" @change="search">
          <option value="">全部菜品分类</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
        </select>
      </label>
      <button class="quiet-button" type="button" @click="search">查找</button>
    </div>
    <p v-if="loading" class="picker-message" role="status">正在查找菜品…</p>
    <p v-else-if="error" class="picker-message" role="alert">
      {{ error }}
      <button class="quiet-button" type="button" @click="loadDishes">重试</button>
    </p>
    <p v-else-if="!candidates.length" class="picker-message">没有找到符合条件的菜品。</p>
    <template v-else>
      <ul class="dish-results">
        <li v-for="dish in candidates" :key="dish.id">
          <div class="dish-name">
            <strong>{{ dish.name }}</strong>
            <span>{{ dish.categoryName }} · {{ money.format(dish.price) }}<span v-if="!dish.onSale"> · 停售</span></span>
          </div>
          <button class="quiet-button" type="button" :aria-label="'添加' + dish.name"
                  :disabled="disabled || isSelected(dish.id) || selected.length >= 50" @click="add(dish)">
            {{ isSelected(dish.id) ? '已添加' : '添加' }}
          </button>
        </li>
      </ul>
      <nav v-if="lastPage > 1" class="picker-pager" aria-label="可选菜品分页">
        <span>第 {{ page }} / {{ lastPage }} 页</span>
        <button class="quiet-button" type="button" :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
        <button class="quiet-button" type="button" :disabled="page === lastPage" @click="changePage(page + 1)">下一页</button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.picker { min-width: 0; padding: .9rem 1rem; border: 1px solid #e5ded4; border-radius: .65rem; }
.picker-header, .selected-dish, .dish-results li, .picker-pager { display: flex; align-items: center; gap: .65rem; }
.picker-header { justify-content: space-between; }
.picker-header h3 { margin: 0; font-size: 1rem; }
.picker-header span, .picker-message, .picker-empty { color: var(--muted); font-size: .875rem; }
.picker-empty, .picker-message { margin-top: .55rem; }
.selected-dishes { margin-top: .55rem; padding: .15rem .65rem; border-radius: .5rem; background: #f7f3ed; }
.selected-dish, .dish-results li { padding: .5rem 0; border-bottom: 1px solid #e5ded4; }
.selected-dish:last-child, .dish-results li:last-child { border-bottom: 0; }
.dish-name { flex: 1; min-width: 0; display: grid; gap: .12rem; overflow-wrap: anywhere; }
.dish-name strong { font-weight: 650; }
.dish-name span, .quantity, .picker-pager { color: var(--muted); font-size: .875rem; }
.quantity { display: flex; flex-shrink: 0; align-items: center; gap: .35rem; }
.quantity input { width: 4rem; min-height: 2.35rem; padding: .2rem .35rem; border: 1px solid #9a8c7f; border-radius: .4rem; background: #fffdf9; color: #25211e; }
.dish-search { margin-top: .95rem; display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr) auto; align-items: end; gap: .55rem; }
.dish-search .field input, .dish-search select { width: 100%; min-height: 2.65rem; padding: .4rem .6rem; border: 1px solid #9a8c7f; border-radius: .45rem; background: #fffdf9; color: inherit; font: inherit; }
.dish-results { padding: 0; margin: .3rem 0 0; list-style: none; }
.picker .quiet-button { min-height: 2.4rem; flex-shrink: 0; padding: .3rem .55rem; font-size: .875rem; }
.picker-pager { justify-content: end; margin-top: .25rem; }

@media (max-width: 520px) {
  .dish-search { grid-template-columns: 1fr auto; }
  .dish-search > label:first-child { grid-column: 1 / -1; }
  .quantity { gap: .2rem; }
  .quantity input { width: 3.25rem; }
}
</style>
