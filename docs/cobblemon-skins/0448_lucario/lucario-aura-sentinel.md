# 0448 Lucario — Aura Sentinel

Status: FULL TRANSFORMATION ACCEPTED ARTISTICALLY; PR/PLAYABLE/CORE VALIDATION PENDING

Aura Sentinel is an Ouros presentation-only cosmetic built from the exact Lucario assets distributed in the official Cobblemon 1.7.3 Fabric JAR for Minecraft 1.21.1. It does not implement or consume Cobblemon battle-state authority.

## Exact official source

- Cobblemon release: 1.7.3
- Minecraft target: 1.21.1
- Modrinth version id: `kF7CvxTo`
- official JAR: `Cobblemon-fabric-1.7.3+1.21.1.jar`
- JAR SHA-256: `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- JAR SHA-512: `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`
- official model: `assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json`
- model SHA-256: `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`
- animation SHA-256: `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`
- poser SHA-256: `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`
- base resolver SHA-256: `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`
- official normal texture SHA-256: `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- official shiny texture SHA-256: `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- model license file SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`

The exact model license extracted from the official JAR is preserved at `official-model-license.txt`. Distribution and use of the adapted model must remain compatible with those terms.

## Why the old pass was superseded

The original 95-bone Aura Sentinel pass was technically valid but was authored under the old accessory-first standard. It used UV-free palette texels only and relied on a crown, pauldrons, bracers and rear frame placed over an otherwise dominant base Lucario read.

Issue #311 and the full-transformation standard from PR #309 supersede that result artistically. The old pass remains engineering history only.

## V2 geometry contract

The official model contains 87 bones. Aura Sentinel v2 preserves all 87 original bones JSON-equivalently and in the same order. No original head, muzzle, eyes, ears, aura sensors, chest spike, torso, limbs, hands, feet or tail are replaced or rewritten.

V2 appends eight cosmetic bones, producing 95 total:

- `ouros_aura_helm_system`
- `ouros_aura_mantle_shell`
- `ouros_aura_breastplate`
- `ouros_aura_shrine_frame`
- `ouros_aura_left_armguard`
- `ouros_aura_right_armguard`
- `ouros_aura_waistcoat`
- `ouros_aura_relic_fin`

The refined accepted pass contains 87 cosmetic cubes distributed across those eight macro groups. The intent is not cube count for its own sake; the cubes are composed into readable larger forms.

## Signature design

The fantasy is a ceremonial aura knight / shrine sentinel.

The dominant signature structures are:

1. an integrated open-face helm and horizontal aura visor that changes the head silhouette while keeping Lucario's face, ears and sensors identifiable;
2. a broad ceremonial mantle and breastplate system that creates one continuous armored upper-body read instead of isolated shoulder blocks;
3. a dorsal shrine/halo frame with layered vertical fins and side aura blades that gives a strong rear and three-quarter silhouette.

The secondary macro-form is a long split waistcoat/mantle with asymmetrical color/material treatment. Armguards and relic hardware support the larger silhouette instead of acting as the design by themselves.

## Full-surface textures

V2 uses deliberate derived normal and shiny textures from the exact official 128×64 textures. The official UV layout and model UV coordinates are unchanged. Transparency semantics remain unchanged.

Derived normal texture:

- `ouros_aura_sentinel.png`
- SHA-256 `1cbb1ca7fe260d01a4e0ca7a2f0a28ea424475f856267caf19d0b4276ed19752`
- 6,163 occupied baseline pixels intentionally changed

Derived shiny texture:

- `ouros_aura_sentinel_shiny.png`
- SHA-256 `7d391c01daba8634a4cfd84cc17f1f37385afe473ed1ff578d989c66fa5cb725`

Normal material language: midnight indigo/cobalt, obsidian, ivory, gold and aura-cyan.

Shiny deliberately shifts to graphite, cool silver, amethyst and brighter aura accents. It is not a silent reuse of the normal texture.

The accessory overlay remains additive for the new geometry and occupies validated UV-free texels. It does not replace the full-surface body treatment.

## Resolver and forms

The resolver keeps the official `cobblemon:lucario` poser and uses the same derived model for normal and shiny branches with deliberately separate derived textures.

The official Cobblemon 1.7.3 resolver exposes one standard Lucario geometry for this presentation path; there is no male/female geometry split to duplicate. Mega Lucario is not claimed or modified by this cosmetic.

No custom emissive runtime, particles or battle behavior are claimed by this slice.

## Blockbench evidence

Primary viewer: Blockbench 5.1.6.

Pinned Blockbench SHA-256: `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

Refined review run: GitHub Actions `Aura Sentinel V2 Refined Full Transformation Review`, run `33285682959`.

Artifact: `aura-sentinel-v2-refined-blockbench-review`, artifact id `9724359565`, artifact SHA-256 digest `84e6e47b81dc80cf3bb290e2a5b84e190ed2827ca2e1f7b854fbc7477e1abccf`.

The review loaded the exact production model, exact derived textures and official Lucario animation file through Blockbench's Bedrock animation codec. Official reference and variant reused the same camera target, position, orthographic zoom and scale.

Evidence set:

- `official_reference_three_quarter.png` — SHA-256 `e1fcfc4fe881ca8a1ce263bd6c89c572e42741adcbe78202e629559261f1839d`
- `hero_three_quarter.png` — SHA-256 `95a6b7feb2fb1a812e41c07a4cc5e9470b0030d6e5b4d4a31edc84721053139f`
- `battle_ready_three_quarter.png` — SHA-256 `db7da2ca4d31561e59791231b6e8bf4d4eeed1be2fe539ff8788f7ca296cfc04`
- `hero_front.png` — SHA-256 `27e980e6c6461580b51d1ca8ecf1ee88c581cfb40eb3cd4a61d6b36b8d1d872c`
- `hero_left.png` — SHA-256 `a73674be9a5e2642e10382e644248a44b307ce393a4b268f34f9cb7264fe4952`
- `hero_right.png` — SHA-256 `9689f3730471756a0d1d4545c1aba268c8ab2fb47b14f60fea82a30bb57ba5a6`
- `hero_back.png` — SHA-256 `b6c0744e495b61efca736d1cbe9cd24b3746176f1a7bb9ba013ee75261a875be`
- `official_reference_gameplay_160.png` — SHA-256 `717546a43a59baadc539847cc6ea2f83dfb574410f7b788f0f490d93cbb90e00`
- `hero_gameplay_160.png` — SHA-256 `533ddca1d9f4355110734dd2e00507943a279256fad1d63268e11cadd8a06dd3`
- `battle_ready_gameplay_160.png` — SHA-256 `2b7af0bbac55844f6f04911747bacb9ae3181e1dc62544c42c42a76f69098d0d`
- `shiny_three_quarter.png` — SHA-256 `21a3be2fe921df17bfad8db454ea249710a4b86410d6637b8bd584d99879cbc6`
- `shiny_gameplay_160.png` — SHA-256 `b377456817001a811a24be41f38c5a84da16b458432671ee29676e31c04176b0`

Official animation states used:

- hero/reference: `animation.lucario.ground_idle` at 0.35
- battle-ready: `animation.lucario.battle_idle` at 0.35

Walking evidence is intentionally omitted. Official Lucario walking is procedural through `q.biped_walk` + `q.bimanual_swing`; Cobblemon does not provide a dedicated Lucario walking Bedrock clip suitable for this independent evidence path. No manual walking pose is fabricated.

## Artistic QA

The first v2 attempt was inspected and iterated again before acceptance. The refined Blockbench result passes the new art bar.

At first glance, the 3/4 view reads as an aura knight rather than standard Lucario with a crown and shoulder cubes. The helm/visor, broad shoulder-mantle, chest framing and large dorsal shrine form one coherent hierarchy. The full-surface palette removes the untouched-base-color dominance. The back view has a clear shrine/halo silhouette, and the left/right views retain readable depth rather than collapsing to thin overlays.

At 160 px gameplay scale the major silhouette and cyan/gold material hierarchy remain readable. The face, ears, sensors, chest spike and tail remain recognizably Lucario. Ground-idle and battle-idle evidence shows no severe detachment or catastrophic clipping of the large pieces.

Artistic status: `FULL TRANSFORMATION ACCEPTED`.

Technical PR-level Playable Test Build and Integration Core CI are still required on the final human head before merge. A green historical run does not substitute for those final gates.

## Production files

- `assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json`
- `assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/90_ouros_aura_sentinel.json`
- `assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel.png`
- `assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png`
- `assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png`
- `docs/cobblemon-skins/0448_lucario/aura-sentinel-v2-build-metadata.json`
- `docs/cobblemon-skins/0448_lucario/aura-sentinel-v2-normal.texture.json`
- `docs/cobblemon-skins/0448_lucario/aura-sentinel-v2-shiny.texture.json`
- `data/cobblemon/species_features/ouros_aura_sentinel.json`
- `data/cobblemon/species_feature_assignments/ouros_lucario_cosmetics.json`

## Authority boundary

This skin is presentation-only. Cobblemon provides official model, texture, poser, resolver, animation and rendering surfaces. Ouros/AutoPTU remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.