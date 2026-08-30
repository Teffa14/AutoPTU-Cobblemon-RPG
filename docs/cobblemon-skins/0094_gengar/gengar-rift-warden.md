# 0094 Gengar — Rift Warden V2

Status: ART ACCEPTED; FINAL PR GATES PENDING

Rift Warden V2 is a presentation-only Ouros cosmetic rebuilt from the exact Gengar model distributed in the current repository-compatible official Cobblemon release. It does not implement or consume Cobblemon battle-state authority.

## Exact official source

The source check for this slice queried the official Modrinth Cobblemon project for stable Fabric releases compatible with Minecraft 1.21.1 and confirmed 1.7.3 remains the current repository-compatible stable target.

- Cobblemon release: 1.7.3
- Minecraft target: 1.21.1
- Modrinth version id: `kF7CvxTo`
- JAR: `Cobblemon-fabric-1.7.3+1.21.1.jar`
- JAR SHA-256: `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- JAR SHA-512: `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`
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

The exact model license distributed in the pinned JAR is preserved beside this document as `official-model-license.txt`.

## Poser and official animation contract

The official resolver uses `cobblemon:gengar`. The 1.7.3 JAR contains no `assets/cobblemon/bedrock/pokemon/posers/0094_gengar/gengar.json`; the runtime Gengar model handles pose behavior. Rift Warden does not invent a poser JSON or take pose/battle authority.

The exact official Bedrock animation file exposes five clips:

- `animation.gengar.ground_idle`
- `animation.gengar.air_idle`
- `animation.gengar.air_fly`
- `animation.gengar.blink`
- `animation.gengar.cry`

There is no dedicated battle clip and no walking clip. The review therefore does not fabricate those states. `air_fly` is used as the official locomotion evidence.

## V2 geometry contract

The exact official Gengar geometry contains 78 bones. V2 preserves all 78 original bones JSON-equivalently and in original order, including names, hierarchy, pivots, rotations, cubes, locators and UV definitions.

V2 appends nine Ouros cosmetic groups, producing 87 total bones and 75 cosmetic cubes:

- `ouros_rift_cowl` → `torso`
- `ouros_rift_mantle` → `torso`
- `ouros_rift_gate` → `torso`
- `ouros_rift_relic_wing` → `torso`
- `ouros_rift_lower_shroud` → `body`
- `ouros_rift_armguard_left` → `arm_left`
- `ouros_rift_armguard_right` → `arm_right`
- `ouros_rift_wrist_left` → `arm_left2`
- `ouros_rift_wrist_right` → `arm_right2`

The official face, eyes, eyelids, grin/mouth states, tongue, ears, arms, hands, legs, feet and tail remain unchanged underneath the cosmetic architecture.

The old eight-bone / 44-cube Rift Warden remains historical engineering evidence only. V2 replaces its thin halo/pylon/accessory-first composition with connected macro-forms.

## Signature design

The fantasy is a spectral fortress / dimensional gate warden.

The first-read hierarchy is:

1. a deep open-face cowl framing Gengar's eyes and grin without covering them;
2. a broad connected shoulder mantle that makes the upper body one deliberate armored mass;
3. a thick dorsal rift-gate/reliquary frame that dominates the rear silhouette;
4. one large asymmetric stepped relic wing that strongly changes the three-quarter outline;
5. a lower split shroud and large arm/wrist guards that carry the transformation through the whole character.

The result deliberately uses fewer, larger systems rather than scattered small hardware. The asymmetry is concentrated in the relic wing while the portal frame and mantle establish the stable fortress silhouette.

## Immutable biological textures and accessory overlay

V2 does not repaint Gengar biology.

- production normal body texture is the exact official `gengar.png`, SHA-256 `7aba3220a0007d5ac3f36bc51611e485a3bedf330319fa474b907fc5b3f77b65`
- production shiny body texture is the exact official `gengar_shiny.png`, SHA-256 `1a0821422f8bfbe43a02132c658e48213e6adedbc1b5854f125683951deaeb21`
- `bodyTexelRework: NONE`
- original UVs are unchanged

Added geometry uses only the transparent accessory overlay:

- `ouros_rift_warden_accessories.png`
- SHA-256 `cd5d2b5e8eb31478ef0a4429bc5b7db13eb6561dac7176fc90e0b9f8eca15725`
- dimensions: 128×128
- eight non-transparent palette texels at x=0..7, y=127
- occupied official biological texel conflicts: 0
- free-texel overlay gate: PASS

Equipment palette: void black, obsidian, violet, magenta, translucent rift purple, silver, bone and sparse ember fracture accents.

## Official presentation variants

The production resolver retains all official presentation branches needed by the historical Gengar resolver contract:

- normal: official `gengar.png` plus official normal emissive layer plus Ouros accessory overlay
- shiny: official `gengar_shiny.png` plus official shiny emissive layer plus Ouros accessory overlay
- `color-green` / Pokemopolis: official base texture plus official emissive and Pokemopolis layer plus Ouros accessory overlay

No new emissive behavior, particles, battle logic or tactical state is introduced.

## Current real Blockbench evidence

Primary viewer: Blockbench 5.1.6.

Pinned Blockbench SHA-256: `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

Current matched-camera review:

- workflow: `Rift Warden V2 Current Official Model Review`
- run: `33304154999`
- reviewed human head: `f4a94e6f5a1c5d733c32fa57709a4db51d2700ec`
- artifact: `rift-warden-v2-current-blockbench-review`
- artifact id: `9729915611`
- artifact digest: `sha256:086b89158dae2a515701ce19cdac968e22209433d7808b161246ea6c5d892ab4`

The workflow downloads the pinned official JAR, re-verifies the exact Gengar source hashes, validates the exact production geometry/overlay/attachments, loads the production `.geo.json` and official animation file through Blockbench's Bedrock codec, then reuses the official-reference camera profile for the candidate states.

Evidence SHA-256 values:

- `official_reference_three_quarter.png`: `bb59b4457d1f8855d1469bf8a41a50cdca1aa1688b2100c89af0bc55de2b1f6d`
- `hero_three_quarter.png`: `08f8f5a3f6f331f37dded5f5c520dd36d1de5f1ef9c8bdc090791c6b3243f819`
- `air_idle_three_quarter.png`: `c4534d70471470f74c90319dabccb8e37a68dd78403a43064fa8e8c69feeba96`
- `air_fly_three_quarter.png`: `febaaf875deb970fdf54d0afcbcf83696af958589956c9df081c2ca905d6fca6`
- `hero_front.png`: `ed05500f1c5d0954b51d53757bc413f88b921c9b15016da8550e8da863a1b787`
- `hero_left.png`: `f5d501e3ddeeb0792f16c3f763235bab89608d5da55ac8919e67934aaa6e5f8e`
- `hero_right.png`: `a8da120ea5218044069c9b526ddcabcffc91ce5b0b7f178adc19eb9a4578576b`
- `hero_back.png`: `85c6ba219d53a6c9f450361d56acd496c4282a0886e726c15bce88a716049460`
- `official_reference_gameplay_160.png`: `f601f6a70b23da6543aab769ca1cacc4ce453e41924855813e5e2bc9e3420dd5`
- `hero_gameplay_160.png`: `963de70ab89d71267666e5d9de894a0c2eeca303fbd76dc8a3af34d1433b1049`
- `air_idle_gameplay_160.png`: `95cbacf2741042ebe6ff8d4f770c9e5d849b567ff959b952dae418493f934e36`
- `air_fly_gameplay_160.png`: `ace02c4385fa1936d0b2cbea6e0b07a3f88d541a9a201b1e79a2b2f03a93cd2b`
- `shiny_three_quarter.png`: `7ae72fc11d577749da431b72febd28ce141106ed3fcac11448a2b747d481b241`

Official states used:

- hero/reference: `animation.gengar.ground_idle` at 0.35
- airborne idle: `animation.gengar.air_idle` at 0.35
- locomotion: `animation.gengar.air_fly` at 0.35

Battle evidence is omitted because the official animation file has no dedicated battle clip. Walking evidence is omitted because the official animation file has no walking clip; `air_fly` is the actual official locomotion evidence.

## Human artistic QA

The current artifact PNGs were opened and inspected, including official reference, hero three-quarter, front, back, both side views, 160 px gameplay scale and official `air_fly`.

The first glance changes materially from official Gengar. The large gate/cowl and shoulder architecture create a substantially taller and wider silhouette. The asymmetric stepped relic wing is visible as a dominant signature object in three-quarter view. The lower shroud and large arm systems prevent the design from collapsing into a head/back-only accessory pass.

Front view keeps Gengar's eyes and grin unobstructed while the portal crown and lateral armor establish the warden fantasy. Rear view reads as a fortress/reliquary assembly with substantial depth. Left/right views show intentionally different mass because of the relic wing rather than duplicated symmetry. At 160 px the large portal silhouette and dark-violet/silver/rift equipment hierarchy remain readable.

The `air_fly` capture keeps the macro architecture coherent without catastrophic detachment. The structural attachment validator also passes all nine cosmetic groups with the normal strict thresholds (`anchor-gap 1.50`, `piece-gap 1.00`).

Artistic decision: `ACCEPTED` for this exact V2 production asset. This acceptance is asset-specific and does not bypass final PR-level build/integration checks.

## Production files

- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0094_gengar/ouros_rift_warden_gengar.geo.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/0094_gengar/90_ouros_rift_warden.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0094_gengar/ouros_rift_warden_accessories.png`
- `fabric-adapter/src/main/resources/data/cobblemon/species_features/ouros_rift_warden.json`
- `fabric-adapter/src/main/resources/data/cobblemon/species_feature_assignments/ouros_gengar_cosmetics.json`
- `docs/cobblemon-skins/0094_gengar/rift-warden-build-metadata.json`
- `docs/cobblemon-skins/0094_gengar/official-model-license.txt`

## Authority boundary

This work is presentation-only. Cobblemon model selection, rendering, official texture/emissive layers and animation may be reused. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.
