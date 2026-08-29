#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { chromium } from 'playwright-core';

function arg(name) {
  const index = process.argv.indexOf(name);
  if (index === -1 || !process.argv[index + 1]) {
    throw new Error(`Missing required argument ${name}`);
  }
  return process.argv[index + 1];
}

function optionalArg(name, fallback = null) {
  const index = process.argv.indexOf(name);
  return index === -1 || !process.argv[index + 1] ? fallback : process.argv[index + 1];
}

const endpoint = arg('--endpoint');
const texturePath = path.resolve(arg('--texture'));
const outputDir = path.resolve(arg('--output-dir'));
const animationArg = optionalArg('--animation');
const animationName = optionalArg('--animation-name');
const animationTime = Number(optionalArg('--animation-time', '0'));
const animationPath = animationArg ? path.resolve(animationArg) : null;
const animationContent = animationPath ? fs.readFileSync(animationPath, 'utf8') : null;

if ((animationPath && !animationName) || (!animationPath && animationName)) {
  throw new Error('--animation and --animation-name must be provided together');
}
if (!Number.isFinite(animationTime) || animationTime < 0) {
  throw new Error(`Invalid --animation-time ${animationTime}`);
}

fs.mkdirSync(outputDir, { recursive: true });

let browser;
let lastError;
for (let attempt = 0; attempt < 40; attempt += 1) {
  try {
    browser = await chromium.connectOverCDP(endpoint);
    break;
  } catch (error) {
    lastError = error;
    await new Promise(resolve => setTimeout(resolve, 500));
  }
}
if (!browser) throw lastError ?? new Error('Could not connect to Blockbench CDP endpoint');

const pages = browser.contexts().flatMap(context => context.pages());
if (!pages.length) throw new Error('Blockbench opened no renderer page');
const page = pages.find(candidate => candidate.url().includes('index.html')) ?? pages[0];

await page.waitForFunction(() => {
  return typeof Project !== 'undefined'
    && Project
    && typeof Format !== 'undefined'
    && Format
    && typeof Preview !== 'undefined'
    && Preview.selected
    && typeof Cube !== 'undefined'
    && Array.isArray(Cube.all)
    && Cube.all.length > 0
    && typeof Group !== 'undefined'
    && typeof AnimationCodec !== 'undefined'
    && typeof Animator !== 'undefined'
    && typeof Timeline !== 'undefined';
}, null, { timeout: 60000 });

const modelInfo = await page.evaluate(async ({ texture, animationContent, animationPath, animationName, animationTime }) => {
  const info = {
    projectName: Project.name,
    formatId: Format.id,
    cubeCount: Cube.all.length,
    boneCount: Group.all.length,
    boneNames: Group.all.map(group => group.name),
  };

  const loaded = new Texture().fromPath(texture).add(false, true);
  if (typeof loaded.setAsDefaultTexture === 'function') {
    loaded.setAsDefaultTexture();
  } else {
    loaded.use_as_default = true;
  }
  loaded.select();

  await new Promise((resolve, reject) => {
    const started = Date.now();
    const timer = setInterval(() => {
      if (loaded.error) {
        clearInterval(timer);
        reject(new Error(`Blockbench texture load failed with error ${loaded.error}`));
        return;
      }
      if (loaded.width > 0 && loaded.height > 0 && loaded.img?.complete) {
        clearInterval(timer);
        resolve();
        return;
      }
      if (Date.now() - started > 15000) {
        clearInterval(timer);
        reject(new Error('Timed out waiting for Blockbench texture load'));
      }
    }, 100);
  });

  Canvas.updateAllFaces();
  Canvas.updateAllBones();

  let appliedAnimation = null;
  if (animationContent && animationName) {
    if (!Animator.open) Animator.join();
    const codec = AnimationCodec.getCodec();
    if (!codec || typeof codec.loadFile !== 'function') {
      throw new Error(`Blockbench has no animation codec for format ${Format.id}`);
    }
    const imported = codec.loadFile(
      { content: animationContent, path: animationPath, name: animationPath?.split(/[\\/]/).pop() },
      [animationName],
    );
    const animation = imported?.find(candidate => candidate.name === animationName)
      ?? Animation.all.find(candidate => candidate.name === animationName);
    if (!animation) {
      throw new Error(`Blockbench did not import animation ${animationName}`);
    }
    animation.select();
    animation.playing = true;
    Timeline.setTime(animationTime);
    Animator.preview();
    scene.updateMatrixWorld(true);
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    appliedAnimation = {
      name: animation.name,
      requestedTime: animationTime,
      timelineTime: Timeline.time,
      length: animation.length,
      loop: animation.loop,
      codec: codec.id,
    };
  }

  return {
    ...info,
    textureName: loaded.name,
    textureWidth: loaded.width,
    textureHeight: loaded.height,
    appliedAnimation,
  };
}, { texture: texturePath, animationContent, animationPath, animationName, animationTime });

if (!String(modelInfo.formatId).includes('bedrock')) {
  throw new Error(`Expected Blockbench Bedrock format, got ${modelInfo.formatId}`);
}
if (modelInfo.textureWidth <= 0 || modelInfo.textureHeight <= 0) {
  throw new Error('Blockbench did not load the Cobblemon texture');
}
if (animationName && modelInfo.appliedAnimation?.name !== animationName) {
  throw new Error(`Expected Blockbench animation ${animationName}, got ${modelInfo.appliedAnimation?.name}`);
}

const views = {
  front: { position: [0, 0, -512], locked: 'north', horizontalAxis: 'x' },
  back:  { position: [0, 0, 512],  locked: 'south', horizontalAxis: 'x' },
  left:  { position: [-512, 0, 0], locked: 'west',  horizontalAxis: 'z' },
  right: { position: [512, 0, 0],  locked: 'east',  horizontalAxis: 'z' },
};

const renderMetadata = {};
for (const [name, view] of Object.entries(views)) {
  const result = await page.evaluate(async ({ name, view }) => {
    if (typeof Animator !== 'undefined' && Animator.open && Animation?.selected) {
      Animator.preview();
    }
    scene.updateMatrixWorld(true);
    const bounds = new THREE.Box3();
    for (const cube of Cube.all) {
      if (cube.mesh) bounds.expandByObject(cube.mesh);
    }
    if (bounds.isEmpty()) throw new Error('Blockbench model bounds are empty');

    const center = bounds.getCenter(new THREE.Vector3());
    const size = bounds.getSize(new THREE.Vector3());
    const horizontalSpan = view.horizontalAxis === 'x' ? size.x : size.z;
    const verticalSpan = size.y;
    const margin = 1.20;
    const targetResolution = 1024;
    const orthoWorldSpanAtZoomOne = targetResolution / 40;
    const zoom = Math.min(
      orthoWorldSpanAtZoomOne / Math.max(horizontalSpan * margin, 0.001),
      orthoWorldSpanAtZoomOne / Math.max(verticalSpan * margin, 0.001),
    );

    const preview = Preview.selected;
    preview.loadAnglePreset({
      id: `ouros_${name}`,
      name: `Ouros ${name}`,
      projection: 'orthographic',
      position: view.position,
      target: [0, 0, 0],
      zoom,
      locked_angle: view.locked,
    });

    const cameraOffset = preview.camera.position.clone().sub(preview.controls.target);
    preview.controls.target.copy(center);
    preview.camera.position.copy(center.clone().add(cameraOffset));
    preview.camOrtho.zoom = zoom;
    preview.camOrtho.updateProjectionMatrix();
    preview.controls.update();
    preview.render();

    const dataUrl = await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('Blockbench screenshot timed out')), 15000);
      Screencam.advancedScreenshot(preview, {
        angle_preset: 'view',
        resolution: [targetResolution, targetResolution],
        anti_aliasing: 'off',
        show_gizmos: false,
        shading: true,
      }, data => {
        clearTimeout(timeout);
        resolve(data);
      });
    });

    return {
      dataUrl,
      bounds: {
        min: bounds.min.toArray(),
        max: bounds.max.toArray(),
        center: center.toArray(),
        size: size.toArray(),
      },
      zoom,
    };
  }, { name, view });

  if (!String(result.dataUrl).startsWith('data:image/png;base64,')) {
    throw new Error(`Blockbench returned invalid PNG data for ${name}`);
  }
  const png = Buffer.from(result.dataUrl.slice(result.dataUrl.indexOf(',') + 1), 'base64');
  if (png.length < 1024) throw new Error(`Blockbench ${name} screenshot is unexpectedly small`);
  fs.writeFileSync(path.join(outputDir, `${name}.png`), png);
  renderMetadata[name] = {
    bounds: result.bounds,
    zoom: result.zoom,
    bytes: png.length,
  };
}

const metadata = {
  viewer: 'Blockbench',
  sourceOfTransforms: modelInfo.appliedAnimation ? 'Blockbench Bedrock animation codec' : 'Blockbench model bind pose',
  modelInfo,
  views: renderMetadata,
};
fs.writeFileSync(
  path.join(outputDir, 'blockbench-metadata.json'),
  `${JSON.stringify(metadata, null, 2)}\n`,
  'utf8',
);

console.log(JSON.stringify(metadata, null, 2));
await browser.close();
