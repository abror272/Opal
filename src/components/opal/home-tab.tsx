'use client'

import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { useOpalStore } from '@/lib/opal-store'
import { computeScores, pickSuggestion, gemsFor, GLASS, OPAL } from '@/lib/opal-ui'
import { formatMinutes, type FocusSession, type StatsResponse, type UserProfile, type BlockApp } from '@/lib/opal-types'
import { cn } from '@/lib/utils'
import {
  ChevronRight,
  Play,
  Moon,
  TreePine,
  Hourglass,
  ShieldOff,
  Flame,
  MoreHorizontal,
} from 'lucide-react'
import { ScorePill, ScoreBracket } from './score-pill'
import { OpalWordmark, HexAvatarButton } from './brand'

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

export function HomeTab() {
  const { statsQ, profileQ, sessionsQ } = useOpalData()
  const qc = useQueryClient()
  const setTab = useOpalStore((s) => s.setTab)
  const setTimerDraft = useOpalStore((s) => s.setTimerDraft)
  const setBreathingOpen = useOpalStore((s) => s.setBreathingOpen)
  const setBlockedView = useOpalStore((s) => s.setBlockedView)

  const profile = profileQ.data
  const stats = statsQ.data
  const scores = computeScores(stats, sessionsQ.data)
  const activeSession = useOpalStore((s) => s.activeSession)

  const [sugOffset, setSugOffset] = useState(0)
  const suggestion = pickSuggestion(stats, new Date(), sugOffset)
  const cycleSuggestion = () => setSugOffset((n) => n + 1)
  const todayIso = new Date().toISOString().slice(0, 10)
  const hadSleep = (sessionsQ.data ?? []).some(
    (s) => s.type === 'SLEEP' && (s.endedAt ?? s.startedAt).slice(0, 10) === todayIso
  )
  const gems = useMemo(() => gemsFor(profile, hadSleep), [profile, hadSleep])

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
  const allAppsQ = useQuery<BlockApp[]>({
    queryKey: ['apps'],
    queryFn: async () => (await fetch('/api/apps')).json(),
  })

  const loading = statsQ.isLoading || profileQ.isLoading
  const today = stats?.today
  const overGoal = today && today.screenTimeMinutes > today.goalMinutes
  const allowedCount = (allAppsQ.data ?? []).filter((a) => !a.blocked).length

  const startFocus = () => {
    if (activeSession) {
      setTab('timer')
      return
    }
    setTimerDraft({ type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', durationMinutes: 45, strict: false })
    setTab('timer')
  }

  const openTimer = () => {
    setTab('timer')
  }

  const runSuggestion = () => {
    if (suggestion.action === 'breathe') {
      setBreathingOpen(true)
    } else if (suggestion.action === 'apps') {
      setTab('apps')
    } else if (suggestion.timer) {
      if (activeSession) {
        setTab('timer')
        return
      }
      setTimerDraft({
        type: suggestion.timer.type,
        label: suggestion.timer.label,
        emoji: suggestion.timer.emoji,
        durationMinutes: suggestion.timer.durationMinutes,
        strict: false,
      })
      setTab('timer')
    }
  }

  const categoryIcon =
    suggestion.category === 'Sleep' ? (
      <Moon size={14} className="text-[#cdb9ff]" />
    ) : suggestion.category === 'Focus' ? (
      <Hourglass size={14} className="text-[#9fe8c0]" />
    ) : (
      <TreePine size={14} className="text-[#9fe8c0]" />
    )

  return (
    <div className="relative flex min-h-full flex-col px-5 pb-4 pt-1">
      {/* header — haqiqiy Opal: wordmark / olov streak / hexagon avatar */}
      <header className="flex items-center justify-between">
        <OpalWordmark />
        <div className="flex items-center gap-3">
          {profile && (
            <span
              className="flex items-center gap-1 text-[15px] font-bold"
              style={{ color: OPAL.flame }}
              aria-label={`${profile.streakDays} kunlik streak`}
            >
              <Flame size={16} className="fill-[#ffb85c]/40 text-[#ffc46b]" />
              {profile.streakDays}
            </span>
          )}
          <HexAvatarButton onClick={() => window.dispatchEvent(new CustomEvent('opal:open-profile'))} />
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
          {/* ── Kristall qahramon (tosh poydevor ustida, mint nur) ── */}
          <div className="relative mt-1 flex flex-col items-center">
            {/* mint ambient glow (haqiqiy Opal kanon yorug'ligi) */}
            <div
              className="pointer-events-none absolute left-1/2 top-3 h-60 w-72 -translate-x-1/2 rounded-full opacity-80"
              style={{
                background:
                  'radial-gradient(ellipse 50% 42% at 50% 46%, rgba(150,240,190,0.20) 0%, rgba(94,234,212,0.10) 48%, transparent 72%)',
              }}
              aria-hidden="true"
            />
            <motion.div
              animate={{ y: [0, -9, 0] }}
              transition={{ duration: 6.5, repeat: Infinity, ease: 'easeInOut' }}
              className="relative z-10 mt-2"
            >
              <button
                onClick={openTimer}
                aria-label="Taymer sahifasini ochish"
                className="block cursor-pointer rounded-full"
              >
                <img
                  src="/opal/crystal.png"
                  alt=""
                  className="h-[172px] w-[172px] rounded-full object-cover mix-blend-screen"
                  style={{
                    filter:
                      'saturate(1.12) brightness(1.06) drop-shadow(0 0 30px rgba(150,240,190,0.4))',
                    WebkitMaskImage:
                      'radial-gradient(circle, black 52%, transparent 68%)',
                    maskImage:
                      'radial-gradient(circle, black 52%, transparent 68%)',
                  }}
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
                  className="absolute rounded-full bg-[#d9ffe8]"
                  style={{
                    ...p,
                    width: p.size,
                    height: p.size,
                    boxShadow: '0 0 6px rgba(217,255,232,0.9)',
                  }}
                  animate={{ opacity: [0.15, 0.95, 0.15], y: [0, -6, 0] }}
                  transition={{ duration: 3.2, repeat: Infinity, delay: p.delay }}
                  aria-hidden="true"
                />
              ))}
            </motion.div>
            {/* tosh poydevor (haqiqiy Opal'ning tosh taxtasi) */}
            <div className="relative -mt-2 flex flex-col items-center" aria-hidden="true">
              <div
                className="h-[26px] w-[128px] rounded-[10px]"
                style={{
                  background:
                    'linear-gradient(180deg, #262b28 0%, #14171a 55%, #0a0c0e 100%)',
                  boxShadow:
                    'inset 0 2px 3px rgba(255,255,255,0.10), inset 0 -3px 6px rgba(0,0,0,0.7), 0 6px 18px rgba(0,0,0,0.6)',
                }}
              />
              <div className="h-3 w-[168px] rounded-[100%] bg-black/70 blur-[6px]" />
            </div>

            {/* Score */}
            <div className="relative z-10 mt-2 flex flex-col items-center">
              <span className="text-[12.5px] font-medium tracking-wide text-white/60">Score</span>
              <div className="flex items-start gap-1.5">
                <motion.span
                  key={scores.score}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="text-[54px] font-extrabold leading-none tracking-tight"
                  style={{ color: OPAL.mint, textShadow: `0 0 26px ${OPAL.mintGlow}` }}
                >
                  {scores.score}
                </motion.span>
                <span
                  className={cn(
                    'mt-2 text-[15px] font-bold',
                    scores.delta >= 0 ? 'text-[#9fe8b5]' : 'text-rose-400'
                  )}
                  aria-label={scores.delta >= 0 ? 'yaxshilandi' : 'pasaydi'}
                >
                  {scores.delta >= 0 ? '▲' : '▼'}
                </span>
              </div>
            </div>

            {/* bracket → pilllar */}
            <ScoreBracket width={216} />
            <div className="relative z-10 flex items-start justify-center gap-3">
              <ScorePill value={scores.sleep} icon={<Moon />} label="Sleep" delay={0.05} onClick={() => window.dispatchEvent(new CustomEvent('opal:open-today'))} />
              <ScorePill value={scores.focus} icon={<Hourglass />} label="Focus" delay={0.12} onClick={() => window.dispatchEvent(new CustomEvent('opal:open-today'))} />
              <ScorePill value={scores.rest} icon={<TreePine />} label="Rest" delay={0.19} onClick={() => window.dispatchEvent(new CustomEvent('opal:open-today'))} />
            </div>
          </div>

          {/* ── TAVSIYA KARTASI (haqiqiy Opal imzo glass kartasi) ── */}
          <section className="relative z-10 mt-5" aria-label="Kunlik tavsiya">
            <div className={cn(GLASS, 'overflow-hidden p-4')}>
              {/* header: kategoriya / teg */}
              <div className="flex items-center justify-between">
                <p className="flex items-center gap-1.5 text-[12.5px] font-bold text-white/75">
                  {categoryIcon}
                  {suggestion.category}
                  <span className="text-white/30">/</span>
                  <span className="font-semibold text-white/55">{suggestion.tag}</span>
                </p>
                <button
                  aria-label="Boshqa tavsiya"
                  className="text-white/30 transition-colors hover:text-white/60"
                  onClick={cycleSuggestion}
                >
                  <MoreHorizontal size={17} />
                </button>
              </div>

              <div className="mt-2 flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-[16.5px] font-bold leading-snug text-white">{suggestion.title}</p>
                  <p className="mt-1 text-[12.5px] leading-relaxed text-white/50">{suggestion.body}</p>
                </div>
                {/* illyustratsiya */}
                <div className="relative flex h-14 w-14 shrink-0 items-center justify-center">
                  <div
                    className="absolute inset-0 rounded-full"
                    style={{
                      background:
                        'radial-gradient(circle, rgba(150,240,190,0.22) 0%, transparent 70%)',
                    }}
                    aria-hidden="true"
                  />
                  <span className="relative text-[30px]" aria-hidden="true">
                    {suggestion.category === 'Sleep' ? '🌙' : suggestion.category === 'Focus' ? '🎯' : '🌿'}
                  </span>
                </div>
              </div>

              {/* CTA — to'q frosted to'liq kenglik tugma */}
              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={runSuggestion}
                className="mt-3.5 flex w-full items-center justify-center gap-2 rounded-2xl border border-white/12 bg-white/[0.09] py-3 text-[13.5px] font-bold text-white shadow-[inset_0_1px_0_0_rgba(255,255,255,0.12)] backdrop-blur-md active:scale-[0.98]"
              >
                <Play size={13} className="fill-white" />
                {suggestion.cta}
              </motion.button>
            </div>

            {/* N allowed — kartaning pastki qirrasiga osilgan pill */}
            <button
              onClick={() => setTab('apps')}
              className="absolute -bottom-4 left-1/2 z-20 flex -translate-x-1/2 items-center gap-1.5 rounded-full border border-white/12 bg-[#101513]/95 py-1.5 pl-2 pr-3 text-[12px] font-bold text-white shadow-[0_6px_20px_rgba(0,0,0,0.5)] backdrop-blur-xl transition-transform active:scale-95"
              aria-label={`${allowedCount} ta ilovaga ruxsat berilgan`}
            >
              <span className="flex -space-x-1.5" aria-hidden="true">
                {(allAppsQ.data ?? [])
                  .filter((a) => !a.blocked)
                  .slice(0, 3)
                  .map((a) => (
                    <span
                      key={a.id}
                      className={cn(
                        'flex h-[18px] w-[18px] items-center justify-center rounded-[6px] text-[9px] ring-1 ring-black/50',
                        a.gradient
                      )}
                    >
                      {a.emoji}
                    </span>
                  ))}
              </span>
              {allowedCount} allowed
              <ChevronRight size={13} className="text-white/50" />
            </button>
          </section>

          {/* ── Tezkor holat: Screen time + Himoya (2 ustun) ── */}
          <section className="mt-8 grid grid-cols-[1.5fr_1fr] gap-2.5" aria-label="Bugungi holat">
            <button
              onClick={() => window.dispatchEvent(new CustomEvent('opal:open-today'))}
              className={cn(GLASS, 'flex flex-col justify-between p-3.5 text-left transition-transform active:scale-[0.98]')}
            >
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-white/40">Bugun</p>
                <p className="mt-1 text-[19px] font-extrabold leading-none text-white">
                  {formatMinutes(today?.screenTimeMinutes ?? 0)}
                  <span className="ml-1.5 text-[11px] font-semibold text-white/40">Screen Time</span>
                </p>
              </div>
              {today && (
                <span
                  className={cn(
                    'mt-2 inline-flex w-fit items-center gap-1 rounded-full px-2 py-0.5 text-[10.5px] font-bold ring-1',
                    overGoal
                      ? 'bg-rose-500/15 text-rose-300 ring-rose-400/30'
                      : 'bg-emerald-500/15 text-emerald-300 ring-emerald-400/30'
                  )}
                >
                  {overGoal ? '▲ maqsaddan oshdi' : `▼ o‘rtachadan ${formatMinutes(Math.abs(today.screenTimeMinutes - (stats?.avgDailyScreenMinutes ?? 0)))}`}
                </span>
              )}
            </button>

            <div className={cn(GLASS, 'flex flex-col justify-between p-3.5')}>
              <div className="flex items-center gap-2">
                {profile?.protectionEnabled ? (
                  <span
                    className="flex h-6 w-6 items-center justify-center rounded-full bg-emerald-500/15 text-[11px] text-emerald-300 ring-1 ring-emerald-400/30"
                    aria-hidden="true"
                  >
                    🛡️
                  </span>
                ) : (
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white/8 text-white/50 ring-1 ring-white/10">
                    <ShieldOff size={12} />
                  </span>
                )}
                <p className="text-[12.5px] font-bold text-white">Himoya</p>
              </div>
              <div className="mt-2 flex items-center justify-between gap-2">
                <p className="text-[9.5px] font-medium leading-tight text-white/40">
                  {profile?.protectionEnabled ? 'Bloklangan' : 'O‘chirilgan'}
                </p>
                <Switch
                  checked={profile?.protectionEnabled ?? false}
                  onCheckedChange={(v) => protectionMutation.mutate(v)}
                  aria-label="Himoyani yoqish/o'chirish"
                  className="data-[state=checked]:bg-emerald-500/80"
                />
              </div>
            </div>
          </section>

          {/* ── Start Timer CTA ──────────────────────────── */}
          <div className="mt-3 flex gap-2.5">
            <motion.button
              whileTap={{ scale: 0.97 }}
              onClick={startFocus}
              className="relative flex flex-[1.4] items-center justify-center gap-2 overflow-hidden rounded-2xl border border-white/15 bg-gradient-to-b from-[#d9efe2]/22 via-[#9fc4ad]/18 to-[#4f7a63]/25 py-3.5 text-[15px] font-bold text-white shadow-[0_10px_30px_rgba(40,90,65,0.35),inset_0_1px_0_0_rgba(255,255,255,0.25)] backdrop-blur-xl active:scale-[0.98]"
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
                      'relative flex h-[54px] w-[54px] items-center justify-center rounded-[18px] text-[24px] ring-1 ring-[#9fe8b5]/45',
                      app.gradient
                    )}
                    style={{ boxShadow: '0 0 16px rgba(150,240,190,0.30)' }}
                  >
                    <span className="drop-shadow">{app.emoji}</span>
                    <span className="absolute -bottom-1 -right-1 flex h-[18px] w-[18px] items-center justify-center rounded-full bg-[#0c1410] text-[9px] ring-1 ring-[#9fe8b5]/50">
                      🔒
                    </span>
                  </span>
                  <span className="w-full truncate text-center text-[10px] font-semibold text-[#b8ecc9]">
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

          {/* ── Gemstones teaser (profilga yo'naltiradi) ── */}
          <section className="mt-5" aria-label="Gemstones">
            <button
              onClick={() => window.dispatchEvent(new CustomEvent('opal:open-profile'))}
              className="mb-2.5 flex w-full items-center justify-between"
              aria-label="Gemstones'ni ochish"
            >
              <span className="text-[14.5px] font-bold text-white/90">Gemstones</span>
              <span className="text-[11px] font-semibold text-white/40">
                {gems.filter((g) => g.unlocked).length}/{gems.length}
              </span>
            </button>
            <div className="no-scrollbar flex gap-3 overflow-x-auto pb-1">
              {gems.slice(0, 5).map((g) => (
                <div key={g.key} className="flex w-[64px] shrink-0 flex-col items-center gap-1">
                  <span
                    className={cn(
                      'relative flex h-[46px] w-[46px] items-center justify-center rounded-[16px]',
                      !g.unlocked && 'opacity-35 grayscale'
                    )}
                    style={{
                      background: `radial-gradient(circle at 35% 30%, ${g.colors[0]} 0%, ${g.colors[1]} 45%, ${g.colors[2]} 100%)`,
                      boxShadow: g.unlocked ? `0 0 16px ${g.colors[1]}66` : 'none',
                    }}
                    aria-hidden="true"
                  >
                    <span className="absolute left-[22%] top-[18%] h-2 w-2.5 rounded-full bg-white/50 blur-[2px]" />
                  </span>
                  <span className="text-[9.5px] font-semibold text-white/55">{g.name}</span>
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}
