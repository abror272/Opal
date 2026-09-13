'use client'

import { useEffect, useRef } from 'react'
import { toast } from 'sonner'
import { allRules, liveRuleStatus, ruleAppsLabel } from '@/lib/opal-ui'

const PRE_NOTIFY_KEY = 'opal:rule-prenotify'

type NotifiedStore = { date: string; ids: string[] }

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
 * QOIDA TRANSITION KUZATUVCHISI — ilova ochiq turganda qoida oynasi
 * boshlanganda/tugaganda jonli bildirishnoma chiqaradi.
 * (Home'dagi statik "aktiv" chipidan farqi: bu HOZIR bo'lgan o'zgarishni tutadi.)
 *
 * - 15s tick bilan barcha qoidalar baholanadi
 * - birinchi baholash faqat boshlang'ich holat sifatida yozib olinadi (spam yo'q)
 * - faqat parse qilinadigan vaqt oynasi bor qoidalar kuzatiladi
 * - QO'SHIMCHA: boshlanishiga ≤5 daqiqa qolganda BIR MARTA "tez orada" ogohlantirishi
 *   (kuniga bir marta, localStorage bilan — reload'da qayta kelmaydi)
 */
export function RuleWatcher() {
  // ruleId → 'active' | 'upcoming'
  const prevRef = useRef<Map<string, 'active' | 'upcoming'> | null>(null)
  const notifiedRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    const loaded = loadNotified()
    notifiedRef.current = new Set(loaded.ids)
    prevRef.current = null // localStorage'dan kelganda ham birinchi sikl snapshot bo'lsin

    const preNotify = (ruleId: string) => {
      notifiedRef.current.add(ruleId)
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
          // birinchi sikl: hozir 5 daqiqa ichida boshlanadigan qoidani belgilaymiz
          // (toast yo'q — lekin sahifa ochilgandan keyin boshlansa 'active' tosti keladi)
          if (st.state === 'upcoming' && st.minutes >= 1 && st.minutes <= 5) preNotify(rule.id)
          continue
        }

        // ── 5 DAQIQALIK OLDINDAN OGohlantirish ──
        if (
          st.state === 'upcoming' &&
          st.minutes >= 1 &&
          st.minutes <= 5 &&
          !notifiedRef.current.has(rule.id)
        ) {
          preNotify(rule.id)
          toast(`${rule.icon} ${rule.title} — tez orada`, {
            description: `≈${st.minutes} daqiqadan so'ng boshlanadi · ${ruleAppsLabel(rule)}`,
            icon: '⏳',
            duration: 7000,
          })
        }

        const before = prev.get(rule.id)
        if (before === st.state) continue

        if (st.state === 'active') {
          // boshlandi
          preNotify(rule.id) // boshlandi — endi oldindan ogohlantirish kerak emas
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
