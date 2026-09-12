'use client'

import { useQuery } from '@tanstack/react-query'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, LineChart, Line } from 'recharts'
import { formatMinutes, dayLabel, type StatsResponse } from '@/lib/opal-types'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { TrendingDown, TrendingUp, Hourglass, MousePointerClick, Target, CalendarCheck2 } from 'lucide-react'

function ChartTooltip({ active, payload, label }: { active?: boolean; payload?: { value: number }[]; label?: string }) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-xl bg-[#10123f] px-3 py-2 text-[12px] font-semibold text-white shadow-lg">
      <div className="opacity-60">{label}</div>
      <div>{formatMinutes(payload[0].value)}</div>
    </div>
  )
}

export function StatsTab() {
  const statsQ = useQuery<StatsResponse>({
    queryKey: ['stats'],
    queryFn: async () => (await fetch('/api/stats')).json(),
  })

  if (statsQ.isLoading) {
    return (
      <div className="space-y-5 p-5 pt-3">
        <Skeleton className="h-9 w-48 rounded-xl" />
        <Skeleton className="h-28 w-full rounded-3xl" />
        <Skeleton className="h-56 w-full rounded-3xl" />
        <Skeleton className="h-24 w-full rounded-3xl" />
      </div>
    )
  }

  const stats = statsQ.data
  if (!stats) return null

  const chartData = stats.days.map((d) => ({
    name: dayLabel(d.date),
    minutes: d.screenTimeMinutes,
    saved: d.savedMinutes,
    goal: d.goalMinutes,
    isToday: d.date === stats.today.date,
  }))

  const goalProgress = Math.min(stats.today.screenTimeMinutes / Math.max(stats.goalMinutes, 1), 1)
  const trendDown = stats.trendPercent <= 0

  return (
    <div className="animate-slide-up space-y-5 px-5 pb-6 pt-3">
      <header>
        <h2 className="text-[22px] font-extrabold tracking-tight text-slate-900">Statistika</h2>
        <p className="text-[13px] text-slate-400">Shu haftalik natijalaringiz</p>
      </header>

      {/* trend hero card */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#10123f] via-[#1b1e5c] to-[#3d2f86] p-5 text-white shadow-lg shadow-indigo-900/25">
        <div className="pointer-events-none absolute -right-8 -top-12 h-36 w-36 rounded-full bg-[#7b61ff]/30 blur-2xl" />
        <div className="relative flex items-start justify-between">
          <div>
            <p className="text-[12px] font-medium text-white/60">Haftada tejaldi</p>
            <p className="mt-1 text-[30px] font-extrabold leading-none">
              {formatMinutes(stats.weekSavedMinutes)}
            </p>
          </div>
          <div
            className={cn(
              'flex items-center gap-1 rounded-full px-2.5 py-1.5 text-[12px] font-bold',
              trendDown ? 'bg-emerald-400/15 text-emerald-300' : 'bg-amber-400/15 text-amber-300'
            )}
          >
            {trendDown ? <TrendingDown size={14} /> : <TrendingUp size={14} />}
            {Math.abs(stats.trendPercent)}%
          </div>
        </div>
        <div className="relative mt-4 grid grid-cols-3 gap-2 text-center">
          <div className="rounded-2xl bg-white/8 p-2.5 ring-1 ring-white/10">
            <p className="text-[15px] font-extrabold">{formatMinutes(stats.weekScreenMinutes)}</p>
            <p className="text-[10px] text-white/55">Ekran vaqti</p>
          </div>
          <div className="rounded-2xl bg-white/8 p-2.5 ring-1 ring-white/10">
            <p className="text-[15px] font-extrabold">{formatMinutes(stats.avgDailyScreenMinutes)}</p>
            <p className="text-[10px] text-white/55">O‘rtacha kun</p>
          </div>
          <div className="rounded-2xl bg-white/8 p-2.5 ring-1 ring-white/10">
            <p className="text-[15px] font-extrabold">{stats.days[stats.days.length - 1]?.pickups ?? 0}</p>
            <p className="text-[10px] text-white/55">Bugun olish</p>
          </div>
        </div>
      </div>

      {/* weekly screen time bar chart */}
      <section className="rounded-3xl bg-white p-5 shadow-sm shadow-slate-200/60" aria-label="Haftalik ekran vaqti">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-[15px] font-bold text-slate-900">Ekran vaqti (hafta)</h3>
          <span className="flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold text-slate-500">
            <Target size={12} /> Maqsad {formatMinutes(stats.goalMinutes)}
          </span>
        </div>
        <div className="h-44">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 4, right: 0, bottom: 0, left: -22 }} barCategoryGap="28%">
              <XAxis
                dataKey="name"
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 11, fill: '#94a3b8', fontWeight: 600 }}
              />
              <YAxis
                tick={{ fontSize: 10, fill: '#cbd5e1' }}
                axisLine={false}
                tickLine={false}
                tickFormatter={(v: number) => `${Math.round(v / 60)}s`}
              />
              <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(61,90,254,0.06)' }} />
              <Bar dataKey="minutes" radius={[8, 8, 8, 8]}>
                {chartData.map((d, i) => (
                  <Cell
                    key={i}
                    fill={d.isToday ? '#3d5afe' : d.minutes > d.goal ? '#f43f5e' : '#c7c9f7'}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-[11px] font-medium text-slate-400">
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-full bg-[#c7c9f7]" /> Maqsad ichida
            <span className="ml-2 h-2.5 w-2.5 rounded-full bg-rose-400" /> Oshib ketdi
            <span className="ml-2 h-2.5 w-2.5 rounded-full bg-[#3d5afe]" /> Bugun
          </span>
        </div>
      </section>

      {/* saved line chart */}
      <section className="rounded-3xl bg-white p-5 shadow-sm shadow-slate-200/60" aria-label="Tejalgan vaqt grafigi">
        <div className="mb-3 flex items-center gap-2">
          <Hourglass size={16} className="text-emerald-500" />
          <h3 className="text-[15px] font-bold text-slate-900">Tejalgan vaqt</h3>
        </div>
        <div className="h-36">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 6, right: 6, bottom: 0, left: -26 }}>
              <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94a3b8', fontWeight: 600 }} />
              <YAxis tick={{ fontSize: 10, fill: '#cbd5e1' }} axisLine={false} tickLine={false} tickFormatter={(v: number) => `${Math.round(v / 60)}s`} />
              <Tooltip content={<ChartTooltip />} cursor={{ stroke: 'rgba(16,185,129,0.3)' }} />
              <Line
                type="monotone"
                dataKey="saved"
                stroke="#10b981"
                strokeWidth={3}
                dot={{ r: 3.5, fill: '#10b981', strokeWidth: 0 }}
                activeDot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </section>

      {/* today goal + pickups */}
      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-3xl bg-white p-4 shadow-sm shadow-slate-200/60">
          <div className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-400">
            <Target size={13} /> Kunlik maqsad
          </div>
          <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100">
            <div
              className={cn(
                'h-full rounded-full transition-all duration-700',
                goalProgress >= 1 ? 'bg-gradient-to-r from-rose-400 to-rose-500' : 'bg-gradient-to-r from-[#3d5afe] to-[#7b61ff]'
              )}
              style={{ width: `${goalProgress * 100}%` }}
            />
          </div>
          <p className="mt-2 text-[13px] font-bold text-slate-700">
            {formatMinutes(stats.today.screenTimeMinutes)}{' '}
            <span className="font-medium text-slate-400">/ {formatMinutes(stats.goalMinutes)}</span>
          </p>
        </div>
        <div className="rounded-3xl bg-white p-4 shadow-sm shadow-slate-200/60">
          <div className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-400">
            <MousePointerClick size={13} /> Telefon olish
          </div>
          <p className="mt-2.5 text-[26px] font-extrabold leading-none text-slate-900">
            {stats.today.pickups}
          </p>
          <p className="mt-1.5 text-[11px] font-medium text-emerald-500">
            kecha {stats.days[stats.days.length - 2]?.pickups ?? '–'} tadan kam ↑
          </p>
        </div>
      </div>

      {/* streak strip */}
      <section className="rounded-3xl bg-gradient-to-br from-orange-400/10 to-amber-400/10 p-5 ring-1 ring-orange-400/20" aria-label="Ketma-ketlik kalendari">
        <h3 className="mb-3 flex items-center gap-2 text-[15px] font-bold text-slate-900">
          <CalendarCheck2 size={16} className="text-orange-500" /> Bu hafta streak
        </h3>
        <div className="flex justify-between">
          {stats.days.map((d, i) => {
            const hit = d.screenTimeMinutes <= d.goalMinutes
            return (
              <div key={d.id} className="flex flex-col items-center gap-1.5">
                <div
                  className={cn(
                    'flex h-9 w-9 items-center justify-center rounded-full text-[13px] font-bold',
                    hit
                      ? 'bg-gradient-to-br from-orange-400 to-amber-400 text-white shadow-sm shadow-orange-300'
                      : 'bg-slate-100 text-slate-400'
                  )}
                >
                  {hit ? '🔥' : '·'}
                </div>
                <span className="text-[10px] font-semibold text-slate-400">{dayLabel(d.date)}</span>
                {i === stats.days.length - 1 && (
                  <span className="sr-only">bugun</span>
                )}
              </div>
            )
          })}
        </div>
      </section>
    </div>
  )
}
