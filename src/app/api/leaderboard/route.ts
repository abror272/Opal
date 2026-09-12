import { NextResponse } from 'next/server'
import { db } from '@/lib/db'

// Demo do'stlar ro'yxati (real ilovada — WebSocket bilan jonli bo'lardi)
const FRIENDS = [
  { name: 'Dilnoza', avatar: '🎨', savedMinutes: 1180, streak: 21 },
  { name: 'Jasur', avatar: '⚽', savedMinutes: 940, streak: 14 },
  { name: 'Malika', avatar: '📚', savedMinutes: 875, streak: 17 },
  { name: 'Bekzod', avatar: '🎧', savedMinutes: 620, streak: 9 },
  { name: 'Sardor', avatar: '💻', savedMinutes: 540, streak: 6 },
  { name: 'Nigora', avatar: '🌱', savedMinutes: 380, streak: 3 },
]

export async function GET() {
  try {
    const profile = await db.userProfile.findFirst()
    const stats = await db.dailyStat.findMany({ orderBy: { date: 'asc' }, take: 7 })

    const weekSaved = stats.reduce((acc, d) => acc + d.savedMinutes, 0)

    const entries = [
      ...FRIENDS.map((f) => ({ ...f, isMe: false })),
      {
        name: profile?.name ?? 'Siz',
        avatar: '⭐',
        savedMinutes: weekSaved,
        streak: profile?.streakDays ?? 0,
        isMe: true,
      },
    ].sort((a, b) => b.savedMinutes - a.savedMinutes)

    const maxSaved = Math.max(...entries.map((e) => e.savedMinutes), 1)

    return NextResponse.json({
      entries: entries.map((e, i) => ({ ...e, rank: i + 1, barPercent: Math.round((e.savedMinutes / maxSaved) * 100) })),
      myRank: entries.findIndex((e) => e.isMe) + 1,
      totalParticipants: entries.length,
    })
  } catch (err) {
    console.error('GET /api/leaderboard error:', err)
    return NextResponse.json({ error: 'Reytingni olish bajarilmadi' }, { status: 500 })
  }
}
