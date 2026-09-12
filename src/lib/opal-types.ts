// Shared types for Opal clone

export interface BlockApp {
  id: string
  name: string
  emoji: string
  gradient: string
  category: string
  blocked: boolean
  dailyLimitMinutes: number
  todayMinutes: number
  order: number
}

export interface FocusSession {
  id: string
  type: SessionType
  label: string
  emoji: string
  durationMinutes: number
  startedAt: string
  endedAt: string | null
  completed: boolean
  savedMinutes: number
  focusScore: number
  blockedApps: string
}

export type SessionType = 'DEEP_FOCUS' | 'WORK' | 'STUDY' | 'SLEEP' | 'CUSTOM'

export interface DailyStat {
  id: string
  date: string
  screenTimeMinutes: number
  savedMinutes: number
  pickups: number
  goalMinutes: number
}

export interface UserProfile {
  id: string
  name: string
  handle: string
  streakDays: number
  totalSavedMinutes: number
  totalSessions: number
  protectionEnabled: boolean
  strictMode: boolean
  goalMinutes: number
  plan: string
}

export interface StatsResponse {
  days: DailyStat[]
  today: DailyStat
  weekSavedMinutes: number
  weekScreenMinutes: number
  avgDailyScreenMinutes: number
  trendPercent: number
  goalMinutes: number
}

export const SESSION_PRESETS: {
  type: SessionType
  label: string
  emoji: string
  duration: number
  gradient: string
  desc: string
}[] = [
  { type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', duration: 45, gradient: 'from-[#3d5afe] to-[#7b61ff]', desc: 'Eng chalg‘ituvchi ilovalar bloklanadi' },
  { type: 'WORK', label: 'Ish rejimi', emoji: '💼', duration: 90, gradient: 'from-[#7b61ff] to-[#e861ff]', desc: 'Faqat ish ilovalari ochiq qoladi' },
  { type: 'STUDY', label: 'O‘qish', emoji: '📚', duration: 60, gradient: 'from-[#ff6b9d] to-[#ff9f5a]', desc: 'Kutubxona rejimi, tinchlik' },
  { type: 'SLEEP', label: 'Uyqu rejimi', emoji: '🌙', duration: 480, gradient: 'from-[#1e2a78] to-[#3d5afe]', desc: 'Tungi oshkorlik bildirishnomalar o‘chadi' },
  { type: 'CUSTOM', label: 'Maxsus', emoji: '⚡', duration: 30, gradient: 'from-[#ff9f5a] to-[#ff6b9d]', desc: 'O‘z davomiyligingizni tanlang' },
]

export const SESSION_DURATIONS = [15, 25, 30, 45, 60, 90, 120]

export function formatMinutes(min: number): string {
  const h = Math.floor(min / 60)
  const m = Math.round(min % 60)
  if (h <= 0) return `${m}d`
  if (m === 0) return `${h}s`
  return `${h}s ${m}d`
}

export function formatClock(totalSeconds: number): string {
  const h = Math.floor(totalSeconds / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = Math.floor(totalSeconds % 60)
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export function dayLabel(iso: string): string {
  const days = ['Yak', 'Dush', 'Sesh', 'Chor', 'Pay', 'Jum', 'Shan']
  const d = new Date(iso + 'T00:00:00')
  return days[d.getDay()]
}

export function todayISO(): string {
  return new Date().toISOString().slice(0, 10)
}
