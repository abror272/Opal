'use client'

import { useEffect, useState, useSyncExternalStore } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { useOpalStore, TAB_KEYS } from '@/lib/opal-store'
import { StatusBar } from './status-bar'
import { BottomTabBar } from './bottom-tab-bar'
import { HomeTab } from './home-tab'
import { AppsTab } from './apps-tab'
import { TimerTab } from './timer-tab'
import { TodayView } from './today-view'
import { ProfileView } from './profile-view'
import { HistoryView } from './history-view'
import { WeeklyReport } from './weekly-report'
import { RuleWatcher } from './rule-watcher'
import { SessionPill } from './session-pill'
import { BlockScreen } from './block-screen'
import { Onboarding, useNeedsOnboarding } from './onboarding'
import { NotificationBanner } from './notification-banner'
import { BreathingOverlay } from './breathing-overlay'
import { LockScreen } from './lock-screen'
import { cn } from '@/lib/utils'
import { ChevronLeft, ChevronRight } from 'lucide-react'

/** PIN sozlamasi — SSR-safe (serverda null, keyin store'dan) */
function usePinEnabled(): boolean | null {
  return useSyncExternalStore(
    (cb) => useOpalStore.subscribe(cb),
    () => useOpalStore.getState().pinEnabled,
    () => null
  )
}

/** suzuvchi opal bo'lakchalari */
function OpalShards() {
  const shards = [
    { left: '8%', top: '16%', size: 26, delay: 0, dur: 9, rot: 24 },
    { left: '84%', top: '24%', size: 20, delay: 2.2, dur: 11, rot: -18 },
    { left: '16%', top: '64%', size: 18, delay: 4.1, dur: 10, rot: 40 },
    { left: '78%', top: '72%', size: 24, delay: 1.2, dur: 12, rot: -30 },
    { left: '58%', top: '6%', size: 14, delay: 3.3, dur: 8, rot: 12 },
  ]
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
      {shards.map((s, i) => (
        <motion.div
          key={i}
          className="absolute rounded-[30%] bg-gradient-to-br from-[#7d8ba8]/40 via-[#4a5570]/30 to-[#1c2233]/50 blur-[1px]"
          style={{ left: s.left, top: s.top, width: s.size, height: s.size * 1.5 }}
          animate={{ y: [0, -18, 0], rotate: [s.rot, s.rot + 14, s.rot], opacity: [0.5, 0.85, 0.5] }}
          transition={{ duration: s.dur, repeat: Infinity, delay: s.delay, ease: 'easeInOut' }}
        />
      ))}
    </div>
  )
}

type OverlayView = 'today' | 'profile' | 'history' | 'report' | null

export function OpalApp() {
  const tab = useOpalStore((s) => s.tab)
  const setTab = useOpalStore((s) => s.setTab)
  const activeSession = useOpalStore((s) => s.activeSession)
  const endSession = useOpalStore((s) => s.endSession)
  const needsOnboarding = useNeedsOnboarding()
  const breathingOpen = useOpalStore((s) => s.breathingOpen)
  const [obDone, setObDone] = useState(false)
  const showOnboarding = needsOnboarding === true && !obDone
  const [view, setView] = useState<OverlayView>(null)

  // PIN lock
  const pinEnabled = usePinEnabled()
  const [unlocked, setUnlocked] = useState(false)
  const [prevPin, setPrevPin] = useState<boolean | null>(pinEnabled)
  if (prevPin !== pinEnabled) {
    setPrevPin(pinEnabled)
    if (prevPin === false && pinEnabled === true) setUnlocked(true)
  }
  const locked = pinEnabled === true && !unlocked

  // eski (5-tab) saqlangan holatini tozalash
  useEffect(() => {
    if (!TAB_KEYS.includes(tab)) setTab('home')
  }, [tab, setTab])

  // stale session cleanup: legacy 'preview' sessions or sessions from a previous day
  useEffect(() => {
    if (activeSession) {
      const elapsed = (Date.now() - activeSession.startedAt) / 1000
      const legacy = !activeSession.sessionId || activeSession.sessionId === 'preview'
      if (legacy || elapsed > activeSession.durationMinutes * 60 + 30) {
        endSession()
      }
    }
  }, [activeSession, endSession])

  // bolalar komponentlaridan drill-in navigatsiya so'rovlari
  useEffect(() => {
    const openToday = () => setView('today')
    const openProfile = () => setView('profile')
    const openHistory = () => setView('history')
    const openReport = () => setView('report')
    window.addEventListener('opal:open-today', openToday)
    window.addEventListener('opal:open-profile', openProfile)
    window.addEventListener('opal:open-history', openHistory)
    window.addEventListener('opal:open-report', openReport)
    return () => {
      window.removeEventListener('opal:open-today', openToday)
      window.removeEventListener('opal:open-profile', openProfile)
      window.removeEventListener('opal:open-history', openHistory)
      window.removeEventListener('opal:open-report', openReport)
    }
  }, [])

  // Escape — ochiq drill-in viewni yopadi (klaviatura qulayligi)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setView(null)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  // PWA service worker (offline shell) — FAQAT production build'da.
  // Dev'da SW cache-first statik keshi yangi kodni qoplaydigan stale bug tug'diradi.
  useEffect(() => {
    if (process.env.NODE_ENV === 'production' && 'serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(() => {
        // SW mavjud bo'lmasa — e'tiborsiz (muhim emas)
      })
    }
  }, [])

  return (
    <div className="relative flex min-h-[100dvh] w-full items-center justify-center overflow-hidden bg-[#04050c]">
      {/* desktop fon: tuman + yulduzlar */}
      <div className="pointer-events-none absolute inset-0 hidden md:block" aria-hidden="true">
        <div
          className="absolute inset-0"
          style={{
            background:
              'radial-gradient(ellipse 70% 50% at 30% 20%, rgba(50,64,102,0.35) 0%, transparent 60%),' +
              'radial-gradient(ellipse 60% 45% at 75% 75%, rgba(70,52,110,0.3) 0%, transparent 60%),' +
              'radial-gradient(ellipse 50% 40% at 55% 45%, rgba(40,80,110,0.22) 0%, transparent 65%)',
          }}
        />
        <div
          className="absolute inset-0 opacity-60"
          style={{
            backgroundImage:
              'radial-gradient(1px 1px at 12% 24%, rgba(255,255,255,0.5) 50%, transparent 51%),' +
              'radial-gradient(1px 1px at 68% 12%, rgba(255,255,255,0.4) 50%, transparent 51%),' +
              'radial-gradient(1.5px 1.5px at 84% 58%, rgba(255,255,255,0.5) 50%, transparent 51%),' +
              'radial-gradient(1px 1px at 32% 78%, rgba(255,255,255,0.35) 50%, transparent 51%),' +
              'radial-gradient(1px 1px at 52% 44%, rgba(255,255,255,0.3) 50%, transparent 51%)',
          }}
        />
      </div>

      {/* desktop yon brending */}
      <aside
        className="pointer-events-none absolute left-[6%] top-1/2 z-10 hidden max-w-xs -translate-y-1/2 xl:block"
        aria-hidden="true"
      >
        <div className="mb-4 flex items-center gap-3">
          <span className="inline-block h-10 w-10 rounded-[13px] bg-gradient-to-br from-[#d9fbe6] via-[#86efac] to-[#5eead4] shadow-[0_0_24px_rgba(134,239,172,0.45)]" />
          <span className="text-2xl font-bold tracking-tight text-white">Opal</span>
        </div>
        <h1 className="text-4xl font-extrabold leading-tight tracking-tight text-white">
          Chalg‘ituvchilar
          <br />
          siz uchun bloklanadi.
        </h1>
        <p className="mt-4 text-sm leading-relaxed text-white/45">
          Uyqu, fokus va dam uchun bitta ko‘rsatkich. Bir bosish — to‘liq fokus.
        </p>
        <div className="mt-6 flex flex-wrap items-center gap-2 text-[11px] font-semibold text-white/40">
          <span className="rounded-full border border-white/10 px-3 py-1">Apple Design Award 2025 uslubi</span>
          <span className="rounded-full border border-white/10 px-3 py-1">Battle Math</span>
        </div>
      </aside>

      {/* telefon */}
      <div className="dark relative z-20 h-[100dvh] w-full overflow-hidden bg-[#05060f] shadow-2xl shadow-black/60 md:h-[844px] md:max-h-[94vh] md:w-[392px] md:rounded-[3.4rem] md:border-[11px] md:border-[#181c26] md:ring-1 md:ring-white/10">
        <div className="relative flex h-full w-full flex-col">
          {/* notch (desktop) */}
          <div
            className="pointer-events-none absolute left-1/2 top-2 z-40 hidden h-7 w-32 -translate-x-1/2 rounded-full bg-[#181c26] md:block"
            aria-hidden="true"
          />
          {/* suzuvchi bo'lakchalar (onboardingda ham ko'rinadi) */}
          <OpalShards />

          {needsOnboarding === null ? (
            <div className="flex h-full items-center justify-center">
              <div className="h-9 w-9 animate-spin rounded-full border-[3px] border-[#86efac]/20 border-t-[#86efac]" />
            </div>
          ) : showOnboarding ? (
            <Onboarding onDone={() => setObDone(true)} />
          ) : (
            <>
              <StatusBar />

              <main className="thin-scrollbar relative flex-1 overflow-y-auto overflow-x-hidden">
                <AnimatePresence mode="wait" initial={false}>
                  <motion.div
                    key={tab}
                    initial={{ opacity: 0, y: 14 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
                    className="min-h-full"
                  >
                    {tab === 'home' && <HomeTab />}
                    {tab === 'apps' && <AppsTab />}
                    {tab === 'timer' && <TimerTab />}
                  </motion.div>
                </AnimatePresence>
              </main>

              <BottomTabBar />
              <SessionPill />
              <RuleWatcher />

              {/* drill-in: Bugun (stats) */}
              <AnimatePresence>
                {view === 'today' && (
                  <motion.div
                    key="today-view"
                    initial={{ x: '100%' }}
                    animate={{ x: 0 }}
                    exit={{ x: '100%' }}
                    transition={{ type: 'spring', stiffness: 340, damping: 34 }}
                    className="absolute inset-0 z-40 flex flex-col bg-[#05060f]"
                  >
                    <StatusBar />
                    <div className="flex items-center justify-between px-4 pb-1 pt-1">
                      <button
                        onClick={() => setView(null)}
                        aria-label="Orqaga"
                        className="flex h-10 w-10 items-center justify-center rounded-full bg-white/8 text-white/80 ring-1 ring-white/12 backdrop-blur transition-transform active:scale-90"
                      >
                        <ChevronLeft size={19} />
                      </button>
                      <div className="flex items-center gap-2.5 text-white">
                        <ChevronLeft size={14} className="text-white/30" />
                        <span className="text-[16px] font-bold">Today</span>
                        <ChevronRight size={14} className="text-white/30" />
                      </div>
                      <span className="h-10 w-10" aria-hidden="true" />
                    </div>
                    <div className="thin-scrollbar flex-1 overflow-y-auto">
                      <TodayView />
                    </div>
                  </motion.div>
                )}

                {/* drill-in: Profil */}
                {view === 'profile' && (
                  <motion.div
                    key="profile-view"
                    initial={{ x: '100%' }}
                    animate={{ x: 0 }}
                    exit={{ x: '100%' }}
                    transition={{ type: 'spring', stiffness: 340, damping: 34 }}
                    className="absolute inset-0 z-40 flex flex-col bg-[#05060f]"
                  >
                    <StatusBar />
                    <div className="flex items-center justify-between px-4 pb-1 pt-1">
                      <button
                        onClick={() => setView(null)}
                        aria-label="Orqaga"
                        className="flex h-10 w-10 items-center justify-center rounded-full bg-white/8 text-white/80 ring-1 ring-white/12 backdrop-blur transition-transform active:scale-90"
                      >
                        <ChevronLeft size={19} />
                      </button>
                      <span className="text-[16px] font-bold text-white">Profil</span>
                      <span className="h-10 w-10" aria-hidden="true" />
                    </div>
                    <div className="thin-scrollbar flex-1 overflow-y-auto">
                      <ProfileView />
                    </div>
                  </motion.div>
                )}

                {/* drill-in: Sessiyalar tarixi */}
                {view === 'history' && (
                  <motion.div
                    key="history-view"
                    initial={{ x: '100%' }}
                    animate={{ x: 0 }}
                    exit={{ x: '100%' }}
                    transition={{ type: 'spring', stiffness: 340, damping: 34 }}
                    className="absolute inset-0 z-40 flex flex-col bg-[#05060f]"
                  >
                    <StatusBar />
                    <div className="flex items-center justify-between px-4 pb-1 pt-1">
                      <button
                        onClick={() => setView(null)}
                        aria-label="Orqaga"
                        className="flex h-10 w-10 items-center justify-center rounded-full bg-white/8 text-white/80 ring-1 ring-white/12 backdrop-blur transition-transform active:scale-90"
                      >
                        <ChevronLeft size={19} />
                      </button>
                      <span className="text-[16px] font-bold text-white">Tarix</span>
                      <span className="h-10 w-10" aria-hidden="true" />
                    </div>
                    <div className="thin-scrollbar flex-1 overflow-y-auto">
                      <HistoryView />
                    </div>
                  </motion.div>
                )}

                {/* drill-in: Haftalik hisobot */}
                {view === 'report' && (
                  <motion.div
                    key="report-view"
                    initial={{ x: '100%' }}
                    animate={{ x: 0 }}
                    exit={{ x: '100%' }}
                    transition={{ type: 'spring', stiffness: 340, damping: 34 }}
                    className="absolute inset-0 z-40 flex flex-col bg-[#05060f]"
                  >
                    <StatusBar />
                    <div className="flex items-center justify-between px-4 pb-1 pt-1">
                      <button
                        onClick={() => setView(null)}
                        aria-label="Orqaga"
                        className="flex h-10 w-10 items-center justify-center rounded-full bg-white/8 text-white/80 ring-1 ring-white/12 backdrop-blur transition-transform active:scale-90"
                      >
                        <ChevronLeft size={19} />
                      </button>
                      <span className="text-[16px] font-bold text-white">Hisobot</span>
                      <span className="h-10 w-10" aria-hidden="true" />
                    </div>
                    <div className="thin-scrollbar flex-1 overflow-y-auto">
                      <WeeklyReport />
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              <NotificationBanner />
              {breathingOpen && !activeSession && <BreathingOverlay />}
              <BlockScreen />
            </>
          )}

          {/* PIN lock — eng ustda */}
          <AnimatePresence>
            {locked && <LockScreen key="lock" onUnlock={() => setUnlocked(true)} />}
          </AnimatePresence>
        </div>
      </div>
    </div>
  )
}
