// Singleton socket.io client for the live leaderboard mini-service.
// Shared across components so any part of the app (overlay, leaderboard, etc.)
// can join / sync minutes / add friends through ONE connection.
import { io, type Socket } from 'socket.io-client'
import type { LiveBoardState, LiveActivity } from '@/lib/opal-live-types'

export interface JoinPayload {
  name: string
  savedMinutes: number
  streak: number
}

export interface AddFriendResult {
  ok: boolean
  error?: string
}

type StateHandler = (s: LiveBoardState) => void
type ActivityHandler = (a: LiveActivity) => void
type ConnectionHandler = (connected: boolean) => void

let socket: Socket | null = null
let joinedPayload: JoinPayload | null = null
let connected = false

const stateHandlers = new Set<StateHandler>()
const activityHandlers = new Set<ActivityHandler>()
const connectionHandlers = new Set<ConnectionHandler>()

function notifyConnection(next: boolean) {
  connected = next
  connectionHandlers.forEach((h) => h(next))
}

function ensureSocket(): Socket {
  if (socket) return socket

  // Never use PORT in the URL, always use XTransformPort.
  // DO NOT change the path, it is used by Caddy to forward to the mini-service.
  const s = io('/?XTransformPort=3003', {
    transports: ['websocket', 'polling'],
    forceNew: true,
    reconnection: true,
    reconnectionAttempts: 8,
    reconnectionDelay: 1500,
    timeout: 8000,
  })
  socket = s

  s.on('connect', () => {
    notifyConnection(true)
    if (joinedPayload) s.emit('leaderboard:join', joinedPayload)
  })
  s.on('disconnect', () => notifyConnection(false))
  s.on('connect_error', () => notifyConnection(false))

  s.on('leaderboard:state', (data: LiveBoardState) => {
    stateHandlers.forEach((h) => h(data))
  })
  s.on('friend:activity', (a: LiveActivity) => {
    activityHandlers.forEach((h) => h(a))
  })

  return s
}

/** Join (or re-join) the board with the user's latest stats. */
export function joinLiveBoard(payload: JoinPayload) {
  joinedPayload = payload
  const s = ensureSocket()
  if (s.connected) s.emit('leaderboard:join', payload)
}

/** Push the user's updated weekly saved minutes (e.g. after a session completes). */
export function syncLiveMinutes(savedMinutes: number) {
  if (socket?.connected && Number.isFinite(savedMinutes)) {
    socket.emit('leaderboard:sync', { savedMinutes })
  }
}

/** Ask the service to add a friend. Resolves via socket acknowledgement. */
export function addLiveFriend(name: string, avatar: string): Promise<AddFriendResult> {
  const s = ensureSocket()
  return new Promise((resolve) => {
    if (!s.connected) {
      resolve({ ok: false, error: 'Jonli reytingga ulanmagansiz' })
      return
    }
    const timer = setTimeout(() => resolve({ ok: false, error: 'Server javob bermadi' }), 4000)
    s.emit('friend:add', { name, avatar }, (res: AddFriendResult) => {
      clearTimeout(timer)
      resolve(res ?? { ok: false, error: 'Noma’lum xato' })
    })
  })
}

export function onLiveState(cb: StateHandler): () => void {
  ensureSocket()
  stateHandlers.add(cb)
  return () => {
    stateHandlers.delete(cb)
  }
}

export function onLiveActivity(cb: ActivityHandler): () => void {
  ensureSocket()
  activityHandlers.add(cb)
  return () => {
    activityHandlers.delete(cb)
  }
}

export function onLiveConnection(cb: ConnectionHandler): () => void {
  ensureSocket()
  connectionHandlers.add(cb)
  return () => {
    connectionHandlers.delete(cb)
  }
}

export function isLiveConnected() {
  return connected
}
