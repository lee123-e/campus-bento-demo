<script setup>
import { computed, onMounted, ref } from 'vue'
import { catalogImage, customerRequest } from './customerApi.js'
import { formatMoney, showFlavors } from '../commerce.js'
import ShopFlavorChoices from './ShopFlavorChoices.vue'
import MenuMealContents from '../menu/MenuMealContents.vue'

const emit = defineEmits(['cart-updated', 'session-expired'])
const cart = ref(null)
const busy = ref(false)
const error = ref('')
const clearDialog = ref(null)
const reselectDialog = ref(null)
const reselectItem = ref(null)
const detail = ref(null)
const choices = ref([])
const choiceError = ref('')
const choosing = ref(false)
const notice = ref('')
const hasUnavailable = computed(() => cart.value?.items.some(item => !item.available))

async function update(path = '/cart', options) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const result = await customerRequest(path, options)
    showCart(result)
  } catch (failure) {
    if (failure.status === 401) emit('session-expired')
    else error.value = failure.message
  } finally {
    busy.value = false
  }
}

function showCart(result) {
  cart.value = result
  emit('cart-updated', result.quantity)
}

async function openReselect(item) {
  reselectItem.value = item
  detail.value = null
  choices.value = []
  choiceError.value = ''
  notice.value = ''
  choosing.value = true
  reselectDialog.value.showModal()
  try {
    const current = await customerRequest('/catalog/1/' + item.itemId)
    detail.value = current
    choices.value = current.flavors.map(group => {
      const previous = item.flavors.find(value => value.name === group.name)?.option
      return group.options.includes(previous) ? previous : undefined
    })
  } catch (failure) {
    if (failure.status === 401) emit('session-expired')
    else choiceError.value = failure.status === 404 ? '这道菜已停售，请返回购物车刷新。' : failure.message
  } finally {
    choosing.value = false
  }
}

async function saveChoices() {
  if (choosing.value || !detail.value) return
  choosing.value = true
  choiceError.value = ''
  try {
    const result = await customerRequest('/cart/items/' + reselectItem.value.id + '/flavors', {
      method: 'PUT', body: {
        flavors: detail.value.flavors.map((group,index) => ({ name: group.name, option: choices.value[index] })),
      },
    })
    showCart(result)
    reselectDialog.value.close()
    notice.value = '口味已更新，原有份数已保留。'
  } catch (failure) {
    if (failure.status === 401) emit('session-expired')
    else choiceError.value = failure.message
  } finally {
    choosing.value = false
  }
}

function quantity(item, delta) {
  return update('/cart/items/' + item.id, { method: 'PUT', body: { quantity: item.quantity + delta } })
}

function remove(item) {
  return update('/cart/items/' + item.id, { method: 'DELETE' })
}

function clear() {
  clearDialog.value.close()
  return update('/cart', { method: 'DELETE' })
}

onMounted(update)
</script>

<template>
  <section class="shop-commerce" aria-labelledby="cart-heading">
    <div class="shop-section-heading">
      <h1 id="cart-heading">购物车</h1>
      <button v-if="cart?.items.length" class="quiet-button" type="button" :disabled="busy"
              @click="clearDialog.showModal()">清空购物车</button>
    </div>

    <p v-if="error" class="notice" role="alert">{{ error }}</p>
    <p v-if="notice" class="shop-success" role="status">{{ notice }}</p>
    <p v-if="busy && !cart" class="shop-empty" role="status">正在加载购物车…</p>
    <div v-else-if="!cart && error" class="shop-empty">
      <button class="quiet-button" type="button" @click="update()">重试</button>
    </div>
    <div v-else-if="cart && !cart.items.length" class="shop-empty">
      <h2>购物车是空的</h2>
      <a class="shop-action primary-button" href="#menu">去看菜单</a>
    </div>
    <template v-else-if="cart?.items.length">
      <ul class="shop-cart-list">
        <li v-for="item in cart.items" :key="item.id" class="shop-cart-item">
          <img v-if="item.available || item.issue === 'FLAVOR_CHANGED'" :src="catalogImage({type:item.type,id:item.itemId})" alt="" loading="lazy" />
          <div class="shop-cart-description">
            <strong>{{ item.name }}</strong>
            <span v-if="item.flavors.length" class="shop-muted">{{ showFlavors(item.flavors) }}</span>
            <MenuMealContents :contents="item.mealContents" />
            <span v-if="item.available" class="shop-price">{{ formatMoney(item.subtotal) }}</span>
            <span v-else class="shop-danger">{{ item.issue === 'FLAVOR_CHANGED' ? '口味已变更' : '暂不可结算' }}</span>
            <button v-if="item.issue === 'FLAVOR_CHANGED'" class="shop-inline-button shop-reselect"
                    type="button" :disabled="busy" :aria-label="'重新选择' + item.name + '的口味'"
                    @click="openReselect(item)">重新选口味</button>
          </div>
          <div class="shop-cart-controls">
            <div class="shop-stepper" :aria-label="item.name + '数量'">
              <button type="button" :disabled="busy" :aria-label="'减少' + item.name" @click="quantity(item, -1)">−</button>
              <span>{{ item.quantity }}</span>
              <button type="button" :disabled="busy || !item.available || item.quantity >= 99"
                      :aria-label="'增加' + item.name" @click="quantity(item, 1)">+</button>
            </div>
            <button class="shop-inline-button" type="button" :disabled="busy" :aria-label="'移除' + item.name"
                    @click="remove(item)">移除</button>
          </div>
        </li>
      </ul>
      <div class="shop-checkout-bar">
        <p>商品合计 <strong class="shop-price">{{ formatMoney(cart.total) }}</strong></p>
        <p v-if="hasUnavailable" class="shop-muted">请先重新选口味或移除不可结算商品。</p>
        <a v-else-if="!busy" class="shop-action primary-button" href="#checkout">去结算</a>
        <button v-else type="button" class="primary-button" disabled>去结算</button>
      </div>
    </template>

    <dialog ref="reselectDialog" class="shop-dialog" aria-labelledby="cart-flavor-heading"
            @cancel="choosing && $event.preventDefault()">
      <h2 id="cart-flavor-heading">重新选择口味</h2>
      <p>{{ reselectItem?.name }}</p>
      <p v-if="choosing && !detail" class="shop-muted" role="status">正在获取最新口味…</p>
      <ShopFlavorChoices v-if="detail" v-model="choices" :groups="detail.flavors" :disabled="choosing" />
      <p v-if="choiceError" class="notice" role="alert">{{ choiceError }}</p>
      <div class="shop-dialog-actions">
        <button class="quiet-button" type="button" autofocus :disabled="choosing" @click="reselectDialog.close()">取消</button>
        <button v-if="detail" class="primary-button" type="button"
                :disabled="choosing || detail.flavors.some((_,index) => !choices[index])" @click="saveChoices">
          {{ choosing ? '保存中…' : '保存口味' }}
        </button>
      </div>
    </dialog>

    <dialog ref="clearDialog" class="shop-dialog" aria-labelledby="clear-cart-heading">
      <h2 id="clear-cart-heading">清空购物车</h2>
      <p>确认移除购物车里的所有商品？</p>
      <div class="shop-dialog-actions">
        <button class="quiet-button" type="button" autofocus @click="clearDialog.close()">取消</button>
        <button class="primary-button" type="button" @click="clear">确认清空</button>
      </div>
    </dialog>
  </section>
</template>
