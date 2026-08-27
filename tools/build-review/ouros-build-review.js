(function () {
'use strict';

window.__ourosViewerBooted = true;

const registryUrl = 'build-data/builds.json';
const assetBase = 'minecraft-assets/';
const canvas = document.getElementById('canvas');
const loading = document.getElementById('loading');
const loadTitle = document.getElementById('load-title');
const loadCopy = document.getElementById('load-copy');
const bar = document.getElementById('bar');
const errorBox = document.getElementById('error');
const buildSelect = document.getElementById('build-select');
const spaceSelect = document.getElementById('space-select');
const spaceRow = document.getElementById('space-row');
const layerButtons = document.getElementById('layer-buttons');
const slice = document.getElementById('slice');
const sliceValue = document.getElementById('slice-value');
const panel = document.getElementById('panel');
const hud = document.getElementById('hud');
const tip = document.getElementById('tip');
const flyPad = document.getElementById('fly-pad');

function fail(error) {
  console.error(error);
  if (loading) loading.style.display = 'none';
  if (errorBox) {
    errorBox.style.display = 'block';
    errorBox.textContent = 'Minecraft viewer error: ' + ((error && error.message) || error)
      + '. The exported geometry remains authoritative; this browser renderer failed.';
  }
}

const ds = window.deepslate;
const gm = window.glMatrix;
if (!ds) { fail(new Error('local Deepslate UMD runtime missing')); return; }
if (!gm || !gm.mat4) { fail(new Error('local gl-matrix UMD runtime missing')); return; }

const { BlockDefinition, BlockModel, Identifier, Structure, StructureRenderer, TextureAtlas, upperPowerOfTwo } = ds;
const { mat4 } = gm;
const gl = canvas.getContext('webgl', { antialias: true, alpha: false, preserveDrawingBuffer: false });
if (!gl) { fail(new Error('WebGL is unavailable')); return; }

let registry = null;
let currentBuild = null;
let manifest = null;
let resources = null;
let renderer = null;
let currentLayer = 'all';
let currentSpace = '';
let cameraMode = 'orbit';
let yaw = -0.72;
let pitch = 0.55;
let orbitDistance = 120;
let orbitTarget = [0, 0, 0];
let flyPosition = [0, 0, 0];
let flyYaw = 0;
let flyPitch = 0;
const keys = new Set();
const pointers = new Map();
let lastPinch = null;
let lastCenter = null;
let dragKind = 'rotate';
let lastFrame = performance.now();
let loadToken = 0;

function progress(percent, title, copy) {
  if (bar) bar.style.width = percent + '%';
  if (title && loadTitle) loadTitle.textContent = title;
  if (copy && loadCopy) loadCopy.textContent = copy;
}

function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }
function add(a, b) { return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]; }
function scale(v, n) { return [v[0] * n, v[1] * n, v[2] * n]; }
function length(v) { return Math.hypot(v[0], v[1], v[2]); }
function normalize(v) {
  const n = length(v) || 1;
  return [v[0] / n, v[1] / n, v[2] / n];
}
function cross(a, b) {
  return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
}
function worldToLocal(v) {
  return [v[0] - manifest.min[0], v[1] - manifest.min[1], v[2] - manifest.min[2]];
}
function localCenter() { return [manifest.size[0] / 2, manifest.size[1] / 2, manifest.size[2] / 2]; }
function orbitEye() {
  const cp = Math.cos(pitch);
  return [
    orbitTarget[0] + Math.sin(yaw) * cp * orbitDistance,
    orbitTarget[1] + Math.sin(pitch) * orbitDistance,
    orbitTarget[2] + Math.cos(yaw) * cp * orbitDistance
  ];
}
function flyForward() {
  const cp = Math.cos(flyPitch);
  return normalize([Math.sin(flyYaw) * cp, Math.sin(flyPitch), Math.cos(flyYaw) * cp]);
}
function orbitForwardHorizontal() {
  const eye = orbitEye();
  return normalize([orbitTarget[0] - eye[0], 0, orbitTarget[2] - eye[2]]);
}

function resize() {
  const dpr = Math.min(window.devicePixelRatio || 1, 1.35);
  const w = Math.max(1, Math.floor(innerWidth * dpr));
  const h = Math.max(1, Math.floor(innerHeight * dpr));
  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w;
    canvas.height = h;
    gl.viewport(0, 0, w, h);
    draw();
  }
}

function draw() {
  if (!renderer || !manifest) return;
  gl.clearColor(0.027, 0.063, 0.047, 1);
  gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  const view = mat4.create();
  if (cameraMode === 'fly') {
    const forward = flyForward();
    mat4.lookAt(view, flyPosition, add(flyPosition, forward), [0, 1, 0]);
  } else {
    mat4.lookAt(view, orbitEye(), orbitTarget, [0, 1, 0]);
  }
  renderer.drawStructure(view);
}

function activeLayerConfig() {
  const layers = (currentBuild && currentBuild.layers) || [];
  return layers.find(layer => layer.id === currentLayer) || null;
}

function activeSpaceConfig() {
  const spaces = (manifest && manifest.reviewSpaces) || [];
  return spaces.find(space => space.id === currentSpace) || null;
}

function included(block) {
  if (block[1] > Number(slice.value)) return false;
  const space = activeSpaceConfig();
  if (space) {
    return block[0] >= space.min[0] && block[0] <= space.max[0]
      && block[1] >= space.min[1] && block[1] <= space.max[1]
      && block[2] >= space.min[2] && block[2] <= space.max[2];
  }
  const layer = activeLayerConfig();
  if (!layer || layer.id === 'all') return true;
  if (typeof layer.minY === 'number' && block[1] < layer.minY) return false;
  if (typeof layer.maxY === 'number' && block[1] > layer.maxY) return false;
  if (layer.cutawayAxis === 'x') {
    const split = typeof layer.cutawayAt === 'number' ? layer.cutawayAt : (manifest.min[0] + manifest.max[0]) / 2;
    return layer.cutawaySide === 'negative' ? block[0] <= split : block[0] >= split;
  }
  if (layer.cutawayAxis === 'z') {
    const split = typeof layer.cutawayAt === 'number' ? layer.cutawayAt : (manifest.min[2] + manifest.max[2]) / 2;
    return layer.cutawaySide === 'negative' ? block[2] <= split : block[2] >= split;
  }
  return true;
}

function propertiesOf(entry) { return entry.properties || {}; }
function shifted(block) { return [block[0] - manifest.min[0], block[1] - manifest.min[1], block[2] - manifest.min[2]]; }
function makeStructure() {
  const structure = new Structure(manifest.size);
  for (const block of manifest.blocks) {
    if (!included(block)) continue;
    const state = manifest.palette[block[3]];
    structure.addBlock(shifted(block), state.id, propertiesOf(state));
  }
  return structure;
}

function rebuild(message) {
  if (!resources || !manifest) return;
  progress(94, 'Rebuilding exact Minecraft view', message || 'Applying review filter to the live-server manifest…');
  if (loading) loading.style.display = 'grid';
  renderer = new StructureRenderer(gl, makeStructure(), resources);
  if (loading) loading.style.display = 'none';
  draw();
}

function paletteCounts() {
  const counts = new Map();
  for (const block of manifest.blocks) {
    const id = manifest.palette[block[3]].id;
    counts.set(id, (counts.get(id) || 0) + 1);
  }
  return Array.from(counts.entries()).sort((a, b) => b[1] - a[1]);
}

function fillMetadata() {
  document.getElementById('build-title').textContent = manifest.displayName || currentBuild.name;
  document.getElementById('classification').textContent = currentBuild.classification || 'BUILD REVIEW';
  document.getElementById('truth-label').textContent = 'EXACT LIVE-SERVER BLOCKSTATE GEOMETRY';
  document.getElementById('block-count').textContent = manifest.blockCount.toLocaleString();
  document.getElementById('palette-count').textContent = manifest.palette.length.toLocaleString();
  document.getElementById('bounds').textContent = manifest.size.join(' × ');
  document.getElementById('hash').textContent = manifest.geometrySha256.slice(0, 12);
  document.getElementById('authority').textContent = 'Authority: ' + manifest.geometryAuthority
    + ' · Minecraft ' + manifest.minecraftVersion + ' · SHA-256 ' + manifest.geometrySha256 + '.';
  document.getElementById('sources').textContent = 'Production sources: ' + (manifest.productionSources || []).join(' + ') + '.';
  slice.min = manifest.min[1];
  slice.max = manifest.max[1];
  slice.value = manifest.max[1];
  sliceValue.value = manifest.max[1];
  const p = document.getElementById('palette');
  p.replaceChildren();
  for (const [id, count] of paletteCounts()) {
    const a = document.createElement('span'); a.textContent = id.replace('minecraft:', '');
    const b = document.createElement('span'); b.textContent = count.toLocaleString();
    p.append(a, b);
  }
}

function populateLayers() {
  layerButtons.replaceChildren();
  const layers = currentBuild.layers && currentBuild.layers.length ? currentBuild.layers : [{ id: 'all', label: 'All' }];
  if (!layers.some(layer => layer.id === currentLayer)) currentLayer = 'all';
  for (const layer of layers) {
    const button = document.createElement('button');
    button.className = 'btn' + (layer.id === currentLayer ? ' active' : '');
    button.textContent = layer.label;
    button.dataset.layer = layer.id;
    button.addEventListener('click', () => {
      currentLayer = layer.id;
      currentSpace = '';
      spaceSelect.value = '';
      populateLayers();
      rebuild('Applying ' + layer.label + ' to the exact block manifest…');
    });
    layerButtons.appendChild(button);
  }
}

function populateSpaces() {
  const spaces = manifest.reviewSpaces || [];
  spaceSelect.replaceChildren();
  const all = document.createElement('option');
  all.value = '';
  all.textContent = spaces.length ? 'Whole build' : 'No authored subspaces';
  spaceSelect.appendChild(all);
  for (const space of spaces) {
    const option = document.createElement('option');
    option.value = space.id;
    option.textContent = space.name;
    spaceSelect.appendChild(option);
  }
  currentSpace = '';
  spaceSelect.value = '';
  spaceSelect.disabled = spaces.length === 0;
  spaceRow.classList.toggle('hidden', spaces.length === 0);
}

function focusCurrentSelection() {
  const space = activeSpaceConfig();
  if (space) {
    const focusWorld = space.focus || [
      (space.min[0] + space.max[0]) / 2,
      (space.min[1] + space.max[1]) / 2,
      (space.min[2] + space.max[2]) / 2
    ];
    orbitTarget = worldToLocal(focusWorld);
    orbitDistance = Math.max(18, Math.max(space.max[0] - space.min[0], space.max[2] - space.min[2]) * 1.75);
    pitch = 0.24;
    yaw = -0.35;
  } else {
    orbitTarget = localCenter();
    orbitTarget[1] = Math.max(3, manifest.size[1] * 0.38);
    orbitDistance = Math.max(60, Math.max(manifest.size[0], manifest.size[2]) * 1.55);
    pitch = 0.52;
    yaw = -0.72;
  }
  if (cameraMode === 'fly') {
    const eye = orbitEye();
    flyPosition = eye.slice();
    const toward = normalize([
      orbitTarget[0] - eye[0], orbitTarget[1] - eye[1], orbitTarget[2] - eye[2]
    ]);
    flyPitch = Math.asin(clamp(toward[1], -1, 1));
    flyYaw = Math.atan2(toward[0], toward[2]);
  }
  draw();
}

function resetCamera() {
  focusCurrentSelection();
}

function setCameraMode(mode) {
  if (mode === cameraMode) return;
  if (mode === 'fly') {
    const eye = orbitEye();
    const toward = normalize([orbitTarget[0] - eye[0], orbitTarget[1] - eye[1], orbitTarget[2] - eye[2]]);
    flyPosition = eye.slice();
    flyPitch = Math.asin(clamp(toward[1], -1, 1));
    flyYaw = Math.atan2(toward[0], toward[2]);
    cameraMode = 'fly';
  } else {
    const forward = flyForward();
    orbitTarget = add(flyPosition, scale(forward, Math.max(14, orbitDistance * 0.45)));
    orbitDistance = Math.max(24, Math.min(180, orbitDistance));
    yaw = Math.atan2(flyPosition[0] - orbitTarget[0], flyPosition[2] - orbitTarget[2]);
    pitch = Math.asin(clamp((flyPosition[1] - orbitTarget[1]) / orbitDistance, -0.98, 0.98));
    cameraMode = 'orbit';
  }
  document.querySelectorAll('[data-camera]').forEach(button => button.classList.toggle('active', button.dataset.camera === cameraMode));
  flyPad.classList.toggle('visible', cameraMode === 'fly');
  tip.textContent = cameraMode === 'fly'
    ? 'FIRST PERSON · click/drag to look · WASD move · Q/E vertical · Shift boost'
    : 'ORBIT · drag rotate · right-drag/two fingers pan · WASD/QE move focus · wheel/pinch dolly';
  draw();
}

function moveCamera(dt) {
  if (!manifest || keys.size === 0) return false;
  const boost = keys.has('ShiftLeft') || keys.has('ShiftRight') ? 3 : 1;
  const base = cameraMode === 'fly' ? 18 : Math.max(8, orbitDistance * 0.18);
  const amount = base * boost * dt;
  let moved = false;
  if (cameraMode === 'fly') {
    const forward = flyForward();
    const right = normalize(cross([0, 1, 0], forward));
    let delta = [0, 0, 0];
    if (keys.has('KeyW') || keys.has('ArrowUp')) delta = add(delta, forward);
    if (keys.has('KeyS') || keys.has('ArrowDown')) delta = add(delta, scale(forward, -1));
    if (keys.has('KeyD') || keys.has('ArrowRight')) delta = add(delta, right);
    if (keys.has('KeyA') || keys.has('ArrowLeft')) delta = add(delta, scale(right, -1));
    if (keys.has('KeyE') || keys.has('Space')) delta[1] += 1;
    if (keys.has('KeyQ')) delta[1] -= 1;
    if (length(delta) > 0) {
      flyPosition = add(flyPosition, scale(normalize(delta), amount));
      moved = true;
    }
  } else {
    const forward = orbitForwardHorizontal();
    const right = normalize(cross([0, 1, 0], forward));
    let delta = [0, 0, 0];
    if (keys.has('KeyW') || keys.has('ArrowUp')) delta = add(delta, forward);
    if (keys.has('KeyS') || keys.has('ArrowDown')) delta = add(delta, scale(forward, -1));
    if (keys.has('KeyD') || keys.has('ArrowRight')) delta = add(delta, right);
    if (keys.has('KeyA') || keys.has('ArrowLeft')) delta = add(delta, scale(right, -1));
    if (keys.has('KeyE') || keys.has('Space')) delta[1] += 1;
    if (keys.has('KeyQ')) delta[1] -= 1;
    if (length(delta) > 0) {
      orbitTarget = add(orbitTarget, scale(normalize(delta), amount));
      moved = true;
    }
  }
  return moved;
}

function frame(now) {
  const dt = Math.min(0.05, (now - lastFrame) / 1000 || 0);
  lastFrame = now;
  if (moveCamera(dt)) draw();
  requestAnimationFrame(frame);
}
requestAnimationFrame(frame);

canvas.addEventListener('contextmenu', event => event.preventDefault());
canvas.addEventListener('pointerdown', event => {
  if (canvas.setPointerCapture) canvas.setPointerCapture(event.pointerId);
  pointers.set(event.pointerId, [event.clientX, event.clientY]);
  dragKind = (event.button === 2 || event.shiftKey) ? 'pan' : 'rotate';
  if (cameraMode === 'fly' && event.pointerType === 'mouse' && canvas.requestPointerLock) {
    canvas.requestPointerLock().catch(function () {});
  }
});
canvas.addEventListener('pointermove', event => {
  if (!pointers.has(event.pointerId)) return;
  const prior = pointers.get(event.pointerId);
  pointers.set(event.pointerId, [event.clientX, event.clientY]);
  const pts = Array.from(pointers.values());
  const dx = event.clientX - prior[0];
  const dy = event.clientY - prior[1];

  if (cameraMode === 'fly') {
    if (document.pointerLockElement !== canvas) {
      flyYaw -= dx / 220;
      flyPitch = clamp(flyPitch - dy / 220, -1.52, 1.52);
      draw();
    }
    return;
  }

  if (pts.length === 1 && dragKind === 'rotate') {
    yaw -= dx / 160;
    pitch = clamp(pitch + dy / 160, -1.35, 1.35);
    draw();
  } else if (pts.length === 1 && dragKind === 'pan') {
    const forward = orbitForwardHorizontal();
    const right = normalize(cross([0, 1, 0], forward));
    const scalePx = orbitDistance / Math.max(400, innerHeight) * 1.6;
    orbitTarget = add(orbitTarget, add(scale(right, -dx * scalePx), [0, dy * scalePx, 0]));
    draw();
  } else if (pts.length >= 2) {
    const a = pts[0], b = pts[1];
    const dist = Math.hypot(a[0] - b[0], a[1] - b[1]);
    const center = [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
    if (lastPinch !== null) orbitDistance = clamp(orbitDistance - (dist - lastPinch) * 0.18, 2, 500);
    if (lastCenter) {
      const forward = orbitForwardHorizontal();
      const right = normalize(cross([0, 1, 0], forward));
      const panScale = orbitDistance / Math.max(400, innerHeight) * 1.4;
      orbitTarget = add(orbitTarget, add(
        scale(right, -(center[0] - lastCenter[0]) * panScale),
        [0, (center[1] - lastCenter[1]) * panScale, 0]
      ));
    }
    lastPinch = dist;
    lastCenter = center;
    draw();
  }
});
function releasePointer(event) {
  pointers.delete(event.pointerId);
  if (pointers.size < 2) { lastPinch = null; lastCenter = null; }
}
canvas.addEventListener('pointerup', releasePointer);
canvas.addEventListener('pointercancel', releasePointer);

document.addEventListener('mousemove', event => {
  if (cameraMode !== 'fly' || document.pointerLockElement !== canvas) return;
  flyYaw -= event.movementX / 420;
  flyPitch = clamp(flyPitch - event.movementY / 420, -1.52, 1.52);
  draw();
});

canvas.addEventListener('wheel', event => {
  event.preventDefault();
  if (cameraMode === 'fly') {
    flyPosition = add(flyPosition, scale(flyForward(), -event.deltaY * 0.035));
  } else {
    orbitDistance = clamp(orbitDistance + event.deltaY * 0.08, 2, 500);
  }
  draw();
}, { passive: false });

window.addEventListener('keydown', event => {
  if (event.target && /select|input|button/i.test(event.target.tagName)) return;
  const movement = ['KeyW','KeyA','KeyS','KeyD','KeyQ','KeyE','Space','ArrowUp','ArrowDown','ArrowLeft','ArrowRight','ShiftLeft','ShiftRight'];
  if (movement.includes(event.code)) {
    event.preventDefault();
    keys.add(event.code);
  }
  if (event.code === 'KeyF') setCameraMode(cameraMode === 'fly' ? 'orbit' : 'fly');
  if (event.code === 'KeyR') resetCamera();
});
window.addEventListener('keyup', event => keys.delete(event.code));
window.addEventListener('blur', () => keys.clear());

for (const button of document.querySelectorAll('[data-move]')) {
  const code = button.dataset.move;
  const on = event => { event.preventDefault(); keys.add(code); };
  const off = event => { event.preventDefault(); keys.delete(code); };
  button.addEventListener('pointerdown', on);
  button.addEventListener('pointerup', off);
  button.addEventListener('pointercancel', off);
  button.addEventListener('pointerleave', off);
}

async function localJson(url, label, cacheMode) {
  const response = await fetch(url, { cache: cacheMode || 'force-cache' });
  if (!response.ok) throw new Error(label + ' HTTP ' + response.status + ' (' + url + ')');
  return response.json();
}
async function localImage(file, label) {
  const url = assetBase + file;
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error(label + ' failed (' + url + ')'));
    image.src = url;
  });
}

async function loadResources() {
  if (resources) return resources;
  progress(22, 'Loading Minecraft block models', 'Reading pinned viewer assets from this Pages deployment…');
  const [blockstates, models, uvMap, atlas] = await Promise.all([
    localJson(assetBase + 'block-definitions.json', 'block definitions'),
    localJson(assetBase + 'block-models.json', 'block models'),
    localJson(assetBase + 'atlas-map.json', 'texture atlas map'),
    localImage('atlas.png', 'Minecraft texture atlas')
  ]);
  progress(55, 'Preparing Minecraft textures', 'Building the local block-model resource set…');
  const blockDefinitions = {};
  for (const id of Object.keys(blockstates)) blockDefinitions['minecraft:' + id] = BlockDefinition.fromJson(blockstates[id]);
  const blockModels = {};
  for (const id of Object.keys(models)) blockModels['minecraft:' + id] = BlockModel.fromJson(models[id]);
  Object.values(blockModels).forEach(model => model.flatten({ getBlockModel: id => blockModels[id] }));
  const atlasCanvas = document.createElement('canvas');
  const atlasSize = upperPowerOfTwo(Math.max(atlas.width, atlas.height));
  atlasCanvas.width = atlasSize;
  atlasCanvas.height = atlasSize;
  const ctx = atlasCanvas.getContext('2d', { willReadFrequently: true });
  if (!ctx) throw new Error('2D canvas unavailable for Minecraft atlas');
  ctx.drawImage(atlas, 0, 0);
  const atlasData = ctx.getImageData(0, 0, atlasSize, atlasSize);
  const idMap = {};
  for (const id of Object.keys(uvMap)) {
    const uv = uvMap[id];
    const u = uv[0], v = uv[1], du = uv[2], dv = uv[3];
    const dv2 = (du !== dv && id.startsWith('block/')) ? du : dv;
    idMap[Identifier.create(id).toString()] = [u / atlasSize, v / atlasSize, (u + du) / atlasSize, (v + dv2) / atlasSize];
  }
  const textureAtlas = new TextureAtlas(atlasData, idMap);
  resources = {
    getBlockDefinition: id => blockDefinitions[id.toString()],
    getBlockModel: id => blockModels[id.toString()],
    getTextureUV: id => textureAtlas.getTextureUV(id),
    getTextureAtlas: () => textureAtlas.getTextureAtlas(),
    getPixelSize: () => textureAtlas.getPixelSize(),
    getBlockFlags: () => ({ opaque: false }),
    getBlockProperties: () => null,
    getDefaultBlockProperties: () => null
  };
  return resources;
}

async function loadBuild(id) {
  const token = ++loadToken;
  const config = registry.builds.find(build => build.id === id) || registry.builds[0];
  if (!config) throw new Error('build registry is empty');
  currentBuild = config;
  currentLayer = 'all';
  currentSpace = '';
  if (errorBox) errorBox.style.display = 'none';
  if (loading) loading.style.display = 'grid';
  progress(8, 'Loading ' + config.name, 'Reading the exact live-server BlockState manifest…');
  const nextManifest = await localJson(config.manifest, 'exact block manifest', 'no-store');
  if (token !== loadToken) return;
  if (nextManifest.geometryAuthority !== 'live_server_final_blockstate_scan') throw new Error('manifest is not live-server authoritative');
  if (nextManifest.blockCount !== nextManifest.blocks.length) throw new Error('manifest block count mismatch');
  if (nextManifest.buildId !== config.id) throw new Error('registry/manifest build id mismatch');
  manifest = nextManifest;
  buildSelect.value = config.id;
  fillMetadata();
  populateLayers();
  populateSpaces();
  focusCurrentSelection();
  await loadResources();
  if (token !== loadToken) return;
  progress(82, 'Building exact voxel scene', 'Materializing ' + manifest.blockCount.toLocaleString() + ' Minecraft blocks…');
  resize();
  renderer = new StructureRenderer(gl, makeStructure(), resources);
  progress(100, 'Build ready', 'Use Orbit or First person to inspect the structure.');
  setTimeout(() => { if (loading) loading.style.display = 'none'; }, 120);
  const url = new URL(location.href);
  url.searchParams.set('build', config.id);
  history.replaceState(null, '', url);
  draw();
}

function populateBuilds() {
  buildSelect.replaceChildren();
  for (const build of registry.builds) {
    const option = document.createElement('option');
    option.value = build.id;
    option.textContent = build.name;
    buildSelect.appendChild(option);
  }
}

buildSelect.addEventListener('change', () => loadBuild(buildSelect.value).catch(fail));
spaceSelect.addEventListener('change', () => {
  currentSpace = spaceSelect.value;
  currentLayer = 'all';
  populateLayers();
  focusCurrentSelection();
  rebuild(currentSpace ? 'Isolating ' + activeSpaceConfig().name + ' from the authoritative manifest…' : 'Showing the whole build…');
});
slice.addEventListener('input', () => { sliceValue.value = slice.value; });
slice.addEventListener('change', () => rebuild('Applying vertical slice at Y=' + slice.value + '…'));
document.getElementById('focus').addEventListener('click', focusCurrentSelection);
document.getElementById('reset').addEventListener('click', resetCamera);
document.getElementById('info').addEventListener('click', () => { panel.style.display = panel.style.display === 'block' ? 'none' : 'block'; });
document.getElementById('hud-toggle').addEventListener('click', () => {
  hud.classList.toggle('collapsed');
  document.getElementById('hud-toggle').textContent = hud.classList.contains('collapsed') ? 'Menu' : 'Hide';
});
for (const button of document.querySelectorAll('[data-camera]')) {
  button.addEventListener('click', () => setCameraMode(button.dataset.camera));
}
window.addEventListener('resize', resize);

(async () => {
  try {
    progress(3, 'Loading Ouros Build Review', 'Reading the build registry…');
    registry = await localJson(registryUrl, 'build registry', 'no-store');
    if (!registry || !Array.isArray(registry.builds) || registry.builds.length === 0) throw new Error('build registry has no builds');
    populateBuilds();
    const requested = new URL(location.href).searchParams.get('build');
    const initial = registry.builds.some(build => build.id === requested) ? requested : registry.builds[0].id;
    await loadBuild(initial);
  } catch (error) {
    fail(error);
  }
})();

})();
