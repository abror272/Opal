'use client'

import { useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { AnimatePresence, motion } from 'framer-motion'
import type { BlockApp } from '@/lib/opal-types'
import { formatMinutes } from '@/lib/opal-types'
import { useOpalStore } from '@/lib/opal-store'
import { GLASS } from '@/lib/opal-ui'
import { Skeleton } from '@/components/ui/skeleton'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { ChevronRight, Lock, Plus, ShieldCheck, Clock3, Zap, MousePointerClick, CalendarDays } from 'lucide-react'
import { UnblockChallenge } from './unblock-challenge'
import type { SessionType } from '@/lib/opal-types'

/* ── Rutinlar (haqiqiy Opal "Routines" bento) ─────────────── */
interface RuleCard {
  id: string
  title: string
  time: string
  sub: string
  photo?: string
  gradient: string
  icon: string
  duration: number
  type: SessionType
  label: string
  emoji: string
  left?: string
}

const DEFAULT_RULES: RuleCard[] = [
  {
    id: 'unblock-daily',
    title: '10 Unblock Daily',
    time: 'Har kuni',
    sub: 'Ijtimoiy ilovalar uchun',
    gradient: 'from-[#2a3b5c] to-[#141c30]',
    icon: '🔓',
    duration: 30,
    type: 'CUSTOM',
    label: 'Unblock Daily',
    emoji: '🔓',
    left: '7 left',
  },
  {
    id: 'sleep-time',
    title: 'Sleep Time',
    time: '10PM — 8AM',
    sub: 'Block All',
    photo: '/opal/routine-sleep.jpg',
    gradient: 'from-[#1a1f3d] to-[#0a0d20]',
    icon: '🌙',
    duration: 480,
    type: 'SLEEP',
    label: 'Uyqu rejimi',
    emoji: '🌙',
  },
  {
    id: 'deep-work',
    title: 'Deep Work',
    time: '9AM — 5PM',
    sub: 'Block All, Except Productivity',
    photo: '/opal/routine-deepwork.jpg',
    gradient: 'from-[#26221c] to-[#0f0d0a]',
    icon: '💻',
    duration: 90,
    type: 'WORK',
    label: 'Ish rejimi',
    emoji: '💼',
    left: '4h 32m left',
  },
  {
    id: 'lunch-break',
    title: 'Lunch Break',
    time: '12—1PM',
    sub: 'Unblock Snapchat if blocked',
    photo: '/opal/routine-family.jpg',
    gradient: 'from-[#1c2626] to-[#0a1010]',
    icon: '🍽️',
    duration: 60,
    type: 'STUDY',
    label: 'O‘qish',
    emoji: '📚',
  },
  {
    id: 'evening-off',
    title: '10PM—8AM',
    time: 'Tungi himoya',
    sub: 'Block Social',
    gradient: 'from-[#241c33] to-[#0d0a14]',
    icon: '🛡️',
    duration: 120,
    type: 'CUSTOM',
    label: 'Tungi tinchlik',
    emoji: '🛡️',
    left: '7 left',
  },
]

const CUSTOM_RULES_KEY = 'opal-custom-rules'

/** deterministic 0..1 pseudo-random */
function seededRand(seed: string, i: number): number {
  let h = 2166136261
  const s = seed + ':' + i
  for (let c = 0; c < s.length; c++) {
    h ^= s.charCodeAt(c)
    h = Math.imul(h, 16777619)
  }
  return ((h >>> 0) % 1000) / 1000
}

/** 24h usage — ijtimoiy ilovalar kechqurun cho'qqida */
function usageTimeline(app: BlockApp): number[] {
  const eveningHeavy = ['Ijtimoiy tarmoq', 'Zerikarli', 'O‘yinlar', 'Messenger'].includes(app.category)
  return Array.from({ length: 24 }, (_, h) => {
    const base = eveningHeavy
      ? h >= 19 && h <= 23
        ? 0.55 + seededRand(app.name, h) * 0.45
        : h >= 12 && h <= 14
          ? 0.25 + seededRand(app.name, h) * 0.3
          : seededRand(app.name, h) * 0.22
      : h >= 9 && h <= 17
        ? 0.4 + seededRand(app.name, h) * 0.4
        : seededRand(app.name, h) * 0.2
    return Math.min(base, 1)
  })
}

/* ── Ilova tafsiloti varag'i ──────────────────────────────── */
function AppDetailSheet({
  app,
  onClose,
  onBlock,
  onLimit,
}: {
  app: BlockApp
  onClose: () => void
  onBlock: (app: BlockApp) => void
  onLimit: (id: string, minutes: number) => void
}) {
  const timeline = useMemo(() => usageTimeline(app), [app])
  const peakHour = timeline.indexOf(Math.max(...timeline))
  const limitProgress =
    app.dailyLimitMinutes > 0 ? Math.min(app.todayMinutes / app.dailyLimitMinutes, 1) : 0
  const overLimit = app.dailyLimitMinutes > 0 && app.todayMinutes > app.dailyLimitMinutes
  const weekMinutes = app.todayMinutes * (5 + Math.round(seededRand(app.name, 99) * 3))

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="absolute inset-0 z-[60] flex flex-col justify-end bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={{ type: 'spring', stiffness: 340, damping: 34 }}
        onClick={(e) => e.stopPropagation()}
        className="max-h-[86%] overflow-y-auto thin-scrollbar rounded-t-[32px] border-t border-white/12 bg-[#0b0e18] px-5 pb-8 pt-3"
        role="dialog"
        aria-label={`${app.name} tafsilotlari`}
      >
        <div className="mx-auto mb-4 h-1.5 w-12 rounded-full bg-white/15" aria-hidden="true" />

        {/* hero */}
        <div className="flex items-center gap-4">
          <span
            className={cn(
              'flex h-16 w-16 items-center justify-center rounded-[20px] text-[30px] ring-1 ring-white/15',
              app.gradient
            )}
          >
            {app.emoji}
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-[19px] font-extrabold text-white">{app.name}</p>
            <p className="text-[12px] font-medium text-white/45">{app.category}</p>
          </div>
          <span
            className={cn(
              'flex items-center gap-1 rounded-full px-3 py-1.5 text-[11px] font-bold ring-1',
              overLimit
                ? 'bg-rose-500/15 text-rose-300 ring-rose-400/30'
                : 'bg-emerald-500/15 text-emerald-300 ring-emerald-400/30'
            )}
          >
            {overLimit ? 'Limit oshdi' : 'Nazoratda'}
          </span>
        </div>

        {/* bugungi foydalanish */}
        <div className="mt-5 rounded-2xl border border-white/8 bg-white/[0.04] p-4">
          <div className="flex items-center justify-between">
            <p className="text-[12.5px] font-bold text-white/80">Bugungi foydalanish</p>
            <p className="text-[13px] font-extrabold text-[#9fd8ff]">
              {formatMinutes(app.todayMinutes)}
              {app.dailyLimitMinutes > 0 && (
                <span className="text-[11px] font-semibold text-white/40">
                  {' '}/ {formatMinutes(app.dailyLimitMinutes)}
                </span>
              )}
            </p>
          </div>
          <div className="mt-2.5 h-2 overflow-hidden rounded-full bg-white/10">
            <div
              className={cn(
                'h-full rounded-full transition-all',
                overLimit
                  ? 'bg-gradient-to-r from-rose-400 to-rose-500'
                  : 'bg-gradient-to-r from-[#7dd3fc] to-[#b18cff]'
              )}
              style={{ width: `${Math.max(limitProgress * 100, 4)}%` }}
            />
          </div>

          {/* 24h timeline */}
          <p className="mt-4 text-[10.5px] font-bold uppercase tracking-widest text-white/40">
            24 soatlik faollik · cho‘qqi {String(peakHour).padStart(2, '0')}:00
          </p>
          <div className="mt-2 flex h-16 items-end gap-[3px]">
            {timeline.map((v, h) => (
              <div
                key={h}
                title={`${String(h).padStart(2, '0')}:00 — ${Math.round(v * 60)} daqiqa`}
                className={cn(
                  'flex-1 rounded-t-[3px] transition-all',
                  h === peakHour ? 'bg-[#9fd8ff]' : 'bg-white/15'
                )}
                style={{ height: `${Math.max(v * 100, 6)}%` }}
              />
            ))}
          </div>

          {/* mini statlar */}
          <div className="mt-4 grid grid-cols-3 gap-2 text-center">
            {[
              { icon: <Clock3 size={12} />, label: 'Bu hafta', val: formatMinutes(weekMinutes) },
              { icon: <MousePointerClick size={12} />, label: 'Ochilish', val: `${8 + Math.round(seededRand(app.name, 7) * 22)} marta` },
              { icon: <Zap size={12} />, label: 'Eng uzun', val: `${Math.round(8 + seededRand(app.name, 3) * 40)} daq` },
            ].map((s) => (
              <div key={s.label} className="rounded-xl bg-white/[0.05] p-2.5 ring-1 ring-white/8">
                <p className="flex items-center justify-center gap-1 text-[10px] font-semibold text-white/40">
                  {s.icon} {s.label}
                </p>
                <p className="mt-0.5 text-[12.5px] font-extrabold text-white">{s.val}</p>
              </div>
            ))}
          </div>
        </div>

        {/* limit tanlash */}
        <p className="mb-2 mt-5 text-[11px] font-bold uppercase tracking-widest text-white/40">
          Kunlik limit
        </p>
        <div className="no-scrollbar flex gap-2 overflow-x-auto pb-1">
          {[0, 15, 30, 45, 60, 120].map((m) => (
            <button
              key={m}
              onClick={() => onLimit(app.id, m)}
              className={cn(
                'shrink-0 rounded-full px-4 py-2 text-[12px] font-bold ring-1 transition-all active:scale-95',
                app.dailyLimitMinutes === m
                  ? 'bg-[#7dd3fc]/15 text-[#bfe9ff] ring-[#7dd3fc]/45'
                  : 'bg-white/[0.05] text-white/60 ring-white/10 hover:text-white'
              )}
            >
              {m === 0 ? 'Yo‘q' : formatMinutes(m)}
            </button>
          ))}
        </div>

        {/* Block tugmasi */}
        <button
          onClick={() => onBlock(app)}
          className="mt-5 flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-[#7dd3fc]/20 to-[#b18cff]/20 py-3.5 text-[14px] font-bold text-white ring-1 ring-[#7dd3fc]/30 active:scale-[0.98]"
        >
          <Lock size={14} /> {app.name}ni bloklash
        </button>
      </motion.div>
    </motion.div>
  )
}

/* ── Asosiy Apps tab ──────────────────────────────────────── */
export function AppsTab() {
  const qc = useQueryClient()
  const setTab = useOpalStore((s) => s.setTab)
  const setTimerDraft = useOpalStore((s) => s.setTimerDraft)

  const appsQ = useQuery<BlockApp[]>({
    queryKey: ['apps'],
    queryFn: async () => (await fetch('/api/apps')).json(),
  })

  const blocked = (appsQ.data ?? []).filter((a) => a.blocked)
  const allowed = (appsQ.data ?? []).filter((a) => !a.blocked)

  const [challengeApp, setChallengeApp] = useState<BlockApp | null>(null)
  const [sheetApp, setSheetApp] = useState<BlockApp | null>(null)
  const [addRuleOpen, setAddRuleOpen] = useState(false)
  const [customRules, setCustomRules] = useState<RuleCard[]>([])
  const [newRuleName, setNewRuleName] = useState('')
  const [newRuleTime, setNewRuleTime] = useState('9AM — 5PM')

  // localStorage'dan bir marta yuklash (render-adjust pattern)
  const [rulesLoaded, setRulesLoaded] = useState(false)
  if (!rulesLoaded) {
    setRulesLoaded(true)
    try {
      if (typeof window !== 'undefined') {
        const raw = window.localStorage.getItem(CUSTOM_RULES_KEY)
        if (raw) setCustomRules(JSON.parse(raw) as RuleCard[])
      }
    } catch {
      // e'tiborsiz
    }
  }

  const saveCustomRules = (rules: RuleCard[]) => {
    setCustomRules(rules)
    try {
      window.localStorage.setItem(CUSTOM_RULES_KEY, JSON.stringify(rules))
    } catch {
      // e'tiborsiz
    }
  }

  const patchApp = useMutation({
    mutationFn: async ({ id, patch }: { id: string; patch: Partial<BlockApp> }) => {
      const res = await fetch('/api/apps', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, ...patch }),
      })
      if (!res.ok) throw new Error('patch failed')
      return res.json()
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['apps'] })
      qc.invalidateQueries({ queryKey: ['apps', 'blocked'] })
    },
  })

  const rules = [...DEFAULT_RULES, ...customRules]

  const startRule = (rule: RuleCard) => {
    setTimerDraft({
      type: rule.type,
      label: rule.label,
      emoji: rule.emoji,
      durationMinutes: rule.duration,
      strict: false,
    })
    setTab('timer')
  }

  const addRule = () => {
    const name = newRuleName.trim()
    if (!name) {
      toast.error('Qoida nomini kiriting')
      return
    }
    const rule: RuleCard = {
      id: `custom-${Date.now()}`,
      title: name,
      time: newRuleTime.trim() || 'Har kuni',
      sub: 'Block distracting apps',
      gradient: 'from-[#2d2a4e] to-[#12101f]',
      icon: '🛡️',
      duration: 60,
      type: 'CUSTOM',
      label: name,
      emoji: '🛡️',
    }
    saveCustomRules([...customRules, rule])
    setNewRuleName('')
    setAddRuleOpen(false)
    toast.success('✅ Qo‘shildi', { description: `${name} rutini tayyor` })
  }

  if (appsQ.isLoading) {
    return (
      <div className="space-y-4 px-5 pt-2">
        <Skeleton className="h-8 w-32 bg-white/5" />
        <Skeleton className="h-24 w-full rounded-3xl bg-white/5" />
        <Skeleton className="h-48 w-full rounded-3xl bg-white/5" />
      </div>
    )
  }

  return (
    <div className="px-5 pb-4 pt-1">
      <header className="flex items-center justify-between">
        <h1 className="text-[24px] font-extrabold tracking-tight text-white">Apps</h1>
        <span
          className="inline-block h-7 w-7 rounded-full bg-gradient-to-br from-[#8fd9ff]/40 via-[#b18cff]/40 to-[#ff9ad5]/40 shadow-[0_0_10px_rgba(143,217,255,0.4)] ring-1 ring-white/20"
          aria-hidden="true"
        />
      </header>

      {/* ── Bloklangan ───────────────────────────────── */}
      <section className="mt-4" aria-label="Bloklangan ilovalar">
        <p className="mb-2.5 text-[14.5px] font-bold text-white/85">Blocked</p>
        {blocked.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-white/12 py-5 text-center text-[12.5px] text-white/40">
            Hozircha hech narsa bloklanmagan — quyida ilova tanlang
          </p>
        ) : (
          <div className="grid grid-cols-4 gap-x-3 gap-y-3.5">
            {blocked.map((app) => (
              <motion.button
                key={app.id}
                whileTap={{ scale: 0.92 }}
                onClick={() => setChallengeApp(app)}
                className="flex flex-col items-center gap-1.5"
                aria-label={`${app.name}ni blokdan chiqarish`}
              >
                <span
                  className={cn(
                    'relative flex h-[54px] w-full max-w-[62px] items-center justify-center rounded-[16px] text-[24px] ring-[1.5px] ring-[#8fd9ff]/60',
                    app.gradient
                  )}
                  style={{ boxShadow: '0 0 16px rgba(125,211,252,0.35)' }}
                >
                  {app.emoji}
                  <span className="absolute inset-0 flex items-center justify-center rounded-[16px] bg-black/25">
                    <Lock size={17} className="text-white/90 drop-shadow" />
                  </span>
                </span>
                <span className="w-full max-w-[62px] truncate text-center text-[10px] font-semibold text-white/85">
                  {app.name}
                </span>
                <span className="text-[9px] font-bold text-[#9fd8ff]">Unblock</span>
              </motion.button>
            ))}
          </div>
        )}
      </section>

      {/* ── Rules ────────────────────────────────────── */}
      <section className="mt-6" aria-label="Qoidalar">
        <p className="mb-2.5 flex items-center gap-1 text-[14.5px] font-bold text-white/85">
          Rules <ChevronRight size={14} className="text-white/35" />
        </p>
        <div className="grid grid-cols-2 gap-3">
          {rules.map((rule, i) => (
            <motion.button
              key={rule.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              whileTap={{ scale: 0.97 }}
              onClick={() => startRule(rule)}
              className={cn(
                'relative flex min-h-[148px] flex-col justify-end overflow-hidden rounded-[22px] border border-white/10 bg-gradient-to-b p-3.5 text-left shadow-[0_10px_28px_rgba(0,0,0,0.4)]',
                rule.gradient
              )}
            >
              {rule.photo && (
                <>
                  <img
                    src={rule.photo}
                    alt=""
                    className="absolute inset-0 h-full w-full object-cover opacity-80"
                    draggable={false}
                  />
                  <div className="absolute inset-0 bg-gradient-to-b from-black/10 via-transparent to-black/75" aria-hidden="true" />
                </>
              )}
              <span className="relative mb-auto text-[20px]">{rule.icon}</span>
              {rule.left && (
                <span className="relative mb-2 inline-flex w-fit items-center rounded-full bg-[#7dd3fc]/15 px-2.5 py-0.5 text-[9.5px] font-bold text-[#bfe9ff] ring-1 ring-[#7dd3fc]/40">
                  {rule.left}
                </span>
              )}
              <span className="relative text-[14.5px] font-extrabold leading-tight text-white drop-shadow">
                {rule.title}
              </span>
              <span className="relative mt-0.5 text-[10.5px] font-semibold text-white/70 drop-shadow">
                {rule.time}
              </span>
              <span className="relative truncate text-[9.5px] font-medium text-white/55 drop-shadow">
                {rule.sub}
              </span>
            </motion.button>
          ))}

          {/* Add Rule */}
          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={() => setAddRuleOpen(true)}
            className="flex min-h-[148px] flex-col items-center justify-center gap-2 rounded-[22px] border border-dashed border-white/15 bg-white/[0.03] text-white/50 transition-colors hover:text-white/80"
          >
            <Plus size={22} />
            <span className="text-[12px] font-bold">Add Rule</span>
          </motion.button>
        </div>
      </section>

      {/* ── Boshqa ilovalar ──────────────────────────── */}
      <section className="mt-6" aria-label="Blokdan chiqarilgan ilovalar">
        <p className="mb-2.5 text-[14.5px] font-bold text-white/85">Apps</p>
        <div className="grid grid-cols-4 gap-x-3 gap-y-3.5">
          {allowed.map((app) => (
            <motion.button
              key={app.id}
              whileTap={{ scale: 0.92 }}
              onClick={() => setSheetApp(app)}
              className="flex flex-col items-center gap-1.5"
              aria-label={`${app.name} tafsilotlari`}
            >
              <span
                className={cn(
                  'flex h-[54px] w-full max-w-[62px] items-center justify-center rounded-[16px] text-[24px] ring-1 ring-white/10',
                  app.gradient
                )}
              >
                {app.emoji}
              </span>
              <span className="w-full truncate text-center text-[9.5px] font-semibold text-white/50">
                {app.name}
              </span>
            </motion.button>
          ))}
        </div>
      </section>

      {/* Unblock challenge */}
      <AnimatePresence>
        {challengeApp && (
          <UnblockChallenge
            key="challenge"
            appName={challengeApp.name}
            onSolved={() => {
              patchApp.mutate({ id: challengeApp.id, patch: { blocked: false } })
              toast.success(`🔓 ${challengeApp.name} blokdan chiqarildi`, {
                description: 'Battle Math yechildi — o‘sishga loyiq!',
              })
              setChallengeApp(null)
            }}
            onClose={() => setChallengeApp(null)}
          />
        )}
      </AnimatePresence>

      {/* App detail sheet */}
      <AnimatePresence>
        {sheetApp && (
          <AppDetailSheet
            key="sheet"
            app={sheetApp}
            onClose={() => setSheetApp(null)}
            onBlock={(app) => {
              patchApp.mutate({ id: app.id, patch: { blocked: true } })
              toast.success(`🔒 ${app.name} bloklandi`, {
                description: 'Ochish uchun endi Battle Math yechish kerak',
              })
              setSheetApp(null)
            }}
            onLimit={(id, minutes) => {
              patchApp.mutate({ id, patch: { dailyLimitMinutes: minutes } })
              toast.success(
                minutes === 0 ? 'Limit olib tashlandi' : `Limit ${formatMinutes(minutes)} qilib qo'yildi`
              )
            }}
          />
        )}
      </AnimatePresence>

      {/* Add rule dialog */}
      <Dialog open={addRuleOpen} onOpenChange={(o) => !o && setAddRuleOpen(false)}>
        <DialogContent
          aria-describedby={undefined}
          className="w-[calc(100%-2rem)] max-w-[320px] translate-y-[-70%] rounded-3xl border border-white/12 bg-[#0c0f1c] p-5 shadow-2xl [top:50%]"
        >
          <DialogTitle className="flex items-center gap-2 text-[16px] font-extrabold text-white">
            <CalendarDays size={16} className="text-[#9fd8ff]" /> Yangi qoida
          </DialogTitle>
          <div className="mt-4 space-y-3">
            <input
              value={newRuleName}
              onChange={(e) => setNewRuleName(e.target.value)}
              placeholder="Qoida nomi (masalan: Sport vaqti)"
              maxLength={28}
              aria-label="Qoida nomi"
              className="h-11 w-full rounded-2xl border border-white/12 bg-white/[0.06] px-3.5 text-[13.5px] font-semibold text-white outline-none placeholder:text-white/30 focus:border-[#7dd3fc]/50"
            />
            <input
              value={newRuleTime}
              onChange={(e) => setNewRuleTime(e.target.value)}
              placeholder="Vaqt (masalan: 6PM — 8PM)"
              maxLength={24}
              aria-label="Qoida vaqti"
              className="h-11 w-full rounded-2xl border border-white/12 bg-white/[0.06] px-3.5 text-[13.5px] font-semibold text-white outline-none placeholder:text-white/30 focus:border-[#7dd3fc]/50"
            />
            <button
              onClick={addRule}
              className="w-full rounded-2xl bg-gradient-to-r from-[#5b7bff] to-[#8b5cf6] py-3 text-[14px] font-bold text-white shadow-lg active:scale-[0.98]"
            >
              Qoidani qo‘shish
            </button>
            <p className="text-center text-[10.5px] text-white/35">
              Qoidani bosganda taymer mos davomiylik bilan ochiladi
            </p>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
