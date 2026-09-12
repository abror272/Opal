'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatMinutes, type UserProfile } from '@/lib/opal-types'
import { useOpalStore } from '@/lib/opal-store'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { Flame, Timer, Hourglass, Crown, ChevronRight, ShieldCheck, Lock, Bell, Gauge, LogOut, CircleHelp, Sparkles } from 'lucide-react'

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

export function ProfileTab() {
  const qc = useQueryClient()
  const setTab = useOpalStore((s) => s.setTab)

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
      <div className="space-y-5 p-5 pt-3">
        <Skeleton className="h-24 w-full rounded-3xl" />
        <Skeleton className="h-36 w-full rounded-3xl" />
        <Skeleton className="h-64 w-full rounded-3xl" />
      </div>
    )
  }

  const profile = profileQ.data
  if (!profile) return null

  const isPlus = profile.plan === 'PLUS'

  return (
    <div className="animate-slide-up space-y-5 px-5 pb-6 pt-3">
      <header>
        <h2 className="text-[22px] font-extrabold tracking-tight text-slate-900">Profil</h2>
      </header>

      {/* identity card */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#10123f] via-[#1b1e5c] to-[#3d2f86] p-5 text-white shadow-lg shadow-indigo-900/25">
        <div className="pointer-events-none absolute -left-8 -top-12 h-36 w-36 rounded-full bg-[#e861ff]/25 blur-2xl" />
        <div className="relative flex items-center gap-4">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-3xl bg-gradient-to-br from-[#3d5afe] via-[#7b61ff] to-[#e861ff] text-2xl font-black shadow-lg shadow-indigo-500/40 ring-2 ring-white/20">
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
            <p className="text-[12.5px] text-white/55">{profile.handle}</p>
            <div className="mt-1.5 flex items-center gap-1.5 text-[11.5px] font-semibold text-orange-300">
              <Flame size={13} /> {profile.streakDays} kunlik streak · {profile.totalSessions} sessiya
            </div>
          </div>
        </div>

        <div className="relative mt-4 grid grid-cols-3 gap-2 text-center">
          <div className="rounded-2xl bg-white/8 p-2.5 ring-1 ring-white/10">
            <p className="flex items-center justify-center gap-1 text-[15px] font-extrabold">
              <Hourglass size={13} className="text-emerald-400" />
              {formatMinutes(profile.totalSavedMinutes)}
            </p>
            <p className="text-[10px] text-white/55">Jami tejaldi</p>
          </div>
          <div className="rounded-2xl bg-white/8 p-2.5 ring-1 ring-white/10">
            <p className="flex items-center justify-center gap-1 text-[15px] font-extrabold">
              <Timer size={13} className="text-violet-300" />
              {profile.totalSessions}
            </p>
            <p className="text-[10px] text-white/55">Sessiyalar</p>
          </div>
          <div className="rounded-2xl bg-white/8 p-2.5 ring-1 ring-white/10">
            <p className="flex items-center justify-center gap-1 text-[15px] font-extrabold">
              <Gauge size={13} className="text-sky-300" />
              {Math.round(profile.totalSavedMinutes / 60)}
            </p>
            <p className="text-[10px] text-white/55">Soat</p>
          </div>
        </div>
      </section>

      {/* Opal Plus banner */}
      {!isPlus ? (
        <button
          onClick={() => {
            patchMutation.mutate({ plan: 'PLUS' })
            toast.success('Opal Plus faollashtirildi! 👑', { description: 'Barcha premium funksiyalar ochildi (demo)' })
          }}
          className="group relative w-full overflow-hidden rounded-3xl bg-gradient-to-r from-amber-400 via-orange-400 to-rose-400 p-[1.5px] text-left shadow-lg shadow-orange-400/25 transition-transform active:scale-[0.98]"
        >
          <span className="relative flex items-center gap-3 rounded-[calc(1.5rem-1.5px)] bg-white px-4 py-4">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-400 to-orange-500 text-white">
              <Crown size={20} />
            </span>
            <span className="flex-1">
              <span className="block text-[14.5px] font-extrabold text-slate-900">Opal Plus’ga o‘tish</span>
              <span className="block text-[11.5px] text-slate-400">Cheklovsriz bloklar, qattiq rejim, maxsus mavzular</span>
            </span>
            <ChevronRight size={18} className="text-slate-300 transition-transform group-hover:translate-x-0.5" />
          </span>
        </button>
      ) : (
        <div className="flex items-center gap-3 rounded-3xl bg-gradient-to-r from-amber-400/15 to-orange-400/15 p-4 ring-1 ring-amber-400/30">
          <Crown size={22} className="text-amber-500" />
          <div>
            <p className="text-[14px] font-extrabold text-slate-900">Opal Plus faol 👑</p>
            <p className="text-[11.5px] text-slate-500">Hamma premium funksiyalar ochiq</p>
          </div>
        </div>
      )}

      {/* achievements */}
      <section aria-label="Yutuqlar" className="rounded-3xl bg-white p-5 shadow-sm shadow-slate-200/60">
        <h3 className="mb-3.5 flex items-center gap-2 text-[15px] font-bold text-slate-900">
          <Sparkles size={16} className="text-violet-500" /> Yutuqlar
        </h3>
        <div className="grid grid-cols-3 gap-3">
          {achievementsFor(profile).map((a) => (
            <div
              key={a.label}
              className={cn(
                'flex flex-col items-center gap-1 rounded-2xl p-3 text-center ring-1',
                a.unlocked
                  ? 'bg-gradient-to-b from-violet-50 to-fuchsia-50 ring-violet-100'
                  : 'bg-slate-50 opacity-45 ring-slate-100 grayscale'
              )}
            >
              <span className="text-[24px]">{a.emoji}</span>
              <span className="text-[10.5px] font-bold leading-tight text-slate-700">{a.label}</span>
              <span className="text-[9.5px] text-slate-400">{a.desc}</span>
            </div>
          ))}
        </div>
      </section>

      {/* settings */}
      <section aria-label="Sozlamalar" className="overflow-hidden rounded-3xl bg-white shadow-sm shadow-slate-200/60">
        <h3 className="px-5 pt-4 text-[15px] font-bold text-slate-900">Sozlamalar</h3>

        <div className="mt-1 divide-y divide-slate-100">
          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 text-emerald-500">
                <ShieldCheck size={17} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-slate-800">Himoya</p>
                <p className="text-[11px] text-slate-400">Doimiy blokdan chiqmaslik</p>
              </div>
            </div>
            <Switch
              checked={profile.protectionEnabled}
              onCheckedChange={(v) => patchMutation.mutate({ protectionEnabled: v })}
              aria-label="Himoya sozlamasi"
            />
          </div>

          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
                <Lock size={16} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-slate-800">Qattiq rejim</p>
                <p className="text-[11px] text-slate-400">Erta chiqish taqiqlanadi</p>
              </div>
            </div>
            <Switch
              checked={profile.strictMode}
              onCheckedChange={(v) => patchMutation.mutate({ strictMode: v })}
              aria-label="Qattiq rejim sozlamasi"
            />
          </div>

          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-50 text-rose-500">
                <Bell size={16} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-slate-800">Eslatmalar</p>
                <p className="text-[11px] text-slate-400">Limit ogohlantirishlari</p>
              </div>
            </div>
            <Switch
              defaultChecked
              aria-label="Eslatmalar sozlamasi"
            />
          </div>

          {/* daily goal */}
          <button
            onClick={() => setTab('stats')}
            className="flex w-full items-center justify-between px-5 py-3.5 text-left transition-colors hover:bg-slate-50"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-50 text-violet-500">
                <Gauge size={16} />
              </div>
              <div>
                <p className="text-[13.5px] font-bold text-slate-800">Kunlik ekran maqsadi</p>
                <p className="text-[11px] text-slate-400">Hozir: {formatMinutes(profile.goalMinutes)}</p>
              </div>
            </div>
            <ChevronRight size={17} className="text-slate-300" />
          </button>

          <div className="flex items-center justify-between px-5 py-3.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-sky-500">
                <CircleHelp size={16} />
              </div>
              <p className="text-[13.5px] font-bold text-slate-800">Yordam</p>
            </div>
            <ChevronRight size={17} className="text-slate-300" />
          </div>

          <button
            onClick={() => toast.info('Bu demo — chiqish shart emas 😊')}
            className="flex w-full items-center justify-between px-5 py-3.5 text-left transition-colors hover:bg-slate-50"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-50 text-rose-500">
                <LogOut size={16} />
              </div>
              <p className="text-[13.5px] font-bold text-rose-500">Chiqish</p>
            </div>
            <ChevronRight size={17} className="text-slate-300" />
          </button>
        </div>
      </section>

      <p className="pb-1 text-center text-[11px] text-slate-300">
        Opal Clone · v1.0 · Next.js bilan qurilgan
      </p>
    </div>
  )
}
