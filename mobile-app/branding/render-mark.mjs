import { Resvg } from '@resvg/resvg-js';
import fs from 'node:fs';
const mark = fs.readFileSync('lumirra-mark.svg', 'utf8');
// Shield markaziga nisbatan 2x kattalashtirish — header'da mayda padding qolmasin.
const tight = mark
  .replace('<defs>', '<g transform="translate(512 537) scale(2.0) translate(-512 -537)"><defs>')
  .replace('</svg>', '</g></svg>');
fs.writeFileSync('lumirra-mark-tight.svg', tight);
const out = '../composeApp/src/commonMain/composeResources/drawable/lumirra_mark.png';
new Resvg(tight, { fitTo: { mode: 'width', value: 512 } }).render().asPng();
const buf = new Resvg(tight, { fitTo: { mode: 'width', value: 512 } }).render().asPng();
fs.writeFileSync(out, buf);
fs.writeFileSync('preview-mark-tight.png', buf);
console.log('mark ok');
