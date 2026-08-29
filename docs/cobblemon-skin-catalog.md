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

Status: EPIC V3 ACCEPTED — BLOCKBENCH + PLAYABLE/CORE VALIDATED

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

Visual acceptance uses pinned Blockbench 5.1.6. Structural comparison uses matched-camera official vs Storm Courier front/left/right/back views for male and female models. Presentation review imports the official Pikachu animations through Blockbench's Bedrock codec and records official-reference, hero, battle-ready and walking evidence. The final epic-v3 review accepted the stronger three-quarter silhouette and confirmed that the equipment remains attached through the tested official idle, battle and walking frames without severe visual detachment.

Production files:
- `assets/cobblemon/bedrock/pokemon/models/0025_pikachu/ouros_storm_courier_pikachu_male.geo.json`
- `assets/cobblemon/bedrock/pokemon/models/0025_pikachu/ouros_storm_courier_pikachu_female.geo.json`
- `assets/cobblemon/bedrock/pokemon/resolvers/0025_pikachu/90_ouros_storm_courier.json`
- `assets/cobblemon/textures/pokemon/0025_pikachu/ouros_storm_courier_accessories.png`
- `data/cobblemon/species_features/ouros_storm_courier.json`
- `data/cobblemon/species_feature_assignments/ouros_pikachu_cosmetics.json`

Detailed provenance and design notes: `docs/cobblemon-skins/0025_pikachu/pikachu-storm-courier.md`.

## 0448 Lucario — Aura Sentinel

Status: EPIC ACCEPTED — BLOCKBENCH + PLAYABLE/CORE VALIDATED

Aura Sentinel is derived from the single normal Lucario geometry distributed in the exact official Cobblemon 1.7.3 Fabric JAR. CI pins the source model (`ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`), animation (`ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`), poser (`7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`), resolver (`a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`) and normal/shiny textures (`98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a` / `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`).

All 87 original Lucario bones remain JSON-equivalent and in order. Eight `ouros_*` cosmetic bones bring the derived model to 95 bones: crown, asymmetric left/right pauldrons, open aura-core frame, rear aura frame, two bracers and split waist mantle. The design leaves the official face, ears, aura sensors, central chest spike, limbs and tail intact.

The first real Blockbench 5.1.6 review accepted the visual direction: a ceremonial armored aura sentinel with stronger shoulder/head/waist hierarchy, visible asymmetry and a clear battle silhouette. Hero and battle-ready use official `ground_idle` and `battle_idle` Bedrock animations. Four-view structural evidence is also recorded. The official walking poser is procedural (`q.biped_walk` + `q.bimanual_swing` + `ground_idle`), so no fake walking clip is generated by the evidence pipeline.

The resolver reuses `cobblemon:lucario`, supports the official normal and shiny textures and adds only the transparent Aura Sentinel accessory overlay. UV palette texels are dynamically selected from positions proven unused by the original 128×64 geometry. No new emissive or particle runtime is claimed.

Detailed provenance, license preservation, geometry contract and review notes: `docs/cobblemon-skins/0448_lucario/lucario-aura-sentinel.md`.

## 0094 Gengar — Rift Warden

Status: EPIC ACCEPTED — BLOCKBENCH + PLAYABLE/CORE VALIDATED

Rift Warden is derived from the single official Gengar geometry in the Cobblemon 1.7.3 Fabric JAR. CI pins model `57449f9653a403a783efdffa3195eb6948aceb855411f4e77caaa9c29175ad38`, animation `68a8bf920086c6dc368a8ffb5c449aedb314aab03a2df7bdde10bd61ea0cdb9f`, resolver `aeecefe6571d99bc9ab38a3b22af5e34769b346ed9858335292c82209ee95afc` and the exact normal, shiny, emissive and Pokemopolis textures.

All 78 official Gengar bones remain JSON-equivalent and in order. Eight `ouros_*` cosmetic bones bring the production model to 86 bones with 44 cosmetic cubes. The signature silhouette uses a broken planar rift halo, asymmetric shoulder shrouds, rear collar guards, twin dimensional pylons, warded wrists and a split shadow mantle. Eyes, mouth states, tongue, ears, limbs, feet and tail remain the original model.

The resolver preserves all three official presentation branches: normal, shiny and `color-green`/Pokemopolis. Official emissive layers remain in the normal and shiny branches. The Ouros overlay occupies only eight proven UV-free texels on the 128×128 texture and does not repaint the body.

Real Blockbench 5.1.6 review accepted the silhouette after direct visual inspection. Matched-camera official vs skin evidence shows a strong front and three-quarter change without masking Gengar's eyes or grin. Official `ground_idle`, `air_idle` and `air_fly` clips keep the attached equipment coherent. The detached center halo fragment is intentional rift imagery and stays stable through the tested frames. The official animation JSON contains no battle or walking clip, so the pipeline does not fabricate either; `air_fly` is used as the locomotion evidence. Playable Fabric, Cobblemon Official Model Review and Integration Core all completed successfully on PR #298 before merge eligibility.

Detailed provenance, license, geometry and QA notes: `docs/cobblemon-skins/0094_gengar/gengar-rift-warden.md`.

## Authority boundary

All skin/model work is presentation-only. Cobblemon/Minecraft model, animation and rendering systems may be reused. Cobblemon battle state, participants, legality, HP/status, positions and combat authority remain outside this workflow; Ouros/AutoPTU remain authoritative for tactical battle facts.
