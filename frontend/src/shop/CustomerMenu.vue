<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { catalogImage, customerRequest } from './customerApi.js'
import ShopFlavorChoices from './ShopFlavorChoices.vue'

const emit = defineEmits(['session-expired','cart-updated'])
const categories = ref([])
const selected = ref(null)
const loading = ref(true)
const error = ref('')
const catalogReady = ref(false)
let catalogRequest
const detailDialog = ref(null)
const detail = ref(null)
const detailError = ref('')
const choices = ref([])
const adding = ref(false)
const addedNotice = ref('')
const detailLoading = ref(false)
let detailRequest
const money = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' })

const items = computed(() => categories.value
  .filter(category => selected.value === null || selected.value === category.id)
  .flatMap(category => category.items.map(item => ({ ...item, categoryName: category.name }))))

async function refresh() {
  catalogRequest?.abort()
  const request = new AbortController()
  catalogRequest = request
  loading.value = true
  error.value = ''
  try {
    const result = await customerRequest('/catalog', { signal: request.signal })
    if (request.signal.aborted) return
    categories.value = result
    catalogReady.value = true
    if (!result.some(category => category.id === selected.value)) selected.value = null
  } catch (failure) {
    if (!request.signal.aborted) {
      if (failure.status === 401) emit('session-expired')
      else error.value = '菜单加载失败，请重试。'
    }
  } finally {
    if (catalogRequest === request) loading.value = false
  }
}

async function openDetail(item) {
  detailRequest?.abort()
  const request = new AbortController()
  detailRequest = request
  detail.value = null
  choices.value = []
  addedNotice.value = ''
  detailError.value = ''
  detailLoading.value = true
  detailDialog.value.showModal()
  try {
    const path = '/catalog/' + item.type + '/' + item.id
    const result = await customerRequest(path, { signal: request.signal })
    if (!request.signal.aborted) detail.value = result
  } catch (failure) {
    if (!request.signal.aborted) {
      if (failure.status === 401) {
        detailDialog.value.close()
        emit('session-expired')
        return
      }
      detailError.value = failure.status === 404 ? '这份商品已停售。' : '详情加载失败，请重试。'
      if (failure.status === 404) refresh()
    }
  } finally {
    if (detailRequest === request) detailLoading.value = false
  }
}

async function addToCart() {
  if (!detail.value || adding.value) return
  adding.value = true
  detailError.value = ''
  const current = detail.value
  try {
    const cart = await customerRequest('/cart/items', { method: 'POST', body: {
      type: current.item.type, itemId: current.item.id,
      flavors: current.flavors.map((group, index) => ({ name: group.name, option: choices.value[index] })),
    } })
    emit('cart-updated', cart.quantity)
    detailDialog.value.close()
    addedNotice.value = '「' + current.item.name + '」已加入购物车。'
  } catch (failure) {
    if (failure.status === 401) {
      detailDialog.value.close()
      emit('session-expired')
    } else {
      detailError.value = failure.message
    }
  } finally {
    adding.value = false
  }
}

function closeDetail() {
  detailRequest?.abort()
}

onMounted(() => {
  refresh()
  window.addEventListener('focus', refresh)
})
onUnmounted(() => {
  window.removeEventListener('focus', refresh)
  catalogRequest?.abort()
  closeDetail()
})
</script>

<template>
  <section class="shop-menu" aria-labelledby="shop-menu-heading">
    <div class="shop-menu-hero">
      <div class="shop-hero-title">
        <h1 id="shop-menu-heading">今日菜单</h1>
        <button type="button" class="quiet-button" :disabled="loading" @click="refresh">刷新菜单</button>
      </div>
    </div>

    <p v-if="error" class="notice shop-menu-notice" role="alert">{{ error }}</p>
    <p v-if="addedNotice" class="shop-success shop-added-to-cart" role="status">{{ addedNotice }}</p>
    <p v-if="loading && !catalogReady" class="shop-empty" role="status">正在加载菜单…</p>
    <div v-else-if="catalogReady && !categories.length" class="shop-empty">
      <h2>店铺暂未启售商品</h2>
      <p>有在售菜品或套餐时会显示在这里。</p>
    </div>
    <template v-else-if="catalogReady">
      <nav class="shop-category-strip" aria-label="菜单分类">
        <button type="button" :aria-pressed="selected === null" @click="selected = null">全部</button>
        <button v-for="category in categories" :key="category.id" type="button"
                :aria-pressed="selected === category.id" @click="selected = category.id">{{ category.name }}</button>
      </nav>
      <ul class="shop-product-grid">
        <li v-for="item in items" :key="item.type + '-' + item.id">
          <button class="shop-product" type="button" :aria-label="'查看' + item.name" @click="openDetail(item)">
            <img :src="catalogImage(item)" alt="" loading="lazy" />
            <span class="shop-product-copy">
              <span class="shop-product-category">{{ item.categoryName }}</span>
              <strong>{{ item.name }}</strong>
              <span v-if="item.description" class="shop-product-description">{{ item.description }}</span>
              <span class="shop-price">{{ money.format(item.price) }}</span>
            </span>
          </button>
        </li>
      </ul>
    </template>
  </section>

  <dialog ref="detailDialog" class="shop-dialog shop-product-detail" aria-labelledby="shop-detail-heading"
          @close="closeDetail" @cancel="adding && $event.preventDefault()">
    <h2 id="shop-detail-heading">{{ detail?.item.name ?? '商品详情' }}</h2>
    <p v-if="detailLoading" class="shop-muted" role="status">正在加载详情…</p>
    <p v-else-if="detailError && !detail" class="notice" role="alert">{{ detailError }}</p>
    <template v-else-if="detail">
      <img class="shop-detail-image" :src="catalogImage(detail.item)" alt="" />
      <p class="shop-price">{{ money.format(detail.item.price) }}</p>
      <p v-if="detail.item.description">{{ detail.item.description }}</p>
      <ShopFlavorChoices v-model="choices" :groups="detail.flavors" :disabled="adding" />
      <section v-if="detail.contents.length" class="shop-product-options" aria-label="套餐内容">
        <h3>套餐内容</h3>
        <p v-for="dish in detail.contents" :key="dish.name">{{ dish.name }} × {{ dish.quantity }}</p>
      </section>
    </template>
    <p v-if="detail && detailError" class="notice" role="alert">{{ detailError }}</p>
    <div class="shop-dialog-actions">
      <button class="quiet-button" type="button" :disabled="adding" autofocus @click="detailDialog.close()">关闭</button>
      <button v-if="detail" class="primary-button" type="button"
              :disabled="adding || detail.flavors.some((_, index) => !choices[index])" @click="addToCart">
        {{ adding ? '添加中…' : '加入购物车' }}
      </button>
    </div>
  </dialog>
</template>
