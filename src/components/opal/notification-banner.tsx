'use client'

import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { toast } from 'sonner'

/**
 * Haqiqiy Opal uslubidagi qorong'u glass bildirishnoma.
 * "Time for a break? You've been on IG for 15 min." — shot 0 dizayni.
 * Telefon konteyneri ICHIDA absolute; bir marta ko'rsatiladi.
 */
const NOTIFS = [
  { emoji: '📱', title: 'Dam olish vaqti?', desc: 'TikTok’da 15 daqiqa bo‘ldi. Biroz tinchlanaylik.' },
  { emoji: '⚠️', title: 'Doomscrolling ogohlantirishi!', desc: '30 daqiqa Instagram — barmog‘ingizga dam bering.' },
]

export function NotificationBanner() {
  const [show, setShow] = useState(false)
  const [notif] = useState(() => NOTIFS[Math.floor(Math.random() * NOTIFS.length)])

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

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          key="notif"
          initial={{ y: -70, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: -70, opacity: 0 }}
          transition={{ type: 'spring', stiffness: 300, damping: 26 }}
          className="absolute inset-x-4 top-12 z-40"
          role="alertdialog"
          aria-label="Bildirishnoma ruxsati"
        >
          <div className="rounded-[26px] border border-white/15 bg-[#2a2f3d]/85 p-4 shadow-[0_22px_60px_rgba(0,0,0,0.6)] backdrop-blur-2xl">
            <div className="flex items-start gap-3">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3a3f4f] to-[#222633] text-xl ring-1 ring-white/15">
                {notif.emoji}
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-[14px] font-extrabold leading-snug text-white">{notif.title}</p>
                <p className="mt-0.5 text-[12.5px] leading-snug text-white/60">{notif.desc}</p>
              </div>
            </div>
            <div className="mt-3.5 grid grid-cols-2 gap-2.5">
              <button
                onClick={() => decide(false)}
                className="rounded-full bg-white/8 py-2.5 text-[13px] font-bold text-white/60 ring-1 ring-white/10 transition-colors hover:bg-white/12 active:scale-[0.97]"
              >
                Hozir emas
              </button>
              <button
                onClick={() => decide(true)}
                className="rounded-full bg-white py-2.5 text-[13px] font-bold text-black shadow-lg transition-transform active:scale-[0.97]"
              >
                Ruxsat berish
              </button>
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
