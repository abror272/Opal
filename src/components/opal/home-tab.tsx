'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useOpalStore } from '@/lib/opal-store'
import { formatMinutes, type StatsResponse, type UserProfile, type BlockApp } from '@/lib/opal-types'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { Flame, ShieldCheck, ShieldOff, ChevronRight, Timer, Sparkles } from 'lucide-react'
import { cn } from '@/lib/utils'

function ProtectionRing({ progress, minutes, goal }: { progress: number; minutes: string; goal: number }) {
  const R = 84
  const C = 2 * Math.PI * R
  const clamped = Math.min(progress, 1)

  return (
    <div className="relative mx-auto h-[212px] w-[212px]">
      <svg viewBox="0 0 200 200" className="h-full w-full -rotate-90">
        <defs>
          <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#3d5afe" />
            <stop offset="60%" stopColor="#7b61ff" />
            <stop offset="100%" stopColor="#e861ff" />
          </linearGradient>
        </defs>
        <circle cx="100" cy="100" r={R} fill="none" stroke="#e9e9f7" strokeWidth="16" />
        <circle
          cx="100"
          cy="100"
          r={R}
          fill="none"
          stroke="url(#ringGrad)"
          strokeWidth="16"
          strokeLinecap="round"
          strokeDasharray={C}
          strokeDashoffset={C * (1 - clamped)}
          className="transition-all duration-700 ease-out"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
          Bugun
        </span>
        <span className="mt-0.5 text-[34px] font-extrabold leading-none tracking-tight text-slate-900">
          {minutes}
        </span>
        <span className="mt-1 text-[12px] font-medium text-slate-400">
          maqsad {formatMinutes(goal)}
        </span>
      </div>
    </div>
  )
}

export function HomeTab() {
  const qc = useQueryClient()
  const setTab = useOpalStore((s) => s.setTab)
  const startSession = useOpalStore((s) => s.startSession)
  const activeSession = useOpalStore((s) => s.activeSession)

  const statsQ = useQuery<StatsResponse>({
    queryKey: ['stats'],
    queryFn: async () => (await fetch('/api/stats')).json(),
  })
  const profileQ = useQuery<UserProfile>({
    queryKey: ['profile'],
    queryFn: async () => (await fetch('/api/profile')).json(),
  })
  const appsQ = useQuery<BlockApp[]>({
    queryKey: ['apps'],
    queryFn: async () => (await fetch('/api/apps')).json(),
  })

  const protectionMutation = useMutation({
    mutationFn: async (enabled: boolean) => {
      const res = await fetch('/api/profile', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ protectionEnabled: enabled }),
      })
      return res.json()
    },
    onMutate: async (enabled) => {
      await qc.cancelQueries({ queryKey: ['profile'] })
      const prev = qc.getQueryData<UserProfile>(['profile'])
      if (prev) qc.setQueryData<UserProfile>(['profile'], { ...prev, protectionEnabled: enabled })
      return { prev }
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(['profile'], ctx.prev)
    },
    onSettled: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  })

  if (statsQ.isLoading || profileQ.isLoading) {
    return (
      <div className="space-y-5 p-5 pt-3">
        <Skeleton className="h-12 w-full rounded-2xl" />
        <Skeleton className="mx-auto h-[212px] w-[212px] rounded-full" />
        <Skeleton className="h-20 w-full rounded-3xl" />
        <Skeleton className="h-40 w-full rounded-3xl" />
      </div>
    )
  }

  const stats = statsQ.data
  const profile = profileQ.data
  const apps = appsQ.data ?? []
  if (!stats || !profile) return null

  const today = stats.today
  const progress = Math.min(today.screenTimeMinutes / Math.max(today.goalMinutes, 1), 1)
  const blockedApps = apps.filter((a) => a.blocked)
  const protectedNow = profile.protectionEnabled || !!activeSession
  const hour = new Date().getHours()
  const greeting = hour < 5 ? 'Xayrli tong' : hour < 12 ? 'Xayrli tong' : hour < 18 ? 'Xayrli kun' : 'Xayrli kech'

  return (
    <div className="animate-slide-up space-y-5 px-5 pb-6 pt-3">
      {/* header */}
      <header className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3d5afe] via-[#7b61ff] to-[#e861ff] text-lg font-black text-white shadow-md shadow-indigo-500/30">
            O
          </div>
          <div>
            <p className="text-[12px] font-medium text-slate-400">{greeting} 👋</p>
            <p className="text-[17px] font-bold leading-tight text-slate-900">{profile.name}</p>
          </div>
        </div>
        <div className="flex items-center gap-1.5 rounded-full bg-gradient-to-r from-orange-400/15 to-amber-400/15 px-3 py-1.5 ring-1 ring-orange-400/25">
          <Flame size={15} className="text-orange-500" strokeWidth={2.4} />
          <span className="text-[14px] font-bold text-orange-600">{profile.streakDays}</span>
          <span className="text-[11px] font-medium text-orange-500/80">kun</span>
        </div>
      </header>

      {/* protection status card */}
      <div
        className={cn(
          'relative overflow-hidden rounded-3xl p-4 text-white shadow-lg transition-all',
          protectedNow
            ? 'bg-gradient-to-br from-[#10123f] via-[#1b1e5c] to-[#3d2f86] shadow-indigo-900/30'
            : 'bg-gradient-to-br from-slate-700 via-slate-800 to-slate-900'
        )}
      >
        <div className="pointer-events-none absolute -right-10 -top-14 h-40 w-40 rounded-full bg-[#7b61ff]/30 blur-2xl" />
        <div className="relative flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className={cn(
                'flex h-11 w-11 items-center justify-center rounded-2xl',
                protectedNow ? 'bg-white/10' : 'bg-white/5'
              )}
            >
              {protectedNow ? (
                <ShieldCheck size={22} className="text-emerald-400" />
              ) : (
                <ShieldOff size={22} className="text-slate-400" />
              )}
            </div>
            <div>
              <p className="text-[15px] font-bold">
                {protectedNow ? 'Himoya faol' : 'Himoya o‘chirilgan'}
              </p>
              <p className="text-[12px] text-white/60">
                {blockedApps.length} ilova bloklangan
                {activeSession ? ` · ${activeSession.label} davom etmoqda` : ''}
              </p>
            </div>
          </div>
          <Switch
            checked={profile.protectionEnabled}
            onCheckedChange={(v) => protectionMutation.mutate(v)}
            disabled={!!activeSession}
            aria-label="Himoyani yoqish/o'chirish"
            className="data-[state=checked]:bg-emerald-500"
          />
        </div>
      </div>

      {/* ring */}
      <section aria-label="Bugungi ekran vaqti" className="rounded-[2rem] bg-white p-5 shadow-sm shadow-slate-200/60">
        <ProtectionRing progress={progress} minutes={formatMinutes(today.screenTimeMinutes)} goal={today.goalMinutes} />
        <div className="mt-4 grid grid-cols-2 gap-3">
          <div className="rounded-2xl bg-gradient-to-br from-emerald-50 to-teal-50 p-3.5 ring-1 ring-emerald-100">
            <div className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-emerald-600">
              <Sparkles size={13} /> Tejaldi
            </div>
            <p className="mt-1 text-xl font-extrabold text-emerald-700">
              {formatMinutes(today.savedMinutes)}
            </p>
          </div>
          <div className="rounded-2xl bg-gradient-to-br from-violet-50 to-fuchsia-50 p-3.5 ring-1 ring-violet-100">
            <div className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-violet-600">
              <Timer size={13} /> Sessiyalar
            </div>
            <p className="mt-1 text-xl font-extrabold text-violet-700">{profile.totalSessions}</p>
          </div>
        </div>
      </section>

      {/* CTA */}
      {activeSession ? (
        <button
          onClick={() => setTab('focus')}
          className="w-full rounded-3xl bg-gradient-to-r from-[#3d5afe] to-[#7b61ff] p-[1.5px] shadow-lg shadow-indigo-500/25 transition-transform active:scale-[0.98]"
        >
          <span className="flex items-center justify-between rounded-[calc(1.5rem-1.5px)] bg-white px-5 py-4">
            <span className="text-left">
              <span className="block text-[15px] font-bold text-slate-900">
                {activeSession.emoji} {activeSession.label} davom etmoqda
              </span>
              <span className="block text-[12px] text-slate-400">Taymerni ko‘rish uchun bosing</span>
            </span>
            <ChevronRight size={20} className="text-[#3d5afe]" />
          </span>
        </button>
      ) : (
        <button
          onClick={() => {
            startSession({
              sessionId: 'preview',
              type: 'DEEP_FOCUS',
              label: 'Deep Focus',
              emoji: '🧠',
              durationMinutes: 45,
              startedAt: Date.now(),
              blockedApps: [],
            })
            setTab('focus')
          }}
          className="group relative w-full overflow-hidden rounded-3xl bg-gradient-to-r from-[#3d5afe] via-[#7b61ff] to-[#e861ff] py-4 text-white shadow-xl shadow-indigo-500/30 transition-transform active:scale-[0.98]"
        >
          <span className="absolute inset-0 animate-shimmer" />
          <span className="relative flex items-center justify-center gap-2 text-[16px] font-bold">
            Fokus sessiyasini boshlash
          </span>
        </button>
      )}

      {/* blocked apps strip */}
      {blockedApps.length > 0 && (
        <section aria-label="Bloklangan ilovalar">
          <div className="mb-2.5 flex items-center justify-between px-1">
            <h3 className="text-[14px] font-bold text-slate-800">Bloklangan ilovalar</h3>
            <button
              onClick={() => setTab('apps')}
              className="flex items-center gap-0.5 text-[12px] font-semibold text-[#3d5afe]"
            >
              Barchasi <ChevronRight size={14} />
            </button>
          </div>
          <div className="no-scrollbar flex gap-3 overflow-x-auto pb-1">
            {blockedApps.slice(0, 8).map((a) => (
              <div
                key={a.id}
                className="flex w-[76px] shrink-0 flex-col items-center gap-1.5 rounded-2xl bg-white p-3 shadow-sm shadow-slate-200/60"
              >
                <div
                  className={cn(
                    'flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br text-xl shadow-sm',
                    a.gradient
                  )}
                >
                  {a.emoji}
                </div>
                <span className="w-full truncate text-center text-[10.5px] font-semibold text-slate-600">
                  {a.name}
                </span>
                <span className="rounded-full bg-rose-50 px-2 py-0.5 text-[9px] font-bold text-rose-500 ring-1 ring-rose-100">
                  BLOK
                </span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
