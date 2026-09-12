'use client'

import { motion } from 'framer-motion'
import { useOpalStore, type TabKey } from '@/lib/opal-store'
import { cn } from '@/lib/utils'

const TABS: { key: TabKey; label: string; icon: React.ReactNode }[] = [
  {
    key: 'home',
    label: 'Home',
    icon: (
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none" aria-hidden="true">
        <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="1.8" />
        <circle cx="11" cy="11" r="3" fill="currentColor" opacity="0.9" />
      </svg>
    ),
  },
  {
    key: 'apps',
    label: 'My Apps',
    icon: (
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none" aria-hidden="true">
        {[4, 11, 18].map((x) =>
          [4, 11, 18].map((y) => (
            <circle key={`${x}-${y}`} cx={x} cy={y} r="1.8" fill="currentColor" />
          ))
        )}
      </svg>
    ),
  },
  {
    key: 'timer',
    label: 'Timer',
    icon: (
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none" aria-hidden="true">
        <path d="M7 4.5v13l11-6.5-11-6.5Z" fill="currentColor" />
      </svg>
    ),
  },
]

export function BottomTabBar() {
  const tab = useOpalStore((s) => s.tab)
  const setTab = useOpalStore((s) => s.setTab)

  return (
    <nav
      aria-label="Asosiy navigatsiya"
      className="relative z-30 flex shrink-0 justify-center pb-[max(env(safe-area-inset-bottom),12px)] pt-1"
    >
      <div className="flex items-center gap-1 rounded-full border border-white/10 bg-[#10131f]/80 p-1.5 shadow-[0_10px_40px_rgba(0,0,0,0.55),inset_0_1px_0_0_rgba(255,255,255,0.09)] backdrop-blur-2xl">
        {TABS.map((t) => {
          const active = tab === t.key
          return (
            <motion.button
              key={t.key}
              whileTap={{ scale: 0.92 }}
              onClick={() => setTab(t.key)}
              aria-label={t.label}
              aria-current={active ? 'page' : undefined}
              className={cn(
                'relative flex h-11 w-[74px] flex-col items-center justify-center gap-0.5 rounded-full transition-colors duration-200',
                active ? 'text-[#9fd8ff]' : 'text-white/45 hover:text-white/75'
              )}
            >
              {active && (
                <motion.span
                  layoutId="tab-glow"
                  transition={{ type: 'spring', stiffness: 420, damping: 32 }}
                  className="absolute inset-0 rounded-full bg-white/[0.09] shadow-[inset_0_1px_0_0_rgba(255,255,255,0.1),0_0_18px_rgba(125,211,252,0.18)]"
                />
              )}
              <span className="relative">{t.icon}</span>
              <span className="relative text-[9.5px] font-semibold tracking-wide">{t.label}</span>
            </motion.button>
          )
        })}
      </div>
    </nav>
  )
}
