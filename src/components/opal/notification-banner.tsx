'use client'

import { useEffect, useState } from 'react'
import { toast } from 'sonner'

/**
 * iOS-uslubidagi bildirishnoma ruxsat banneri.
 * Telefon konteyneri ICHIDA absolute joylashadi (desktop ramka buzilmasligi uchun).
 * Bir marta ko'rsatiladi — tanlov localStorage'da saqlanadi.
 */
export function NotificationBanner() {
  const [show, setShow] = useState(false)

  useEffect(() => {
    try {
      if (!localStorage.getItem('opal-notif-choice')) {
        const t = setTimeout(() => setShow(true), 1400)
        return () => clearTimeout(t)
      }
    } catch {
      // ignore
    }
  }, [])

  const decide = (allow: boolean) => {
    try {
      localStorage.setItem('opal-notif-choice', allow ? 'allow' : 'later')
    } catch {
      // ignore
    }
    setShow(false)
    if (allow)
      toast.success('Bildirishnomalar yoqildi 🔔', {
        description: 'Limit ogohlantirishlarini olasiz',
      })
  }

  if (!show) return null

  return (
    <div className="absolute inset-x-4 top-14 z-40 animate-slide-up">
      <div className="overflow-hidden rounded-3xl bg-white/95 shadow-2xl shadow-slate-900/25 ring-1 ring-slate-200 backdrop-blur-xl dark:bg-[#1c1f4e]/95 dark:shadow-black/50 dark:ring-white/10">
        <div className="flex items-center gap-2 border-b border-slate-100 px-4 py-2 dark:border-white/10">
          <span className="text-[11px]">📱</span>
          <span className="text-[11.5px] font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Opal</span>
          <span className="ml-auto text-[11px] text-slate-400">hozir</span>
        </div>
        <div className="flex items-center gap-3 p-4">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3d5afe] via-[#7b61ff] to-[#e861ff] text-lg font-black text-white">
            O
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-[13px] font-bold leading-snug text-slate-900 dark:text-slate-50">
              “Opal” sizga bildirishnoma yuborishi mumkinmi?
            </p>
            <p className="mt-0.5 text-[11px] text-slate-400 dark:text-slate-500">
              Limit ogohlantirishlari, streak eslatmalari
            </p>
          </div>
        </div>
        <div className="grid grid-cols-2 divide-x divide-slate-100 border-t border-slate-100 dark:divide-white/10 dark:border-white/10">
          <button
            onClick={() => decide(false)}
            className="py-3 text-[14px] font-semibold text-slate-400 transition-colors hover:bg-slate-50 dark:hover:bg-white/5"
          >
            Hozir emas
          </button>
          <button
            onClick={() => decide(true)}
            className="py-3 text-[14px] font-bold text-[#3d5afe] transition-colors hover:bg-indigo-50/50 dark:text-[#8ea2ff] dark:hover:bg-white/5"
          >
            Ruxsat berish
          </button>
        </div>
      </div>
    </div>
  )
}
