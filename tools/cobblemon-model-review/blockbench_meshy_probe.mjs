#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { chromium } from 'playwright-core';

function arg(name) {
  const index = process.argv.indexOf(name);
  if (index === -1 || !process.argv[index + 1]) throw new Error(`Missing required argument ${name}`);
  return process.argv[index + 1];
}

const endpoint = arg('--endpoint');
const modelPath = path.resolve(arg('--model'));
const meshyPath = path.resolve(arg('--meshy'));
const texturePath = path.resolve(arg('--texture'));
const outputDir = path.resolve(arg('--output-dir'));

fs.mkdirSync(outputDir, { recursive: true });
const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
const meshySource = fs.readFileSync(meshyPath, 'utf8');

let browser;
let lastError;
for (let attempt = 0; attempt < 60; attempt += 1) {
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
  return typeof Codecs !== 'undefined'
    && Codecs?.bedrock
    && typeof Formats !== 'undefined'
    && Formats?.bedrock
    && typeof Plugin !== 'undefined'
    && typeof Texture !== 'undefined'
    && typeof Preview !== 'undefined';
}, null, { timeout: 60000 });

// Load Meshy before parsing the Bedrock model. Meshy's onload enables meshes on
// Bedrock formats and attaches the poly_mesh parse/compile codec hooks.
await page.addScriptTag({ content: meshySource });
await page.waitForFunction(() => Formats?.bedrock?.meshes === true, null, { timeout: 15000 });

const parseInfo = await page.evaluate(async ({ model, modelPath, texturePath }) => {
  Codecs.bedrock.load(model, { path: modelPath, name: 'meshy_poly_mesh_probe.geo.json', no_file: true });

  const started = Date.now();
  while ((!Project || !Array.isArray(Mesh?.all) || Mesh.all.length < 1) && Date.now() - started < 15000) {
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  if (!Project) throw new Error('Bedrock project did not load');
  if (!Array.isArray(Mesh?.all) || Mesh.all.length < 1) {
    throw new Error(`Meshy did not materialize poly_mesh; mesh count=${Mesh?.all?.length ?? 'unavailable'}`);
  }

  const loaded = new Texture().fromPath(texturePath).add(false, true);
  if (typeof loaded.setAsDefaultTexture === 'function') loaded.setAsDefaultTexture();
  else loaded.use_as_default = true;
  loaded.select();

  await new Promise((resolve, reject) => {
    const start = Date.now();
    const timer = setInterval(() => {
      if (loaded.error) {
        clearInterval(timer);
        reject(new Error(`Texture load failed with error ${loaded.error}`));
      } else if (loaded.width > 0 && loaded.height > 0 && loaded.img?.complete) {
        clearInterval(timer);
        resolve();
      } else if (Date.now() - start > 15000) {
        clearInterval(timer);
        reject(new Error('Timed out waiting for probe texture'));
      }
    }, 100);
  });

  Canvas.updateAllFaces();
  Canvas.updateAllBones();
  scene.updateMatrixWorld(true);
  await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));

  const compiled = Codecs.bedrock.compile();
  const compiledObject = typeof compiled === 'string' ? JSON.parse(compiled) : compiled;
  const geometry = compiledObject?.['minecraft:geometry']?.[0];
  const probeBone = geometry?.bones?.find(bone => bone.name === 'ouros_probe');
  if (!probeBone?.poly_mesh) throw new Error('Meshy compile hook did not preserve ouros_probe.poly_mesh');

  return {
    projectName: Project.name,
    formatId: Format.id,
    meshyLoaded: Formats.bedrock.meshes === true,
    meshCount: Mesh.all.length,
    cubeCount: Cube?.all?.length ?? 0,
    meshNames: Mesh.all.map(mesh => mesh.name),
    vertexCount: Object.keys(Mesh.all[0].vertices ?? {}).length,
    faceCount: Object.keys(Mesh.all[0].faces ?? {}).length,
    compiledPolyMesh: {
      positions: probeBone.poly_mesh.positions?.length ?? 0,
      normals: probeBone.poly_mesh.normals?.length ?? 0,
      uvs: probeBone.poly_mesh.uvs?.length ?? 0,
      polys: probeBone.poly_mesh.polys?.length ?? 0,
    },
    compiledObject,
  };
}, { model, modelPath, texturePath });

fs.writeFileSync(
  path.join(outputDir, 'compiled.geo.json'),
  `${JSON.stringify(parseInfo.compiledObject, null, 2)}\n`,
  'utf8',
);
delete parseInfo.compiledObject;

const views = {
  front: [0, 0, -96],
  three_quarter: [-72, 28, -72],
  back: [0, 0, 96],
};

const renders = {};
for (const [name, position] of Object.entries(views)) {
  const result = await page.evaluate(async ({ name, position }) => {
    scene.updateMatrixWorld(true);
    const bounds = new THREE.Box3();
    for (const cube of (Cube?.all ?? [])) if (cube.mesh) bounds.expandByObject(cube.mesh);
    for (const mesh of (Mesh?.all ?? [])) if (mesh.mesh) bounds.expandByObject(mesh.mesh);
    if (bounds.isEmpty()) throw new Error('Mesh-aware probe bounds are empty');

    const center = bounds.getCenter(new THREE.Vector3());
    const size = bounds.getSize(new THREE.Vector3());
    const preview = Preview.selected;
    preview.loadAnglePreset({
      id: `ouros_meshy_${name}`,
      name: `Ouros Meshy ${name}`,
      projection: 'orthographic',
      position,
      target: [0, 0, 0],
      zoom: 2.2,
    });
    const offset = preview.camera.position.clone().sub(preview.controls.target);
    preview.controls.target.copy(center);
    preview.camera.position.copy(center.clone().add(offset));
    preview.camOrtho.zoom = Math.min(3.2, Math.max(1.1, 11 / Math.max(size.x, size.y, size.z, 0.001)));
    preview.camOrtho.updateProjectionMatrix();
    preview.controls.update();
    preview.render();

    const dataUrl = await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error(`Screenshot timed out for ${name}`)), 15000);
      Screencam.advancedScreenshot(preview, {
        angle_preset: 'view',
        resolution: [768, 768],
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
      bounds: { min: bounds.min.toArray(), max: bounds.max.toArray(), size: size.toArray() },
    };
  }, { name, position });

  if (!String(result.dataUrl).startsWith('data:image/png;base64,')) throw new Error(`Invalid PNG for ${name}`);
  const png = Buffer.from(result.dataUrl.slice(result.dataUrl.indexOf(',') + 1), 'base64');
  if (png.length < 1024) throw new Error(`Probe screenshot ${name} unexpectedly small`);
  fs.writeFileSync(path.join(outputDir, `${name}.png`), png);
  renders[name] = { bytes: png.length, bounds: result.bounds };
}

const report = { ...parseInfo, renders };
fs.writeFileSync(path.join(outputDir, 'probe-report.json'), `${JSON.stringify(report, null, 2)}\n`, 'utf8');
console.log(JSON.stringify(report, null, 2));
await browser.close();
