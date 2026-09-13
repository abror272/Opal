'use client'

import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { AnimatePresence } from 'framer-motion'
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
import { computeScores, GLASS, OPAL, clamp, lastSleep, clockTime } from '@/lib/opal-ui'
import { dayLabel, formatMinutes, sessionFullyCompleted } from '@/lib/opal-types'
import { LiveLeaderboard } from './live-leaderboard'
import { Moon, TreePine, Hourglass, ChevronLeft, ChevronRight, ScreenShare } from 'lucide-react'
import { cn } from '@/lib/utils'
import { ScorePill } from './score-pill'

type MetricKey = 'Sleep' | 'Focus' | 'Rest'

const METRIC_INFO: Record<
  MetricKey,
  { desc: string; icon: React.ReactNode }
> = {
  Sleep: {
    desc: 'Uyqu Score kechki, uyqu vaqtidagi va ertalibgi holatni hamda kun davomida qanday his qilayotganingizni o‘lchaydi.',
    icon: <Moon size={15} />,
  },
  Focus: {
    desc: 'Focus Score fokus sessiyalaringiz, tejalgan vaqt va chalg‘ituvchi ilovalardan qochganingizni birlashtiradi.',
    icon: <Hourglass size={15} />,
  },
  Rest: {
    desc: 'Rest Score ekrandan tanaffuslar va umumiy ekran vaqti muvozanatini aks ettiradi.',
    icon: <TreePine size={15} />,
  },
}

/** Bitta metrik qatori — "Sleep 0m — Short" + AVG slider (haqiqiy Opal kabi) */
function MetricRow({
  title,
  value,
  rating,
  position,
  avgAt,
}: {
  title: string
  value: string
  rating: 'Great' | 'OK' | 'Short'
  /** 0..100 — sizning pozitsiyangiz */
  position: number
  /** 0..100 — AVG belgisi */
  avgAt: number
}) {
  const ratingColor =
    rating === 'Great' ? 'text-[#7ee8b2]' : rating === 'OK' ? 'text-amber-300' : 'text-rose-400'
  const fillColor =
    rating === 'Great'
      ? 'linear-gradient(90deg, rgba(126,232,178,0.25), #6ee7b7)'
      : rating === 'OK'
        ? 'linear-gradient(90deg, rgba(252,211,77,0.25), #fbbf24)'
        : 'linear-gradient(90deg, rgba(251,113,133,0.3), #fb7185)'
  return (
    <div>
      <div className="flex items-baseline justify-between">
        <p className="text-[14px] font-bold text-white">
          {title} <span className="font-semibold text-white/45">{value}</span>
        </p>
        <span className={cn('text-[13px] font-bold', ratingColor)}>{rating}</span>
      </div>
      <div className="relative mt-2.5 h-2.5">
        {/* segmentlangan track */}
        <div className="absolute inset-0 flex gap-1">
          {[0, 1, 2, 3, 4].map((i) => (
            <span key={i} className="h-full flex-1 rounded-full bg-white/8" />
          ))}
        </div>
        {/* to'ldiruvchi */}
        <div
          className="absolute left-0 top-0 h-full rounded-full transition-all duration-700"
          style={{
            width: `${clamp(position, 2, 100)}%`,
            background: fillColor,
            boxShadow: rating === 'Great' ? '0 0 10px rgba(110,231,183,0.4)' : 'none',
          }}
        />
        {/* AVG belgisi */}
        <span
          className="absolute -top-[7px] z-10 -translate-x-1/2 rounded-md bg-white px-1.5 py-[2px] text-[8.5px] font-black tracking-wide text-black shadow"
          style={{ left: `${clamp(avgAt, 6, 94)}%` }}
        >
          AVG
        </span>
      </div>
    </div>
  )
}

/** Yarim arc gauge (haqiqiy Opal Score vizuali, mint gradient) */
function ScoreArc({ score, delta }: { score: number; delta: number }) {
  const R = 88
  const LEN = Math.PI * R
  return (
    <div className="relative mx-auto w-[240px]">
      <svg viewBox="0 0 220 128" className="w-full">
        <defs>
          <linearGradient id="arcGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#86efac" />
            <stop offset="55%" stopColor={OPAL.mint} />
            <stop offset="100%" stopColor="#5eead4" />
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
          style={{ filter: `drop-shadow(0 0 10px ${OPAL.mintGlow})` }}
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
            className="text-[44px] font-extrabold leading-none tracking-tight"
            style={{ color: OPAL.mint, textShadow: `0 0 22px ${OPAL.mintGlow}` }}
          >
            {score}
          </span>
          <span
            className={cn(
              'mt-1 text-[13px] font-bold',
              delta >= 0 ? 'text-[#9fe8b5]' : 'text-rose-400'
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

  const [selected, setSelected] = useState<MetricKey>('Sleep')

  const stats = statsQ.data
  const profile = profileQ.data
  const scores = computeScores(stats, sessionsQ.data)

  const todayIso = new Date().toISOString().slice(0, 10)
  const todaysSessions = (sessionsQ.data ?? []).filter(
    (s) => (s.endedAt ?? s.startedAt).slice(0, 10) === todayIso
  )
  const completedToday = todaysSessions.filter(sessionFullyCompleted)

  // chalg'ituvchi ilovalar daqiqasi
  const distracting = (appsQ.data ?? [])
    .filter((a) => ['Ijtimoiy tarmoq', 'Zerikarli', 'O‘yinlar', 'Messenger'].includes(a.category))
    .reduce((acc, a) => acc + a.todayMinutes, 0)

  const today = stats?.today
  const screenDelta = stats ? today!.screenTimeMinutes - stats.avgDailyScreenMinutes : 0
  const distractingDelta = Math.round(distracting * 0.18)
  // REAL UYQU: oxirgi kechagi SLEEP sessiyasidan (web demo seed'i bilan ham ishlaydi)
  const sleepRec = lastSleep(sessionsQ.data)
  const sleepMinutes = sleepRec?.minutes ?? 0
  const pickups = today?.pickups ?? 0

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

      {/* tanlanadigan metrik pilllar (haqiqiy Opal — stadion outline) */}
      <div className="flex items-start justify-center gap-3">
        <ScorePill
          value={scores.sleep}
          icon={<Moon size={15} />}
          label="Sleep"
          size="lg"
          selected={selected === 'Sleep'}
          delay={0.05}
          onClick={() => setSelected('Sleep')}
        />
        <ScorePill
          value={scores.focus}
          icon={<Hourglass size={15} />}
          label="Focus"
          size="lg"
          selected={selected === 'Focus'}
          delay={0.12}
          onClick={() => setSelected('Focus')}
        />
        <ScorePill
          value={scores.rest}
          icon={<TreePine size={15} />}
          label="Rest"
          size="lg"
          selected={selected === 'Rest'}
          delay={0.19}
          onClick={() => setSelected('Rest')}
        />
      </div>

      {/* What is X Score? + metrik slayderlar (haqiqiy Opal Today ekrani) */}
      <AnimatePresence mode="wait">
        <motion.section
          key={selected}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
          aria-label={`${selected} Score tafsiloti`}
        >
          <h2 className="text-[17px] font-bold text-white">What is {selected} Score?</h2>
          <p className="mt-1.5 text-[13px] leading-relaxed text-white/45">
            {METRIC_INFO[selected].desc}
          </p>

          <div className={cn(GLASS, 'mt-4 space-y-5 p-4')}>
            {selected === 'Sleep' && (
              <>
                <MetricRow
                  title="Sleep"
                  value={sleepRec ? formatMinutes(sleepRec.minutes) : '0m'}
                  rating={sleepMinutes >= 420 ? 'Great' : sleepMinutes >= 360 ? 'OK' : 'Short'}
                  position={clamp((sleepMinutes / 480) * 100, 4, 100)}
                  avgAt={78}
                />
                {/* yotish / uyg'onish vaqtlari — real sessiyadan */}
                {sleepRec && (
                  <div className="grid grid-cols-2 gap-2">
                    <div className="flex items-center gap-2 rounded-2xl bg-white/[0.05] p-2.5 ring-1 ring-white/8">
                      <span className="text-[16px]" aria-hidden="true">
                        🌙
                      </span>
                      <div>
                        <p className="text-[9px] font-bold uppercase tracking-wider text-white/40">Yotish</p>
                        <p className="text-[13px] font-extrabold text-white">{clockTime(sleepRec.startedAt)}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 rounded-2xl bg-white/[0.05] p-2.5 ring-1 ring-white/8">
                      <span className="text-[16px]" aria-hidden="true">
                        ☀️
                      </span>
                      <div>
                        <p className="text-[9px] font-bold uppercase tracking-wider text-white/40">Uyg‘onish</p>
                        <p className="text-[13px] font-extrabold text-white">{clockTime(sleepRec.endedAt)}</p>
                      </div>
                    </div>
                  </div>
                )}
                <MetricRow
                  title="Pickups"
                  value={`${pickups} marta`}
                  rating={pickups <= 12 ? 'Great' : pickups <= 30 ? 'OK' : 'Short'}
                  position={clamp(100 - pickups * 2, 6, 100)}
                  avgAt={42}
                />
                <MetricRow
                  title="Ekran vaqti (kech 3s)"
                  value={formatMinutes(Math.round((today?.screenTimeMinutes ?? 0) * 0.3))}
                  rating={(today?.screenTimeMinutes ?? 0) < 200 ? 'Great' : 'OK'}
                  position={clamp(100 - (today?.screenTimeMinutes ?? 0) / 5, 8, 100)}
                  avgAt={55}
                />
              </>
            )}
            {selected === 'Focus' && (
              <>
                <MetricRow
                  title="Fokus sessiyalari"
                  value={`${completedToday.length} ta`}
                  rating={completedToday.length >= 2 ? 'Great' : completedToday.length >= 1 ? 'OK' : 'Short'}
                  position={clamp(completedToday.length * 33 + 8, 6, 100)}
                  avgAt={38}
                />
                <MetricRow
                  title="Bugun tejaldi"
                  value={formatMinutes(today?.savedMinutes ?? 0)}
                  rating={(today?.savedMinutes ?? 0) >= 30 ? 'Great' : (today?.savedMinutes ?? 0) >= 10 ? 'OK' : 'Short'}
                  position={clamp(((today?.savedMinutes ?? 0) / 90) * 100, 5, 100)}
                  avgAt={45}
                />
                <MetricRow
                  title="Distracting Apps"
                  value={formatMinutes(distracting)}
                  rating={distracting <= 30 ? 'Great' : distracting <= 60 ? 'OK' : 'Short'}
                  position={clamp(100 - distracting, 6, 100)}
                  avgAt={52}
                />
              </>
            )}
            {selected === 'Rest' && (
              <>
                <MetricRow
                  title="Ekran vaqti"
                  value={formatMinutes(today?.screenTimeMinutes ?? 0)}
                  rating={screenDelta <= 0 ? 'Great' : screenDelta < 45 ? 'OK' : 'Short'}
                  position={clamp(100 - (today?.screenTimeMinutes ?? 0) / 5, 6, 100)}
                  avgAt={48}
                />
                <MetricRow
                  title="Dam sessiyalari"
                  value={`${todaysSessions.filter((s) => s.type === 'CUSTOM' && sessionFullyCompleted(s)).length} ta`}
                  rating={
                    todaysSessions.filter((s) => s.type === 'CUSTOM' && sessionFullyCompleted(s)).length >= 1
                      ? 'Great'
                      : 'Short'
                  }
                  position={todaysSessions.some((s) => s.type === 'CUSTOM' && sessionFullyCompleted(s)) ? 72 : 10}
                  avgAt={40}
                />
                <MetricRow
                  title="O‘rtachaga nisbat"
                  value={`${screenDelta <= 0 ? '▼' : '▲'} ${formatMinutes(Math.abs(screenDelta))}`}
                  rating={screenDelta <= 0 ? 'Great' : 'Short'}
                  position={clamp(50 - screenDelta / 4, 6, 100)}
                  avgAt={50}
                />
              </>
            )}
          </div>
        </motion.section>
      </AnimatePresence>

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
                  screenDelta <= 0 ? 'text-[#7ee8b2]' : 'text-rose-300'
                )}
              >
                {screenDelta <= 0 ? '▼' : '▲'} {formatMinutes(Math.abs(screenDelta))} ›
              </span>
            </div>
            <div className="relative mt-2.5 h-2.5 rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-gradient-to-r from-[#86efac] to-[#34d399]"
                style={{
                  width: `${Math.min(((today?.screenTimeMinutes ?? 0) / Math.max((today?.goalMinutes ?? 240) * 1.4, 1)) * 100, 100)}%`,
                  boxShadow: '0 0 10px rgba(134,239,172,0.4)',
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
                  distractingDelta <= 0 ? 'text-[#7ee8b2]' : 'text-rose-300'
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
                    background: '#0c120e',
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
                        fill={over ? '#fb7185' : isToday ? OPAL.mint : 'rgba(94,234,212,0.45)'}
                      />
                    )
                  })}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-2 flex items-center gap-4 text-[10px] font-semibold text-white/40">
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full" style={{ background: OPAL.mint }} /> Bugun
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-[#5eead4]/45" /> Maqsad ichida
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
                    background: '#0c120e',
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
                  stroke={OPAL.mint}
                  strokeWidth={2.5}
                  dot={{ r: 3, fill: OPAL.mint, strokeWidth: 0 }}
                  activeDot={{ r: 4.5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <p className="mt-2 text-center text-[11px] font-semibold text-white/45">
            Bu hafta jami{' '}
            <span className="font-extrabold text-[#c9fbdc]">
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
        className="relative overflow-hidden rounded-3xl border border-white/12 bg-gradient-to-br from-[#12241a] via-[#0e1b16] to-[#0a100d] p-5 shadow-[0_14px_44px_rgba(0,0,0,0.5)]"
      >
        <div
          className="pointer-events-none absolute -right-10 -top-14 h-40 w-40 rounded-full bg-[#86efac]/12 blur-2xl"
          aria-hidden="true"
        />
        <div className="relative flex items-center justify-between">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-widest text-white/40">
              Haftalik hisobot
            </p>
            <div className="mt-1.5 flex items-center gap-2.5">
              <span
                className="text-[34px] font-extrabold leading-none"
                style={{ color: OPAL.mint, textShadow: `0 0 18px ${OPAL.mintGlow}` }}
              >
                {grade}
              </span>
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
