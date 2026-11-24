<script setup>
import { computed, onMounted, ref } from 'vue'
import { customerRequest } from './customerApi.js'
import ShopAddressEditor from './ShopAddressEditor.vue'
import MenuMealContents from '../menu/MenuMealContents.vue'
import { formatMoney, showFlavors } from '../commerce.js'

defineProps({ customerPhone: { type: String, required: true } })
const addressEditor = ref(null)
const emit = defineEmits(['cart-updated', 'session-expired'])
const cart = ref(null)
const addresses = ref([])
const addressId = ref(null)
const note = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const stale = ref(false)
const canSubmit = computed(() => addressId.value && cart.value?.items.length && cart.value.items.every(item => item.available))
let requestId

function handle(failure) {
  if (failure.status === 401) emit('session-expired')
  else error.value = failure.message
}

async function load() {
  loading.value = true
  error.value = ''
  stale.value = false
  try {
    const [result, ownAddresses] = await Promise.all([customerRequest('/cart'), customerRequest('/addresses')])
    cart.value = result
    addresses.value = ownAddresses
    if (!ownAddresses.some(address => address.id === addressId.value)) {
      addressId.value = (ownAddresses.find(address => address.defaultAddress) ?? ownAddresses[0])?.id ?? null
    }
    requestId = crypto.randomUUID()
    emit('cart-updated', result.quantity)
  } catch (failure) {
    handle(failure)
  } finally {
    loading.value = false
  }
}

function addressSaved({ address }) {
  addresses.value = [address, ...addresses.value]
  addressId.value = address.id
}

async function place() {
  if (saving.value || !canSubmit.value || stale.value) return
  saving.value = true
  error.value = ''
  try {
    const created = await customerRequest('/orders', { method: 'POST', body: {
      addressId: addressId.value, note: note.value, quote: cart.value.quote, requestId,
    } })
    emit('cart-updated', 0)
    location.hash = 'order/' + created.id
  } catch (failure) {
    if (failure.status === 409 || failure.status === 404) stale.value = true
    handle(failure)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="shop-commerce" aria-labelledby="checkout-heading">
    <a href="#cart" class="shop-account-back">返回购物车</a>
    <h1 id="checkout-heading">确认订单</h1>
    <p v-if="loading" class="shop-empty" role="status">正在核对购物车和地址…</p>
    <div v-if="error" class="shop-order-feedback">
      <p class="notice" role="alert">{{ error }}</p>
      <button v-if="stale || !cart" type="button" class="quiet-button" @click="load">刷新结算信息</button>
    </div>
    <div v-if="!loading && cart && !cart.items.length" class="shop-empty">
      <h2>购物车是空的</h2>
      <a class="shop-action primary-button" href="#menu">去看菜单</a>
    </div>
    <form v-else-if="!loading && cart?.items.length" class="shop-checkout-form" @submit.prevent="place">
      <fieldset class="shop-order-panel">
        <legend>收货地址</legend>
        <ul v-if="addresses.length" class="shop-address-options">
          <li v-for="address in addresses" :key="address.id">
            <label>
              <input v-model.number="addressId" type="radio" name="address" :value="address.id" :disabled="saving" required />
              <span><strong>{{ address.deliveryAddress }}</strong>
                <span class="shop-muted">{{ address.contactName }} · {{ address.phone }}</span>
                <span v-if="address.defaultAddress" class="shop-default-label">默认地址</span>
              </span>
            </label>
          </li>
        </ul>
        <button type="button" class="shop-inline-button shop-address-add" :disabled="saving" @click="addressEditor.open()">
          {{ addresses.length ? '添加新地址' : '添加收货地址' }}
        </button>
      </fieldset>

      <section class="shop-order-panel" aria-labelledby="checkout-items-heading">
        <h2 id="checkout-items-heading">商品明细</h2>
        <ul class="shop-order-lines">
          <li v-for="item in cart.items" :key="item.id">
            <span><strong>{{ item.name }} × {{ item.quantity }}</strong>
              <small v-if="item.flavors.length">{{ showFlavors(item.flavors) }}</small>
              <MenuMealContents :contents="item.mealContents" />
            </span>
            <span v-if="item.available">{{ formatMoney(item.subtotal) }}</span>
            <span v-else class="shop-danger">{{ item.issue === 'FLAVOR_CHANGED' ? '口味已变更' : '暂不可结算' }}</span>
          </li>
        </ul>
        <p v-if="cart.items.some(item => !item.available)" class="notice">请返回购物车调整口味或移除不可结算商品。</p>
      </section>
      <label class="shop-field">
        <span>订单备注（可选）</span>
        <textarea v-model.trim="note" maxlength="200" rows="2" placeholder="如送到后电话联系" :disabled="saving"></textarea>
      </label>
      <div class="shop-checkout-bar">
        <p>应付 <strong class="shop-price">{{ formatMoney(cart.total) }}</strong></p>
        <button class="primary-button" type="submit" :disabled="saving || !canSubmit || stale">
          {{ saving ? '提交中…' : '提交订单' }}
        </button>
      </div>
    </form>
    <ShopAddressEditor ref="addressEditor" :customer-phone="customerPhone" @saved="addressSaved"
                       @session-expired="emit('session-expired')" />
  </section>
</template>
