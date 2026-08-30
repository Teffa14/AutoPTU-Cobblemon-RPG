# Pikachu — Storm Courier

Status: USER REJECTED — REWORK REQUIRED
Sale eligibility: NOT ELIGIBLE.

This document is retained only as technical/provenance history. Any historical acceptance language below is superseded: the owner rejected the current art, no professional manifest certifies it, and its production assets are locked until the registry gates are satisfied.


The previous 98-bone epic-v3 pass remains a technically validated historical baseline. It is no longer considered artistically complete because its first read remains too close to "Pikachu plus goggles, straps, backpack and small field hardware". Do not use v3 as the visual quality bar for new skins.

## Source of truth

Storm Courier is derived from the exact Pikachu geometry distributed in the official Cobblemon 1.7.3 Fabric JAR (`Cobblemon-fabric-1.7.3+1.21.1.jar`, Modrinth version id `kF7CvxTo`). It is not derived from a mirror, old model, screenshot, fork or Ouros anatomy reconstruction.

Pinned official hashes:

- male `pikachu_male.geo.json`: `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female `pikachu_female.geo.json`: `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- official base texture `pikachu.png`: `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`
- official `pikachu.animation.json`: `d9ca00604978f295ad312d358a06f2655c725b30ac3da73c3637ae160c543384`

Both official sex models contain 90 bones. Every accepted Storm Courier model must preserve all 90 original bones exactly at JSON-object level and in original order, including cubes, pivots, parents, rotations, locators and UV definitions. Male and female are always derived independently so the official female tail remains intact.

## Historical epic-v3 baseline

The existing production pass appends eight Ouros bones to each independent official model, producing 98 bones total:

- `ouros_storm_goggles` parented to `head_angle`
- `ouros_storm_cowl` parented to `head_angle`
- `ouros_storm_mantle` parented to `torso2`
- `ouros_storm_harness` parented to `torso2`
- `ouros_storm_pack` parented to `torso2`
- `ouros_storm_coils` parented to `torso2`
- `ouros_storm_tail_clamp` parented to `tail2`
- `ouros_storm_tail_vanes` parented to `tail2`

That pass was useful for proving exact anatomy preservation, sex-specific derivation, resolver behavior, official animation attachment and real Blockbench CI. It is not the target art direction anymore.

## Why v3 failed the new visual bar

The old implementation optimized individual accessory objects instead of the character-wide read. The base Pikachu color/material identity remained dominant and most added surfaces were separated into many small cuboids. At gameplay scale the player still read Pikachu first and then a collection of accessories.

The full-transformation standard reverses that priority. The first read must be the storm-runner fantasy while Pikachu remains anatomically intact underneath.

## Full-transformation v4 direction

V4 must be designed as one coherent storm-courier system, not as a larger version of the same accessory list.

The three dominant signature structures are:

1. an integrated storm visor + aerodynamic cowl that reads as one head assembly while leaving Pikachu's face, ears and muzzle identifiable;
2. a connected mantle/shoulder shell + torso storm suit that creates a large continuous upper-body mass rather than separate shoulder cubes and painted straps;
3. an expedition power-frame that combines the rear pack, storm core, field conductors and tail grounding system into one visually connected machine.

Secondary pieces such as buckles, route cases, storm vials, small fins and charms are permitted only after those three structures work at gameplay scale.

### Silhouette target

The three-quarter silhouette must change immediately. The head should have a recognizable cowl/visor profile. The shoulders should form a broad but motion-safe storm mantle. The rear power-frame should create a strong asymmetric dorsal read. The tail conductor should visually continue the machine rather than look like isolated clamps placed on the tail.

Large silhouette changes are allowed. The official ears, head, limbs, torso and tail remain present and untouched below the cosmetic shell.

### Full-surface material target

V4 may use a complete derived Pikachu texture based on the exact official `pikachu.png` and exact official UV layout.

The official 128×64 dimensions and UV mapping remain unchanged. CI pins the immutable official texture SHA-256 and records the new derived texture SHA-256.

The intended material language is a storm-proof field suit rather than unchanged yellow fur plus accessory colors. The texture pass should deliberately unify exposed body regions and equipment with a limited palette such as insulated stormcloth, dark conductive panels, weathered copper/brass hardware and controlled electric accent surfaces. The exact palette must be decided from the real Blockbench review, not copied from third-party skins.

Recoloring must preserve species identity and face readability. It is a supporting system, not a substitute for the large geometry transformation.

Required v4 texture metadata:

- `officialTextureBaselineSha256`
- `derivedTexture`
- `derivedTextureSha256`
- `bodyTexelRework`
- `paletteIntent`
- `materialIntent`
- accessory-overlay path and UV reservation if a separate atlas is still used for added geometry

## Animation and attachment

The official `cobblemon:pikachu` poser remains presentation authority. No alternate body rig is introduced.

Blockbench review imports the official `pikachu.animation.json` through the Bedrock animation codec. Acceptance uses official `ground_idle`, `battle_idle` and `ground_walk` states at recorded times. Cosmetic pieces must inherit from official animated parents and remain attached through those states.

## Blockbench acceptance

The exact production model and textures must be loaded in pinned Blockbench. The official Pikachu reference and Storm Courier must use the same camera, projection, scale, pose and animation frame. Independent auto-fit is not accepted for official-vs-skin comparison.

Minimum accepted evidence for v4:

- official reference three-quarter
- Storm Courier hero three-quarter
- battle-ready three-quarter using official `battle_idle`
- walking three-quarter using official `ground_walk`
- front/left/right/back structural views
- male/female comparison sufficient to prove the official female-tail difference remains intact
- gameplay-scale 128–192 px readability image derived from the real Blockbench render

The v4 review must explicitly reject the asset if the thumbnail reads as ordinary Pikachu with accessories, if the signature structures collapse into noise, if the added geometry obscures species identity, or if major equipment detaches/clips severely in the official animations.

## Validation

`tools/cobblemon-model-review/validate_original_model.py` remains the anatomy gate. It must continue rejecting any drift in the 90 official bones.

Full-surface texture derivation is validated separately. The texture validator must verify exact dimensions, immutable official baseline hash, expected derived hash and an explicit record of body-texel changes. Full-body recolor is legal; silent UV remapping is not.

Playable Test Build and Integration Core CI remain mandatory after the reviewed v4 assets are actually committed. A green technical pipeline cannot override a failed artistic review.

## Provenance and license

The official Cobblemon JAR and applicable license remain the only source for the base model/texture/animation assets. Third-party server skins, Pokemon Unite designs, fan packs and mod skins may be studied only for high-level composition/ambition and may not contribute copied geometry, UVs, textures, logos or distinctive protected motifs.

## Authority boundary

Storm Courier is presentation-only. It does not use or alter Cobblemon battle-state authority, participants, legality, HP/status, tactical positions, combatant selection, damage or RNG. Ouros/AutoPTU remains authoritative for tactical battle facts.
