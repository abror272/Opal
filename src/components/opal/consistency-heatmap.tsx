'use client'

import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import type { StatsResponse } from '@/lib/opal-types'
import { formatMinutes } from '@/lib/opal-types'
import { GLASS } from '@/lib/opal-ui'
import { cn } from '@/lib/utils'

/**
 * CONSISTENCY HEATMAP — so'nggi 5 haftalik "tejash" xaritasi
 * (GitHub contribution graph uslubi, Opal mint tilida).
 * Manba: GET /api/stats?days=35 (DailyStat.savedMinutes).
 */

const WEEKDAY_LABELS = [
  { row: 0, label: 'Du' },
  { row: 2, label: 'Ch' },
  { row: 4, label: 'Ju' },
] // dushanba/chorshanba/juma — GitHub uslubidagi qisqa yorliqlar

const MONTHS_UZ = ['Yan', 'Fev', 'Mar', 'Apr', 'May', 'Iyn', 'Iyl', 'Avg', 'Sen', 'Okt', 'Noy', 'Dek']

interface Cell {
  dateISO: string
  label: string
  saved: number
  /** hafta ustuni (0..) va hafta kuni qatori (0=Du .. 6=Ya) */
  week: number
  dow: number
  isToday: boolean
  isFuture: boolean
}

/** tejash daqiqasiga qarab intensivlik darajasi (0..4) */
function levelOf(saved: number): 0 | 1 | 2 | 3 | 4 {
  if (saved <= 0) return 0
  if (saved < 45) return 1
  if (saved < 90) return 2
  if (saved < 150) return 3
  return 4
}

const LEVEL_CLASSES: Record<0 | 1 | 2 | 3 | 4, string> = {
  0: 'bg-white/[0.055]',
  1: 'bg-[#86efac]/22',
  2: 'bg-[#86efac]/45',
  3: 'bg-[#86efac]/70',
  4: 'bg-[#86efac] shadow-[0_0_10px_rgba(134,239,172,0.55)]',
}

export function ConsistencyHeatmap() {
  const statsQ = useQuery<StatsResponse>({
    queryKey: ['stats', '35d'],
    queryFn: async () => (await fetch('/api/stats?days=35')).json(),
  })

  const { cells, weeks, totalSaved, activeDays } = useMemo(() => {
    const byDate = new Map<string, number>()
    for (const d of statsQ.data?.days ?? []) byDate.set(d.date, d.savedMinutes)

    const now = new Date()
    const todayIso = now.toISOString().slice(0, 10)

    // 35 kunlik oyna dushanbaga tekislanadi → to'liq hafta ustunlari
    const start = new Date(now)
    start.setDate(start.getDate() - 34)
    const backToMonday = (start.getDay() + 6) % 7
    start.setDate(start.getDate() - backToMonday)

    const span = 34 + backToMonday // 0..span indeksli kunlar (bugun = oxirgi)
    const totalCells = Math.ceil((span + 1) / 7) * 7 // oxirgi haftani to'ldirish

    const list: Cell[] = []
    const cursor = new Date(start)
    for (let i = 0; i < totalCells; i++) {
      const iso = cursor.toISOString().slice(0, 10)
      const dow = (cursor.getDay() + 6) % 7
      const isFuture = cursor > now
      const saved = byDate.get(iso)
      list.push({
        dateISO: iso,
        label: `${cursor.getDate()} ${MONTHS_UZ[cursor.getMonth()]}`,
        saved: Math.max(saved ?? 0, 0),
        week: Math.floor(i / 7),
        dow,
        isToday: iso === todayIso,
        isFuture,
      })
      cursor.setDate(cursor.getDate() + 1)
    }

    const valid = list.filter((c) => !c.isFuture && byDate.has(c.dateISO))
    return {
      cells: list,
      weeks: Math.ceil(totalCells / 7),
      totalSaved: valid.reduce((a, c) => a + c.saved, 0),
      activeDays: valid.filter((c) => c.saved > 0).length,
    }
  }, [statsQ.data])

  // oy yorliqlari — ustun tepasida (ustundagi 1-kun oyi)
  const monthLabels = useMemo(() => {
    const out: { week: number; label: string }[] = []
    let lastMonth = -1
    for (const c of cells) {
      if (c.dow !== 0 || c.isFuture) continue
      const m = new Date(c.dateISO + 'T00:00:00').getMonth()
      if (m !== lastMonth) {
        out.push({ week: c.week, label: MONTHS_UZ[m] })
        lastMonth = m
      }
    }
    return out
  }, [cells])

  const loading = statsQ.isLoading

  return (
    <section aria-label="Izchillik xaritasi" className="pt-1">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-[16px] font-bold text-white">Consistency</h3>
        {loading ? (
          <span className="text-[11.5px] font-semibold text-white/30">yuklanmoqda…</span>
        ) : (
          <span className="text-[11.5px] font-semibold text-white/40">
            {activeDays}/35 kun · <span className="text-[#9fe8b5]">{formatMinutes(totalSaved)}</span> tejaldi
          </span>
        )}
      </div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
        className={cn(GLASS, 'p-4')}
      >
        {loading ? (
          <div className="flex h-[118px] items-center justify-center">
            <span className="h-5 w-5 animate-spin rounded-full border-2 border-white/15 border-t-[#86efac]/80" aria-hidden="true" />
          </div>
        ) : (
          <>
            {/* oy yorliqlari */}
            <div className="grid" style={{ gridTemplateColumns: `26px repeat(${weeks}, minmax(0,1fr))` }} aria-hidden="true">
              <span />
              {Array.from({ length: weeks }).map((_, w) => {
                const found = monthLabels.find((m) => m.week === w)
                return (
                  <span key={w} className="pb-1 text-left text-[9px] font-bold uppercase tracking-wide text-white/30">
                    {found?.label ?? ''}
                  </span>
                )
              })}
            </div>

            <div className="flex gap-1.5">
              {/* hafta kunlari */}
              <div className="flex w-[20px] shrink-0 flex-col gap-[4.5px]" aria-hidden="true">
                {Array.from({ length: 7 }).map((_, r) => {
                  const lab = WEEKDAY_LABELS.find((w) => w.row === r)
                  return (
                    <span key={r} className="flex h-[13px] items-center text-[8.5px] font-semibold text-white/28">
                      {lab?.label ?? ''}
                    </span>
                  )
                })}
              </div>

              {/* katakchalar grid'i */}
              <div
                className="grid flex-1 gap-[4.5px]"
                style={{ gridTemplateColumns: `repeat(${weeks}, minmax(0,1fr))`, gridTemplateRows: 'repeat(7, 13px)', gridAutoFlow: 'column' }}
                role="img"
                aria-label={`So'nggi 5 haftada ${activeDays} kun fokus — jami ${formatMinutes(totalSaved)} tejaldi`}
              >
                {cells.map((c, i) =>
                  c.isFuture ? (
                    <span key={c.dateISO} aria-hidden="true" />
                  ) : (
                    <motion.span
                      key={c.dateISO}
                      initial={{ opacity: 0, scale: 0.4 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: Math.min(i * 0.008, 0.5), duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                      className={cn(
                        'h-[13px] w-full rounded-[3.5px]',
                        LEVEL_CLASSES[levelOf(c.saved)],
                        c.isToday && 'ring-1 ring-[#e6fff0] ring-offset-1 ring-offset-transparent'
                      )}
                      title={`${c.label} · ${c.saved > 0 ? `${formatMinutes(c.saved)} tejaldi` : 'fokus yo‘q'}`}
                    />
                  )
                )}
              </div>
            </div>

            {/* legend */}
            <div className="mt-3 flex items-center justify-end gap-1.5" aria-hidden="true">
              <span className="mr-0.5 text-[9px] font-semibold text-white/30">Kam</span>
              {[0, 1, 2, 3, 4].map((lv) => (
                <span key={lv} className={cn('h-[10px] w-[10px] rounded-[3px]', LEVEL_CLASSES[lv as 0 | 1 | 2 | 3 | 4])} />
              ))}
              <span className="ml-0.5 text-[9px] font-semibold text-white/30">Ko‘p</span>
            </div>
          </>
        )}
      </motion.div>
    </section>
  )
}
