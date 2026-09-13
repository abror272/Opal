'use client'

import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { AnimatePresence, motion } from 'framer-motion'
import type { DailyStat, StatsResponse } from '@/lib/opal-types'
import { formatMinutes } from '@/lib/opal-types'
import { GLASS } from '@/lib/opal-ui'
import { cn } from '@/lib/utils'
import { X } from 'lucide-react'

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

const LEVEL_NAMES: Record<0 | 1 | 2 | 3 | 4, string> = {
  0: 'Fokus yo‘q',
  1: 'Yengil kun',
  2: 'O‘rtacha kun',
  3: 'Yaxshi kun',
  4: 'Zo‘r kun!',
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

  // tanlangan kun tafsilotlari (katakchani bosganda chiqadi)
  const [selectedISO, setSelectedISO] = useState<string | null>(null)

  const { cells, weeks, totalSaved, activeDays, dayByDate } = useMemo(() => {
    const byDate = new Map<string, number>()
    const full = new Map<string, DailyStat>()
    for (const d of statsQ.data?.days ?? []) {
      byDate.set(d.date, d.savedMinutes)
      full.set(d.date, d)
    }

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
      dayByDate: full,
    }
  }, [statsQ.data])

  const selected = selectedISO ? dayByDate.get(selectedISO) ?? null : null

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
          <span className="flex items-center gap-2">
            <span className="text-[11.5px] font-semibold text-white/40">
              {activeDays}/35 kun · <span className="text-[#9fe8b5]">{formatMinutes(totalSaved)}</span>
            </span>
            <span className="rounded-full bg-white/[0.05] px-2 py-0.5 text-[9px] font-semibold text-white/35 ring-1 ring-white/8">
              kunni bosing
            </span>
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
                    <motion.button
                      key={c.dateISO}
                      type="button"
                      initial={{ opacity: 0, scale: 0.4 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: Math.min(i * 0.008, 0.5), duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                      whileTap={{ scale: 1.35 }}
                      onClick={() => setSelectedISO((prev) => (prev === c.dateISO ? null : c.dateISO))}
                      aria-label={`${c.label}: ${c.saved > 0 ? `${formatMinutes(c.saved)} tejaldi` : 'fokus yo‘q'} — tafsilotlarni ko‘rish`}
                      aria-pressed={selectedISO === c.dateISO}
                      className={cn(
                        'h-[13px] w-full rounded-[3.5px] transition-shadow cursor-pointer outline-none',
                        'focus-visible:ring-2 focus-visible:ring-[#e6fff0]/70',
                        LEVEL_CLASSES[levelOf(c.saved)],
                        c.isToday && selectedISO !== c.dateISO && 'ring-1 ring-[#e6fff0] ring-offset-1 ring-offset-transparent',
                        selectedISO === c.dateISO && 'ring-[1.5px] ring-white ring-offset-2 ring-offset-[#0a0f0c]'
                      )}
                    />
                  )
                )}
              </div>
            </div>

            {/* tanlangan kun tafsilotlari */}
            <AnimatePresence initial={false}>
              {selected && (
                <motion.div
                  key="day-details"
                  initial={{ opacity: 0, height: 0, y: -6 }}
                  animate={{ opacity: 1, height: 'auto', y: 0 }}
                  exit={{ opacity: 0, height: 0, y: -6 }}
                  transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
                  className="overflow-hidden"
                  role="region"
                  aria-label={`${selected.date} kun tafsilotlari`}
                >
                  <div className="relative mt-3 rounded-2xl border border-[#86efac]/22 bg-gradient-to-b from-[#86efac]/10 to-white/[0.02] p-3.5">
                    <button
                      onClick={() => setSelectedISO(null)}
                      aria-label="Kun tafsilotlarini yopish"
                      className="absolute right-2.5 top-2.5 flex h-6 w-6 items-center justify-center rounded-full bg-white/6 text-white/45 transition-colors hover:bg-white/12 hover:text-white/80"
                    >
                      <X size={12} />
                    </button>
                    <div className="flex items-baseline gap-2">
                      <p className="text-[13px] font-extrabold text-white">{selected.date.slice(0, 10)}</p>
                      <span
                        className={cn(
                          'rounded-full px-2 py-0.5 text-[9px] font-bold uppercase tracking-wide ring-1',
                          levelOf(selected.savedMinutes) >= 3
                            ? 'bg-[#86efac]/15 text-[#c9fbdc] ring-[#86efac]/35'
                            : levelOf(selected.savedMinutes) >= 1
                              ? 'bg-white/6 text-white/60 ring-white/12'
                              : 'bg-white/4 text-white/35 ring-white/8'
                        )}
                      >
                        {LEVEL_NAMES[levelOf(selected.savedMinutes)]}
                      </span>
                    </div>
                    <div className="mt-2.5 grid grid-cols-3 gap-2 text-center">
                      <div className="rounded-xl bg-black/25 px-2 py-2 ring-1 ring-white/8">
                        <p className="text-[14px] font-extrabold text-[#b7f5cd]">
                          {formatMinutes(selected.savedMinutes)}
                        </p>
                        <p className="mt-0.5 text-[8.5px] font-bold uppercase tracking-wider text-white/40">
                          Tejaldi
                        </p>
                      </div>
                      <div className="rounded-xl bg-black/25 px-2 py-2 ring-1 ring-white/8">
                        <p className="text-[14px] font-extrabold text-white">
                          {formatMinutes(selected.screenTimeMinutes)}
                        </p>
                        <p className="mt-0.5 text-[8.5px] font-bold uppercase tracking-wider text-white/40">
                          Ekran vaqti
                        </p>
                      </div>
                      <div className="rounded-xl bg-black/25 px-2 py-2 ring-1 ring-white/8">
                        <p className="text-[14px] font-extrabold text-white">{selected.pickups}</p>
                        <p className="mt-0.5 text-[8.5px] font-bold uppercase tracking-wider text-white/40">
                          Ko‘tarilishlar
                        </p>
                      </div>
                    </div>
                    {/* maqsad progressi */}
                    {selected.goalMinutes > 0 && (
                      <div className="mt-2.5">
                        <div className="mb-1 flex items-center justify-between text-[9px] font-semibold text-white/40">
                          <span>Kunlik maqsad ({Math.round(selected.goalMinutes / 60)}h ekran)</span>
                          <span
                            className={cn(
                              selected.screenTimeMinutes <= selected.goalMinutes
                                ? 'text-[#9fe8b5]'
                                : 'text-amber-300'
                            )}
                          >
                            {selected.screenTimeMinutes <= selected.goalMinutes
                              ? '✓ maqsadda'
                              : `+${formatMinutes(selected.screenTimeMinutes - selected.goalMinutes)} ortiq`}
                          </span>
                        </div>
                        <div className="h-[5px] overflow-hidden rounded-full bg-white/8">
                          <div
                            className={cn(
                              'h-full rounded-full bg-gradient-to-r transition-[width] duration-500',
                              selected.screenTimeMinutes <= selected.goalMinutes
                                ? 'from-[#86efac] to-[#5eead4]'
                                : 'from-amber-300 to-orange-400'
                            )}
                            style={{
                              width: `${Math.min((selected.screenTimeMinutes / selected.goalMinutes) * 100, 100)}%`,
                            }}
                          />
                        </div>
                      </div>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

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
