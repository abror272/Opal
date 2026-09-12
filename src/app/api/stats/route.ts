import { NextResponse } from 'next/server'
import { db } from '@/lib/db'

function todayISO(): string {
  return new Date().toISOString().slice(0, 10)
}

export async function GET() {
  try {
    const profile = await db.userProfile.findFirst()
    const goalMinutes = profile?.goalMinutes ?? 240

    // ensure today's stat exists (upsert style)
    const today = await db.dailyStat.upsert({
      where: { date: todayISO() },
      update: {},
      create: {
        date: todayISO(),
        screenTimeMinutes: 0,
        savedMinutes: 0,
        pickups: 0,
        goalMinutes,
      },
    })

    const all = await db.dailyStat.findMany({
      orderBy: { date: 'asc' },
      take: 7,
    })

    // backfill to always have 7 days
    let days = all
    if (all.length < 7) {
      days = all
    }

    const weekSavedMinutes = days.reduce((acc, d) => acc + d.savedMinutes, 0)
    const weekScreenMinutes = days.reduce((acc, d) => acc + d.screenTimeMinutes, 0)
    const avgDailyScreenMinutes = days.length ? Math.round(weekScreenMinutes / days.length) : 0

    // trend: last 3 days avg vs previous 3 days avg (screen time)
    const last3 = days.slice(-3)
    const prev3 = days.slice(-6, -3)
    const avg = (arr: typeof days) => (arr.length ? arr.reduce((a, d) => a + d.screenTimeMinutes, 0) / arr.length : 0)
    const a1 = avg(last3)
    const a0 = avg(prev3)
    const trendPercent = a0 > 0 ? Math.round(((a1 - a0) / a0) * 100) : 0

    return NextResponse.json({
      days: [...days.slice(0, -1), today],
      today,
      weekSavedMinutes,
      weekScreenMinutes,
      avgDailyScreenMinutes,
      trendPercent,
      goalMinutes,
    })
  } catch (err) {
    console.error('GET /api/stats error:', err)
    return NextResponse.json({ error: 'Statistikani olish bajarilmadi' }, { status: 500 })
  }
}
