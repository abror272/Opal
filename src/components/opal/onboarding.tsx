'use client'

import { useState, useSyncExternalStore } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { StatusBar } from './status-bar'
import { ArrowRight } from 'lucide-react'
import { cn } from '@/lib/utils'

const STORAGE_KEY = 'opal-onboarded'

const SLIDES = [
  {
    img: '/opal/crystal.png',
    blend: true,
    title: 'Opal chalg‘ituvchilarni siz uchun bloklaydi',
    desc: 'TikTok, Instagram va boshqalar — bir bosish bilan himoya ostida.',
  },
  {
    img: '/opal/timer-scene.jpg',
    blend: false,
    title: 'Bir bosish. To‘liq fokus.',
    desc: 'Flip-clock taymer bilan sessiya boshlang — ilovalar o‘z-o‘zidan bloklanadi.',
  },
  {
    img: null,
    blend: false,
    title: 'Progressingizni chuqur his qiling',
    desc: 'Opal Score uyqu, fokus va damni bitta ko‘rsatkichga jamsheydi.',
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
    <div className="relative flex h-full w-full flex-col overflow-hidden bg-[#05060f] text-white">
      <StatusBar />

      {/* fon: slayd rasmi yoki kristall */}
      <AnimatePresence mode="wait">
        <motion.div
          key={step}
          initial={{ opacity: 0, scale: 1.06 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.98 }}
          transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="pointer-events-none absolute inset-0"
          aria-hidden="true"
        >
          {slide.img ? (
            <>
              <img
                src={slide.img}
                alt=""
                className={cn(
                  'h-full w-full object-cover',
                  slide.blend ? 'mix-blend-screen opacity-45' : 'opacity-40'
                )}
                draggable={false}
              />
              <div className="absolute inset-0 bg-gradient-to-b from-[#05060f]/40 via-transparent to-[#05060f]" />
            </>
          ) : (
            <>
              <div
                className="absolute inset-0"
                style={{
                  background:
                    'radial-gradient(ellipse 80% 55% at 50% 30%, rgba(50,64,102,0.5) 0%, transparent 65%), radial-gradient(ellipse 60% 45% at 65% 80%, rgba(90,60,130,0.4) 0%, transparent 60%)',
                }}
              />
              <div
                className="absolute inset-0 opacity-50"
                style={{
                  backgroundImage:
                    'radial-gradient(1.2px 1.2px at 22% 30%, rgba(255,255,255,0.55) 50%, transparent 51%),' +
                    'radial-gradient(1px 1px at 74% 18%, rgba(255,255,255,0.45) 50%, transparent 51%),' +
                    'radial-gradient(1.6px 1.6px at 58% 62%, rgba(255,255,255,0.5) 50%, transparent 51%),' +
                    'radial-gradient(1px 1px at 38% 76%, rgba(255,255,255,0.35) 50%, transparent 51%)',
                }}
              />
            </>
          )}
        </motion.div>
      </AnimatePresence>

      <div className="relative flex items-center justify-between px-6 pt-2">
        <div className="flex items-center gap-2.5">
          <span className="inline-block h-8 w-8 rounded-[10px] bg-gradient-to-br from-[#86efac] via-[#b18cff] to-[#ff9ad5] shadow-[0_0_18px_rgba(134,239,172,0.5)]" />
          <span className="text-lg font-bold tracking-tight">Opal</span>
        </div>
        <button
          onClick={finish}
          className="rounded-full px-3 py-1.5 text-[12.5px] font-semibold text-white/50 transition-colors hover:bg-white/5 hover:text-white/85"
        >
          O‘tkazib yuborish
        </button>
      </div>

      <div className="relative flex flex-1 flex-col items-center justify-center px-8 pb-6">
        {/* matn o'qilishi uchun radial scrim (yorqin kristall ustida ham) */}
        <div
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              'radial-gradient(ellipse 82% 62% at 50% 52%, rgba(5,6,15,0.88) 0%, rgba(5,6,15,0.55) 55%, rgba(5,6,15,0.15) 80%, transparent 92%)',
          }}
          aria-hidden="true"
        />
        <AnimatePresence mode="wait">
          <motion.div
            key={step}
            initial={{ opacity: 0, y: 26 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -18 }}
            transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
            className="flex flex-col items-center text-center"
          >
            <span
              key={`badge-${step}`}
              className="mb-5 rounded-full border border-white/15 bg-white/8 px-4 py-1.5 text-[11px] font-extrabold uppercase tracking-[0.2em] text-white/70 backdrop-blur"
            >
              {step === 0 ? 'Block' : step === 1 ? 'Focus' : 'Score'}
            </span>
            <h2 className="max-w-[300px] text-[27px] font-extrabold leading-snug tracking-tight [text-shadow:0_2px_24px_rgba(0,0,0,0.6)]">
              {slide.title}
            </h2>
            <p className="mt-3.5 max-w-[280px] text-[14px] leading-relaxed text-white/60">
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
                i === step
                  ? 'w-7 bg-[#86efac] shadow-[0_0_10px_rgba(134,239,172,0.7)]'
                  : 'w-2 bg-white/20 hover:bg-white/35'
              )}
            />
          ))}
        </div>

        <motion.button
          whileTap={{ scale: 0.97 }}
          onClick={next}
          className="group flex w-full items-center justify-center gap-2 rounded-full border border-white/20 bg-white/12 py-4 text-[15.5px] font-bold text-white shadow-[0_14px_40px_rgba(0,0,0,0.45),inset_0_1px_0_0_rgba(255,255,255,0.3)] backdrop-blur-xl active:scale-[0.98]"
        >
          {step === SLIDES.length - 1 ? 'Boshlash' : 'Davom etish'}
          <ArrowRight size={17} className="transition-transform group-hover:translate-x-1" />
        </motion.button>
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
