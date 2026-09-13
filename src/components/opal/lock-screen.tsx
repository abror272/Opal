'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'
import { ShieldCheck } from 'lucide-react'
import { PinPad } from './pin-pad'
import { useOpalStore } from '@/lib/opal-store'

export function LockScreen({ onUnlock }: { onUnlock: () => void }) {
  const pinCode = useOpalStore((s) => s.pinCode)
  const [errorPulse, setErrorPulse] = useState(0)

  const handleComplete = (pin: string) => {
    if (pin === '__face__') {
      onUnlock()
      return
    }
    if (pin === pinCode) {
      onUnlock()
    } else {
      setErrorPulse((n) => n + 1)
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="absolute inset-0 z-[70] flex flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-[#07121c] via-[#0a1d24] to-[#0c2620] px-6"
      role="dialog"
      aria-label="Ilova qulflangan"
    >
      {/* ambient blobs — mint nur */}
      <div className="pointer-events-none absolute -left-16 top-16 h-52 w-52 animate-blob rounded-full bg-[#5eead4]/18 blur-3xl" />
      <div className="pointer-events-none absolute -right-12 bottom-24 h-48 w-48 animate-blob-delayed rounded-full bg-[#86efac]/14 blur-3xl" />

      <div className="relative flex flex-col items-center">
        <motion.div
          initial={{ scale: 0.7, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ delay: 0.1, type: 'spring', stiffness: 220, damping: 16 }}
          className="flex h-20 w-20 items-center justify-center rounded-[1.6rem] bg-gradient-to-br from-[#b7f5cd] via-[#86efac] to-[#5eead4] shadow-[0_16px_44px_rgba(134,239,172,0.35)] ring-1 ring-white/25"
        >
          <ShieldCheck size={34} className="text-[#052e16]" strokeWidth={2.2} />
        </motion.div>

        <p className="mt-5 text-[26px] font-black tracking-tight text-white">Opal qulflangan</p>
        <p className="mt-1 text-[12.5px] text-white/50">Sizning ekran vaqtingiz himoyalanadi</p>

        <div className="mt-8 w-full">
          <PinPad
            onComplete={handleComplete}
            title="PIN kodni kiriting"
            subtitle="4 xonali kodingizni tering"
            allowFaceId
            errorPulse={errorPulse}
            dark
          />
        </div>

        <p className="mt-6 text-[10.5px] text-white/30">
          Face ID (demo) — skaner tugmasi bilan ham ochiladi
        </p>
      </div>
    </motion.div>
  )
}
