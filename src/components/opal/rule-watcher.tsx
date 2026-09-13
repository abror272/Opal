'use client'

import { useEffect, useRef } from 'react'
import { toast } from 'sonner'
import { allRules, liveRuleStatus, ruleAppsLabel } from '@/lib/opal-ui'

/**
 * QOIDA TRANSITION KUZATUVCHISI — ilova ochiq turganda qoida oynasi
 * boshlanganda/tugaganda jonli bildirishnoma chiqaradi.
 * (Home'dagi statik "aktiv" chipidan farqi: bu HOZIR bo'lgan o'zgarishni tutadi.)
 *
 * - 15s tick bilan barcha qoidalar baholanadi
 * - birinchi baholash faqat boshlang'ich holat sifatida yozib olinadi (spam yo'q)
 * - faqat parse qilinadigan vaqt oynasi bor qoidalar kuzatiladi
 */
export function RuleWatcher() {
  // ruleId → 'active' | 'upcoming'
  const prevRef = useRef<Map<string, 'active' | 'upcoming'> | null>(null)

  useEffect(() => {
    const evaluate = () => {
      const now = new Date()
      const prev = prevRef.current
      const next = new Map<string, 'active' | 'upcoming'>()

      for (const rule of allRules()) {
        const st = liveRuleStatus(rule, now)
        if (!st) continue // vaqt oynasi yo'q — kuzatilmaydi
        next.set(rule.id, st.state)

        if (!prev) continue // birinchi sikl — faqat snapshot

        const before = prev.get(rule.id)
        if (before === st.state) continue

        if (st.state === 'active') {
          // boshlandi
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
