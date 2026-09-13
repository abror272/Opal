'use client'

import { useState } from 'react'
import type { OpalGem } from '@/lib/opal-ui'
import { cn } from '@/lib/utils'

/**
 * AI generatsiya qilingan haqiqiy tosh rasmi (qora fonda, mix-blend-screen).
 * Rasm yuklanmasa — avvalgi CSS gradient blob'ga qaytadi (graceful fallback).
 */
export function GemImage({
  gem,
  size = 58,
  unlocked,
  className,
}: {
  gem: Pick<OpalGem, 'key' | 'colors'>
  /** px */
  size?: number
  unlocked: boolean
  className?: string
}) {
  const [failed, setFailed] = useState(false)

  if (failed) {
    return (
      <span
        className={cn('relative block', !unlocked && 'opacity-30 grayscale', className)}
        style={{
          width: size,
          height: size,
          borderRadius: '42% 58% 55% 45% / 48% 44% 56% 52%',
          background: `radial-gradient(circle at 32% 28%, ${gem.colors[0]} 0%, ${gem.colors[1]} 48%, ${gem.colors[2]} 100%)`,
          boxShadow: unlocked
            ? `0 0 22px ${gem.colors[1]}77, inset 0 -4px 10px rgba(0,0,0,0.35), inset 0 3px 6px rgba(255,255,255,0.25)`
            : 'inset 0 -4px 10px rgba(0,0,0,0.4)',
        }}
        aria-hidden="true"
      >
        <span className="absolute left-[22%] top-[16%] h-2.5 w-3 rounded-full bg-white/55 blur-[3px]" />
      </span>
    )
  }

  return (
    <img
      src={`/opal/gems/${gem.key}.png`}
      alt=""
      draggable={false}
      loading="lazy"
      onError={() => setFailed(true)}
      className={cn('block object-cover mix-blend-screen', !unlocked && 'opacity-30 grayscale', className)}
      style={{
        width: size,
        height: size,
        borderRadius: '34%',
        WebkitMaskImage: 'radial-gradient(circle, black 58%, transparent 74%)',
        maskImage: 'radial-gradient(circle, black 58%, transparent 74%)',
      }}
    />
  )
}
