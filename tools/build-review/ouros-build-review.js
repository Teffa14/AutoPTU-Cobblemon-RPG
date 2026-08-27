(function () {
'use strict';

window.__ourosViewerBooted = true;

const registryUrl = 'build-data/builds.json';
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
      + '. The authoritative Fabric block manifest remains valid; the low-memory review mesh failed.';
  }
}

const gm = window.glMatrix;
if (!gm || !gm.mat4) { fail(new Error('local gl-matrix UMD runtime missing')); return; }
const { mat4 } = gm;
const gl = canvas.getContext('webgl', { antialias: true, alpha: false, preserveDrawingBuffer: false });
if (!gl) { fail(new Error('WebGL is unavailable')); return; }

let registry = null;
let currentBuild = null;
let manifest = null;
let renderer = null;
let atlasTexture = null;
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
function normalize(v) { const n = length(v) || 1; return [v[0] / n, v[1] / n, v[2] / n]; }
function cross(a, b) { return [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]; }
function worldToLocal(v) { return [v[0]-manifest.min[0], v[1]-manifest.min[1], v[2]-manifest.min[2]]; }
function localCenter() { return [manifest.size[0]/2, manifest.size[1]/2, manifest.size[2]/2]; }
function orbitEye() {
  const cp = Math.cos(pitch);
  return [orbitTarget[0] + Math.sin(yaw)*cp*orbitDistance,
    orbitTarget[1] + Math.sin(pitch)*orbitDistance,
    orbitTarget[2] + Math.cos(yaw)*cp*orbitDistance];
}
function flyForward() {
  const cp = Math.cos(flyPitch);
  return normalize([Math.sin(flyYaw)*cp, Math.sin(flyPitch), Math.cos(flyYaw)*cp]);
}
function orbitForwardHorizontal() {
  const eye = orbitEye();
  return normalize([orbitTarget[0]-eye[0], 0, orbitTarget[2]-eye[2]]);
}

function compileShader(type, source) {
  const shader = gl.createShader(type);
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(shader) || 'shader compile failed');
  return shader;
}
function createProgram() {
  const vs = compileShader(gl.VERTEX_SHADER, `
    attribute vec3 aPosition;
    attribute vec2 aUv;
    attribute vec3 aNormal;
    uniform mat4 uMvp;
    uniform float uPositionScale;
    varying vec2 vUv;
    varying vec3 vPos;
    varying float vLight;
    void main() {
      vec3 p = aPosition / uPositionScale;
      vPos = p;
      vUv = aUv;
      vec3 n = normalize(aNormal);
      vLight = 0.58 + 0.42 * max(0.0, dot(n, normalize(vec3(0.35, 0.85, 0.4))));
      gl_Position = uMvp * vec4(p, 1.0);
    }
  `);
  const fs = compileShader(gl.FRAGMENT_SHADER, `
    precision mediump float;
    uniform sampler2D uAtlas;
    uniform vec3 uClipMin;
    uniform vec3 uClipMax;
    uniform float uSliceMax;
    uniform float uAlphaMode;
    varying vec2 vUv;
    varying vec3 vPos;
    varying float vLight;
    void main() {
      if (vPos.x < uClipMin.x || vPos.y < uClipMin.y || vPos.z < uClipMin.z ||
          vPos.x > uClipMax.x || vPos.y > uClipMax.y || vPos.z > uClipMax.z ||
          vPos.y > uSliceMax) discard;
      vec4 tex = texture2D(uAtlas, vUv);
      if (tex.a < 0.08) discard;
      if (uAlphaMode < 0.5 && tex.a < 0.98) tex.a = 1.0;
      gl_FragColor = vec4(tex.rgb * vLight, tex.a);
    }
  `);
  const program = gl.createProgram();
  gl.attachShader(program, vs); gl.attachShader(program, fs); gl.linkProgram(program);
  gl.deleteShader(vs); gl.deleteShader(fs);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(program) || 'shader link failed');
  return program;
}

class SurfaceMeshRenderer {
  constructor(descriptor, texture) {
    this.descriptor = descriptor;
    this.texture = texture;
    this.program = createProgram();
    this.opaque = null;
    this.translucent = null;
    this.aPosition = gl.getAttribLocation(this.program, 'aPosition');
    this.aUv = gl.getAttribLocation(this.program, 'aUv');
    this.aNormal = gl.getAttribLocation(this.program, 'aNormal');
    this.uMvp = gl.getUniformLocation(this.program, 'uMvp');
    this.uPositionScale = gl.getUniformLocation(this.program, 'uPositionScale');
    this.uAtlas = gl.getUniformLocation(this.program, 'uAtlas');
    this.uClipMin = gl.getUniformLocation(this.program, 'uClipMin');
    this.uClipMax = gl.getUniformLocation(this.program, 'uClipMax');
    this.uSliceMax = gl.getUniformLocation(this.program, 'uSliceMax');
    this.uAlphaMode = gl.getUniformLocation(this.program, 'uAlphaMode');
  }
  async loadPart(part) {
    if (!part || !part.vertices || !part.bytes) return null;
    const response = await fetch(part.url, { cache: 'force-cache' });
    if (!response.ok) throw new Error('surface mesh HTTP ' + response.status + ' (' + part.url + ')');

    const buffer = gl.createBuffer();
    if (!buffer) throw new Error('WebGL could not allocate surface mesh buffer');
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, part.bytes, gl.STATIC_DRAW);
    let received = 0;

    try {
      // Stream network chunks directly into the already-sized GPU VBO. This prevents JavaScript
      // from retaining a full second copy of a large Palace mesh during upload on memory-limited
      // browsers. Older browsers fall back to one temporary Uint8Array.
      if (response.body && typeof response.body.getReader === 'function') {
        const reader = response.body.getReader();
        while (true) {
          const chunk = await reader.read();
          if (chunk.done) break;
          const value = chunk.value;
          if (!value || value.byteLength === 0) continue;
          if (received + value.byteLength > part.bytes) throw new Error('surface mesh exceeded declared byte count for ' + part.url);
          gl.bufferSubData(gl.ARRAY_BUFFER, received, value);
          received += value.byteLength;
        }
      } else {
        const data = new Uint8Array(await response.arrayBuffer());
        if (data.byteLength > part.bytes) throw new Error('surface mesh exceeded declared byte count for ' + part.url);
        gl.bufferSubData(gl.ARRAY_BUFFER, 0, data);
        received = data.byteLength;
      }
      if (received !== part.bytes) throw new Error('surface mesh byte count mismatch for ' + part.url + ': expected ' + part.bytes + ', received ' + received);
    } catch (error) {
      gl.deleteBuffer(buffer);
      throw error;
    }

    return { buffer, vertices: part.vertices, bytes: part.bytes };
  }
  async load() {
    // Load sequentially to keep the transient network/decoder peak bounded to one mesh part.
    this.opaque = await this.loadPart(this.descriptor.opaque);
    this.translucent = await this.loadPart(this.descriptor.translucent);
    gl.bindBuffer(gl.ARRAY_BUFFER, null);
  }
  dispose() {
    for (const part of [this.opaque, this.translucent]) if (part && part.buffer) gl.deleteBuffer(part.buffer);
    if (this.program) gl.deleteProgram(this.program);
    this.opaque = null; this.translucent = null; this.program = null;
  }
  drawPart(part, alphaMode) {
    if (!part || !part.vertices) return;
    gl.bindBuffer(gl.ARRAY_BUFFER, part.buffer);
    gl.enableVertexAttribArray(this.aPosition);
    gl.enableVertexAttribArray(this.aUv);
    gl.enableVertexAttribArray(this.aNormal);
    gl.vertexAttribPointer(this.aPosition, 3, gl.UNSIGNED_SHORT, false, 16, 0);
    gl.vertexAttribPointer(this.aUv, 2, gl.UNSIGNED_SHORT, true, 16, 6);
    gl.vertexAttribPointer(this.aNormal, 3, gl.BYTE, true, 16, 10);
    gl.uniform1f(this.uAlphaMode, alphaMode);
    gl.drawArrays(gl.TRIANGLES, 0, part.vertices);
  }
  draw(view, clipMin, clipMax, sliceMax) {
    gl.useProgram(this.program);
    const projection = mat4.create();
    mat4.perspective(projection, Math.PI / 3, canvas.width / Math.max(1, canvas.height), 0.05, 1200);
    const mvp = mat4.create();
    mat4.multiply(mvp, projection, view);
    gl.uniformMatrix4fv(this.uMvp, false, mvp);
    gl.uniform1f(this.uPositionScale, this.descriptor.vertexFormat.positionScale || 16);
    gl.uniform3fv(this.uClipMin, clipMin);
    gl.uniform3fv(this.uClipMax, clipMax);
    gl.uniform1f(this.uSliceMax, sliceMax);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, this.texture);
    gl.uniform1i(this.uAtlas, 0);
    gl.enable(gl.DEPTH_TEST);
    // Surface faces are intentionally visible from both sides. This keeps first-person room review
    // and shader cutaways stable even when a baked face's winding differs from Minecraft's model.
    gl.disable(gl.CULL_FACE);
    gl.disable(gl.BLEND);
    gl.depthMask(true);
    this.drawPart(this.opaque, 0);
    if (this.translucent && this.translucent.vertices) {
      gl.enable(gl.BLEND);
      gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
      gl.depthMask(false);
      this.drawPart(this.translucent, 1);
      gl.depthMask(true);
      gl.disable(gl.BLEND);
    }
    gl.bindBuffer(gl.ARRAY_BUFFER, null);
  }
}

async function localJson(url, label, cacheMode) {
  const response = await fetch(url, { cache: cacheMode || 'force-cache' });
  if (!response.ok) throw new Error(label + ' HTTP ' + response.status + ' (' + url + ')');
  return response.json();
}
async function loadAtlas() {
  if (atlasTexture) return atlasTexture;
  progress(28, 'Loading compact texture atlas', 'The browser no longer loads Minecraft block definitions or model JSON.');
  const image = await new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Minecraft texture atlas failed'));
    img.src = 'minecraft-assets/atlas.png';
  });
  const texture = gl.createTexture();
  gl.bindTexture(gl.TEXTURE_2D, texture);
  gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  gl.bindTexture(gl.TEXTURE_2D, null);
  atlasTexture = texture;
  return texture;
}

function activeLayerConfig() {
  const layers = (currentBuild && currentBuild.layers) || [];
  return layers.find(layer => layer.id === currentLayer) || null;
}
function activeSpaceConfig() {
  const spaces = (manifest && manifest.reviewSpaces) || [];
  return spaces.find(space => space.id === currentSpace) || null;
}
function reviewClip() {
  let min = [0, 0, 0];
  let max = [manifest.size[0], manifest.size[1], manifest.size[2]];
  const space = activeSpaceConfig();
  if (space) {
    min = worldToLocal(space.min);
    max = worldToLocal([space.max[0] + 1, space.max[1] + 1, space.max[2] + 1]);
  } else {
    const layer = activeLayerConfig();
    if (layer) {
      if (typeof layer.minY === 'number') min[1] = layer.minY - manifest.min[1];
      if (typeof layer.maxY === 'number') max[1] = layer.maxY - manifest.min[1] + 1;
      if (layer.cutawayAxis === 'x') {
        const split = typeof layer.cutawayAt === 'number' ? layer.cutawayAt : (manifest.min[0] + manifest.max[0]) / 2;
        if (layer.cutawaySide === 'negative') max[0] = split - manifest.min[0] + 1;
        else min[0] = split - manifest.min[0];
      }
      if (layer.cutawayAxis === 'z') {
        const split = typeof layer.cutawayAt === 'number' ? layer.cutawayAt : (manifest.min[2] + manifest.max[2]) / 2;
        if (layer.cutawaySide === 'negative') max[2] = split - manifest.min[2] + 1;
        else min[2] = split - manifest.min[2];
      }
    }
  }
  return { min, max };
}

function resize() {
  const dpr = Math.min(window.devicePixelRatio || 1, 1.15);
  const w = Math.max(1, Math.floor(innerWidth * dpr));
  const h = Math.max(1, Math.floor(innerHeight * dpr));
  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w; canvas.height = h; gl.viewport(0, 0, w, h); draw();
  }
}
function draw() {
  if (!renderer || !manifest) return;
  gl.clearColor(0.027, 0.063, 0.047, 1);
  gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  const view = mat4.create();
  if (cameraMode === 'fly') mat4.lookAt(view, flyPosition, add(flyPosition, flyForward()), [0,1,0]);
  else mat4.lookAt(view, orbitEye(), orbitTarget, [0,1,0]);
  const clip = reviewClip();
  const sliceLocal = Number(slice.value) - manifest.min[1] + 1;
  renderer.draw(view, clip.min, clip.max, sliceLocal);
}

function paletteCounts() { return manifest.paletteCounts || []; }
function fillMetadata() {
  document.getElementById('build-title').textContent = manifest.displayName || currentBuild.name;
  document.getElementById('classification').textContent = currentBuild.classification || 'BUILD REVIEW';
  document.getElementById('truth-label').textContent = 'EXACT SERVER BLOCK DATA · LOW-MEMORY SURFACE MESH';
  document.getElementById('block-count').textContent = manifest.blockCount.toLocaleString();
  document.getElementById('palette-count').textContent = manifest.paletteCount.toLocaleString();
  document.getElementById('bounds').textContent = manifest.size.join(' × ');
  document.getElementById('hash').textContent = manifest.sourceGeometrySha256.slice(0, 12);
  const stats = manifest.meshStats || {};
  document.getElementById('authority').textContent = 'Authority: ' + manifest.geometryAuthority
    + ' · Minecraft ' + manifest.minecraftVersion + ' · source SHA-256 ' + manifest.sourceGeometrySha256
    + ' · review mesh ' + Number(stats.visibleFaces || 0).toLocaleString() + ' visible faces.';
  document.getElementById('sources').textContent = 'Production sources: ' + (manifest.productionSources || []).join(' + ')
    + '. Browser does not materialize the source BlockState array.';
  slice.min = manifest.min[1]; slice.max = manifest.max[1]; slice.value = manifest.max[1]; sliceValue.value = manifest.max[1];
  const p = document.getElementById('palette'); p.replaceChildren();
  for (const [id, count] of paletteCounts()) {
    const a = document.createElement('span'); a.textContent = id.replace('minecraft:', '');
    const b = document.createElement('span'); b.textContent = count.toLocaleString();
    p.append(a, b);
  }
}
function populateLayers() {
  layerButtons.replaceChildren();
  const layers = currentBuild.layers && currentBuild.layers.length ? currentBuild.layers : [{ id:'all', label:'All' }];
  if (!layers.some(layer => layer.id === currentLayer)) currentLayer = 'all';
  for (const layer of layers) {
    const button = document.createElement('button');
    button.className = 'btn' + (layer.id === currentLayer ? ' active' : '');
    button.textContent = layer.label; button.dataset.layer = layer.id;
    button.addEventListener('click', () => {
      currentLayer = layer.id; currentSpace = ''; spaceSelect.value = ''; populateLayers(); draw();
    });
    layerButtons.appendChild(button);
  }
}
function populateSpaces() {
  const spaces = manifest.reviewSpaces || [];
  spaceSelect.replaceChildren();
  const all = document.createElement('option'); all.value = ''; all.textContent = spaces.length ? 'Whole build' : 'No authored subspaces';
  spaceSelect.appendChild(all);
  for (const space of spaces) { const option = document.createElement('option'); option.value = space.id; option.textContent = space.name; spaceSelect.appendChild(option); }
  currentSpace = ''; spaceSelect.value = ''; spaceSelect.disabled = spaces.length === 0; spaceRow.classList.toggle('hidden', spaces.length === 0);
}
function focusCurrentSelection() {
  const space = activeSpaceConfig();
  if (space) {
    const focusWorld = space.focus || [(space.min[0]+space.max[0])/2,(space.min[1]+space.max[1])/2,(space.min[2]+space.max[2])/2];
    orbitTarget = worldToLocal(focusWorld);
    orbitDistance = Math.max(18, Math.max(space.max[0]-space.min[0], space.max[2]-space.min[2]) * 1.75);
    pitch = 0.24; yaw = -0.35;
  } else {
    orbitTarget = localCenter(); orbitTarget[1] = Math.max(3, manifest.size[1] * 0.38);
    orbitDistance = Math.max(60, Math.max(manifest.size[0], manifest.size[2]) * 1.55);
    pitch = 0.52; yaw = -0.72;
  }
  if (cameraMode === 'fly') {
    const eye = orbitEye(); flyPosition = eye.slice();
    const toward = normalize([orbitTarget[0]-eye[0], orbitTarget[1]-eye[1], orbitTarget[2]-eye[2]]);
    flyPitch = Math.asin(clamp(toward[1], -1, 1)); flyYaw = Math.atan2(toward[0], toward[2]);
  }
  draw();
}
function resetCamera() { focusCurrentSelection(); }
function setCameraMode(mode) {
  if (mode === cameraMode) return;
  if (mode === 'fly') {
    const eye = orbitEye(); const toward = normalize([orbitTarget[0]-eye[0], orbitTarget[1]-eye[1], orbitTarget[2]-eye[2]]);
    flyPosition = eye.slice(); flyPitch = Math.asin(clamp(toward[1], -1, 1)); flyYaw = Math.atan2(toward[0], toward[2]); cameraMode = 'fly';
  } else {
    const forward = flyForward(); orbitTarget = add(flyPosition, scale(forward, Math.max(14, orbitDistance * 0.45)));
    orbitDistance = Math.max(24, Math.min(180, orbitDistance));
    yaw = Math.atan2(flyPosition[0]-orbitTarget[0], flyPosition[2]-orbitTarget[2]);
    pitch = Math.asin(clamp((flyPosition[1]-orbitTarget[1])/orbitDistance, -0.98, 0.98)); cameraMode = 'orbit';
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
    const forward = flyForward(); const right = normalize(cross([0,1,0], forward)); let delta = [0,0,0];
    if (keys.has('KeyW') || keys.has('ArrowUp')) delta = add(delta, forward);
    if (keys.has('KeyS') || keys.has('ArrowDown')) delta = add(delta, scale(forward,-1));
    if (keys.has('KeyD') || keys.has('ArrowRight')) delta = add(delta, right);
    if (keys.has('KeyA') || keys.has('ArrowLeft')) delta = add(delta, scale(right,-1));
    if (keys.has('KeyE') || keys.has('Space')) delta[1] += 1; if (keys.has('KeyQ')) delta[1] -= 1;
    if (length(delta) > 0) { flyPosition = add(flyPosition, scale(normalize(delta), amount)); moved = true; }
  } else {
    const forward = orbitForwardHorizontal(); const right = normalize(cross([0,1,0], forward)); let delta = [0,0,0];
    if (keys.has('KeyW') || keys.has('ArrowUp')) delta = add(delta, forward);
    if (keys.has('KeyS') || keys.has('ArrowDown')) delta = add(delta, scale(forward,-1));
    if (keys.has('KeyD') || keys.has('ArrowRight')) delta = add(delta, right);
    if (keys.has('KeyA') || keys.has('ArrowLeft')) delta = add(delta, scale(right,-1));
    if (keys.has('KeyE') || keys.has('Space')) delta[1] += 1; if (keys.has('KeyQ')) delta[1] -= 1;
    if (length(delta) > 0) { orbitTarget = add(orbitTarget, scale(normalize(delta), amount)); moved = true; }
  }
  return moved;
}
function frame(now) { const dt = Math.min(0.05, (now-lastFrame)/1000 || 0); lastFrame = now; if (moveCamera(dt)) draw(); requestAnimationFrame(frame); }
requestAnimationFrame(frame);

canvas.addEventListener('contextmenu', event => event.preventDefault());
canvas.addEventListener('pointerdown', event => {
  if (canvas.setPointerCapture) canvas.setPointerCapture(event.pointerId);
  pointers.set(event.pointerId, [event.clientX,event.clientY]); dragKind = (event.button === 2 || event.shiftKey) ? 'pan' : 'rotate';
  if (cameraMode === 'fly' && event.pointerType === 'mouse' && canvas.requestPointerLock) canvas.requestPointerLock().catch(function () {});
});
canvas.addEventListener('pointermove', event => {
  if (!pointers.has(event.pointerId)) return;
  const prior = pointers.get(event.pointerId); pointers.set(event.pointerId,[event.clientX,event.clientY]);
  const pts = Array.from(pointers.values()); const dx = event.clientX-prior[0]; const dy = event.clientY-prior[1];
  if (cameraMode === 'fly') { if (document.pointerLockElement !== canvas) { flyYaw -= dx/220; flyPitch = clamp(flyPitch-dy/220,-1.52,1.52); draw(); } return; }
  if (pts.length === 1 && dragKind === 'rotate') { yaw -= dx/160; pitch = clamp(pitch+dy/160,-1.35,1.35); draw(); }
  else if (pts.length === 1 && dragKind === 'pan') {
    const forward = orbitForwardHorizontal(); const right = normalize(cross([0,1,0],forward)); const s = orbitDistance/Math.max(400,innerHeight)*1.6;
    orbitTarget = add(orbitTarget, add(scale(right,-dx*s),[0,dy*s,0])); draw();
  } else if (pts.length >= 2) {
    const a=pts[0],b=pts[1]; const dist=Math.hypot(a[0]-b[0],a[1]-b[1]); const center=[(a[0]+b[0])/2,(a[1]+b[1])/2];
    if (lastPinch !== null) orbitDistance = clamp(orbitDistance-(dist-lastPinch)*0.18,2,500);
    if (lastCenter) { const forward=orbitForwardHorizontal(); const right=normalize(cross([0,1,0],forward)); const s=orbitDistance/Math.max(400,innerHeight)*1.4;
      orbitTarget=add(orbitTarget,add(scale(right,-(center[0]-lastCenter[0])*s),[0,(center[1]-lastCenter[1])*s,0])); }
    lastPinch=dist; lastCenter=center; draw();
  }
});
function releasePointer(event) { pointers.delete(event.pointerId); if (pointers.size < 2) { lastPinch=null; lastCenter=null; } }
canvas.addEventListener('pointerup', releasePointer); canvas.addEventListener('pointercancel', releasePointer);
document.addEventListener('mousemove', event => { if (cameraMode !== 'fly' || document.pointerLockElement !== canvas) return; flyYaw -= event.movementX/420; flyPitch=clamp(flyPitch-event.movementY/420,-1.52,1.52); draw(); });
canvas.addEventListener('wheel', event => { event.preventDefault(); if (cameraMode === 'fly') flyPosition=add(flyPosition,scale(flyForward(),-event.deltaY*0.035)); else orbitDistance=clamp(orbitDistance+event.deltaY*0.08,2,500); draw(); }, { passive:false });
window.addEventListener('keydown', event => {
  if (event.target && /select|input|button/i.test(event.target.tagName)) return;
  const movement=['KeyW','KeyA','KeyS','KeyD','KeyQ','KeyE','Space','ArrowUp','ArrowDown','ArrowLeft','ArrowRight','ShiftLeft','ShiftRight'];
  if (movement.includes(event.code)) { event.preventDefault(); keys.add(event.code); }
  if (event.code === 'KeyF') setCameraMode(cameraMode === 'fly' ? 'orbit' : 'fly'); if (event.code === 'KeyR') resetCamera();
});
window.addEventListener('keyup', event => keys.delete(event.code)); window.addEventListener('blur', () => keys.clear());
for (const button of document.querySelectorAll('[data-move]')) {
  const code=button.dataset.move; const on=event=>{event.preventDefault();keys.add(code);}; const off=event=>{event.preventDefault();keys.delete(code);};
  button.addEventListener('pointerdown',on); button.addEventListener('pointerup',off); button.addEventListener('pointercancel',off); button.addEventListener('pointerleave',off);
}

async function loadBuild(id) {
  const token = ++loadToken;
  const config = registry.builds.find(build => build.id === id) || registry.builds[0];
  if (!config) throw new Error('build registry is empty');
  currentBuild = config; currentLayer = 'all'; currentSpace = '';
  if (errorBox) errorBox.style.display = 'none'; if (loading) loading.style.display = 'grid';
  progress(8, 'Loading ' + config.name, 'Reading compact server-precomputed review metadata…');
  const nextManifest = await localJson('build-mesh/' + config.id + '.mesh.json', 'surface mesh descriptor', 'no-store');
  if (token !== loadToken) return;
  if (nextManifest.format !== 'ouros.minecraft.surface-mesh.v1') throw new Error('unsupported surface mesh format');
  if (nextManifest.geometryAuthority !== 'live_server_final_blockstate_scan') throw new Error('mesh descriptor lost live-server authority');
  if (nextManifest.buildId !== config.id) throw new Error('registry/mesh build id mismatch');
  if (!nextManifest.vertexFormat || nextManifest.vertexFormat.coordinates !== 'capture_local') throw new Error('surface mesh coordinates are not capture-local');

  // Free the prior build before allocating any VBO for the next one. Holding both scenes at once was
  // a major source of avoidable peak memory when switching between the Gym and the Palace.
  if (renderer) {
    renderer.dispose();
    renderer = null;
    gl.clearColor(0.027, 0.063, 0.047, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  }

  manifest = nextManifest; buildSelect.value = config.id; fillMetadata(); populateLayers(); populateSpaces(); focusCurrentSelection(); resize();
  const texture = await loadAtlas(); if (token !== loadToken) return;
  progress(55, 'Streaming optimized 3D surface', 'Streaming precomputed visible surfaces directly into GPU buffers…');
  const nextRenderer = new SurfaceMeshRenderer(manifest, texture);
  try {
    await nextRenderer.load();
  } catch (error) {
    nextRenderer.dispose();
    throw error;
  }
  if (token !== loadToken) { nextRenderer.dispose(); return; }
  renderer = nextRenderer;
  progress(100, 'Build ready', 'Filters use GPU clipping. No BlockState scene rebuild occurs in this browser.');
  setTimeout(() => { if (loading) loading.style.display = 'none'; }, 100);
  const url = new URL(location.href); url.searchParams.set('build', config.id); history.replaceState(null,'',url); draw();
}
function populateBuilds() { buildSelect.replaceChildren(); for (const build of registry.builds) { const option=document.createElement('option'); option.value=build.id; option.textContent=build.name; buildSelect.appendChild(option); } }

buildSelect.addEventListener('change', () => loadBuild(buildSelect.value).catch(fail));
spaceSelect.addEventListener('change', () => { currentSpace=spaceSelect.value; currentLayer='all'; populateLayers(); focusCurrentSelection(); draw(); });
slice.addEventListener('input', () => { sliceValue.value=slice.value; draw(); });
slice.addEventListener('change', draw);
document.getElementById('focus').addEventListener('click', focusCurrentSelection);
document.getElementById('reset').addEventListener('click', resetCamera);
document.getElementById('info').addEventListener('click', () => { panel.style.display = panel.style.display === 'block' ? 'none' : 'block'; });
document.getElementById('hud-toggle').addEventListener('click', () => { hud.classList.toggle('collapsed'); document.getElementById('hud-toggle').textContent = hud.classList.contains('collapsed') ? 'Menu' : 'Hide'; });
for (const button of document.querySelectorAll('[data-camera]')) button.addEventListener('click', () => setCameraMode(button.dataset.camera));
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
  } catch (error) { fail(error); }
})();

})();