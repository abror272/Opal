import { NextRequest, NextResponse } from 'next/server'
import { db } from '@/lib/db'

const LABELS: Record<string, string> = {
  DEEP_FOCUS: 'Deep Focus',
  WORK: 'Ish rejimi',
  STUDY: 'O‘qish',
  SLEEP: 'Uyqu rejimi',
  CUSTOM: 'Maxsus',
}

export async function GET() {
  try {
    const sessions = await db.focusSession.findMany({
      orderBy: { startedAt: 'desc' },
      take: 20,
    })
    return NextResponse.json(sessions)
  } catch (err) {
    console.error('GET /api/sessions error:', err)
    return NextResponse.json({ error: 'Sessiyalarni olish bajarilmadi' }, { status: 500 })
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const type: string = typeof body.type === 'string' ? body.type : 'CUSTOM'
    const durationMinutes = Math.min(Math.max(Math.round(Number(body.durationMinutes) || 25), 1), 720)

    const session = await db.focusSession.create({
      data: {
        type,
        label: LABELS[type] ?? 'Maxsus',
        emoji: typeof body.emoji === 'string' && body.emoji ? body.emoji : '⚡',
        durationMinutes,
      },
    })
    return NextResponse.json(session, { status: 201 })
  } catch (err) {
    console.error('POST /api/sessions error:', err)
    return NextResponse.json({ error: 'Sessiya yaratish bajarilmadi' }, { status: 500 })
  }
}

export async function PATCH(request: NextRequest) {
  try {
    const body = await request.json()
    const id: string | undefined = body.id
    if (!id) return NextResponse.json({ error: 'id kerak' }, { status: 400 })

    const existing = await db.focusSession.findUnique({ where: { id } })
    if (!existing) return NextResponse.json({ error: 'Sessiya topilmadi' }, { status: 404 })
    if (existing.completed) {
      // rescore: foydalanuvchi sessiya tugagach FOKUS BAHOSINI kiritishi mumkin
      // (focus-rating kartasi — real focusScore, taxminiy emas)
      if (body.rescore === true) {
        const focusScore = Math.min(
          Math.max(Math.round(Number(body.focusScore) || existing.focusScore), 0),
          100
        )
        const rescored = await db.focusSession.update({
          where: { id },
          data: { focusScore },
        })
        return NextResponse.json(rescored)
      }
      return NextResponse.json(existing)
    }

    const early = body.early === true
    const now = new Date()
    const elapsedMinutes = Math.max(
      Math.min(Math.round((now.getTime() - existing.startedAt.getTime()) / 60000), existing.durationMinutes),
      0
    )
    const savedMinutes = early ? Math.max(elapsedMinutes - 2, 0) : existing.durationMinutes
    const focusScore = Math.min(Math.max(Math.round(Number(body.focusScore) || 70), 0), 100)

    const session = await db.focusSession.update({
      where: { id },
      data: {
        completed: true,
        endedAt: now,
        savedMinutes,
        focusScore,
      },
    })

    // update profile totals
    const profile = await db.userProfile.findFirst()
    if (profile) {
      await db.userProfile.update({
        where: { id: profile.id },
        data: {
          totalSessions: { increment: 1 },
          totalSavedMinutes: { increment: savedMinutes },
          streakDays: early ? Math.max(profile.streakDays - 1, 0) : { increment: 1 },
        },
      })
    }

    // update today's stat
    const todayISO = new Date().toISOString().slice(0, 10)
    const stat = await db.dailyStat.findUnique({ where: { date: todayISO } })
    if (stat) {
      await db.dailyStat.update({
        where: { id: stat.id },
        data: { savedMinutes: { increment: savedMinutes } },
      })
    }

    return NextResponse.json(session)
  } catch (err) {
    console.error('PATCH /api/sessions error:', err)
    return NextResponse.json({ error: 'Sessiyani yangilash bajarilmadi' }, { status: 500 })
  }
}
