'use client'

import { useEffect, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import { Delete, ScanFace } from 'lucide-react'
import { cn } from '@/lib/utils'

interface PinPadProps {
  onComplete: (pin: string) => void
  /** message above the dots */
  title: string
  subtitle?: string
  /** show a FaceID-style demo shortcut button that auto-completes */
  allowFaceId?: boolean
  /** external error trigger counter — increments to shake */
  errorPulse?: number
  dark?: boolean
}

const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'face', '0', 'del']

export function PinPad({ onComplete, title, subtitle, allowFaceId = false, errorPulse = 0, dark = false }: PinPadProps) {
  const [pin, setPin] = useState('')
  const [scanning, setScanning] = useState(false)
  const lockRef = useRef(false)

  // errorPulse o‘zgarganda pin’ni tozalash (render-adjust pattern)
  const [lastErrorPulse, setLastErrorPulse] = useState(errorPulse)
  if (errorPulse !== lastErrorPulse) {
    setLastErrorPulse(errorPulse)
    setPin('')
  }
  const justErrored = errorPulse > 0 && pin.length === 0

  useEffect(() => {
    if (pin.length === 4 && !lockRef.current) {
      lockRef.current = true
      const t = setTimeout(() => {
        onComplete(pin)
        lockRef.current = false
      }, 180)
      return () => clearTimeout(t)
    }
  }, [pin, onComplete])

  const press = (k: string) => {
    if (scanning) return
    if (k === 'del') setPin((p) => p.slice(0, -1))
    else if (k === 'face') {
      if (!allowFaceId) return
      setScanning(true)
      setTimeout(() => {
        setScanning(false)
        onComplete('__face__')
      }, 1400)
    } else setPin((p) => (p.length < 4 ? p + k : p))
  }

  return (
    <div className="flex w-full flex-col items-center select-none">
      <p className={cn('text-[15px] font-extrabold', dark ? 'text-white' : 'text-slate-900 dark:text-slate-50')}>
        {title}
      </p>
      {subtitle && (
        <p className={cn('mt-1 text-[12px]', dark ? 'text-white/50' : 'text-slate-400')}>{subtitle}</p>
      )}

      {/* dots */}
      <motion.div
        key={errorPulse}
        animate={{ x: errorPulse > 0 ? [0, -14, 14, -9, 9, -4, 0] : 0 }}
        transition={{ duration: 0.5 }}
        className="mt-6 flex items-center gap-5"
        aria-label="PIN raqamlari"
        role="status"
      >
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className={cn(
              'h-3.5 w-3.5 rounded-full transition-all duration-200',
              justErrored
                ? 'scale-110 bg-rose-400'
                : i < pin.length
                  ? 'scale-110 bg-gradient-to-br from-[#3d5afe] to-[#7b61ff] shadow-sm shadow-indigo-400/40'
                  : dark
                    ? 'bg-white/15'
                    : 'bg-slate-200 dark:bg-white/15'
            )}
          />
        ))}
      </motion.div>

      {scanning && (
        <p className="mt-4 flex items-center gap-2 text-[12px] font-bold text-emerald-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-emerald-400/30 border-t-emerald-400" />
          Yuz skanerlanmoqda…
        </p>
      )}

      {/* keypad */}
      <div className="mt-7 grid w-full max-w-[264px] grid-cols-3 gap-x-5 gap-y-3.5">
        {KEYS.map((k) => {
          if (k === 'face') {
            return (
              <button
                key="face"
                type="button"
                onClick={() => press('face')}
                disabled={!allowFaceId || scanning}
                aria-label="Face ID bilan ochish"
                className={cn(
                  'flex h-[62px] items-center justify-center rounded-full transition-all active:scale-90',
                  allowFaceId && !scanning
                    ? dark
                      ? 'text-white/70 hover:bg-white/10'
                      : 'text-slate-500 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-white/10'
                    : 'opacity-25'
                )}
              >
                <ScanFace size={26} strokeWidth={1.8} />
              </button>
            )
          }
          if (k === 'del') {
            return (
              <button
                key="del"
                type="button"
                onClick={() => press('del')}
                aria-label="O‘chirish"
                className={cn(
                  'flex h-[62px] items-center justify-center rounded-full transition-all active:scale-90',
                  dark ? 'text-white/70 hover:bg-white/10' : 'text-slate-500 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-white/10'
                )}
              >
                <Delete size={24} strokeWidth={1.8} />
              </button>
            )
          }
          return (
            <button
              key={k}
              type="button"
              onClick={() => press(k)}
              aria-label={`Raqam ${k}`}
              className={cn(
                'flex h-[62px] items-center justify-center rounded-full text-[26px] font-semibold transition-all active:scale-90',
                dark
                  ? 'text-white hover:bg-white/10'
                  : 'text-slate-800 hover:bg-slate-100 dark:text-slate-100 dark:hover:bg-white/10'
              )}
            >
              {k}
            </button>
          )
        })}
      </div>
    </div>
  )
}
