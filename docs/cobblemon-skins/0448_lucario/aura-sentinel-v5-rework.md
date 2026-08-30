# Lucario Aura Sentinel V5 rework

Status: USER REJECTED — REWORK IN PROGRESS. No artistic acceptance is claimed.

This pass responds directly to the owner rejection of the merged V4 visual result. The technical current-source baseline is retained, but the appended cosmetic geometry is materially redesigned.

The exact official Cobblemon 1.7.3 Fabric Lucario body remains the source. Minecraft target is 1.21.1. Modrinth version id is `kF7CvxTo`. JAR SHA-256 is `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`; model SHA-256 is `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`. All 87 official bones remain intact and ordered.

V5 keeps the same ten `ouros_*` attachment roles so resolver/runtime integration stays stable, but replaces the previous box-heavy geometry. The authored-shape target is an open half-helm that preserves Lucario's ear/sensor silhouette, a descending crescent mantle built from overlapping rotated plates, a split V cuirass around the chest spike, a compact dorsal fan rather than a rectangular shrine frame, blade-following bracers, three uneven coat leaves, one dominant stepped relic wing and slimmer leg-following greaves.

The builder is `tools/cobblemon-model-review/build_aura_sentinel_v5_authored.py`. Production generation runs through `.github/workflows/cobblemon-generate-aura-sentinel-v5.yml` and always re-extracts the exact pinned official model and textures before generating the candidate.

The current texture contract remains unchanged for this slice: official normal and shiny biological textures are copied byte-for-byte; original UVs are unchanged; `bodyTexelRework: NONE`; added equipment colors use the validated accessory overlay only.

Presentation-only authority remains unchanged. Cobblemon/Minecraft provide model, texture, animation, resolver, poser and render surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and outcomes.

No acceptance language may be added until the exact current Blockbench PNG set has been inspected and the owner explicitly approves that exact asset head.
