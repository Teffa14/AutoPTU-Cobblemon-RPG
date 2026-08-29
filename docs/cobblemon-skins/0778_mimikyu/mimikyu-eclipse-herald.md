# 0778 Mimikyu — Eclipse Herald

Status: EPIC ACCEPTED — BLOCKBENCH + PLAYABLE/CORE VALIDATED

## Official source

- Target: Minecraft 1.21.1.
- Cobblemon: 1.7.3 Fabric.
- Modrinth version id: `kF7CvxTo`.
- Official primary JAR: `Cobblemon-fabric-1.7.3+1.21.1.jar`.
- Geometry: `assets/cobblemon/bedrock/pokemon/models/0778_mimikyu/mimikyu.geo.json` — SHA-256 `d763a4704b345606af71fe567422abada299c44391bbf39a26628c63070236ab`.
- Animation: `assets/cobblemon/bedrock/pokemon/animations/0778_mimikyu/mimikyu.animation.json` — SHA-256 `5adc2d0a21d9f0264906f01e2eec960801698894924eb39589119263b5ab24c5`.
- Poser: `assets/cobblemon/bedrock/pokemon/posers/0778_mimikyu/mimikyu.json` — SHA-256 `3f5b3a7bf7f2c21b0fc80cb3b991bf8f3b1d6fcaefc19f3bce1678085505f5c9`.
- Base resolver: `assets/cobblemon/bedrock/pokemon/resolvers/0778_mimikyu/0_mimikyu_base.json` — SHA-256 `98326f43805816badd6de27639943694ff6aeb800e00402888de4b4114bc7ec3`.
- Normal texture SHA-256: `8a715d58bf5eb8904537946acf7c902bdc6fa8a5e56bd7cea3cb39ebfef3d376`.
- Shiny texture SHA-256: `349afd9be3b5540fd9d468e2c8910db2851aa028a94d464f17467d34b591c731`.
- Official model license SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`. The exact extracted file is stored as `official-model-license.txt` beside this document.

## Anatomy contract

The official geometry contains 48 bones. Eclipse Herald keeps all 48 complete and in the exact official order. Existing cubes, pivots, rotations, locators, hierarchy, UVs and names are untouched. The derived geometry changes only its identifier and appends eight cosmetic `ouros_*` bones, producing 56 bones total.

The appended bones are `ouros_eclipse_halo`, `ouros_eclipse_cowl`, `ouros_eclipse_mantle`, `ouros_eclipse_pennant_left`, `ouros_eclipse_pennant_right`, `ouros_eclipse_tail_reliquary`, `ouros_eclipse_hand_charm_right`, and `ouros_eclipse_hand_charm_left`. The accepted second artistic pass contains 42 cosmetic cubes.

Mimikyu's original fake face, ears, flower, cloth body, tail chain and ghost hands remain original Cobblemon geometry. The cosmetics attach to `torso_top`, `head`, `tail1`, `hand_right3` and `hand_left3` so official animation transforms remain authoritative for presentation.

## Visual design

Eclipse Herald is a ritual moon/eclipsed-relic silhouette rather than a recolor. The signature read is a broken lateral moon-shard frame, broad asymmetric ritual mantle, two rear standards at different heights, a tail reliquary and hand-mounted charms. The first Blockbench pass was rejected internally because the halo looked like unrelated floating fragments and the standards competed with the ears. Pass two moved the standards behind and below the head and rebuilt the moon frame into a coherent left-biased relic with a small opposite fracture.

The palette is an accessory-only material breakup: void charcoal, dark ritual cloth, moon ivory, silver, violet, translucent aura, ember and teal accents. It does not repaint Mimikyu's base texture.

## UV contract

The official texture is 64×64. The builder derives the occupied UV footprint from the exact official geometry and reserves only verified-unused pixels. Pass two uses `(0..7, 63)` for the eight material swatches. The generator validates that the reserved pixels do not intersect the original footprint and that every non-transparent overlay pixel is one of those reservations.

## Resolver and variants

The Ouros resolver uses the exact official `cobblemon:mimikyu` poser and the derived production geometry. It provides normal and shiny branches using the original normal/shiny base textures plus the same transparent accessory overlay. Other Mimikyu-themed texture resources present in the JAR are not silently remapped by this resolver.

## Blockbench evidence

Blockbench 5.1.6 is pinned by SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

Matched-camera evidence uses:
- official reference: `animation.mimikyu.ground_idle` at 0.35;
- hero: `animation.mimikyu.ground_idle` at 0.35;
- action: official `animation.mimikyu.physical` at 0.35;
- locomotion: official `animation.mimikyu.ground_walk` at 0.25.

No manual battle-ready pose is fabricated. The evidence metadata records whether a distinct official Bedrock battle clip exists and leaves battle-ready absent when it does not.

The accepted files are under `test-evidence/visual/cobblemon-skins/0778_mimikyu/eclipse-herald-real-poses/` and include official reference, hero, action, walking, front, left, right and back views.

## Repository-wide validation

PR #299 completed Cobblemon Official Model Review, Playable Test Build and Integration Core CI successfully before merge. Integration Core also booted the production Fabric + Cobblemon dedicated server twice. The merged main commit is `a63a1712b53f584fccc08647d3792ee654fdc09d`.

## Authority boundary

This asset is presentation only. It adds no Cobblemon/Minecraft battle-state authority, combatant selection, legality, HP/status truth, tactical positions, RNG, damage or outcome logic. Ouros/AutoPTU remains authoritative for battle facts.
