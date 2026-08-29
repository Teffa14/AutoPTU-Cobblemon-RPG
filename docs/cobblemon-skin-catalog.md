# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokémon variants. For Pokémon that already exist in Cobblemon, cosmetic work must begin from the species' original Cobblemon geometry and preserve its anatomy, proportions, pivots and animation-compatible bone structure. Rebuilding the Pokémon from scratch is not an acceptable skin workflow.

## 0025 Pikachu — Storm Courier

Status: CORRECTED / ORIGINAL COBBLEMON MODEL BASE / CLIENT RUNTIME VALIDATION PENDING

Aspect: `ouros_storm_courier`

Species feature key: `ouros_storm_courier`

Design intent: a field courier Pikachu with wearable expedition equipment. Pikachu's original Cobblemon body, head, muzzle, eyes, ears, arms, legs, feet and lightning tail remain the base model. The cosmetic adds only storm goggles, a crossed courier harness, a compact back/side pack and a tail clamp.

Implementation rule applied:
- the 22 original `pikachu_male.geo.json` bones and their cubes/pivots are preserved;
- four Ouros accessory bones are appended and parented to original Cobblemon bones;
- the resolver uses Cobblemon's original `cobblemon:pikachu` poser;
- base and shiny body textures resolve to Cobblemon's original Pikachu textures;
- a separate 64x64 transparent accessory overlay supplies charcoal, leather, copper and storm-glass materials only to UV pixels unused by the original Pikachu geometry;
- original Cobblemon animations continue to drive the original bones, so accessories inherit movement through their parents;
- the previous custom rebuilt body, custom poser, custom animation set and replacement body textures are removed.

Runtime test:
`/pokespawn pikachu ouros_storm_courier`

Shiny test:
`/pokespawn pikachu ouros_storm_courier shiny`

Review evidence rule:
Four-view evidence must be regenerated from the corrected model or captured in the real client. The previous four PNGs represented the rejected rebuilt body and are removed rather than retained as misleading evidence.

Reference and licensing notes:
- Base geometry is an adaptation of Cobblemon's `pikachu_male.geo.json`; source: `codemonkey85/Cobblemon-Mirror`, path `common/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0025_pikachu/pikachu_male.geo.json`.
- Base/shiny textures remain Cobblemon resources and are referenced by identifier rather than copied into Ouros.
- The source Pikachu asset carries Cobblemon's included Creative Commons public-license/non-commercial terms. This adaptation must retain the required attribution and license conditions.
- The added courier accessory geometry and accessory-only overlay texture are authored for Ouros.
- Pokémon UNITE/Holowear and public skin packs may inform high-level cosmetic principles only; no third-party costume geometry, texture or artwork is copied.
