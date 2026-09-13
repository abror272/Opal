'use client'

import { useEffect, useRef } from 'react'
import { toast } from 'sonner'
import { allRules, liveRuleStatus, ruleAppsLabel } from '@/lib/opal-ui'

const PRE_NOTIFY_KEY = 'opal:rule-prenotify-v2'

type NotifiedStore = { date: string; ids: string[] }

/** eski (v1) kalitni o'chirish — bir marta */
function cleanupLegacyKey() {
  try {
    window.localStorage.removeItem('opal:rule-prenotify')
  } catch {
    // ignore
  }
}

/** Kunlik o'tgan bildirishnomalarni localStorage'dan o'qish (reload'da spam yo'q) */
function loadNotified(): NotifiedStore {
  if (typeof window === 'undefined') return { date: '', ids: [] }
  try {
    const raw = window.localStorage.getItem(PRE_NOTIFY_KEY)
    if (!raw) return { date: '', ids: [] }
    const parsed = JSON.parse(raw) as NotifiedStore
    const today = new Date().toISOString().slice(0, 10)
    // kechagi yozuvlar bugungi kun uchun amali emas
    return parsed.date === today ? parsed : { date: today, ids: [] }
  } catch {
    return { date: new Date().toISOString().slice(0, 10), ids: [] }
  }
}

function saveNotified(store: NotifiedStore) {
  try {
    window.localStorage.setItem(PRE_NOTIFY_KEY, JSON.stringify(store))
  } catch {
    // localStorage band emas — sessiya darajasida ham ishlaydi
  }
}

/**
 * Oldindan ogohlantirish darajalari — qoida boshlanishidan OLDIN:
 *   15 daqiqa → reja qiling (🔔), 5 daqiqa → tez orada (⏳), 1 daqiqa → oxirgi eslatma (⏰)
 * Har daraja kuniga BIR MARTA (localStorage kaliti + tier suffiksi bilan).
 */
const PRE_NOTIFY_TIERS = [
  { at: 15, icon: '🔔', title: 'tez orada', verb: 'daqiqadan so‘ng boshlanadi' },
  { at: 5, icon: '⏳', title: 'tayyorlaning', verb: 'daqiqadan so‘ng boshlanadi' },
  { at: 1, icon: '⏰', title: '1 daqiqa qoldi', verb: 'daqiqadan so‘ng boshlanadi!' },
] as const

/**
 * QOIDA TRANSITION KUZATUVCHISI — ilova ochiq turganda qoida oynasi
 * boshlanganda/tugaganda jonli bildirishnoma chiqaradi.
 * (Home'dagi statik "aktiv" chipidan farqi: bu HOZIR bo'lgan o'zgarishni tutadi.)
 *
 * - 15s tick bilan barcha qoidalar baholanadi
 * - birinchi baholash faqat boshlang'ich holat sifatida yozib olinadi (spam yo'q)
 * - faqat parse qilinadigan vaqt oynasi bor qoidalar kuzatiladi
 * - QO'SHIMCHA: 15 / 5 / 1 daqiqalik oldindan ogohlantirish darajalari
 *   (har biri kuniga bir marta, localStorage bilan — reload'da qayta kelmaydi)
 */
export function RuleWatcher() {
  // ruleId → 'active' | 'upcoming'
  const prevRef = useRef<Map<string, 'active' | 'upcoming'> | null>(null)
  const notifiedRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    cleanupLegacyKey()
    const loaded = loadNotified()
    notifiedRef.current = new Set(loaded.ids)
    prevRef.current = null // localStorage'dan kelganda ham birinchi sikl snapshot bo'lsin

    const keyOf = (ruleId: string, tierAt: number) => `${ruleId}:${tierAt}`

    const markNotified = (key: string) => {
      notifiedRef.current.add(key)
      saveNotified({ date: new Date().toISOString().slice(0, 10), ids: [...notifiedRef.current] })
    }

    const evaluate = () => {
      const now = new Date()
      const prev = prevRef.current
      const next = new Map<string, 'active' | 'upcoming'>()

      for (const rule of allRules()) {
        const st = liveRuleStatus(rule, now)
        if (!st) continue // vaqt oynasi yo'q — kuzatilmaydi
        next.set(rule.id, st.state)

        if (!prev) {
          // birinchi sikl: allaqachon o'tib ketgan darajalarni jim belgilaymiz
          // (toast yo'q — lekin sahifa ochilgandan keyin boshlansa 'active' tosti keladi)
          if (st.state === 'upcoming' && st.minutes >= 1) {
            for (const tier of PRE_NOTIFY_TIERS) {
              if (st.minutes <= tier.at) markNotified(keyOf(rule.id, tier.at))
            }
          }
          continue
        }

        // ── 15 / 5 / 1 DAQIQALIK OLDINDAN OGohlantirishlar ──
        if (st.state === 'upcoming' && st.minutes >= 1) {
          for (const tier of PRE_NOTIFY_TIERS) {
            const key = keyOf(rule.id, tier.at)
            if (st.minutes <= tier.at && !notifiedRef.current.has(key)) {
              markNotified(key)
              toast(`${rule.icon} ${rule.title} — ${tier.title}`, {
                description: `≈${tier.at} ${tier.verb} · ${ruleAppsLabel(rule)}`,
                icon: tier.icon,
                duration: tier.at <= 5 ? 7000 : 6000,
              })
            }
          }
        }

        const before = prev.get(rule.id)
        if (before === st.state) continue

        if (st.state === 'active') {
          // boshlandi — oldindan ogohlantirishlar endi kerak emas
          for (const tier of PRE_NOTIFY_TIERS) markNotified(keyOf(rule.id, tier.at))
          toast(`${rule.icon} ${rule.title} boshlandi`, {
            description: `${ruleAppsLabel(rule)} · ${st.label} qoldi`,
            icon: '🔒',
            duration: 6000,
          })
        } else if (before === 'active') {
          // tugadi
          toast(`${rule.icon} ${rule.title} tugadi`, {
            description: 'Ilovalar qayta ochildi — davom etishingiz mumkin',
            icon: '🔓',
            duration: 6000,
          })
        }
      }
      prevRef.current = next
    }

    evaluate() // boshlang'ich snapshot (bildirishnomasiz)
    const id = setInterval(evaluate, 15_000)
    return () => clearInterval(id)
  }, [])

  return null //faqat tomon-effekt komponenti
}
