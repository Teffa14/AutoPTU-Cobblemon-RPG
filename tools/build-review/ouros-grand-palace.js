(function () {
'use strict';

window.__ourosViewerBooted = true;

const manifestUrl = '../build-data/ouros-grand-palace.blocks.json';
const assetBase = '../minecraft-assets/';
const canvas = document.getElementById('canvas');
const loading = document.getElementById('loading');
const loadTitle = document.getElementById('load-title');
const loadCopy = document.getElementById('load-copy');
const bar = document.getElementById('bar');
const errorBox = document.getElementById('error');
const slice = document.getElementById('slice');
const sliceValue = document.getElementById('slice-value');
const panel = document.getElementById('panel');
const roomSelect = document.getElementById('room-select');

const rooms = {
  antechamber: ['Antechamber', -11, 0, -53, 11, 29, -31],
  audience: ['Audience Chamber', -11, 0, -25, 11, 29, -3],
  themis: ['Themis Hall', -11, 0, 3, 11, 29, 25],
  cabinet: ['Cabinet', -39, 0, -53, -17, 14, -31],
  salla: ['Salla Terrena', 17, 0, -53, 39, 14, -31],
  blooming: ['Blooming Salon', -39, 0, -25, -17, 14, -3],
  hunting: ['Hunting Salon', 17, 0, -25, 39, 14, -3],
  library: ['Library', -39, 0, 3, -17, 14, 25],
  geography: ['Geography Cabinet', 17, 0, 3, 39, 14, 25],
  porcelain: ['Porcelain Hall', -39, 0, 31, -17, 14, 53],
  marble: ['Marble Salon', -11, 0, 31, 11, 29, 53],
  gallery: ['Gallery of Art', 17, 0, 31, 39, 14, 53],
  railings: ['Railings, Tables and Chairs', -39, 15, -53, -17, 29, -31],
  relief: ['Coat of Arms Relief Hall', 17, 15, -53, 39, 29, -31],
  accounting: ['Accounting Office', -39, 15, -25, -17, 29, -3],
  music: ['Music Chamber with Harpsichord', 17, 15, -25, 39, 29, -3],
  globe: ['Book Cabinet and Globe Room', -39, 15, 3, -17, 29, 25],
  blue: ['Blue Salon', 17, 15, 3, 39, 29, 25],
  banquet: ['Banquet Hall', -39, 15, 31, -17, 29, 53]
};

function fail(error) {
  console.error(error);
  if (loading) loading.style.display = 'none';
  if (errorBox) {
    errorBox.style.display = 'block';
    errorBox.textContent = 'Minecraft viewer error: ' + ((error && error.message) || error) + '. Geometry remains authoritative; rendering failed on this device.';
  }
}

const ds = window.deepslate;
const gm = window.glMatrix;
if (!ds) { fail(new Error('local Deepslate UMD runtime missing')); return; }
if (!gm || !gm.mat4) { fail(new Error('local gl-matrix UMD runtime missing')); return; }

const { BlockDefinition, BlockModel, Identifier, Structure, StructureRenderer, TextureAtlas, upperPowerOfTwo } = ds;
const { mat4 } = gm;
let manifest, resources, renderer, currentMode = 'all', currentRoom = '';
let xRotation = .67, yRotation = -.70, viewDist = 188, panX = 0, panY = -4;
let focus = [0, 22, 0];
const pointers = new Map();
let lastPinch = null, lastCenter = null;

function progress(percent, title, copy) {
  bar.style.width = percent + '%';
  if (title) loadTitle.textContent = title;
  if (copy) loadCopy.textContent = copy;
}
function propertiesOf(entry) { return entry.properties || {}; }
function shifted(block) { return [block[0] - manifest.min[0], block[1] - manifest.min[1], block[2] - manifest.min[2]]; }
function roomContains(block, room) {
  return block[0] >= room[1] && block[0] <= room[4]
    && block[1] >= room[2] && block[1] <= room[5]
    && block[2] >= room[3] && block[2] <= room[6];
}
function included(block) {
  const y = block[1], z = block[2], maxY = Number(slice.value);
  if (y > maxY) return false;
  if (currentRoom && rooms[currentRoom]) return roomContains(block, rooms[currentRoom]);
  if (currentMode === 'ground') return y >= -3 && y <= 14;
  if (currentMode === 'upper') return y >= 15 && y <= 29;
  if (currentMode === 'roof') return y >= 30;
  if (currentMode === 'ceremonial') return block[0] >= -12 && block[0] <= 12 && y <= 30;
  if (currentMode === 'cutaway') return z >= 0;
  return true;
}
function makeStructure() {
  const structure = new Structure(manifest.size);
  for (const block of manifest.blocks) {
    if (!included(block)) continue;
    const state = manifest.palette[block[3]];
    structure.addBlock(shifted(block), state.id, propertiesOf(state));
  }
  return structure;
}
function rebuild() {
  if (!resources) return;
  progress(95, 'Rebuilding Minecraft view', currentRoom ? 'Isolating ' + rooms[currentRoom][0] + ' from the authoritative manifest…' : 'Applying exact block filter…');
  renderer = new StructureRenderer(gl, makeStructure(), resources);
  loading.style.display = 'none';
  draw();
}

const gl = canvas.getContext('webgl', { antialias: true, alpha: false, preserveDrawingBuffer: false });
if (!gl) { fail(new Error('WebGL is unavailable')); return; }

function resize() {
  const dpr = Math.min(window.devicePixelRatio || 1, 1.25);
  const w = Math.max(1, Math.floor(innerWidth * dpr));
  const h = Math.max(1, Math.floor(innerHeight * dpr));
  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w; canvas.height = h; gl.viewport(0, 0, w, h); draw();
  }
}
function draw() {
  if (!renderer || !gl) return;
  gl.clearColor(0.027, 0.063, 0.047, 1);
  gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  const view = mat4.create();
  mat4.translate(view, view, [panX, panY, -viewDist]);
  mat4.rotate(view, view, xRotation, [1, 0, 0]);
  mat4.rotate(view, view, yRotation, [0, 1, 0]);
  const fx = focus[0] - manifest.min[0];
  const fy = focus[1] - manifest.min[1];
  const fz = focus[2] - manifest.min[2];
  mat4.translate(view, view, [-fx, -fy, -fz]);
  renderer.drawStructure(view);
}
function resetCamera() {
  xRotation = currentRoom ? .28 : .67;
  yRotation = currentRoom ? -.25 : -.70;
  viewDist = currentRoom ? 50 : Math.max(150, Math.max(manifest.size[0], manifest.size[2]) * 1.62);
  panX = 0; panY = currentRoom ? 0 : -4;
  if (currentRoom && rooms[currentRoom]) {
    const r = rooms[currentRoom];
    focus = [(r[1] + r[4]) / 2, (r[2] + r[5]) / 2, (r[3] + r[6]) / 2];
  } else {
    focus = [(manifest.min[0] + manifest.max[0]) / 2, 20, (manifest.min[2] + manifest.max[2]) / 2];
  }
  draw();
}

canvas.addEventListener('pointerdown', e => {
  if (canvas.setPointerCapture) canvas.setPointerCapture(e.pointerId);
  pointers.set(e.pointerId, [e.clientX, e.clientY]);
});
canvas.addEventListener('pointermove', e => {
  if (!pointers.has(e.pointerId)) return;
  const prior = pointers.get(e.pointerId);
  pointers.set(e.pointerId, [e.clientX, e.clientY]);
  const pts = Array.from(pointers.values());
  if (pts.length === 1) {
    yRotation += (e.clientX - prior[0]) / 110;
    xRotation += (e.clientY - prior[1]) / 110;
    xRotation = Math.max(-1.45, Math.min(1.45, xRotation));
    draw();
  } else if (pts.length >= 2) {
    const a = pts[0], b = pts[1];
    const dist = Math.hypot(a[0] - b[0], a[1] - b[1]);
    const center = [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
    if (lastPinch !== null) viewDist = Math.max(20, Math.min(320, viewDist - (dist - lastPinch) * .18));
    if (lastCenter) { panX += (center[0] - lastCenter[0]) * .035; panY -= (center[1] - lastCenter[1]) * .035; }
    lastPinch = dist; lastCenter = center; draw();
  }
});
function release(e) {
  pointers.delete(e.pointerId);
  if (pointers.size < 2) { lastPinch = null; lastCenter = null; }
}
canvas.addEventListener('pointerup', release);
canvas.addEventListener('pointercancel', release);
canvas.addEventListener('wheel', e => {
  e.preventDefault(); viewDist = Math.max(20, Math.min(320, viewDist + e.deltaY * .08)); draw();
}, { passive: false });

function paletteCounts() {
  const counts = new Map();
  for (const block of manifest.blocks) {
    const id = manifest.palette[block[3]].id;
    counts.set(id, (counts.get(id) || 0) + 1);
  }
  return Array.from(counts.entries()).sort((a, b) => b[1] - a[1]);
}
function fillMetadata() {
  document.getElementById('truth-label').textContent = 'EXACT LIVE-SERVER BLOCKSTATE GEOMETRY';
  document.getElementById('block-count').textContent = manifest.blockCount.toLocaleString();
  document.getElementById('palette-count').textContent = manifest.palette.length.toLocaleString();
  document.getElementById('bounds').textContent = manifest.size.join(' × ');
  document.getElementById('hash').textContent = manifest.geometrySha256.slice(0, 12);
  document.getElementById('authority').textContent = 'Authority: ' + manifest.geometryAuthority + ' · Minecraft ' + manifest.minecraftVersion + '. Geometry SHA-256: ' + manifest.geometrySha256;
  document.getElementById('sources').textContent = 'Production sources: ' + manifest.productionSources.join(' + ') + '.';
  slice.min = manifest.min[1]; slice.max = manifest.max[1]; slice.value = manifest.max[1]; sliceValue.value = manifest.max[1];
  const p = document.getElementById('palette'); p.replaceChildren();
  for (const [id, count] of paletteCounts()) {
    const a = document.createElement('span'); a.textContent = id.replace('minecraft:', '');
    const b = document.createElement('span'); b.textContent = count.toLocaleString(); p.append(a, b);
  }
}
async function localJson(file, label) {
  const url = assetBase + file;
  const response = await fetch(url, { cache: 'force-cache' });
  if (!response.ok) throw new Error(label + ' local asset HTTP ' + response.status + ' (' + url + ')');
  return response.json();
}
async function localImage(file, label) {
  const url = assetBase + file;
  return new Promise((resolve, reject) => {
    const image = new Image(); image.onload = () => resolve(image);
    image.onerror = () => reject(new Error(label + ' local asset failed (' + url + ')')); image.src = url;
  });
}
async function loadResources() {
  progress(28, 'Loading Minecraft block models', 'Reading pinned vanilla viewer assets from this GitHub Pages deploy…');
  const [blockstates, models, uvMap, atlas] = await Promise.all([
    localJson('block-definitions.json', 'block definitions'), localJson('block-models.json', 'block models'),
    localJson('atlas-map.json', 'texture atlas map'), localImage('atlas.png', 'Minecraft texture atlas')
  ]);
  progress(65, 'Preparing Minecraft textures', 'Building block models and texture atlas locally on this device…');
  const blockDefinitions = {};
  for (const id of Object.keys(blockstates)) blockDefinitions['minecraft:' + id] = BlockDefinition.fromJson(blockstates[id]);
  const blockModels = {};
  for (const id of Object.keys(models)) blockModels['minecraft:' + id] = BlockModel.fromJson(models[id]);
  Object.values(blockModels).forEach(model => model.flatten({ getBlockModel: id => blockModels[id] }));
  const atlasCanvas = document.createElement('canvas');
  const atlasSize = upperPowerOfTwo(Math.max(atlas.width, atlas.height));
  atlasCanvas.width = atlasSize; atlasCanvas.height = atlasSize;
  const ctx = atlasCanvas.getContext('2d', { willReadFrequently: true });
  if (!ctx) throw new Error('2D canvas unavailable for Minecraft atlas');
  ctx.drawImage(atlas, 0, 0);
  const atlasData = ctx.getImageData(0, 0, atlasSize, atlasSize);
  const idMap = {};
  for (const id of Object.keys(uvMap)) {
    const uv = uvMap[id]; const u = uv[0], v = uv[1], du = uv[2], dv = uv[3];
    const dv2 = (du !== dv && id.startsWith('block/')) ? du : dv;
    idMap[Identifier.create(id).toString()] = [u / atlasSize, v / atlasSize, (u + du) / atlasSize, (v + dv2) / atlasSize];
  }
  const textureAtlas = new TextureAtlas(atlasData, idMap);
  return {
    getBlockDefinition: id => blockDefinitions[id.toString()], getBlockModel: id => blockModels[id.toString()],
    getTextureUV: id => textureAtlas.getTextureUV(id), getTextureAtlas: () => textureAtlas.getTextureAtlas(),
    getPixelSize: () => textureAtlas.getPixelSize(), getBlockFlags: () => ({ opaque: false }),
    getBlockProperties: () => null, getDefaultBlockProperties: () => null
  };
}

for (const button of document.querySelectorAll('[data-mode]')) button.addEventListener('click', () => {
  document.querySelectorAll('[data-mode]').forEach(b => b.classList.remove('active'));
  button.classList.add('active'); currentMode = button.dataset.mode; currentRoom = ''; roomSelect.value = '';
  resetCamera(); loading.style.display = 'grid'; rebuild();
});
roomSelect.addEventListener('change', () => {
  currentRoom = roomSelect.value;
  if (currentRoom) currentMode = 'all';
  document.querySelectorAll('[data-mode]').forEach(b => b.classList.toggle('active', !currentRoom && b.dataset.mode === currentMode));
  resetCamera(); loading.style.display = 'grid'; rebuild();
});
document.getElementById('reset').addEventListener('click', resetCamera);
document.getElementById('info').addEventListener('click', () => { panel.style.display = panel.style.display === 'block' ? 'none' : 'block'; });
slice.addEventListener('input', () => { sliceValue.value = slice.value; });
slice.addEventListener('change', () => { loading.style.display = 'grid'; rebuild(); });
window.addEventListener('resize', resize);

(async () => {
  try {
    progress(10, 'Loading exact Grand Palace', 'Reading the live-server BlockState manifest…');
    const response = await fetch(manifestUrl, { cache: 'no-store' });
    if (!response.ok) throw new Error('exact palace manifest HTTP ' + response.status);
    manifest = await response.json();
    if (manifest.geometryAuthority !== 'live_server_final_blockstate_scan') throw new Error('manifest is not live-server authoritative');
    if (manifest.blockCount !== manifest.blocks.length) throw new Error('manifest block count mismatch');
    fillMetadata(); resources = await loadResources();
    progress(88, 'Building exact voxel palace', 'Materializing ' + manifest.blockCount.toLocaleString() + ' Minecraft blocks…');
    focus = [(manifest.min[0] + manifest.max[0]) / 2, 20, (manifest.min[2] + manifest.max[2]) / 2];
    viewDist = Math.max(150, Math.max(manifest.size[0], manifest.size[2]) * 1.62);
    resize(); renderer = new StructureRenderer(gl, makeStructure(), resources);
    progress(100, 'Grand Palace ready', 'Exact block geometry loaded. Use the room selector for interior review.');
    setTimeout(() => { loading.style.display = 'none'; }, 180); draw();
  } catch (error) { fail(error); }
})();

})();