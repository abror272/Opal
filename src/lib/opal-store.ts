'use client'

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { SessionType } from '@/lib/opal-types'

export type TabKey = 'home' | 'apps' | 'timer'

export const TAB_KEYS: TabKey[] = ['home', 'apps', 'timer']

export interface ActiveSession {
  sessionId: string
  type: SessionType
  label: string
  emoji: string
  durationMinutes: number
  startedAt: number
  blockedApps: string[]
  strict: boolean
}

/** Rule/Tanlov → Timer tab'ga uzatiladigan qoralama */
export interface TimerDraft {
  type: SessionType
  label: string
  emoji: string
  durationMinutes: number
  strict: boolean
  nonce: number // qayta tanlashda ham effekt ishlashi uchun
}

/** Bloklangan ilova ekrani (haqiqiy Opal "Blocked by Opal" sahifasi) */
export interface BlockedAppView {
  name: string
  emoji: string
  gradient: string
}

/** Yakunlangan sessiya — "Sessiya qanday o'tdi?" baholash kartasi uchun */
export interface RatingPending {
  id: string
  label: string
  emoji: string
  early: boolean
  /** baholanganmi (kartani yashirish uchun) */
  rated: boolean
}

interface OpalState {
  tab: TabKey
  setTab: (t: TabKey) => void
  activeSession: ActiveSession | null
  startSession: (s: ActiveSession) => void
  endSession: () => void
  timerDraft: TimerDraft | null
  setTimerDraft: (d: Omit<TimerDraft, 'nonce'>) => void
  clearTimerDraft: () => void
  blockedView: BlockedAppView | null
  setBlockedView: (v: BlockedAppView | null) => void
  breathingOpen: boolean
  setBreathingOpen: (v: boolean) => void
  /** legacy — eski saqlangan holat bilan mos kelishi uchun; endi ishlatilmaydi */
  theme: 'light' | 'dark'
  // PIN lock
  pinEnabled: boolean
  pinCode: string | null
  setPin: (code: string) => void
  removePin: () => void
  // achievements already celebrated (avoid duplicate toasts)
  seenAchievements: string[]
  markAchievementsSeen: (labels: string[]) => void
  // fokus baholash (sessiya tugagach)
  ratingPending: RatingPending | null
  setRatingPending: (r: Omit<RatingPending, 'rated'>) => void
  markRatingDone: () => void
}

export const useOpalStore = create<OpalState>()(
  persist(
    (set) => ({
      tab: 'home',
      setTab: (tab) => set({ tab }),
      activeSession: null,
      startSession: (activeSession) => set({ activeSession }),
      endSession: () => set({ activeSession: null }),
      timerDraft: null,
      setTimerDraft: (d) => set({ timerDraft: { ...d, nonce: Date.now() } }),
      clearTimerDraft: () => set({ timerDraft: null }),
      blockedView: null,
      setBlockedView: (blockedView) => set({ blockedView }),
      breathingOpen: false,
      setBreathingOpen: (breathingOpen) => set({ breathingOpen }),
      theme: 'dark',
      pinEnabled: false,
      pinCode: null,
      setPin: (code) => set({ pinCode: code, pinEnabled: true }),
      removePin: () => set({ pinCode: null, pinEnabled: false }),
      seenAchievements: [],
      markAchievementsSeen: (labels) =>
        set((s) => ({ seenAchievements: [...s.seenAchievements, ...labels] })),
      ratingPending: null,
      setRatingPending: (r) => set({ ratingPending: { ...r, rated: false } }),
      markRatingDone: () =>
        set((s) => (s.ratingPending ? { ratingPending: { ...s.ratingPending, rated: true } } : {})),
    }),
    {
      name: 'opal-session-store',
      migrate: (persisted) => {
        const p = (persisted ?? {}) as Partial<OpalState>
        // eski 5-tab holatini yangi 3-tab'ga o'tkazish
        if (p.tab && !TAB_KEYS.includes(p.tab)) p.tab = 'home'
        return p as OpalState
      },
      version: 2,
    }
  )
)
