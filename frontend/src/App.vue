<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { adminFetch } from './adminApi.js'
import CategoryWorkspace from './categories/CategoryWorkspace.vue'
import DishWorkspace from './dishes/DishWorkspace.vue'
import SetmealWorkspace from './setmeals/SetmealWorkspace.vue'
import AdminOrderWorkspace from './orders/AdminOrderWorkspace.vue'

const employee = ref(null)
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const invalidCredentials = ref(false)
const error = ref('')
const loading = ref(true)
const pending = ref(false)
function currentWorkspace() {
  const section = location.hash.slice(1)
  return ['dishes', 'setmeals', 'orders'].includes(section) ? section : 'categories'
}

const workspace = ref(currentWorkspace())

function syncWorkspace() {
  workspace.value = currentWorkspace()
}

function expireSession() {
  employee.value = null
  error.value = '登录已过期，请重新登录。'
}

async function refreshSession() {
  const response = await adminFetch('/session')
  if (response.status === 401) {
    employee.value = null
    return
  }
  if (!response.ok) {
    throw new Error('无法连接店铺服务，请稍后重试。')
  }
  employee.value = await response.json()
}

function clearCredentialsError() {
  if (invalidCredentials.value) {
    invalidCredentials.value = false
    error.value = ''
  }
}

async function signIn() {
  invalidCredentials.value = false
  showPassword.value = false
  pending.value = true
  error.value = ''
  try {
    const response = await adminFetch('/login', { method: 'POST', body: new URLSearchParams({
      username: username.value,
      password: password.value,
    }) })
    if (response.status === 401) {
      invalidCredentials.value = true
      error.value = '登录失败，请检查账号和密码。'
      return
    }
    if (!response.ok) {
      throw new Error('登录未完成，请稍后重试。')
    }
    await refreshSession()
  } catch (failure) {
    error.value = failure.message
  } finally {
    password.value = ''
    pending.value = false
  }
}

async function signOut() {
  pending.value = true
  error.value = ''
  try {
    const response = await adminFetch('/logout', { method: 'POST' })
    if (!response.ok) {
      throw new Error('退出未完成，请稍后重试。')
    }
    employee.value = null
  } catch (failure) {
    error.value = failure.message
  } finally {
    pending.value = false
  }
}

onMounted(async () => {
  window.addEventListener('hashchange', syncWorkspace)
  try {
    await refreshSession()
  } catch {
    error.value = '无法连接店铺服务，请确认后端已启动。'
  } finally {
    loading.value = false
  }
})
onUnmounted(() => window.removeEventListener('hashchange', syncWorkspace))
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <span class="wordmark">
        <span class="brand-mark" aria-hidden="true"></span>
        校园便当
      </span>
      <div v-if="employee" class="employee-controls">
        <span class="header-context">{{ employee.displayName }}</span>
        <button class="quiet-button" type="button" :disabled="pending" @click="signOut">
          {{ pending ? '退出中…' : '退出登录' }}
        </button>
      </div>
      <a v-else class="header-context entry-home-link" href="/">返回首页</a>
    </header>

    <main v-if="employee" class="workspace-body">
      <p v-if="error" role="alert" class="notice">{{ error }}</p>
      <nav class="workspace-navigation" aria-label="店铺管理">
        <a href="#categories" :aria-current="workspace === 'categories' ? 'page' : undefined">分类管理</a>
        <a href="#dishes" :aria-current="workspace === 'dishes' ? 'page' : undefined">菜品管理</a>
        <a href="#setmeals" :aria-current="workspace === 'setmeals' ? 'page' : undefined">套餐管理</a>
        <a href="#orders" :aria-current="workspace === 'orders' ? 'page' : undefined">订单管理</a>
      </nav>
      <CategoryWorkspace v-if="workspace === 'categories'" @session-expired="expireSession" />
      <DishWorkspace v-else-if="workspace === 'dishes'" @session-expired="expireSession" />
      <SetmealWorkspace v-else-if="workspace === 'setmeals'" @session-expired="expireSession" />
      <AdminOrderWorkspace v-else @session-expired="expireSession" />
    </main>

    <main v-else class="sign-in">
      <section class="sign-in-panel" aria-labelledby="login-heading">
        <div class="sign-in-content">
          <h1 id="login-heading">员工登录</h1>
          <p class="description">请使用店铺分配的员工账号。</p>
          <p v-if="loading" class="session-loading" role="status">正在检查登录状态…</p>
          <form v-else :aria-busy="pending" @submit.prevent="signIn">
            <div class="field">
              <label for="username">账号</label>
              <input id="username" v-model.trim="username" name="username"
                     autocomplete="username" autocapitalize="none" spellcheck="false"
                     maxlength="64" required :aria-invalid="invalidCredentials || undefined"
                     :aria-describedby="invalidCredentials ? 'login-error' : undefined"
                     @input="clearCredentialsError" />
            </div>
            <div class="field">
              <label for="password">密码</label>
              <div class="password-control">
                <input id="password" v-model="password" name="password"
                       :type="showPassword ? 'text' : 'password'"
                       autocomplete="current-password" autocapitalize="none" spellcheck="false"
                       required :aria-invalid="invalidCredentials || undefined"
                       :aria-describedby="invalidCredentials ? 'login-error' : undefined"
                       @input="clearCredentialsError" />
                <button class="password-toggle" type="button" aria-controls="password"
                        :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                        :disabled="pending" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>
            <p v-if="error" id="login-error" role="alert" class="notice">{{ error }}</p>
            <button class="primary-button" type="submit" :disabled="pending">
              {{ pending ? '登录中…' : '登录' }}
            </button>
          </form>
        </div>
      </section>

      <div class="food-photo" aria-hidden="true"></div>
    </main>
  </div>
</template>
