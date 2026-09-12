'use client'

import { useState, useSyncExternalStore } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { StatusBar } from './status-bar'
import { ArrowRight, ShieldCheck, Timer, BarChart3 } from 'lucide-react'
import { cn } from '@/lib/utils'

const STORAGE_KEY = 'opal-onboarded'

const SLIDES = [
  {
    emoji: '🛡️',
    icon: ShieldCheck,
    gradient: 'from-[#3d5afe] to-[#7b61ff]',
    title: 'Chalg‘ituvchi ilovalarni bloklang',
    desc: 'TikTok, Instagram va boshqalarni bir touch bilan himoya ostiga oling.',
  },
  {
    emoji: '⏱️',
    icon: Timer,
    gradient: 'from-[#7b61ff] to-[#e861ff]',
    title: 'Fokus sessiyasini boshlang',
    desc: 'Deep Focus, Ish yoki O‘qish rejimida vaqtingizni qadrlang.',
  },
  {
    emoji: '🔥',
    icon: BarChart3,
    gradient: 'from-[#ff9f5a] to-[#ff6b9d]',
    title: 'Streak quring, natijani ko‘ring',
    desc: 'Har kun fokus — katta farq. Statistikangiz bilan o‘sishingizni kuzating.',
  },
]

export function Onboarding({ onDone }: { onDone: () => void }) {
  const [step, setStep] = useState(0)
  const slide = SLIDES[step]

  const next = () => {
    if (step < SLIDES.length - 1) setStep((s) => s + 1)
    else finish()
  }

  const finish = () => {
    try {
      localStorage.setItem(STORAGE_KEY, '1')
    } catch {
      // localStorage mavjud bo'lmasa — faqat shu sessiya uchun o'tkazib yuboramiz
    }
    onDone()
  }

  return (
    <div className="flex h-full w-full flex-col bg-gradient-to-b from-[#10123f] via-[#181b58] to-[#2a1e7a] text-white">
      <StatusBar dark />

      <div className="flex items-center justify-between px-6 pt-2">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3d5afe] via-[#7b61ff] to-[#e861ff] text-base font-black shadow-lg shadow-indigo-500/40">
            O
          </div>
          <span className="text-lg font-extrabold tracking-tight">Opal</span>
        </div>
        <button
          onClick={finish}
          className="text-[13px] font-semibold text-white/50 transition-colors hover:text-white/80"
        >
          O‘tkazib yuborish
        </button>
      </div>

      <div className="relative flex flex-1 flex-col items-center justify-center px-8">
        {/* floating blobs */}
        <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
          <div className="absolute -left-10 top-1/4 h-48 w-48 animate-blob rounded-full bg-[#3d5afe]/25 blur-3xl" />
          <div className="absolute -right-12 bottom-1/4 h-48 w-48 animate-blob-delayed rounded-full bg-[#e861ff]/20 blur-3xl" />
        </div>

        <AnimatePresence mode="wait">
          <motion.div
            key={step}
            initial={{ opacity: 0, x: 40, scale: 0.96 }}
            animate={{ opacity: 1, x: 0, scale: 1 }}
            exit={{ opacity: 0, x: -40, scale: 0.96 }}
            transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
            className="relative flex flex-col items-center text-center"
          >
            <div
              className={cn(
                'flex h-32 w-32 items-center justify-center rounded-[2.5rem] bg-gradient-to-br shadow-2xl',
                slide.gradient
              )}
            >
              <span className="animate-float text-[64px]">{slide.emoji}</span>
            </div>
            <h2 className="mt-9 text-[26px] font-extrabold leading-snug tracking-tight">
              {slide.title}
            </h2>
            <p className="mt-3 max-w-[270px] text-[14.5px] leading-relaxed text-white/60">
              {slide.desc}
            </p>
          </motion.div>
        </AnimatePresence>
      </div>

      <div className="relative px-8 pb-10">
        <div className="mb-6 flex items-center justify-center gap-2">
          {SLIDES.map((_, i) => (
            <button
              key={i}
              onClick={() => setStep(i)}
              aria-label={`${i + 1}-slayd`}
              className={cn(
                'h-2 rounded-full transition-all duration-300',
                i === step ? 'w-7 bg-white' : 'w-2 bg-white/25 hover:bg-white/40'
              )}
            />
          ))}
        </div>

        <button
          onClick={next}
          className="group flex w-full items-center justify-center gap-2 rounded-3xl bg-white py-4 text-[16px] font-extrabold text-[#10123f] shadow-xl transition-transform active:scale-[0.98]"
        >
          {step === SLIDES.length - 1 ? 'Boshlash 🚀' : 'Davom etish'}
          <ArrowRight
            size={18}
            className="transition-transform group-hover:translate-x-1"
          />
        </button>
      </div>
    </div>
  )
}

/** localStorage'ga qarab kerakligini aniqlaydi (SSR-safe, useSyncExternalStore) */
const noopSubscribe = () => () => {}

function clientSnapshot(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) !== '1'
  } catch {
    return false
  }
}

function serverSnapshot(): boolean | null {
  return null // hali noma'lum — yuklanmoqda holati
}

export function useNeedsOnboarding(): boolean | null {
  return useSyncExternalStore(noopSubscribe, clientSnapshot, serverSnapshot)
}
