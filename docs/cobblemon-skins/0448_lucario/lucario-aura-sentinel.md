# 0448 Lucario — Aura Sentinel

Status: ARTISTICALLY ACCEPTED IN BLOCKBENCH; PLAYABLE/CORE CI PENDING

Aura Sentinel is an Ouros presentation-only cosmetic built around the exact normal Lucario model distributed in the official Cobblemon 1.7.3 Fabric JAR for Minecraft 1.21.1. It does not implement or consume Cobblemon battle-state authority.

## Exact official source

- Cobblemon release: 1.7.3
- Minecraft target: 1.21.1
- Modrinth version id: `kF7CvxTo`
- official model: `assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json`
- model SHA-256: `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`
- animation SHA-256: `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`
- poser SHA-256: `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`
- base resolver SHA-256: `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`
- normal texture SHA-256: `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- shiny texture SHA-256: `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- model license file SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`

The exact model license text extracted from that JAR is preserved at `official-model-license.txt`. Its terms include attribution and non-commercial restrictions; distribution/use of the adapted model must remain compatible with those terms.

## Geometry contract

The official model contains 87 bones. Aura Sentinel preserves those 87 bones JSON-equivalently and in the same order. The generator changes only the geometry identifier and appends eight cosmetic bones, giving 95 total:

- `ouros_aura_crown` → `head_angle`
- `ouros_aura_pauldron_left` → `shoulder_left`
- `ouros_aura_pauldron_right` → `shoulder_right`
- `ouros_aura_core` → `torso3`
- `ouros_aura_backframe` → `torso3`
- `ouros_aura_bracer_left` → `arm_left2`
- `ouros_aura_bracer_right` → `arm_right2`
- `ouros_aura_waist_mantle` → `torso`

No original head, muzzle, eye, ear, aura-sensor, chest-spike, torso, limb, hand, foot or tail geometry is replaced.

## Epic visual direction

The fantasy is an aura sentinel / ceremonial field knight rather than Lucario wearing small utility accessories. The signature read comes from an angular temple crown, deliberately asymmetric shoulder armor, an open chest-core frame around the original spike, a rear aura frame, armored bracers and a split ceremonial waist mantle.

The materials use obsidian, steel, silver, gold, royal blue, deep cloth, ivory and a translucent aura-cyan accent. The overlay uses only dynamically detected UV-free palette texels from the official 128×64 texture; CI proves those texels do not overlap any original cube UV footprint.

## Resolver and variants

The resolver keeps the official `cobblemon:lucario` poser and supports normal and shiny base textures. The official JAR exposes one normal Lucario geometry for this resolver; there is no male/female geometry split to duplicate. The Pokédex data also advertises a Mega form, but this cosmetic does not claim to implement or alter Mega Lucario.

No custom emissive layer or custom particle runtime is claimed by this slice. Cobblemon contains official Lucario aura particle assets, but Aura Sentinel does not bind new particle behavior to them.

## Blockbench evidence

Visual acceptance uses pinned Blockbench 5.1.6, SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`, loading the exact production `.geo.json`, exact official texture and exact Ouros overlay.

Evidence directory: `test-evidence/visual/cobblemon-skins/0448_lucario/aura-sentinel-real-poses/`

Validated states:
- official reference: `animation.lucario.ground_idle` at 0.35
- Aura Sentinel hero: `animation.lucario.ground_idle` at 0.35
- Aura Sentinel battle-ready: `animation.lucario.battle_idle` at 0.35
- hero front / left / right / back structural views from the same production model

Lucario walking is deliberately not represented by a fabricated Bedrock clip. Its official poser uses `q.biped_walk` plus `q.bimanual_swing` and `ground_idle`, rather than a dedicated `animation.lucario.ground_walk`. The current independent evidence pipeline imports official Bedrock animation clips. It records this limitation instead of inventing manual transforms.

## Artistic QA

The first real Blockbench review was accepted artistically. The variant reads as a distinct armored aura sentinel at gameplay distance, especially in three-quarter and battle views. The left/right shoulder treatment and bracers produce useful asymmetry, the chest remains recognizably Lucario, and the original face, ears, sensors, spike and tail remain legible. The tested ground-idle and battle-idle states show no severe cosmetic detachment.

Playable Fabric and Integration Core checks remain required before the status can be promoted to fully validated/mergeable.

## Production files

- `assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json`
- `assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/90_ouros_aura_sentinel.json`
- `assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png`
- `data/cobblemon/species_features/ouros_aura_sentinel.json`
- `data/cobblemon/species_feature_assignments/ouros_lucario_cosmetics.json`

## Authority boundary

This skin is presentation-only. Model selection, textures, animation presentation and cosmetic attachment may use Cobblemon systems. Ouros/AutoPTU remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.
