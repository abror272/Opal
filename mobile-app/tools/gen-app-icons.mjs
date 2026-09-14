// Opal app icon generator — dependency-free PNG writer.
// Draws the Opal ring mark (white "O" with a top gap) on the dark brand background.
// Usage: node tools/gen-app-icons.mjs
import zlib from 'node:zlib';
import fs from 'node:fs';
import path from 'node:path';

const SIG = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body), 0);
  return Buffer.concat([len, body, crc]);
}

function encodePNG(width, height, rgba) {
  const stride = width * 4;
  const raw = Buffer.alloc((stride + 1) * height);
  for (let y = 0; y < height; y++) {
    raw[y * (stride + 1)] = 0;
    rgba.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;  // bit depth
  ihdr[9] = 6;  // RGBA
  const idat = zlib.deflateSync(raw, { level: 9 });
  return Buffer.concat([SIG, chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', Buffer.alloc(0))]);
}

function insideRoundRect(x, y, size, r) {
  const cx = Math.min(Math.max(x, r), size - r);
  const cy = Math.min(Math.max(y, r), size - r);
  const dx = x - cx, dy = y - cy;
  return dx * dx + dy * dy <= r * r;
}

/** Background vertical gradient: #151824 -> #05060B */
const BG_TOP = [21, 24, 36];
const BG_BOT = [5, 6, 11];

/**
 * @param {number} size  output size in px
 * @param {{round?:boolean, radius?:number, opaque?:boolean}} o
 */
function render(size, o = {}) {
  const S = 4;                       // supersampling
  const round = !!o.round;
  const radius = o.radius ?? size * 0.22;
  const opaque = !!o.opaque;
  const cx = size / 2, cy = size / 2;
  const R = size * 0.30;             // ring radius
  const W = size * 0.080;            // ring stroke width
  const buf = Buffer.alloc(size * size * 4);
  const N = S * S;

  for (let y = 0; y < size; y++) {
    const t = size > 1 ? y / (size - 1) : 0;
    const br = BG_TOP[0] + (BG_BOT[0] - BG_TOP[0]) * t;
    const bg = BG_TOP[1] + (BG_BOT[1] - BG_TOP[1]) * t;
    const bb = BG_TOP[2] + (BG_BOT[2] - BG_TOP[2]) * t;
    for (let x = 0; x < size; x++) {
      let bgHits = 0, ringHits = 0;
      for (let sy = 0; sy < S; sy++) {
        for (let sx = 0; sx < S; sx++) {
          const px = x + (sx + 0.5) / S;
          const py = y + (sy + 0.5) / S;
          const dx = px - cx, dy = py - cy;
          const d = Math.sqrt(dx * dx + dy * dy);
          const inBg = round ? d <= size / 2 : insideRoundRect(px, py, size, radius);
          if (inBg) bgHits++;
          if (Math.abs(d - R) <= W / 2) {
            // Gap centred on top, matching the in-app "O" (arc spans -72deg .. 252deg).
            const a = ((Math.atan2(dy, dx) * 180 / Math.PI) + 360) % 360;
            if (a >= 285 || a <= 255) ringHits++;
          }
        }
      }
      const bgCov = bgHits / N;
      const ringCov = ringHits / N;
      const k = (y * size + x) * 4;
      buf[k] = Math.round(br * (1 - ringCov) + 255 * ringCov);
      buf[k + 1] = Math.round(bg * (1 - ringCov) + 255 * ringCov);
      buf[k + 2] = Math.round(bb * (1 - ringCov) + 255 * ringCov);
      buf[k + 3] = opaque ? 255 : Math.round(bgCov * 255);
    }
  }
  return encodePNG(size, size, buf);
}

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')), '..');
const androidRes = path.join(root, 'composeApp/src/androidMain/res');

const DENSITIES = [
  ['mdpi', 48],
  ['hdpi', 72],
  ['xhdpi', 96],
  ['xxhdpi', 144],
  ['xxxhdpi', 192]
];

for (const [dpi, px] of DENSITIES) {
  const dir = path.join(androidRes, `mipmap-${dpi}`);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, 'ic_launcher.png'), render(px));
  fs.writeFileSync(path.join(dir, 'ic_launcher_round.png'), render(px, { round: true }));
  fs.writeFileSync(path.join(dir, 'ic_launcher_foreground.png'), render(px, { opaque: false, radius: 0 }));
}

// iOS marketing icon: full-bleed square, no alpha.
const iosIcon = path.join(root, 'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon1024.png');
if (fs.existsSync(path.dirname(iosIcon))) {
  fs.writeFileSync(iosIcon, render(1024, { opaque: true, radius: 0 }));
}

console.log('Opal app icons generated.');
