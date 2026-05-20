// Generate Expo icon PNGs from SVG sources in assets/.
// Run: npm run generate:icons
import { readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..');
const assets = join(root, 'assets');

const tasks = [
  { src: 'icon-source.svg', out: 'icon.png', size: 1024 },
  { src: 'adaptive-icon-source.svg', out: 'adaptive-icon.png', size: 1024 },
  { src: 'splash-icon-source.svg', out: 'splash-icon.png', size: 1242 },
  { src: 'icon-source.svg', out: 'favicon.png', size: 48 },
];

for (const { src, out, size } of tasks) {
  const svgPath = join(assets, src);
  const outPath = join(assets, out);
  const svg = await readFile(svgPath);
  const png = await sharp(svg, { density: 384 })
    .resize(size, size, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toBuffer();
  await writeFile(outPath, png);
  console.log(`✓ ${out} (${size}×${size})`);
}
