# Pikachu Storm Courier Asset Notes

## Direct-use contract

The production geometry lives inside the Fabric adapter resource pack. Cobblemon remains presentation-only; this aspect does not provide PTU stats, HP, legality, moves, positions, combatants or battle-state authority.

Model identifier: `geometry.ouros_storm_courier_pikachu`

Resolver species: `cobblemon:pikachu`

Resolver order: `90`

Aspect: `ouros_storm_courier`

Poser: `cobblemon:pikachu` (the stock Cobblemon 1.7.3 Pikachu poser)

## Cobblemon 1.7.3 source of truth

Storm Courier is built from the male Pikachu geometry used by Cobblemon 1.7.3, pinned for reproducibility to `MayIHaveK/cobblemon-1.7.3` commit `e428d34ea7f4aeca6dc9031488fb51fe0e315218`.

The previous 64x64/22-bone mirror asset was rejected as an obsolete Pikachu generation and is not an acceptable anatomical source for this skin.

The 1.7.3 source uses a 128x64 atlas and the newer hierarchy, including `torso2`, `neck`, `head_ai`, `head_angle`, a separate `muzzle`, expanded mouth/eye structures, segmented tail bones (`tail`, `tail2`, `tail3`), item/face/tail locators and the current animation-compatible limb hierarchy.

The build tool copies every upstream source bone unchanged and in the same order. It changes only the geometry identifier and appends four Ouros accessory bones. CI fails if any upstream anatomy changes during the build.

## Added Ouros geometry

- `ouros_courier_goggles`, parented to `head_angle` so the goggles follow the current head/face animation stack;
- `ouros_courier_harness`, parented to `torso2`;
- `ouros_courier_pack`, parented to `torso2`;
- `ouros_courier_tail_clamp`, parented to the first `tail` segment so it follows the segmented lightning tail.

The accessory atlas is 128x64. The builder scans the official normal, shiny and emissive Pikachu textures plus the source model UV footprint, then chooses four pixels that are unused by the source geometry and transparent in all supplied official layers. Those pixels carry charcoal, leather, copper and storm-glass colors. This prevents the accessory UVs from repainting Pikachu's body.

## Resolver and animation compatibility

The resolver keeps `cobblemon:pikachu` as the poser and keeps the official 1.7.3 normal/shiny textures. It also preserves Pikachu's official emissive and emissive-shiny layers before adding the transparent courier accessory layer.

No Ouros Pikachu poser or production animation file exists. Cobblemon 1.7.3 remains responsible for Pikachu presentation and drives the stock `ground_idle`, `ground_walk`, `battle_idle`, face/quirk and other animation states. Accessories inherit those parent-bone transforms.

Review PNGs are non-generative evidence. The review workflow fetches the pinned 1.7.3 animation file and applies real `ground_idle`, `battle_idle` and `ground_walk` transforms to the exact built model. The renderer does not author substitute hero/battle/walking rotations anymore.

## Production files

- geometry: `assets/cobblemon/bedrock/pokemon/models/0025_pikachu/ouros_storm_courier_pikachu.geo.json`
- resolver: `assets/cobblemon/bedrock/pokemon/resolvers/0025_pikachu/90_ouros_storm_courier.json`
- accessory-only overlay: `assets/cobblemon/textures/pokemon/0025_pikachu/ouros_storm_courier_accessories.png`
- visual feature: `data/cobblemon/species_features/ouros_storm_courier.json`
- Pikachu feature assignment: `data/cobblemon/species_feature_assignments/ouros_pikachu_cosmetics.json`
- reproducible builder: `tools/cobblemon-model-review/build_storm_courier_173.py`
- official-animation review renderer: `tools/cobblemon-model-review/render_cobblemon_animation.py`

## Rejected implementations

Do not restore any of these:
- custom rebuilt Pikachu body geometry;
- the obsolete 64x64/22-bone Pikachu mirror model;
- Ouros replacement body textures;
- Ouros Pikachu poser or standalone production animation rig;
- preview poses made from manually invented bone rotations.

## Acceptance rule

For a Pokémon already supplied by Cobblemon, the installed Cobblemon version is the anatomical and animation source of truth. A cosmetic can add wearable/local geometry but cannot silently substitute an older model generation or reconstruct the Pokémon. Review evidence must represent the production geometry under stock Cobblemon states or come from the real client.

## Licensing/provenance

Base geometry and referenced Pikachu textures/animations originate from Cobblemon 1.7.3. The build is pinned to the public 1.7.3 source copy above for reproducibility and remains subject to the upstream asset licensing/attribution terms. Original Cobblemon body textures and animations are referenced or fetched for validation rather than redistributed as Ouros-authored assets. All `ouros_courier_*` accessory geometry and the generated accessory-only palette layer are Ouros additions.
