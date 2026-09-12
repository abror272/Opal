'use client'

import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { BlockApp } from '@/lib/opal-types'
import { formatMinutes } from '@/lib/opal-types'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { Search, ShieldBan, SearchX, Lock } from 'lucide-react'

const CATEGORIES = ['Barchasi', 'Ijtimoiy tarmoq', 'Zerikarli', 'O‘yinlar', 'Messenger', 'Foydali']

export function AppsTab() {
  const qc = useQueryClient()
  const [category, setCategory] = useState('Barchasi')
  const [search, setSearch] = useState('')

  const appsQ = useQuery<BlockApp[]>({
    queryKey: ['apps'],
    queryFn: async () => (await fetch('/api/apps')).json(),
  })

  const toggleMutation = useMutation({
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
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(['apps'], ctx.prev)
      toast.error('Blokni o‘zgartirish bajarilmadi')
    },
    onSuccess: (_d, vars) => {
      if (vars.blocked) toast.success(`${vars.name ?? 'Ilova'} bloklandi 🛡️`)
      else toast.info(`${vars.name ?? 'Ilova}'} blokdan chiqarildi`)
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

  return (
    <div className="animate-slide-up space-y-4 px-5 pb-6 pt-3">
      <header className="flex items-start justify-between">
        <div>
          <h2 className="text-[22px] font-extrabold tracking-tight text-slate-900">Ilovalar</h2>
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
          className="w-full rounded-2xl border-0 bg-white py-3 pl-11 pr-4 text-[14px] font-medium text-slate-700 shadow-sm shadow-slate-200/60 ring-1 ring-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#7b61ff]/40"
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
                : 'bg-white text-slate-500 ring-1 ring-slate-200 hover:bg-slate-50'
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
        <div className="flex flex-col items-center gap-2 rounded-3xl bg-white py-10 text-slate-400 shadow-sm">
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
                  'rounded-3xl bg-white p-4 shadow-sm shadow-slate-200/60 transition-opacity',
                  app.blocked && 'ring-1 ring-rose-100'
                )}
              >
                <div className="flex items-center gap-3">
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
                      <p className="truncate text-[15px] font-bold text-slate-900">{app.name}</p>
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
                  <Switch
                    checked={app.blocked}
                    onCheckedChange={(v) =>
                      toggleMutation.mutate({ id: app.id, blocked: v, name: app.name } as never)
                    }
                    aria-label={`${app.name} bloklash`}
                    className="data-[state=checked]:bg-rose-500"
                  />
                </div>

                {/* limit row */}
                <div className="mt-3 flex items-center gap-3 border-t border-slate-100 pt-3">
                  <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-100">
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
                    className="cursor-pointer rounded-xl bg-slate-100 px-2.5 py-1.5 text-[11.5px] font-bold text-slate-600 focus:outline-none focus:ring-2 focus:ring-[#7b61ff]/40"
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
      <div className="flex items-start gap-2.5 rounded-2xl bg-violet-50 p-3.5 ring-1 ring-violet-100">
        <Lock size={15} className="mt-0.5 shrink-0 text-violet-500" />
        <p className="text-[11.5px] leading-relaxed text-violet-700/80">
          Bu demo versiyada bloklash web ilova ichida simulyatsiya qilinadi. Haqiqiy iOS ilovada
          Screen Time API orqali tizim darajasida amalga oshiriladi.
        </p>
      </div>
    </div>
  )
}
