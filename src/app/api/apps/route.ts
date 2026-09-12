import { NextRequest, NextResponse } from 'next/server'
import { db } from '@/lib/db'

export async function GET(request: NextRequest) {
  try {
    const blockedOnly = request.nextUrl.searchParams.get('blocked') === '1'
    const apps = await db.blockApp.findMany({
      where: blockedOnly ? { blocked: true } : undefined,
      orderBy: { order: 'asc' },
    })
    return NextResponse.json(apps)
  } catch (err) {
    console.error('GET /api/apps error:', err)
    return NextResponse.json({ error: 'Ilovalarni olish bajarilmadi' }, { status: 500 })
  }
}

export async function PATCH(request: NextRequest) {
  try {
    const body = await request.json()
    const id: string | undefined = body.id
    if (!id) return NextResponse.json({ error: 'id kerak' }, { status: 400 })

    const data: { blocked?: boolean; dailyLimitMinutes?: number } = {}
    if (typeof body.blocked === 'boolean') data.blocked = body.blocked
    if (typeof body.dailyLimitMinutes === 'number' && body.dailyLimitMinutes >= 0) {
      data.dailyLimitMinutes = Math.min(Math.round(body.dailyLimitMinutes), 1440)
    }

    const app = await db.blockApp.update({ where: { id }, data })
    return NextResponse.json(app)
  } catch (err) {
    console.error('PATCH /api/apps error:', err)
    return NextResponse.json({ error: 'Yangilash bajarilmadi' }, { status: 500 })
  }
}
