'use client'

import { useEffect, useState, useSyncExternalStore } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { useOpalStore } from '@/lib/opal-store'
import { formatClock } from '@/lib/opal-types'
import { getAmbientMode, setAmbientMode, type AmbientMode } from '@/lib/ambient-audio'

const noopSub = () => () => {}
const ambSnapshot = (): AmbientMode | null => getAmbientMode()
const ambServer = (): AmbientMode | null => null

/** Boshqa tablarda bo'lganda faol sessiyani ko'rsatuvchi suzuvchi glass pill */
export function SessionPill() {
  const activeSession = useOpalStore((s) => s.activeSession)
  const tab = useOpalStore((s) => s.tab)
  const setTab = useOpalStore((s) => s.setTab)
  const [now, setNow] = useState(Date.now())
  const ambient = useSyncExternalStore(noopSub, ambSnapshot, ambServer)

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])

  // sessiya tugadi → ambient tovushni ham o'chirish (boshqa tab'da bo'lsa ham)
  useEffect(() => {
    if (!activeSession && getAmbientMode() !== 'off') {
      setAmbientMode('off')
    }
  }, [activeSession])

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
          {/* ambient tovush indikatori/mute */}
          {ambient && ambient !== 'off' && (
            <span
              role="button"
              tabIndex={0}
              aria-label="Ambient tovushni o'chirish"
              onClick={(e) => {
                e.stopPropagation()
                setAmbientMode('off')
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.stopPropagation()
                  setAmbientMode('off')
                }
              }}
              className="relative z-20 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#b18cff]/25 ring-1 ring-[#b18cff]/50"
            >
              <span className="flex items-end gap-[1.5px]" aria-hidden="true">
                {[0, 1, 2].map((i) => (
                  <motion.span
                    key={i}
                    className="w-[2px] rounded-full bg-[#e6d9ff]"
                    style={{ height: 4 }}
                    animate={{ height: [2, 8, 4, 9, 3] }}
                    transition={{ duration: 0.9 + i * 0.2, repeat: Infinity, ease: 'easeInOut', delay: i * 0.14 }}
                  />
                ))}
              </span>
            </span>
          )}
          {/* progress fon */}
          <span
            className="absolute inset-y-0 left-0 bg-gradient-to-r from-[#86efac]/12 to-[#5eead4]/12"
            style={{ width: `${progress * 100}%` }}
            aria-hidden="true"
          />
        </motion.button>
      )}
    </AnimatePresence>
  )
}
