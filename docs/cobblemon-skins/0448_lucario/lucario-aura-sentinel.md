# 0448 Lucario — Aura Sentinel

Status: FULL TRANSFORMATION ACCEPTED; FINAL HUMAN HEAD VALIDATED

Aura Sentinel is an Ouros presentation-only cosmetic derived from the exact Lucario assets distributed in the official Cobblemon 1.7.3 Fabric JAR for Minecraft 1.21.1. It never grants Cobblemon/Minecraft battle-state authority.

## Exact official source

- Cobblemon release: 1.7.3
- Minecraft target: 1.21.1
- Modrinth version id: `kF7CvxTo`
- official JAR: `Cobblemon-fabric-1.7.3+1.21.1.jar`
- JAR SHA-256: `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- JAR SHA-512: `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`
- model: `assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json`
- model SHA-256: `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`
- animation SHA-256: `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`
- poser SHA-256: `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`
- base resolver SHA-256: `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`
- official normal texture SHA-256: `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- official shiny texture SHA-256: `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- model license SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`

The exact model license extracted from the official JAR is preserved at `official-model-license.txt`.

## Superseded accessory pass

The original Aura Sentinel was technically valid but belonged to the old accessory-first standard: crown, pauldrons, bracers and rear hardware over a visually dominant base Lucario. Issue #311 supersedes that pass artistically. It remains engineering history only.

An unreviewed later 99-cube experiment also reached the branch temporarily. It was rejected as the canonical candidate because it was not the asset set that passed human visual review. The production branch was deterministically regenerated from `build_aura_sentinel_v2_refined.py`, restoring the reviewed 87-cube candidate. The generator and review gate now explicitly require that exact 87-cube macro-form so future accidental candidate drift fails closed.

## Geometry contract

The official Lucario geometry contains 87 bones. Aura Sentinel v2 preserves all 87 original bones JSON-equivalently and in original order, including cubes, pivots, hierarchy, locators, rotations and original UV definitions.

V2 appends eight cosmetic groups for 95 total bones:

- `ouros_aura_helm_system`
- `ouros_aura_mantle_shell`
- `ouros_aura_breastplate`
- `ouros_aura_shrine_frame`
- `ouros_aura_left_armguard`
- `ouros_aura_right_armguard`
- `ouros_aura_waistcoat`
- `ouros_aura_relic_fin`

The accepted candidate contains 87 cosmetic cubes across those eight groups. Cube count is not the artistic objective; those primitives compose large readable forms instead of isolated micro-accessories.

No original head, muzzle, eyes, ears, aura sensors, chest spike, torso, limbs, hands, feet or tail are replaced or rewritten.

## Signature design

The fantasy is a ceremonial aura knight / shrine sentinel.

The first-read hierarchy is:

1. integrated open-face helm and horizontal aura visor;
2. broad connected mantle and breastplate creating one armored upper-body mass;
3. dominant dorsal shrine/halo frame with layered vertical fins and side aura blades;
4. long split waistcoat/mantle, with asymmetric arm and relic treatment supporting the macro silhouette.

The design intentionally changes the three-quarter, front and rear silhouette while leaving Lucario's biological identity readable underneath.

## Full-surface textures

V2 uses deliberate derived normal and shiny textures from the exact official 128×64 textures. Original model UV coordinates and texture dimensions are unchanged. Transparency semantics are unchanged.

Normal derived texture:

- `ouros_aura_sentinel.png`
- SHA-256 `1cbb1ca7fe260d01a4e0ca7a2f0a28ea424475f856267caf19d0b4276ed19752`
- 6,163 occupied official pixels deliberately changed

Shiny derived texture:

- `ouros_aura_sentinel_shiny.png`
- SHA-256 `7d391c01daba8634a4cfd84cc17f1f37385afe473ed1ff578d989c66fa5cb725`
- 6,163 occupied official pixels deliberately changed

Normal uses midnight indigo/cobalt, obsidian, ivory, gold and aura-cyan. Shiny deliberately shifts to graphite, cool silver, amethyst and brighter aura accents rather than silently reusing the normal palette.

The additive accessory texture remains limited to validated UV-free texels for the added geometry. It supplements the full-surface body treatment rather than substituting for it.

## Resolver and forms

The cosmetic resolver retains the official `cobblemon:lucario` poser and supplies deliberate normal/shiny branches with the same accepted derived model and separate derived textures.

Cobblemon 1.7.3 exposes one standard Lucario geometry for this resolver path. There is no male/female model split to duplicate. Mega Lucario is not implemented or modified by this cosmetic.

No custom emissive runtime, particle behavior or battle behavior is claimed by this slice.

## Final Blockbench evidence

Primary viewer: Blockbench 5.1.6.

Pinned Blockbench SHA-256: `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

The accepted assets were regenerated by CI, then a later human head triggered the final review. The final PR-level matched-camera run is:

- workflow: `Aura Sentinel V2 Refined Full Transformation Review`
- run: `33286136164`
- artifact: `aura-sentinel-v2-refined-blockbench-review`
- artifact id: `9724492898`
- artifact digest: `sha256:3bebb334fac699f09c51e0e990bb2be56351cb8af05d71f6e39f380764701353`
- reviewed human head: `cd759c80ae502648079d0ed06594b8809293d8d6`

The final artifact reports 87 original bones, 95 derived bones, eight cosmetic groups and exactly 87 cosmetic cubes. It loads the exact production model, derived textures and official Lucario animation file through Blockbench's Bedrock animation codec. Variant captures reuse the official-reference Blockbench camera target, camera position, orthographic zoom and scale.

Final evidence hashes:

- `official_reference_three_quarter.png`: `e1fcfc4fe881ca8a1ce263bd6c89c572e42741adcbe78202e629559261f1839d`
- `hero_three_quarter.png`: `95a6b7feb2fb1a812e41c07a4cc5e9470b0030d6e5b4d4a31edc84721053139f`
- `battle_ready_three_quarter.png`: `db7da2ca4d31561e59791231b6e8bf4d4eeed1be2fe539ff8788f7ca296cfc04`
- `hero_front.png`: `27e980e6c6461580b51d1ca8ecf1ee88c581cfb40eb3cd4a61d6b36b8d1d872c`
- `hero_left.png`: `a73674be9a5e2642e10382e644248a44b307ce393a4b268f34f9cb7264fe4952`
- `hero_right.png`: `9689f3730471756a0d1d4545c1aba268c8ab2fb47b14f60fea82a30bb57ba5a6`
- `hero_back.png`: `b6c0744e495b61efca736d1cbe9cd24b3746176f1a7bb9ba013ee75261a875be`
- `official_reference_gameplay_160.png`: `717546a43a59baadc539847cc6ea2f83dfb574410f7b788f0f490d93cbb90e00`
- `hero_gameplay_160.png`: `533ddca1d9f4355110734dd2e00507943a279256fad1d63268e11cadd8a06dd3`
- `battle_ready_gameplay_160.png`: `2b7af0bbac55844f6f04911747bacb9ae3181e1dc62544c42c42a76f69098d0d`
- `shiny_three_quarter.png`: `21a3be2fe921df17bfad8db454ea249710a4b86410d6637b8bd584d99879cbc6`
- `shiny_gameplay_160.png`: `b377456817001a811a24be41f38c5a84da16b458432671ee29676e31c04176b0`

Official animation states used:

- reference/hero: `animation.lucario.ground_idle` at 0.35
- battle-ready: `animation.lucario.battle_idle` at 0.35

Walking evidence remains intentionally omitted. Official Lucario locomotion uses procedural `q.biped_walk` plus `q.bimanual_swing`; there is no dedicated Lucario Bedrock walking clip for the independent evidence path. No manual walking pose is fabricated.

## Human artistic QA

The PR-generated final PNG artifact was opened and inspected after the reviewed 87-cube production assets were restored.

The three-quarter image reads immediately as an aura knight rather than ordinary Lucario with small attached blocks. The helm/visor, broad shoulder mantle, breastplate and large shrine frame establish the hierarchy before secondary details. The back view has a distinct shrine/halo silhouette. Side views retain depth. The full-surface palette prevents the original base-color read from dominating.

At 160 px gameplay scale the main silhouette and cyan/gold material hierarchy remain visible. Lucario's face, ears, sensors, chest spike and tail remain identifiable. Ground-idle and battle-idle captures show no severe cosmetic detachment or catastrophic clipping.

Artistic status: `FULL TRANSFORMATION ACCEPTED`.

## Final PR gates

On human head `cd759c80ae502648079d0ed06594b8809293d8d6`:

- Aura Sentinel V2 Refined Full Transformation Review — PASS, run `33286136164`
- Cobblemon Official Model Review — PASS, run `33286136163`
- Playable Test Build — PASS, run `33286136181`
- Integration Core CI — PASS, run `33286136176`
- retired legacy Aura Sentinel evidence stub — PASS, run `33286136172`

The next documentation-only human head must retain the exact same production model/textures. Normal repository checks must remain green before merge.

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
