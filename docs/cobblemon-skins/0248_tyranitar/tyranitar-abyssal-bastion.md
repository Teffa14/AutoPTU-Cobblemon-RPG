# Tyranitar — Abyssal Bastion

Status: USER REJECTED — REWORK REQUIRED
Sale eligibility: NOT ELIGIBLE.

This document is retained only as technical/provenance history. Any historical acceptance language below is superseded: the owner rejected the current art, no professional manifest certifies it, and its production assets are locked until the registry gates are satisfied.


## Scope

Abyssal Bastion is a presentation-only Ouros cosmetic for Tyranitar. It adds external fortress/siege equipment around the exact Cobblemon model. It does not add or modify battle-state authority, combatants, legality, HP, status, positions, RNG, damage, action economy, or tactical outcomes.

## Exact official source

Production target:

- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3 Fabric
- Modrinth version id `kF7CvxTo`
- primary file `Cobblemon-fabric-1.7.3+1.21.1.jar`

Pinned SHA-256 values extracted from that exact JAR:

- `tyranitar.geo.json`: `541ac55dd56e5d8f0caeea3d5947ba3ab32437978e1d676ad335ffaef79c93a9`
- `tyranitar.animation.json`: `c86725c386fd3d77b5892bd6ee0b4b425d7a5be9025c89a1737e5aeb60d92800`
- base resolver `0_tyranitar_base.json`: `8c62ec6145817c6183e841367d697189c22c2455ec0a8d8840ed7549330ee569`
- normal texture: `ef017d459e84edcca93203f04e7d86da4cc4d0446e42cb061e66a43c96fcc122`
- shiny texture: `c111a7211b0cfd010bc8f3c76177377ed101d216d9120ac54e79ec59f24cdeca`

The JAR's top-level `LICENSE` is preserved alongside this document. Its SHA-256 is `1f256ecad192880510e84ad60474eab7589218784b9a50bc7ceee34c2b91f1d5` and the file is Mozilla Public License 2.0.

## Anatomy contract

The official Tyranitar geometry contains 61 bones. `validate_original_model.py` compares every source bone in order and requires JSON-equivalence. Abyssal Bastion preserves all 61 official bones unchanged and appends eight cosmetic groups, producing 69 total bones.

Appended groups and official parents:

- `ouros_bastion_crown` -> `head_rotation`
- `ouros_bastion_gorget_core` -> `chest2`
- `ouros_bastion_pauldron_left` -> `chest`
- `ouros_bastion_pauldron_right` -> `chest`
- `ouros_bastion_dorsal_rampart` -> `torso`
- `ouros_bastion_gauntlet_left` -> `arm_left2`
- `ouros_bastion_gauntlet_right` -> `arm_right2`
- `ouros_bastion_tail_bulwark` -> `tail3`

Accepted v2 contains 80 cosmetic cubes. Tyranitar's head, jaw, eyes, biological spikes, torso, arms, legs, feet and tail remain the exact official geometry underneath the equipment.

## Epic v2 visual design

The first structurally valid pass was rejected during direct Blockbench review. Its front gorget read as a large rectangular plate and its dorsal towers sat too close to the biological back spikes, so the three-quarter view looked like heavy armor rather than a living fortress.

V2 corrects that before PR acceptance. The neck is open again and the chest core is a stepped V-shaped assembly. The left shoulder becomes the dominant siege-tower silhouette while the right shoulder is a lower shield-like counterweight. Dorsal ramparts move outward beyond the biological spikes so they remain visible in three-quarter and rear views. The crown pushes upward and outward with open battlements, and the tail receives a reinforced bulwark frame rather than a generic clamp.

Signature pieces are therefore the asymmetric siege-tower shoulder, the outward twin dorsal ramparts, and the open battlement crown/core hierarchy. The armor uses basalt, obsidian, iron, gold, magma/amber, sand and void accents to create material breakup without repainting Tyranitar's body.

## Texture and resolver contract

The official texture layout is 256x256. The builder computes the complete original UV footprint before choosing palette locations. V2 uses exactly eight proven-free texels: `(0,255)` through `(7,255)`. CI asserts that these positions do not intersect any official cube UV and that the transparent Ouros overlay has non-zero alpha only on those eight texels.

The cosmetic resolver preserves both official presentation branches: normal and shiny. It reuses the official `cobblemon:tyranitar` poser/inheritance behavior and the corresponding official base textures, then appends only the transparent `ouros_abyssal_bastion` accessory layer. The shiny source variation legitimately inherits some fields rather than repeating them; the generator preserves those semantics instead of fabricating explicit values.

No emissive, particle, material-hook or battle runtime behavior is claimed by this slice.

## Animation and Blockbench evidence

Acceptance uses pinned Blockbench 5.1.6 with AppImage SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

The exact production model, official texture, Ouros overlay and official `tyranitar.animation.json` are imported into Blockbench. Matched-camera reference/skin evidence uses:

- hero: `animation.tyranitar.ground_idle` at `0.35`
- action/cry: `animation.tyranitar.cry` at `0.25`

The official Cobblemon 1.7.3 Tyranitar animation JSON exposes only `ground_idle`, `blink`, `mouth_open` and `cry`. It contains no Bedrock `ground_walk`, `battle_idle` or equivalent battle clip. Walking and battle screenshots are therefore deliberately omitted rather than fabricated from manual transforms. Normal movement remains code-backed/procedural in Cobblemon.

The historical v2 Blockbench review recorded 61 bones for the official reference and 69 for the production cosmetic model. Front, left, right, back and three-quarter renders are stored under `test-evidence/visual/cobblemon-skins/0248_tyranitar/abyssal-bastion-real-poses/` with hashes and provenance in `pose-metadata.json`.

## Files

Production and evidence files:

- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0248_tyranitar/ouros_abyssal_bastion_tyranitar.geo.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/0248_tyranitar/90_ouros_abyssal_bastion.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0248_tyranitar/ouros_abyssal_bastion_accessories.png`
- `fabric-adapter/src/main/resources/data/cobblemon/species_features/ouros_abyssal_bastion.json`
- `fabric-adapter/src/main/resources/data/cobblemon/species_feature_assignments/ouros_tyranitar_cosmetics.json`
- `docs/cobblemon-skins/0248_tyranitar/abyssal-bastion-build-metadata.json`
- `docs/cobblemon-skins/0248_tyranitar/official-license.txt`
- `docs/cobblemon-skins/0248_tyranitar/official-license-metadata.json`
- `test-evidence/visual/cobblemon-skins/0248_tyranitar/abyssal-bastion-real-poses/`

The final merge remains conditional on the repository's normal Cobblemon model-review, playable Fabric build and Integration Core gates.
