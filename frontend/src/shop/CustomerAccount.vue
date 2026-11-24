<script setup>
import { computed, ref } from 'vue'
import CustomerAddresses from './CustomerAddresses.vue'
import CustomerAvatar from './CustomerAvatar.vue'
import { customerRequest } from './customerApi.js'

const props = defineProps({
  customer: { type: Object, required: true },
  section: { type: String, required: true },
  signingOut: Boolean,
})
const emit = defineEmits(['profile-updated', 'sign-out', 'session-expired'])
const nickname = ref(props.customer.nickname)
const maskedPhone = computed(() => props.customer.phone.slice(0, 3) + '****' + props.customer.phone.slice(-4))
const displayName = computed(() => props.customer.nickname || '顾客 ' + props.customer.phone.slice(-4))
const saving = ref(false)
const error = ref('')
const notice = ref('')

async function saveProfile() {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const profile = await customerRequest('/profile', { method: 'PUT', body: { nickname: nickname.value } })
    emit('profile-updated', profile)
    nickname.value = profile.nickname
    notice.value = '个人资料已保存。'
  } catch (failure) {
    if (failure.status === 401) emit('session-expired')
    else error.value = failure.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="shop-personal">
    <a v-if="section !== 'me'" href="#me" class="shop-account-back">返回我的</a>
    <CustomerAddresses v-if="section === 'addresses'" :customer-phone="customer.phone" @session-expired="emit('session-expired')" />
    <section v-else-if="section === 'profile'" class="shop-profile" aria-labelledby="shop-profile-heading">
      <h1 id="shop-profile-heading">个人资料</h1>
      <div class="shop-profile-identity">
        <CustomerAvatar :customer="customer" />
        <span>{{ displayName }}</span>
      </div>
      <form class="shop-form" @submit.prevent="saveProfile">
        <label class="shop-field">
          <span>昵称</span>
          <input v-model.trim="nickname" maxlength="24" autocomplete="nickname" required :disabled="saving" />
        </label>
        <div class="shop-profile-phone">
          <strong>登录手机号</strong>
          <span>{{ maskedPhone }}</span>
        </div>
        <p v-if="error" role="alert" class="notice">{{ error }}</p>
        <p v-if="notice" role="status" class="shop-success">{{ notice }}</p>
        <button class="primary-button shop-profile-save" type="submit" :disabled="saving || !nickname">
          {{ saving ? '保存中…' : '保存个人资料' }}
        </button>
      </form>
    </section>
    <section v-else class="shop-profile" aria-labelledby="shop-personal-heading">
      <h1 id="shop-personal-heading">我的</h1>
      <div class="shop-profile-identity">
        <CustomerAvatar :customer="customer" />
        <div>
          <strong>{{ displayName }}</strong>
          <p class="shop-muted">{{ maskedPhone }}</p>
        </div>
      </div>
      <nav class="shop-profile-links" aria-label="账户管理">
        <a href="#profile"><strong>个人资料</strong><span>编辑昵称</span></a>
        <a href="#orders"><strong>我的订单</strong><span>查看订单和支付状态</span></a>
        <a href="#addresses"><strong>收货地址</strong><span>管理配送地址</span></a>
      </nav>
      <button class="quiet-button" type="button" :disabled="signingOut" @click="emit('sign-out')">
        {{ signingOut ? '退出中…' : '退出登录' }}
      </button>
    </section>
  </div>
</template>
