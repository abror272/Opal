import { NextRequest, NextResponse } from 'next/server'
import { db } from '@/lib/db'

async function getOrCreateProfile() {
  const existing = await db.userProfile.findFirst()
  if (existing) return existing
  return db.userProfile.create({ data: {} })
}

export async function GET() {
  try {
    const profile = await getOrCreateProfile()
    return NextResponse.json(profile)
  } catch (err) {
    console.error('GET /api/profile error:', err)
    return NextResponse.json({ error: 'Profilni olish bajarilmadi' }, { status: 500 })
  }
}

export async function PATCH(request: NextRequest) {
  try {
    const body = await request.json()
    const profile = await getOrCreateProfile()

    const data: {
      protectionEnabled?: boolean
      strictMode?: boolean
      goalMinutes?: number
      plan?: string
      name?: string
    } = {}

    if (typeof body.protectionEnabled === 'boolean') data.protectionEnabled = body.protectionEnabled
    if (typeof body.strictMode === 'boolean') data.strictMode = body.strictMode
    if (typeof body.goalMinutes === 'number' && body.goalMinutes >= 30) {
      data.goalMinutes = Math.min(Math.round(body.goalMinutes), 960)
    }
    if (body.plan === 'PLUS' || body.plan === 'FREE') data.plan = body.plan
    if (typeof body.name === 'string' && body.name.trim()) data.name = body.name.trim().slice(0, 40)

    const updated = await db.userProfile.update({ where: { id: profile.id }, data })
    return NextResponse.json(updated)
  } catch (err) {
    console.error('PATCH /api/profile error:', err)
    return NextResponse.json({ error: 'Profilni yangilash bajarilmadi' }, { status: 500 })
  }
}
