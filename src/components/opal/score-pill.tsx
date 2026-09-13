'use client'

// Haqiqiy Opal "score pill" — stadion shakli, stropka o'zi progress,
// ikonka + raqam ichida, yorliq PASTDA (referens skrinshotlardagidek)

import { motion } from 'framer-motion'
import { cn } from '@/lib/utils'
import { OPAL } from '@/lib/opal-ui'

let pillSeq = 0

export function ScorePill({
  value,
  icon,
  label,
  size = 'sm',
  selected = false,
  delay = 0,
  onClick,
}: {
  value: number
  icon: React.ReactNode
  label: string
  size?: 'sm' | 'lg'
  selected?: boolean
  delay?: number
  onClick?: () => void
}) {
  const gradId = `opal-pill-grad-${++pillSeq}`
  const dims = size === 'lg' ? { w: 108, h: 54, fs: 19, icon: 16, label: 12.5 } : { w: 92, h: 44, fs: 16.5, icon: 14, label: 11 }
  const r = dims.h / 2

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
      className="flex flex-col items-center gap-2"
    >
      <motion.button
        whileTap={onClick ? { scale: 0.94 } : undefined}
        onClick={onClick}
        disabled={!onClick}
        aria-label={`${label} ${value}`}
        className={cn(
          'relative block rounded-full outline-none transition-transform',
          !onClick && 'cursor-default'
        )}
        style={{ width: dims.w, height: dims.h }}
      >
        <svg
          viewBox={`0 0 ${dims.w} ${dims.h}`}
          width={dims.w}
          height={dims.h}
          className="absolute inset-0"
          aria-hidden="true"
        >
          <defs>
            <linearGradient id={gradId} x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor={OPAL.ringGrad[0]} />
              <stop offset="100%" stopColor={OPAL.ringGrad[1]} />
            </linearGradient>
          </defs>
          {/* track */}
          <rect
            x="1.6"
            y="1.6"
            width={dims.w - 3.2}
            height={dims.h - 3.2}
            rx={r - 1.6}
            fill="rgba(8,14,10,0.55)"
            stroke="rgba(255,255,255,0.13)"
            strokeWidth="1.8"
          />
          {/* progress stropka — pathLength bilan normallashtirilgan */}
          <motion.rect
            x="1.6"
            y="1.6"
            width={dims.w - 3.2}
            height={dims.h - 3.2}
            rx={r - 1.6}
            fill="none"
            stroke={`url(#${gradId})`}
            strokeWidth={selected ? '3' : '2.4'}
            strokeLinecap="round"
            pathLength={100}
            strokeDasharray={100}
            initial={{ strokeDashoffset: 100 }}
            animate={{ strokeDashoffset: 100 - Math.max(Math.min(value, 100), 3) }}
            transition={{ duration: 1.1, ease: [0.22, 1, 0.36, 1], delay: delay + 0.15 }}
            style={{
              filter: `drop-shadow(0 0 ${selected ? 8 : 5}px ${OPAL.mintGlow})`,
              opacity: selected ? 1 : 0.9,
            }}
          />
        </svg>
        <span className="absolute inset-0 flex items-center justify-center gap-1.5 text-white">
          <span style={{ opacity: 0.9 }} className="[&>svg]:h-[15px] [&>svg]:w-[15px]">
            {icon}
          </span>
          <span
            className="font-extrabold leading-none tracking-tight"
            style={{
              fontSize: dims.fs,
              color: selected ? OPAL.mint : '#fff',
              textShadow: selected ? `0 0 14px ${OPAL.mintGlow}` : 'none',
            }}
          >
            {value}
          </span>
        </span>
      </motion.button>
      <span
        className={cn(
          'font-semibold tracking-wide',
          selected ? 'text-[#c9fbdc]' : 'text-white/70'
        )}
        style={{ fontSize: dims.label }}
      >
        {label}
      </span>
    </motion.div>
  )
}

/** Score'dan pilllargacha osilgan ingichka bracket (haqiqiy Opal kabi) */
export function ScoreBracket({ width = 210 }: { width?: number }) {
  const w = width
  const h = 26
  return (
    <svg
      viewBox={`0 0 ${w} ${h}`}
      width={w}
      height={h}
      className="mx-auto -mb-1 mt-1 opacity-60"
      aria-hidden="true"
    >
      {/* markazdan pastga, so'ng ikki tomonga yoqilgan nozik bracket */}
      <path
        d={`M ${w / 2} 0 V 9 Q ${w / 2} 14 ${w / 2 - 8} 14 H 12 Q 4 14 4 20 V ${h}`}
        fill="none"
        stroke="rgba(255,255,255,0.28)"
        strokeWidth="1.4"
      />
      <path
        d={`M ${w / 2} 0 V 9 Q ${w / 2} 14 ${w / 2 + 8} 14 H ${w - 12} Q ${w - 4} 14 ${w - 4} 20 V ${h}`}
        fill="none"
        stroke="rgba(255,255,255,0.28)"
        strokeWidth="1.4"
      />
    </svg>
  )
}
