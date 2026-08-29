# 0658 Greninja — Shadow Tide

Status: EPIC V2 ACCEPTED — BLOCKBENCH + PLAYABLE/CORE VALIDATED.

## Official source

- Target: Minecraft Java Edition 1.21.1.
- Cobblemon: 1.7.3 Fabric.
- Modrinth version id: `kF7CvxTo`.
- Official primary JAR: `Cobblemon-fabric-1.7.3+1.21.1.jar`.
- JAR SHA-512: `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`.
- Normal geometry: `assets/cobblemon/bedrock/pokemon/models/0658_greninja/greninja.geo.json` — SHA-256 `b9608c2eef6dc1a407da31192fde20ae2cea135e7399c14926598acce091e30d`.
- Ash geometry distributed in the same JAR: `assets/cobblemon/bedrock/pokemon/models/0658_greninja/ashgreninja.geo.json` — SHA-256 `be45c61cc17a7ab6408c06e70905e53c7dbd3c99cba4e8bff0192a03849ca7f5`.
- Animation: `assets/cobblemon/bedrock/pokemon/animations/0658_greninja/greninja.animation.json` — SHA-256 `9e0176549e372324127fc2c8edbaa5d72ca6009dac06088c65df89790d3548f3`.
- Base resolver: `assets/cobblemon/bedrock/pokemon/resolvers/0658_greninja/0_greninja_base.json` — SHA-256 `6c437d1397d607e2dcc17f193a40c666cb206884dc2b1c5f612b44584ea607de`.
- Normal texture SHA-256: `6a7b6ed1b1e93111413e47c11e9423a35e2877140e69b1bac1cb285c46ab6464`.
- Official model license SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`; the exact license file extracted from the JAR is stored beside this document.

`official-source-inspection.json` records the model hierarchy, animations, resolver and eight Greninja/Ash-Greninja normal/shiny/tongue texture assets found in the official JAR. Greninja does not distribute a species poser JSON beside those assets. The JAR contains the code-backed `GreninjaModel.class`, so this slice does not invent a missing poser path.

## Anatomy and form contract

The normal official model has 78 bones and the Ash geometry has 81. Each derivative is built independently from its corresponding official file. Shadow Tide appends the same eight cosmetic groups to each, producing 86 normal bones and 89 Ash bones. All original bones remain JSON-equivalent and in the same original order.

The cosmetic groups are `ouros_shadow_tide_cowl`, `ouros_shadow_tide_gorget`, asymmetric left/right pauldrons, left/right bracers, `ouros_shadow_tide_back_frame` and `ouros_shadow_tide_split_mantle`. The accepted v2 contains 60 cosmetic cubes. No original head, tongue, crest, torso, arm, hand, leg, foot or Ash-specific shuriken bone is rewritten.

## Epic visual design

Shadow Tide is a water-executioner / shadow-shinobi field variant. The first Blockbench pass was rejected despite passing structural CI because the three-quarter silhouette still read too close to standard Greninja and the rear frame looked like crossed straps.

The accepted v2 deliberately concentrates visual hierarchy into three signature reads: an open broken-crescent cowl around the head, a heavy asymmetric shoulder/gorget assembly with a cyan tide core, and an oversized asymmetric executioner tide-glaive mounted behind the torso. The second pass also enlarges the split rear mantle and adds side pennants so the lower silhouette has more mass without replacing Greninja's legs.

Direct review of the real Blockbench renders accepted v2. The face and tongue remain dominant, while the executioner blade is clearly visible from rear and three-quarter angles. Battle idle keeps the shoulder armor and glaive attached during the crouched pose. Ground walk keeps the cowl, bracers and mantle with their official parents without severe clipping or world-fixed pieces.

## UV contract

Both official geometries use a 128×64 texture. The builder computes the occupied UV footprint of normal and Ash geometry and selects texels free in both layouts. Shadow Tide uses only `(0..7,63)` for eight material swatches: abyss cloth/armor, indigo, steel, silver, cyan, translucent glass, violet and foam trim. Generation CI proves those eight pixels are disjoint from both official UV footprints and that every non-transparent pixel of the Ouros overlay is inside that reservation set. Base body textures are not repainted.

## Resolver and Battle Bond boundary

The official base resolver contains two presentation variations: normal and shiny. The Ouros resolver preserves that exact scope, keeps `cobblemon:greninja` as the official poser reference where present, preserves the official base/shiny texture branches and appends the transparent accessory layer.

The JAR separately distributes an Ash-Greninja geometry and assigns the `battle_bond` species feature. Runtime inspection also confirms the code-backed Greninja model class. This presentation slice deliberately does not invent an `ash` aspect or make Cobblemon Battle Bond authoritative. The Ash derivative is generated and anatomy-validated as form-parity evidence, but Shadow Tide does not add new battle-state routing. AutoPTU/Ouros remains authoritative for tactical battle facts.

## Blockbench evidence

Blockbench 5.1.6 is pinned by SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`. The evidence workflow loads the exact generated production model, exact official base texture plus Ouros overlay, and the official Greninja animation JSON through Blockbench's Bedrock animation codec.

Matched normal-form evidence uses `animation.greninja.ground_idle` at 0.35 for the official reference and hero, `animation.greninja.battle_idle` at 0.35, and `animation.greninja.ground_walk` at 0.25. The workflow also records front/left/right/back views. A separate Ash geometry render is retained as diagnostic form-parity evidence; because the official base resolver does not expose an Ash variation and the code-backed presentation path is not reconstructed here, that diagnostic is not treated as proof of an in-game Ash routing path.

Evidence is stored under `test-evidence/visual/cobblemon-skins/0658_greninja/shadow-tide-real-poses/` with provenance metadata and PNG hashes.

PR #303 merged the accepted v2. On its final head, Cobblemon Official Model Review, Playable Test Build and Integration Core CI all completed successfully, alongside the real Blockbench evidence workflow. This closes the prior documentation-only “repository-wide PR gates pending” status.

## Authority boundary

Shadow Tide is presentation-only. It adds no combatant selection, legality, HP/status authority, tactical positioning, RNG, damage, move resolution, Battle Bond transformation authority or battle outcome logic to Cobblemon/Minecraft. AutoPTU/Ouros remains authoritative for tactical facts.
