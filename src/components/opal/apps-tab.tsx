'use client'

import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { BlockApp } from '@/lib/opal-types'
import { formatMinutes } from '@/lib/opal-types'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { Search, ShieldBan, SearchX, Lock, ChevronRight, ShieldCheck, Clock3, Zap, MousePointerClick } from 'lucide-react'

const CATEGORIES = ['Barchasi', 'Ijtimoiy tarmoq', 'Zerikarli', 'O‘yinlar', 'Messenger', 'Foydali']

/** deterministic 0..1 pseudo-random from a string + index */
function seededRand(seed: string, i: number): number {
  let h = 2166136261
  const s = seed + ':' + i
  for (let c = 0; c < s.length; c++) {
    h ^= s.charCodeAt(c)
    h = Math.imul(h, 16777619)
  }
  return ((h >>> 0) % 1000) / 1000
}

/** 24h usage profile — social apps peak in the evening, useful apps midday */
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

function AppDetailSheet({
  app,
  onClose,
  onToggleBlock,
  onLimit,
}: {
  app: BlockApp
  onClose: () => void
  onToggleBlock: (app: BlockApp) => void
  onLimit: (id: string, minutes: number) => void
}) {
  const timeline = useMemo(() => usageTimeline(app), [app])
  const peakHour = timeline.indexOf(Math.max(...timeline))
  const limitProgress =
    app.dailyLimitMinutes > 0 ? Math.min(app.todayMinutes / app.dailyLimitMinutes, 1) : 0
  const overLimit = app.dailyLimitMinutes > 0 && app.todayMinutes > app.dailyLimitMinutes
  const weekMinutes = app.todayMinutes * (5 + Math.round(seededRand(app.name, 99) * 3))

  return (
    <Dialog open onOpenChange={(v) => !v && onClose()}>
      <DialogContent aria-describedby={undefined} className="max-w-[340px] gap-0 rounded-[2rem] border-0 bg-white p-0 shadow-2xl dark:bg-[#181b42]">
        <DialogTitle className="sr-only">{app.name} tafsilotlari</DialogTitle>

        {/* hero */}
        <div className={cn('relative flex items-center gap-3.5 bg-gradient-to-br p-5', app.gradient)}>
          <div className="pointer-events-none absolute inset-0 bg-black/10" />
          <div className="relative flex h-14 w-14 items-center justify-center rounded-2xl bg-white/25 text-[26px] shadow-inner ring-1 ring-white/40 backdrop-blur">
            {app.emoji}
          </div>
          <div className="relative min-w-0 flex-1">
            <p className="truncate text-[17px] font-extrabold text-white">{app.name}</p>
            <p className="text-[11.5px] font-medium text-white/75">{app.category}</p>
          </div>
          {app.blocked && (
            <span className="relative rounded-full bg-white/25 px-2.5 py-1 text-[9.5px] font-black text-white ring-1 ring-white/40 backdrop-blur">
              BLOK YONIQ
            </span>
          )}
        </div>

        <div className="space-y-4 p-5">
          {/* today usage */}
          <div className="flex items-end justify-between">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">Bugungi foydalanish</p>
              <p className="mt-0.5 text-[24px] font-black leading-none text-slate-900 dark:text-slate-50">
                {formatMinutes(app.todayMinutes)}
                {app.dailyLimitMinutes > 0 && (
                  <span className="ml-1.5 text-[13px] font-bold text-slate-400">/ {formatMinutes(app.dailyLimitMinutes)}</span>
                )}
              </p>
            </div>
            <span
              className={cn(
                'rounded-full px-2.5 py-1 text-[10.5px] font-bold',
                overLimit
                  ? 'bg-rose-50 text-rose-500 ring-1 ring-rose-100 dark:bg-rose-500/10 dark:ring-rose-500/20'
                  : 'bg-emerald-50 text-emerald-600 ring-1 ring-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-500/20'
              )}
            >
              {overLimit ? 'Limit oshdi' : 'Rejimda'}
            </span>
          </div>

          {/* 24h timeline */}
          <div aria-label="24 soatlik foydalanish grafigi">
            <div className="mb-2 flex items-center justify-between">
              <p className="flex items-center gap-1.5 text-[12px] font-bold text-slate-700 dark:text-slate-200">
                <Clock3 size={13} className="text-[#7b61ff]" /> 24 soatlik profayl
              </p>
              <p className="text-[10.5px] font-semibold text-slate-400">
                eng yuqori: {String(peakHour).padStart(2, '0')}:00
              </p>
            </div>
            <div className="flex h-16 items-end gap-[3px]">
              {timeline.map((v, h) => (
                <div
                  key={h}
                  className={cn(
                    'flex-1 rounded-t-sm transition-all',
                    h === peakHour
                      ? 'bg-gradient-to-t from-[#e861ff] to-[#7b61ff]'
                      : overLimit
                        ? 'bg-gradient-to-t from-rose-300 to-rose-400'
                        : 'bg-gradient-to-t from-[#3d5afe]/70 to-[#7b61ff]/70'
                  )}
                  style={{ height: `${Math.max(v * 100, 4)}%` }}
                  title={`${String(h).padStart(2, '0')}:00 — taxminiy ${Math.round(v * app.todayMinutes)} daq`}
                />
              ))}
            </div>
            <div className="mt-1.5 flex justify-between text-[9.5px] font-semibold text-slate-300 dark:text-slate-600">
              <span>00</span><span>06</span><span>12</span><span>18</span><span>24</span>
            </div>
          </div>

          {/* mini stats */}
          <div className="grid grid-cols-3 gap-2">
            <div className="rounded-2xl bg-slate-50 p-2.5 text-center dark:bg-white/5">
              <p className="text-[13.5px] font-extrabold text-slate-800 dark:text-slate-100">{formatMinutes(weekMinutes)}</p>
              <p className="text-[9.5px] font-medium text-slate-400">bu hafta</p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-2.5 text-center dark:bg-white/5">
              <p className="flex items-center justify-center gap-0.5 text-[13.5px] font-extrabold text-slate-800 dark:text-slate-100">
                <MousePointerClick size={12} className="text-slate-400" />
                {8 + Math.round(seededRand(app.name, 7) * 26)}
              </p>
              <p className="text-[9.5px] font-medium text-slate-400">bugun olish</p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-2.5 text-center dark:bg-white/5">
              <p className="flex items-center justify-center gap-0.5 text-[13.5px] font-extrabold text-slate-800 dark:text-slate-100">
                <Zap size={12} className="text-amber-400" />
                {Math.max(Math.round(app.todayMinutes / (2 + seededRand(app.name, 3) * 3)), 1)}
              </p>
              <p className="text-[9.5px] font-medium text-slate-400">eng uzun</p>
            </div>
          </div>

          {/* limit row */}
          <div>
            <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
              <div
                className={cn(
                  'h-full rounded-full transition-all duration-700',
                  overLimit ? 'bg-gradient-to-r from-rose-400 to-rose-500' : 'bg-gradient-to-r from-[#3d5afe] to-[#7b61ff]'
                )}
                style={{ width: `${limitProgress * 100}%` }}
              />
            </div>
            <div className="mt-2.5 flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400">Kunlik limit</span>
              <select
                value={String(app.dailyLimitMinutes)}
                onChange={(e) => onLimit(app.id, Number(e.target.value))}
                aria-label={`${app.name} kunlik limit`}
                className="cursor-pointer rounded-xl bg-slate-100 px-3 py-1.5 text-[12px] font-bold text-slate-600 focus:outline-none focus:ring-2 focus:ring-[#7b61ff]/40 dark:bg-white/10 dark:text-slate-300"
              >
                <option value="0">Limit yo‘q</option>
                {[5, 10, 15, 20, 30, 45, 60].map((m) => (
                  <option key={m} value={m}>{m} daq</option>
                ))}
              </select>
            </div>
          </div>

          {/* block action */}
          <button
            onClick={() => onToggleBlock(app)}
            className={cn(
              'flex w-full items-center justify-center gap-2 rounded-2xl py-3 text-[13px] font-bold transition-all active:scale-[0.98]',
              app.blocked
                ? 'bg-emerald-50 text-emerald-600 ring-1 ring-emerald-100 hover:bg-emerald-100/60 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-500/20'
                : 'bg-gradient-to-r from-rose-500 to-rose-600 text-white shadow-md shadow-rose-500/25'
            )}
          >
            {app.blocked ? (
              <><ShieldCheck size={15} /> Blokdan chiqarish</>
            ) : (
              <><ShieldBan size={15} /> {app.name}’ni bloklash</>
            )}
          </button>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export function AppsTab() {
  const qc = useQueryClient()
  const [category, setCategory] = useState('Barchasi')
  const [search, setSearch] = useState('')
  const [detailApp, setDetailApp] = useState<BlockApp | null>(null)

  const appsQ = useQuery<BlockApp[]>({
    queryKey: ['apps'],
    queryFn: async () => (await fetch('/api/apps')).json(),
  })

  const toggleMutation = useMutation<BlockApp, Error, { id: string; blocked: boolean; name: string }>({
    mutationFn: async ({ id, blocked }: { id: string; blocked: boolean }) => {
      const res = await fetch('/api/apps', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, blocked }),
      })
      return res.json()
    },
    onMutate: async ({ id, blocked }) => {
      await qc.cancelQueries({ queryKey: ['apps'] })
      const prev = qc.getQueryData<BlockApp[]>(['apps'])
      if (prev) qc.setQueryData<BlockApp[]>(['apps'], prev.map((a) => (a.id === id ? { ...a, blocked } : a)))
      return { prev }
    },
    onError: () => {
      void qc.invalidateQueries({ queryKey: ['apps'] })
      toast.error('Blokni o‘zgartirish bajarilmadi')
    },
    onSuccess: (_d, vars) => {
      if (vars.blocked) toast.success(`${vars.name} bloklandi 🛡️`)
      else toast.info(`${vars.name} blokdan chiqarildi`)
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['apps'] })
      qc.invalidateQueries({ queryKey: ['stats'] })
    },
  })

  const limitMutation = useMutation({
    mutationFn: async ({ id, dailyLimitMinutes }: { id: string; dailyLimitMinutes: number }) => {
      const res = await fetch('/api/apps', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, dailyLimitMinutes }),
      })
      return res.json()
    },
    onSuccess: () => {
      toast.success('Kunlik limit saqlandi')
      qc.invalidateQueries({ queryKey: ['apps'] })
    },
  })

  const filtered = useMemo(() => {
    const apps = appsQ.data ?? []
    return apps.filter(
      (a) =>
        (category === 'Barchasi' || a.category === category) &&
        a.name.toLowerCase().includes(search.toLowerCase())
    )
  }, [appsQ.data, category, search])

  const blockedCount = (appsQ.data ?? []).filter((a) => a.blocked).length
  const totalTodayMinutes = (appsQ.data ?? []).reduce((acc, a) => acc + a.todayMinutes, 0)

  const openDetail = (app: BlockApp) => {
    const fresh = appsQ.data?.find((a) => a.id === app.id)
    setDetailApp(fresh ?? app)
  }
  const toggleBlock = (app: BlockApp) =>
    toggleMutation.mutate({ id: app.id, blocked: !app.blocked, name: app.name })

  return (
    <div className="animate-slide-up space-y-4 px-5 pb-6 pt-3">
      <header className="flex items-start justify-between">
        <div>
          <h2 className="text-[22px] font-extrabold tracking-tight text-slate-900 dark:text-slate-50">Ilovalar</h2>
          <p className="text-[13px] text-slate-400">
            {blockedCount} bloklangan · bugun {formatMinutes(totalTodayMinutes)}
          </p>
        </div>
        <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3d5afe]/10 to-[#e861ff]/10 text-xl">
          🛡️
        </div>
      </header>

      {/* search */}
      <div className="relative">
        <Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Ilova izlash…"
          aria-label="Ilova izlash"
          className="w-full rounded-2xl border-0 bg-white py-3 pl-11 pr-4 text-[14px] font-medium text-slate-700 shadow-sm shadow-slate-200/60 ring-1 ring-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#7b61ff]/40 dark:bg-[#181b42] dark:text-slate-200 dark:shadow-black/30 dark:ring-white/10"
        />
      </div>

      {/* categories */}
      <div className="no-scrollbar -mx-5 flex gap-2 overflow-x-auto px-5">
        {CATEGORIES.map((c) => (
          <button
            key={c}
            onClick={() => setCategory(c)}
            className={cn(
              'shrink-0 rounded-full px-3.5 py-2 text-[12.5px] font-semibold transition-all',
              category === c
                ? 'bg-gradient-to-r from-[#3d5afe] to-[#7b61ff] text-white shadow-md shadow-indigo-500/25'
                : 'bg-white text-slate-500 ring-1 ring-slate-200 hover:bg-slate-50 dark:bg-white/5 dark:text-slate-400 dark:ring-white/10 dark:hover:bg-white/10'
            )}
          >
            {c}
          </button>
        ))}
      </div>

      {/* list */}
      {appsQ.isLoading ? (
        <div className="space-y-2.5">
          {[...Array(6)].map((_, i) => (
            <Skeleton key={i} className="h-[76px] w-full rounded-3xl" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-3xl bg-white py-10 text-slate-400 shadow-sm dark:bg-[#181b42] dark:shadow-black/30">
          <SearchX size={28} />
          <p className="text-[13px] font-medium">Hech narsa topilmadi</p>
        </div>
      ) : (
        <ul className="space-y-2.5">
          {filtered.map((app) => {
            const overLimit = app.dailyLimitMinutes > 0 && app.todayMinutes > app.dailyLimitMinutes
            const limitProgress =
              app.dailyLimitMinutes > 0
                ? Math.min(app.todayMinutes / app.dailyLimitMinutes, 1)
                : 0
            return (
              <li
                key={app.id}
                className={cn(
                  'group rounded-3xl bg-white p-4 shadow-sm shadow-slate-200/60 transition-all hover:-translate-y-px hover:shadow-md active:scale-[0.99] dark:bg-[#181b42] dark:shadow-black/30',
                  app.blocked && 'ring-1 ring-rose-100 dark:ring-rose-500/20'
                )}
              >
                <div
                  className="flex cursor-pointer items-center gap-3"
                  onClick={() => openDetail(app)}
                  role="button"
                  aria-label={`${app.name} tafsilotlarini ochish`}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') openDetail(app)
                  }}
                  tabIndex={0}
                >
                  <div className="relative">
                    <div
                      className={cn(
                        'flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br text-[22px] shadow-sm',
                        app.gradient
                      )}
                    >
                      {app.emoji}
                    </div>
                    {app.blocked && (
                      <span className="absolute -bottom-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-rose-500 ring-2 ring-white">
                        <ShieldBan size={11} className="text-white" />
                      </span>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="truncate text-[15px] font-bold text-slate-900 dark:text-slate-50">{app.name}</p>
                      {app.blocked && (
                        <span className="shrink-0 rounded-full bg-rose-50 px-2 py-0.5 text-[9px] font-bold text-rose-500 ring-1 ring-rose-100">
                          BLOK
                        </span>
                      )}
                    </div>
                    <p className="text-[11.5px] text-slate-400">
                      {app.category} · bugun {formatMinutes(app.todayMinutes)}
                    </p>
                  </div>
                  <ChevronRight
                    size={17}
                    className="shrink-0 text-slate-300 transition-transform group-hover:translate-x-0.5"
                  />
                  <Switch
                    checked={app.blocked}
                    onCheckedChange={(v) =>
                      toggleMutation.mutate({ id: app.id, blocked: v, name: app.name })
                    }
                    aria-label={`${app.name} bloklash`}
                    className="data-[state=checked]:bg-rose-500"
                  />
                </div>

                {/* limit row */}
                <div className="mt-3 flex items-center gap-3 border-t border-slate-100 pt-3 dark:border-white/10">
                  <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
                    <div
                      className={cn(
                        'h-full rounded-full transition-all duration-500',
                        overLimit
                          ? 'bg-gradient-to-r from-rose-400 to-rose-500'
                          : 'bg-gradient-to-r from-[#3d5afe] to-[#7b61ff]'
                      )}
                      style={{ width: `${limitProgress * 100}%` }}
                    />
                  </div>
                  <select
                    value={String(app.dailyLimitMinutes)}
                    onChange={(e) =>
                      limitMutation.mutate({ id: app.id, dailyLimitMinutes: Number(e.target.value) })
                    }
                    aria-label={`${app.name} kunlik limit`}
                    className="cursor-pointer rounded-xl bg-slate-100 px-2.5 py-1.5 text-[11.5px] font-bold text-slate-600 focus:outline-none focus:ring-2 focus:ring-[#7b61ff]/40 dark:bg-white/10 dark:text-slate-300"
                  >
                    <option value="0">Limit yo‘q</option>
                    {[5, 10, 15, 20, 30, 45, 60].map((m) => (
                      <option key={m} value={m}>
                        {m} daq
                      </option>
                    ))}
                  </select>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {/* ios-style note */}
      <div className="flex items-start gap-2.5 rounded-2xl bg-violet-50 p-3.5 ring-1 ring-violet-100 dark:bg-violet-500/10 dark:ring-violet-500/20">
        <Lock size={15} className="mt-0.5 shrink-0 text-violet-500 dark:text-violet-300" />
        <p className="text-[11.5px] leading-relaxed text-violet-700/80 dark:text-violet-300/80">
          Bu demo versiyada bloklash web ilova ichida simulyatsiya qilinadi. Haqiqiy iOS ilovada
          Screen Time API orqali tizim darajasida amalga oshiriladi.
        </p>
      </div>

      {/* app detail bottom sheet */}
      {detailApp && appsQ.data && (
        <AppDetailSheet
          app={appsQ.data.find((a) => a.id === detailApp.id) ?? detailApp}
          onClose={() => setDetailApp(null)}
          onToggleBlock={toggleBlock}
          onLimit={(id, minutes) => limitMutation.mutate({ id, dailyLimitMinutes: minutes })}
        />
      )}
    </div>
  )
}
