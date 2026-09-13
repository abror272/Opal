'use client'

import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { useOpalStore } from '@/lib/opal-store'
import { formatClock } from '@/lib/opal-types'

/** Boshqa tablarda bo'lganda faol sessiyani ko'rsatuvchi suzuvchi glass pill */
export function SessionPill() {
  const activeSession = useOpalStore((s) => s.activeSession)
  const tab = useOpalStore((s) => s.tab)
  const setTab = useOpalStore((s) => s.setTab)
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])

  const visible = !!activeSession && tab !== 'timer'
  if (!activeSession) return null

  const remaining = Math.max(
    activeSession.durationMinutes * 60 - Math.floor((now - activeSession.startedAt) / 1000),
    0
  )
  const progress = Math.min(
    Math.floor((now - activeSession.startedAt) / 1000) / (activeSession.durationMinutes * 60),
    1
  )

  return (
    <AnimatePresence>
      {visible && (
        <motion.button
          initial={{ y: 56, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 56, opacity: 0 }}
          transition={{ type: 'spring', stiffness: 320, damping: 28 }}
          onClick={() => setTab('timer')}
          className="absolute inset-x-8 bottom-[86px] z-30 flex items-center gap-3 overflow-hidden rounded-full border border-white/15 bg-[#10131f]/85 px-4 py-2.5 shadow-[0_14px_40px_rgba(0,0,0,0.55)] backdrop-blur-2xl active:scale-[0.98]"
          aria-label="Jonli sessiyani ochish"
        >
          <span className="relative flex h-2.5 w-2.5 shrink-0">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-70" />
            <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-emerald-400" />
          </span>
          <span className="relative z-10 flex-1 truncate text-left text-[12px] font-bold text-white">
            {activeSession.emoji} {activeSession.label}
          </span>
          <span className="relative z-10 font-mono text-[13px] font-bold tabular-nums text-[#bfe9ff]">
            {formatClock(remaining)}
          </span>
          {/* progress fon */}
          <span
            className="absolute inset-y-0 left-0 bg-gradient-to-r from-[#7dd3fc]/12 to-[#b18cff]/12"
            style={{ width: `${progress * 100}%` }}
            aria-hidden="true"
          />
        </motion.button>
      )}
    </AnimatePresence>
  )
}
