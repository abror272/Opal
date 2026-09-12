'use client'

import { useEffect, useState } from 'react'

export function StatusBar({ dark = false }: { dark?: boolean }) {
  const [time, setTime] = useState('')

  useEffect(() => {
    const update = () => {
      const d = new Date()
      setTime(`${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`)
    }
    update()
    const id = setInterval(update, 15_000)
    return () => clearInterval(id)
  }, [])

  const color = dark ? 'text-white' : 'text-slate-900'

  return (
    <div
      className={`flex items-center justify-between px-7 pt-3.5 pb-1 text-[13px] font-semibold ${color} select-none`}
      aria-hidden="true"
    >
      <span className="tracking-tight tabular-nums">{time || '–:–'}</span>
      <div className="flex items-center gap-1.5">
        {/* signal */}
        <svg width="17" height="11" viewBox="0 0 17 11" fill="currentColor">
          <rect x="0" y="7" width="3" height="4" rx="0.8" />
          <rect x="4.5" y="5" width="3" height="6" rx="0.8" />
          <rect x="9" y="2.5" width="3" height="8.5" rx="0.8" />
          <rect x="13.5" y="0" width="3" height="11" rx="0.8" />
        </svg>
        {/* wifi */}
        <svg width="16" height="11" viewBox="0 0 16 12" fill="currentColor">
          <path d="M8 9.6a1.4 1.4 0 1 0 0 2.8 1.4 1.4 0 0 0 0-2.8Z" />
          <path d="M8 5.7c1.5 0 2.9.6 3.9 1.6l-1.3 1.3A3.9 3.9 0 0 0 8 7.5c-1 0-1.9.4-2.6 1.1L4.1 7.3a5.5 5.5 0 0 1 3.9-1.6Z" />
          <path d="M8 1.8c2.6 0 5 1 6.8 2.8l-1.3 1.3A7.6 7.6 0 0 0 8 3.6c-2.1 0-4 .8-5.5 2.3L1.2 4.6A9.6 9.6 0 0 1 8 1.8Z" />
        </svg>
        {/* battery */}
        <svg width="25" height="12" viewBox="0 0 25 12" fill="none">
          <rect x="0.5" y="0.5" width="21" height="11" rx="3" stroke="currentColor" opacity="0.4" />
          <rect x="2" y="2" width="16" height="8" rx="1.8" fill="currentColor" />
          <path d="M23 4v4c1-.2 1.6-1 1.6-2S24 4.2 23 4Z" fill="currentColor" opacity="0.4" />
        </svg>
      </div>
    </div>
  )
}
