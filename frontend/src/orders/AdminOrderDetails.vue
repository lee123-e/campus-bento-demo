<script setup>
import { onUnmounted, ref } from 'vue'
import { requestAdminData } from '../adminApi.js'
import { formatMoney, orderStatus, orderTime, showFlavors } from '../commerce.js'
import MenuMealContents from '../menu/MenuMealContents.vue'

const emit = defineEmits(['session-expired', 'status-changed'])
const dialog = ref(null)
const detail = ref(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
let detailRequest
let orderId

async function loadDetail(id) {
  detailRequest?.abort()
  const request = new AbortController()
  detailRequest = request
  loading.value = true
  try {
    const result = await requestAdminData('/orders/' + id, { signal: request.signal })
    if (!request.signal.aborted) detail.value = result
  } catch (failure) {
    if (request.signal.aborted) return
    if (failure.status === 401) emit('session-expired')
    else error.value = failure.message
  } finally {
    if (request === detailRequest) loading.value = false
  }
}

function open(id) {
  orderId = id
  detail.value = null
  error.value = ''
  notice.value = ''
  dialog.value.showModal()
  loadDetail(id)
}

async function advance(action) {
  if (saving.value) return
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const order = await requestAdminData('/orders/' + orderId + '/status', { method: 'PUT', body: { action } })
    detail.value = { ...detail.value, order }
    notice.value = action === 'DISPATCH' ? '订单已标记为已派送。' : '订单已标记为已完成。'
    emit('status-changed')
  } catch (failure) {
    if (failure.status === 401) emit('session-expired')
    else {
      error.value = failure.message
      if (failure.status === 409) {
        await loadDetail(orderId)
        emit('status-changed')
      }
    }
  } finally {
    saving.value = false
  }
}

function cancelLoad() {
  detailRequest?.abort()
}

onUnmounted(cancelLoad)
defineExpose({ open })
</script>

<template>
  <dialog ref="dialog" class="admin-order-dialog" aria-labelledby="admin-order-detail-heading"
          @close="cancelLoad" @cancel="saving && $event.preventDefault()">
    <h2 id="admin-order-detail-heading">订单明细</h2>
    <p v-if="loading" class="detail-muted" role="status">正在加载订单…</p>
    <p v-if="error" class="notice detail-feedback" role="alert">{{ error }}</p>
    <p v-if="notice" class="detail-success detail-feedback" role="status">{{ notice }}</p>

    <template v-if="detail">
      <div class="detail-summary">
        <div>
          <strong class="detail-status">{{ orderStatus(detail.order.status) }}</strong>
          <p class="detail-number">{{ detail.order.number }}</p>
        </div>
        <strong>{{ formatMoney(detail.order.total) }}</strong>
      </div>

      <section class="detail-section" aria-labelledby="admin-order-food-heading">
        <h3 id="admin-order-food-heading">备餐明细</h3>
        <ul class="detail-items">
          <li v-for="(item,index) in detail.items" :key="index">
            <div>
              <strong>{{ item.name }} × {{ item.quantity }}</strong>
              <p v-if="item.flavors.length" class="detail-flavors">{{ showFlavors(item.flavors) }}</p>
              <MenuMealContents :contents="item.mealContents" :copies="item.quantity" />
              <small v-if="item.type === 2 && !item.mealContents?.length" class="detail-muted">这笔历史订单未记录套餐内菜品。</small>
            </div>
            <span class="detail-amount">{{ formatMoney(item.subtotal) }}</span>
          </li>
        </ul>
      </section>

      <section class="detail-section" aria-labelledby="admin-order-delivery-heading">
        <h3 id="admin-order-delivery-heading">配送信息</h3>
        <dl class="detail-fields">
          <dt>收货人</dt><dd>{{ detail.delivery.contactName }}</dd>
          <dt>联系电话</dt><dd>{{ detail.delivery.phone }}</dd>
          <dt>收货地址</dt><dd>{{ detail.delivery.deliveryAddress }}</dd>
          <template v-if="detail.note"><dt>订单备注</dt><dd>{{ detail.note }}</dd></template>
        </dl>
      </section>

      <section class="detail-section" aria-labelledby="admin-order-timeline-heading">
        <h3 id="admin-order-timeline-heading">订单时间</h3>
        <dl class="detail-fields">
          <dt>下单</dt><dd>{{ orderTime(detail.order.createdAt) }}</dd>
          <template v-if="detail.order.paidAt"><dt>支付</dt><dd>{{ orderTime(detail.order.paidAt) }}</dd></template>
          <template v-if="detail.order.dispatchedAt"><dt>派送</dt><dd>{{ orderTime(detail.order.dispatchedAt) }}</dd></template>
          <template v-if="detail.order.completedAt"><dt>完成</dt><dd>{{ orderTime(detail.order.completedAt) }}</dd></template>
        </dl>
      </section>
    </template>

    <div class="detail-actions">
      <button type="button" class="quiet-button" :disabled="saving" autofocus @click="dialog.close()">关闭</button>
      <button v-if="detail?.order.status === 2" type="button" class="primary-button" :disabled="loading || saving"
              @click="advance('DISPATCH')">{{ saving ? '处理中…' : '标记已派送' }}</button>
      <button v-if="detail?.order.status === 3" type="button" class="primary-button" :disabled="loading || saving"
              @click="advance('COMPLETE')">{{ saving ? '处理中…' : '标记已完成' }}</button>
    </div>
  </dialog>
</template>

<style scoped>
.admin-order-dialog {
  width: min(100% - 2rem, 41rem); max-height: calc(100svh - 2rem); padding: clamp(1rem, 4vw, 1.75rem);
  border: 1px solid #e5ded4; border-radius: .9rem; background: #fffdf9; color: #25211e;
}
.admin-order-dialog::backdrop { background: #33201965; }
.admin-order-dialog h2 { margin: 0 0 .8rem; font-size: 1.4rem; }
.detail-summary { display: flex; justify-content: space-between; align-items: start; gap: .5rem; padding: .75rem; border-radius: .55rem; background: #f7eee6; }
.detail-summary > strong { white-space: nowrap; font-variant-numeric: tabular-nums; }
.detail-status { color: #8d321f; }
.detail-muted, .detail-number { color: var(--muted); font-size: .875rem; overflow-wrap: anywhere; }
.detail-feedback { margin-block: .5rem; }
.detail-success { color: #375041; }
.detail-section { margin-top: .8rem; padding: .7rem .8rem; border: 1px solid #e5ded4; border-radius: .55rem; }
.detail-section h3 { margin: 0 0 .4rem; font-size: 1rem; }
.detail-items { margin: 0; padding: 0; list-style: none; }
.detail-items li { display: flex; gap: .65rem; justify-content: space-between; padding: .4rem 0; }
.detail-items li + li { border-top: 1px solid #ece6dc; }
.detail-items li > div { min-width: 0; overflow-wrap: anywhere; }
.detail-amount { white-space: nowrap; font-variant-numeric: tabular-nums; }
.detail-flavors { color: var(--muted); font-size: .88rem; }
.detail-fields { display: grid; grid-template-columns: auto minmax(0,1fr); gap: .35rem .65rem; margin: 0; }
.detail-fields dt { color: var(--muted); font-size: .875rem; }
.detail-fields dd { margin: 0; overflow-wrap: anywhere; }
.detail-actions { display: flex; justify-content: end; gap: .6rem; margin-top: 1rem; }
.detail-actions .primary-button { margin: 0; }
</style>
