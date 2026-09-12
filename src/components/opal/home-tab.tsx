'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { useOpalStore } from '@/lib/opal-store'
import { computeScores, GLASS } from '@/lib/opal-ui'
import { formatMinutes, type FocusSession, type StatsResponse, type UserProfile, type BlockApp } from '@/lib/opal-types'
import { cn } from '@/lib/utils'
import { ChevronRight, Play, Moon, TreePine, Sigma, ShieldOff } from 'lucide-react'

function useOpalData() {
  const statsQ = useQuery<StatsResponse>({
    queryKey: ['stats'],
    queryFn: async () => (await fetch('/api/stats')).json(),
  })
  const profileQ = useQuery<UserProfile>({
    queryKey: ['profile'],
    queryFn: async () => (await fetch('/api/profile')).json(),
  })
  const sessionsQ = useQuery<FocusSession[]>({
    queryKey: ['sessions'],
    queryFn: async () => (await fetch('/api/sessions')).json(),
  })
  return { statsQ, profileQ, sessionsQ }
}

/** Kichik dumaloq progress halqasi (Sleep/Focus/Rest pilllari uchun) */
function MiniRing({ value, icon }: { value: number; icon: React.ReactNode }) {
  const R = 13
  const C = 2 * Math.PI * R
  return (
    <span className="relative inline-flex h-8 w-8 items-center justify-center">
      <svg viewBox="0 0 32 32" className="absolute inset-0 h-full w-full -rotate-90">
        <circle cx="16" cy="16" r={R} fill="none" stroke="rgba(255,255,255,0.14)" strokeWidth="2.4" />
        <circle
          cx="16"
          cy="16"
          r={R}
          fill="none"
          stroke="#8fd9ff"
          strokeWidth="2.4"
          strokeLinecap="round"
          strokeDasharray={C}
          strokeDashoffset={C * (1 - value / 100)}
          style={{ filter: 'drop-shadow(0 0 3px rgba(143,217,255,0.6))' }}
        />
      </svg>
      <span className="relative text-white/90">{icon}</span>
    </span>
  )
}

export function HomeTab() {
  const { statsQ, profileQ, sessionsQ } = useOpalData()
  const qc = useQueryClient()
  const setTab = useOpalStore((s) => s.setTab)
  const setTimerDraft = useOpalStore((s) => s.setTimerDraft)
  const setBreathingOpen = useOpalStore((s) => s.setBreathingOpen)
  const setBlockedView = useOpalStore((s) => s.setBlockedView)

  const [navigating, setNavigating] = useState(false)

  const profile = profileQ.data
  const stats = statsQ.data
  const scores = computeScores(stats, sessionsQ.data)
  const activeSession = useOpalStore((s) => s.activeSession)

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

  const blockedAppsQ = useQuery<BlockApp[]>({
    queryKey: ['apps', 'blocked'],
    queryFn: async () => (await fetch('/api/apps?blocked=1')).json(),
  })

  const loading = statsQ.isLoading || profileQ.isLoading
  const today = stats?.today
  const overGoal = today && today.screenTimeMinutes > today.goalMinutes
  const savedToday = today?.savedMinutes ?? 0
  const screenDelta = stats && today ? today.screenTimeMinutes - stats.avgDailyScreenMinutes : 0

  const startFocus = () => {
    if (activeSession) {
      setTab('timer')
      return
    }
    setNavigating(true)
    setTimerDraft({ type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', durationMinutes: 45, strict: false })
    setTab('timer')
    setTimeout(() => setNavigating(false), 700)
  }

  const openTimer = () => {
    setTab('timer')
  }

  return (
    <div className="relative flex min-h-full flex-col px-5 pb-4 pt-1">
      {/* header */}
      <header className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span
            className="inline-block h-6 w-6 rounded-[7px] bg-gradient-to-br from-[#8fd9ff] via-[#b18cff] to-[#ff9ad5] shadow-[0_0_12px_rgba(143,217,255,0.5)]"
            aria-hidden="true"
          />
          <span className="text-[19px] font-bold tracking-tight text-white">Opal</span>
        </div>
        <div className="flex items-center gap-2.5">
          {profile && (
            <span className="flex items-center gap-1 rounded-full bg-white/[0.07] px-2.5 py-1 text-[12px] font-bold text-orange-300 ring-1 ring-white/10">
              🔥 {profile.streakDays}
            </span>
          )}
          <motion.button
            whileTap={{ scale: 0.92 }}
            onClick={() => window.dispatchEvent(new CustomEvent('opal:open-profile'))}
            aria-label="Profil"
            className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-[#5b7bff] via-[#8b7bff] to-[#c86bff] text-[12px] font-black text-white shadow-[0_0_14px_rgba(139,123,255,0.45)] ring-1 ring-white/25"
          >
            {profile ? profile.name[0] : 'A'}
          </motion.button>
        </div>
      </header>

      {loading ? (
        <div className="mt-8 space-y-5">
          <Skeleton className="mx-auto h-56 w-56 rounded-full bg-white/5" />
          <Skeleton className="h-20 w-full rounded-3xl bg-white/5" />
          <Skeleton className="h-32 w-full rounded-3xl bg-white/5" />
        </div>
      ) : (
        <>
          {/* ── Kristall qahramon ─────────────────────────── */}
          <div className="relative mt-2 flex flex-col items-center">
            {/* yulduzli glow */}
            <div
              className="pointer-events-none absolute left-1/2 top-2 h-64 w-64 -translate-x-1/2 rounded-full opacity-70"
              style={{
                background:
                  'radial-gradient(circle, rgba(125,211,252,0.16) 0%, rgba(177,140,255,0.10) 45%, transparent 70%)',
              }}
              aria-hidden="true"
            />
            <motion.div
              animate={{ y: [0, -10, 0], rotate: [0, 1.2, 0] }}
              transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
              className="relative z-10 mt-3"
            >
              {/* kristal tasviri — qora fon screen blend bilan eriydi */}
              <button
                onClick={openTimer}
                aria-label="Taymer sahifasini ochish"
                className="block cursor-pointer rounded-full"
              >
                <img
                  src="/opal/crystal.png"
                  alt=""
                  className="h-[190px] w-[190px] rounded-full object-cover mix-blend-screen"
                  style={{ filter: 'saturate(1.15) brightness(1.08) drop-shadow(0 0 34px rgba(140,120,255,0.35))' }}
                  draggable={false}
                />
              </button>
              {/* yon parilklar */}
              {[
                { left: '4%', top: '22%', size: 5, delay: 0 },
                { right: '2%', top: '12%', size: 4, delay: 1.4 },
                { right: '10%', bottom: '30%', size: 5, delay: 2.6 },
                { left: '12%', bottom: '18%', size: 3.5, delay: 0.8 },
              ].map((p, i) => (
                <motion.span
                  key={i}
                  className="absolute rounded-full bg-[#bfe9ff]"
                  style={{
                    ...p,
                    width: p.size,
                    height: p.size,
                    boxShadow: '0 0 6px rgba(191,233,255,0.9)',
                  }}
                  animate={{ opacity: [0.15, 0.95, 0.15], y: [0, -6, 0] }}
                  transition={{ duration: 3.2, repeat: Infinity, delay: p.delay }}
                  aria-hidden="true"
                />
              ))}
            </motion.div>
            {/* poydevor soya */}
            <div
              className="mt-1 h-4 w-40 rounded-[100%] bg-black/60 blur-md"
              aria-hidden="true"
            />

            {/* Score */}
            <div className="relative z-10 -mt-1 flex flex-col items-center">
              <span className="text-[12px] font-medium tracking-wide text-white/55">Score</span>
              <div className="flex items-start gap-1.5">
                <motion.span
                  key={scores.score}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="text-[52px] font-extrabold leading-none tracking-tight text-[#a5e3ff]"
                  style={{ textShadow: '0 0 24px rgba(125,211,252,0.55)' }}
                >
                  {scores.score}
                </motion.span>
                <span
                  className={cn(
                    'mt-2 text-[15px] font-bold',
                    scores.delta >= 0 ? 'text-emerald-400' : 'text-rose-400'
                  )}
                  aria-label={scores.delta >= 0 ? 'yaxshilandi' : 'pasaydi'}
                >
                  {scores.delta >= 0 ? '▲' : '▼'}
                </span>
              </div>
            </div>

            {/* Sub-metrik pilllar */}
            <div className="relative z-10 mt-4 flex items-center gap-2.5">
              {[
                { icon: <Sigma size={13} />, label: 'Focus', value: scores.focus },
                { icon: <TreePine size={13} />, label: 'Rest', value: scores.rest },
                { icon: <Moon size={13} />, label: 'Sleep', value: scores.sleep },
              ].map((m) => (
                <button
                  key={m.label}
                  onClick={() => window.dispatchEvent(new CustomEvent('opal:open-today'))}
                  className={cn(
                    GLASS,
                    'flex items-center gap-2 rounded-full py-1.5 pl-1.5 pr-3.5 transition-transform active:scale-95'
                  )}
                >
                  <MiniRing value={m.value} icon={m.icon} />
                  <span className="flex flex-col items-start leading-tight">
                    <span className="text-[13px] font-bold text-white">{m.value}</span>
                    <span className="text-[9.5px] font-semibold uppercase tracking-wide text-white/50">
                      {m.label}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          </div>

          {/* ── Tezkor holat kartasi ─────────────────────── */}
          <section className={cn(GLASS, 'mt-5 p-4')} aria-label="Bugungi holat">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase tracking-widest text-white/45">
                  Bugun
                </p>
                <p className="mt-0.5 flex flex-wrap items-baseline gap-x-2 text-[21px] font-extrabold text-white">
                  {formatMinutes(today?.screenTimeMinutes ?? 0)}
                  <span className="whitespace-nowrap text-[12.5px] font-semibold text-white/45">
                    Screen Time
                  </span>
                </p>
              </div>
              {today && (
                <span
                  className={cn(
                    'flex shrink-0 items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-1 text-[11px] font-bold ring-1',
                    overGoal
                      ? 'bg-rose-500/15 text-rose-300 ring-rose-400/30'
                      : 'bg-emerald-500/15 text-emerald-300 ring-emerald-400/30'
                  )}
                >
                  {overGoal
                    ? '▲ oshdi'
                    : `${screenDelta <= 0 ? '▼' : '▲'} ${formatMinutes(Math.abs(screenDelta))}`}
                </span>
              )}
            </div>

            {/* ekran vaqti mini-bar */}
            <div className="mt-3">
              <div className="h-2 overflow-hidden rounded-full bg-white/10">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{
                    width: `${Math.min(((today?.screenTimeMinutes ?? 0) / (today?.goalMinutes || 240)) * 100, 100)}%`,
                  }}
                  transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1], delay: 0.2 }}
                  className={cn(
                    'h-full rounded-full',
                    overGoal
                      ? 'bg-gradient-to-r from-rose-400 to-rose-500'
                      : 'bg-gradient-to-r from-teal-300 to-emerald-400'
                  )}
                  style={{ boxShadow: '0 0 12px rgba(94,234,212,0.4)' }}
                />
              </div>
              <div className="mt-1.5 flex justify-between text-[10px] font-semibold text-white/40">
                <span>AVG</span>
                <span>Maqsad {formatMinutes(stats?.goalMinutes ?? 240)}</span>
              </div>
            </div>

            <div className="mt-3.5 flex items-center justify-between border-t border-white/8 pt-3">
              <div className="flex items-center gap-2">
                {profile?.protectionEnabled ? (
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-300 ring-1 ring-emerald-400/30">
                    🛡️
                  </span>
                ) : (
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-white/8 text-white/50 ring-1 ring-white/10">
                    <ShieldOff size={13} />
                  </span>
                )}
                <div className="leading-tight">
                  <p className="text-[12.5px] font-bold text-white">Himoya</p>
                  <p className="text-[10px] font-medium text-white/45">
                    {profile?.protectionEnabled ? 'Chalg‘ituvchilar bloklangan' : 'O‘chirilgan'}
                  </p>
                </div>
              </div>
              <Switch
                checked={profile?.protectionEnabled ?? false}
                onCheckedChange={(v) => protectionMutation.mutate(v)}
                aria-label="Himoyani yoqish/o'chirish"
                className="data-[state=checked]:bg-emerald-500/80"
              />
            </div>
          </section>

          {/* ── Start Timer CTA ──────────────────────────── */}
          <div className="mt-3.5 flex gap-2.5">
            <motion.button
              whileTap={{ scale: 0.97 }}
              onClick={startFocus}
              disabled={navigating}
              className="relative flex flex-[1.4] items-center justify-center gap-2 overflow-hidden rounded-2xl border border-white/15 bg-gradient-to-b from-[#cfe3f4]/25 via-[#9fb8d8]/20 to-[#5f7ba6]/25 py-3.5 text-[15px] font-bold text-white shadow-[0_10px_30px_rgba(60,90,140,0.35),inset_0_1px_0_0_rgba(255,255,255,0.25)] backdrop-blur-xl active:scale-[0.98]"
            >
              <Play size={15} className="fill-white" />
              {activeSession ? 'Jonli taymer' : 'Start Timer'}
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => setBreathingOpen(true)}
              aria-label="1 daqiqalik nafas mashqi"
              className="flex flex-1 items-center justify-center gap-1.5 rounded-2xl border border-white/10 bg-white/[0.05] py-3.5 text-[12.5px] font-bold text-white/75 backdrop-blur-xl active:scale-[0.98]"
            >
              🌿 1 daq
            </motion.button>
          </div>

          {/* ── My Apps strip ────────────────────────────── */}
          <section className="mt-5" aria-label="Ilovalarim">
            <button
              onClick={() => setTab('apps')}
              className="mb-2.5 flex w-full items-center justify-between"
              aria-label="Ilovalarni boshqarish"
            >
              <span className="text-[14.5px] font-bold text-white/90">My Apps</span>
              <ChevronRight size={15} className="text-white/35" />
            </button>
            <div className="no-scrollbar flex gap-3 overflow-x-auto pb-1">
              {(blockedAppsQ.data ?? []).slice(0, 8).map((app) => (
                <motion.button
                  key={app.id}
                  whileTap={{ scale: 0.92 }}
                  onClick={() =>
                    setBlockedView({ name: app.name, emoji: app.emoji, gradient: app.gradient })
                  }
                  className="flex w-[62px] shrink-0 flex-col items-center gap-1.5"
                  aria-label={`${app.name} bloklangan`}
                >
                  <span
                    className={cn(
                      'relative flex h-[54px] w-[54px] items-center justify-center rounded-[18px] text-[24px] ring-1 ring-[#8fd9ff]/45',
                      app.gradient
                    )}
                    style={{ boxShadow: '0 0 16px rgba(125,211,252,0.35)' }}
                  >
                    <span className="drop-shadow">{app.emoji}</span>
                    <span className="absolute -bottom-1 -right-1 flex h-[18px] w-[18px] items-center justify-center rounded-full bg-[#0c1120] text-[9px] ring-1 ring-[#8fd9ff]/50">
                      🔒
                    </span>
                  </span>
                  <span className="w-full truncate text-center text-[10px] font-semibold text-[#9fd8ff]">
                    Unblock
                  </span>
                </motion.button>
              ))}
              {(blockedAppsQ.data ?? []).length === 0 && !blockedAppsQ.isLoading && (
                <button
                  onClick={() => setTab('apps')}
                  className="flex w-full items-center justify-center gap-2 rounded-2xl border border-dashed border-white/15 py-5 text-[12.5px] font-semibold text-white/45"
                >
                  Hech narsa bloklangan emas — ilova qo‘shish
                  <ChevronRight size={13} />
                </button>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  )
}
