# Pikachu Storm Courier Asset Notes

## Direct-use contract

The production geometry lives inside the Fabric adapter resource pack. Cobblemon remains presentation-only; this aspect does not provide PTU stats, HP, legality, moves, positions, combatants or battle-state authority.

Model identifier: `geometry.ouros_storm_courier_pikachu`

Resolver species: `cobblemon:pikachu`

Resolver order: `90`

Aspect: `ouros_storm_courier`

Poser: `cobblemon:pikachu` (original Cobblemon poser)

## Corrected model architecture

The cosmetic model is an edit of Cobblemon's original male Pikachu geometry, not a replacement interpretation.

Preserved from `pikachu_male.geo.json`:
- root `pikachu`;
- `body`, `torso`, `head` and muzzle/mouth bones;
- eye/eyelid hierarchy;
- both ear bones and their pivots/rotations;
- both arm bones;
- original two-plane lightning-tail geometry and tail pivot;
- leg, foot and toe hierarchies on both sides;
- original 64x64 UV layout and Cobblemon base/shiny texture identifiers.

Added Ouros geometry:
- `ouros_courier_goggles`, parented to `head`;
- `ouros_courier_harness`, parented to `torso`;
- `ouros_courier_pack`, parented to `torso`;
- `ouros_courier_tail_clamp`, parented to `tail`.

Because the original poser remains active, Cobblemon's own Pikachu movement/idle/battle/face animation stack drives the preserved base bones. Accessory pieces follow those bones through normal hierarchy inheritance. There is no parallel Ouros Pikachu animation rig in this corrected slice.

## Production files

- corrected Bedrock geometry: `assets/cobblemon/bedrock/pokemon/models/0025_pikachu/ouros_storm_courier_pikachu.geo.json`
- resolver: `assets/cobblemon/bedrock/pokemon/resolvers/0025_pikachu/90_ouros_storm_courier.json`
- visual feature: `data/cobblemon/species_features/ouros_storm_courier.json`
- Pikachu feature assignment: `data/cobblemon/species_feature_assignments/ouros_pikachu_cosmetics.json`

Removed as rejected implementation:
- Ouros replacement body textures;
- Ouros Pikachu poser;
- Ouros standalone Pikachu animation file;
- four review PNGs generated from the rebuilt-body geometry.

## Acceptance rule

A cosmetic for an existing Cobblemon Pokémon must preserve the original model as the anatomical source of truth. New geometry may dress or locally modify the Pokémon but must not recreate its body from generic cubes. Four-view review renders must come from this corrected geometry or from the real Cobblemon client; no concept-art substitute counts.

## Licensing/provenance

Base geometry is adapted from Cobblemon's `pikachu_male.geo.json` in the public Cobblemon source/mirror. The upstream Pikachu model directory contains a Creative Commons public license with non-commercial restrictions; required attribution/license conditions apply to the adaptation. Original Cobblemon texture files are referenced at runtime instead of copied into this repository. All `ouros_courier_*` accessory geometry is newly authored for Ouros.
