import fs from 'node:fs';
import path from 'node:path';

const [registryPath, outputDir, atlasMapPath, atlasPngPath] = process.argv.slice(2);
if (!registryPath || !outputDir || !atlasMapPath || !atlasPngPath) {
  console.error('usage: node compile-surface-mesh.mjs <registry.json> <output-dir> <atlas-map.json> <atlas.png>');
  process.exit(2);
}

const siteRoot = path.resolve(path.dirname(registryPath), '..');
const registry = JSON.parse(fs.readFileSync(registryPath, 'utf8'));
const atlasMap = JSON.parse(fs.readFileSync(atlasMapPath, 'utf8'));
const png = fs.readFileSync(atlasPngPath);
if (png.length < 24 || png.toString('ascii', 1, 4) !== 'PNG') throw new Error('atlas PNG is invalid');
const atlasWidth = png.readUInt32BE(16);
const atlasHeight = png.readUInt32BE(20);
fs.mkdirSync(outputDir, { recursive: true });

const DIRECTIONS = [
  { key: 'west',  d: [-1, 0, 0], n: [-1, 0, 0], corners: [[0,0,1],[0,0,0],[0,1,0],[0,1,1]] },
  { key: 'east',  d: [ 1, 0, 0], n: [ 1, 0, 0], corners: [[1,0,0],[1,0,1],[1,1,1],[1,1,0]] },
  { key: 'down',  d: [ 0,-1, 0], n: [ 0,-1, 0], corners: [[0,0,0],[1,0,0],[1,0,1],[0,0,1]] },
  { key: 'up',    d: [ 0, 1, 0], n: [ 0, 1, 0], corners: [[0,1,1],[1,1,1],[1,1,0],[0,1,0]] },
  { key: 'north', d: [ 0, 0,-1], n: [ 0, 0,-1], corners: [[1,0,0],[0,0,0],[0,1,0],[1,1,0]] },
  { key: 'south', d: [ 0, 0, 1], n: [ 0, 0, 1], corners: [[0,0,1],[1,0,1],[1,1,1],[0,1,1]] }
];
const TRI = [0,1,2, 0,2,3];

class VertexWriter {
  constructor() {
    this.buffer = Buffer.allocUnsafe(1024 * 1024);
    this.offset = 0;
    this.vertices = 0;
  }
  ensure(bytes) {
    if (this.offset + bytes <= this.buffer.length) return;
    let size = this.buffer.length;
    while (size < this.offset + bytes) size *= 2;
    const next = Buffer.allocUnsafe(size);
    this.buffer.copy(next, 0, 0, this.offset);
    this.buffer = next;
  }
  vertex(x, y, z, u, v, nx, ny, nz) {
    this.ensure(16);
    const o = this.offset;
    this.buffer.writeUInt16LE(Math.max(0, Math.min(65535, Math.round(x * 16))), o);
    this.buffer.writeUInt16LE(Math.max(0, Math.min(65535, Math.round(y * 16))), o + 2);
    this.buffer.writeUInt16LE(Math.max(0, Math.min(65535, Math.round(z * 16))), o + 4);
    this.buffer.writeUInt16LE(Math.max(0, Math.min(65535, Math.round(u * 65535))), o + 6);
    this.buffer.writeUInt16LE(Math.max(0, Math.min(65535, Math.round(v * 65535))), o + 8);
    this.buffer.writeInt8(Math.round(nx * 127), o + 10);
    this.buffer.writeInt8(Math.round(ny * 127), o + 11);
    this.buffer.writeInt8(Math.round(nz * 127), o + 12);
    this.buffer[o + 13] = 0;
    this.buffer[o + 14] = 0;
    this.buffer[o + 15] = 0;
    this.offset += 16;
    this.vertices++;
  }
  data() { return this.buffer.subarray(0, this.offset); }
}

function blockPath(id) { return id.replace(/^minecraft:/, ''); }
function key(x, y, z) { return `${x},${y},${z}`; }

function isTranslucent(id) {
  const p = blockPath(id);
  return /(glass|pane|water|ice|leaves|grate|barrier)/.test(p);
}

function isNonFull(id) {
  const p = blockPath(id);
  return /(stairs|slab|pane|fence|wall|door|trapdoor|lantern|chain|torch|rod|carpet|cauldron|rail|button|pressure_plate|flower|grass|sapling|vine|leaves|bars|ladder|sign|banner|bed|candle|pot|head|skull)/.test(p);
}

function isOccluder(state) {
  if (!state || isTranslucent(state.id) || isNonFull(state.id)) return false;
  return true;
}

function woodTexture(p) {
  for (const wood of ['dark_oak','oak','spruce','birch','jungle','acacia','mangrove','cherry','pale_oak','bamboo','crimson','warped']) {
    if (p.startsWith(wood + '_fence') || p.startsWith(wood + '_door') || p.startsWith(wood + '_trapdoor')) {
      return wood === 'bamboo' ? 'bamboo_planks' : `${wood}_planks`;
    }
  }
  return null;
}

function textureKey(id) {
  let p = blockPath(id);
  const candidates = [`block/${p}`];
  if (p.startsWith('waxed_')) p = p.slice(6);
  candidates.push(`block/${p}`);
  const wood = woodTexture(p);
  if (wood) candidates.push(`block/${wood}`);
  if (p.endsWith('_stained_glass_pane')) candidates.push(`block/${p.slice(0, -5)}`);
  if (p === 'glass_pane') candidates.push('block/glass');
  for (const suffix of ['_stairs','_slab','_wall']) {
    if (p.endsWith(suffix)) candidates.push(`block/${p.slice(0, -suffix.length)}`);
  }
  if (p.endsWith('_fence')) candidates.push(`block/${p.slice(0, -6)}_planks`);
  if (p.endsWith('_fence_gate')) candidates.push(`block/${p.slice(0, -11)}_planks`);
  if (p.endsWith('_bricks')) candidates.push(`block/${p}`);
  if (p === 'deepslate_tile_stairs' || p === 'deepslate_tile_slab') candidates.push('block/deepslate_tiles');
  if (p === 'copper_grate') candidates.push('block/copper_grate');
  for (const c of candidates) if (atlasMap[c]) return c;
  return atlasMap['block/stone'] ? 'block/stone' : Object.keys(atlasMap)[0];
}

function uvRect(texture) {
  const [x, y, w, h] = atlasMap[texture];
  const inset = 0.35;
  return [
    (x + inset) / atlasWidth,
    1 - (y + h - inset) / atlasHeight,
    (x + w - inset) / atlasWidth,
    1 - (y + inset) / atlasHeight
  ];
}

function stateProp(state, name, fallback) {
  return state.properties && state.properties[name] !== undefined ? state.properties[name] : fallback;
}

function geometryBoxes(state) {
  const p = blockPath(state.id);
  if (p.endsWith('_slab')) {
    const type = stateProp(state, 'type', 'bottom');
    if (type === 'double') return [[0,0,0,1,1,1]];
    return type === 'top' ? [[0,0.5,0,1,1,1]] : [[0,0,0,1,0.5,1]];
  }
  if (p.endsWith('_stairs')) {
    const half = stateProp(state, 'half', 'bottom');
    const facing = stateProp(state, 'facing', 'north');
    const low = half === 'top' ? [0,0.5,0,1,1,1] : [0,0,0,1,0.5,1];
    const y1 = half === 'top' ? 0 : 0.5;
    const y2 = half === 'top' ? 0.5 : 1;
    let high;
    if (facing === 'north') high = [0,y1,0,1,y2,0.5];
    else if (facing === 'south') high = [0,y1,0.5,1,y2,1];
    else if (facing === 'west') high = [0,y1,0,0.5,y2,1];
    else high = [0.5,y1,0,1,y2,1];
    return [low, high];
  }
  if (p.includes('pane') || p === 'iron_bars') {
    const boxes = [[0.4375,0,0.4375,0.5625,1,0.5625]];
    if (stateProp(state, 'north', 'false') === 'true') boxes.push([0.4375,0,0,0.5625,1,0.5]);
    if (stateProp(state, 'south', 'false') === 'true') boxes.push([0.4375,0,0.5,0.5625,1,1]);
    if (stateProp(state, 'west', 'false') === 'true') boxes.push([0,0,0.4375,0.5,1,0.5625]);
    if (stateProp(state, 'east', 'false') === 'true') boxes.push([0.5,0,0.4375,1,1,0.5625]);
    return boxes;
  }
  if (p.includes('fence') && !p.includes('gate')) {
    const boxes = [[0.375,0,0.375,0.625,1,0.625]];
    if (stateProp(state, 'north', 'false') === 'true') boxes.push([0.4375,0.35,0,0.5625,0.85,0.5]);
    if (stateProp(state, 'south', 'false') === 'true') boxes.push([0.4375,0.35,0.5,0.5625,0.85,1]);
    if (stateProp(state, 'west', 'false') === 'true') boxes.push([0,0.35,0.4375,0.5,0.85,0.5625]);
    if (stateProp(state, 'east', 'false') === 'true') boxes.push([0.5,0.35,0.4375,1,0.85,0.5625]);
    return boxes;
  }
  if (p.endsWith('_door')) {
    const facing = stateProp(state, 'facing', 'north');
    return (facing === 'east' || facing === 'west') ? [[0.4375,0,0,0.5625,1,1]] : [[0,0,0.4375,1,1,0.5625]];
  }
  if (p.endsWith('_trapdoor')) {
    const half = stateProp(state, 'half', 'bottom');
    const open = stateProp(state, 'open', 'false') === 'true';
    if (!open) return half === 'top' ? [[0,0.8125,0,1,1,1]] : [[0,0,0,1,0.1875,1]];
    const facing = stateProp(state, 'facing', 'north');
    if (facing === 'north') return [[0,0,0,1,1,0.1875]];
    if (facing === 'south') return [[0,0,0.8125,1,1,1]];
    if (facing === 'west') return [[0,0,0,0.1875,1,1]];
    return [[0.8125,0,0,1,1,1]];
  }
  if (p.includes('lantern')) return [[0.3125,0.125,0.3125,0.6875,0.75,0.6875]];
  if (p.includes('chain')) return [[0.4375,0,0.4375,0.5625,1,0.5625]];
  if (p.includes('carpet')) return [[0,0,0,1,0.0625,1]];
  return [[0,0,0,1,1,1]];
}

function isFullBox(box) {
  return box[0] === 0 && box[1] === 0 && box[2] === 0 && box[3] === 1 && box[4] === 1 && box[5] === 1;
}

function faceCorners(box, dir) {
  const [x1,y1,z1,x2,y2,z2] = box;
  return dir.corners.map(([x,y,z]) => [x ? x2 : x1, y ? y2 : y1, z ? z2 : z1]);
}

function emitFace(writer, block, box, dir, uv) {
  const corners = faceCorners(box, dir);
  const tex = [[uv[0],uv[1]],[uv[2],uv[1]],[uv[2],uv[3]],[uv[0],uv[3]]];
  for (const i of TRI) {
    const c = corners[i];
    const t = tex[i];
    writer.vertex(block[0] + c[0], block[1] + c[1], block[2] + c[2], t[0], t[1], ...dir.n);
  }
}

function compileBuild(build) {
  const manifestPath = path.resolve(siteRoot, build.manifest);
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  if (manifest.geometryAuthority !== 'live_server_final_blockstate_scan') throw new Error(`${build.id}: non-authoritative source manifest`);
  if (manifest.blockCount !== manifest.blocks.length) throw new Error(`${build.id}: blockCount mismatch`);

  const states = manifest.palette;
  const occupancy = new Map();
  for (const b of manifest.blocks) occupancy.set(key(b[0], b[1], b[2]), b[3]);
  const opaque = new VertexWriter();
  const translucent = new VertexWriter();
  const paletteCounts = new Map();
  let exposedBlocks = 0;
  let culledInteriorBlocks = 0;
  let faceCount = 0;

  for (const b of manifest.blocks) {
    const state = states[b[3]];
    paletteCounts.set(state.id, (paletteCounts.get(state.id) || 0) + 1);
    const boxes = geometryBoxes(state);
    const texture = textureKey(state.id);
    const uv = uvRect(texture);
    const writer = isTranslucent(state.id) ? translucent : opaque;
    let emittedForBlock = 0;

    for (const box of boxes) {
      const full = isFullBox(box);
      for (const dir of DIRECTIONS) {
        if (full) {
          const ni = occupancy.get(key(b[0] + dir.d[0], b[1] + dir.d[1], b[2] + dir.d[2]));
          if (ni !== undefined) {
            const neighbor = states[ni];
            if ((isOccluder(state) && isOccluder(neighbor)) || (neighbor.id === state.id && isFullBox(geometryBoxes(neighbor)[0]))) continue;
          }
        }
        emitFace(writer, b, box, dir, uv);
        emittedForBlock++;
        faceCount++;
      }
    }
    if (emittedForBlock) exposedBlocks++; else culledInteriorBlocks++;
  }

  const opaqueName = `${build.id}.opaque.bin`;
  const translucentName = `${build.id}.translucent.bin`;
  fs.writeFileSync(path.join(outputDir, opaqueName), opaque.data());
  fs.writeFileSync(path.join(outputDir, translucentName), translucent.data());

  const descriptor = {
    format: 'ouros.minecraft.surface-mesh.v1',
    buildId: manifest.buildId,
    displayName: manifest.displayName || build.name,
    minecraftVersion: manifest.minecraftVersion,
    geometryAuthority: manifest.geometryAuthority,
    sourceGeometrySha256: manifest.geometrySha256,
    blockCount: manifest.blockCount,
    paletteCount: manifest.palette.length,
    captureEnvelopeAudit: manifest.captureEnvelopeAudit,
    floatingComponentAudit: manifest.floatingComponentAudit,
    min: manifest.min,
    max: manifest.max,
    size: manifest.size,
    productionSources: manifest.productionSources || [],
    authoredSpaces: manifest.authoredSpaces || [],
    reviewSpaces: manifest.reviewSpaces || [],
    paletteCounts: Array.from(paletteCounts.entries()).sort((a,b) => b[1] - a[1]),
    renderMode: 'server_precomputed_surface_mesh',
    visualContract: 'Exact block count, bounds and source SHA come from the Fabric live-server manifest. Browser geometry is a non-interactive precomputed surface mesh and does not materialize BlockState objects.',
    vertexFormat: { stride: 16, positionScale: 16, uvNormalizedU16: true, normalNormalizedI8: true },
    atlas: 'minecraft-assets/atlas.png',
    opaque: { url: `build-mesh/${opaqueName}`, vertices: opaque.vertices, bytes: opaque.offset },
    translucent: { url: `build-mesh/${translucentName}`, vertices: translucent.vertices, bytes: translucent.offset },
    meshStats: { sourceBlocks: manifest.blockCount, exposedBlocks, culledInteriorBlocks, visibleFaces: faceCount }
  };
  fs.writeFileSync(path.join(outputDir, `${build.id}.mesh.json`), JSON.stringify(descriptor));
  console.log(`${build.id}: ${manifest.blockCount} source blocks -> ${exposedBlocks} exposed blocks, ${faceCount} faces, ${(opaque.offset + translucent.offset).toLocaleString()} mesh bytes`);
}

for (const build of registry.builds) compileBuild(build);
