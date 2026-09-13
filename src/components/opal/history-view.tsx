'use client'

import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import type { FocusSession } from '@/lib/opal-types'
import { formatMinutes } from '@/lib/opal-types'
import { GLASS, clockTime } from '@/lib/opal-ui'
import { cn } from '@/lib/utils'
import { Skeleton } from '@/components/ui/skeleton'
import { Check, Undo2, Moon, Hourglass } from 'lucide-react'

/** Kunlar bo'yicha guruhlangan sessiyalar (App Store "Updated" timeline uslubi) */
interface DayGroup {
  key: string
  label: string
  savedMinutes: number
  sessions: FocusSession[]
}

const MONTHS_UZ = [
  'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'Iyun',
  'Iyul', 'Avgust', 'Sentabr', 'Oktabr', 'Noyabr', 'Dekabr',
]

function groupLabel(iso: string, todayIso: string, yesterdayIso: string): string {
  if (iso === todayIso) return 'Bugun'
  if (iso === yesterdayIso) return 'Kecha'
  const d = new Date(iso + 'T00:00:00')
  return `${d.getDate()} ${MONTHS_UZ[d.getMonth()]}`
}

function statusOf(s: FocusSession): { done: boolean; text: string } {
  if (!s.endedAt) return { done: false, text: 'Davom etmoqda' }
  return s.completed ? { done: true, text: "To'liq yakunlandi" } : { done: false, text: 'Erta chiqildi' }
}

function SessionRow({ s, index }: { s: FocusSession; index: number }) {
  const st = statusOf(s)
  const isSleep = s.type === 'SLEEP'
  const start = new Date(s.startedAt)
  const end = s.endedAt ? new Date(s.endedAt) : null
  const liveMinutes = end
    ? Math.max(Math.round((end.getTime() - start.getTime()) / 60_000), 1)
    : s.durationMinutes

  return (
    <motion.div
      initial={{ opacity: 0, x: 18 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: Math.min(index * 0.045, 0.4), duration: 0.32, ease: [0.22, 1, 0.36, 1] }}
      className="flex items-center gap-3 py-2.5"
    >
      {/* vertikal timeline chizig'i */}
      <div className="relative flex h-11 w-11 shrink-0 items-center justify-center">
        <span
          className={cn(
            'flex h-11 w-11 items-center justify-center rounded-[15px] text-[19px] ring-1',
            isSleep ? 'bg-indigo-400/12 ring-indigo-300/25' : 'bg-white/[0.05] ring-white/10',
            !st.done && !isSleep && 'opacity-75'
          )}
          aria-hidden="true"
        >
          {s.emoji}
        </span>
        <span className="absolute -bottom-[9px] left-1/2 top-full w-px -translate-x-1/2 bg-white/8 last:hidden" aria-hidden="true" />
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-[13.5px] font-bold text-white">{s.label}</p>
        <p className="mt-0.5 text-[10.5px] font-medium text-white/40">
          {clockTime(start)}
          {end ? ` — ${clockTime(end)}` : ' · hozir'} · {isSleep ? 'uyqu' : 'fokus'}
        </p>
      </div>

      <div className="flex shrink-0 flex-col items-end gap-1">
        <span
          className={cn(
            'rounded-full px-2 py-0.5 text-[10px] font-bold ring-1',
            st.done
              ? 'bg-emerald-500/12 text-emerald-300 ring-emerald-400/25'
              : 'bg-amber-500/12 text-amber-300 ring-amber-400/25'
          )}
        >
          <span className="inline-flex items-center gap-1">
            {st.done ? <Check size={9} /> : <Undo2 size={9} />}
            {st.text}
          </span>
        </span>
        <span className="text-[10.5px] font-semibold text-white/45">
          {formatMinutes(liveMinutes)}
          {s.savedMinutes > 0 && (
            <span className="ml-1 text-[#9fe8b5]">+{formatMinutes(s.savedMinutes)} saqlandi</span>
          )}
        </span>
      </div>
    </motion.div>
  )
}

export function HistoryView() {
  const sessionsQ = useQuery<FocusSession[]>({
    queryKey: ['sessions'],
    queryFn: async () => (await fetch('/api/sessions')).json(),
  })

  const groups = useMemo<DayGroup[]>(() => {
    const now = new Date()
    const iso = (d: Date) => d.toISOString().slice(0, 10)
    const todayIso = iso(now)
    const yest = new Date(now)
    yest.setDate(yest.getDate() - 1)
    const yesterdayIso = iso(yest)

    const map = new Map<string, FocusSession[]>()
    for (const s of sessionsQ.data ?? []) {
      const key = iso(new Date(s.startedAt))
      const list = map.get(key) ?? []
      list.push(s)
      map.set(key, list)
    }
    return [...map.entries()]
      .sort((a, b) => (a[0] < b[0] ? 1 : -1))
      .map(([key, list]) => ({
        key,
        label: groupLabel(key, todayIso, yesterdayIso),
        savedMinutes: list.reduce((acc, s) => acc + (s.completed ? s.savedMinutes : 0), 0),
        sessions: list.sort((a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime()),
      }))
  }, [sessionsQ.data])

  const all = sessionsQ.data ?? []
  const completed = all.filter((s) => s.completed && s.endedAt)
  const completionPct = all.length ? Math.round((completed.length / all.length) * 100) : 0
  const totalSaved = completed.reduce((acc, s) => acc + s.savedMinutes, 0)
  const sleepCount = completed.filter((s) => s.type === 'SLEEP').length

  if (sessionsQ.isLoading) {
    return (
      <div className="space-y-4 px-5 pt-3">
        <Skeleton className="h-20 w-full rounded-3xl bg-white/5" />
        <Skeleton className="h-40 w-full rounded-3xl bg-white/5" />
        <Skeleton className="h-40 w-full rounded-3xl bg-white/5" />
      </div>
    )
  }

  return (
    <div className="space-y-5 px-5 pb-6 pt-1">
      {/* xulosa */}
      <section className={cn(GLASS, 'grid grid-cols-3 gap-2 p-4 text-center')} aria-label="Tarix xulosasi">
        <div>
          <p className="flex items-center justify-center gap-1 text-[16px] font-extrabold text-white">
            <Hourglass size={13} className="text-[#9fe8b5]" /> {all.length}
          </p>
          <p className="mt-0.5 text-[9.5px] font-semibold uppercase tracking-wider text-white/40">
            Sessiyalar
          </p>
        </div>
        <div className="border-x border-white/8">
          <p className="text-[16px] font-extrabold text-[#9fe8b5]">{completionPct}%</p>
          <p className="mt-0.5 text-[9.5px] font-semibold uppercase tracking-wider text-white/40">
            To‘liq yakunlangan
          </p>
        </div>
        <div>
          <p className="flex items-center justify-center gap-1 text-[16px] font-extrabold text-white">
            <Moon size={13} className="text-indigo-300" /> {sleepCount}
          </p>
          <p className="mt-0.5 text-[9.5px] font-semibold uppercase tracking-wider text-white/40">
            Uyqu kechalari
          </p>
        </div>
        <p className="col-span-3 -mt-1 border-t border-white/8 pt-2.5 text-[11px] font-semibold text-white/45">
          Jami tejaldi: <span className="font-extrabold text-[#c9fbdc]">{formatMinutes(totalSaved)}</span>
        </p>
      </section>

      {/* kunlar bo'yicha timeline */}
      {groups.length === 0 ? (
        <p className="rounded-3xl border border-dashed border-white/12 py-8 text-center text-[12.5px] text-white/40">
          Hozircha sessiyalar yo‘q — birinchi fokusni boshlang!
        </p>
      ) : (
        groups.map((g) => (
          <section key={g.key} aria-label={`${g.label} sessiyalari`}>
            <div className="mb-1 flex items-baseline justify-between">
              <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-white/55">
                {g.label}
              </p>
              {g.savedMinutes > 0 && (
                <span className="text-[10.5px] font-bold text-[#9fe8b5]">
                  +{formatMinutes(g.savedMinutes)}
                </span>
              )}
            </div>
            <div className={cn(GLASS, 'divide-y divide-white/[0.04] px-4 py-1.5')}>
              {g.sessions.map((s, i) => (
                <SessionRow key={s.id} s={s} index={i} />
              ))}
            </div>
          </section>
        ))
      )}
    </div>
  )
}
