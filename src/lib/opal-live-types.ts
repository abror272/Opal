// Shared types for the live WebSocket leaderboard

export interface LiveBoardEntry {
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

export interface LiveBoardState {
  entries: LiveBoardEntry[]
  online: number
}

export interface LiveActivity {
  id: string
  name: string
  avatar: string
  text: string
  emoji: string
  ts: number
}
