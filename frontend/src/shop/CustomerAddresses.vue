<script setup>
import { onMounted, ref } from 'vue'
import ShopAddressEditor from './ShopAddressEditor.vue'
import { customerRequest } from './customerApi.js'

defineProps({ customerPhone: { type: String, required: true } })
const emit = defineEmits(['session-expired'])
const addresses = ref([])
const loading = ref(true)
const error = ref('')
const formError = ref('')
const status = ref('')
const saving = ref(false)
const editor = ref(null)
const confirmation = ref(null)
const deletion = ref(null)

function showError(failure, destination) {
  if (failure.status === 401) {
    confirmation.value?.close()
    emit('session-expired')
  } else {
    destination.value = failure.message || '操作失败，请稍后重试。'
  }
}

async function loadAddresses() {
  loading.value = true
  error.value = ''
  try {
    addresses.value = await customerRequest('/addresses')
  } catch (failure) {
    showError(failure, error)
  } finally {
    loading.value = false
  }
}

function openEditor(address) {
  editor.value.open(address)
}

function onSaved(result) {
  status.value = result.edited ? '收货地址已更新。' : '收货地址已添加。'
  loadAddresses()
}

async function perform(path, options, message, dialog) {
  if (saving.value) return
  saving.value = true
  formError.value = ''
  error.value = ''
  status.value = ''
  try {
    await customerRequest(path, options)
    dialog?.close()
    status.value = message
    await loadAddresses()
  } catch (failure) {
    showError(failure, dialog ? formError : error)
  } finally {
    saving.value = false
  }
}

function setDefault(address) {
  return perform('/addresses/' + address.id + '/default', { method: 'PUT' }, '已设置为默认地址。')
}

function confirmDelete(address) {
  deletion.value = address
  formError.value = ''
  confirmation.value.showModal()
}

function deleteAddress() {
  return perform('/addresses/' + deletion.value.id, { method: 'DELETE' }, '收货地址已删除。', confirmation.value)
}

function preventClose(event) {
  if (saving.value) event.preventDefault()
}

onMounted(loadAddresses)
</script>

<template>
  <section class="shop-addresses" aria-labelledby="shop-address-heading">
    <div class="shop-section-heading">
      <h1 id="shop-address-heading">收货地址</h1>
      <button class="primary-button" type="button" :disabled="saving || loading" @click="openEditor()">添加收货地址</button>
    </div>
    <p v-if="status" class="shop-success" role="status">{{ status }}</p>
    <div v-if="error" class="shop-menu-notice">
      <p class="notice" role="alert">{{ error }}</p>
      <button class="quiet-button" type="button" @click="loadAddresses">重试</button>
    </div>

    <p v-if="loading" class="shop-empty" role="status">正在加载地址…</p>
    <div v-else-if="!addresses.length && !error" class="shop-empty">
      <h2>还没有收货地址</h2>
      <p>添加地址后，可以在下单时使用。</p>
    </div>
    <ul v-else-if="addresses.length" class="shop-address-list">
      <li v-for="address in addresses" :key="address.id" class="shop-address-card">
        <div class="shop-address-line">
          <span v-if="address.defaultAddress" class="shop-default-label">默认地址</span>
          <span v-if="address.label" class="shop-address-label">{{ address.label }}</span>
        </div>
        <h2>{{ address.deliveryAddress }}</h2>
        <p class="shop-address-line">
          <span>{{ address.contactName }}</span><span>{{ address.phone }}</span>
        </p>
        <div class="shop-address-actions">
          <button v-if="!address.defaultAddress" class="quiet-button" type="button" :disabled="saving"
                  :aria-label="'设为默认地址 ' + address.deliveryAddress" @click="setDefault(address)">设为默认</button>
          <button class="quiet-button" type="button" :disabled="saving" :aria-label="'编辑 ' + address.deliveryAddress"
                  @click="openEditor(address)">编辑</button>
          <button class="quiet-button shop-danger" type="button" :disabled="saving" :aria-label="'删除 ' + address.deliveryAddress"
                  @click="confirmDelete(address)">删除</button>
        </div>
      </li>
    </ul>

    <ShopAddressEditor ref="editor" :customer-phone="customerPhone" @saved="onSaved"
                       @session-expired="emit('session-expired')" />

    <dialog ref="confirmation" class="shop-dialog" aria-labelledby="address-deletion-heading" @cancel="preventClose">
      <h2 id="address-deletion-heading">删除地址</h2>
      <p>确认删除「{{ deletion?.deliveryAddress }}」？</p>
      <p v-if="formError" class="notice" role="alert">{{ formError }}</p>
      <div class="shop-dialog-actions">
        <button class="quiet-button" type="button" autofocus :disabled="saving" @click="confirmation.close()">取消</button>
        <button class="primary-button" type="button" :disabled="saving" @click="deleteAddress">确认删除</button>
      </div>
    </dialog>
  </section>
</template>
