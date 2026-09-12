'use client'

import { useEffect, useRef, useState } from 'react'

/**
 * Animates a number from its previous value to `target` with ease-out cubic.
 * Use for "odometer style" counters on stats and streaks.
 */
export function useCountUp(target: number, durationMs = 900): number {
  const [value, setValue] = useState(target)
  const fromRef = useRef(target)
  const valueRef = useRef(target)
  const rafRef = useRef(0)

  useEffect(() => {
    const from = fromRef.current
    if (from === target) return

    const start = performance.now()
    const tick = (t: number) => {
      const p = Math.min((t - start) / durationMs, 1)
      const eased = 1 - Math.pow(1 - p, 3)
      const next = Math.round(from + (target - from) * eased)
      valueRef.current = next
      setValue(next)
      if (p < 1) {
        rafRef.current = requestAnimationFrame(tick)
      } else {
        fromRef.current = target
      }
    }
    rafRef.current = requestAnimationFrame(tick)

    return () => {
      cancelAnimationFrame(rafRef.current)
      // continue smoothly from wherever we stopped next time
      fromRef.current = valueRef.current
    }
  }, [target, durationMs])

  return value
}
