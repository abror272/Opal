'use client'

import { useEffect } from 'react'
import { useOpalStore } from '@/lib/opal-store'
import { StatusBar } from './status-bar'
import { BottomTabBar } from './bottom-tab-bar'
import { HomeTab } from './home-tab'
import { FocusTab } from './focus-tab'
import { StatsTab } from './stats-tab'
import { AppsTab } from './apps-tab'
import { ProfileTab } from './profile-tab'
import { ActiveSessionOverlay } from './active-session-overlay'

export function OpalApp() {
  const tab = useOpalStore((s) => s.tab)
  const activeSession = useOpalStore((s) => s.activeSession)
  const endSession = useOpalStore((s) => s.endSession)

  // stale session cleanup: if session persisted from a previous day, drop it
  useEffect(() => {
    if (activeSession) {
      const elapsed = (Date.now() - activeSession.startedAt) / 1000
      if (elapsed > activeSession.durationMinutes * 60 + 30) {
        endSession()
      }
    }
  }, [activeSession, endSession])

  return (
    <div className="relative flex min-h-[100dvh] w-full items-center justify-center overflow-hidden bg-[#0a0b2a]">
      {/* Desktop aurora backdrop */}
      <div className="pointer-events-none absolute inset-0 hidden md:block" aria-hidden="true">
        <div className="absolute -left-32 top-1/4 h-96 w-96 animate-blob rounded-full bg-[#3d5afe]/30 blur-3xl" />
        <div className="absolute -right-24 top-10 h-80 w-80 animate-blob-delayed rounded-full bg-[#e861ff]/25 blur-3xl" />
        <div className="absolute bottom-0 left-1/3 h-72 w-72 animate-blob rounded-full bg-[#ff9f5a]/20 blur-3xl" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_0%,#0a0b2a_75%)]" />
      </div>

      {/* Desktop side branding */}
      <aside className="pointer-events-none absolute left-[6%] top-1/2 z-10 hidden max-w-xs -translate-y-1/2 xl:block" aria-hidden="true">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3d5afe] via-[#7b61ff] to-[#e861ff] text-2xl shadow-lg shadow-indigo-500/40">
            <span className="text-white font-black">O</span>
          </div>
          <span className="text-2xl font-extrabold tracking-tight text-white">Opal</span>
        </div>
        <h1 className="bg-gradient-to-r from-white via-indigo-100 to-violet-300 bg-clip-text text-4xl font-extrabold leading-tight text-transparent">
          Ijtimoiy tarmoqlardan ozod hayot
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-indigo-200/70">
          Ilovalarni bloklang, fokus sessiyalarini boshlang va kunlik ekran vaqtingizni nazorat qiling.
        </p>
        <div className="mt-6 flex items-center gap-2 text-xs text-indigo-200/50">
          <span className="rounded-full border border-white/10 px-3 py-1">iOS uslubi</span>
          <span className="rounded-full border border-white/10 px-3 py-1">Web clone</span>
          <span className="rounded-full border border-white/10 px-3 py-1">Next.js</span>
        </div>
      </aside>

      <aside className="pointer-events-none absolute right-[6%] top-1/2 z-10 hidden max-w-[220px] -translate-y-1/2 space-y-4 xl:block" aria-hidden="true">
        {[
          { icon: '🛡️', title: 'Aqlli himoya', desc: 'Chalg‘ituvchi ilovalar bloklang' },
          { icon: '⏱️', title: 'Fokus sessiyalari', desc: 'Deep Focus, Ish, O‘qish rejimlari' },
          { icon: '🔥', title: 'Ketma-ketlik', desc: 'Streakni saqlab qoling' },
        ].map((f) => (
          <div
            key={f.title}
            className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-sm"
          >
            <div className="text-xl">{f.icon}</div>
            <div className="mt-1.5 text-sm font-semibold text-white">{f.title}</div>
            <div className="text-xs text-indigo-200/60">{f.desc}</div>
          </div>
        ))}
      </aside>

      {/* Phone */}
      <div className="relative z-20 h-[100dvh] w-full overflow-hidden bg-[#f4f4fb] shadow-2xl shadow-black/60 md:h-[844px] md:max-h-[94vh] md:w-[392px] md:rounded-[3.4rem] md:border-[11px] md:border-[#131540] md:ring-1 md:ring-white/10">
        <div className="relative flex h-full w-full flex-col">
          {/* notch (desktop only) */}
          <div className="pointer-events-none absolute left-1/2 top-2 z-40 hidden h-7 w-32 -translate-x-1/2 rounded-full bg-[#131540] md:block" aria-hidden="true" />

          <StatusBar />

          <main className="thin-scrollbar relative flex-1 overflow-y-auto overflow-x-hidden">
            {tab === 'home' && <HomeTab />}
            {tab === 'focus' && <FocusTab />}
            {tab === 'stats' && <StatsTab />}
            {tab === 'apps' && <AppsTab />}
            {tab === 'profile' && <ProfileTab />}
          </main>

          <BottomTabBar />
          {activeSession && <ActiveSessionOverlay />}
        </div>
      </div>
    </div>
  )
}
