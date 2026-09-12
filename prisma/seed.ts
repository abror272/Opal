// Seed Opal Clone database
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

function dateStr(offsetDays: number): string {
  const d = new Date()
  d.setDate(d.getDate() - offsetDays)
  return d.toISOString().slice(0, 10)
}

async function main() {
  // wipe
  await prisma.focusSession.deleteMany()
  await prisma.dailyStat.deleteMany()
  await prisma.blockApp.deleteMany()
  await prisma.userProfile.deleteMany()

  // Apps
  const apps = [
    { name: 'TikTok', emoji: '🎵', gradient: 'from-slate-800 to-cyan-500', category: 'Ijtimoiy tarmoq', blocked: true, dailyLimitMinutes: 15, todayMinutes: 47, order: 1 },
    { name: 'Instagram', emoji: '📸', gradient: 'from-fuchsia-500 via-rose-500 to-amber-400', category: 'Ijtimoiy tarmoq', blocked: true, dailyLimitMinutes: 20, todayMinutes: 36, order: 2 },
    { name: 'YouTube', emoji: '▶️', gradient: 'from-red-600 to-rose-500', category: 'Zerikarli', blocked: false, dailyLimitMinutes: 45, todayMinutes: 58, order: 3 },
    { name: 'X (Twitter)', emoji: '𝕏', gradient: 'from-slate-900 to-slate-600', category: 'Ijtimoiy tarmoq', blocked: true, dailyLimitMinutes: 10, todayMinutes: 22, order: 4 },
    { name: 'Telegram', emoji: '✈️', gradient: 'from-sky-400 to-blue-500', category: 'Messenger', blocked: false, dailyLimitMinutes: 0, todayMinutes: 31, order: 5 },
    { name: 'Snapchat', emoji: '👻', gradient: 'from-yellow-300 to-amber-500', category: 'Ijtimoiy tarmoq', blocked: false, dailyLimitMinutes: 15, todayMinutes: 9, order: 6 },
    { name: 'Reddit', emoji: '👽', gradient: 'from-orange-500 to-red-500', category: 'Zerikarli', blocked: true, dailyLimitMinutes: 10, todayMinutes: 18, order: 7 },
    { name: 'Netflix', emoji: '🎬', gradient: 'from-red-700 to-red-500', category: 'Zerikarli', blocked: false, dailyLimitMinutes: 60, todayMinutes: 0, order: 8 },
    { name: 'Whisper', emoji: '🕹️', gradient: 'from-violet-500 to-purple-700', category: 'O‘yinlar', blocked: true, dailyLimitMinutes: 0, todayMinutes: 41, order: 9 },
    { name: 'Safari', emoji: '🧭', gradient: 'from-cyan-400 to-sky-500', category: 'Foydali', blocked: false, dailyLimitMinutes: 0, todayMinutes: 64, order: 10 },
  ]
  await prisma.blockApp.createMany({ data: apps })

  // Daily stats — last 7 days
  const stats = [
    { date: dateStr(6), screenTimeMinutes: 342, savedMinutes: 96, pickups: 74, goalMinutes: 240 },
    { date: dateStr(5), screenTimeMinutes: 296, savedMinutes: 128, pickups: 61, goalMinutes: 240 },
    { date: dateStr(4), screenTimeMinutes: 388, savedMinutes: 64, pickups: 88, goalMinutes: 240 },
    { date: dateStr(3), screenTimeMinutes: 231, savedMinutes: 156, pickups: 52, goalMinutes: 240 },
    { date: dateStr(2), screenTimeMinutes: 265, savedMinutes: 142, pickups: 58, goalMinutes: 240 },
    { date: dateStr(1), screenTimeMinutes: 204, savedMinutes: 181, pickups: 44, goalMinutes: 240 },
    { date: dateStr(0), screenTimeMinutes: 118, savedMinutes: 97, pickups: 26, goalMinutes: 240 },
  ]
  await prisma.dailyStat.createMany({ data: stats })

  // Past sessions
  const now = Date.now()
  const sessions = [
    { type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', durationMinutes: 45, startedAt: new Date(now - 5 * 3600_000), endedAt: new Date(now - 5 * 3600_000 + 45 * 60_000), completed: true, savedMinutes: 45, focusScore: 92, blockedApps: JSON.stringify(['TikTok', 'Instagram', 'X (Twitter)']) },
    { type: 'WORK', label: 'Ish rejimi', emoji: '💼', durationMinutes: 90, startedAt: new Date(now - 9 * 3600_000), endedAt: new Date(now - 9 * 3600_000 + 90 * 60_000), completed: true, savedMinutes: 90, focusScore: 87, blockedApps: JSON.stringify(['TikTok', 'Instagram', 'Reddit']) },
    { type: 'STUDY', label: 'O‘qish', emoji: '📚', durationMinutes: 60, startedAt: new Date(now - 26 * 3600_000), endedAt: new Date(now - 26 * 3600_000 + 60 * 60_000), completed: true, savedMinutes: 60, focusScore: 95, blockedApps: JSON.stringify(['TikTok', 'Whisper', 'Instagram']) },
    { type: 'DEEP_FOCUS', label: 'Deep Focus', emoji: '🧠', durationMinutes: 25, startedAt: new Date(now - 30 * 3600_000), endedAt: new Date(now - 30 * 3600_000 + 25 * 60_000), completed: true, savedMinutes: 25, focusScore: 78, blockedApps: JSON.stringify(['TikTok', 'X (Twitter)']) },
    { type: 'SLEEP', label: 'Uyqu rejimi', emoji: '🌙', durationMinutes: 480, startedAt: new Date(now - 52 * 3600_000), endedAt: new Date(now - 52 * 3600_000 + 480 * 60_000), completed: true, savedMinutes: 120, focusScore: 100, blockedApps: JSON.stringify(['TikTok', 'Instagram', 'YouTube', 'Reddit']) },
  ]
  await prisma.focusSession.createMany({ data: sessions })

  // Profile
  await prisma.userProfile.create({
    data: {
      name: 'Aziz',
      handle: '@aziz.dev',
      streakDays: 12,
      totalSavedMinutes: 3480,
      totalSessions: 87,
      protectionEnabled: true,
      strictMode: true,
      goalMinutes: 240,
      plan: 'FREE',
    },
  })

  console.log('✅ Seed complete')
}

main()
  .catch((e) => { console.error(e); process.exit(1) })
  .finally(() => prisma.$disconnect())
