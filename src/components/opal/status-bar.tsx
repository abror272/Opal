'use client'

/** iOS uslubidagi status paneli — Opal har doim qorong'u rejimda */
export function StatusBar() {
  return (
    <div className="relative z-30 flex h-11 shrink-0 select-none items-center justify-between px-7 pt-1 text-white">
      <span className="text-[14px] font-semibold tabular-nums tracking-wide">9:41</span>
      <div className="flex items-center gap-1.5" aria-hidden="true">
        {/* signal */}
        <svg width="17" height="11" viewBox="0 0 17 11" fill="none">
          {[0, 1, 2, 3].map((i) => (
            <rect
              key={i}
              x={i * 4.4}
              y={8 - i * 2.6}
              width="3"
              height={3 + i * 2.6}
              rx="0.8"
              fill="white"
              opacity={i === 3 ? 0.35 : 1}
            />
          ))}
        </svg>
        {/* wifi */}
        <svg width="15" height="11" viewBox="0 0 15 11" fill="none">
          <path d="M7.5 9.5 L9.6 7.2 A3.2 3.2 0 0 0 5.4 7.2 Z" fill="white" />
          <path
            d="M3.2 5.1a6.6 6.6 0 0 1 8.6 0"
            stroke="white"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
          <path
            d="M1 2.6a10 10 0 0 1 13 0"
            stroke="white"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
        </svg>
        {/* battery */}
        <svg width="25" height="12" viewBox="0 0 25 12" fill="none">
          <rect x="0.5" y="0.5" width="21" height="11" rx="3.5" stroke="white" opacity="0.4" />
          <rect x="2" y="2" width="15" height="8" rx="2" fill="white" />
          <path d="M23 4v4a2.2 2.2 0 0 0 0-4Z" fill="white" opacity="0.4" />
        </svg>
      </div>
    </div>
  )
}
