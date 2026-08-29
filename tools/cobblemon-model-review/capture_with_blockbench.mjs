#!/usr/bin/env node
/**
 * Capture an untouched Cobblemon Bedrock model through Blockbench itself.
 *
 * This script does NOT interpret Bedrock geometry, calculate bone matrices,
 * rasterize cubes, or apply project-authored poses. It only automates a pinned
 * Blockbench web build, asks Blockbench to import the official texture and
 * official animation, selects that animation, sets the Blockbench camera, and
 * captures Blockbench's own WebGL canvas.
 */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { chromium } from 'playwright';

function arg(name) {
  const index = process.argv.indexOf(`--${name}`);
  if (index < 0 || !process.argv[index + 1]) {
    throw new Error(`Missing --${name}`);
  }
  return process.argv[index + 1];
}

function sha256(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

const baseUrl = arg('base-url');
const modelPath = path.resolve(arg('model'));
const texturePath = path.resolve(arg('texture'));
const animationPath = path.resolve(arg('animation'));
const outputPath = path.resolve(arg('output'));
const metadataPath = path.resolve(arg('metadata'));
const animationName = process.argv.includes('--animation-name')
  ? arg('animation-name')
  : 'animation.pikachu.ground_idle';

fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.mkdirSync(path.dirname(metadataPath), { recursive: true });

const modelText = fs.readFileSync(modelPath, 'utf8');
const animationText = fs.readFileSync(animationPath, 'utf8');

const browser = await chromium.launch({
  headless: true,
  args: [
    '--use-gl=swiftshader',
    '--enable-webgl',
    '--ignore-gpu-blocklist',
  ],
});

try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 960 } });
  page.on('console', msg => console.log(`[blockbench:${msg.type()}] ${msg.text()}`));
  page.on('pageerror', error => console.error(`[blockbench:pageerror] ${error.stack || error}`));

  // Blockbench's own documented web loader parses the JSON and selects the
  // matching Bedrock Entity codec. We only transport the exact upstream JSON.
  const url = `${baseUrl}/?loadtype=json&loadname=${encodeURIComponent(path.basename(modelPath))}&loaddata=${encodeURIComponent(modelText)}`;
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 120_000 });

  await page.waitForFunction(() => {
    return typeof Group !== 'undefined' && Group.all?.length > 0 &&
           typeof main_preview !== 'undefined' && main_preview?.canvas;
  }, null, { timeout: 120_000 });

  const globals = await page.evaluate(() => ({
    groups: Group.all.length,
    cubes: typeof Cube !== 'undefined' ? Cube.all.length : null,
    animationCodec: typeof AnimationCodec,
    animation: typeof Animation,
    animator: typeof Animator,
    timeline: typeof Timeline,
    modes: typeof Modes,
    texture: typeof Texture,
    barItems: typeof BarItems,
    mainPreview: typeof main_preview,
  }));
  console.log('Blockbench globals:', JSON.stringify(globals));
  for (const key of ['animationCodec', 'animation', 'animator', 'timeline', 'modes', 'texture', 'barItems', 'mainPreview']) {
    if (globals[key] === 'undefined') throw new Error(`Blockbench global unavailable: ${key}`);
  }

  // Import the exact release texture through Blockbench's own texture action.
  const chooserPromise = page.waitForEvent('filechooser', { timeout: 30_000 });
  await page.evaluate(() => BarItems.import_texture.click());
  const chooser = await chooserPromise;
  await chooser.setFiles(texturePath);
  await page.waitForFunction(() => Texture.all?.some(t => t.width === 128 && t.height === 64), null, { timeout: 30_000 });

  // Import only the official standing animation using Blockbench's Bedrock
  // animation codec. No project-authored transform or pose data is supplied.
  const imported = await page.evaluate(({ content, animationName }) => {
    const codec = AnimationCodec.codecs?.bedrock;
    if (!codec || typeof codec.loadFile !== 'function') {
      throw new Error('Blockbench Bedrock animation codec is unavailable');
    }
    const loaded = codec.loadFile(
      { content, path: 'pikachu.animation.json', name: 'pikachu.animation.json' },
      [animationName],
    );
    return loaded.map(animation => animation.name);
  }, { content: animationText, animationName });
  if (!imported.includes(animationName)) {
    throw new Error(`Blockbench did not import ${animationName}: ${JSON.stringify(imported)}`);
  }

  const state = await page.evaluate(({ animationName }) => {
    Modes.options.animate.select();
    const animation = Animation.all.find(item => item.name === animationName);
    if (!animation) throw new Error(`Animation not found after import: ${animationName}`);
    animation.select();
    Timeline.setTime(0);
    Animator.preview();

    // Review-only camera/background controls. These affect no model bones.
    settings.grids?.set(false);
    settings.ground_plane?.set(false);
    settings.preview_checkerboard?.set(false);
    Canvas.show_gizmos = false;

    // Pikachu's face points toward negative Z. Use Blockbench's own orthographic
    // camera to look from negative Z toward the model center.
    main_preview.loadAnglePreset({
      projection: 'orthographic',
      position: [0, 12, -512],
      target: [0, 12, 0],
      zoom: 0.95,
    });
    main_preview.resize();
    main_preview.render();

    return {
      format: Format?.id,
      geometryName: Project?.geometry_name,
      groupCount: Group.all.length,
      cubeCount: Cube.all.length,
      textureCount: Texture.all.length,
      selectedAnimation: Animation.selected?.name,
      timelineTime: Timeline.time,
      mode: Modes.id,
      canvasWidth: main_preview.canvas.width,
      canvasHeight: main_preview.canvas.height,
    };
  }, { animationName });

  await page.waitForTimeout(800);
  await page.evaluate(() => main_preview.render());

  // The PNG bytes come directly from Blockbench's WebGL canvas.
  const canvas = page.locator('#preview canvas').first();
  await canvas.waitFor({ state: 'visible', timeout: 30_000 });
  await canvas.screenshot({ path: outputPath, omitBackground: false });

  const png = fs.readFileSync(outputPath);
  if (png.length < 10_000 || png.subarray(1, 4).toString('ascii') !== 'PNG') {
    throw new Error(`Invalid Blockbench PNG output (${png.length} bytes)`);
  }

  const metadata = {
    format: 'ouros.blockbench-cobblemon-review.v1',
    viewer: {
      name: 'Blockbench',
      version: '5.1.6',
      sourceTag: 'v5.1.6',
      renderer: 'Blockbench WebGL canvas',
    },
    inputs: {
      model: { file: path.basename(modelPath), sha256: sha256(modelPath) },
      texture: { file: path.basename(texturePath), sha256: sha256(texturePath) },
      animation: { file: path.basename(animationPath), sha256: sha256(animationPath) },
      animationName,
    },
    blockbenchState: state,
    output: {
      file: path.basename(outputPath),
      sha256: sha256(outputPath),
      bytes: png.length,
    },
    assertions: {
      customGeometryRendererUsed: false,
      projectAuthoredBoneTransformsUsed: false,
      modelEditedBeforeCapture: false,
    },
  };
  fs.writeFileSync(metadataPath, JSON.stringify(metadata, null, 2) + '\n');
  console.log(JSON.stringify(metadata, null, 2));
} finally {
  await browser.close();
}
