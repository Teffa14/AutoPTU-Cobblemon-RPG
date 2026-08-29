# 0006 Charizard — Solar Legion

Status: EPIC ACCEPTED — BLOCKBENCH + PLAYABLE/CORE VALIDATED.

## Official source

- Target: Minecraft 1.21.1.
- Cobblemon: 1.7.3 Fabric.
- Modrinth version id: `kF7CvxTo`.
- Official primary JAR: `Cobblemon-fabric-1.7.3+1.21.1.jar`.
- JAR SHA-512: `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`.
- Geometry: `assets/cobblemon/bedrock/pokemon/models/0006_charizard/charizard.geo.json` — SHA-256 `b0e4a255876ef0cda88d0f61c9773bdcb7aee852cde929da49cda0da817bcadb`.
- Animation: `assets/cobblemon/bedrock/pokemon/animations/0006_charizard/charizard.animation.json` — SHA-256 `f16a510fec4fb00d8669ba07bee40e4ee80fb41e7cf4d798597a97da33d3880b`.
- Poser: `assets/cobblemon/bedrock/pokemon/posers/0006_charizard/charizard.json` — SHA-256 `89bfe55055fea4d7f0c0398e13060cc6fb724988028c50e2d341dd6b94c8ec8e`.
- Base resolver: `assets/cobblemon/bedrock/pokemon/resolvers/0006_charizard/0_charizard_base.json` — SHA-256 `fa46e648441b555d33bea16be49ab1abd0e1a5ac68958427b5ac914341b51711`.
- Normal texture SHA-256: `b2990c16bdf37002ee9a5ad3a68a562d03e8cd7105510a84c396e9e757ba3215`.
- Shiny texture SHA-256: `ceb2458e18f2dc825e0a50fa8a400e95b593ab7b2d4fa8452a41a685e609a4c3`.
- Official model license SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`. The exact file extracted from the official JAR is stored beside this document as `official-model-license.txt`.

The source-inspection workflow queried Modrinth for stable Fabric releases compatible with Minecraft 1.21.1 before modeling and confirmed 1.7.3 / `kF7CvxTo` as the latest compatible stable release at the time of this slice. `official-source-inspection.json` records the exact JAR paths, hashes, model hierarchy, resolver, poser, animations and related texture files inspected before authoring the cosmetic.

## Anatomy contract

The exact official geometry has 130 bones. Solar Legion keeps all 130 complete and in the exact original order. Existing cubes, pivots, rotations, locators, hierarchy, UV data and animation-facing names are untouched. The derived model changes only its geometry identifier and appends eight cosmetic `ouros_*` bones, producing 138 bones total.

The appended groups are:

- `ouros_solar_crown`, parent `head_angle`;
- `ouros_solar_gorget_core`, parent `torso2`;
- `ouros_solar_pauldron_right`, parent `shoulder_right`;
- `ouros_solar_pauldron_left`, parent `shoulder_left`;
- `ouros_solar_wing_standard_right`, parent `wing_right_base`;
- `ouros_solar_wing_standard_left`, parent `wing_left_base`;
- `ouros_solar_tail_brazier`, parent `tail5`;
- `ouros_solar_legion_mantle`, parent `torso`.

The accepted pass contains 55 cosmetic cubes. Charizard's head, muzzle, eyes, horns, neck, torso, arms, hands, legs, wings, tail chain and flame geometry remain the official Cobblemon model.

## Epic visual design

Solar Legion is a ceremonial aerial legion commander rather than a recolor or small accessory kit. The visual hierarchy is built around three signature reads: a tall open solar crown that frames the original head and horns, a heavy asymmetric shoulder/gorget assembly with a central sun core, and unequal wing-root standards that expand the rear silhouette without replacing the wings.

A split crimson/ash mantle adds mass behind the torso. The tail brazier frames the original flame root with a brass/obsidian cage and angled fins while leaving the official flame planes visible and authoritative. The palette separates obsidian armor, brass hardware, ivory ceremonial trim, crimson cloth, gold/sun plates, translucent glass, ash cloth and ember accents.

Direct review of the real Blockbench output accepted the first modeled pass. The official-vs-skin three-quarter comparison changes the silhouette immediately while keeping Charizard unmistakable. Battle stance opens the wings without detaching the shoulder or wing-root equipment. Ground walk keeps the mantle, crown and tail assembly attached. `air_fly` is a dedicated motion-safety check for the wing standards and confirms they follow their official wing parents rather than remaining world-fixed.

## UV contract

The official geometry uses a 256×128 texture. The builder derives the occupied UV footprint from the exact JAR model and dynamically selects unused texels. The accepted build reserves only `(0..7, 127)` for its eight material swatches. Generation CI proves those pixels are disjoint from every original cube UV and that every non-transparent pixel in `ouros_solar_legion_accessories.png` belongs to that reservation set. The base Charizard texture is not repainted.

## Resolver and official variants

The Ouros resolver keeps `cobblemon:charizard` as the poser and uses the derived geometry only when the `ouros_solar_legion` aspect is present. It preserves normal and shiny base textures, the official animated tail-flame layer at 10 fps, and the official `alpha_eyes` presentation layer.

Normal Solar Legion uses the four official normal flame frames. Shiny Solar Legion uses the separate four official shiny flame frames distributed in the JAR; those are not replaced by the normal flames. Alpha-eye branches retain the official `charizard_alpha.png` emissive layer and add the same transparent Ouros accessory overlay last.

## Blockbench evidence

Blockbench 5.1.6 is pinned by SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`. The evidence workflow loads the exact generated production `.geo.json`, the exact official base texture plus the exact accessory overlay, and the official Charizard animation JSON through Blockbench's Bedrock animation codec.

Matched-camera evidence records:

- official reference: `animation.charizard.ground_idle` at 0.35, 130 bones;
- hero: `animation.charizard.ground_idle` at 0.35, 138 bones;
- battle ready: `animation.charizard.battle_idle` at 0.35, 138 bones;
- walking: `animation.charizard.ground_walk` at 0.25, 138 bones;
- flight: `animation.charizard.air_fly` at 0.25, 138 bones.

The accepted files are under `test-evidence/visual/cobblemon-skins/0006_charizard/solar-legion-real-poses/` and include the matched official reference, hero, battle-ready, walking, air-fly, front, left, right and back PNGs plus provenance metadata and hashes.

## Repository-wide validation

PR #302 completed Cobblemon Official Model Review, Playable Test Build and Integration Core CI successfully on the production branch head before merge eligibility. Integration Core booted the production Fabric + Cobblemon dedicated server twice. Generation CI also revalidated the exact official source, 130→138 anatomy contract, UV reservations, resolver structure and official animation availability after the shiny-flame correction.

## Authority boundary

Solar Legion is presentation-only. It adds no Cobblemon/Minecraft battle state, combatant selection, legality, HP/status truth, tactical positioning, RNG, damage, move logic or battle outcomes. Ouros/AutoPTU remains authoritative for tactical facts.
