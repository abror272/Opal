'use client'

import { useEffect, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatMinutes, type UserProfile } from '@/lib/opal-types'
import { useOpalStore } from '@/lib/opal-store'
import { GLASS } from '@/lib/opal-ui'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { PinPad } from './pin-pad'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import {
  Flame,
  Timer,
  Hourglass,
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

  return (
    <div className="space-y-5 px-5 pb-6 pt-1">
      <AchievementWatcher
        achievements={achievements}
        seen={seenAchievements}
        onSeen={markAchievementsSeen}
      />

      {/* identifikatsiya kartasi */}
      <section
        className="relative overflow-hidden rounded-3xl border border-white/12 bg-gradient-to-br from-[#141b3a] via-[#151233] to-[#0d0a1e] p-5 text-white shadow-[0_14px_44px_rgba(0,0,0,0.5)]"
        aria-label="Profil ma'lumotlari"
      >
        <div
          className="pointer-events-none absolute -left-8 -top-12 h-36 w-36 rounded-full bg-[#8fd9ff]/15 blur-2xl"
          aria-hidden="true"
        />
        <div className="relative flex items-center gap-4">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-3xl bg-gradient-to-br from-[#5b7bff] via-[#8b7bff] to-[#c86bff] text-2xl font-black shadow-[0_0_22px_rgba(139,123,255,0.5)] ring-2 ring-white/25">
            {profile.name[0]}
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <p className="truncate text-[19px] font-extrabold">{profile.name}</p>
              {isPlus && (
                <span className="flex items-center gap-0.5 rounded-full bg-amber-400/20 px-2 py-0.5 text-[9px] font-bold text-amber-300 ring-1 ring-amber-400/40">
                  <Crown size={10} /> PLUS
                </span>
              )}
            </div>
            <p className="text-[12.5px] text-white/50">{profile.handle}</p>
            <div className="mt-1.5 flex items-center gap-1.5 text-[11.5px] font-semibold text-orange-300">
              <Flame size={13} /> {profile.streakDays} kunlik streak · {profile.totalSessions} sessiya
            </div>
          </div>
        </div>

        <div className="relative mt-4 grid grid-cols-3 gap-2 text-center">
          <div className="rounded-2xl bg-white/[0.07] p-2.5 ring-1 ring-white/10">
            <p className="flex items-center justify-center gap-1 text-[15px] font-extrabold">
              <Hourglass size={13} className="text-emerald-300" />
              {formatMinutes(profile.totalSavedMinutes)}
            </p>
            <p className="text-[10px] text-white/50">Jami tejaldi</p>
          </div>
          <div className="rounded-2xl bg-white/[0.07] p-2.5 ring-1 ring-white/10">
            <p className="flex items-center justify-center gap-1 text-[15px] font-extrabold">
              <Timer size={13} className="text-violet-300" />
              {profile.totalSessions}
            </p>
            <p className="text-[10px] text-white/50">Sessiyalar</p>
          </div>
          <div className="rounded-2xl bg-white/[0.07] p-2.5 ring-1 ring-white/10">
            <p className="flex items-center justify-center gap-1 text-[15px] font-extrabold">
              <Gauge size={13} className="text-sky-300" />
              {Math.round(profile.totalSavedMinutes / 60)}
            </p>
            <p className="text-[10px] text-white/50">Soat</p>
          </div>
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
          <span className="relative flex items-center gap-3 rounded-[calc(1.5rem-1.5px)] bg-[#0c0f1c] px-4 py-4">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-300 to-orange-400 text-[#0c0f1c]">
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
          <Sparkles size={16} className="text-[#b18cff]" /> Yutuqlar
        </h3>
        <div className="grid grid-cols-3 gap-3">
          {achievements.map((a) => (
            <div
              key={a.label}
              className={cn(
                'flex flex-col items-center gap-1 rounded-2xl p-3 text-center ring-1',
                a.unlocked
                  ? 'bg-gradient-to-b from-[#7dd3fc]/12 to-[#b18cff]/12 ring-[#8fd9ff]/25'
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
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-500/12 text-violet-300 ring-1 ring-violet-400/20">
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
                      ? 'bg-[#7dd3fc]/15 text-[#bfe9ff] ring-[#7dd3fc]/45'
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
        className="max-w-[320px] rounded-3xl border border-white/12 bg-[#0c0f1c] p-6 shadow-2xl"
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
