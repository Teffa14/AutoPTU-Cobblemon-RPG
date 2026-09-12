# Cobblemon Skin Blockbench-First Authoring Standard

Effective: 2026-08-31

This document is an authoritative extension of `docs/cobblemon-skin-art-direction.md` and `docs/cobblemon-skin-professional-pipeline.md`. Where older documentation implies that a Python builder should make premium artistic shape decisions, this newer rule wins.

The supported live agent bridge is defined in `docs/cobblemon-skin-mcp-toolchain.md`. Premium authoring should use the pinned Blockbench MCP bridge when a compatible local authoring environment is available.

## Core rule

Premium Ouros Cobblemon skin geometry is authored visually in Blockbench first.

Python, CI and validators may extract the official baseline, verify provenance, preserve anatomy, validate attachment, validate texture derivation, copy or normalize committed authored bytes, fingerprint evidence and reproduce the runtime package. They must not be the primary interface used to invent a premium costume's silhouette or solve its macro-form by repeatedly declaring `origin + size + rotation` cuboids.

A deterministic pipeline remains mandatory. Determinism now means:

`official pinned baseline + committed Blockbench-authored source + deterministic validation/materialization = exact production bytes`

It does not mean that an artistically significant form must be generated procedurally from Python literals.

## Live MCP authoring rule

When Blockbench MCP is reachable, the modeling agent should operate the actual Blockbench scene through MCP for geometry, mesh, hierarchy, UV, paint, camera, animation and export operations instead of translating visual decisions into a procedural Python model generator.

The reviewed MCP implementation exposes Blockbench-native project, cube, element, mesh, armature, texture, paint, UV, animation, camera, import/export and history tools. Mesh editing exists but several mesh operations are explicitly experimental upstream, so every novel mesh feature still requires Cobblemon runtime proof before production use.

Minecraft Dev MCP is a companion implementation-inspection tool. Use it to decompile/search Minecraft or a mod, inspect mappings and registry behavior, analyze a mod JAR and validate integration code. It never receives battle authority and it is not the artistic modeling surface.

If the local MCP bridge is unavailable, do not fall back to procedural art generation and claim the candidate is Blockbench-authored. The correct state is `AUTHORING ENVIRONMENT REQUIRED` until a real visual authoring pass occurs.

## Authoring source of truth

For a professional candidate, the manifest must identify a committed authored source and its SHA-256. Preferred source is the exact Blockbench-editable `.geo.json` used for the candidate when that preserves all required Bedrock semantics. A `.bbmodel` may be used only after the runtime pipeline proves a deterministic, version-pinned export path to the exact production `.geo.json`.

The production model must still be derived from the exact current official Cobblemon model extracted from the pinned stable JAR. All official biological bones remain JSON-equivalent and in original order. Only `ouros_*` geometry may be added.

Manual or MCP-driven Blockbench authoring never grants permission to alter official anatomy.

## Geometry language

Cuboids are allowed. High element count by itself is not a failure. The failure is macro-form construction that reads as infrastructure instead of a character.

Use large cuboids only for volumes that are supposed to read as solid blocks. Clothing, ribbons, fins, flames, torn edges, hat brims, apron panels, cap folds and other thin or flexible forms should preferentially use thin or zero/minimal-thickness Bedrock elements when runtime-safe, with directional changes, overlap and deliberate contour flow.

A successful low-poly form may use many small elements when each element contributes a controlled local change to contour, taper, depth or material. It must not use many near-identical large cuboids merely to approximate a curve by stair-stepping.

## Mandatory anti-stack review

Before a candidate can reach `OWNER REVIEW REQUIRED`, inspect its major cosmetic systems for all of the following failure modes:

- repeated large cuboids with nearly identical dimensions;
- repeated rotations that create a staircase or tiered tower;
- a dominant rectangular front face on cloth or organic material;
- stacked horizontal slabs used to fake a curved cap, hat, mantle or skirt;
- large orthogonal surfaces whose only purpose is to increase silhouette;
- insufficient taper between neighboring contour elements;
- insufficient directional variation along a curved or torn edge;
- macro-forms whose contact with anatomy is a narrow hidden point rather than a visually convincing root;
- detail added on top of a failed rectangular mass instead of rebuilding the mass.

If any dominant signature piece exhibits those patterns, the candidate is `ARTISTIC FAIL` or, after explicit owner rejection, `USER REJECTED — REWORK REQUIRED`. Do not rescue it by increasing cube count.

## Visual authoring loop

For each dominant signature piece:

1. Load the exact official current model, official/derived texture stack and applicable official animation in the pinned Blockbench version.
2. Lock the official biological hierarchy from artistic edits.
3. Author only `ouros_*` additions.
4. Establish one connected large mass or silhouette flow first.
5. Inspect front, side, rear and three-quarter views before adding small detail.
6. Inspect at gameplay scale before adding micro-detail.
7. Adjust contour, taper, overlap, negative space, depth and asymmetry visually.
8. Test attachment in every relevant official animation state available to the species.
9. Only after the macro-form works, add material breakup and smaller secondary pieces.
10. Export or save the exact authored source and let deterministic tooling validate/materialize it without redesigning it.

When MCP is active, the steps above are performed through the live Blockbench scene. Camera and animation tools are part of the modeling loop, not merely a final screenshot stage.

## Python builder/materializer boundary

Legacy procedural builders may remain in repository history and may continue to support neutral technical fixtures. They are not the artistic authoring path for new premium skin macro-forms.

For a Blockbench-first professional candidate, Python may:

- extract the exact official source from the pinned JAR;
- compare official bones, pivots, cubes, locators and UVs;
- merge an explicitly authored `ouros_*` section only when the merge is byte/semantic deterministic and does not change authored shape decisions;
- copy a committed authored model to production;
- generate deterministic metadata;
- validate derived textures;
- validate attachment and animation contact;
- calculate hashes and evidence metadata.

Python must not silently replace or procedurally approximate a rejected authored signature piece with a new stack of cuboids.

## Blockbench capability experiments

Runtime capabilities such as zero-thickness elements, very thin planes, unusual rotations or `poly_mesh` must be tested in an isolated technical fixture before relying on them in a production skin. An experimental feature is never assumed supported merely because Blockbench can display it or an MCP tool can create it.

A capability fixture must prove:

- Blockbench can load and display it;
- Cobblemon 1.7.3 can load/render it in the target runtime;
- official animations do not corrupt the attachment;
- the serialized format survives the deterministic pipeline;
- no validator is weakened to make the fixture pass.

Until a capability passes that test, use only already-proven Bedrock geometry features.

## Reference-study lesson from high-quality community renders

Other-species community renders may teach generic modeling technique but never count toward the mandatory same-species three-reference gate.

The useful general lesson is that strong Cobblemon models can obtain complex silhouettes while retaining the voxel/low-poly language by combining thin geometry, small contour contributions, changing directions, taper, negative space and strong silhouette hierarchy. The objective is not to eliminate cuboids. It is to stop using large repetitive cuboids as a substitute for shape design.

Do not copy the distinctive geometry, motifs, palettes, outfits or silhouettes of those examples unless an explicit compatible license permits derivative reuse.

## Lucario V40 rejection and V41 reset

The owner explicitly rejected the V40 visual solution on 2026-08-31. The V40 cap was implemented as a repeated stack of large cuboids and the apron used repeated box-like panels. Its status is `USER REJECTED — REWORK REQUIRED`, regardless of green CI.

V41 remains locked to Lucario. V41 must start from the exact current official Cobblemon 1.7.3 Lucario baseline and the already-valid same-species reference dossier. It must not inherit V40 cosmetic geometry as an artistic base. Any useful biological bytes come only from the official source; prior Ouros cosmetics are reference-only evidence of what failed.

The first V41 modeling objective is a Blockbench-authored cap and garment silhouette that demonstrates continuous contour flow without a stacked-slab read. No production candidate or owner-review request exists until new Blockbench evidence proves that material change.
