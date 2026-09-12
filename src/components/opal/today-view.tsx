'use client'

import { useQuery } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import {
  Bar,
  BarChart,
  Cell,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { BlockApp, DailyStat, FocusSession, StatsResponse, UserProfile } from '@/lib/opal-types'
import { computeScores, GLASS } from '@/lib/opal-ui'
import { dayLabel, formatMinutes } from '@/lib/opal-types'
import { LiveLeaderboard } from './live-leaderboard'
import { Moon, TreePine, Sigma, ChevronLeft, ChevronRight, ScreenShare } from 'lucide-react'
import { cn } from '@/lib/utils'

function MetricBubble({
  icon,
  label,
  value,
  delay,
}: {
  icon: React.ReactNode
  label: string
  value: number
  delay: number
}) {
  const R = 15
  const C = 2 * Math.PI * R
  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.4 }}
      className={cn(GLASS, 'flex flex-col items-center gap-1.5 rounded-full px-4 py-3')}
    >
      <span className="relative inline-flex h-10 w-10 items-center justify-center">
        <svg viewBox="0 0 36 36" className="absolute inset-0 h-full w-full -rotate-90">
          <circle cx="18" cy="18" r={R} fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="2.6" />
          <circle
            cx="18"
            cy="18"
            r={R}
            fill="none"
            stroke="#8fd9ff"
            strokeWidth="2.6"
            strokeLinecap="round"
            strokeDasharray={C}
            strokeDashoffset={C * (1 - value / 100)}
            style={{ filter: 'drop-shadow(0 0 4px rgba(143,217,255,0.65))' }}
          />
        </svg>
        <span className="text-white/85">{icon}</span>
      </span>
      <span className="text-[15px] font-extrabold leading-none text-white">{value}</span>
      <span className="text-[9.5px] font-semibold uppercase tracking-wide text-white/45">{label}</span>
    </motion.div>
  )
}

/** Yarim arc gauge (haqiqiy Opal Score vizuali) */
function ScoreArc({ score, delta }: { score: number; delta: number }) {
  const R = 88
  const LEN = Math.PI * R
  return (
    <div className="relative mx-auto w-[240px]">
      <svg viewBox="0 0 220 128" className="w-full">
        <defs>
          <linearGradient id="arcGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#5b9bd5" />
            <stop offset="50%" stopColor="#8fd9ff" />
            <stop offset="100%" stopColor="#b18cff" />
          </linearGradient>
        </defs>
        {/* fon arc */}
        <path
          d={`M 22 118 A ${R} ${R} 0 0 1 198 118`}
          fill="none"
          stroke="rgba(255,255,255,0.1)"
          strokeWidth="7"
          strokeLinecap="round"
        />
        {/* qiymat arc */}
        <motion.path
          d={`M 22 118 A ${R} ${R} 0 0 1 198 118`}
          fill="none"
          stroke="url(#arcGrad)"
          strokeWidth="7"
          strokeLinecap="round"
          strokeDasharray={LEN}
          initial={{ strokeDashoffset: LEN }}
          animate={{ strokeDashoffset: LEN * (1 - score / 100) }}
          transition={{ duration: 1.2, ease: [0.22, 1, 0.36, 1] }}
          style={{ filter: 'drop-shadow(0 0 10px rgba(143,217,255,0.55))' }}
        />
        {/* bracket (haqiqiy Opal'dagi kabi) */}
        <path
          d="M 62 122 Q 110 138 158 122"
          fill="none"
          stroke="rgba(255,255,255,0.18)"
          strokeWidth="1.6"
          strokeLinecap="round"
        />
      </svg>
      <div className="absolute inset-x-0 top-[34px] flex flex-col items-center">
        <div className="flex items-start gap-1.5">
          <span
            className="text-[44px] font-extrabold leading-none tracking-tight text-[#a5e3ff]"
            style={{ textShadow: '0 0 22px rgba(125,211,252,0.5)' }}
          >
            {score}
          </span>
          <span
            className={cn(
              'mt-1 text-[13px] font-bold',
              delta >= 0 ? 'text-emerald-400' : 'text-rose-400'
            )}
          >
            {delta >= 0 ? '▲' : '▼'}
          </span>
        </div>
        <span className="mt-0.5 text-[11.5px] font-semibold text-white/50">Opal Score</span>
      </div>
    </div>
  )
}

export function TodayView() {
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
  const appsQ = useQuery<BlockApp[]>({
    queryKey: ['apps'],
    queryFn: async () => (await fetch('/api/apps')).json(),
  })

  const stats = statsQ.data
  const profile = profileQ.data
  const scores = computeScores(stats, sessionsQ.data)

  // chalg'ituvchi ilovalar daqiqasi
  const distracting = (appsQ.data ?? [])
    .filter((a) => ['Ijtimoiy tarmoq', 'Zerikarli', 'O‘yinlar', 'Messenger'].includes(a.category))
    .reduce((acc, a) => acc + a.todayMinutes, 0)

  const today = stats?.today
  const screenDelta = stats ? today!.screenTimeMinutes - stats.avgDailyScreenMinutes : 0
  const distractingDelta = Math.round(distracting * 0.18)

  const chartData: (DailyStat & { label: string })[] =
    stats?.days.map((d) => ({ ...d, label: dayLabel(d.date) })) ?? []

  // haftalik baho: maqsad ichida kunlar
  const inGoalDays = (stats?.days ?? []).filter((d) => d.screenTimeMinutes <= d.goalMinutes).length
  const grade = inGoalDays >= 6 ? 'A' : inGoalDays >= 4 ? 'B' : inGoalDays >= 2 ? 'C' : 'D'

  const bestDay = [...(stats?.days ?? [])].sort(
    (a, b) => a.screenTimeMinutes - b.screenTimeMinutes
  )[0]

  const shareReport = async () => {
    const text = `📊 Opal haftalik hisobotim: ${grade} baho · ${formatMinutes(stats?.weekSavedMinutes ?? 0)} tejaldi · 🔥 ${profile?.streakDays ?? 0} kunlik streak!`
    try {
      await Promise.race([
        navigator.clipboard.writeText(text),
        new Promise((_, rej) => setTimeout(() => rej(new Error('timeout')), 1500)),
      ])
      const { toast } = await import('sonner')
      toast.success('Hisobot nusxalandi 📋')
    } catch {
      const { toast } = await import('sonner')
      toast.info('Hisobot', { description: text })
    }
  }

  if (statsQ.isLoading) {
    return (
      <div className="space-y-5 px-5 pt-3">
        <div className="mx-auto mt-4 h-32 w-56 animate-pulse rounded-3xl bg-white/5" />
        <div className="h-24 animate-pulse rounded-3xl bg-white/5" />
        <div className="h-56 animate-pulse rounded-3xl bg-white/5" />
      </div>
    )
  }

  return (
    <div className="space-y-6 px-5 pb-6 pt-1">
      {/* score arc */}
      <div className="pt-2">
        <ScoreArc score={scores.score} delta={scores.delta} />
      </div>

      {/* metric bubbles */}
      <div className="flex items-center justify-center gap-3">
        <MetricBubble icon={<Moon size={14} />} label="Sleep" value={scores.sleep} delay={0.05} />
        <MetricBubble icon={<Sigma size={14} />} label="Focus" value={scores.focus} delay={0.13} />
        <MetricBubble icon={<TreePine size={14} />} label="Rest" value={scores.rest} delay={0.21} />
      </div>

      <p className="mx-auto -mt-1 max-w-[300px] text-center text-[12px] leading-relaxed text-white/40">
        Opal Score uyqu, fokus va dam signallarini birlashtirib, texnologiyaning farovonligingizga
        mosligini bitta ko‘rsatkichda ifodalaydi.
      </p>

      {/* TODAY'S HIGHLIGHTS */}
      <section aria-label="Bugungi yo'l ko'rsatkichlar">
        <p className="mb-3 text-[11px] font-bold uppercase tracking-[0.18em] text-white/40">
          Today&apos;s Highlights
        </p>
        <div className={cn(GLASS, 'space-y-5 p-4')}>
          {/* Screen time */}
          <div>
            <div className="flex items-center justify-between">
              <p className="flex items-center gap-2 text-[13.5px] font-bold text-white">
                <ScreenShare size={14} className="text-white/50" /> Screen Time{' '}
                <span className="font-semibold text-white/50">
                  {formatMinutes(today?.screenTimeMinutes ?? 0)}
                </span>
              </p>
              <span
                className={cn(
                  'text-[12px] font-bold',
                  screenDelta <= 0 ? 'text-teal-300' : 'text-rose-300'
                )}
              >
                {screenDelta <= 0 ? '▼' : '▲'} {formatMinutes(Math.abs(screenDelta))} ›
              </span>
            </div>
            <div className="relative mt-2.5 h-2.5 rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-gradient-to-r from-teal-300 to-emerald-400"
                style={{
                  width: `${Math.min(((today?.screenTimeMinutes ?? 0) / Math.max((today?.goalMinutes ?? 240) * 1.4, 1)) * 100, 100)}%`,
                  boxShadow: '0 0 10px rgba(94,234,212,0.4)',
                }}
              />
              <span
                className="absolute -top-1 rounded-full bg-white px-1.5 py-px text-[8px] font-black text-black"
                style={{ left: '42%' }}
              >
                AVG
              </span>
            </div>
          </div>

          {/* Distracting apps */}
          <div>
            <div className="flex items-center justify-between">
              <p className="flex items-center gap-2 text-[13.5px] font-bold text-white">
                <span>📱</span> Distracting Apps{' '}
                <span className="font-semibold text-white/50">{formatMinutes(distracting)}</span>
              </p>
              <span
                className={cn(
                  'text-[12px] font-bold',
                  distractingDelta <= 0 ? 'text-teal-300' : 'text-rose-300'
                )}
              >
                {distractingDelta <= 0 ? '▼' : '▲'} {formatMinutes(Math.abs(distractingDelta))} ›
              </span>
            </div>
            <div className="relative mt-2.5 h-2.5 rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-gradient-to-r from-rose-300 to-rose-500"
                style={{
                  width: `${Math.min((distracting / Math.max((today?.goalMinutes ?? 240) * 0.6, 1)) * 100, 100)}%`,
                  boxShadow: '0 0 10px rgba(251,113,133,0.4)',
                }}
              />
              <span
                className="absolute -top-1 rounded-full bg-white px-1.5 py-px text-[8px] font-black text-black"
                style={{ left: '35%' }}
              >
                AVG
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* haftalik grafik */}
      <section aria-label="Haftalik ekran vaqti">
        <p className="mb-3 text-[11px] font-bold uppercase tracking-[0.18em] text-white/40">
          Bu hafta · Maqsad {formatMinutes(stats?.goalMinutes ?? 240)}
        </p>
        <div className={cn(GLASS, 'p-4')}>
          <div className="h-[150px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} barSize={16} margin={{ top: 8, right: 0, left: -22, bottom: 0 }}>
                <XAxis
                  dataKey="label"
                  tick={{ fill: 'rgba(255,255,255,0.4)', fontSize: 10, fontWeight: 600 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis tick={{ fill: 'rgba(255,255,255,0.3)', fontSize: 9 }} axisLine={false} tickLine={false} />
                <Tooltip
                  cursor={{ fill: 'rgba(255,255,255,0.05)' }}
                  contentStyle={{
                    background: '#0c0f1c',
                    border: '1px solid rgba(255,255,255,0.12)',
                    borderRadius: 12,
                    fontSize: 11,
                    color: '#fff',
                  }}
                  formatter={(v: number) => [formatMinutes(v), 'Ekran vaqti']}
                />
                <Bar dataKey="screenTimeMinutes" radius={[5, 5, 2, 2]}>
                  {chartData.map((d, i) => {
                    const isToday = d.date === stats?.today.date
                    const over = d.screenTimeMinutes > d.goalMinutes
                    return (
                      <Cell
                        key={i}
                        fill={over ? '#fb7185' : isToday ? '#8fd9ff' : 'rgba(177,140,255,0.55)'}
                      />
                    )
                  })}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-2 flex items-center gap-4 text-[10px] font-semibold text-white/40">
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-[#8fd9ff]" /> Bugun
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-[#b18cff]/60" /> Maqsad ichida
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-rose-400" /> Maqsaddan oshgan
            </span>
          </div>
        </div>
      </section>

      {/* tejalgan vaqt trendi */}
      <section aria-label="Tejalgan vaqt trendi">
        <p className="mb-3 text-[11px] font-bold uppercase tracking-[0.18em] text-white/40">
          Tejalgan vaqt
        </p>
        <div className={cn(GLASS, 'p-4')}>
          <div className="h-[110px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 8, right: 8, left: -22, bottom: 0 }}>
                <XAxis
                  dataKey="label"
                  tick={{ fill: 'rgba(255,255,255,0.4)', fontSize: 10, fontWeight: 600 }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  contentStyle={{
                    background: '#0c0f1c',
                    border: '1px solid rgba(255,255,255,0.12)',
                    borderRadius: 12,
                    fontSize: 11,
                    color: '#fff',
                  }}
                  formatter={(v: number) => [formatMinutes(v), 'Tejaldi']}
                />
                <Line
                  type="monotone"
                  dataKey="savedMinutes"
                  stroke="#8fd9ff"
                  strokeWidth={2.5}
                  dot={{ r: 3, fill: '#8fd9ff', strokeWidth: 0 }}
                  activeDot={{ r: 4.5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <p className="mt-2 text-center text-[11px] font-semibold text-white/45">
            Bu hafta jami{' '}
            <span className="font-extrabold text-[#9fd8ff]">
              {formatMinutes(stats?.weekSavedMinutes ?? 0)}
            </span>{' '}
            tejaldi
          </p>
        </div>
      </section>

      {/* streak kalendari */}
      <section aria-label="Streak kalendari">
        <p className="mb-3 text-[11px] font-bold uppercase tracking-[0.18em] text-white/40">
          Streak kalendari
        </p>
        <div className={cn(GLASS, 'flex items-center justify-between p-4')}>
          {(stats?.days ?? []).map((d) => (
            <div key={d.date} className="flex flex-col items-center gap-1.5">
              <span className="text-[10px] font-semibold text-white/40">{dayLabel(d.date)}</span>
              <span
                className={cn(
                  'flex h-9 w-9 items-center justify-center rounded-full text-[13px] ring-1',
                  d.savedMinutes > 0
                    ? 'bg-orange-400/15 text-orange-300 ring-orange-400/30'
                    : 'bg-white/[0.04] text-white/25 ring-white/8'
                )}
                title={`${formatMinutes(d.savedMinutes)} tejaldi`}
              >
                {d.savedMinutes > 0 ? '🔥' : '·'}
              </span>
              <span className="text-[8.5px] font-semibold text-white/35">
                {d.savedMinutes > 0 ? `${Math.round(d.savedMinutes)}d` : ''}
              </span>
            </div>
          ))}
        </div>
      </section>

      {/* haftalik hisobot kartasi */}
      <section
        aria-label="Haftalik hisobot"
        className="relative overflow-hidden rounded-3xl border border-white/12 bg-gradient-to-br from-[#141b3a] via-[#101331] to-[#0b0e20] p-5 shadow-[0_14px_44px_rgba(0,0,0,0.5)]"
      >
        <div
          className="pointer-events-none absolute -right-10 -top-14 h-40 w-40 rounded-full bg-[#8fd9ff]/15 blur-2xl"
          aria-hidden="true"
        />
        <div className="relative flex items-center justify-between">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-widest text-white/40">
              Haftalik hisobot
            </p>
            <div className="mt-1.5 flex items-center gap-2.5">
              <span className="text-[34px] font-extrabold leading-none text-[#a5e3ff]">{grade}</span>
              <div className="text-[11.5px] leading-tight text-white/55">
                <p>
                  <span className="font-bold text-white/85">{inGoalDays}/7</span> kun maqsad ichida
                </p>
                {bestDay && <p>Eng yaxshi kun: {dayLabel(bestDay.date)}</p>}
              </div>
            </div>
          </div>
          <button
            onClick={shareReport}
            className="flex items-center gap-1.5 rounded-full bg-white/10 px-4 py-2.5 text-[12px] font-bold text-white ring-1 ring-white/15 backdrop-blur transition-transform active:scale-95"
          >
            <ChevronRight size={13} /> Ulashish
          </button>
        </div>
      </section>

      {/* jonli leaderboard */}
      <LiveLeaderboard
        myName={profile?.name ?? 'Aziz'}
        mySavedMinutes={stats?.weekSavedMinutes ?? 0}
        myStreak={profile?.streakDays ?? 0}
        loading={profileQ.isLoading}
      />
    </div>
  )
}
