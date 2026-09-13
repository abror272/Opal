import { NextRequest, NextResponse } from 'next/server'
import { db } from '@/lib/db'

function todayISO(): string {
  return new Date().toISOString().slice(0, 10)
}

export async function GET(request: NextRequest) {
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

    // ?days=N — ko'proq kun (heatmap uchun 35+). Standart: 7 (eski shakl saqlanadi)
    const want = Math.min(Math.max(Math.round(Number(request.nextUrl.searchParams.get('days')) || 7), 7), 60)

    const all = await db.dailyStat.findMany({ orderBy: { date: 'asc' } })
    const days = all.slice(-want)
    const last7 = all.slice(-7)

    // hafta metrikalari HAR DOIM oxirgi 7 kundan (days param'ga bog'lanmaydi —
    // eski mijozlar shakli o'zgarmas)
    const weekSavedMinutes = last7.reduce((acc, d) => acc + d.savedMinutes, 0)
    const weekScreenMinutes = last7.reduce((acc, d) => acc + d.screenTimeMinutes, 0)
    const avgDailyScreenMinutes = last7.length ? Math.round(weekScreenMinutes / last7.length) : 0

    // trend: last 3 days avg vs previous 3 days avg (screen time) — oxirgi 7 kun ichida
    const last3 = last7.slice(-3)
    const prev3 = last7.slice(-6, -3)
    const avg = (arr: typeof last7) => (arr.length ? arr.reduce((a, d) => a + d.screenTimeMinutes, 0) / arr.length : 0)
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
