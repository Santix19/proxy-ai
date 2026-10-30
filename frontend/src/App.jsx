import { useCallback, useEffect, useRef, useState } from 'react'
import { api, estimateTokens, userId } from './api.js'

const PLAN_COLORS = { FREE: '#64748b', PRO: '#2563eb', ENTERPRISE: '#9333ea' }
const fmt = (n) => Number(n).toLocaleString('es-CO')

function PlanBadge({ plan }) {
  return <span className="badge" style={{ background: PLAN_COLORS[plan] || '#64748b' }}>{plan}</span>
}

function QuotaBar({ status }) {
  if (!status) return null
  if (status.unlimited) {
    return (
      <div className="card">
        <div className="row"><strong>Cuota de tokens</strong><span>Ilimitada · usados {fmt(status.tokensUsed)}</span></div>
        <div className="bar"><div className="bar-fill" style={{ width: '100%', background: PLAN_COLORS.ENTERPRISE }} /></div>
      </div>
    )
  }
  const pct = Math.min(100, (status.tokensUsed / status.tokensLimit) * 100)
  const color = pct >= 90 ? '#dc2626' : pct >= 70 ? '#f59e0b' : '#16a34a'
  return (
    <div className="card">
      <div className="row">
        <strong>Cuota de tokens</strong>
        <span>{fmt(status.tokensUsed)} / {fmt(status.tokensLimit)}</span>
      </div>
      <div className="bar"><div className="bar-fill" style={{ width: pct + '%', background: color }} /></div>
      <div className="row small">
        <span>Restantes: {fmt(status.tokensRemaining)}</span>
        <span>Se reinicia el {status.resetDate}</span>
      </div>
    </div>
  )
}

function RateCounter({ status, countdown }) {
  if (!status) return null
  const limit = status.requestsLimit
  return (
    <div className="card">
      <div className="row">
        <strong>Requests este minuto</strong>
        <span>{status.requestsThisMinute} / {limit < 0 ? '∞' : limit}</span>
      </div>
      <div className="row small"><span>Reset en</span><span className="mono">{countdown}s</span></div>
    </div>
  )
}

function HistoryChart({ days }) {
  const max = Math.max(1, ...days.map((d) => d.tokens))
  return (
    <div className="card">
      <strong>Uso diario (últimos 7 días)</strong>
      <div className="chart">
        {days.map((d) => (
          <div className="chart-col" key={d.date} title={`${d.date}: ${fmt(d.tokens)} tokens, ${d.requests} requests`}>
            <span className="chart-val">{d.tokens ? fmt(d.tokens) : ''}</span>
            <div className="chart-bar" style={{ height: Math.max(2, (d.tokens / max) * 100) + '%' }} />
            <span className="chart-lbl">{d.date.slice(5)}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function UpgradeModal({ onClose, onDone, currentPlan }) {
  const [target, setTarget] = useState(currentPlan === 'FREE' ? 'PRO' : 'ENTERPRISE')
  const [card, setCard] = useState('4242 4242 4242 4242')
  const [paying, setPaying] = useState(false)
  const [error, setError] = useState('')

  const pay = async () => {
    setPaying(true); setError('')
    try {
      await new Promise((r) => setTimeout(r, 1500)) // simulación de pago
      await api.upgrade(target)
      onDone()
    } catch (e) {
      setError(e.message)
    } finally {
      setPaying(false)
    }
  }

  const options = ['PRO', 'ENTERPRISE'].filter((p) => p !== currentPlan && (currentPlan === 'FREE' || p === 'ENTERPRISE'))

  return (
    <div className="overlay">
      <div className="modal">
        <h2>Cuota mensual agotada</h2>
        <p>Actualiza tu plan para seguir generando texto. (Pago simulado, no se realiza ningún cobro real.)</p>
        <div className="plans">
          {options.map((p) => (
            <label key={p} className={'plan-opt' + (target === p ? ' active' : '')}>
              <input type="radio" checked={target === p} onChange={() => setTarget(p)} />
              <PlanBadge plan={p} />
              <span>{p === 'PRO' ? '60 req/min · 500k tokens/mes' : 'Ilimitado'}</span>
            </label>
          ))}
        </div>
        <label className="field">Tarjeta (simulada)
          <input value={card} onChange={(e) => setCard(e.target.value)} />
        </label>
        {error && <div className="error">{error}</div>}
        <div className="actions">
          <button className="secondary" onClick={onClose} disabled={paying}>Cancelar</button>
          <button onClick={pay} disabled={paying}>{paying ? 'Procesando pago…' : `Pagar y cambiar a ${target}`}</button>
        </div>
      </div>
    </div>
  )
}

export default function App() {
  const [status, setStatus] = useState(null)
  const [history, setHistory] = useState([])
  const [messages, setMessages] = useState([])
  const [prompt, setPrompt] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [countdown, setCountdown] = useState(0)
  const [blockedUntil, setBlockedUntil] = useState(0) // epoch ms (rate limit)
  const [showUpgrade, setShowUpgrade] = useState(false)
  const bottom = useRef(null)

  const refresh = useCallback(async () => {
    try {
      const [s, h] = await Promise.all([api.status(), api.history()])
      setStatus(s)
      setHistory(h.days)
      setCountdown(s.secondsToRateReset)
    } catch (e) {
      setError('No se pudo conectar con el backend: ' + e.message)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])
  useEffect(() => { bottom.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, loading])

  // Contador regresivo hacia el reset del rate limit (cada minuto)
  useEffect(() => {
    const t = setInterval(() => {
      setCountdown((c) => {
        if (c <= 1) { refresh(); return 60 }
        return c - 1
      })
    }, 1000)
    return () => clearInterval(t)
  }, [refresh])

  const rateLimited = status && status.requestsLimit >= 0 && status.requestsThisMinute >= status.requestsLimit
  const blocked = rateLimited || Date.now() < blockedUntil
  const quotaOut = status && !status.unlimited && status.tokensRemaining <= 0
  const estimate = estimateTokens(prompt)

  // Si la cuota mensual se agota, se abre automáticamente el modal de upgrade
  useEffect(() => { if (quotaOut) setShowUpgrade(true) }, [quotaOut])

  const exhaustForDemo = async () => {
    try { setStatus(await api.exhaust()) } catch (e) { setError(e.message) }
  }

  const send = async () => {
    const text = prompt.trim()
    if (!text || loading || blocked) return
    setError('')
    setLoading(true)
    setMessages((m) => [...m, { role: 'user', text }])
    setPrompt('')
    try {
      const r = await api.generate(text)
      setMessages((m) => [...m, { role: 'ai', text: r.text, tokens: r.totalTokens }])
      setStatus(r.status)
      setCountdown(r.status.secondsToRateReset)
      api.history().then((h) => setHistory(h.days))
    } catch (e) {
      if (e.status === 429) {
        const secs = e.retryAfter || countdown || 60
        setBlockedUntil(Date.now() + secs * 1000)
        setCountdown(secs)
        setError(`Rate limit alcanzado. Podrás volver a enviar en ${secs}s.`)
      } else if (e.status === 402) {
        setShowUpgrade(true)
        setError(e.message)
      } else {
        setError(e.message)
      }
      await refresh()
    } finally {
      setLoading(false)
    }
  }

  // Al terminar el bloqueo por 429, se rehabilita el botón
  useEffect(() => {
    if (blockedUntil <= Date.now()) return
    const t = setTimeout(() => { setBlockedUntil(0); refresh() }, blockedUntil - Date.now() + 50)
    return () => clearTimeout(t)
  }, [blockedUntil, refresh])

  const onKey = (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }

  return (
    <div className="layout">
      <header>
        <h1>🤖 AI Proxy Platform</h1>
        <div className="who">
          <span className="mono small">{userId}</span>
          {status && <PlanBadge plan={status.plan} />}
          {status && status.plan !== 'ENTERPRISE' && (
            <button className="link" onClick={() => setShowUpgrade(true)}>Mejorar plan</button>
          )}
        </div>
      </header>

      <main>
        <section className="chat">
          <div className="messages">
            {messages.length === 0 && <p className="empty">Escribe un prompt para generar texto con el servicio de IA simulado.</p>}
            {messages.map((m, i) => (
              <div key={i} className={'msg ' + m.role}>
                <div>{m.text}</div>
                {m.tokens != null && <div className="meta">{m.tokens} tokens consumidos</div>}
              </div>
            ))}
            {loading && <div className="msg ai typing">Generando respuesta…</div>}
            <div ref={bottom} />
          </div>

          {error && <div className="error">{error}</div>}
          {blocked && !loading && (
            <div className="warn">⏳ Límite de requests alcanzado. Podrás enviar de nuevo en <b>{countdown}s</b>.</div>
          )}

          <div className="composer">
            <textarea
              rows={2}
              value={prompt}
              placeholder="Escribe tu prompt…"
              onChange={(e) => setPrompt(e.target.value)}
              onKeyDown={onKey}
              maxLength={4000}
            />
            <div className="composer-bar">
              <span className="small">
                Estimado: ~<b>{estimate}</b> tokens (prompt) + respuesta
              </span>
              <button onClick={send} disabled={loading || blocked || !prompt.trim() || quotaOut}>
                {blocked ? `Espera ${countdown}s` : loading ? 'Enviando…' : 'Enviar'}
              </button>
            </div>
            {quotaOut && <div className="warn">Cuota agotada. <button className="link" onClick={() => setShowUpgrade(true)}>Actualizar plan</button></div>}
          </div>
        </section>

        <aside>
          <QuotaBar status={status} />
          <RateCounter status={status} countdown={countdown} />
          <HistoryChart days={history} />
          {status && !status.unlimited && (
            <button className="secondary small-btn" onClick={exhaustForDemo}>
              Demo: agotar cuota mensual
            </button>
          )}
        </aside>
      </main>

      {showUpgrade && status && (
        <UpgradeModal
          currentPlan={status.plan}
          onClose={() => setShowUpgrade(false)}
          onDone={() => { setShowUpgrade(false); setError(''); refresh() }}
        />
      )}
    </div>
  )
}
