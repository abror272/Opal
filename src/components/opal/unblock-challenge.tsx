'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import { cn } from '@/lib/utils'
import { SlidersHorizontal, X, Sword } from 'lucide-react'

const QUESTIONS = 3

interface Challenge {
  a: number
  b: number
  answer: number
}

function makeQuestion(): Challenge {
  const a = 6 + Math.floor(Math.random() * 5) // 6..10
  const b = 5 + Math.floor(Math.random() * 6) // 5..10
  return { a, b, answer: a * b }
}

/**
 * Haqiqiy Opal "Battle Math" — blokdan chiqish oson bo'lmaydi.
 * 3 ta ko'paytirish masalasini to'g'ri yechilsa ilova ochiladi.
 */
export function UnblockChallenge({
  appName,
  onSolved,
  onClose,
}: {
  appName: string
  onSolved: () => void
  onClose: () => void
}) {
  const [questions, setQuestions] = useState<Challenge[]>(() =>
    Array.from({ length: QUESTIONS }, makeQuestion)
  )
  const [values, setValues] = useState<string[]>(['', '', ''])
  const [focusIdx, setFocusIdx] = useState(0)
  const [shake, setShake] = useState(false)
  const [solved, setSolved] = useState(false)
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])

  const allFilled = values.every((v) => v !== '')

  useEffect(() => {
    if (!allFilled || solved) return
    // kichik kechikish: katakchalar to'ldirilgach tekshirish (sync setState yo'q)
    const t = setTimeout(() => {
      const correct = values.every((v, i) => Number(v) === questions[i].answer)
      if (correct) {
        setSolved(true)
        setTimeout(() => onSolved(), 650)
      } else {
        setShake(true)
        setTimeout(() => {
          setValues(['', '', ''])
          setQuestions(Array.from({ length: QUESTIONS }, makeQuestion))
          setShake(false)
          setFocusIdx(0)
          inputRefs.current[0]?.focus()
        }, 550)
      }
    }, 80)
    return () => clearTimeout(t)
  }, [values])

  const setDigit = (digit: string) => {
    if (solved) return
    const idx = focusIdx
    setValues((prev) => {
      const next = [...prev]
      if (digit === 'del') {
        next[idx] = next[idx].slice(0, -1)
        return next
      }
      if (next[idx].length >= 3) return next
      const grown = next[idx] + digit
      next[idx] = grown
      return next
    })
    if (digit !== 'del') {
      // javob uzunligiga to'lganda keyingi katakchaga o'tish
      const expectedLen = String(questions[idx].answer).length
      const currentLen = values[idx].length + 1
      if (currentLen >= expectedLen && idx < QUESTIONS - 1) {
        const nextIdx = idx + 1
        setFocusIdx(nextIdx)
        inputRefs.current[nextIdx]?.focus()
      }
    }
  }

  const keypad = useMemo(() => ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del'], [])

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="absolute inset-0 z-[70] flex flex-col bg-[#050608]/95 px-5 pb-6 pt-4 backdrop-blur-xl"
      role="dialog"
      aria-label={`${appName}ni ochish uchun matematik masala`}
    >
      {/* header */}
      <div className="flex items-center justify-between">
        <button
          onClick={onClose}
          aria-label="Yopish"
          className="flex h-10 w-10 items-center justify-center rounded-full bg-white/8 text-white/80 ring-1 ring-white/12 backdrop-blur active:scale-90"
        >
          <X size={17} />
        </button>
        <span className="flex items-center gap-1.5 rounded-full bg-white/8 px-3 py-1.5 text-[10.5px] font-extrabold uppercase tracking-widest text-white/70 ring-1 ring-white/12">
          <Sword size={11} className="text-[#9fd8ff]" /> Battle Math
        </span>
        <button
          aria-label="Sozlamalar"
          className="flex h-10 w-10 items-center justify-center rounded-full bg-white/8 text-white/50 ring-1 ring-white/12 backdrop-blur"
        >
          <SlidersHorizontal size={15} />
        </button>
      </div>

      {/* savollar kartasi */}
      <motion.div
        animate={shake ? { x: [0, -9, 9, -7, 7, -3, 0] } : {}}
        transition={{ duration: 0.45 }}
        className={cn(
          'mt-6 rounded-[26px] border p-5',
          solved
            ? 'border-emerald-400/40 bg-emerald-500/10 shadow-[0_0_40px_rgba(52,211,153,0.25)]'
            : 'border-white/10 bg-white/[0.045] shadow-[inset_0_1px_0_0_rgba(255,255,255,0.06)]'
        )}
      >
        <p className="text-center text-[12.5px] font-semibold text-white/55">
          {solved ? (
            <span className="font-bold text-emerald-300">✓ {appName} ochildi!</span>
          ) : (
            'Barcha masalalarni yeching:'
          )}
        </p>
        <div className="mt-4 space-y-3.5">
          {questions.map((q, i) => (
            <div key={i} className="flex items-center justify-center gap-3">
              <span className="text-[21px] font-bold text-white/90">
                {q.a} <span className="mx-1 text-[15px] text-white/45">×</span> {q.b} =
              </span>
              <div
                className={cn(
                  'relative h-11 w-[84px] overflow-hidden rounded-xl border-2 transition-colors',
                  solved
                    ? 'border-emerald-400/70 bg-emerald-400/10'
                    : values[i]
                      ? 'border-[#86efac]/60 bg-[#5eead4]/5 shadow-[0_0_12px_rgba(94,234,212,0.2)]'
                      : 'border-[#86efac]/25 bg-transparent'
                )}
              >
                <input
                  ref={(el) => {
                    inputRefs.current[i] = el
                  }}
                  value={values[i]}
                  onChange={() => {}}
                  onFocus={() => setFocusIdx(i)}
                  inputMode="none"
                  aria-label={`${q.a} × ${q.b} javobi`}
                  className="h-full w-full cursor-default bg-transparent text-center font-mono text-[19px] font-bold text-[#bfe9ff] outline-none"
                  readOnly
                />
              </div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* keypad */}
      <div className="mt-auto grid grid-cols-3 gap-2.5 pt-6" aria-label="Klaviatura">
        {keypad.map((k, i) =>
          k === '' ? (
            <span key={i} />
          ) : (
            <motion.button
              key={i}
              whileTap={{ scale: 0.9 }}
              onClick={() => setDigit(k)}
              className={cn(
                'flex h-[54px] items-center justify-center rounded-2xl bg-white/[0.055] text-[21px] font-semibold text-white ring-1 ring-white/10 backdrop-blur transition-colors hover:bg-white/10',
                k === 'del' && 'text-[#9fd8ff]'
              )}
              aria-label={k === 'del' ? 'O‘chirish' : k}
            >
              {k === 'del' ? '⌫' : k}
            </motion.button>
          )
        )}
      </div>
    </motion.div>
  )
}
