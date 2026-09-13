// Umumiy UI yordamchilari — haqiqiy Opal (Apple Design Award 2025) dizayn tili
// Referens: foydalanuvchi yuklagan haqiqiy Opal skrinshotlari (Mobbin)

import { sessionFullyCompleted } from '@/lib/opal-types'
import type { FocusSession, StatsResponse, SessionType } from '@/lib/opal-types'

/* ────────────────────────────────────────────────────────────
   UYQU KUZATUVI — haqiqiy SLEEP sessiyalaridan oxirgi kechagi
   uyquni chiqarib oladi (Sleep Score endi real ma'lumot)
   ──────────────────────────────────────────────────────────── */

export interface SleepRecord {
  /** uyqu davomiyligi (daqiqada, 12 soatga kesilgan) */
  minutes: number
  startedAt: Date
  endedAt: Date
  completed: boolean
}

/**
 * Oxirgi kechagi uyqu: 32 soat ichida tugagan SLEEP sessiyasi.
 * sort: endedAt bo'yicha eng yangisi.
 */
export function lastSleep(
  sessions: FocusSession[] | undefined,
  now: Date = new Date()
): SleepRecord | null {
  const cands = (sessions ?? [])
    .filter((s) => s.type === 'SLEEP' && s.endedAt)
    .sort((a, b) => new Date(b.endedAt!).getTime() - new Date(a.endedAt!).getTime())
  for (const s of cands) {
    const end = new Date(s.endedAt!).getTime()
    if (now.getTime() - end <= 32 * 3600_000) {
      const start = new Date(s.startedAt).getTime()
      return {
        minutes: clamp(Math.round((end - start) / 60_000), 0, 12 * 60),
        startedAt: new Date(s.startedAt),
        endedAt: new Date(end),
        completed: s.completed,
      }
    }
  }
  return null
}

/** Uyqu davomiyligidan Sleep Score (7h30 ≈ 89, 8h ≈ 92) */
export function sleepScoreFromMinutes(minutes: number): number {
  return clamp(Math.round(40 + minutes * 0.108), 40, 97)
}

/** HH:mm ko'rinishi (yotish/uyg'onish vaqtlari uchun) */
export function clockTime(d: Date): string {
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

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
  // faqat REJALASHTIRILGAN davomiylikka yetgan sessiyalar to'liq hisoblanadi
  // (erta chiqish `completed: true` bilan DB'ga yoziladi)
  const completed = todaysSessions.filter(sessionFullyCompleted)

  const over = Math.max(screen - goal, 0)
  const score = Math.round(clamp(88 - over * 0.1 + saved * 0.18, 42, 99))
  const focus = Math.round(clamp(52 + completed.length * 11 + saved * 0.09, 38, 98))
  const rest = Math.round(clamp(95 - screen * 0.1, 44, 97))
  const sleepRec = lastSleep(sessions)
  const sleep = sleepRec
    ? sleepScoreFromMinutes(sleepRec.minutes)
    : Math.round(clamp(80 - screen * 0.04, 55, 82))

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
   RULES REAL-TIME SCHEDULER — qoidalar endi jonli baholanadi:
   "9AM — 5PM" kabi oynalar parse qilinadi, aktiv/qolgan vaqt
   hisoblanadi va ilova bo'ylab ko'rsatiladi.
   ──────────────────────────────────────────────────────────── */

/** Parse qilingan kunlik vaqt oynasi (daqiqalar, 0..1440) */
export interface RuleWindow {
  startMin: number
  endMin: number
}

/**
 * "9AM — 5PM" / "10PM—8AM" / "12—1PM" / "6pm - 8pm" / "9:00-17:00"
 * hamda YIQIQ formatlar ("755AM", "831am", "1730", "930-1045")
 * ni kunlik daqiqalar oynasiga aylantiradi.
 * Mos kelmasa (masalan "Har kuni") null qaytaradi.
 */
export function parseRuleWindow(time: string): RuleWindow | null {
  if (!time) return null
  const t = time
    .toLowerCase()
    .replace(/[–—−]/g, '-')
    // yiqiq soat: "755am"/"1730"/"930" → "7:55am"/"17:30"/"9:30"
    // (faqat 3-4 xonali ketma-ket raqamlar; "12—1PM" kabi 1-2 xonalarga tegmaydi)
    .replace(/\b(\d{1,2})(\d{2})\s*(am|pm)?\b/g, '$1:$2$3')
  if (!/\d/.test(t)) return null

  const re = /(\d{1,2})(?::(\d{2}))?\s*(am|pm)?/g
  const tokens: { hour: number; min: number; suffix: boolean }[] = []
  let m: RegExpExecArray | null
  while ((m = re.exec(t)) !== null) {
    const hour = parseInt(m[1], 10)
    const min = m[2] ? parseInt(m[2], 10) : 0
    if (min > 59) return null
    const suffix = m[3]
    let h = hour
    if (suffix === 'pm' && h !== 12) h += 12
    if (suffix === 'am' && h === 12) h = 0
    if (h > 23) return null
    tokens.push({ hour: h, min, suffix: !!suffix })
  }
  if (tokens.length < 2) return null
  // koeffitsiyentsiz 24-soat format ham bo'lishi mumkin (9:00-17:00)
  const [a, b] = tokens
  return { startMin: a.hour * 60 + a.min, endMin: b.hour * 60 + b.min }
}

export type RuleState = 'active' | 'upcoming'

export interface RuleStatusInfo {
  state: RuleState
  /** "4s 32d" ko'rinishida qolgan/keladigan vaqt */
  label: string
  /** xom daqiqa: aktiv bo'lsa qolgan, upcoming bo'lsa boshlanishiga (bildirishnomalar uchun) */
  minutes: number
}

/** Qoida oynasining hozirgi holati: aktiv (qolgan vaqt) yoki kutilmoqda (boshlanishiga) */
export function ruleWindowStatus(win: RuleWindow, now: Date = new Date()): RuleStatusInfo | null {
  const nowMin = now.getHours() * 60 + now.getMinutes()
  const crosses = win.endMin <= win.startMin
  const isActive = crosses
    ? nowMin >= win.startMin || nowMin < win.endMin
    : nowMin >= win.startMin && nowMin < win.endMin

  if (isActive) {
    const endAbs = crosses && nowMin < win.endMin ? win.endMin : win.endMin + (crosses ? 1440 : 0)
    const left = Math.max(endAbs - nowMin, 1)
    return { state: 'active', label: formatMinutesShort(left), minutes: left }
  }
  // boshlanishigacha (keyingi sikl)
  const until = nowMin < win.startMin ? win.startMin - nowMin : win.startMin + 1440 - nowMin
  return { state: 'upcoming', label: formatMinutesShort(until), minutes: until }
}

/** "4s 32d" uslubidagi qisqa vaqt (formatMinutes bilan bir xil, aniqroq nom) */
function formatMinutesShort(min: number): string {
  const h = Math.floor(min / 60)
  const m = Math.round(min % 60)
  if (h <= 0) return `${m}d`
  if (m === 0) return `${h}s`
  return `${h}s ${m}d`
}

/* ── RuleCard (Apps tab bento) — endi opal-ui'da, Home ham o'qiydi ── */
export interface RuleCard {
  id: string
  title: string
  time: string
  sub: string
  photo?: string
  gradient: string
  icon: string
  duration: number
  type: SessionType
  label: string
  emoji: string
  left?: string
  /** parse qilingan oyna (custom qoidalar uchun saqlanadi) */
  startMin?: number
  endMin?: number
  /** qoida qaysi ilovalarga taalluqli (yo'q = hammasi / sub'dagi matn) */
  apps?: string[]
}

/**
 * Qoida ilovalari yorlig'i: "Block All" yoki "TikTok, IG +2" ko'rinishida.
 * Kartaning `sub` qatorida ko'rsatiladi.
 */
export function ruleAppsLabel(rule: Pick<RuleCard, 'apps' | 'sub'>): string {
  const apps = rule.apps
  if (!apps || apps.length === 0) return rule.sub
  if (apps.length === 1) return apps[0]
  if (apps.length === 2) return apps.join(', ')
  return `${apps[0]}, ${shortAppName(apps[1])} +${apps.length - 2}`
}

/** uzun ilova nomini qisqartirish ("X (Twitter)" → "X") */
function shortAppName(name: string): string {
  return name.split(' ')[0]
}

export const DEFAULT_RULES: RuleCard[] = [
  {
    id: 'unblock-daily',
    title: '10 Unblock Daily',
    time: 'Har kuni',
    sub: 'Ijtimoiy ilovalar uchun',
    gradient: 'from-[#2a3b5c] to-[#141c30]',
    icon: '🔓',
    duration: 30,
    type: 'CUSTOM',
    label: 'Unblock Daily',
    emoji: '🔓',
    left: '7 left',
    apps: ['TikTok', 'Instagram', 'X (Twitter)', 'Reddit', 'Whisper'],
  },
  {
    id: 'sleep-time',
    title: 'Sleep Time',
    time: '10PM — 8AM',
    sub: 'Block All',
    photo: '/opal/routine-sleep.jpg',
    gradient: 'from-[#1a1f3d] to-[#0a0d20]',
    icon: '🌙',
    duration: 480,
    type: 'SLEEP',
    label: 'Uyqu rejimi',
    emoji: '🌙',
  },
  {
    id: 'deep-work',
    title: 'Deep Work',
    time: '9AM — 5PM',
    sub: 'Block All, Except Productivity',
    photo: '/opal/routine-deepwork.jpg',
    gradient: 'from-[#26221c] to-[#0f0d0a]',
    icon: '💻',
    duration: 90,
    type: 'WORK',
    label: 'Ish rejimi',
    emoji: '💼',
    apps: ['TikTok', 'Instagram', 'X (Twitter)', 'Reddit', 'Whisper', 'Netflix'],
  },
  {
    id: 'lunch-break',
    title: 'Lunch Break',
    time: '12—1PM',
    sub: 'Unblock Snapchat if blocked',
    photo: '/opal/routine-family.jpg',
    gradient: 'from-[#1c2626] to-[#0a1010]',
    icon: '🍽️',
    duration: 60,
    type: 'STUDY',
    label: 'O‘qish',
    emoji: '📚',
    apps: ['Snapchat'],
  },
  {
    id: 'evening-off',
    title: '10PM—8AM',
    time: 'Tungi himoya',
    sub: 'Block Social',
    gradient: 'from-[#241c33] to-[#0d0a14]',
    icon: '🛡️',
    duration: 120,
    type: 'CUSTOM',
    label: 'Tungi tinchlik',
    emoji: '🛡️',
    left: '7 left',
    apps: ['TikTok', 'Instagram', 'X (Twitter)', 'Reddit'],
  },
]

/* ────────────────────────────────────────────────────────────
   HAFTALIK HISOBOT — haqiqiy Opal "Weekly Report" kartasi
   ──────────────────────────────────────────────────────────── */

/** "8–14 Sentabr" ko'rinishida hafta oraliqi (oxirgi 7 kun) */
export function weekRangeLabel(now: Date = new Date()): string {
  const months = [
    'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'Iyun',
    'Iyul', 'Avgust', 'Sentabr', 'Oktabr', 'Noyabr', 'Dekabr',
  ]
  const end = new Date(now)
  const start = new Date(now)
  start.setDate(start.getDate() - 6)
  const sameMonth = start.getMonth() === end.getMonth()
  return sameMonth
    ? `${start.getDate()}–${end.getDate()} ${months[end.getMonth()]}`
    : `${start.getDate()} ${months[start.getMonth()]} – ${end.getDate()} ${months[end.getMonth()]}`
}

export interface WeekDayBar {
  /** "Du", "Se"... */
  label: string
  /** to'liq kun nomi ("Dushanba") */
  full: string
  saved: number
  screen: number
  /** bugunmi? */
  isToday: boolean
  dateISO: string
}

/** stats.days'dan oxirgi 7 kun bar ma'lumotlari (eng chapda eng eski) */
export function weekSavedSeries(days: DailyStatLike[] | undefined, now: Date = new Date()): WeekDayBar[] {
  const shorts = ['Ya', 'Du', 'Se', 'Cho', 'Pa', 'Ju', 'Sha']
  const fulls = ['Yakshanba', 'Dushanba', 'Seshanba', 'Chorshanba', 'Payshanba', 'Juma', 'Shanba']
  const byDate = new Map((days ?? []).map((d) => [d.date, d]))
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(now)
    d.setDate(d.getDate() - (6 - i))
    const iso = d.toISOString().slice(0, 10)
    const stat = byDate.get(iso)
    return {
      label: shorts[d.getDay()],
      full: fulls[d.getDay()],
      saved: stat?.savedMinutes ?? 0,
      screen: stat?.screenTimeMinutes ?? 0,
      isToday: i === 6,
      dateISO: iso,
    }
  })
}

/** stats.days'ga minimal interfeys (import tsiklini oldini olish uchun) */
export interface DailyStatLike {
  date: string
  savedMinutes: number
  screenTimeMinutes: number
}

export const CUSTOM_RULES_KEY = 'opal-custom-rules'

/** localStorage'dagi custom qoidalarni o'qish (SSR-safe, xatosiz) */
export function readCustomRules(): RuleCard[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(CUSTOM_RULES_KEY)
    return raw ? (JSON.parse(raw) as RuleCard[]) : []
  } catch {
    return []
  }
}

/** Barcha qoidalar (standart + custom) */
export function allRules(): RuleCard[] {
  return [...DEFAULT_RULES, ...readCustomRules()]
}

/** Qoidaning jonli holati: oyna bo'lmasa null (statik `left` ishlatiladi) */
export function liveRuleStatus(rule: RuleCard, now: Date = new Date()): RuleStatusInfo | null {
  const win =
    rule.startMin !== undefined && rule.endMin !== undefined
      ? { startMin: rule.startMin, endMin: rule.endMin }
      : parseRuleWindow(rule.time)
  return win ? ruleWindowStatus(win, now) : null
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
