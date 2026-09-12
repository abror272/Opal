'use client'

import { useCallback, useEffect, useState } from 'react'
import {
  joinLiveBoard,
  onLiveActivity,
  onLiveConnection,
  onLiveState,
  syncLiveMinutes,
  type JoinPayload,
} from '@/lib/opal-live-client'
import type { LiveBoardState, LiveActivity } from '@/lib/opal-live-types'

/**
 * Subscribes to the shared live-leaderboard singleton client.
 * The socket itself lives in `opal-live-client.ts` (one connection per app),
 * this hook just mirrors its state into React.
 */
export function useLiveLeaderboard(
  join: JoinPayload | null,
  onActivity?: (a: LiveActivity) => void
) {
  const [state, setState] = useState<LiveBoardState | null>(null)
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    const offState = onLiveState(setState)
    const offConn = onLiveConnection(setConnected)
    const offActivity = onActivity ? onLiveActivity(onActivity) : undefined
    return () => {
      offState()
      offConn()
      offActivity?.()
    }
  }, [onActivity])

  // join / re-join whenever the payload changes
  useEffect(() => {
    if (join) joinLiveBoard(join)
  }, [join, connected])

  const syncMinutes = useCallback((savedMinutes: number) => {
    syncLiveMinutes(savedMinutes)
  }, [])

  return { state, connected, syncMinutes }
}
