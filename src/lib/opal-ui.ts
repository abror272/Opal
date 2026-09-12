// Umumiy UI yordamchilari — haqiqiy Opal (Apple Design Award 2025) dizayn tili

import type { FocusSession, StatsResponse } from '@/lib/opal-types'

/** Opal glassmorphism asosiy klassi */
export const GLASS =
  'rounded-3xl border border-white/10 bg-white/[0.055] backdrop-blur-xl shadow-[inset_0_1px_0_0_rgba(255,255,255,0.07),0_8px_32px_rgba(0,0,0,0.35)]'

/** Neon cyan glow klassi (bloklangan ilova ringlari, ballar) */
export const CYAN_GLOW = 'shadow-[0_0_14px_rgba(125,211,252,0.35)]'

export function clamp(n: number, min: number, max: number): number {
  return Math.min(Math.max(n, min), max)
}

export interface OpalScores {
  score: number
  focus: number
  rest: number
  sleep: number
  /** ball o'zgarishi (ijobiy = yaxshilanish) */
  delta: number
}

/**
 * Haqiqiy Opal kabi sub-ballarni hisoblaydi:
 * Score = sleep/focus/rest kombinatsiyasi, Screen Time va Distracting Apps dan.
 */
export function computeScores(
  stats: StatsResponse | undefined,
  sessions: FocusSession[] | undefined
): OpalScores {
  const today = stats?.today
  const screen = today?.screenTimeMinutes ?? 180
  const saved = today?.savedMinutes ?? 0
  const goal = stats?.goalMinutes ?? 240

  const todayKey = new Date().toISOString().slice(0, 10)
  const todaysSessions = (sessions ?? []).filter(
    (s) => (s.endedAt ?? s.startedAt).slice(0, 10) === todayKey
  )
  const completed = todaysSessions.filter((s) => s.completed)

  const over = Math.max(screen - goal, 0)
  const score = Math.round(clamp(88 - over * 0.1 + saved * 0.18, 42, 99))
  const focus = Math.round(clamp(52 + completed.length * 11 + saved * 0.09, 38, 98))
  const rest = Math.round(clamp(95 - screen * 0.1, 44, 97))
  const hadSleep = todaysSessions.some((s) => s.type === 'SLEEP')
  const sleep = hadSleep ? 88 : Math.round(clamp(80 - screen * 0.04, 55, 82))

  // trend: ijobiy trendPercent (screen time o'sishi) → ball pasayishi
  const trend = stats?.trendPercent ?? 0
  const delta = trend === 0 ? 0 : trend > 0 ? -Math.min(Math.round(trend / 8), 6) : Math.min(Math.round(-trend / 8), 6)

  return { score, focus, rest, sleep, delta }
}

/** Sessiya tugallanganda moye ilova shakllari uchun */
export const SESSION_WITTY_LINES = [
  'Siz bu qoidani kecha o‘rnatgansiz. O‘tgan siz to‘g‘ri edi.',
  'Kelajakdagi sizga minnatdorchilik bildiradi.',
  'Bu ilova sizning eng yaxshi versiyangizga yo‘l to‘sqinlik qiladi.',
  'Bir oz tinchlanish — eng yaxshi tanlov.',
]
