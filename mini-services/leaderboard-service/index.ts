// Opal clone — Live friends leaderboard service (socket.io, port 3003)
// Simulates demo friends' focus activity and broadcasts real-time leaderboard state.
import { createServer } from 'http'
import { Server } from 'socket.io'

const PORT = 3003

const httpServer = createServer()
const io = new Server(httpServer, {
  // DO NOT change the path, it is used by Caddy to forward the request to the correct port
  path: '/',
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
  },
  pingTimeout: 60000,
  pingInterval: 25000,
})

// ── Types ────────────────────────────────────────────────
interface Friend {
  id: string
  name: string
  avatar: string
  savedMinutes: number
  streak: number
  focusing: boolean
}

interface Player {
  socketId: string
  name: string
  avatar: string
  savedMinutes: number
  streak: number
}

interface BoardEntry {
  id: string
  name: string
  avatar: string
  savedMinutes: number
  streak: number
  focusing: boolean
  isMe: boolean
  rank: number
  barPercent: number
}

interface ActivityEvent {
  id: string
  name: string
  avatar: string
  text: string
  emoji: string
  ts: number
}

// ── Demo friends state ───────────────────────────────────
const friends: Friend[] = [
  { id: 'f1', name: 'Malika', avatar: '👩‍💻', savedMinutes: 262, streak: 18, focusing: true },
  { id: 'f2', name: 'Jasur', avatar: '🧑‍🎓', savedMinutes: 214, streak: 9, focusing: false },
  { id: 'f3', name: 'Dilnoza', avatar: '👩‍🎨', savedMinutes: 189, streak: 14, focusing: true },
  { id: 'f4', name: 'Bekzod', avatar: '🧑‍💼', savedMinutes: 156, streak: 5, focusing: false },
  { id: 'f5', name: 'Kamola', avatar: '👩‍🔬', savedMinutes: 132, streak: 21, focusing: false },
  { id: 'f6', name: 'Otabek', avatar: '🧑‍🚀', savedMinutes: 87, streak: 3, focusing: false },
]

const MAX_FRIENDS = 12

const players = new Map<string, Player>()
let activitySeq = 0

const ACTIVITIES: { text: (n: string) => string; emoji: string }[] = [
  { text: (n) => `${n} fokus sessiyasini boshladi`, emoji: '🚀' },
  { text: (n) => `${n} sessiyani tugatdi — ${20 + Math.floor(Math.random() * 70)} daqiqa tejaldi`, emoji: '🎉' },
  { text: (n) => `${n} Instagram’ni 2 soat blokladi`, emoji: '🛡️' },
  { text: (n) => `${n} kunlik streakni oshirdi`, emoji: '🔥' },
  { text: (n) => `${n} TikTok’dan 15 daqiqa voz kechdi`, emoji: '💪' },
  { text: (n) => `${n} uyqu rejimini yoqdi`, emoji: '🌙' },
]

function pickActivity(): ActivityEvent {
  const f = friends[Math.floor(Math.random() * friends.length)]
  const a = ACTIVITIES[Math.floor(Math.random() * ACTIVITIES.length)]
  activitySeq += 1
  return {
    id: `act-${activitySeq}`,
    name: f.name,
    avatar: f.avatar,
    text: a.text(f.name),
    emoji: a.emoji,
    ts: Date.now(),
  }
}

function buildBoard(): { entries: BoardEntry[]; online: number } {
  type Row = Omit<BoardEntry, 'rank' | 'barPercent'>
  const rows: Row[] = [
    ...friends.map((f) => ({
      id: f.id,
      name: f.name,
      avatar: f.avatar,
      savedMinutes: f.savedMinutes,
      streak: f.streak,
      focusing: f.focusing,
      isMe: false,
    })),
    ...[...players.values()].map((p) => ({
      id: p.socketId,
      name: p.name,
      avatar: p.avatar,
      savedMinutes: p.savedMinutes,
      streak: p.streak,
      focusing: true,
      isMe: true,
    })),
  ]
  rows.sort((a, b) => b.savedMinutes - a.savedMinutes)
  const max = Math.max(...rows.map((r) => r.savedMinutes), 1)
  const entries = rows.map((r, i) => ({
    ...r,
    rank: i + 1,
    barPercent: Math.max(Math.round((r.savedMinutes / max) * 100), 8),
  }))
  return { entries, online: players.size }
}

function broadcastState() {
  io.emit('leaderboard:state', buildBoard())
}

// ── Simulation tick ──────────────────────────────────────
const TICK_MS = 7000
setInterval(() => {
  // every friend has a chance to gain minutes / toggle focusing
  for (const f of friends) {
    const gain = Math.random()
    if (gain < 0.55) {
      f.savedMinutes += Math.floor(Math.random() * 9) + 2
      if (!f.focusing && Math.random() < 0.35) f.focusing = true
    } else if (f.focusing && Math.random() < 0.4) {
      f.focusing = false
    }
  }
  // occasional streak bump
  if (Math.random() < 0.2) {
    const f = friends[Math.floor(Math.random() * friends.length)]
    f.streak += 1
  }
  broadcastState()
  if (Math.random() < 0.75) {
    io.emit('friend:activity', pickActivity())
  }
}, TICK_MS)

// ── Connection handling ──────────────────────────────────
io.on('connection', (socket) => {
  console.log(`[leaderboard] connected: ${socket.id}`)

  socket.on('leaderboard:join', (data: { name: string; savedMinutes: number; streak: number }) => {
    players.set(socket.id, {
      socketId: socket.id,
      name: typeof data?.name === 'string' && data.name.trim() ? data.name.trim() : 'Siz',
      avatar: '🛡️',
      savedMinutes: Number.isFinite(data?.savedMinutes) ? Math.max(0, data.savedMinutes) : 0,
      streak: Number.isFinite(data?.streak) ? Math.max(0, data.streak) : 0,
    })
    socket.emit('leaderboard:state', buildBoard())
    broadcastState()
    console.log(`[leaderboard] joined: ${players.get(socket.id)?.name}`)
  })

  // client updates its saved minutes (e.g. after finishing a session)
  socket.on('leaderboard:sync', (data: { savedMinutes: number }) => {
    const p = players.get(socket.id)
    if (p && Number.isFinite(data?.savedMinutes)) {
      p.savedMinutes = Math.max(0, data.savedMinutes)
      broadcastState()
    }
  })

  // add a friend to the board (ack-based so the client can show errors)
  socket.on(
    'friend:add',
    (
      data: { name: string; avatar: string },
      ack: (res: { ok: boolean; error?: string }) => void
    ) => {
      const name = typeof data?.name === 'string' ? data.name.trim().slice(0, 24) : ''
      const avatar =
        typeof data?.avatar === 'string' && [...data.avatar].length <= 4 ? data.avatar : '🙂'

      if (!name) {
        ack({ ok: false, error: 'Ism bo‘sh bo‘lmasligi kerak' })
        return
      }
      if (friends.length >= MAX_FRIENDS) {
        ack({ ok: false, error: `Ko‘pi bilan ${MAX_FRIENDS} ta do‘st qo‘shish mumkin` })
        return
      }
      if (friends.some((f) => f.name.toLowerCase() === name.toLowerCase())) {
        ack({ ok: false, error: 'Bu ismli do‘st allaqachon bor' })
        return
      }

      const friend: Friend = {
        id: `friend-${Date.now()}`,
        name,
        avatar,
        savedMinutes: Math.floor(Math.random() * 160) + 40, // demo start range
        streak: Math.floor(Math.random() * 10) + 1,
        focusing: Math.random() < 0.4,
      }
      friends.push(friend)
      ack({ ok: true })

      activitySeq += 1
      io.emit('friend:activity', {
        id: `act-${activitySeq}`,
        name: friend.name,
        avatar: friend.avatar,
        text: `${friend.name} do‘stlar ro‘yxatiga qo‘shildi`,
        emoji: '👋',
        ts: Date.now(),
      })
      broadcastState()
      console.log(`[leaderboard] friend added: ${friend.name}`)
    }
  )

  socket.on('disconnect', () => {
    if (players.delete(socket.id)) {
      broadcastState()
    }
    console.log(`[leaderboard] disconnected: ${socket.id}`)
  })

  socket.on('error', (error) => {
    console.error(`[leaderboard] socket error (${socket.id}):`, error)
  })
})

httpServer.listen(PORT, () => {
  console.log(`Leaderboard WebSocket server running on port ${PORT}`)
})

process.on('SIGTERM', () => {
  httpServer.close(() => process.exit(0))
})
process.on('SIGINT', () => {
  httpServer.close(() => process.exit(0))
})
