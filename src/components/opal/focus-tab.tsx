'use client'

import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useOpalStore } from '@/lib/opal-store'
import {
  SESSION_PRESETS,
  SESSION_DURATIONS,
  formatMinutes,
  type FocusSession,
} from '@/lib/opal-types'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import { createAndStartSession } from '@/lib/opal-session-actions'
import { cn } from '@/lib/utils'
import { Clock3, History, Lock, Play, Trophy, ChevronDown, Sparkles } from 'lucide-react'

const VISIBLE_DEFAULT = 8

function keyOf(d: Date): string {
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
}

function scoreColor(score: number) {
  if (score >= 90) return 'text-emerald-600 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-500/10 ring-emerald-200 dark:ring-emerald-500/25'
  if (score >= 75) return 'text-violet-600 dark:text-violet-300 bg-violet-50 dark:bg-violet-500/10 ring-violet-200 dark:ring-violet-500/25'
  return 'text-amber-600 dark:text-amber-300 bg-amber-50 dark:bg-amber-500/10 ring-amber-200 dark:ring-amber-500/25'
}

function dayGroupLabel(d: Date): string {
  const today = new Date()
  const dayMs = 24 * 60 * 60 * 1000
  const startOfDay = (x: Date) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const diffDays = Math.round((startOfDay(today) - startOfDay(d)) / dayMs)
  if (diffDays === 0) return 'Bugun'
  if (diffDays === 1) return 'Kecha'
  return d.toLocaleDateString('uz-UZ', { day: 'numeric', month: 'long' })
}

function sessionStatus(s: FocusSession): { label: string; className: string; dot: string } {
  if (!s.completed) {
    return {
      label: 'Davom etmoqda',
      className: 'bg-sky-50 text-sky-600 ring-sky-200 dark:bg-sky-500/10 dark:text-sky-300 dark:ring-sky-500/25',
      dot: '#3d5afe',
    }
  }
  if (s.savedMinutes >= s.durationMinutes) {
    return {
      label: 'Bajarildi',
      className: 'bg-emerald-50 text-emerald-600 ring-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-500/25',
      dot: '#10b981',
    }
  }
  return {
    label: 'Erta chiqish',
    className: 'bg-rose-50 text-rose-600 ring-rose-200 dark:bg-rose-500/10 dark:text-rose-300 dark:ring-rose-500/25',
    dot: '#f43f5e',
  }
}

export function FocusTab() {
  const qc = useQueryClient()
  const activeSession = useOpalStore((s) => s.activeSession)

  const [picker, setPicker] = useState<(typeof SESSION_PRESETS)[number] | null>(null)
  const [duration, setDuration] = useState(45)
  const [strict, setStrict] = useState(false)
  const [starting, setStarting] = useState(false)
  const [expanded, setExpanded] = useState(false)

  const historyQ = useQuery<FocusSession[]>({
    queryKey: ['sessions'],
    queryFn: async () => (await fetch('/api/sessions')).json(),
  })

  const handleStart = async () => {
    if (!picker || starting) return
    setStarting(true)
    try {
      const { blockedCount } = await createAndStartSession({
        type: picker.type,
        label: picker.label,
        emoji: picker.emoji,
        durationMinutes: duration,
        strict,
      })
      setPicker(null)
      toast.success(`${picker.emoji} ${duration} daqiqa fokus boshlandi!`, {
        description: `${blockedCount} ilova bloklandi`,
      })
    } catch {
      toast.error('Sessiyani boshlash bajarilmadi')
    } finally {
      setStarting(false)
    }
  }

  return (
    <div className="animate-slide-up space-y-5 px-5 pb-6 pt-3">
      <header>
        <h2 className="text-[22px] font-extrabold tracking-tight text-slate-900 dark:text-slate-50">Fokus sessiyalari</h2>
        <p className="text-[13px] text-slate-400">O‘zingizga mos rejimni tanlang va boshlang</p>
      </header>

      {/* presets */}
      <div className="space-y-3">
        {SESSION_PRESETS.map((p) => (
          <button
            key={p.type}
            onClick={() => {
              setPicker(p)
              setDuration(p.duration)
            }}
            disabled={!!activeSession}
            className={cn(
              'group relative flex w-full items-center gap-4 overflow-hidden rounded-3xl p-4 text-left text-white shadow-lg transition-transform',
              'bg-gradient-to-br',
              p.gradient,
              activeSession ? 'opacity-50' : 'shadow-indigo-500/20 active:scale-[0.98]'
            )}
          >
            <span className="pointer-events-none absolute -right-6 -top-10 h-28 w-28 rounded-full bg-white/15 blur-xl" />
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/20 text-2xl backdrop-blur-sm">
              {p.emoji}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-[16px] font-bold">{p.label}</span>
              <span className="block truncate text-[12px] text-white/75">{p.desc}</span>
            </span>
            <span className="flex shrink-0 items-center gap-1.5 rounded-full bg-black/20 px-3 py-1.5 text-[12px] font-bold backdrop-blur-sm">
              <Clock3 size={13} /> {p.duration} daq
            </span>
          </button>
        ))}
      </div>

      {/* history — App Store style day-grouped timeline */}
      <SessionTimeline
        sessions={historyQ.data ?? []}
        loading={historyQ.isLoading}
        expanded={expanded}
        onToggle={() => setExpanded((v) => !v)}
      />

      {/* duration picker dialog */}
      <Dialog open={!!picker} onOpenChange={(o) => !o && setPicker(null)}>
        <DialogContent className="w-[calc(100%-2rem)] max-w-[350px] translate-y-[-70%] rounded-3xl border-0 bg-white p-5 shadow-2xl [top:50%] dark:bg-[#1c1f4e]">
          {picker && (
            <>
              <DialogHeader className="space-y-1 text-left">
                <DialogTitle className="flex items-center gap-2 text-[18px] font-extrabold text-slate-900 dark:text-slate-50">
                  <span className="text-2xl">{picker.emoji}</span> {picker.label}
                </DialogTitle>
                <DialogDescription className="text-[12.5px]">
                  Davomiylikni tanlang va fokusni boshlang
                </DialogDescription>
              </DialogHeader>

              <div className="mt-4 grid grid-cols-3 gap-2">
                {SESSION_DURATIONS.map((d) => (
                  <button
                    key={d}
                    onClick={() => setDuration(d)}
                    className={cn(
                      'rounded-2xl py-2.5 text-[13.5px] font-bold transition-all',
                      duration === d
                        ? 'bg-gradient-to-r from-[#3d5afe] to-[#7b61ff] text-white shadow-md shadow-indigo-500/30'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-white/10 dark:text-slate-300 dark:hover:bg-white/15'
                    )}
                  >
                    {d} daq
                  </button>
                ))}
              </div>

              <div className="mt-4 flex items-center justify-between rounded-2xl bg-slate-50 p-3.5 ring-1 ring-slate-100 dark:bg-white/5 dark:ring-white/10">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-800 text-white">
                    <Lock size={15} />
                  </div>
                  <div>
                    <p className="text-[13px] font-bold text-slate-800 dark:text-slate-100">Qattiq rejim</p>
                    <p className="text-[11px] text-slate-400 dark:text-slate-500">Erta chiqish imkonini berma</p>
                  </div>
                </div>
                <Switch checked={strict} onCheckedChange={setStrict} aria-label="Qattiq rejim" />
              </div>

              <button
                onClick={handleStart}
                disabled={starting}
                className={cn(
                  'mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-[#3d5afe] via-[#7b61ff] to-[#e861ff] py-3.5 text-[15px] font-bold text-white shadow-lg shadow-indigo-500/30 transition-transform',
                  starting ? 'opacity-60' : 'active:scale-[0.98]'
                )}
              >
                <Play size={16} fill="currentColor" />
                {starting ? 'Boshlanmoqda…' : `${duration} daqiqa boshlash`}
              </button>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}

/* ── day-grouped timeline (App Store / Screen Time style) ── */

interface TimelineProps {
  sessions: FocusSession[]
  loading: boolean
  expanded: boolean
  onToggle: () => void
}

function SessionTimeline({ sessions, loading, expanded, onToggle }: TimelineProps) {
  const visible = expanded ? sessions : sessions.slice(0, VISIBLE_DEFAULT)
  const visibleCount = visible.length

  // group the visible slice by local calendar day (API returns newest first)
  const visibleGroups = useMemo(() => {
    const map = new Map<string, { date: Date; items: FocusSession[] }>()
    for (const s of visible) {
      const d = new Date(s.startedAt)
      const key = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
      const g = map.get(key)
      if (g) g.items.push(s)
      else map.set(key, { date: d, items: [s] })
    }
    return [...map.values()]
  }, [visible])

  if (loading) {
    return (
      <section aria-label="Sessiyalar tarixi" className="space-y-2">
        <Skeleton className="h-5 w-40 rounded-full" />
        <Skeleton className="h-16 w-full rounded-2xl" />
        <Skeleton className="h-16 w-full rounded-2xl" />
        <Skeleton className="h-16 w-full rounded-2xl" />
      </section>
    )
  }

  return (
    <section aria-label="Sessiyalar tarixi">
      <div className="mb-3 flex items-center justify-between px-1">
        <h3 className="flex items-center gap-1.5 text-[14px] font-bold text-slate-800 dark:text-slate-100">
          <History size={15} className="text-slate-400" /> Sessiyalar tarixi
        </h3>
        {sessions.length > 0 && (
          <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10.5px] font-bold text-slate-500 dark:bg-white/10 dark:text-slate-400">
            {sessions.length} ta sessiya
          </span>
        )}
      </div>

      {sessions.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-3xl bg-white py-8 shadow-sm shadow-slate-200/60 dark:bg-[#181b42] dark:shadow-black/30">
          <span className="text-3xl">🌱</span>
          <p className="text-[13px] font-semibold text-slate-500 dark:text-slate-300">Hali sessiya yo‘q</p>
          <p className="max-w-[210px] text-center text-[11.5px] text-slate-400">
            Yuqoridagi rejimlardan birini tanlab, birinchi fokusni boshlang
          </p>
        </div>
      ) : (
        <div className="space-y-5">
          {visibleGroups.map(({ date, items }) => {
            const daySaved = items.reduce((sum, s) => sum + s.savedMinutes, 0)
            return (
              <div key={keyOf(date)}>
                {/* day header */}
                <div className="mb-2 flex items-center gap-2 px-1">
                  <p className="text-[12.5px] font-extrabold text-slate-800 dark:text-slate-100">{dayGroupLabel(date)}</p>
                  {daySaved > 0 && (
                    <span className="flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-600 ring-1 ring-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-500/20">
                      <Sparkles size={9} /> {formatMinutes(daySaved)} tejaldi
                    </span>
                  )}
                  <span aria-hidden="true" className="h-px flex-1 bg-gradient-to-r from-slate-200 to-transparent dark:from-white/10" />
                </div>

                {/* rows */}
                <ol className="relative">
                  <span
                    aria-hidden="true"
                    className="absolute bottom-4 left-[57px] top-4 w-[2px] rounded-full bg-gradient-to-b from-[#7b61ff]/25 via-slate-200/70 to-transparent dark:via-white/10"
                  />
                  {items.map((s) => {
                    const st = sessionStatus(s)
                    const time = new Date(s.startedAt).toLocaleTimeString('uz-UZ', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })
                    return (
                      <li key={s.id} className="relative flex items-stretch gap-2.5 pb-2 last:pb-0">
                        <span className="w-11 shrink-0 pt-3.5 text-right text-[10.5px] font-bold tabular-nums text-slate-400 dark:text-slate-500">
                          {time}
                        </span>
                        <span
                          aria-hidden="true"
                          className="relative z-10 mt-[15px] h-3 w-3 shrink-0 rounded-full ring-4 ring-[#f4f4fb] dark:ring-[#0d0e2b]"
                          style={{ backgroundColor: st.dot }}
                        />
                        <div className="min-w-0 flex-1 rounded-2xl bg-white p-3 shadow-sm shadow-slate-200/60 ring-1 ring-slate-100/70 transition-shadow hover:shadow-md dark:bg-[#181b42] dark:shadow-black/30 dark:ring-white/5">
                          <div className="flex items-center gap-2.5">
                            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-slate-100 to-slate-200 text-lg dark:from-white/10 dark:to-white/15">
                              {s.emoji}
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="truncate text-[13px] font-bold text-slate-800 dark:text-slate-100">{s.label}</p>
                              <p className="text-[10.5px] font-medium text-slate-400 dark:text-slate-500">
                                {s.durationMinutes} daqiqa
                                {s.savedMinutes > 0 ? ` · ${formatMinutes(s.savedMinutes)} tejaldi` : ''}
                              </p>
                            </div>
                            <div className="flex shrink-0 flex-col items-end gap-1">
                              <span className={cn('rounded-full px-2 py-0.5 text-[9.5px] font-bold ring-1', st.className)}>
                                {st.label}
                              </span>
                              {s.completed && (
                                <span className={cn('flex items-center gap-1 rounded-full px-2 py-0.5 text-[9.5px] font-bold ring-1', scoreColor(s.focusScore))}>
                                  <Trophy size={9} /> {s.focusScore}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </li>
                    )
                  })}
                </ol>
              </div>
            )
          })}

          {sessions.length > VISIBLE_DEFAULT && (
            <button
              onClick={onToggle}
              className="flex w-full items-center justify-center gap-1.5 rounded-2xl bg-slate-100/70 py-2.5 text-[12.5px] font-bold text-slate-500 transition-colors hover:bg-slate-200/70 active:scale-[0.98] dark:bg-white/5 dark:text-slate-300 dark:hover:bg-white/10"
            >
              <ChevronDown size={14} className={cn('transition-transform duration-300', expanded && 'rotate-180')} />
              {expanded ? 'Kamaytirish' : `Yana ${sessions.length - visibleCount} tasini ko‘rsatish`}
            </button>
          )}
        </div>
      )}
    </section>
  )
}
