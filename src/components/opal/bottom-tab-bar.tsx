'use client'

import { useOpalStore, type TabKey } from '@/lib/opal-store'
import { Shield, Timer, BarChart3, LayoutGrid, User } from 'lucide-react'
import { cn } from '@/lib/utils'

const TABS: { key: TabKey; label: string; icon: React.ElementType }[] = [
  { key: 'home', label: 'Himoya', icon: Shield },
  { key: 'focus', label: 'Fokus', icon: Timer },
  { key: 'stats', label: 'Statistika', icon: BarChart3 },
  { key: 'apps', label: 'Ilovalar', icon: LayoutGrid },
  { key: 'profile', label: 'Profil', icon: User },
]

export function BottomTabBar() {
  const tab = useOpalStore((s) => s.tab)
  const setTab = useOpalStore((s) => s.setTab)
  const activeSession = useOpalStore((s) => s.activeSession)

  return (
    <nav
      className="relative z-30 shrink-0 border-t border-slate-200/70 bg-white/85 backdrop-blur-xl dark:border-white/10 dark:bg-[#12143a]/90"
      aria-label="Asosiy navigatsiya"
    >
      {activeSession && (
        <button
          onClick={() => setTab('focus')}
          className="absolute -top-12 left-3 right-3 h-10 rounded-2xl bg-gradient-to-r from-[#3d5afe] to-[#7b61ff] text-white flex items-center justify-between px-4 shadow-lg shadow-indigo-500/30 transition-transform active:scale-[0.98]"
        >
          <span className="text-[13px] font-semibold flex items-center gap-2">
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-300 opacity-75" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
            </span>
            Sessiya davom etmoqda
          </span>
          <span className="text-[12px] font-medium opacity-90">Ko‘rish →</span>
        </button>
      )}
      <div className="mx-auto grid max-w-md grid-cols-5 px-1 pb-5 pt-1.5">
        {TABS.map(({ key, label, icon: Icon }) => {
          const active = tab === key
          return (
            <button
              key={key}
              onClick={() => setTab(key)}
              aria-label={label}
              aria-current={active ? 'page' : undefined}
              className={cn(
                'group flex flex-col items-center gap-0.5 rounded-xl py-1.5 transition-all duration-200',
                active ? 'text-[#3d5afe] dark:text-[#8ea2ff]' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300',
                !active && 'active:scale-90'
              )}
            >
              <span
                key={active ? 'on' : 'off'}
                className={cn(
                  'relative flex h-8 w-12 items-center justify-center rounded-full transition-all',
                  active && 'animate-pop bg-gradient-to-br from-[#3d5afe]/20 to-[#e861ff]/15 shadow-sm shadow-indigo-300/40 dark:shadow-indigo-900/40'
                )}
              >
                <Icon
                  size={21}
                  strokeWidth={active ? 2.4 : 2}
                  className="transition-transform duration-200 group-active:scale-90"
                />
                {active && (
                  <span
                    aria-hidden="true"
                    className="absolute -bottom-1 h-1 w-1 rounded-full bg-gradient-to-r from-[#3d5afe] to-[#e861ff]"
                  />
                )}
              </span>
              <span className={cn('text-[10px] transition-all', active ? 'font-semibold' : 'font-medium')}>
                {label}
              </span>
            </button>
          )
        })}
      </div>
    </nav>
  )
}
