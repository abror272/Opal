'use client'

import { useEffect, useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { useOpalStore } from '@/lib/opal-store'
import { toast } from 'sonner'
import { X } from 'lucide-react'
import { cn } from '@/lib/utils'

const CYCLE_MS = 12_000 // 4s inhale, 4s hold, 4s exhale
const TOTAL_MS = 60_000 // 5 sikl = 1 daqiqa
const PHASES = ['Nafas oling', 'Usqlab turing', 'Nafas chiqaring'] as const

export function BreathingOverlay() {
  const breathingOpen = useOpalStore((s) => s.breathingOpen)
  const setBreathingOpen = useOpalStore((s) => s.setBreathingOpen)

  const [elapsed, setElapsed] = useState(0)
  const [running, setRunning] = useState(true)

  useEffect(() => {
    if (!running) return
    const id = setInterval(() => {
      setElapsed((e) => {
        const next = e + 100
        if (next >= TOTAL_MS) {
          clearInterval(id)
          setRunning(false)
          setTimeout(() => {
            toast.success('🌿 Tinchladingiz!', { description: '1 daqiqalik nafas mashqi tugadi' })
            setBreathingOpen(false)
          }, 900)
          return TOTAL_MS
        }
        return next
      })
    }, 100)
    return () => clearInterval(id)
  }, [running])

  const phaseInfo = useMemo(() => {
    const inCycle = elapsed % CYCLE_MS
    if (inCycle < 4000) return { label: PHASES[0], scale: 1.38, color: 'from-[#4d9fd6] to-[#5eead4]' }
    if (inCycle < 8000) return { label: PHASES[1], scale: 1.38, color: 'from-[#5eead4] to-[#b18cff]' }
    return { label: PHASES[2], scale: 1, color: 'from-[#3a6ea8] to-[#5eead4]' }
  }, [elapsed])

  const secondsLeft = Math.ceil((TOTAL_MS - elapsed) / 1000)
  const cycleNum = Math.min(Math.floor(elapsed / CYCLE_MS) + 1, 5)
  const done = elapsed >= TOTAL_MS

  if (!breathingOpen) return null

  return (
    <div className="absolute inset-0 z-[60] flex flex-col bg-[#05060f]/97 text-white backdrop-blur-2xl">
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div className="absolute left-1/2 top-1/2 h-80 w-80 -translate-x-1/2 -translate-y-1/2 rounded-full bg-[#5eead4]/10 blur-3xl" />
        <div
          className="absolute inset-0 opacity-40"
          style={{
            backgroundImage:
              'radial-gradient(1px 1px at 24% 28%, rgba(255,255,255,0.5) 50%, transparent 51%),' +
              'radial-gradient(1.4px 1.4px at 76% 40%, rgba(255,255,255,0.45) 50%, transparent 51%),' +
              'radial-gradient(1px 1px at 60% 78%, rgba(255,255,255,0.35) 50%, transparent 51%)',
          }}
        />
      </div>

      {/* top */}
      <div className="relative z-20 flex items-center justify-between px-5 pt-3">
        <div className="rounded-full border border-white/12 bg-white/8 px-3.5 py-1.5 text-[12px] font-semibold backdrop-blur">
          🌬️ Nafas mashqi
        </div>
        <button
          onClick={() => setBreathingOpen(false)}
          aria-label="Nafas mashqini yopish"
          className="flex h-9 w-9 items-center justify-center rounded-full border border-white/12 bg-white/8 backdrop-blur transition-transform active:scale-90"
        >
          <X size={17} />
        </button>
      </div>

      {/* breathing circle */}
      <div className="relative z-20 flex flex-1 flex-col items-center justify-center">
        <div className="relative flex h-[280px] w-[280px] items-center justify-center">
          <div className="absolute inset-0 rounded-full border border-white/10" aria-hidden="true" />
          <div className="absolute inset-5 rounded-full border border-white/5" aria-hidden="true" />

          <motion.div
            className={cn(
              'relative h-[190px] w-[190px] rounded-full bg-gradient-to-br shadow-[0_0_70px_rgba(94,234,212,0.35)] transition-colors duration-1000',
              phaseInfo.color
            )}
            animate={{ scale: phaseInfo.scale }}
            transition={{ duration: 4, ease: 'easeInOut', times: [0, 0.33, 0.66, 1] }}
            style={{ transformOrigin: 'center' }}
          />

          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <AnimatePresence mode="wait">
              <motion.span
                key={phaseInfo.label + String(done)}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                transition={{ duration: 0.3 }}
                className="text-[19px] font-bold"
              >
                {done ? 'Ajoyib ✨' : phaseInfo.label}
              </motion.span>
            </AnimatePresence>
            <span className="mt-1 text-[12px] font-medium tabular-nums text-white/55">
              {done ? 'Mashq tugadi' : `${secondsLeft}s qoldi · ${cycleNum}/5 sikl`}
            </span>
          </div>
        </div>

        <div className="mt-8 flex gap-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <span
              key={i}
              className={cn(
                'h-1.5 rounded-full transition-all duration-500',
                cycleNum > i || done
                  ? 'w-6 bg-[#86efac]'
                  : cycleNum === i
                    ? 'w-6 bg-white/70'
                    : 'w-1.5 bg-white/15'
              )}
            />
          ))}
        </div>
      </div>

      {/* bottom */}
      <div className="relative z-20 px-6 pb-8 text-center">
        {running && !done ? (
          <button
            onClick={() => setRunning(false)}
            className="rounded-full border border-white/15 bg-white/8 px-6 py-2.5 text-[13px] font-bold text-white/80 backdrop-blur transition-transform active:scale-95"
          >
            Pauza
          </button>
        ) : !done ? (
          <button
            onClick={() => setRunning(true)}
            className="rounded-full bg-white px-7 py-2.5 text-[13px] font-bold text-black shadow-lg transition-transform active:scale-95"
          >
            Davom etish
          </button>
        ) : (
          <button
            onClick={() => setBreathingOpen(false)}
            className="rounded-full bg-gradient-to-r from-[#5b9bd5] to-[#b18cff] px-7 py-2.5 text-[13px] font-bold text-white shadow-lg transition-transform active:scale-95"
          >
            Yopish
          </button>
        )}
        <p className="mt-3 text-[11px] text-white/30">4 soniya oling · 4 ushlab turing · 4 chiqaring</p>
      </div>
    </div>
  )
}
