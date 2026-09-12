'use client'

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { SessionType } from '@/lib/opal-types'

export type TabKey = 'home' | 'focus' | 'stats' | 'apps' | 'profile'

interface ActiveSession {
  sessionId: string
  type: SessionType
  label: string
  emoji: string
  durationMinutes: number
  startedAt: number
  blockedApps: string[]
  strict: boolean
}

interface OpalState {
  tab: TabKey
  setTab: (t: TabKey) => void
  activeSession: ActiveSession | null
  startSession: (s: ActiveSession) => void
  endSession: () => void
  theme: 'light' | 'dark'
  setTheme: (t: 'light' | 'dark') => void
  toggleTheme: () => void
  breathingOpen: boolean
  setBreathingOpen: (v: boolean) => void
  // PIN lock
  pinEnabled: boolean
  pinCode: string | null
  setPin: (code: string) => void
  removePin: () => void
  // achievements already celebrated (avoid duplicate toasts)
  seenAchievements: string[]
  markAchievementsSeen: (labels: string[]) => void
}

export const useOpalStore = create<OpalState>()(
  persist(
    (set) => ({
      tab: 'home',
      setTab: (tab) => set({ tab }),
      activeSession: null,
      startSession: (activeSession) => set({ activeSession }),
      endSession: () => set({ activeSession: null }),
      theme: 'light',
      setTheme: (theme) => set({ theme }),
      toggleTheme: () => set((s) => ({ theme: s.theme === 'light' ? 'dark' : 'light' })),
      breathingOpen: false,
      setBreathingOpen: (breathingOpen) => set({ breathingOpen }),
      pinEnabled: false,
      pinCode: null,
      setPin: (code) => set({ pinCode: code, pinEnabled: true }),
      removePin: () => set({ pinCode: null, pinEnabled: false }),
      seenAchievements: [],
      markAchievementsSeen: (labels) =>
        set((s) => ({ seenAchievements: [...s.seenAchievements, ...labels] })),
    }),
    { name: 'opal-session-store' }
  )
)
