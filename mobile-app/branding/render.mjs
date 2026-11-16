import { Resvg } from '@resvg/resvg-js';
import { PNG } from 'pngjs';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve('..');
const icon = fs.readFileSync('panoh-icon.svg', 'utf8');
const mark = fs.readFileSync('panoh-mark.svg', 'utf8');
// iOS / store: to'liq kvadrat fon (alpha yo'q, burchaklar Apple tomonidan maskalanadi)
const iconSquare = icon.replaceAll('rx="232"', 'rx="0"');

function render(svg, size) {
  return new Resvg(svg, { fitTo: { mode: 'width', value: size } }).render().asPng();
}

function write(file, buf) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, buf);
}

function stripAlpha(pngBuf, out) {
  const png = PNG.sync.read(pngBuf);
  const rgb = new PNG({ width: png.width, height: png.height, colorType: 2 });
  for (let i = 0, j = 0; i < png.data.length; i += 4, j += 3) {
    rgb.data[j] = png.data[i];
    rgb.data[j + 1] = png.data[i + 1];
    rgb.data[j + 2] = png.data[i + 2];
  }
  write(out, PNG.sync.write(rgb, { colorType: 2 }));
}

const sizes = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
for (const [dpi, size] of Object.entries(sizes)) {
  const buf = render(icon, size);
  write(`${root}/composeApp/src/androidMain/res/mipmap-${dpi}/ic_launcher.png`, buf);
  write(`${root}/composeApp/src/androidMain/res/mipmap-${dpi}/ic_launcher_round.png`, buf);
}

// iOS AppIcon (alpha'siz)
stripAlpha(render(iconSquare, 1024), `${root}/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon1024.png`);
// Store / umumiy 1024
stripAlpha(render(iconSquare, 1024), `${root}/icon1024.png`);
// Ilova ichidagi logo (shaffof fon) — render-mark.mjs tomonidan (tight versiya)
// Preview / hujjat uchun
write('preview-icon.png', render(icon, 512));
write('preview-mark.png', render(mark, 256));
write('panoh-store-512.png', render(iconSquare, 512));
console.log('rendered');
