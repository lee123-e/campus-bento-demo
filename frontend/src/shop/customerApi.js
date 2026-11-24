import { sessionFetch } from '../sessionFetch.js'

export async function customerRequest(path, options) {
  const response = await sessionFetch('customer', path, options)
  if (!response.ok) {
    const data = await response.json().catch(() => null)
    const message = data?.detail && /[\u4e00-\u9fff]/.test(data.detail) ? data.detail : '操作未完成，请检查输入后重试。'
    throw Object.assign(new Error(message), { status: response.status })
  }
  if (response.status !== 204) return response.json()
}

export function catalogImage(item) {
  return '/api/customer/catalog/' + item.type + '/' + item.id + '/image'
}
