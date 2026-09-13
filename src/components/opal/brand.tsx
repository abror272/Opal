'use client'

// Umumiy brend elementlari — Opal wordmark va hexagon avatar
// (home + apps tablari bir xil header ishlatadi, haqiqiy Opal kabi)

import { motion } from 'framer-motion'
import { OPAL } from '@/lib/opal-ui'

/** Opal so'zi — haqiqiy logotipdagi kabi upperrashka O va 'pal' */
export function OpalWordmark({ size = 'md' }: { size?: 'md' | 'sm' }) {
  const s = size === 'md' ? { box: 21, text: 20 } : { box: 18, text: 17 }
  return (
    <span className="flex items-end gap-[3px]">
      <svg
        width={s.box}
        height={s.box}
        viewBox="0 0 22 22"
        fill="none"
        aria-hidden="true"
        className="mb-[1px]"
      >
        {/* halqa + yuqori chapdagi kichik bo'shliq (haqiqiy O glyfi) */}
        <path
          d="M 8.2 2.6 A 9 9 0 1 0 13.8 2.6"
          stroke="#fff"
          strokeWidth="2.9"
          strokeLinecap="round"
        />
      </svg>
      <span
        className="font-bold leading-none tracking-tight text-white"
        style={{ fontSize: s.text }}
      >
        pal
      </span>
    </span>
  )
}

/** Haqiqiy Opal profil tugmasi — olti burchakli ramka + odam silueti */
export function HexAvatarButton({ onClick, size = 36 }: { onClick: () => void; size?: number }) {
  return (
    <motion.button
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      aria-label="Profil"
      className="relative flex items-center justify-center"
      style={{ width: size, height: size }}
    >
      <svg viewBox="0 0 36 36" className="absolute inset-0 h-full w-full" aria-hidden="true">
        <path
          d="M18 2.5 L31.5 9.2 V26.8 L18 33.5 L4.5 26.8 V9.2 Z"
          fill="rgba(183,245,205,0.07)"
          stroke={OPAL.mint}
          strokeWidth="1.7"
          strokeLinejoin="round"
          style={{ filter: `drop-shadow(0 0 6px ${OPAL.mintGlow})` }}
        />
        {/* odam silueti */}
        <circle cx="18" cy="14.2" r="3.4" fill="#d8ffe6" />
        <path d="M 11.6 26.5 Q 18 19.5 24.4 26.5 Z" fill="#d8ffe6" />
      </svg>
    </motion.button>
  )
}
