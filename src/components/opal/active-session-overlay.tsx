'use client'

import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useOpalStore } from '@/lib/opal-store'
import { formatClock } from '@/lib/opal-types'
import { toast } from 'sonner'
import { X, Flame, ShieldCheck, Ban } from 'lucide-react'
import { cn } from '@/lib/utils'

export function ActiveSessionOverlay() {
  const qc = useQueryClient()
  const activeSession = useOpalStore((s) => s.activeSession)
  const endSession = useOpalStore((s) => s.endSession)

  const [now, setNow] = useState(Date.now())
  const [confirmEnd, setConfirmEnd] = useState(false)
  const [finishing, setFinishing] = useState(false)
  const [celebrate, setCelebrate] = useState(false)

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])

  const totalSeconds = activeSession ? activeSession.durationMinutes * 60 : 1
  const elapsedSeconds = activeSession ? Math.floor((now - activeSession.startedAt) / 1000) : 0
  const remaining = Math.max(totalSeconds - elapsedSeconds, 0)
  const progress = Math.min(elapsedSeconds / totalSeconds, 1)
  const finished = remaining <= 0

  const completeMutation = useMutation({
    mutationFn: async ({ focusScore, early }: { focusScore: number; early?: boolean }) => {
      const res = await fetch('/api/sessions', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: activeSession!.sessionId,
          completed: true,
          early: !!early,
          focusScore,
        }),
      })
      return res.json()
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sessions'] })
      qc.invalidateQueries({ queryKey: ['stats'] })
      qc.invalidateQueries({ queryKey: ['profile'] })
    },
  })

  useEffect(() => {
    if (finished && activeSession && !celebrate) {
      setCelebrate(true)
      completeMutation.mutate({ focusScore: 90 + Math.floor(Math.random() * 9) })
    }
  }, [finished])

  const finish = async (early: boolean) => {
    setFinishing(true)
    try {
      const score = early
        ? 55 + Math.floor((1 - progress) * 40)
        : 88 + Math.floor(Math.random() * 11)
      await completeMutation.mutateAsync({ focusScore: score, early })
      endSession()
      toast.success(early ? 'Sessiya yakunlandi' : '🎉 Ajoyib! Sessiya to‘liq yakunlandi', {
        description: early
          ? 'Keyingi safar oxirigacha davom eting!'
          : 'Streak va statistika yangilandi',
      })
    } finally {
      setFinishing(false)
      setConfirmEnd(false)
    }
  }

  const R = 110
  const C = 2 * Math.PI * R
  const clock = useMemo(() => formatClock(remaining), [remaining])

  if (!activeSession) return null

  return (
    <div className="absolute inset-0 z-50 flex flex-col bg-gradient-to-b from-[#10123f] via-[#181b58] to-[#2a1e7a] text-white">
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div className="absolute -left-16 top-16 h-56 w-56 animate-blob rounded-full bg-[#3d5afe]/25 blur-3xl" />
        <div className="absolute -right-16 bottom-24 h-56 w-56 animate-blob-delayed rounded-full bg-[#e861ff]/20 blur-3xl" />
      </div>

      {celebrate && (
        <div className="pointer-events-none absolute inset-0 z-10 animate-pop overflow-hidden" aria-hidden="true">
          {Array.from({ length: 24 }).map((_, i) => (
            <span
              key={i}
              className={cn(
                'absolute text-xl',
                i % 3 === 0 ? 'animate-float' : i % 3 === 1 ? 'animate-bounce' : 'animate-pulse'
              )}
              style={{
                left: `${(i * 37) % 92}%`,
                top: `${(i * 53) % 80}%`,
                animationDelay: `${(i % 8) * 0.18}s`,
              }}
            >
              {['🎉', '✨', '🌟', '💜', '🔮'][i % 5]}
            </span>
          ))}
        </div>
      )}

      {/* top bar */}
      <div className="relative z-20 flex items-center justify-between px-5 pt-3">
        <div className="flex items-center gap-2 rounded-full bg-white/10 px-3.5 py-1.5 backdrop-blur-sm">
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
          </span>
          <span className="text-[12px] font-semibold">{activeSession.label}</span>
        </div>
        <button
          onClick={() => setConfirmEnd(true)}
          disabled={finishing}
          aria-label="Sessiyani tugatish"
          className="flex h-9 w-9 items-center justify-center rounded-full bg-white/10 backdrop-blur-sm transition-colors hover:bg-white/20 active:scale-95"
        >
          <X size={18} />
        </button>
      </div>

      {/* timer ring */}
      <div className="relative z-20 flex flex-1 flex-col items-center justify-center px-6">
        <div className="relative h-[264px] w-[264px]">
          <svg viewBox="0 0 260 260" className="h-full w-full -rotate-90">
            <defs>
              <linearGradient id="timerGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#5b7bff" />
                <stop offset="55%" stopColor="#a78bff" />
                <stop offset="100%" stopColor="#f472d0" />
              </linearGradient>
            </defs>
            <circle cx="130" cy="130" r={R} fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="12" />
            <circle
              cx="130"
              cy="130"
              r={R}
              fill="none"
              stroke="url(#timerGrad)"
              strokeWidth="12"
              strokeLinecap="round"
              strokeDasharray={C}
              strokeDashoffset={C * (1 - progress)}
              className="transition-all duration-1000 ease-linear"
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-[44px]">{activeSession.emoji}</span>
            <span className="mt-1 font-mono text-[40px] font-bold tabular-nums tracking-tight">
              {finished ? '00:00' : clock}
            </span>
            <span className="text-[12.5px] font-medium text-white/60">
              {finished ? 'BajARINGIZ!' : `${activeSession.durationMinutes} daqiqalik sessiya`}
            </span>
          </div>
        </div>

        {/* blocked chips */}
        <div className="mt-7 w-full">
          <p className="mb-2 flex items-center gap-1.5 text-[12px] font-semibold text-white/50">
            <Ban size={13} /> Bloklangan ilovalar
          </p>
          <div className="no-scrollbar flex gap-2 overflow-x-auto pb-1">
            {(activeSession.blockedApps.length
              ? activeSession.blockedApps
              : ['TikTok', 'Instagram', 'X (Twitter)']
            ).map((name) => (
              <span
                key={name}
                className="flex shrink-0 items-center gap-1.5 rounded-full bg-white/10 px-3 py-1.5 text-[12px] font-semibold text-white/85 backdrop-blur-sm"
              >
                <ShieldCheck size={12} className="text-emerald-400" /> {name}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* bottom hint */}
      <div className="relative z-20 px-6 pb-6 pt-2 text-center">
        <div className="mx-auto flex max-w-[280px] items-center justify-center gap-2 rounded-2xl bg-white/5 px-4 py-3 text-[12px] text-white/60 ring-1 ring-white/10">
          <Flame size={14} className="text-orange-400" />
          Sessiya davomida ilovalar ochilmaydi
        </div>
      </div>

      {/* confirm end dialog */}
      {confirmEnd && (
        <div className="absolute inset-0 z-30 flex items-center justify-center bg-black/60 p-6 backdrop-blur-sm">
          <div className="w-full max-w-[300px] animate-pop rounded-3xl bg-white p-5 text-center shadow-2xl">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-2xl">
              🫥
            </div>
            <h3 className="mt-3 text-[17px] font-extrabold text-slate-900">Sessiyani tugatasizmi?</h3>
            <p className="mt-1 text-[12.5px] leading-relaxed text-slate-400">
              {Math.round(progress * 100)}% bajarildi. Erta chiqish streakingizga ta&apos;sir qilishi mumkin.
            </p>
            <div className="mt-4 space-y-2">
              <button
                onClick={() => finish(false)}
                disabled={finishing}
                className="w-full rounded-2xl bg-gradient-to-r from-[#3d5afe] to-[#7b61ff] py-3 text-[14px] font-bold text-white shadow-md shadow-indigo-500/30 active:scale-[0.98]"
              >
                Davom etish (tugatmaslik)
              </button>
              <button
                onClick={() => finish(true)}
                disabled={finishing}
                className="w-full rounded-2xl bg-slate-100 py-3 text-[14px] font-bold text-slate-600 active:scale-[0.98]"
              >
                {finishing ? 'Yakunlanmoqda…' : 'Baribir tugatish'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
