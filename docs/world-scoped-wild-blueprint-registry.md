# World-scoped canonical WILD encounter blueprints

`WorldScopedCanonicalWildEncounterBlueprintRegistry` is the production in-memory implementation of `CanonicalWildEncounterBlueprintSource` for one live Minecraft world lifecycle.

Trusted RPG/campaign code registers a fully decided `CanonicalWildEncounterBlueprint` by canonical encounter ID before Cobblemon supplies any presentation correlation. Registration is create-only. A second registration for the same canonical encounter ID is rejected instead of replacing species, level, HP, moves, abilities, status state, held items or other PTU values.

`FabricCanonicalPlayerStoreRuntime` creates one registry when `SERVER_STARTED` fires and removes the complete world runtime handle when `SERVER_STOPPED` fires. The registry therefore cannot leak encounter blueprints between concurrently running Minecraft server instances.

The registry is intentionally not durable across restart. Trainer, encounter-profile, Pokemon and item/reservation authority already have explicit durable contracts. WILD encounter recovery does not yet have transaction journaling, expiry/reconciliation or a durable provisioning protocol, so this slice does not imply crash recovery that the integration cannot prove.

The allowed flow is:

1. trusted RPG/campaign logic chooses a canonical encounter ID and complete WILD blueprint;
2. that server-owned blueprint is registered in the active world's registry;
3. `ServerOwnedWildEncounterPreparationService` resolves it using only the canonical encounter ID;
4. only after that resolution may an opaque Cobblemon WILD actor UUID be attached for correlation;
5. `ServerOwnedWildEncounterProvisioningService` derives deterministic canonical participant/combatant identities and exposes canonical combatants to the reservation layer.

Cobblemon and Minecraft entity state never populate the blueprint. `PokemonEntity` species, level, HP, stats, moves, abilities, held items and status values remain presentation data unless a separate trusted RPG service has already written the corresponding canonical values.

This feature depends only on the partial Minecraft/Cobblemon/Craftics adapter boundary. It does not authorize the adapter to execute movement, targeting, damage, status, ability, item, Trainer Feature, terrain, reaction, forced-movement or tactical-AI rules.
