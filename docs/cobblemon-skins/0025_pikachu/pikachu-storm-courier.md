# Pikachu Storm Courier Asset Notes

## Direct-use contract

The production files live inside the Fabric adapter resources so the remapped mod jar carries the resource/data pack content directly. Cobblemon remains presentation-only. The aspect changes visual resolution and does not provide PTU facts or battle authority.

Model identifier: `geometry.ouros_storm_courier_pikachu`

Animation group: `ouros_storm_courier_pikachu`

Resolver species: `cobblemon:pikachu`

Resolver order: `90`

Aspect: `ouros_storm_courier`

## Files

- Bedrock geometry: `assets/cobblemon/bedrock/pokemon/models/0025_pikachu/ouros_storm_courier_pikachu.geo.json`
- animations: `assets/cobblemon/bedrock/pokemon/animations/0025_pikachu/ouros_storm_courier_pikachu.animation.json`
- poser: `assets/cobblemon/bedrock/pokemon/posers/0025_pikachu/ouros_storm_courier_pikachu.json`
- resolver: `assets/cobblemon/bedrock/pokemon/resolvers/0025_pikachu/90_ouros_storm_courier.json`
- base/shiny textures: `assets/cobblemon/textures/pokemon/0025_pikachu/`
- visual feature: `data/cobblemon/species_features/ouros_storm_courier.json`
- Pikachu feature assignment: `data/cobblemon/species_feature_assignments/ouros_pikachu_cosmetics.json`

## Review rule

The four PNG review images are generated from the same `.geo.json` cube/bone source used by Cobblemon. They are review evidence, not concept art. They do not replace a real-client load test.

## Current validation

Static validation checks passed:
- all JSON parses;
- every geometry bone name is lowercase;
- cube `size` values are integers;
- root and head bones exist;
- every animation bone target exists in the geometry;
- poser root exists and its Bedrock animation names resolve;
- resolver points to the expected unique model, poser and texture names;
- the custom species feature aspect matches the resolver aspect exactly.

Runtime validation still requires the real Fabric 1.21.1 + Cobblemon 1.7.3 client because the production smoke is headless. Emissive and particle emission are deliberately excluded from this first slice until their Cobblemon 1.7.3 runtime binding is verified in-client.
