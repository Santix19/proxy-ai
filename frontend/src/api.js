const BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')

function getUserId() {
  let id = localStorage.getItem('userId')
  if (!id) {
    id = 'user-' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem('userId', id)
  }
  return id
}

export const userId = getUserId()

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    ...options,
    headers: { 'Content-Type': 'application/json', 'X-User-Id': userId, ...(options.headers || {}) },
  })
  let data = null
  try { data = await res.json() } catch { /* sin cuerpo */ }
  if (!res.ok) {
    const err = new Error(data?.message || `Error ${res.status}`)
    err.status = res.status
    err.retryAfter = Number(res.headers.get('Retry-After')) || data?.retryAfterSeconds || null
    throw err
  }
  return data
}

export const api = {
  generate: (prompt) => request('/api/ai/generate', { method: 'POST', body: JSON.stringify({ prompt }) }),
  status: () => request('/api/quota/status'),
  history: () => request('/api/quota/history'),
  upgrade: (plan) => request('/api/quota/upgrade', { method: 'POST', body: JSON.stringify({ plan }) }),
  exhaust: () => request('/api/quota/demo/exhaust', { method: 'POST' }),
}

// Misma heurística que el backend: ~4 caracteres por token
export const estimateTokens = (text) => (text && text.trim() ? Math.ceil(text.length / 4) : 0)
