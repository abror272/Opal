'use client'

import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AnimatePresence, motion } from 'framer-motion'
import { toast } from 'sonner'
import { useOpalStore } from '@/lib/opal-store'
import { cn } from '@/lib/utils'
import { X } from 'lucide-react'

/**
 * FOKUS BAHOLASH KARTASI — sessiya tugagach (to'liq yoki erta) chiqadi.
 * Focus Score endi FOYDALANUVCHIning real bahosi (avval random edi):
 * PATCH /api/sessions { rescore: true, focusScore }.
 */

const LEVELS = [
  { score: 45, emoji: '😵', label: 'Qiyin' },
  { score: 58, emoji: '😕', label: "O'ta" },
  { score: 72, emoji: '🙂', label: 'Yaxshi' },
  { score: 86, emoji: '😊', label: "Zo'r" },
  { score: 96, emoji: '🤩', label: 'Mukammal' },
] as const

export function FocusRatingCard({ compact = false }: { compact?: boolean }) {
  const rating = useOpalStore((s) => s.ratingPending)
  const markRatingDone = useOpalStore((s) => s.markRatingDone)
  const qc = useQueryClient()
  const [picked, setPicked] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)

  // sessiya yo'q bo'lsa yoki allaqachon baholangan — kartani umuman ko'rsatmaymiz
  const visible = rating && !rating.rated

  const rate = async (levelIdx: number) => {
    if (!rating || saving) return
    const level = LEVELS[levelIdx]
    setPicked(levelIdx)
    setSaving(true)
    try {
      const res = await fetch('/api/sessions', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: rating.id, rescore: true, focusScore: level.score }),
      })
      if (!res.ok) throw new Error('rescore failed')
      qc.invalidateQueries({ queryKey: ['sessions'] })
      qc.invalidateQueries({ queryKey: ['stats'] })
      markRatingDone()
      toast.success(`Focus Score ${level.score} saqlandi ${level.emoji}`, {
        description: `${rating.emoji} ${rating.label} — haqiqiy bahoyingiz statistikada`,
      })
    } catch {
      toast.error('Bahoni saqlashda xatolik — yana urinib ko‘ring')
      setPicked(null)
    } finally {
      setSaving(false)
    }
  }

  const dismiss = () => {
    // baholamasdan yopish — kartani yashiramiz (yangi sessiyaungacha qaytmaydi:
    // setRatingPending yangi sessiya tugaganda qayta chaqiriladi)
    markRatingDone()
  }

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ opacity: 0, y: 14, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: -8, scale: 0.98 }}
          transition={{ duration: 0.38, ease: [0.22, 1, 0.36, 1] }}
          className={cn(
            'relative w-full overflow-hidden rounded-[24px] border p-4',
            'border-[#86efac]/25 bg-gradient-to-b from-[#86efac]/12 to-white/[0.03] backdrop-blur-xl',
            'shadow-[0_0_28px_rgba(134,239,172,0.14),inset_0_1px_0_rgba(255,255,255,0.08)]'
          )}
          role="group"
          aria-label="Sessiyani baholash"
        >
          {/* mint nur */}
          <div
            className="pointer-events-none absolute -right-10 -top-12 h-28 w-28 rounded-full"
            style={{ background: 'radial-gradient(circle, rgba(134,239,172,0.20), transparent 70%)' }}
            aria-hidden="true"
          />

          <button
            onClick={dismiss}
            aria-label="Baholashni yopish"
            className="absolute right-3 top-3 flex h-7 w-7 items-center justify-center rounded-full bg-white/6 text-white/45 transition-colors hover:bg-white/12 hover:text-white/80"
          >
            <X size={13} />
          </button>

          <p className="text-[13.5px] font-extrabold text-white">
            {rating.emoji} {rating.label} — sessiya qanday o‘tdi?
          </p>
          <p className="mt-0.5 text-[11px] font-medium text-white/45">
            {rating.early
              ? 'Erta tugaldi — lekin har bir daqiqa hisoblandi. Bahoyingiz?'
              : 'To‘liq yakunlandi! Focus Score’ingizni tanlang 👇'}
          </p>

          <div className={cn('mt-3 flex justify-between gap-1.5', compact && 'mt-2.5')}>
            {LEVELS.map((lv, i) => {
              const active = picked === i
              return (
                <motion.button
                  key={lv.label}
                  whileTap={{ scale: 0.88 }}
                  animate={active ? { scale: 1.12, y: -3 } : { scale: 1, y: 0 }}
                  transition={{ type: 'spring', stiffness: 420, damping: 22 }}
                  onClick={() => rate(i)}
                  disabled={saving}
                  aria-label={`Baholash: ${lv.label} (${lv.score} ball)`}
                  aria-pressed={active}
                  className={cn(
                    'flex flex-1 flex-col items-center gap-1 rounded-2xl py-2.5 ring-1 transition-colors',
                    active
                      ? 'bg-[#86efac]/18 ring-[#86efac]/50 shadow-[0_0_18px_rgba(134,239,172,0.30)]'
                      : 'bg-white/[0.045] ring-white/10 hover:bg-white/[0.09] hover:ring-white/20'
                  )}
                >
                  <span className="text-[21px] leading-none" role="img" aria-hidden="true">
                    {lv.emoji}
                  </span>
                  <span
                    className={cn(
                      'text-[9px] font-bold uppercase tracking-wide',
                      active ? 'text-[#c9fbdc]' : 'text-white/40'
                    )}
                  >
                    {lv.label}
                  </span>
                </motion.button>
              )
            })}
          </div>

          {saving && (
            <p className="mt-2 text-center text-[10.5px] font-semibold text-[#9fe8b5]" role="status">
              Saqlanmoqda…
            </p>
          )}
        </motion.div>
      )}
    </AnimatePresence>
  )
}
