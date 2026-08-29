# 0094 Gengar — Rift Warden

Status: EPIC ACCEPTED IN BLOCKBENCH — PLAYABLE/CORE CI PENDING

Rift Warden is a presentation-only Ouros cosmetic derived from the exact Gengar model distributed in the official Cobblemon 1.7.3 Fabric JAR for Minecraft 1.21.1. It does not implement or consume Cobblemon battle-state authority.

## Exact official source

- Cobblemon release: 1.7.3
- Minecraft target: 1.21.1
- Modrinth version id: `kF7CvxTo`
- JAR: `Cobblemon-fabric-1.7.3+1.21.1.jar`
- official model: `assets/cobblemon/bedrock/pokemon/models/0094_gengar/gengar.geo.json`
- model SHA-256: `57449f9653a403a783efdffa3195eb6948aceb855411f4e77caaa9c29175ad38`
- animation SHA-256: `68a8bf920086c6dc368a8ffb5c449aedb314aab03a2df7bdde10bd61ea0cdb9f`
- base resolver SHA-256: `aeecefe6571d99bc9ab38a3b22af5e34769b346ed9858335292c82209ee95afc`
- normal texture SHA-256: `7aba3220a0007d5ac3f36bc51611e485a3bedf330319fa474b907fc5b3f77b65`
- normal emissive SHA-256: `c1b53aac2c0c48c417bade392198a65cb79fa76b78ad17907b3a10a6ce944d87`
- shiny texture SHA-256: `1a0821422f8bfbe43a02132c658e48213e6adedbc1b5854f125683951deaeb21`
- shiny emissive SHA-256: `835b1ee1221ce476655cccb366d9143952555604c37824bf0be03a28b55695c8`
- Pokemopolis layer SHA-256: `715f050dcb0daf3ef3fecb6ecc9d8bd878861c903bcb4d6e7b1d93ceb6732463`
- model license SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`

The exact model license distributed in the JAR is preserved beside this document as `official-model-license.txt`.

## Poser and animation contract

The official resolver names `cobblemon:gengar` as the poser/model runtime. Unlike Lucario and Pikachu, the official 1.7.3 JAR contains no Gengar poser JSON resource. It contains the runtime `GengarModel.class`. Rift Warden therefore does not invent a poser JSON or duplicate pose authority.

The exact official Bedrock animation JSON exposes five clips: `animation.gengar.ground_idle`, `animation.gengar.air_idle`, `animation.gengar.air_fly`, `animation.gengar.blink` and `animation.gengar.cry`. There is no dedicated battle clip and no walking clip in that file. Blockbench evidence does not fabricate either. `air_fly` is the available official locomotion clip.

## Geometry contract

The official model contains 78 bones. Rift Warden preserves all 78 JSON-equivalently and in original order. The generator changes only the geometry identifier and appends eight cosmetic bones, producing 86 total and 44 cosmetic cubes:

- `ouros_rift_halo` → `torso`
- `ouros_rift_collar` → `torso`
- `ouros_rift_shroud_left` → `arm_left`
- `ouros_rift_shroud_right` → `arm_right`
- `ouros_rift_back_pylons` → `torso`
- `ouros_rift_wrist_left` → `arm_left2`
- `ouros_rift_wrist_right` → `arm_right2`
- `ouros_rift_shadow_mantle` → `body`

The official face, eyes, eyelids, mouth states, tongue, ears, arms, hands, legs, feet and tail remain untouched.

## Epic visual direction

The fantasy is a spectral rift guardian rather than Gengar with small equipment. The signature read is a broken planar halo behind the head, twin dimensional pylons behind the shoulders, asymmetric spectral shoulder shrouds, rear collar guards, warded wrists and a split shadow mantle. The largest pieces stay behind or outside the official face plane so Gengar's eyes and grin remain dominant.

The palette uses void-black, obsidian, violet, magenta, translucent rift-purple, silver, bone and ember accents. The accessory overlay occupies eight UV-free palette texels at x=0..7, y=127 on the official 128×128 layout. CI proves that those texels are disjoint from every original cube UV footprint.

## Official variants

The custom resolver preserves every presentation variant exposed by the official base resolver: normal, shiny and `color-green`/Pokemopolis. Normal and shiny retain their matching official emissive layers. The green variant retains the official Pokemopolis layer. The Ouros accessory overlay is appended after those official layers.

No new particle runtime, emissive behavior or battle-state behavior is claimed by this slice.

## Blockbench acceptance

Pinned viewer: Blockbench 5.1.6, SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

Evidence directory: `test-evidence/visual/cobblemon-skins/0094_gengar/rift-warden-real-poses/`.

The accepted review loads the exact production `.geo.json`, official texture and Ouros overlay. It compares official and cosmetic Gengar with matched camera/scale/frame and records hero front/left/right/back plus official `ground_idle`, `air_idle` and `air_fly` states. The front silhouette changes immediately through the shoulder shrouds and rear pylons, the three-quarter view gains a strong vertical rift-frame hierarchy, and the official eyes and grin remain unobstructed. The split halo fragment above the head is intentionally detached spatially as a rift motif; it remains visually stable across idle and flight. No severe cosmetic detachment appears in the tested official clips.

No `battle_ready` PNG is claimed because the official Gengar animation JSON has no dedicated battle clip. No walking PNG is claimed because that JSON has no walking clip; `air_fly` is used as the official locomotion evidence instead.

Playable Fabric and Integration Core checks are still required before this status can be promoted to fully validated/mergeable.

## Production files

- `assets/cobblemon/bedrock/pokemon/models/0094_gengar/ouros_rift_warden_gengar.geo.json`
- `assets/cobblemon/bedrock/pokemon/resolvers/0094_gengar/90_ouros_rift_warden.json`
- `assets/cobblemon/textures/pokemon/0094_gengar/ouros_rift_warden_accessories.png`
- `data/cobblemon/species_features/ouros_rift_warden.json`
- `data/cobblemon/species_feature_assignments/ouros_gengar_cosmetics.json`

## Authority boundary

This work is presentation-only. Cobblemon model selection, rendering and animation may be reused. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.
