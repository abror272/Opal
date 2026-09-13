'use client'

/**
 * Ambient fokus tovushlari — 100% Web Audio sintezi (audio fayl kerak emas).
 *  - rain  : oq shovqin + bandpass (yomg'ir shamasasi)
 *  - waves : brown shovqin + lowpass + sekin LFO (dengiz to'lqinlari)
 *  - deep  : brown shovqin + chuqur lowpass (fokus gumbazi)
 *
 * Barcha o'tishlar smooth fade bilan (1s+) — hech qachon "tı" qilib uzilmaydi.
 * SSR-safe: serverda doim 'off'.
 */

export type AmbientMode = 'off' | 'rain' | 'waves' | 'deep'

export const AMBIENT_MODES: { mode: AmbientMode; label: string; emoji: string }[] = [
  { mode: 'off', label: 'Ovoz yo‘q', emoji: '🔇' },
  { mode: 'rain', label: 'Yomg‘ir', emoji: '🌧️' },
  { mode: 'waves', label: 'Dengiz', emoji: '🌊' },
  { mode: 'deep', label: 'Chuqur', emoji: '🎧' },
]

let current: AmbientMode = 'off'
const listeners = new Set<() => void>()

let ctx: AudioContext | null = null
let master: GainNode | null = null
let stopSources: (() => void) | null = null
let fadeTimeout: ReturnType<typeof setTimeout> | null = null

function ensureCtx(): AudioContext {
  if (!ctx) {
    const AC: typeof AudioContext =
      window.AudioContext ??
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    ctx = new AC()
    master = ctx.createGain()
    master.gain.value = 0
    master.connect(ctx.destination)
  }
  if (ctx.state === 'suspended') void ctx.resume()
  return ctx
}

function makeNoiseBuffer(ac: AudioContext, brown: boolean): AudioBuffer {
  const len = ac.sampleRate * 3
  const buf = ac.createBuffer(1, len, ac.sampleRate)
  const data = buf.getChannelData(0)
  let last = 0
  for (let i = 0; i < len; i++) {
    const white = Math.random() * 2 - 1
    if (brown) {
      last = (last + 0.02 * white) / 1.02
      data[i] = last * 3.2
    } else {
      data[i] = white
    }
  }
  return buf
}

function startMode(mode: Exclude<AmbientMode, 'off'>) {
  const ac = ensureCtx()
  stopSourcesNow(false)

  const src = ac.createBufferSource()
  src.buffer = makeNoiseBuffer(ac, mode !== 'rain')
  src.loop = true

  const filter = ac.createBiquadFilter()
  const gain = ac.createGain()

  let lfo: OscillatorNode | null = null
  if (mode === 'rain') {
    filter.type = 'bandpass'
    filter.frequency.value = 1500
    filter.Q.value = 0.55
    gain.gain.value = 0.09
  } else if (mode === 'waves') {
    filter.type = 'lowpass'
    filter.frequency.value = 520
    gain.gain.value = 0.3
    // to'lqinpuze swell — sekin LFO gain ustida
    lfo = ac.createOscillator()
    lfo.frequency.value = 0.085
    const lfoGain = ac.createGain()
    lfoGain.gain.value = 0.17
    lfo.connect(lfoGain)
    lfoGain.connect(gain.gain)
    lfo.start()
  } else {
    filter.type = 'lowpass'
    filter.frequency.value = 210
    gain.gain.value = 0.36
  }

  src.connect(filter)
  filter.connect(gain)
  gain.connect(master!)
  src.start()

  stopSources = () => {
    try {
      src.stop()
    } catch {
      /* already stopped */
    }
    if (lfo) {
      try {
        lfo.stop()
      } catch {
        /* noop */
      }
    }
    src.disconnect()
    filter.disconnect()
    gain.disconnect()
  }

  // smooth fade-in
  const t = ac.currentTime
  master!.gain.cancelScheduledValues(t)
  master!.gain.setValueAtTime(Math.max(master!.gain.value, 0.0001), t)
  master!.gain.linearRampToValueAtTime(1, t + 1.4)
}

function stopSourcesNow(fadeOut: boolean) {
  if (!ctx || !master) {
    stopSources = null
    return
  }
  const t = ctx.currentTime
  if (fadeOut) {
    master.gain.cancelScheduledValues(t)
    master.gain.setValueAtTime(Math.max(master.gain.value, 0.0001), t)
    master.gain.linearRampToValueAtTime(0.0001, t + 0.9)
  } else {
    master.gain.cancelScheduledValues(t)
    master.gain.setValueAtTime(0.0001, t)
  }
  const toStop = stopSources
  stopSources = null
  if (fadeTimeout) clearTimeout(fadeTimeout)
  fadeTimeout = setTimeout(
    () => {
      toStop?.()
    },
    fadeOut ? 950 : 0
  )
}

/** Rejimni o'zgartirish (smooth crossfade bilan). */
export function setAmbientMode(mode: AmbientMode) {
  if (mode === current) return
  current = mode
  if (typeof window === 'undefined') return
  try {
    if (mode === 'off') {
      stopSourcesNow(true)
    } else {
      startMode(mode)
    }
  } catch {
    // AudioContext mavjud emas (test muhit) — jim o'tkazamiz
  }
  listeners.forEach((l) => l())
}

export function getAmbientMode(): AmbientMode {
  return current
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

const serverSnapshot = (): AmbientMode => 'off'

import { useSyncExternalStore } from 'react'

/** React uchun hook — joriy ambient rejim. */
export function useAmbientMode(): AmbientMode {
  return useSyncExternalStore(subscribe, getAmbientMode, serverSnapshot)
}
