// Opal clone — PWA Service Worker
// Strategiya: navigatsiyalar network-first (offline → /offline.html),
// statik assetlar (ikonka/rasmlar/_next/static) cache-first.
// /api/* so'rovlar umuman kesilmaydi (yangi ma'lumot muhim).

const VERSION = 'opal-v1'
const STATIC_CACHE = `${VERSION}-static`
const PRECACHE = [
  '/offline.html',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/opal/crystal.png',
  '/opal/timer-scene.jpg',
  '/opal/routine-sleep.jpg',
  '/opal/routine-deepwork.jpg',
  '/opal/routine-family.jpg',
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(STATIC_CACHE)
      .then((cache) => cache.addAll(PRECACHE.map((u) => new Request(u, { cache: 'reload' }))))
      .catch(() => {})
      .then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => !k.startsWith(VERSION)).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (event) => {
  const req = event.request
  if (req.method !== 'GET') return

  const url = new URL(req.url)
  if (url.origin !== self.location.origin) return
  // API hech qachon cache qilinmaydi
  if (url.pathname.startsWith('/api/')) return
  // Next.js HMR / dev websocket so'rovlarini tegma
  if (url.pathname.startsWith('/_next/webpack-hmr')) return

  // 1) Sahifa navigatsiyalari — network-first, offline fallback
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req)
        .then((res) => {
          const copy = res.clone()
          caches.open(STATIC_CACHE).then((c) => c.put('/', copy)).catch(() => {})
          return res
        })
        .catch(async () => {
          const cache = await caches.open(STATIC_CACHE)
          return (await cache.match('/')) || (await cache.match('/offline.html')) || Response.error()
        })
    )
    return
  }

  // 2) Statik assetlar — cache-first
  const isStatic =
    url.pathname.startsWith('/_next/static/') ||
    url.pathname.startsWith('/icons/') ||
    url.pathname.startsWith('/opal/') ||
    /\.(png|jpg|jpeg|webp|svg|ico|woff2?)$/.test(url.pathname)

  if (isStatic) {
    event.respondWith(
      caches.match(req).then(
        (hit) =>
          hit ||
          fetch(req).then((res) => {
            if (res.ok) {
              const copy = res.clone()
              caches.open(STATIC_CACHE).then((c) => c.put(req, copy)).catch(() => {})
            }
            return res
          })
      )
    )
  }
})
