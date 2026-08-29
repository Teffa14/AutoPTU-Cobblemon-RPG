# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokemon variants.

## Mandatory model-source rule

For any Pokemon that already exists in Cobblemon, Ouros MUST begin from the exact original model distributed by the latest stable Cobblemon release used by this project. Do not rebuild anatomy. Do not substitute an older mirror. Do not approximate the model with generic cubes.

Current production target:
- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3
- source of truth: the released Cobblemon 1.7.3 Fabric JAR published on Modrinth, version id `kF7CvxTo`

A skin implementation may only change or append deliberate cosmetic geometry. Every original bone, cube, pivot, locator, hierarchy relationship and animation-facing name must remain unchanged unless a design explicitly requires a local modification and that exception is documented.

## Visual validation rule

The old in-repo Python software renderer is rejected as acceptance evidence.

New assets must be validated through an independent Minecraft model viewer. Blockbench is the primary review viewer because it supports Minecraft Bedrock Entity geometry, bone rotations, per-face UV, model animations and MoLang. The CI review path must load the generated `.geo.json` in Blockbench rather than reimplementing Bedrock transforms in project code.

A preview is evidence only when:
- the exact production `.geo.json` is loaded;
- the exact release texture is applied;
- any accessory texture used in production is applied;
- the model imports without geometry errors;
- the original species anatomy remains the unmodified release model;
- screenshots come from the external viewer, not from concept art or a project-authored geometry renderer.

## Epic-skin visual standard

Functional cosmetics are not enough. Ouros skins should read as premium variants immediately at gameplay distance.

A strong skin may push the silhouette aggressively with clothing, armor, equipment, mantles, collars, packs, fins, conductors, ornaments and other attached geometry, provided the original Pokemon anatomy remains intact underneath. The goal is not to hide the species; the goal is to make the variant unmistakable.

Epic skins should prioritize:
- a strong three-quarter silhouette;
- one clear visual fantasy or class identity;
- layered materials and visible depth rather than thin painted straps;
- meaningful asymmetry;
- large signature pieces supported by smaller hardware details;
- front, side and rear readability;
- motion-safe attachment to official animated parents;
- visual hierarchy: signature silhouette first, secondary equipment second, micro-detail last.

Do not reject a cosmetic merely because it changes the outer silhouette. Reject it when it rewrites anatomy, obscures species identity, detaches during official animations, creates severe clipping, or becomes visually incoherent.

## 0025 Pikachu — Storm Courier

Status: EPIC V3 IN BLOCKBENCH REVIEW

Storm Courier is built on the exact official Cobblemon 1.7.3 Pikachu male and female models. The obsolete 22-bone implementation is rejected and is not used as a template.

Official source pinned by CI:
- male geometry SHA-256: `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female geometry SHA-256: `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- base texture SHA-256: `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`
- official animation SHA-256: `d9ca00604978f295ad312d358a06f2655c725b30ac3da73c3637ae160c543384`
- original bones per gender model: `90`
- epic-v3 derived bones per gender model: `98`

The generator preserves all 90 official bones exactly and appends eight cosmetic bones. Male and female models are derived independently so the official female tail remains intact.

Epic v3 deliberately pushes the skin much harder than the earlier functional premium pass. Its signature silhouette is built from:
- a heavier storm visor/goggle assembly;
- an open-faced storm cowl with crown wings and jawline guards;
- layered shoulder mantle/pauldrons plus split rear storm tabs;
- a heavy crossed harness with an oversized storm-core chest unit;
- a larger expedition pack with bedroll, route case, storm vial and rear lightning sigil;
- twin storm-field pylons rising behind the shoulders;
- a reinforced tail grounding clamp;
- segmented tail conductor vanes.

These pieces are equipment around Pikachu. They do not replace head, torso, limbs, ears, muzzle, eyes or tail anatomy.

The resolver reuses `cobblemon:pikachu`, Cobblemon base/shiny textures and Cobblemon emissive layers. Ouros contributes only a transparent 128×64 accessory layer. The accessory palette occupies eight reserved texels on row `y=63`; CI verifies that those texels are outside the UV footprint of every original cube.

Visual acceptance uses pinned Blockbench 5.1.6. Structural comparison uses matched-camera official vs Storm Courier front/left/right/back views for male and female models. Presentation review imports the official Pikachu animations through Blockbench's Bedrock codec and records official-reference, hero, battle-ready and walking evidence.

Production files:
- `assets/cobblemon/bedrock/pokemon/models/0025_pikachu/ouros_storm_courier_pikachu_male.geo.json`
- `assets/cobblemon/bedrock/pokemon/models/0025_pikachu/ouros_storm_courier_pikachu_female.geo.json`
- `assets/cobblemon/bedrock/pokemon/resolvers/0025_pikachu/90_ouros_storm_courier.json`
- `assets/cobblemon/textures/pokemon/0025_pikachu/ouros_storm_courier_accessories.png`
- `data/cobblemon/species_features/ouros_storm_courier.json`
- `data/cobblemon/species_feature_assignments/ouros_pikachu_cosmetics.json`

Detailed provenance and design notes: `docs/cobblemon-skins/0025_pikachu/pikachu-storm-courier.md`.

## Authority boundary

All skin/model work is presentation-only. Cobblemon/Minecraft model, animation and rendering systems may be reused. Cobblemon battle state, participants, legality, HP/status, positions and combat authority remain outside this workflow; Ouros/AutoPTU remain authoritative for tactical battle facts.
