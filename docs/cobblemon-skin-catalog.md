# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokémon variants. A catalog entry is complete only when the repository contains the Cobblemon-loadable model, texture, poser/resolver, animation assets, and exact-model review images.

## 0025 Pikachu — Storm Courier

Status: IMPLEMENTED / RUNTIME VALIDATION PENDING

Aspect: `ouros_storm_courier`

Species feature key: `ouros_storm_courier`

Design intent: a field courier prepared for electrically violent weather. The silhouette changes through storm goggles, an indigo scarf with independently animated tails, a cross-body satchel, copper wrist conductors, and a conductor assembly on the lightning tail. This is a geometry change, not a recolor.

Visual systems:
- custom Bedrock Entity geometry with dedicated accessory bones;
- normal and shiny textures;
- ground idle, battle idle, walk, blink, storm-charge quirk and faint animations;
- storm-charge accessory animation on the tail conductor and goggles.
- emissive and particle hooks are intentionally deferred until their 1.7.3 runtime binding is verified.

Runtime test:
`/pokespawn pikachu ouros_storm_courier`

Shiny test:
`/pokespawn pikachu ouros_storm_courier shiny`

Review evidence:
- `test-evidence/visual/cobblemon-skins/0025_pikachu/ouros_storm_courier/front.png`
- `test-evidence/visual/cobblemon-skins/0025_pikachu/ouros_storm_courier/left.png`
- `test-evidence/visual/cobblemon-skins/0025_pikachu/ouros_storm_courier/right.png`
- `test-evidence/visual/cobblemon-skins/0025_pikachu/ouros_storm_courier/back.png`

Reference and licensing notes:
- Cobblemon's current model/addon documentation was used for Bedrock Entity, Box UV, poser, resolver and species-feature conventions.
- The public Cobblemon source/mirror was inspected only to verify contemporary Pikachu bone/layout conventions. Its Pikachu asset license contains non-commercial terms, so no source geometry, texture or animation was copied into this asset.
- Pokémon UNITE Holowear was used only as a high-level reference for the idea that premium cosmetics can change costume silhouette and presentation. No Holowear geometry, texture, artwork or named costume was copied.
- All Ouros geometry, textures, accessory design, animation data in this entry were authored for this repository.
