'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useOpalStore } from '@/lib/opal-store'
import {
  SESSION_PRESETS,
  SESSION_DURATIONS,
  formatMinutes,
  formatClock,
  type FocusSession,
} from '@/lib/opal-types'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import { createAndStartSession } from '@/lib/opal-session-actions'
import { cn } from '@/lib/utils'
import { Clock3, History, Lock, Play, Trophy } from 'lucide-react'

function scoreColor(score: number) {
  if (score >= 90) return 'text-emerald-600 bg-emerald-50 ring-emerald-200'
  if (score >= 75) return 'text-violet-600 bg-violet-50 ring-violet-200'
  return 'text-amber-600 bg-amber-50 ring-amber-200'
}

export function FocusTab() {
  const qc = useQueryClient()
  const activeSession = useOpalStore((s) => s.activeSession)

  const [picker, setPicker] = useState<(typeof SESSION_PRESETS)[number] | null>(null)
  const [duration, setDuration] = useState(45)
  const [strict, setStrict] = useState(false)
  const [starting, setStarting] = useState(false)

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
        <h2 className="text-[22px] font-extrabold tracking-tight text-slate-900">Fokus sessiyalari</h2>
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

      {/* history */}
      <section aria-label="Sessiyalar tarixi">
        <h3 className="mb-2.5 flex items-center gap-1.5 px-1 text-[14px] font-bold text-slate-800">
          <History size={15} className="text-slate-400" /> So‘nggi sessiyalar
        </h3>
        {historyQ.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-16 w-full rounded-2xl" />
            <Skeleton className="h-16 w-full rounded-2xl" />
            <Skeleton className="h-16 w-full rounded-2xl" />
          </div>
        ) : (
          <div className="space-y-2">
            {(historyQ.data ?? []).slice(0, 6).map((s) => (
              <div
                key={s.id}
                className="flex items-center gap-3 rounded-2xl bg-white p-3.5 shadow-sm shadow-slate-200/60"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-slate-100 to-slate-200 text-lg">
                  {s.emoji}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[13.5px] font-bold text-slate-800">{s.label}</p>
                  <p className="text-[11.5px] text-slate-400">
                    {new Date(s.startedAt).toLocaleDateString('uz-UZ', { day: 'numeric', month: 'short' })}
                    {' · '}
                    {new Date(s.startedAt).toLocaleTimeString('uz-UZ', { hour: '2-digit', minute: '2-digit' })}
                    {' · '}
                    {formatMinutes(s.savedMinutes)} tejaldi
                  </p>
                </div>
                <span
                  className={cn(
                    'flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-bold ring-1',
                    scoreColor(s.focusScore)
                  )}
                >
                  <Trophy size={11} /> {s.focusScore}
                </span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* duration picker dialog */}
      <Dialog open={!!picker} onOpenChange={(o) => !o && setPicker(null)}>
        <DialogContent className="w-[calc(100%-2rem)] max-w-[350px] translate-y-[-70%] rounded-3xl border-0 bg-white p-5 shadow-2xl [top:50%]">
          {picker && (
            <>
              <DialogHeader className="space-y-1 text-left">
                <DialogTitle className="flex items-center gap-2 text-[18px] font-extrabold text-slate-900">
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
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    )}
                  >
                    {d} daq
                  </button>
                ))}
              </div>

              <div className="mt-4 flex items-center justify-between rounded-2xl bg-slate-50 p-3.5 ring-1 ring-slate-100">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-800 text-white">
                    <Lock size={15} />
                  </div>
                  <div>
                    <p className="text-[13px] font-bold text-slate-800">Qattiq rejim</p>
                    <p className="text-[11px] text-slate-400">Erta chiqish imkonini berma</p>
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
