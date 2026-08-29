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
- the original species silhouette matches the unmodified release model;
- screenshots come from the external viewer, not from concept art or a project-authored geometry renderer.

## 0025 Pikachu — Storm Courier

Status: IMPLEMENTED / PREMIUM ACCESSORY PASS IN REVIEW

Storm Courier is built on the exact official Cobblemon 1.7.3 Pikachu male and female models. The obsolete 22-bone implementation is rejected and is not used as a template.

Official source pinned by CI:
- male geometry SHA-256: `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female geometry SHA-256: `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- base texture SHA-256: `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`
- official animation SHA-256: `d9ca00604978f295ad312d358a06f2655c725b30ac3da73c3637ae160c543384`
- original bones per gender model: `90`
- derived bones per gender model: `94`

The generator preserves all 90 official bones exactly and appends only four accessory bones: goggles, harness, pack and tail clamp. Male and female models are derived independently so the official female tail remains intact.

The premium pass deliberately adds detail inside those same four bones instead of adding anatomy: layered storm goggles with hardware; a three-dimensional crossed courier harness with a weather clasp; a compact expedition pack with pockets, bedroll, straps, buckles and storm mark; and a fitted grounding clamp on the official tail plane.

The resolver reuses `cobblemon:pikachu`, Cobblemon base/shiny textures and Cobblemon emissive layers. Ouros contributes only a transparent 128×64 accessory layer. The accessory palette occupies eight reserved texels on row `y=63`; CI verifies that those texels are outside the UV footprint of every original cube.

Visual review uses pinned Blockbench 5.1.6. Structural comparison uses matched-camera official vs Storm Courier front/left/right/back views. Presentation review imports official Pikachu animations through Blockbench's Bedrock codec and records official-reference, hero, battle-ready and walking three-quarter evidence.

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
