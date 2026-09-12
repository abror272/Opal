'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { toast } from 'sonner'
import { Trophy, Radio, Wifi, WifiOff, UserPlus } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import { formatMinutes } from '@/lib/opal-types'
import type { LiveActivity } from '@/lib/opal-live-types'
import { addLiveFriend } from '@/lib/opal-live-client'
import { useLiveLeaderboard } from '@/hooks/use-live-leaderboard'

const FRIEND_AVATARS = ['👩‍🚀', '🧑‍🎤', '👨‍🌾', '👩‍🏫', '🧑‍⚕️', '👨‍🍳', '🧑‍🔧', '👩‍⚖️', '🧑‍🎨', '🦊', '🐼', '🦉']

interface Props {
  myName: string
  mySavedMinutes: number
  myStreak: number
  loading: boolean
}

const ACTIVITY_TOAST_KEY = 'opal-live-activity'

export function LiveLeaderboard({ myName, mySavedMinutes, myStreak, loading }: Props) {
  const joinPayload = useMemo(
    () => ({ name: myName, savedMinutes: mySavedMinutes, streak: myStreak }),
    [myName, mySavedMinutes, myStreak]
  )

  const [addOpen, setAddOpen] = useState(false)
  const [friendName, setFriendName] = useState('')
  const [friendAvatar, setFriendAvatar] = useState(FRIEND_AVATARS[0])
  const [adding, setAdding] = useState(false)

  const lastToastTs = useRef(0)

  const handleActivity = (a: LiveActivity) => {
    // throttle toasts: at most 1 per 6s, and not on the very first seconds
    const now = Date.now()
    if (now - lastToastTs.current < 6000) return
    lastToastTs.current = now
    if (typeof window !== 'undefined' && !window.localStorage.getItem(ACTIVITY_TOAST_KEY)) {
      window.localStorage.setItem(ACTIVITY_TOAST_KEY, String(now))
      return // skip the very first burst after page load
    }
    toast(`${a.emoji} ${a.text}`, {
      description: 'Do‘stlar reytingi — jonli',
      duration: 4000,
    })
  }

  const { state, connected } = useLiveLeaderboard(joinPayload, handleActivity)

  // clear the throttle key occasionally so future sessions still get toasts
  useEffect(() => {
    const t = setInterval(() => {
      window.localStorage.removeItem(ACTIVITY_TOAST_KEY)
    }, 60_000)
    return () => clearInterval(t)
  }, [])

  const submitFriend = async () => {
    if (adding) return
    if (!friendName.trim()) {
      toast.error('Do‘stingiz ismini kiriting')
      return
    }
    setAdding(true)
    const res = await addLiveFriend(friendName, friendAvatar)
    setAdding(false)
    if (res.ok) {
      toast.success(`👋 ${friendName.trim()} do‘stlaringizga qo‘shildi!`, {
        description: 'Reytingda omad tilaymiz',
      })
      setAddOpen(false)
      setFriendName('')
      setFriendAvatar(FRIEND_AVATARS[0])
    } else {
      toast.error('Qo‘shib bo‘lmadi', { description: res.error })
    }
  }

  const entries = state?.entries ?? []
  const myEntry = entries.find((e) => e.isMe)

  if (loading) {
    return (
      <section className="rounded-3xl bg-white p-5 shadow-sm shadow-slate-200/60 dark:bg-[#181b42] dark:shadow-black/30" aria-label="Do'stlar reytingi">
        <div className="mb-4 flex items-center gap-2">
          <Trophy size={16} className="text-amber-500" />
          <h3 className="text-[15px] font-bold text-slate-900 dark:text-slate-50">Do‘stlar reytingi</h3>
        </div>
        <div className="space-y-2.5">
          {[...Array(4)].map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-2xl" />
          ))}
        </div>
      </section>
    )
  }

  return (
    <section
      className="rounded-3xl bg-white p-5 shadow-sm shadow-slate-200/60 dark:bg-[#181b42] dark:shadow-black/30"
      aria-label="Do'stlar reytingi"
    >
      <div className="mb-4 flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-[15px] font-bold text-slate-900 dark:text-slate-50">
          <Trophy size={16} className="text-amber-500" /> Do‘stlar reytingi
        </h3>
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setAddOpen(true)}
            disabled={!connected}
            aria-label="Do'st qo'shish"
            className="flex h-7 w-7 items-center justify-center rounded-full bg-gradient-to-br from-[#3d5afe] to-[#7b61ff] text-white shadow-sm shadow-indigo-500/30 transition-transform hover:scale-105 active:scale-95 disabled:opacity-40"
          >
            <UserPlus size={13} />
          </button>
          <span
            className={cn(
              'flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[10.5px] font-bold ring-1 transition-colors',
              connected
                ? 'bg-emerald-50 text-emerald-600 ring-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-500/20'
                : 'bg-slate-100 text-slate-400 ring-slate-200 dark:bg-white/5 dark:ring-white/10'
            )}
          >
            {connected ? (
              <>
                <span className="relative flex h-1.5 w-1.5">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                  <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-500" />
                </span>
                <Radio size={10} /> JONLI
              </>
            ) : (
              <>
                <WifiOff size={11} /> ulanmoqda…
              </>
            )}
          </span>
        </div>
      </div>

      {connected && myEntry && (
        <div className="mb-3 flex items-center justify-between rounded-2xl bg-gradient-to-r from-[#3d5afe]/8 to-[#e861ff]/8 px-3.5 py-2.5 ring-1 ring-[#3d5afe]/15 dark:ring-[#7b93ff]/20">
          <p className="text-[12px] font-semibold text-slate-500 dark:text-slate-300">
            Sizning o‘rningiz
          </p>
          <p className="text-[13px] font-extrabold text-[#3d5afe] dark:text-[#8ea2ff]">
            {myEntry.rank}-o‘rin / {entries.length}
            {state && state.online > 0 && (
              <span className="ml-2 inline-flex items-center gap-1 text-[10.5px] font-bold text-emerald-500">
                <Wifi size={10} /> {state.online} online
              </span>
            )}
          </p>
        </div>
      )}

      {entries.length === 0 ? (
        <div className="flex flex-col items-center gap-2 py-6 text-slate-400">
          <Wifi size={22} />
          <p className="text-[12.5px] font-medium">Jonli reytingga ulanmoqda…</p>
        </div>
      ) : (
        <ol className="space-y-2">
          {entries.map((e) => (
            <li
              key={e.id}
              className={cn(
                'flex items-center gap-3 rounded-2xl p-2.5 transition-colors',
                e.isMe
                  ? 'bg-gradient-to-r from-[#3d5afe]/8 to-[#7b61ff]/8 ring-1 ring-[#3d5afe]/20 dark:ring-[#7b93ff]/25'
                  : 'hover:bg-slate-50 dark:hover:bg-white/5'
              )}
            >
              <span
                className={cn(
                  'flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[11.5px] font-extrabold',
                  e.rank === 1
                    ? 'bg-gradient-to-br from-amber-300 to-yellow-500 text-white shadow-sm shadow-amber-300'
                    : e.rank === 2
                      ? 'bg-gradient-to-br from-slate-300 to-slate-400 text-white'
                      : e.rank === 3
                        ? 'bg-gradient-to-br from-orange-300 to-amber-600 text-white'
                        : 'bg-slate-100 text-slate-500 dark:bg-white/10 dark:text-slate-400'
                )}
              >
                {e.rank}
              </span>
              <span className="relative flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-lg dark:bg-white/10">
                {e.avatar}
                {e.focusing && (
                  <span className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-emerald-500 ring-2 ring-white dark:ring-[#181b42]" />
                )}
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1.5">
                  <p className={cn('truncate text-[13px] font-bold', e.isMe ? 'text-[#3d5afe] dark:text-[#8ea2ff]' : 'text-slate-800 dark:text-slate-100')}>
                    {e.name}{e.isMe ? ' (siz)' : ''}
                  </p>
                  {e.streak >= 10 && <span className="shrink-0 text-[10px]">🔥</span>}
                </div>
                <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
                  <div
                    className={cn(
                      'h-full rounded-full transition-all duration-700 ease-out',
                      e.isMe
                        ? 'bg-gradient-to-r from-[#3d5afe] to-[#7b61ff]'
                        : 'bg-gradient-to-r from-violet-300 to-fuchsia-300'
                    )}
                    style={{ width: `${e.barPercent}%` }}
                  />
                </div>
              </div>
              <span className="shrink-0 text-[12px] font-extrabold tabular-nums text-slate-600 dark:text-slate-300">
                {formatMinutes(e.savedMinutes)}
              </span>
            </li>
          ))}
        </ol>
      )}

      <p className="mt-3 text-center text-[10px] font-medium text-slate-300 dark:text-slate-600">
        Do‘stlaringiz har soniyada jonli yangilanadi · WebSocket
      </p>

      {/* add friend dialog */}
      <Dialog open={addOpen} onOpenChange={(o) => !o && setAddOpen(false)}>
        <DialogContent
          aria-describedby={undefined}
          className="w-[calc(100%-2rem)] max-w-[330px] translate-y-[-70%] rounded-3xl border-0 bg-white p-5 shadow-2xl [top:50%] dark:bg-[#1c1f4e]"
        >
          <DialogHeader className="space-y-1 text-left">
            <DialogTitle className="flex items-center gap-2 text-[17px] font-extrabold text-slate-900 dark:text-slate-50">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-[#3d5afe] to-[#7b61ff] text-white">
                <UserPlus size={16} />
              </span>
              Do‘st qo‘shish
            </DialogTitle>
            <DialogDescription className="text-[12.5px]">
              Do‘stingiz reytingga darhol qo‘shiladi
            </DialogDescription>
          </DialogHeader>

          <div className="mt-4 space-y-3">
            <Input
              value={friendName}
              onChange={(e) => setFriendName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && submitFriend()}
              placeholder="Ism (masalan: Aziza)"
              maxLength={24}
              aria-label="Do'st ismi"
              className="h-11 rounded-2xl border-slate-200 bg-slate-50 text-[14px] font-semibold focus-visible:ring-[#7b61ff]/40 dark:border-white/10 dark:bg-white/10 dark:text-slate-100"
            />
            <div>
              <p className="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-400">Avatar</p>
              <div className="grid grid-cols-6 gap-1.5">
                {FRIEND_AVATARS.map((a) => (
                  <button
                    key={a}
                    onClick={() => setFriendAvatar(a)}
                    aria-label={`Avatar ${a}`}
                    className={cn(
                      'flex h-9 items-center justify-center rounded-xl text-lg transition-all',
                      friendAvatar === a
                        ? 'bg-gradient-to-br from-[#3d5afe]/20 to-[#e861ff]/20 ring-2 ring-[#7b61ff] scale-105'
                        : 'bg-slate-100 hover:bg-slate-200 dark:bg-white/10 dark:hover:bg-white/15'
                    )}
                  >
                    {a}
                  </button>
                ))}
              </div>
            </div>
            <button
              onClick={submitFriend}
              disabled={adding}
              className={cn(
                'flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-[#3d5afe] via-[#7b61ff] to-[#e861ff] py-3 text-[14px] font-bold text-white shadow-lg shadow-indigo-500/30 transition-transform',
                adding ? 'opacity-60' : 'active:scale-[0.98]'
              )}
            >
              <UserPlus size={15} />
              {adding ? 'Qo‘shilmoqda…' : 'Reytingga qo‘shish'}
            </button>
          </div>
        </DialogContent>
      </Dialog>
    </section>
  )
}
