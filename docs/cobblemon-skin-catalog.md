# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokémon variants. For Pokémon that already exist in Cobblemon, cosmetic work must begin from the species geometry used by the installed Cobblemon version and preserve its anatomy, proportions, pivots and animation-compatible bone structure. Rebuilding the Pokémon from scratch or silently using an obsolete model generation is not an acceptable skin workflow.

## 0025 Pikachu — Storm Courier

Status: REBUILT ON COBBLEMON 1.7.3 SOURCE / AUTOMATED MODEL VALIDATION ACTIVE / CLIENT RUNTIME VALIDATION PENDING

Aspect: `ouros_storm_courier`

Species feature key: `ouros_storm_courier`

Design intent: a field courier Pikachu with wearable expedition equipment. The cosmetic adds only storm goggles, a crossed courier harness, a compact back/side pack and a tail clamp.

Implementation rule applied:
- source anatomy is the male Pikachu model from Cobblemon 1.7.3, pinned to `MayIHaveK/cobblemon-1.7.3@e428d34ea7f4aeca6dc9031488fb51fe0e315218`;
- the source uses the current 128x64 Pikachu atlas and hierarchy (`torso2`, `neck`, `head_ai`, `head_angle`, separate muzzle, segmented tail and current locators/limbs);
- every upstream source bone is preserved unchanged and in the same order before four Ouros accessory bones are appended;
- goggles follow `head_angle`; harness and pack follow `torso2`; the clamp follows the first `tail` segment;
- the resolver uses the stock `cobblemon:pikachu` poser and preserves normal, shiny, emissive and emissive-shiny Pikachu resources;
- a separate 128x64 transparent accessory layer uses four UV pixels automatically proven unused by source geometry and transparent in all official Pikachu layers used by the build;
- review PNGs use official 1.7.3 `ground_idle`, `battle_idle` and `ground_walk` transforms rather than hand-authored substitute poses;
- the obsolete 64x64/22-bone mirror model, rebuilt-body implementation, custom poser, custom production animation set and replacement body textures are rejected.

Runtime test:
`/pokespawn pikachu ouros_storm_courier`

Shiny test:
`/pokespawn pikachu ouros_storm_courier shiny`

Review evidence rule:
Evidence must be generated from the production geometry under stock Cobblemon animation states or captured in the real client. Bind-pose renders or manually invented poses are not sufficient for visual acceptance.

Reference and licensing notes:
- Base geometry/animations are sourced from the pinned Cobblemon 1.7.3 source copy above.
- Base/shiny/emissive textures remain Cobblemon resources and are referenced at runtime; CI fetches them only to validate UV safety and render review evidence.
- Upstream asset licensing and attribution conditions continue to apply to the adapted geometry.
- The courier accessory geometry and accessory-only palette layer are Ouros additions.
- Pokémon UNITE/Holowear and public skin packs may inform high-level cosmetic principles only; no third-party costume geometry, texture or artwork is copied.
