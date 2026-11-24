import { sessionFetch } from './sessionFetch.js'

export function adminFetch(path, options) {
  return sessionFetch('admin', path, options)
}

export async function requestAdminData(path, options) {
  const response = await adminFetch(path, options)
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw Object.assign(new Error(problem?.detail || '操作未完成，请检查输入后重试。'), { status: response.status })
  }
  if (response.status !== 204) return response.json()
}
