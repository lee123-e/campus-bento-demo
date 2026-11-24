import { createApp } from 'vue'
import './style.css'

const customer = /^\/shop(?:\/|$)/.test(location.pathname)
const legacyAdmin = location.pathname === '/' && /^#(?:categories|dishes|setmeals|orders)$/.test(location.hash)
const admin = /^\/admin(?:\/|$)/.test(location.pathname) || legacyAdmin

let view
if (customer) {
  view = await import('./shop/CustomerApp.vue')
  document.title = '校园便当 · 今日菜单'
} else if (admin) {
  view = await import('./App.vue')
  document.title = '校园便当 · 店铺管理'
} else {
  view = await import('./EntryApp.vue')
  document.title = '校园便当 · 选择入口'
}
createApp(view.default).mount('#app')
