import { NextResponse } from 'next/server'
import { db } from '@/lib/db'

/**
 * GET /api/export — barcha ma'lumotlarni JSON zaxira sifatida qaytaradi.
 * Profil ilovasidan "Ma'lumotlarni eksport qilish" orqali yuklab olinadi.
 */
export async function GET() {
  try {
    const [profile, apps, sessions, stats] = await Promise.all([
      db.userProfile.findFirst(),
      db.blockApp.findMany({ orderBy: { order: 'asc' } }),
      db.focusSession.findMany({ orderBy: { startedAt: 'desc' }, take: 1000 }),
      db.dailyStat.findMany({ orderBy: { date: 'asc' } }),
    ])

    const payload = {
      format: 'opal-backup',
      version: 1,
      exportedAt: new Date().toISOString(),
      profile,
      apps,
      sessions,
      dailyStats: stats,
      meta: {
        sessionCount: sessions.length,
        appCount: apps.length,
        daysTracked: stats.length,
        totalSavedMinutes: stats.reduce((a, d) => a + d.savedMinutes, 0),
      },
    }

    return new NextResponse(JSON.stringify(payload, null, 2), {
      status: 200,
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Disposition': `attachment; filename="opal-backup-${new Date()
          .toISOString()
          .slice(0, 10)}.json"`,
      },
    })
  } catch (err) {
    console.error('GET /api/export error:', err)
    return NextResponse.json({ error: 'Eksport bajarilmadi' }, { status: 500 })
  }
}
