<script setup>
import { onUnmounted, ref } from 'vue'
import cover from '../assets/bento-editorial.jpg'
import { customerRequest } from './customerApi.js'

const emit = defineEmits(['signed-in'])
const phone = ref('')
const code = ref('')
const demoCode = ref('')
const issuedTo = ref('')
const remaining = ref(0)
const error = ref('')
const busy = ref(false)
const sending = ref(false)
let timer

function cooldown(seconds) {
  clearInterval(timer)
  const until = Date.now() + seconds * 1000
  const update = () => {
    remaining.value = Math.max(0, Math.ceil((until - Date.now()) / 1000))
    if (!remaining.value) clearInterval(timer)
  }
  update()
  if (remaining.value) timer = setInterval(update, 1000)
}

async function getCode() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    error.value = '请输入有效的 11 位手机号。'
    return
  }
  sending.value = true
  error.value = ''
  const requestedPhone = phone.value
  try {
    const response = await customerRequest('/auth/code', { method: 'POST', body: { phone: requestedPhone } })
    issuedTo.value = requestedPhone
    demoCode.value = response.demoCode || ''
    code.value = ''
    cooldown(response.retryAfterSeconds)
  } catch (failure) {
    error.value = failure.message
  } finally {
    sending.value = false
  }
}

async function signIn() {
  busy.value = true
  error.value = ''
  try {
    const result = await customerRequest('/auth/login', { method: 'POST', body: { phone: phone.value, code: code.value } })
    demoCode.value = ''
    code.value = ''
    cooldown(0)
    emit('signed-in', result)
  } catch (failure) {
    error.value = failure.message
  } finally {
    busy.value = false
  }
}

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <section class="shop-login" aria-labelledby="customer-login-heading">
    <div class="shop-login-panel">
      <h1 id="customer-login-heading">手机号登录</h1>
      <p class="shop-muted">验证手机号后进入校园便当。</p>
      <form class="shop-form" @submit.prevent="signIn">
        <label class="shop-field">
          <span>手机号</span>
          <input v-model.trim="phone" type="tel" name="phone" inputmode="numeric" autocomplete="tel-national"
                 maxlength="11" pattern="1[3-9][0-9]{9}" placeholder="11 位手机号" required autofocus
                 :disabled="busy || sending" />
        </label>
        <div class="shop-code-field">
          <label class="shop-field">
            <span>验证码</span>
            <input v-model.trim="code" type="text" name="code" inputmode="numeric" autocomplete="one-time-code"
                   maxlength="6" pattern="[0-9]{6}" placeholder="6 位验证码" required :disabled="busy" />
          </label>
          <button class="quiet-button" type="button" :disabled="busy || sending || remaining > 0" @click="getCode">
            {{ remaining ? remaining + ' 秒后重试' : sending ? '获取中…' : '获取验证码' }}
          </button>
        </div>
        <p v-if="demoCode && phone === issuedTo" class="shop-demo-code" role="status">
          本地演示验证码 <strong>{{ demoCode }}</strong>
        </p>
        <p v-if="error" class="notice" role="alert">{{ error }}</p>
        <button class="primary-button shop-login-submit" type="submit" :disabled="busy || sending">{{ busy ? '登录中…' : '登录并进入' }}</button>
      </form>
    </div>
    <img class="shop-login-photo" :src="cover" alt="" width="1122" height="1402" />
  </section>
</template>
