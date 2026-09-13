'use client'

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AnimatePresence, motion } from 'framer-motion'
import { useOpalStore } from '@/lib/opal-store'
import { createAndStartSession } from '@/lib/opal-session-actions'
import { formatClock, formatMinutes, type FocusSession, type StatsResponse, type SessionType } from '@/lib/opal-types'
import { syncLiveMinutes } from '@/lib/opal-live-client'
import { AMBIENT_MODES, getAmbientMode, setAmbientMode, useAmbientMode } from '@/lib/ambient-audio'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { Lock, Unlock, ShieldCheck, Ban, Flame, Minus, Plus, Play } from 'lucide-react'

const QUOTES = [
  { text: 'Diqqat — yangi super qudratdir.', author: 'Cal Newport' },
  { text: 'Sizni to‘xtatgan narsa — sizning istagingiz emas, odatiatingiz.', author: 'Opal' },
  { text: 'Kichik qadamlar — katta o‘zgarishlar boshi.', author: 'Lao Tzu' },
  { text: 'Har bir bloklangan ilova — ozod qilingan ong.', author: 'Opal' },
  { text: 'Endi qilingan ish — kech qilingan ishdan yaxshiroq.', author: 'Franklin' },
]

const CONFETTI_COLORS = ['#86efac', '#5eead4', '#9fe8c8', '#ffd48a', '#c9fbdc']

/** Jonli ekvayzer chiziqlari — ambient tovush faol bo'lganda. */
function EqBars() {
  return (
    <span className="flex items-end gap-[2px]" aria-hidden="true">
      {[0, 1, 2].map((i) => (
        <motion.span
          key={i}
          className="w-[2.5px] rounded-full bg-[#e6d9ff]"
          style={{ height: 4 }}
          animate={{ height: [3, 9, 5, 11, 4] }}
          transition={{
            duration: 0.9 + i * 0.23,
            repeat: Infinity,
            ease: 'easeInOut',
            delay: i * 0.12,
          }}
        />
      ))}
    </span>
  )
}

const PRESETS: { type: SessionType; label: string; emoji: string; duration: number }[] = [
  { type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', duration: 45 },
  { type: 'WORK', label: 'Ish rejimi', emoji: '💼', duration: 90 },
  { type: 'STUDY', label: 'O‘qish', emoji: '📚', duration: 60 },
  { type: 'SLEEP', label: 'Uyqu', emoji: '🌙', duration: 480 },
  { type: 'CUSTOM', label: 'Maxsus', emoji: '⚡', duration: 30 },
]

const HOLD_MS_STRICT = 2500
const HOLD_MS_NORMAL = 1200

export function TimerTab() {
  const qc = useQueryClient()
  const activeSession = useOpalStore((s) => s.activeSession)
  const endSession = useOpalStore((s) => s.endSession)
  const timerDraft = useOpalStore((s) => s.timerDraft)
  const clearTimerDraft = useOpalStore((s) => s.clearTimerDraft)

  // ── idle holat ─────────────────────────────────────
  const [duration, setDuration] = useState(45)
  const [type, setType] = useState<SessionType>('DEEP_FOCUS')
  const [label, setLabel] = useState('Deep Focus')
  const [emoji, setEmoji] = useState('🧠')
  const [blockOn, setBlockOn] = useState(true)
  const [strict, setStrict] = useState(false)
  const [starting, setStarting] = useState(false)

  useEffect(() => {
    if (timerDraft) {
      setDuration(timerDraft.durationMinutes)
      setType(timerDraft.type)
      setLabel(timerDraft.label)
      setEmoji(timerDraft.emoji)
      setStrict(timerDraft.strict)
      clearTimerDraft()
    }
  }, [timerDraft, clearTimerDraft])

  const step = (dir: 1 | -1) => {
    setDuration((d) => Math.min(Math.max(d + dir * 15, 15), 180))
  }

  const applyPreset = (p: (typeof PRESETS)[number]) => {
    setType(p.type)
    setLabel(p.label)
    setEmoji(p.emoji)
    setDuration(Math.min(p.duration, 180))
  }

  const start = async () => {
    if (starting) return
    setStarting(true)
    try {
      await createAndStartSession({
        type,
        label,
        emoji,
        durationMinutes: duration,
        strict,
        includeBlocked: blockOn,
      })
      toast.success(`${emoji} ${label} boshlandi`, {
        description: blockOn
          ? 'Chalg‘ituvchi ilovalar bloklandi'
          : 'Ilovalar bloklanmaydi',
      })
    } catch {
      toast.error('Sessiyani boshlash bajarilmadi')
    } finally {
      setStarting(false)
    }
  }

  // ── running holat ──────────────────────────────────
  const [now, setNow] = useState(Date.now())
  const [finishing, setFinishing] = useState(false)
  const [celebrate, setCelebrate] = useState(false)
  const [quote] = useState(() => QUOTES[Math.floor(Math.random() * QUOTES.length)])
  const ambient = useAmbientMode()

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
      const session: FocusSession | null = res.ok ? await res.json() : null
      return { graceful: res.status === 404, session }
    },
    onSuccess: ({ graceful, session }) => {
      if (!graceful && session && activeSession) {
        const cached = qc.getQueryData<StatsResponse>(['stats'])
        if (cached) syncLiveMinutes(cached.weekSavedMinutes + session.savedMinutes)
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
      toast.success('🎉 Ajoyib! Sessiya to‘liq yakunlandi', {
        description: 'Streak va statistika yangilandi',
      })
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
        setCelebrate(false)
        toast.success(early ? 'Sessiya yakunlandi' : '🎉 Sessiya to‘liq yakunlandi', {
          description: early ? 'Keyingi safar oxirigacha davom eting!' : 'Streak va statistika yangilandi',
        })
      } catch {
        endSession()
        toast.error('Sessiya yakunlandi (saqlashda xatolik)')
      } finally {
        setFinishing(false)
      }
    },
    [finishing, progress, completeMutation, endSession]
  )

  // hold-to-stop
  const [holdProgress, setHoldProgress] = useState(0)
  const holdTimer = useRef<ReturnType<typeof setInterval> | null>(null)
  const holdDone = useRef(false)
  const holdNeeded = activeSession?.strict ? HOLD_MS_STRICT : HOLD_MS_NORMAL

  const startHold = useCallback(() => {
    if (holdDone.current || finishing) return
    holdTimer.current = setInterval(() => {
      setHoldProgress((p) => {
        const next = p + 60
        if (next >= holdNeeded) {
          holdDone.current = true
          if (holdTimer.current) clearInterval(holdTimer.current)
          holdTimer.current = null
          void finish(true)
        }
        return Math.min(next, holdNeeded)
      })
    }, 60)
  }, [finishing, finish, holdNeeded])

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

  const displaySeconds = activeSession ? remaining : duration * 60
  const clock = useMemo(() => formatClock(displaySeconds), [displaySeconds])
  const running = !!activeSession

  // sessiya tugaganda ambient tovushni avtomatik o'chirish (running/finished keyin)
  useEffect(() => {
    if ((!running || finished) && getAmbientMode() !== 'off') {
      setAmbientMode('off')
    }
  }, [running, finished])

  return (
    <div className="relative flex min-h-full flex-col">
      {/* immersiv sahna foni */}
      <div className="pointer-events-none absolute inset-0" aria-hidden="true">
        <img
          src="/opal/timer-scene.jpg"
          alt=""
          className="h-full w-full object-cover opacity-90"
          draggable={false}
        />
        <div className="absolute inset-0 bg-gradient-to-b from-[#05060f]/70 via-[#05060f]/35 to-[#05060f]/85" />
        <div className="absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-[#05060f]/80 to-transparent" />
        <div className="absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-[#05060f] to-transparent" />
      </div>

      <div className="relative z-10 flex min-h-full flex-1 flex-col px-5 pb-4 pt-2">
        {/* yuqori qator: sessiya badge / sarlavha */}
        <div className="flex min-h-[36px] items-center justify-between">
          {running ? (
            <div className="flex items-center gap-2 rounded-full bg-black/40 px-3.5 py-1.5 ring-1 ring-white/15 backdrop-blur-md">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
              </span>
              <span className="text-[12px] font-semibold text-white">
                {activeSession.emoji} {activeSession.label}
              </span>
              {activeSession.strict && (
                <span className="flex items-center gap-1 rounded-full bg-white/15 px-2 py-0.5 text-[9.5px] font-bold text-white">
                  <Lock size={9} /> QATTIQ
                </span>
              )}
            </div>
          ) : (
            <p className="text-[15px] font-bold text-white/90">Timer</p>
          )}
          {running && (
            <span className="flex items-center gap-1 rounded-full bg-orange-400/15 px-2.5 py-1 text-[11px] font-bold text-orange-300 ring-1 ring-orange-300/30">
              <Flame size={11} /> fokusda
            </span>
          )}
        </div>

        {/* ── LCD soat ─────────────────────────────────── */}
        <div className="flex flex-1 flex-col items-center justify-center">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
            className="relative w-full max-w-[330px]"
          >
            {/* metall-glass korpus */}
            <div
              className="rounded-[28px] border border-white/25 bg-gradient-to-b from-[#c8d8e8]/30 via-[#8fa5c0]/25 to-[#3d5170]/30 p-[7px] shadow-[0_24px_60px_rgba(0,0,0,0.55),inset_0_1px_0_0_rgba(255,255,255,0.35)] backdrop-blur-xl"
            >
              <div className="relative overflow-hidden rounded-[22px] border border-white/10 bg-[#0a0f16]/90 px-4 py-7">
                {/* skan chiziqlari */}
                <div
                  className="pointer-events-none absolute inset-0 opacity-[0.14]"
                  style={{
                    backgroundImage:
                      'repeating-linear-gradient(0deg, transparent 0 2px, rgba(134,239,172,0.30) 2px 3px)',
                  }}
                  aria-hidden="true"
                />
                <div
                  className={cn(
                    'text-center font-mono text-[64px] font-bold leading-none tracking-[0.06em] tabular-nums',
                    finished ? 'text-emerald-300' : 'text-[#1e2a38]'
                  )}
                  style={
                    finished
                      ? { textShadow: '0 0 30px rgba(110,231,183,0.7)' }
                      : {
                          color: '#cfeeff',
                          textShadow:
                            '0 0 18px rgba(134,239,172,0.6), 0 0 44px rgba(134,239,172,0.28)',
                        }
                  }
                >
                  {finished ? '00:00' : clock}
                </div>
                {/* pastki progress */}
                <div className="mx-auto mt-5 h-1 w-4/5 overflow-hidden rounded-full bg-white/10">
                  <div
                    className={cn(
                      'h-full rounded-full transition-all duration-1000 ease-linear',
                      finished
                        ? 'w-full bg-emerald-400'
                        : 'bg-gradient-to-r from-[#86efac] to-[#5eead4]'
                    )}
                    style={{
                      width: `${(running ? progress : 0) * 100}%`,
                      boxShadow: '0 0 10px rgba(134,239,172,0.55)',
                    }}
                  />
                </div>
              </div>
            </div>

            {/* konfet */}
            <AnimatePresence>
              {celebrate && (
                <div className="pointer-events-none absolute inset-0 z-20 overflow-visible" aria-hidden="true">
                  {Array.from({ length: 34 }).map((_, i) => (
                    <motion.span
                      key={i}
                      initial={{ y: -20, opacity: 1, x: 0, rotate: 0 }}
                      animate={{ y: 420, opacity: [1, 1, 0], x: (i % 2 ? 1 : -1) * (14 + (i % 5) * 12), rotate: (i * 47) % 360 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 2.4 + (i % 5) * 0.4, delay: (i % 9) * 0.14, ease: 'easeIn' }}
                      className={cn('absolute left-1/2 top-0', i % 4 === 3 ? 'h-2 w-2 rounded-full' : 'h-2.5 w-1.5 rounded-[1px]')}
                      style={{ backgroundColor: CONFETTI_COLORS[i % CONFETTI_COLORS.length] }}
                    />
                  ))}
                </div>
              )}
            </AnimatePresence>
          </motion.div>

          {/* iqtibos — faqat running */}
          {running && !finished && !celebrate && (
            <figure className="mt-5 max-w-[270px] text-center">
              <blockquote className="text-[12.5px] font-medium leading-relaxed text-white/80 drop-shadow">
                “{quote.text}”
              </blockquote>
              <figcaption className="mt-1 text-[10.5px] font-semibold text-white/45">
                — {quote.author}
              </figcaption>
            </figure>
          )}

          {/* ── boshqaruv ────────────────────────────────── */}
          {!running ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15, duration: 0.5 }}
              className="mt-6 w-full max-w-[330px]"
            >
              {/* preset chiplar — justify-start: overflow'da chap chiplar kesilib qolmasligi uchun */}
              <div className="no-scrollbar mb-4 flex justify-start gap-2 overflow-x-auto px-0.5 pb-1">
                {PRESETS.map((p) => {
                  const active = p.label === label
                  return (
                    <button
                      key={p.label}
                      onClick={() => applyPreset(p)}
                      className={cn(
                        'flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-[11.5px] font-bold ring-1 backdrop-blur-md transition-all active:scale-95',
                        active
                          ? 'bg-white/20 text-white ring-white/40 shadow-[0_0_14px_rgba(134,239,172,0.30)]'
                          : 'bg-black/30 text-white/60 ring-white/12 hover:text-white/85'
                      )}
                    >
                      <span>{p.emoji}</span> {p.label}
                    </button>
                  )
                })}
              </div>

              {/* stepper */}
              <div className="flex items-center justify-center gap-3">
                <button
                  onClick={() => step(-1)}
                  disabled={duration <= 15}
                  aria-label="15 daqiqa kamaytirish"
                  className="flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-black/35 text-white/80 backdrop-blur-md transition-all hover:bg-black/50 active:scale-90 disabled:opacity-30"
                >
                  <Minus size={17} />
                </button>
                <div className="min-w-[96px] rounded-full border border-white/15 bg-black/40 px-5 py-2.5 text-center text-[19px] font-extrabold text-white backdrop-blur-md">
                  {formatMinutes(duration)}
                </div>
                <button
                  onClick={() => step(1)}
                  disabled={duration >= 180}
                  aria-label="15 daqiqa oshirish"
                  className="flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-black/35 text-white/80 backdrop-blur-md transition-all hover:bg-black/50 active:scale-90 disabled:opacity-30"
                >
                  <Plus size={17} />
                </button>
              </div>

              {/* Start Timer */}
              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={start}
                disabled={starting}
                className="mt-4 flex w-full items-center justify-center gap-2 rounded-full border border-white/20 bg-gradient-to-b from-white/25 to-white/8 py-4 text-[16px] font-bold text-white shadow-[0_14px_40px_rgba(0,0,0,0.4),inset_0_1px_0_0_rgba(255,255,255,0.35)] backdrop-blur-xl active:scale-[0.98] disabled:opacity-60"
              >
                <Play size={16} className="fill-white" />
                {starting ? 'Boshlanmoqda…' : 'Start Timer'}
              </motion.button>

              {/* Block Apps toggle */}
              <div className="mt-3.5 flex items-center justify-center gap-2.5">
                <button
                  onClick={() => setBlockOn((v) => !v)}
                  aria-pressed={blockOn}
                  className={cn(
                    'flex items-center gap-1.5 rounded-full px-4 py-2 text-[12.5px] font-bold ring-1 backdrop-blur-md transition-all active:scale-95',
                    blockOn
                      ? 'bg-[#86efac]/15 text-[#c9fbdc] ring-[#86efac]/40 shadow-[0_0_14px_rgba(134,239,172,0.22)]'
                      : 'bg-black/35 text-white/55 ring-white/15'
                  )}
                >
                  {blockOn ? <Lock size={12} /> : <Unlock size={12} />}
                  Block Apps {blockOn ? 'On' : 'Off'}
                </button>
                <button
                  onClick={() => setStrict((v) => !v)}
                  aria-pressed={strict}
                  aria-label="Qattiq rejim"
                  className={cn(
                    'flex items-center gap-1.5 rounded-full px-4 py-2 text-[12.5px] font-bold ring-1 backdrop-blur-md transition-all active:scale-95',
                    strict
                      ? 'bg-rose-400/15 text-rose-200 ring-rose-300/40'
                      : 'bg-black/35 text-white/55 ring-white/15'
                  )}
                >
                  <Ban size={12} />
                  Strict {strict ? 'On' : 'Off'}
                </button>
              </div>
            </motion.div>
          ) : (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="mt-6 w-full max-w-[330px]"
            >
              {/* bloklangan chiplar */}
              <div className="no-scrollbar mb-3 flex justify-start gap-2 overflow-x-auto px-0.5 pb-1">
                {(activeSession.blockedApps.length
                  ? activeSession.blockedApps
                  : ['TikTok', 'Instagram', 'X (Twitter)']
                ).map((name) => (
                  <span
                    key={name}
                    className="flex shrink-0 items-center gap-1.5 rounded-full bg-black/40 px-3 py-1.5 text-[11px] font-semibold text-white/80 ring-1 ring-white/12 backdrop-blur-md"
                  >
                    <ShieldCheck size={11} className="text-emerald-400" /> {name}
                  </span>
                ))}
              </div>

              {/* ambient fokus tovushlari (Web Audio sintez) */}
              <div className="no-scrollbar mb-3 flex justify-start gap-2 overflow-x-auto px-0.5 pb-0.5">
                {AMBIENT_MODES.map(({ mode, label, emoji }) => {
                  const active = ambient === mode
                  return (
                    <button
                      key={mode}
                      onClick={() => setAmbientMode(mode)}
                      aria-pressed={active}
                      aria-label={`Ambient tovush: ${label}`}
                      className={cn(
                        'flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-[11px] font-bold ring-1 backdrop-blur-md transition-all active:scale-95',
                        active
                          ? 'bg-[#b18cff]/20 text-[#e6d9ff] ring-[#b18cff]/45 shadow-[0_0_14px_rgba(177,140,255,0.3)]'
                          : 'bg-black/35 text-white/55 ring-white/12 hover:text-white/85'
                      )}
                    >
                      <span>{emoji}</span> {label}
                      {active && mode !== 'off' && <EqBars />}
                    </button>
                  )
                })}
              </div>

              {/* hold-to-stop */}
              {finished ? (
                <button
                  onClick={() => endSession()}
                  className="w-full rounded-full bg-gradient-to-b from-emerald-400/30 to-emerald-500/20 py-4 text-[15px] font-bold text-emerald-100 ring-1 ring-emerald-300/40 backdrop-blur-xl active:scale-[0.98]"
                >
                  ✨ Davom etish
                </button>
              ) : (
                <button
                  onMouseDown={startHold}
                  onMouseUp={stopHold}
                  onMouseLeave={stopHold}
                  onTouchStart={startHold}
                  onTouchEnd={stopHold}
                  onTouchCancel={stopHold}
                  disabled={finishing}
                  aria-label="Bosib turib tugatish"
                  className="relative w-full select-none overflow-hidden rounded-full border border-white/12 bg-black/45 py-4 text-[14px] font-bold text-white/85 backdrop-blur-xl active:scale-[0.98]"
                >
                  <span
                    className="absolute inset-y-0 left-0 bg-gradient-to-r from-rose-500/80 to-rose-400/70 transition-none"
                    style={{ width: `${(holdProgress / holdNeeded) * 100}%` }}
                    aria-hidden="true"
                  />
                  <span className="relative">
                    {finishing
                      ? 'Yakunlanmoqda…'
                      : holdProgress > 0
                        ? 'Davom eting…'
                        : activeSession.strict
                          ? '⏳ Tugatish uchun 2.5s bosib turing'
                          : '⏳ Tugatish uchun bosib turing'}
                  </span>
                </button>
              )}
              <p className="mt-2.5 text-center text-[10.5px] font-medium text-white/40">
                {activeSession.strict
                  ? 'Qattiq rejim: chiqish uchun tugmani bosib turish kerak'
                  : 'Erta chiqish streakga ta’sir qiladi'}
              </p>
            </motion.div>
          )}
        </div>
      </div>
    </div>
  )
}
