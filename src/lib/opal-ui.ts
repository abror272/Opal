// Umumiy UI yordamchilari — haqiqiy Opal (Apple Design Award 2025) dizayn tili
// Referens: foydalanuvchi yuklagan haqiqiy Opal skrinshotlari (Mobbin)

import type { FocusSession, StatsResponse, SessionType } from '@/lib/opal-types'

/** Opal glassmorphism asosiy klassi */
export const GLASS =
  'rounded-3xl border border-white/10 bg-white/[0.055] backdrop-blur-xl shadow-[inset_0_1px_0_0_rgba(255,255,255,0.07),0_8px_32px_rgba(0,0,0,0.35)]'

/**
 * Haqiqiy Opal aksent tizimi — mint-yashil nur (#b7f5cd → #5eead4),
 * olovli streak (oltin) va deyarli qora fon.
 */
export const OPAL = {
  /** asosiy mint nur (score, pilllar) */
  mint: '#b7f5cd',
  mintSoft: 'rgba(183,245,205,0.55)',
  mintGlow: 'rgba(134,239,172,0.45)',
  /** gradient pill stropkasi */
  ringGrad: ['#8ee7ae', '#5eead4'],
  /** oltin olov (streak) */
  flame: '#ffc46b',
} as const

/** Neon glow klassi (score raqamlari, bloklangan ilova ringlari) */
export const CYAN_GLOW = 'shadow-[0_0_14px_rgba(183,245,205,0.4)]'

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

/* ────────────────────────────────────────────────────────────
   KONTEKSTUAL TAVSIYA DVIGATELI (haqiqiy Opal "Sleep / Last
   Pickup — Trouble winding down?" glass kartasi kabi)
   ──────────────────────────────────────────────────────────── */

export type SuggestionAction = 'breathe' | 'timer' | 'apps'

export interface OpalSuggestion {
  /** kategoriya (kartaning chap ikonkasi) */
  category: 'Sleep' | 'Focus' | 'Rest'
  /** qo'shimcha yorliq (o'ng ikonka uchun kontekst) */
  tag: string
  title: string
  body: string
  cta: string
  action: SuggestionAction
  /** timer uchun draft (action === 'timer') */
  timer?: { type: SessionType; label: string; emoji: string; durationMinutes: number }
}

/**
 * Berilgan vaqt uchun mos tavsiyalar ro'yxati (birinchi — asosiy).
 * Haqiqiy Opal ham kun vaqti bo'yicha karochani almashadi.
 */
export function suggestionsFor(stats: StatsResponse | undefined, now: Date = new Date()): OpalSuggestion[] {
  const h = now.getHours()
  const today = stats?.today
  const overGoal = !!today && today.screenTimeMinutes > today.goalMinutes
  const out: OpalSuggestion[] = []

  // maqsaddan oshgan bo'lsa — har qanday vaqtda birinchi ustuvorlik
  if (overGoal) {
    out.push({
      category: 'Focus',
      tag: 'Maqsad oshdi',
      title: 'Ekrandan tanaffus kerak',
      body: 'Bugungi ekran vaqti maqsaddan oshdi. Qisqa fokus sessiyasi reytingni tiklaydi.',
      cta: 'Fokusni boshlash',
      action: 'timer',
      timer: { type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', durationMinutes: 45 },
    })
  }

  if (h >= 22 || h < 5) {
    out.push({
      category: 'Sleep',
      tag: 'Uyqu rejimi',
      title: 'Yotish vaqti bo‘ldi',
      body: 'Telefon yotoqda ertangi kunning energiyasini o‘g‘irlaydi. Uyqu rejimini yoqing.',
      cta: 'Uyqu rejimini boshlash',
      action: 'timer',
      timer: { type: 'SLEEP', label: 'Uyqu', emoji: '🌙', durationMinutes: 480 },
    })
  }
  if (h >= 18 && h < 22) {
    out.push({
      category: 'Sleep',
      tag: 'Oxirgi Pickup',
      title: 'Tinchlash qiyinmi?',
      body: 'Uyquga yengil kirish uchun yo‘naltirilgan meditatsiyani sinab ko‘ring.',
      cta: 'Meditatsiya va uyqu',
      action: 'breathe',
    })
  }
  if (h >= 14 && h < 18) {
    out.push({
      category: 'Rest',
      tag: 'Tushlikdan keyin',
      title: 'Kun o‘rtasidagi pasayish?',
      body: '1 daqiqalik nafas mashg‘uloti fokusni qayta tiklaydi — ilova ochmasdan.',
      cta: '1 daqiqa nafas olish',
      action: 'breathe',
    })
  }
  if (h >= 11 && h < 14) {
    out.push({
      category: 'Rest',
      tag: 'Tushlik yaqin',
      title: 'Kichik dam rejimini rejalashtiring',
      body: 'Ish rejimidan oldin qisqa dam bloki energiyani saqlab qoladi.',
      cta: 'Dam taymerini qo‘yish',
      action: 'timer',
      timer: { type: 'CUSTOM', label: 'Dam olish', emoji: '🌳', durationMinutes: 20 },
    })
  }
  if (h >= 5 && h < 11) {
    out.push({
      category: 'Focus',
      tag: 'Yangi kun',
      title: 'Yangi kun — yangi rekord',
      body: 'Chalg‘ituvchilar ortga to‘planishidan oldin chuqur fokus bilan boshlang.',
      cta: 'Deep Focus 45d',
      action: 'timer',
      timer: { type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', durationMinutes: 45 },
    })
  }

  // har doim mavjud umumiy variantlar (almashish uchun)
  out.push(
    {
      category: 'Focus',
      tag: 'Fokus pakti',
      title: 'Keyingi fokus bloki',
      body: 'Chalg‘ituvchilar bloklangan holda chuqur ish — eng tez natija beradi.',
      cta: 'Deep Focus 45d',
      action: 'timer',
      timer: { type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', durationMinutes: 45 },
    },
    {
      category: 'Rest',
      tag: 'Mikro-tanaffus',
      title: '1 daqiqada qayta yuklaning',
      body: 'Nafas mashg‘uloti bilan asab tizimini tinchlantiring.',
      cta: 'Nafas olish mashqi',
      action: 'breathe',
    }
  )
  return out
}

/** offset bo'yicha almashib turuvchi tavsiya */
export function pickSuggestion(
  stats: StatsResponse | undefined,
  now: Date = new Date(),
  offset = 0
): OpalSuggestion {
  const list = suggestionsFor(stats, now)
  return list[offset % list.length]
}

/* ────────────────────────────────────────────────────────────
   GEMSTONES (haqiqiy Opal profil "Gemstones" karuseli kabi)
   ──────────────────────────────────────────────────────────── */

export interface OpalGem {
  key: string
  name: string
  /** dunyoda egallagan ulush (haqiqiy Opal "Owned by 23%") */
  ownedPct: number
  /** CSS radial-gradient — har bir tosh o'z rangida */
  colors: [string, string, string]
  unlocked: boolean
  desc: string
}

export function gemsFor(
  profile: { totalSessions: number; streakDays: number; totalSavedMinutes: number; plan: string } | undefined,
  hadSleepSession: boolean
): OpalGem[] {
  const p = profile
  return [
    {
      key: 'first',
      name: 'First',
      ownedPct: 98,
      colors: ['#a5f3fc', '#67e8f9', '#0e7490'],
      unlocked: (p?.totalSessions ?? 0) >= 1,
      desc: 'Birinchi sessiya',
    },
    {
      key: 'motivated',
      name: 'Motivated',
      ownedPct: 95,
      colors: ['#fbcfe8', '#f0abfc', '#a21caf'],
      unlocked: (p?.totalSessions ?? 0) >= 5,
      desc: '5 sessiya',
    },
    {
      key: 'night-owl',
      name: 'Night Owl',
      ownedPct: 41,
      colors: ['#c7d2fe', '#a5b4fc', '#4338ca'],
      unlocked: hadSleepSession,
      desc: 'Uyqu sessiyasi',
    },
    {
      key: 'pride',
      name: 'Pride',
      ownedPct: 23,
      colors: ['#fed7aa', '#fdba74', '#c2410c'],
      unlocked: (p?.streakDays ?? 0) >= 7,
      desc: '7 kunlik streak',
    },
    {
      key: 'iron-will',
      name: 'Iron Will',
      ownedPct: 12,
      colors: ['#e2e8f0', '#94a3b8', '#334155'],
      unlocked: (p?.totalSessions ?? 0) >= 50,
      desc: '50 sessiya',
    },
    {
      key: 'opal-plus',
      name: 'Opal Plus',
      ownedPct: 9,
      colors: ['#fde68a', '#fcd34d', '#92400e'],
      unlocked: p?.plan === 'PLUS',
      desc: 'Premium a’zo',
    },
    {
      key: 'time-lord',
      name: 'Time Lord',
      ownedPct: 7,
      colors: ['#bbf7d0', '#86efac', '#15803d'],
      unlocked: (p?.totalSavedMinutes ?? 0) >= 3000,
      desc: '50 soat tejash',
    },
    {
      key: 'century',
      name: 'Century',
      ownedPct: 3,
      colors: ['#fca5a5', '#f87171', '#991b1b'],
      unlocked: (p?.totalSessions ?? 0) >= 100,
      desc: '100 sessiya',
    },
  ]
}

/* ────────────────────────────────────────────────────────────
   JAHON FOIZI ("Top 17% WORLDWIDE")
   ──────────────────────────────────────────────────────────── */

/**
 * Haftalik tejalgan daqiqaga qarab "Top X%" (mock global taqsimot).
 * 516+ daqiqa/hafta → Top 17% kabi.
 */
export function worldwideTopPercent(weekSavedMinutes: number): number {
  return clamp(Math.round(60 - weekSavedMinutes / 12), 1, 87)
}

/** Sessiya tugallanganda moye ilova shakllari uchun */
export const SESSION_WITTY_LINES = [
  'Siz bu qoidani kecha o‘rnatgansiz. O‘tgan siz to‘g‘ri edi.',
  'Kelajakdagi sizga minnatdorchilik bildiradi.',
  'Bu ilova sizning eng yaxshi versiyangizga yo‘l to‘sqinlik qiladi.',
  'Bir oz tinchlanish — eng yaxshi tanlov.',
]
