'use client'

import { useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { toast } from 'sonner'
import { toPng } from 'html-to-image'
import type { BlockApp, FocusSession, StatsResponse, UserProfile } from '@/lib/opal-types'
import { formatMinutes, sessionFullyCompleted } from '@/lib/opal-types'
import {
  weekRangeLabel,
  weekSavedSeries,
  worldwideTopPercent,
  lastSleep,
  clamp,
  OPAL,
} from '@/lib/opal-ui'
import { cn } from '@/lib/utils'
import { Flame, Globe2, Hourglass, ImageDown, Moon, Share2, ShieldCheck, Smartphone, Trophy } from 'lucide-react'

/**
 * HAFTALIK HISOBOT — haqiqiy Opal'ning imzo "Weekly Report" kartasi.
 * Profil → "Haftalik hisobot" qatoridan drill-in sifatida ochiladi.
 * Ulashish: Web Share API (mobil) → fallback: clipboard matn.
 */
export function WeeklyReport() {
  const statsQ = useQuery<StatsResponse>({
    queryKey: ['stats'],
    queryFn: async () => (await fetch('/api/stats')).json(),
  })
  const sessionsQ = useQuery<FocusSession[]>({
    queryKey: ['sessions'],
    queryFn: async () => (await fetch('/api/sessions')).json(),
  })
  const profileQ = useQuery<UserProfile>({
    queryKey: ['profile'],
    queryFn: async () => (await fetch('/api/profile')).json(),
  })
  const appsQ = useQuery<BlockApp[]>({
    queryKey: ['apps'],
    queryFn: async () => (await fetch('/api/apps')).json(),
  })

  const [sharing, setSharing] = useState(false)
  const [exporting, setExporting] = useState(false)
  const reportRef = useRef<HTMLDivElement>(null)

  const stats = statsQ.data
  const sessions = sessionsQ.data ?? []
  const profile = profileQ.data

  const week = useMemo(() => weekSavedSeries(stats?.days), [stats])
  const totalSaved = week.reduce((a, d) => a + d.saved, 0)
  const totalScreen = week.reduce((a, d) => a + d.screen, 0)
  const best = useMemo(
    () => week.reduce((b, d) => (d.saved > b.saved ? d : b), week[0] ?? { saved: 0, full: '—', label: '', screen: 0, isToday: false, dateISO: '' }),
    [week]
  )
  const maxSaved = Math.max(...week.map((d) => d.saved), 1)

  // bu haftadagi sessiyalar (oxirgi 7 kun)
  const weekSessions = useMemo(() => {
    const cutoff = Date.now() - 7 * 24 * 3600_000
    return sessions.filter((s) => new Date(s.startedAt).getTime() >= cutoff)
  }, [sessions])
  const completedCount = weekSessions.filter(sessionFullyCompleted).length
  const completionPct = weekSessions.length ? Math.round((completedCount / weekSessions.length) * 100) : 0

  // o'rtacha uyqu (oxirgi 7 kunlik SLEEP)
  const avgSleep = useMemo(() => {
    const sleeps = sessions
      .filter((s) => s.type === 'SLEEP' && s.endedAt && new Date(s.endedAt).getTime() >= Date.now() - 7 * 24 * 3600_000)
      .map((s) => clamp(Math.round((new Date(s.endedAt!).getTime() - new Date(s.startedAt).getTime()) / 60_000), 0, 720))
    return sleeps.length ? Math.round(sleeps.reduce((a, b) => a + b, 0) / sleeps.length) : 0
  }, [sessions])

  // eng ko'p bloklangan ilova (blocked orasida eng ko'p todayMinutes)
  const topBlocked = useMemo(() => {
    const list = (appsQ.data ?? []).filter((a) => a.blocked).sort((a, b) => b.todayMinutes - a.todayMinutes)
    return list[0] ?? null
  }, [appsQ])

  const topPct = worldwideTopPercent(stats?.weekSavedMinutes ?? totalSaved)
  const trend = stats?.trendPercent ?? 0

  const share = async () => {
    const text = [
      `💎 Opal — Haftalik hisobot (${weekRangeLabel()})`,
      `⏳ Tejalgan vaqt: ${formatMinutes(totalSaved)}`,
      `🏆 Eng yaxshi kun: ${best.full} (${formatMinutes(best.saved)})`,
      `🔥 Streak: ${profile?.streakDays ?? 0} kun`,
      `✅ Sessiyalar: ${completedCount}/${weekSessions.length} (${completionPct}%)`,
      avgSleep ? `🌙 O'rtacha uyqu: ${formatMinutes(avgSleep)}` : '',
      `🌍 Top ${topPct}% worldwide`,
    ]
      .filter(Boolean)
      .join('\n')
    setSharing(true)
    try {
      if (typeof navigator !== 'undefined' && navigator.share) {
        await navigator.share({ title: 'Opal — Haftalik hisobot', text })
        toast.success('Hisobot ulashildi 🎉')
      } else {
        await navigator.clipboard.writeText(text)
        toast.success('Hisobot nusxalandi 📋', { description: 'Do‘stingizga yuboring' })
      }
    } catch {
      // share bekor qilindi — jim
    } finally {
      setSharing(false)
    }
  }

  const bars = week.map((d, i) => ({ ...d, h: 6 + (d.saved / maxSaved) * 80, i }))

  /** Hisobotni PNG rasm sifatida yuklab olish (html-to-image) */
  const exportPng = async () => {
    const node = reportRef.current
    if (!node || exporting) return
    setExporting(true)
    try {
      // framer-motion animatsiyalari tugashini kutamiz
      await new Promise((r) => setTimeout(r, 80))
      const dataUrl = await toPng(node, {
        pixelRatio: 2,
        backgroundColor: '#05060f',
        cacheBust: true,
      })
      const a = document.createElement('a')
      a.download = `opal-haftalik-hisobot-${new Date().toISOString().slice(0, 10)}.png`
      a.href = dataUrl
      a.click()
      toast.success('Hisobot PNG sifatida saqlandi 🖼️', {
        description: 'Rasm galereyangizda — do‘stlaringizga ulashing',
      })
    } catch {
      toast.error('Rasm yaratishda xatolik', { description: 'Yana bir urinib ko‘ring' })
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="px-5 pb-8 pt-2">
      <div ref={reportRef}>
      {/* ── Hero karta ── */}
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
        className="relative overflow-hidden rounded-[28px] border border-[#86efac]/20 p-5"
        style={{
          background:
            'linear-gradient(160deg, rgba(134,239,172,0.14) 0%, rgba(94,234,212,0.07) 45%, rgba(5,6,15,0) 100%), rgba(255,255,255,0.035)',
          boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.08), 0 18px 50px rgba(0,0,0,0.45)',
        }}
        aria-label="Haftalik hisobot"
      >
        {/* mint nur */}
        <div
          className="pointer-events-none absolute -right-16 -top-16 h-44 w-44 rounded-full"
          style={{ background: 'radial-gradient(circle, rgba(134,239,172,0.22), transparent 70%)' }}
          aria-hidden="true"
        />
        <div className="flex items-center justify-between">
          <p className="text-[10.5px] font-bold uppercase tracking-[0.22em] text-[#9fe8b5]">Haftalik hisobot</p>
          <span className="rounded-full bg-white/8 px-2.5 py-1 text-[10.5px] font-bold text-white/70 ring-1 ring-white/12">
            {weekRangeLabel()}
          </span>
        </div>
        <div className="mt-3 flex items-end gap-2">
          <span
            className="text-[52px] font-extrabold leading-none tracking-tight text-white"
            style={{ textShadow: `0 0 28px ${OPAL.mintGlow}` }}
          >
            {formatMinutes(totalSaved)}
          </span>
          <span className="pb-1.5 text-[12.5px] font-bold text-[#9fe8b5]">tejaldi</span>
        </div>
        <p className="mt-1.5 text-[12px] font-medium text-white/50">
          {trend <= 0 ? (
            <>Ekran vaqti o'tgan haftaga nisbatan <span className="font-bold text-[#9fe8b5]">{Math.abs(trend)}% kamaydi</span> ▼</>
          ) : (
            <>Ekran vaqti o'tgan haftaga nisbatan <span className="font-bold text-amber-300">{trend}% oshdi</span> ▲</>
          )}
        </p>

        {/* 7 kunlik bar chart */}
        <div className="mt-5 flex h-[126px] gap-[7px]" role="img" aria-label="Kunlik tejash grafigi">
          {bars.map((b) => (
            <div key={b.dateISO} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1.5">
              <motion.div
                initial={{ height: '4%' }}
                animate={{ height: `${b.h}%` }}
                transition={{ delay: 0.25 + b.i * 0.055, duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
                className={cn(
                  'w-full rounded-t-[6px]',
                  b.saved === best.saved && b.saved > 0
                    ? 'bg-gradient-to-t from-[#f59e0b] to-[#fcd34d] shadow-[0_0_14px_rgba(252,211,77,0.45)]'
                    : b.isToday
                      ? 'bg-gradient-to-t from-[#5eead4] to-[#b7f5cd] shadow-[0_0_14px_rgba(134,239,172,0.4)]'
                      : 'bg-gradient-to-t from-[#86efac]/45 to-[#86efac]/80'
                )}
              />
              <span className={cn('shrink-0 text-[9px] font-bold', b.isToday ? 'text-[#c9fbdc]' : 'text-white/40')}>
                {b.label}
              </span>
            </div>
          ))}
        </div>
      </motion.section>

      {/* ── Statistikalar grid ── */}
      <div className="mt-4 grid grid-cols-2 gap-3">
        <StatCard
          icon={<Hourglass size={14} />}
          label="Sessiyalar"
          value={`${completedCount}/${weekSessions.length}`}
          sub={`${completionPct}% oxirigacha`}
          tone="mint"
          delay={0.1}
        />
        <StatCard
          icon={<Smartphone size={14} />}
          label="Ekran vaqti"
          value={formatMinutes(totalScreen)}
          sub={`o'rtacha ${formatMinutes(Math.round(totalScreen / 7))}/kun`}
          tone="sky"
          delay={0.16}
        />
        <StatCard
          icon={<Moon size={14} />}
          label="O'rtacha uyqu"
          value={avgSleep ? formatMinutes(avgSleep) : '—'}
          sub={avgSleep ? (avgSleep >= 420 ? 'Yaxshi rejim ✓' : "Ko'proq uqlang") : 'Uyqu sessiyasi yo‘q'}
          tone="violet"
          delay={0.22}
        />
        <StatCard
          icon={<Globe2 size={14} />}
          label="Worldwide"
          value={`Top ${topPct}%`}
          sub="global reytingda"
          tone="gold"
          delay={0.28}
        />
      </div>

      {/* ── Eng yaxshi kun + top blok ── */}
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.32, duration: 0.4 }}
        className="mt-4 space-y-3"
      >
        {best.saved > 0 && (
          <div className="flex items-center gap-3.5 rounded-[22px] border border-amber-400/20 bg-gradient-to-r from-amber-400/10 to-transparent p-4">
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-amber-400/15 text-amber-300 ring-1 ring-amber-300/30">
              <Trophy size={18} />
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-[13.5px] font-extrabold text-white">Eng yaxshi kun — {best.full}</p>
              <p className="text-[11.5px] text-white/50">
                {formatMinutes(best.saved)} tejaldi · ekran {formatMinutes(best.screen)}
              </p>
            </div>
            <Flame size={16} className="shrink-0 text-amber-300" />
          </div>
        )}
        {topBlocked && (
          <div className="flex items-center gap-3.5 rounded-[22px] border border-white/10 bg-white/[0.04] p-4">
            <span
              className={cn(
                'flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl text-[20px] ring-1 ring-white/15',
                topBlocked.gradient
              )}
            >
              {topBlocked.emoji}
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-[13.5px] font-extrabold text-white">
                Eng ko‘p bloklangan — {topBlocked.name}
              </p>
              <p className="text-[11.5px] text-white/50">
                Bugun {formatMinutes(topBlocked.todayMinutes)} bloklandi
              </p>
            </div>
            <ShieldCheck size={16} className="shrink-0 text-[#9fe8b5]" />
          </div>
        )}

        {/* streak satrı */}
        {profile && (
          <div className="flex items-center justify-between rounded-[22px] border border-white/10 bg-white/[0.04] p-4">
            <div className="flex items-center gap-3">
              <Flame size={17} className="text-amber-300" />
              <p className="text-[13.5px] font-extrabold text-white">Joriy streak</p>
            </div>
            <p className="text-[15px] font-extrabold text-[#c9fbdc]">
              {profile.streakDays} kun
              <span className="ml-2 text-[11px] font-semibold text-white/40">
                jami {profile.totalSessions} sessiya
              </span>
            </p>
          </div>
        )}
      </motion.section>
      </div>

      {/* ── Ulashish + PNG eksport ── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4, duration: 0.4 }}
        className="mt-5 flex gap-2.5"
      >
        <motion.button
          whileTap={{ scale: 0.97 }}
          onClick={share}
          disabled={sharing || exporting}
          className="flex flex-1 items-center justify-center gap-2 rounded-full border border-[#86efac]/35 bg-gradient-to-b from-emerald-400/25 to-emerald-500/12 py-4 text-[15px] font-bold text-emerald-100 shadow-[0_0_24px_rgba(134,239,172,0.18),inset_0_1px_0_rgba(255,255,255,0.15)] backdrop-blur-xl active:scale-[0.98] disabled:opacity-60"
        >
          <Share2 size={16} /> {sharing ? 'Ulanmoqda…' : 'Ulashish'}
        </motion.button>
        <motion.button
          whileTap={{ scale: 0.97 }}
          onClick={exportPng}
          disabled={exporting || sharing}
          aria-label="Hisobotni PNG sifatida yuklab olish"
          className="flex w-[58px] shrink-0 items-center justify-center rounded-full border border-white/12 bg-white/[0.05] text-white/70 backdrop-blur-xl transition-colors hover:text-white active:scale-[0.98] disabled:opacity-60"
        >
          {exporting ? (
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/25 border-t-white/85" aria-hidden="true" />
          ) : (
            <ImageDown size={17} />
          )}
        </motion.button>
      </motion.div>
      <p className="mt-2.5 text-center text-[10.5px] text-white/35">
        Opal · Apple Design Award 2025 uslubi — haftada bir marta yangilanadi
      </p>
    </div>
  )
}

/* ── kichik stat karta ── */
function StatCard({
  icon,
  label,
  value,
  sub,
  tone,
  delay,
}: {
  icon: React.ReactNode
  label: string
  value: string
  sub: string
  tone: 'mint' | 'sky' | 'violet' | 'gold'
  delay: number
}) {
  const tones = {
    mint: 'text-[#9fe8b5] bg-[#86efac]/12 ring-[#86efac]/25',
    sky: 'text-sky-300 bg-sky-400/12 ring-sky-300/25',
    violet: 'text-violet-300 bg-violet-400/12 ring-violet-300/25',
    gold: 'text-amber-300 bg-amber-400/12 ring-amber-300/25',
  }[tone]
  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.38 }}
      className="rounded-[22px] border border-white/10 bg-white/[0.04] p-3.5"
    >
      <span className={cn('flex h-8 w-8 items-center justify-center rounded-xl ring-1', tones)}>{icon}</span>
      <p className="mt-2.5 text-[10px] font-bold uppercase tracking-wider text-white/40">{label}</p>
      <p className="mt-0.5 truncate text-[17px] font-extrabold text-white">{value}</p>
      <p className="truncate text-[10.5px] font-medium text-white/40">{sub}</p>
    </motion.div>
  )
}
