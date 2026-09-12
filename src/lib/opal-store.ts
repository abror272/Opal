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
}

interface OpalState {
  tab: TabKey
  setTab: (t: TabKey) => void
  activeSession: ActiveSession | null
  startSession: (s: ActiveSession) => void
  endSession: () => void
}

export const useOpalStore = create<OpalState>()(
  persist(
    (set) => ({
      tab: 'home',
      setTab: (tab) => set({ tab }),
      activeSession: null,
      startSession: (activeSession) => set({ activeSession }),
      endSession: () => set({ activeSession: null }),
    }),
    { name: 'opal-session-store' }
  )
)
