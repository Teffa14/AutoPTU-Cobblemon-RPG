# Lucario Aura Sentinel V6 rework

Status: USER REJECTED — REWORK IN PROGRESS. No artistic acceptance is claimed.

This line responds to the owner rejection of the merged V4 visual result. V5 was generated and reviewed in real Blockbench evidence, then failed internal artistic QA because the horizontal visor, fragmented dorsal hardware and weak gameplay-scale identity still read too much like attached blockwork. V5 is therefore superseded as an art candidate.

The active candidate is V6. The exact official Cobblemon 1.7.3 Fabric Lucario body remains the source. Minecraft target is 1.21.1. Modrinth version id is `kF7CvxTo`. JAR SHA-256 is `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`; model SHA-256 is `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`. All 87 official bones remain intact and ordered.

V6 keeps the ten stable `ouros_*` attachment roles but materially redesigns their geometry again. It removes the visor bar, opens the face around Lucario's expression and sensors, uses split brow/cheek plates, concentrates silhouette weight into one asymmetric shoulder/relic blade, replaces fragmented dorsal hardware with a compact diamond sigil, keeps an open V around the chest spike, moves coat mass rearward into two uneven tails, and narrows the greaves and bracers into flatter diagonal platework.

The V6 builder is `tools/cobblemon-model-review/build_aura_sentinel_v6_platework.py`. Production assets were regenerated from the pinned official JAR by `.github/workflows/cobblemon-generate-aura-sentinel-v6.yml` on bot asset commit `654e6182b95da3fdea0d215d3bf655b6d48d2193`. Exact-source, original-model, body-texture, accessory-overlay and attachment validation passed during that generation.

This documentation commit intentionally changes no production geometry or texture. Its purpose is to create a human-authored head after the bot generation so PR-level Blockbench, official-model, playable-build and integration checks run against the exact generated V6 assets.

The current texture contract remains unchanged for this slice: official normal and shiny biological textures are copied byte-for-byte; original UVs are unchanged; `bodyTexelRework: NONE`; added equipment colors use the validated accessory overlay only.

Presentation authority remains unchanged. Cobblemon/Minecraft provide model, texture, animation, resolver, poser and render surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and outcomes.

No acceptance language may be added until the exact current V6 Blockbench PNG set has been opened and inspected and the owner explicitly approves that exact asset head.