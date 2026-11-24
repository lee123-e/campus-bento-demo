<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { requestAdminData } from '../adminApi.js'
import { formatMoney, orderStatus, orderTime } from '../commerce.js'
import AdminOrderDetails from './AdminOrderDetails.vue'

const emit = defineEmits(['session-expired'])
const filters = reactive({ number: '', status: '', startDate: '', endDate: '' })
const statuses = [1, 2, 3, 4, 5]
const page = ref(1)
const pageSize = 10
const total = ref(0)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const rows = ref([])
const loading = ref(true)
const error = ref('')
const details = ref(null)
let request
let applied = {}

async function loadOrders() {
  request?.abort()
  const current = new AbortController()
  request = current
  loading.value = true
  error.value = ''
  const query = new URLSearchParams({ page: page.value, size: pageSize })
  for (const [key, value] of Object.entries(applied)) {
    if (value !== '') query.set(key, value)
  }
  try {
    const result = await requestAdminData('/orders?' + query, { signal: current.signal })
    if (current.signal.aborted) return
    total.value = result.total
    if (page.value > pageCount.value) {
      page.value = pageCount.value
      await loadOrders()
      return
    }
    rows.value = result.items
  } catch (failure) {
    if (current.signal.aborted) return
    if (failure.status === 401) emit('session-expired')
    else error.value = failure.message || '订单加载失败，请重试。'
  } finally {
    if (request === current) loading.value = false
  }
}

function search() {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    error.value = '开始日期不能晚于结束日期。'
    return
  }
  applied = { ...filters }
  page.value = 1
  loadOrders()
}

function reset() {
  Object.assign(filters, { number: '', status: '', startDate: '', endDate: '' })
  search()
}

function goToPage(next) {
  page.value = next
  loadOrders()
}

onMounted(loadOrders)
onUnmounted(() => request?.abort())
</script>

<template>
  <section class="admin-orders" aria-labelledby="admin-orders-heading">
    <div class="order-heading">
      <h1 id="admin-orders-heading">订单管理</h1>
      <p class="description">查询订单、查看备餐明细和更新派送状态。</p>
    </div>

    <div class="order-panel">
      <form class="order-filter" @submit.prevent="search">
        <label class="order-number-input">
          <span>订单号</span>
          <input v-model.trim="filters.number" type="search" maxlength="40" placeholder="输入全部或部分订单号" />
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option v-for="status in statuses" :key="status" :value="status">{{ orderStatus(status) }}</option>
          </select>
        </label>
        <label>
          <span>开始日期</span>
          <input v-model="filters.startDate" type="date" min="0001-01-01" max="9999-12-31" />
        </label>
        <label>
          <span>结束日期</span>
          <input v-model="filters.endDate" type="date" min="0001-01-01" max="9999-12-31" />
        </label>
        <div class="order-filter-actions">
          <button type="submit" class="primary-button">查询</button>
          <button type="button" class="quiet-button" @click="reset">重置</button>
        </div>
      </form>

      <p v-if="error" class="notice order-error" role="alert">{{ error }}</p>
      <p v-if="loading" class="order-empty" role="status">正在加载订单…</p>
      <div v-else-if="!rows.length" class="order-empty">
        <p>{{ error ? '订单列表暂不可查看。' : '当前条件下暂无订单。' }}</p>
      </div>
      <template v-else>
        <p class="order-count" role="status">共 {{ total }} 笔订单</p>
        <div class="order-table-scroll">
          <table class="order-table">
            <thead>
              <tr>
                <th scope="col">订单号</th>
                <th scope="col">状态</th>
                <th scope="col" class="contact-cell">收货人</th>
                <th scope="col">金额</th>
                <th scope="col" class="time-cell">下单时间</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.order.id">
                <th scope="row" class="order-number">{{ row.order.number }}</th>
                <td><span class="order-status" :data-state="row.order.status">{{ orderStatus(row.order.status) }}</span></td>
                <td class="contact-cell">{{ row.contactName }}</td>
                <td class="money-cell">{{ formatMoney(row.order.total) }}</td>
                <td class="time-cell"><time :datetime="row.order.createdAt">{{ orderTime(row.order.createdAt) }}</time></td>
                <td><button type="button" class="order-detail-link" :aria-label="'查看明细 ' + row.order.number"
                            @click="details.open(row.order.id)">查看明细</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <nav class="order-pager" aria-label="商家订单分页">
          <span>第 {{ page }} / {{ pageCount }} 页</span>
          <button type="button" class="quiet-button" :disabled="page === 1" @click="goToPage(page - 1)">上一页</button>
          <button type="button" class="quiet-button" :disabled="page === pageCount" @click="goToPage(page + 1)">下一页</button>
        </nav>
      </template>
    </div>

    <AdminOrderDetails ref="details" @status-changed="loadOrders" @session-expired="emit('session-expired')" />
  </section>
</template>

<style scoped>
.order-heading { margin-bottom: 1.55rem; }
.order-panel { border: 1px solid #e5ded4; border-radius: .85rem; background: #fffdf9; }
.order-filter {
  display: flex; flex-wrap: wrap; align-items: end; gap: .65rem; margin: 0; padding: 1.1rem 1.25rem;
  border-bottom: 1px solid #e5ded4;
}
.order-filter label { display: grid; gap: .35rem; color: var(--muted); font-size: .875rem; }
.order-number-input { flex: 1 1 14rem; max-width: 22rem; min-width: 0; }
.order-filter input, .order-filter select {
  width: 100%; min-height: 2.75rem; padding: .5rem .65rem; border: 1px solid #9a8c7f;
  border-radius: .55rem; background: #fffdf9; color: #25211e; font: inherit;
}
.order-filter-actions { display: flex; gap: .4rem; }
.order-filter-actions .primary-button { margin: 0; min-height: 2.75rem; }
.order-error { margin: .8rem 1.25rem; }
.order-empty { padding: 2.5rem 1rem; color: var(--muted); text-align: center; }
.order-count { padding: .8rem 1.25rem; color: var(--muted); font-size: .875rem; }
.order-table-scroll { overflow-x: auto; }
.order-table { width: 100%; border-collapse: collapse; text-align: left; }
.order-table th, .order-table td { padding: .7rem .8rem; border-bottom: 1px solid #ece6dc; }
.order-table thead { background: #f7f3ed; color: #60554b; font-size: .875rem; white-space: nowrap; }
.order-table tbody tr:hover { background: #fcf8f1; }
.order-number { overflow-wrap: anywhere; font-weight: 650; font-size: .88rem; }
.money-cell, .time-cell { font-variant-numeric: tabular-nums; white-space: nowrap; }
.time-cell { color: var(--muted); font-size: .875rem; }
.order-status { display: inline-block; padding: .2rem .45rem; border-radius: .35rem; background: #eee9e2; color: #62544a; font-size: .82rem; white-space: nowrap; }
.order-status[data-state="2"] { color: #8d321f; background: #f5e7df; }
.order-status[data-state="3"] { color: #305370; background: #e5eef3; }
.order-status[data-state="4"] { color: #355341; background: #eaf0e8; }
.order-detail-link { min-height: 2.25rem; padding: .25rem .4rem; border: 0; border-radius: .35rem; background: transparent; color: #8d321f; cursor: pointer; white-space: nowrap; }
.order-detail-link:hover { background: #f2ece4; }
.order-pager { display: flex; align-items: center; justify-content: end; gap: .5rem; padding: .85rem 1.25rem; color: var(--muted); font-size: .875rem; }
.order-pager button:disabled { cursor: default; }

@media (max-width: 680px) {
  .order-table .time-cell { display: none; }
  .order-table th, .order-table td, .order-filter, .order-pager { padding-inline: .6rem; }
}
@media (max-width: 430px) {
  .order-table .contact-cell { display: none; }
  .order-filter label { min-width: 8rem; }
}
</style>
