export async function sessionFetch(scope, path, { method = 'GET', body, signal } = {}) {
  const headers = new Headers()
  const base = '/api/' + scope
  if (method !== 'GET') {
    const response = await fetch(base + '/csrf', { cache: 'no-store', signal })
    if (!response.ok) {
      throw new Error('无法连接店铺服务，请稍后重试。')
    }
    const { headerName, token } = await response.json()
    headers.set(headerName, token)
  }

  if (body && !(body instanceof URLSearchParams) && !(body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(body)
  }

  return fetch(base + path, { method, headers, body, signal, cache: 'no-store' })
}
