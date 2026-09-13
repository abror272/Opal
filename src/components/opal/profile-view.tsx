'use client'

import { useEffect, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatMinutes, type FocusSession, type UserProfile } from '@/lib/opal-types'
import { useOpalStore } from '@/lib/opal-store'
import { GLASS, OPAL, gemsFor, worldwideTopPercent } from '@/lib/opal-ui'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { PinPad } from './pin-pad'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import {
  Flame,
  Hourglass,
  Globe,
  Crown,
  ShieldCheck,
  Lock,
  Bell,
  Gauge,
  LogOut,
  CircleHelp,
  Sparkles,
  KeyRound,
  ChevronRight,
} from 'lucide-react'

function achievementsFor(profile: UserProfile) {
  return [
    { emoji: '🌱', label: 'Birinchi qadam', desc: '1-sessiya', unlocked: profile.totalSessions >= 1 },
    { emoji: '🔥', label: '7 kunlik streak', desc: 'Haftalik odat', unlocked: profile.streakDays >= 7 },
    { emoji: '💎', label: '100 sessiya', desc: 'Temir iroda', unlocked: profile.totalSessions >= 100 },
    { emoji: '🚀', label: '50 soat tejash', desc: 'Vaqt LORDI', unlocked: profile.totalSavedMinutes >= 3000 },
    { emoji: '🌙', label: 'Tungi rejim', desc: 'Uyqu sessiyasi', unlocked: profile.totalSessions >= 1 },
    { emoji: '👑', label: 'Opal Plus', desc: 'Premium a’zo', unlocked: profile.plan === 'PLUS' },
  ]
}

/** Haqiqiy Opal profil gerbi: dafna chambeli + olti burchakli avatar */
function ProfileCrest({ initial }: { initial: string }) {
  // dafna yaproqlari — ikki yoy bo'ylab (pastki markazda bo'shliq, yonlarda yuqoriga)
  const leaves = Array.from({ length: 7 }, (_, i) => i)
  const leaf = (angleDeg: number, radius: number, flip: boolean, key: number) => {
    const a = (angleDeg * Math.PI) / 180
    const cx = 100 + Math.cos(a) * radius
    const cy = 100 - Math.sin(a) * radius * 0.92
    const rot = angleDeg + (flip ? 62 : -62)
    return (
      <ellipse
        key={key}
        cx={cx}
        cy={cy}
        rx="7"
        ry="15"
        fill="rgba(183,245,205,0.10)"
        stroke="rgba(183,245,205,0.16)"
        strokeWidth="1"
        transform={`rotate(${rot} ${cx} ${cy})`}
      />
    )
  }
  return (
    <div className="relative mx-auto flex h-[168px] w-[268px] items-center justify-center">
      <svg viewBox="0 0 200 200" className="absolute inset-0 h-full w-full" aria-hidden="true">
        {/* chap shox — 190°→262° (pastki chap, yuqoriga yo'nalgan) */}
        {leaves.map((i) => leaf(192 + i * 12, 94, false, i))}
        {/* o'ng shox — 350°→278° (pastki o'ng) */}
        {leaves.map((i) => leaf(-12 - i * 12, 94, true, 100 + i))}
      </svg>
      {/* olti burchakli avatar */}
      <div className="relative flex h-[92px] w-[92px] items-center justify-center">
        <svg viewBox="0 0 36 36" className="absolute inset-0 h-full w-full" aria-hidden="true">
          <path
            d="M18 2.5 L31.5 9.2 V26.8 L18 33.5 L4.5 26.8 V9.2 Z"
            fill="rgba(183,245,205,0.08)"
            stroke={OPAL.mint}
            strokeWidth="1.3"
            strokeLinejoin="round"
            style={{ filter: `drop-shadow(0 0 10px ${OPAL.mintGlow})` }}
          />
        </svg>
        <span className="relative flex h-11 w-11 items-center justify-center rounded-full bg-gradient-to-br from-[#9fe8b5] to-[#5eead4] text-[20px] font-black text-[#06281a] shadow-[0_0_18px_rgba(134,239,172,0.4)]">
          {initial}
        </span>
      </div>
    </div>
  )
}

/** Haqiqiy Opal katta statistikasi — nur ichida ikonka, raqam ustida, yorliq pastda */
function BigStat({
  icon,
  value,
  label,
  glow,
}: {
  icon: React.ReactNode
  value: string
  label: string
  glow: string
}) {
  return (
    <div className="flex flex-col items-center">
      <div className="relative flex h-[74px] w-[86px] items-end justify-center">
        {/* nur fon */}
        <div
          className="absolute left-1/2 top-1 h-[64px] w-[64px] -translate-x-1/2 rounded-full"
          style={{ background: `radial-gradient(circle, ${glow} 0%, transparent 68%)` }}
          aria-hidden="true"
        />
        <span className="relative text-[34px] opacity-90 [&>svg]:h-full [&>svg]:w-full" style={{ color: glow.includes('flame') ? '#ffc46b' : undefined }}>
          {icon}
        </span>
        <span className="absolute bottom-0 text-[21px] font-extrabold leading-none text-white drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
          {value}
        </span>
      </div>
      <span className="mt-1.5 text-[10px] font-bold uppercase tracking-[0.14em] text-white/60">
        {label}
      </span>
    </div>
  )
}

export function ProfileView() {
  const qc = useQueryClient()
  const pinEnabled = useOpalStore((s) => s.pinEnabled)
  const setPin = useOpalStore((s) => s.setPin)
  const removePin = useOpalStore((s) => s.removePin)
  const seenAchievements = useOpalStore((s) => s.seenAchievements)
  const markAchievementsSeen = useOpalStore((s) => s.markAchievementsSeen)
  const [pinDialogOpen, setPinDialogOpen] = useState(false)

  const profileQ = useQuery<UserProfile>({
    queryKey: ['profile'],
    queryFn: async () => (await fetch('/api/profile')).json(),
  })
  const statsQ = useQuery<{ weekSavedMinutes: number }>({
    queryKey: ['stats'],
    queryFn: async () => (await fetch('/api/stats')).json(),
  })
  const sessionsQ = useQuery<FocusSession[]>({
    queryKey: ['sessions'],
    queryFn: async () => (await fetch('/api/sessions')).json(),
  })

  const patchMutation = useMutation({
    mutationFn: async (patch: Partial<UserProfile>) => {
      const res = await fetch('/api/profile', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(patch),
      })
      return res.json()
    },
    onMutate: async (patch) => {
      await qc.cancelQueries({ queryKey: ['profile'] })
      const prev = qc.getQueryData<UserProfile>(['profile'])
      if (prev) qc.setQueryData<UserProfile>(['profile'], { ...prev, ...patch })
      return { prev }
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(['profile'], ctx.prev)
      toast.error('Saqlash bajarilmadi')
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  })

  if (profileQ.isLoading) {
    return (
      <div className="space-y-5 px-5 pt-3">
        <Skeleton className="h-28 w-full rounded-3xl bg-white/5" />
        <Skeleton className="h-40 w-full rounded-3xl bg-white/5" />
        <Skeleton className="h-64 w-full rounded-3xl bg-white/5" />
      </div>
    )
  }

  const profile = profileQ.data
  if (!profile) return null

  const isPlus = profile.plan === 'PLUS'
  const achievements = achievementsFor(profile)

  const todayIso = new Date().toISOString().slice(0, 10)
  const hadSleep = (sessionsQ.data ?? []).some(
    (s) => s.type === 'SLEEP' && (s.endedAt ?? s.startedAt).slice(0, 10) === todayIso
  )
  const gems = gemsFor(profile, hadSleep)
  const topPct = worldwideTopPercent(statsQ.data?.weekSavedMinutes ?? 0)
  const focusHours = Math.round(profile.totalSavedMinutes / 60)

  return (
    <div className="space-y-5 px-5 pb-6 pt-1">
      <AchievementWatcher
        achievements={achievements}
        seen={seenAchievements}
        onSeen={markAchievementsSeen}
      />

      {/* ── GERB: dafna + hexagon avatar + ism ── */}
      <section className="pt-2 text-center" aria-label="Profil gerbi">
        <ProfileCrest initial={profile.name[0]} />
        <p className="mt-2 text-[26px] font-extrabold tracking-tight text-white">
          {profile.name}
        </p>
        <p className="mt-0.5 text-[12px] font-medium text-white/40">{profile.handle}</p>
        {isPlus && (
          <span className="mt-1.5 inline-flex items-center gap-1 rounded-full bg-amber-400/15 px-2.5 py-0.5 text-[10px] font-bold text-amber-300 ring-1 ring-amber-400/35">
            <Crown size={10} /> OPAL PLUS
          </span>
        )}

        {/* 3 katta statistika (haqiqiy Opal tartibida) */}
        <div className="mt-4 grid grid-cols-3 gap-1">
          <BigStat
            icon={<Hourglass />}
            value={focusHours > 0 ? `${focusHours}h` : '--'}
            label="Focus Hours"
            glow="rgba(183,245,205,0.28)"
          />
          <BigStat
            icon={<Flame className="fill-[#ffb85c]/45 text-[#ffc46b]" />}
            value={`${profile.streakDays}`}
            label="Day Streak"
            glow="rgba(255,184,92,0.30)"
          />
          <BigStat
            icon={<Globe />}
            value={`Top ${topPct}%`}
            label="Worldwide"
            glow="rgba(94,234,212,0.28)"
          />
        </div>
      </section>

      {/* ── GEMSTONES karuseli (haqiqiy Opal) ── */}
      <section aria-label="Gemstones">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-[16px] font-bold text-white">Gemstones</h3>
          <span className="text-[11.5px] font-semibold text-white/40">
            {gems.filter((g) => g.unlocked).length}/{gems.length} to‘plandi
          </span>
        </div>
        <div className="no-scrollbar -mx-5 flex gap-4 overflow-x-auto px-5 pb-2">
          {gems.map((g) => (
            <div key={g.key} className="flex w-[92px] shrink-0 flex-col items-center gap-2">
              <div className="relative flex h-[76px] w-[76px] items-center justify-center">
                {g.unlocked && (
                  <div
                    className="absolute inset-0 rounded-full"
                    style={{
                      background: `radial-gradient(circle, ${g.colors[1]}55 0%, transparent 70%)`,
                    }}
                    aria-hidden="true"
                  />
                )}
                {/* tosh shakli — qirrali blob */}
                <div
                  className={cn(
                    'relative h-[58px] w-[58px] transition-all',
                    !g.unlocked && 'opacity-30 grayscale'
                  )}
                  style={{
                    borderRadius: '42% 58% 55% 45% / 48% 44% 56% 52%',
                    background: `radial-gradient(circle at 32% 28%, ${g.colors[0]} 0%, ${g.colors[1]} 48%, ${g.colors[2]} 100%)`,
                    boxShadow: g.unlocked
                      ? `0 0 22px ${g.colors[1]}77, inset 0 -4px 10px rgba(0,0,0,0.35), inset 0 3px 6px rgba(255,255,255,0.25)`
                      : 'inset 0 -4px 10px rgba(0,0,0,0.4)',
                  }}
                  aria-hidden="true"
                >
                  <span className="absolute left-[22%] top-[16%] h-2.5 w-3 rounded-full bg-white/55 blur-[3px]" />
                  <span className="absolute bottom-[20%] right-[24%] h-1.5 w-1.5 rounded-full bg-white/25 blur-[1px]" />
                </div>
                {!g.unlocked && (
                  <span className="absolute text-[13px] drop-shadow" aria-label="Bloklangan">
                    🔒
                  </span>
                )}
              </div>
              <p className="text-[12.5px] font-bold text-white">{g.name}</p>
              <p className="-mt-1.5 text-[10px] font-medium text-white/40">
                Owned by {g.ownedPct}%
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Jami ko'rsatkichlar ── */}
      <section className={cn(GLASS, 'grid grid-cols-3 gap-2 p-4 text-center')} aria-label="Jami statistika">
        <div>
          <p className="text-[15px] font-extrabold text-white">{formatMinutes(profile.totalSavedMinutes)}</p>
          <p className="mt-0.5 text-[9.5px] font-semibold uppercase tracking-wider text-white/40">
            Time Saved
          </p>
        </div>
        <div className="border-x border-white/8">
          <p className="text-[15px] font-extrabold text-white">{profile.totalSessions}</p>
          <p className="mt-0.5 text-[9.5px] font-semibold uppercase tracking-wider text-white/40">
            Sessions
          </p>
        </div>
        <div>
          <p className="text-[15px] font-extrabold text-white">
            {Math.round((statsQ.data?.weekSavedMinutes ?? 0) / 7)}d
          </p>
          <p className="mt-0.5 text-[9.5px] font-semibold uppercase tracking-wider text-white/40">
            AVG Daily Saved
          </p>
        </div>
      </section>

      {/* Opal Plus */}
      {!isPlus ? (
        <button
          onClick={() => {
            patchMutation.mutate({ plan: 'PLUS' })
            toast.success('Opal Plus faollashtirildi! 👑', {
              description: 'Barcha premium funksiyalar ochildi (demo)',
            })
          }}
          className="group relative block w-full overflow-hidden rounded-3xl bg-gradient-to-r from-amber-300 via-orange-300 to-rose-300 p-[1.5px] text-left shadow-[0_10px_36px_rgba(251,191,36,0.25)] transition-transform active:scale-[0.98]"
        >
          <span className="relative flex items-center gap-3 rounded-[calc(1.5rem-1.5px)] bg-[#0c120e] px-4 py-4">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-300 to-orange-400 text-[#0c120e]">
              <Crown size={20} />
            </span>
            <span className="flex-1">
              <span className="block text-[14.5px] font-extrabold text-white">Opal Plus’ga o‘tish</span>
              <span className="block text-[11.5px] text-white/50">
                Cheksiz bloklar, qattiq rejim, maxsus mavzular
              </span>
            </span>
            <ChevronRight size={18} className="text-white/40 transition-transform group-hover:translate-x-0.5" />
          </span>
        </button>
      ) : (
        <div className="flex items-center gap-3 rounded-3xl border border-amber-400/30 bg-amber-400/10 p-4">
          <Crown size={22} className="text-amber-300" />
          <div>
            <p className="text-[14px] font-extrabold text-white">Opal Plus faol 👑</p>
            <p className="text-[11.5px] text-white/50">Hamma premium funksiyalar ochiq</p>
          </div>
        </div>
      )}

      {/* yutuqlar */}
      <section aria-label="Yutuqlar" className={GLASS + ' p-5'}>
        <h3 className="mb-3.5 flex items-center gap-2 text-[15px] font-bold text-white">
          <Sparkles size={16} className="text-[#9fe8b5]" /> Yutuqlar
        </h3>
        <div className="grid grid-cols-3 gap-3">
          {achievements.map((a) => (
            <div
              key={a.label}
              className={cn(
                'flex flex-col items-center gap-1 rounded-2xl p-3 text-center ring-1',
                a.unlocked
                  ? 'bg-gradient-to-b from-[#86efac]/12 to-[#5eead4]/12 ring-[#9fe8b5]/25'
                  : 'bg-white/[0.03] opacity-40 ring-white/8 grayscale'
              )}
            >
              <span className="text-[24px]">{a.emoji}</span>
              <span className="text-[10.5px] font-bold leading-tight text-white/85">{a.label}</span>
              <span className="text-[9.5px] text-white/40">{a.desc}</span>
            </div>
          ))}
        </div>
      </section>

      {/* sozlamalar */}
      <section aria-label="Sozlamalar" className={cn('overflow-hidden', GLASS)}>
        <h3 className="px-5 pt-4 text-[15px] font-bold text-white">Sozlamalar</h3>

        <div className="mt-1 divide-y divide-white/8">
          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/12 text-emerald-300 ring-1 ring-emerald-400/20">
                <ShieldCheck size={17} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-white">Himoya</p>
                <p className="text-[11px] text-white/40">Doimiy blokdan chiqmaslik</p>
              </div>
            </div>
            <Switch
              checked={profile.protectionEnabled}
              onCheckedChange={(v) => patchMutation.mutate({ protectionEnabled: v })}
              aria-label="Himoya sozlamasi"
              className="data-[state=checked]:bg-emerald-500/70"
            />
          </div>

          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/8 text-white/70 ring-1 ring-white/10">
                <Lock size={16} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-white">Qattiq rejim</p>
                <p className="text-[11px] text-white/40">Erta chiqish taqiqlanadi</p>
              </div>
            </div>
            <Switch
              checked={profile.strictMode}
              onCheckedChange={(v) => patchMutation.mutate({ strictMode: v })}
              aria-label="Qattiq rejim sozlamasi"
              className="data-[state=checked]:bg-rose-500/70"
            />
          </div>

          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-500/12 text-rose-300 ring-1 ring-rose-400/20">
                <Bell size={16} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-white">Eslatmalar</p>
                <p className="text-[11px] text-white/40">Limit ogohlantirishlari</p>
              </div>
            </div>
            <Switch defaultChecked aria-label="Eslatmalar sozlamasi" />
          </div>

          {/* kunlik maqsad */}
          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#86efac]/12 text-[#9fe8b5] ring-1 ring-[#86efac]/20">
                <Gauge size={16} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-white">Kunlik ekran maqsadi</p>
                <p className="text-[11px] text-white/40">Hozir: {formatMinutes(profile.goalMinutes)}</p>
              </div>
            </div>
            <div className="flex gap-1.5">
              {[180, 240, 300].map((g) => (
                <button
                  key={g}
                  onClick={() => patchMutation.mutate({ goalMinutes: g })}
                  aria-label={`Maqsad ${g} daqiqa`}
                  className={cn(
                    'rounded-full px-2.5 py-1 text-[10.5px] font-bold ring-1 transition-all active:scale-95',
                    profile.goalMinutes === g
                      ? 'bg-[#9fe8b5]/15 text-[#c9fbdc] ring-[#9fe8b5]/45'
                      : 'bg-white/[0.05] text-white/50 ring-white/10'
                  )}
                >
                  {g / 60}h
                </button>
              ))}
            </div>
          </div>

          {/* PIN qulfi */}
          <button
            onClick={() => {
              if (!pinEnabled) setPinDialogOpen(true)
              else {
                removePin()
                toast.info('PIN qulfi o‘chirildi 🔓')
              }
            }}
            className="flex w-full items-center justify-between px-5 py-3.5 text-left transition-colors hover:bg-white/[0.03]"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/12 text-amber-300 ring-1 ring-amber-400/20">
                <KeyRound size={16} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-white">PIN qulfi</p>
                <p className="text-[11px] text-white/40">
                  {pinEnabled ? 'Ilova PIN bilan himoyalangan' : 'Ilovani PIN kod bilan himoyalang'}
                </p>
              </div>
            </div>
            <span
              className={cn(
                'rounded-full px-2.5 py-1 text-[10.5px] font-bold ring-1',
                pinEnabled
                  ? 'bg-emerald-500/10 text-emerald-300 ring-emerald-400/25'
                  : 'bg-white/5 text-white/40 ring-white/10'
              )}
            >
              {pinEnabled ? 'YONIQ' : 'O‘CHIQ'}
            </span>
          </button>

          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-500/12 text-sky-300 ring-1 ring-sky-400/20">
                <CircleHelp size={16} />
              </div>
              <p className="text-[13.5px] font-bold text-white">Yordam</p>
            </div>
            <ChevronRight size={17} className="text-white/25" />
          </div>

          <button
            onClick={() => toast.info('Bu demo — chiqish shart emas 😊')}
            className="flex w-full items-center justify-between px-5 py-3.5 text-left transition-colors hover:bg-white/[0.03]"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-500/12 text-rose-300 ring-1 ring-rose-400/20">
                <LogOut size={16} />
              </div>
              <p className="text-[13.5px] font-bold text-rose-300">Chiqish</p>
            </div>
            <ChevronRight size={17} className="text-white/25" />
          </button>
        </div>
      </section>

      <p className="pb-1 text-center text-[10.5px] text-white/25">
        Opal Clone · v2.0 · Apple Design Award ruhida
      </p>

      <PinSetupDialog open={pinDialogOpen} onOpenChange={setPinDialogOpen} onSaved={setPin} />
    </div>
  )
}

/** Yangi ochilgan yutuq uchun toast */
function AchievementWatcher({
  achievements,
  seen,
  onSeen,
}: {
  achievements: { emoji: string; label: string; desc: string; unlocked: boolean }[]
  seen: string[]
  onSeen: (labels: string[]) => void
}) {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  useEffect(() => {
    const fresh = achievements.filter((a) => a.unlocked && !seen.includes(a.label))
    if (fresh.length > 0) {
      timer.current = setTimeout(() => {
        for (const a of fresh) {
          toast.success(`${a.emoji} Yutuq ochildi!`, { description: `${a.label} — ${a.desc}` })
        }
        onSeen(fresh.map((a) => a.label))
      }, 800)
    }
    return () => {
      if (timer.current) clearTimeout(timer.current)
    }
  }, [achievements, seen, onSeen])
  return null
}

/** Ikki qadamli PIN sozlash (kiritish → tasdiqlash) */
function PinSetupDialog({
  open,
  onOpenChange,
  onSaved,
}: {
  open: boolean
  onOpenChange: (v: boolean) => void
  onSaved: (code: string) => void
}) {
  const [step, setStep] = useState<'enter' | 'confirm'>('enter')
  const [first, setFirst] = useState('')
  const [errorPulse, setErrorPulse] = useState(0)

  const [lastOpen, setLastOpen] = useState(open)
  if (open !== lastOpen) {
    setLastOpen(open)
    if (open) {
      setStep('enter')
      setFirst('')
      setErrorPulse(0)
    }
  }

  const handle = (pin: string) => {
    if (step === 'enter') {
      setFirst(pin)
      setStep('confirm')
    } else if (pin === first) {
      onSaved(pin)
      onOpenChange(false)
      toast.success('PIN qulfi yoqildi 🔐', { description: 'Ilova endi PIN bilan himoyalanadi' })
    } else {
      setErrorPulse((n) => n + 1)
      toast.error('Kodlar mos kelmadi', { description: 'Qaytadan urinib ko‘ring' })
      setStep('enter')
      setFirst('')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        aria-describedby={undefined}
        className="max-w-[320px] rounded-3xl border border-white/12 bg-[#0c120e] p-6 shadow-2xl"
      >
        <DialogTitle className="sr-only">PIN kod sozlash</DialogTitle>
        <PinPad
          key={step}
          onComplete={handle}
          title={step === 'enter' ? 'Yangi PIN kod' : 'Kodni tasdiqlang'}
          subtitle={step === 'enter' ? 'Esda qoladigan 4 raqam tanlang' : 'Bir xil kodni qayta tering'}
          errorPulse={errorPulse}
        />
      </DialogContent>
    </Dialog>
  )
}
