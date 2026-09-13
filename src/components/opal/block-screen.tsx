'use client'

import { AnimatePresence, motion } from 'framer-motion'
import { useOpalStore } from '@/lib/opal-store'
import { SESSION_WITTY_LINES } from '@/lib/opal-ui'

/**
 * Haqiqiy Opal "X is Blocked by Opal" ekrani.
 * Bloklangan ilova belgisini bosganda ko'rsatiladi.
 */
export function BlockScreen() {
  const blockedView = useOpalStore((s) => s.blockedView)
  const setBlockedView = useOpalStore((s) => s.setBlockedView)

  return (
    <AnimatePresence>
      {blockedView && (
        <motion.div
          key="block-screen"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.25 }}
          className="absolute inset-0 z-[80] flex flex-col items-center justify-between bg-[#020204] px-8 pb-10 pt-24"
          role="alertdialog"
          aria-label={`${blockedView.name} bloklandi`}
          onClick={() => setBlockedView(null)}
        >
          {/* fon nur */}
          <div
            className="pointer-events-none absolute left-1/2 top-1/3 h-72 w-72 -translate-x-1/2 -translate-y-1/2 rounded-full opacity-30"
            style={{
              background:
                'radial-gradient(circle, rgba(143,217,255,0.35) 0%, rgba(177,140,255,0.2) 45%, transparent 70%)',
            }}
            aria-hidden="true"
          />

          <motion.div
            initial={{ scale: 0.85, opacity: 0, y: 14 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            transition={{ delay: 0.1, type: 'spring', stiffness: 260, damping: 22 }}
            className="relative z-10 flex flex-col items-center text-center"
          >
            {/* ilova belgisi — biroz xiralashtirilgan (haqiqiy Opal kabi) */}
            <div
              className={`flex h-[74px] w-[74px] items-center justify-center rounded-[20px] text-[36px] ${blockedView.gradient}`}
              style={{ filter: 'blur(1px) saturate(1.35) brightness(1.25)', boxShadow: '0 0 40px rgba(160,140,255,0.35)' }}
              aria-hidden="true"
            >
              {blockedView.emoji}
            </div>
            <h2 className="mt-7 text-[24px] font-extrabold leading-snug text-white">
              {blockedView.name} is
              <br />
              <span className="text-white/95">Blocked by Opal</span>
            </h2>
            <p className="mt-3 max-w-[240px] text-[13px] leading-relaxed text-white/45">
              {SESSION_WITTY_LINES[Math.floor(blockedView.name.length % SESSION_WITTY_LINES.length)]}
            </p>
          </motion.div>

          <motion.button
            initial={{ y: 24, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.22 }}
            whileTap={{ scale: 0.97 }}
            onClick={(e) => {
              e.stopPropagation()
              setBlockedView(null)
            }}
            className="relative z-10 w-full max-w-[300px] rounded-full bg-white py-4 text-[15px] font-bold text-black shadow-[0_10px_40px_rgba(255,255,255,0.12)] active:scale-[0.98]"
          >
            Dismiss
          </motion.button>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
