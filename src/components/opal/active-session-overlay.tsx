'use client'

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useOpalStore } from '@/lib/opal-store'
import { formatClock, type FocusSession, type StatsResponse } from '@/lib/opal-types'
import { syncLiveMinutes } from '@/lib/opal-live-client'
import { toast } from 'sonner'
import { X, Flame, ShieldCheck, Ban, Lock } from 'lucide-react'
import { cn } from '@/lib/utils'

const QUOTES = [
  { text: 'Diqqat — yangi super qudratdir.', author: 'Cal Newport' },
  { text: 'Sizni to‘xtatgan narsa — sizning istagingiz emas, odatiatingiz.', author: 'Opal' },
  { text: 'Kichik qadamlar — katta o‘zgarishlar boshi.', author: 'Lao Tzu' },
  { text: 'Har bir bloklangan ilova — ozod qilingan ong.', author: 'Opal' },
  { text: 'Endi qilingan ish — kech qilingan ishdan yaxshiroq.', author: 'Franklin' },
]

const CONFETTI_COLORS = ['#3d5afe', '#7b61ff', '#e861ff', '#ff9f5a', '#10b981', '#f43f5e']
const HOLD_MS = 2500

export function ActiveSessionOverlay() {
  const qc = useQueryClient()
  const activeSession = useOpalStore((s) => s.activeSession)
  const endSession = useOpalStore((s) => s.endSession)

  const [now, setNow] = useState(Date.now())
  const [confirmEnd, setConfirmEnd] = useState(false)
  const [finishing, setFinishing] = useState(false)
  const [celebrate, setCelebrate] = useState(false)
  const [quote] = useState(() => QUOTES[Math.floor(Math.random() * QUOTES.length)])

  // hold-to-quit (strict mode)
  const [holdProgress, setHoldProgress] = useState(0)
  const holdTimer = useRef<ReturnType<typeof setInterval> | null>(null)
  const holdDone = useRef(false)

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
      if (!res.ok && res.status !== 404) {
        throw new Error('Sessiyani yakunlash bajarilmadi')
      }
      // 404 — sessiya DB'da yo'q (masalan, qayta seed qilingan): lokal ravishda yakunlaymiz
      const session: FocusSession | null = res.ok ? await res.json() : null
      return { graceful: res.status === 404, session }
    },
    onSuccess: ({ graceful, session }) => {
      // jonli do'stlar reytingiga yangi natijani darhol yuboramiz
      if (!graceful && session && activeSession) {
        const cached = qc.getQueryData<StatsResponse>(['stats'])
        if (cached) {
          syncLiveMinutes(cached.weekSavedMinutes + session.savedMinutes)
        }
      }
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

  const finish = useCallback(
    async (early: boolean) => {
      if (finishing) return
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
      } catch {
        // hatto server xatosida ham lokal sessiyani yopamiz — foydalanuvchi qolib ketmasin
        endSession()
        toast.error('Sessiya yakunlandi (saqlashda xatolik)')
      } finally {
        setFinishing(false)
        setConfirmEnd(false)
      }
    },
    [finishing, progress, completeMutation, endSession]
  )

  // ── hold-to-quit logic ────────────────────────────────
  const startHold = useCallback(() => {
    if (holdDone.current || finishing) return
    holdTimer.current = setInterval(() => {
      setHoldProgress((p) => {
        const next = p + 60
        if (next >= HOLD_MS) {
          holdDone.current = true
          if (holdTimer.current) clearInterval(holdTimer.current)
          holdTimer.current = null
          void finish(true)
        }
        return Math.min(next, HOLD_MS)
      })
    }, 60)
  }, [finishing, finish])

  const stopHold = useCallback(() => {
    if (holdTimer.current) {
      clearInterval(holdTimer.current)
      holdTimer.current = null
    }
    if (!holdDone.current) setHoldProgress(0)
  }, [])

  useEffect(() => {
    return () => {
      if (holdTimer.current) clearInterval(holdTimer.current)
    }
  }, [])

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
        <div className="pointer-events-none absolute inset-0 z-10 overflow-hidden" aria-hidden="true">
          {/* falling confetti pieces */}
          {Array.from({ length: 42 }).map((_, i) => {
            const isRound = i % 4 === 3
            return (
              <span
                key={i}
                className={cn('absolute top-[-16px] animate-confetti-fall', isRound ? 'h-2 w-2 rounded-full' : 'h-2.5 w-1.5 rounded-[1px]')}
                style={{
                  left: `${(i * 23.7) % 100}%`,
                  backgroundColor: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
                  animationDelay: `${(i % 12) * 0.22}s`,
                  animationDuration: `${2.6 + (i % 5) * 0.5}s`,
                  transform: `rotate(${(i * 47) % 360}deg)`,
                }}
              />
            )
          })}
          {/* celebratory emoji sparkles */}
          {Array.from({ length: 10 }).map((_, i) => (
            <span
              key={`e${i}`}
              className={cn(
                'absolute text-xl',
                i % 3 === 0 ? 'animate-float' : i % 3 === 1 ? 'animate-bounce' : 'animate-pulse'
              )}
              style={{
                left: `${(i * 41) % 88}%`,
                top: `${(i * 57) % 72}%`,
                animationDelay: `${(i % 6) * 0.25}s`,
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
          {activeSession.strict && (
            <span className="flex items-center gap-1 rounded-full bg-white/15 px-2 py-0.5 text-[10px] font-bold">
              <Lock size={9} /> QATTIQ
            </span>
          )}
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
          {/* breathing halo */}
          <div
            className={cn(
              'absolute inset-4 rounded-full bg-gradient-to-br from-[#5b7bff]/25 to-[#f472d0]/25 blur-xl',
              finished ? 'opacity-100' : 'animate-float'
            )}
            aria-hidden="true"
          />
          <svg viewBox="0 0 260 260" className="relative h-full w-full -rotate-90">
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
            <span className="animate-float text-[44px]">{activeSession.emoji}</span>
            <span className="mt-1 font-mono text-[40px] font-bold tabular-nums tracking-tight">
              {finished ? '00:00' : clock}
            </span>
            <span className="text-[12.5px] font-medium text-white/60">
              {finished ? 'BajARINGIZ!' : `${activeSession.durationMinutes} daqiqalik sessiya`}
            </span>
          </div>
        </div>

        {/* quote */}
        {!finished && (
          <figure className="mt-6 max-w-[260px] text-center">
            <blockquote className="text-[13px] font-medium leading-relaxed text-white/75">
              “{quote.text}”
            </blockquote>
            <figcaption className="mt-1 text-[11px] font-semibold text-white/40">
              — {quote.author}
            </figcaption>
          </figure>
        )}

        {/* blocked chips */}
        <div className="mt-6 w-full">
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
          <div className="w-full max-w-[300px] animate-pop rounded-3xl bg-white p-5 text-center shadow-2xl dark:bg-[#1c1f4e]">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-2xl dark:bg-rose-500/15">
              🫥
            </div>
            <h3 className="mt-3 text-[17px] font-extrabold text-slate-900 dark:text-slate-50">Sessiyani tugatasizmi?</h3>
            <p className="mt-1 text-[12.5px] leading-relaxed text-slate-400">
              {Math.round(progress * 100)}% bajarildi. Erta chiqish streakingizga ta&apos;sir qilishi mumkin.
            </p>
            <div className="mt-4 space-y-2">
              <button
                onClick={() => setConfirmEnd(false)}
                disabled={finishing}
                className="w-full rounded-2xl bg-gradient-to-r from-[#3d5afe] to-[#7b61ff] py-3 text-[14px] font-bold text-white shadow-md shadow-indigo-500/30 active:scale-[0.98]"
              >
                Davom etish (tugatmaslik)
              </button>

              {activeSession.strict ? (
                <button
                  onMouseDown={startHold}
                  onMouseUp={stopHold}
                  onMouseLeave={stopHold}
                  onTouchStart={startHold}
                  onTouchEnd={stopHold}
                  onTouchCancel={stopHold}
                  disabled={finishing}
                  aria-label="Bosib turib tugatish"
                  className="relative w-full select-none overflow-hidden rounded-2xl bg-slate-900 py-3 text-[14px] font-bold text-white active:scale-[0.98]"
                >
                  <span
                    className="absolute inset-y-0 left-0 bg-gradient-to-r from-rose-500 to-rose-400 transition-none"
                    style={{ width: `${(holdProgress / HOLD_MS) * 100}%` }}
                    aria-hidden="true"
                  />
                  <span className="relative">
                    {finishing
                      ? 'Yakunlanmoqda…'
                      : holdProgress > 0
                        ? 'Davom eting…'
                        : '⏳ 2.5s bosib turing'}
                  </span>
                </button>
              ) : (
                <button
                  onClick={() => finish(true)}
                  disabled={finishing}
                  className="w-full rounded-2xl bg-slate-100 py-3 text-[14px] font-bold text-slate-600 active:scale-[0.98] dark:bg-white/10 dark:text-slate-200"
                >
                  {finishing ? 'Yakunlanmoqda…' : 'Baribir tugatish'}
                </button>
              )}
            </div>
            {activeSession.strict && (
              <p className="mt-2 text-[10.5px] text-slate-400">
                Qattiq rejim: chiqish uchun tugmani bosib turish kerak
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
