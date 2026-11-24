<script setup>
import { computed, onMounted, ref } from 'vue'
import { customerRequest } from './customerApi.js'
import MenuMealContents from '../menu/MenuMealContents.vue'
import { formatMoney, orderStatus, orderTime, showFlavors } from '../commerce.js'

const props = defineProps({ orderId: { type: String, default: '' } })
const emit = defineEmits(['session-expired'])
const result = ref(null)
const detail = ref(null)
const page = ref(1)
const size = 10
const lastPage = computed(() => Math.max(1, Math.ceil((result.value?.total ?? 0) / size)))
const loading = ref(true)
const paying = ref(false)
const error = ref('')
const notice = ref('')

function handle(failure) {
  if (failure.status === 401) emit('session-expired')
  else error.value = failure.message
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (props.orderId) {
      detail.value = await customerRequest('/orders/' + props.orderId)
    } else {
      result.value = await customerRequest('/orders?page=' + page.value + '&size=' + size)
    }
  } catch (failure) {
    handle(failure)
  } finally {
    loading.value = false
  }
}

function changePage(next) {
  page.value = next
  load()
}

async function pay() {
  if (paying.value) return
  paying.value = true
  error.value = ''
  try {
    const order = await customerRequest('/orders/' + props.orderId + '/demo-payment', { method: 'POST' })
    detail.value = { ...detail.value, order }
    notice.value = order.status === 2 ? '模拟支付成功，订单已进入待派送。' : '订单状态已更新。'
  } catch (failure) {
    handle(failure)
  } finally {
    paying.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="shop-commerce" aria-labelledby="orders-heading">
    <a class="shop-account-back" :href="orderId ? '#orders' : '#me'">{{ orderId ? '返回我的订单' : '返回我的' }}</a>
    <h1 id="orders-heading">{{ orderId ? '订单详情' : '我的订单' }}</h1>
    <p v-if="error" class="notice shop-order-feedback" role="alert">{{ error }}</p>
    <p v-if="notice" class="shop-success" role="status">{{ notice }}</p>
    <p v-if="loading" class="shop-empty" role="status">正在加载订单…</p>
    <template v-else-if="orderId && detail">
      <div class="shop-order-panel shop-order-summary">
        <div><strong class="shop-order-status">{{ orderStatus(detail.order.status) }}</strong>
          <p class="shop-order-number">订单号 {{ detail.order.number }}</p>
        </div>
        <strong class="shop-price">{{ formatMoney(detail.order.total) }}</strong>
      </div>
      <section class="shop-order-panel" aria-labelledby="order-items-heading">
        <h2 id="order-items-heading">商品明细</h2>
        <ul class="shop-order-lines">
          <li v-for="(item,index) in detail.items" :key="index">
            <span><strong>{{ item.name }} × {{ item.quantity }}</strong>
              <small v-if="item.flavors.length">{{ showFlavors(item.flavors) }}</small>
              <MenuMealContents :contents="item.mealContents" />
              <small v-if="item.type === 2 && !item.mealContents?.length">这笔历史订单未记录套餐内菜品。</small>
            </span>
            <span>{{ formatMoney(item.subtotal) }}</span>
          </li>
        </ul>
      </section>
      <section class="shop-order-panel" aria-labelledby="order-delivery-heading">
        <h2 id="order-delivery-heading">配送信息</h2>
        <p>{{ detail.delivery.deliveryAddress }}</p>
        <p class="shop-muted">{{ detail.delivery.contactName }} · {{ detail.delivery.phone.slice(0, 3) + '****' + detail.delivery.phone.slice(-4) }}</p>
        <p v-if="detail.note">备注 {{ detail.note }}</p>
        <p class="shop-muted">下单时间 {{ orderTime(detail.order.createdAt) }}</p>
        <p v-if="detail.order.dispatchedAt" class="shop-muted">派送时间 {{ orderTime(detail.order.dispatchedAt) }}</p>
        <p v-if="detail.order.completedAt" class="shop-muted">完成时间 {{ orderTime(detail.order.completedAt) }}</p>
      </section>
      <button v-if="detail.order.status === 1" type="button" class="primary-button" :disabled="paying" @click="pay">
        {{ paying ? '处理中…' : '模拟支付' }}
      </button>
    </template>
    <div v-else-if="!orderId && result && !result.items.length" class="shop-empty">
      <h2>还没有订单</h2>
      <a class="shop-action primary-button" href="#menu">去看菜单</a>
    </div>
    <template v-else-if="!orderId && result?.items.length">
      <ul class="shop-orders-list">
        <li v-for="order in result.items" :key="order.id">
          <a class="shop-order-panel shop-order-summary" :href="'#order/' + order.id">
            <span><strong>{{ orderStatus(order.status) }}</strong>
              <small class="shop-order-number">{{ order.number }}</small>
              <small class="shop-muted">{{ orderTime(order.createdAt) }}</small>
            </span>
            <strong class="shop-price">{{ formatMoney(order.total) }}</strong>
          </a>
        </li>
      </ul>
      <nav v-if="lastPage > 1" class="shop-orders-pager" aria-label="订单分页">
        <span>第 {{ page }} / {{ lastPage }} 页</span>
        <button type="button" class="quiet-button" :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
        <button type="button" class="quiet-button" :disabled="page === lastPage" @click="changePage(page + 1)">下一页</button>
      </nav>
    </template>
    <button v-else-if="error" class="quiet-button" type="button" @click="load">重试</button>
  </section>
</template>
