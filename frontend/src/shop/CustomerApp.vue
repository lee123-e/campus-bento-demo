<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import CustomerAccount from './CustomerAccount.vue'
import CustomerAvatar from './CustomerAvatar.vue'
import CustomerLoginForm from './CustomerLoginForm.vue'
import CustomerMenu from './CustomerMenu.vue'
import CustomerCart from './CustomerCart.vue'
import CustomerCheckout from './CustomerCheckout.vue'
import CustomerOrders from './CustomerOrders.vue'
import { customerRequest } from './customerApi.js'
import './shop.css'

function currentView() {
  const section = location.hash.slice(1)
  if (/^order\/[1-9]\d*$/.test(section)) return section
  return ['me', 'profile', 'addresses', 'cart', 'checkout', 'orders'].includes(section) ? section : 'menu'
}

const user = ref(null)
const cartQuantity = ref(0)
let cartRevision = 0
const activeView = ref(currentView())
const sessionLoading = ref(true)
const sessionError = ref('')
const signingOut = ref(false)

function syncView() {
  activeView.value = currentView()
}

async function loadSession() {
  sessionLoading.value = true
  sessionError.value = ''
  try {
    user.value = await customerRequest('/session')
    refreshCartQuantity()
  } catch (error) {
    user.value = null
    if (error.status !== 401) sessionError.value = '登录状态加载失败，请重试。'
  } finally {
    sessionLoading.value = false
  }
}

function setCartQuantity(quantity) {
  cartRevision++
  cartQuantity.value = quantity
}

async function refreshCartQuantity() {
  const activeCustomerId = user.value?.id
  const revision = ++cartRevision
  try {
    const cart = await customerRequest('/cart')
    if (user.value?.id === activeCustomerId && cartRevision === revision) setCartQuantity(cart.quantity)
  } catch {
    // The cart page reports load errors and refreshes the count on success.
  }
}

function signedIn(customer) {
  user.value = customer
  sessionError.value = ''
  refreshCartQuantity()
}

function expire() {
  user.value = null
  setCartQuantity(0)
  sessionError.value = '登录已过期，请重新登录。'
}

async function signOut() {
  signingOut.value = true
  try {
    await customerRequest('/logout', { method: 'POST' })
    user.value = null
    setCartQuantity(0)
    sessionError.value = ''
    location.hash = 'menu'
  } catch (error) {
    if (error.status === 401) expire()
    else sessionError.value = error.message
  } finally {
    signingOut.value = false
  }
}

onMounted(() => {
  window.addEventListener('hashchange', syncView)
  loadSession()
})
onUnmounted(() => window.removeEventListener('hashchange', syncView))
</script>

<template>
  <div class="shop-shell">
    <header class="shop-header">
      <div class="shop-header-inner">
        <a href="/" class="wordmark" aria-label="校园便当，返回首页"><span class="brand-mark" aria-hidden="true"></span>校园便当</a>
        <nav v-if="user" class="shop-navigation" aria-label="顾客端">
          <a href="#menu" :aria-current="activeView === 'menu' ? 'page' : undefined">菜单</a>
          <a href="#cart" :aria-current="['cart','checkout'].includes(activeView) ? 'page' : undefined">
            购物车<span v-if="cartQuantity" class="shop-cart-count">{{ cartQuantity }}</span>
          </a>
        </nav>
        <div class="shop-account">
          <a v-if="user" class="shop-account-link" href="#me" :aria-current="!['menu','cart','checkout'].includes(activeView) ? 'page' : undefined">
            <CustomerAvatar :customer="user" />
            <span>我的</span>
          </a>
          <a v-else href="/" class="shop-return-home">返回首页</a>
        </div>
      </div>
    </header>

    <main class="shop-main">
      <p v-if="sessionError" class="notice shop-session-notice" role="alert">{{ sessionError }}</p>
      <p v-if="sessionLoading" role="status" class="shop-empty">正在检查登录状态…</p>
      <CustomerLoginForm v-else-if="!user" @signed-in="signedIn" />
      <CustomerMenu v-else-if="activeView === 'menu'" @cart-updated="setCartQuantity" @session-expired="expire" />
      <CustomerCart v-else-if="activeView === 'cart'" @cart-updated="setCartQuantity" @session-expired="expire" />
      <CustomerCheckout v-else-if="activeView === 'checkout'" :customer-phone="user.phone" @cart-updated="setCartQuantity" @session-expired="expire" />
      <CustomerOrders v-else-if="activeView === 'orders' || activeView.startsWith('order/')" :key="activeView"
                      :order-id="activeView.startsWith('order/') ? activeView.slice(6) : ''" @session-expired="expire" />
      <CustomerAccount v-else :key="user.id + ':' + activeView" :section="activeView" :customer="user"
                       :signing-out="signingOut" @profile-updated="signedIn" @sign-out="signOut" @session-expired="expire" />
    </main>
  </div>
</template>
