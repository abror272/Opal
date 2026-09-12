'use client'

import { useOpalStore } from '@/lib/opal-store'
import type { SessionType } from '@/lib/opal-types'

/**
 * Real sessiya yaratadi (DB) va uni aktiv sessiya sifatida o'rnatadi.
 * Home CTA va FocusTab ikkalasi ham shundan foydalanadi —
 * shunda sessionId har doim DB'da mavjud bo'ladi (PATCH 404 oldini oladi).
 */
export async function createAndStartSession(opts: {
  type: SessionType
  label: string
  emoji: string
  durationMinutes: number
  strict?: boolean
  /** false bo'lsa ilovalar bloklanmaydi (oddiy taymer) */
  includeBlocked?: boolean
}): Promise<{ ok: boolean; blockedCount: number }> {
  const { startSession } = useOpalStore.getState()

  const res = await fetch('/api/sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      type: opts.type,
      label: opts.label,
      emoji: opts.emoji,
      durationMinutes: opts.durationMinutes,
    }),
  })
  if (!res.ok) throw new Error('Sessiya yaratish bajarilmadi')
  const created = (await res.json()) as { id: string }

  let blockedApps: string[] = []
  if (opts.includeBlocked !== false) {
    try {
      const appsRes = await fetch('/api/apps?blocked=1')
      if (appsRes.ok) {
        const apps = (await appsRes.json()) as { name: string }[]
        blockedApps = apps.map((a) => a.name)
      }
    } catch {
      // bloklangan ilovalar ro'yxati ixtiyoriy
    }
  }

  startSession({
    sessionId: created.id,
    type: opts.type,
    label: opts.label,
    emoji: opts.emoji,
    durationMinutes: opts.durationMinutes,
    startedAt: Date.now(),
    blockedApps,
    strict: opts.strict ?? false,
  })

  return { ok: true, blockedCount: blockedApps.length }
}
