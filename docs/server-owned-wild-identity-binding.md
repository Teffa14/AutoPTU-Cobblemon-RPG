# Server-owned wild encounter identity binding

`ServerOwnedWildEncounterIdentityBinder` closes the WILD side of the identity-only Cobblemon pre-start boundary without making a live `PokemonEntity` authoritative PTU state.

The input from `BATTLE_STARTED_PRE` is limited to the Cobblemon battle ID, encounter side, WILD actor UUID and ordered Pokémon UUIDs. These values are correlation keys. The binder does not read species, level, HP, stats, moves, abilities, held items, statuses, legality or outcomes from Cobblemon.

`ServerOwnedWildEncounterProvisioningService` can now prepare the required canonical roster before the claim. It creates canonical participant/combatant identities and `CanonicalEncounterPokemonState` values from trusted server-owned RPG encounter blueprints. The external WILD actor UUID participates only in later correlation; it cannot determine the canonical Pokémon values or IDs. See `server-owned-wild-encounter-provisioning.md`.

At claim time the `CanonicalWildRosterSource` resolves battle/side/actor identity to a `CanonicalWildRoster` containing only the canonical participant ID and ordered canonical Pokémon IDs. The canonical Pokémon battle values already exist independently through the canonical encounter repository contract.

Binding succeeds only when the external and canonical roster cardinalities match exactly. The ordered external Pokémon UUIDs are paired with the ordered canonical IDs and installed through `CobblemonCanonicalEncounterIdentityRegistry.registerOrReplace`. Existing registry alias protection prevents one external Pokémon UUID or canonical combatant ID from being silently shared across participants.

`CobblemonPlayerVsWildClaimCoordinator` composes this binder before canonical participant resolution. Missing provisioning, a cardinality mismatch or an identity conflict causes the claim to fail closed. Cobblemon cancellation still occurs only after the full authoritative reservation succeeds.

The provisioner deliberately stops before RPG encounter-generation policy. Its blueprint must come from trusted server-owned encounter data. Minecraft/Cobblemon must not infer species, level, moves, abilities, HP, items, status state or other PTU values from a live entity. The deterministic provisioning seed also remains separate from the current shared battle reservation RNG seed until a later composition slice.

The bounded provisioning/binding path depends only on the partial Minecraft/Cobblemon/Craftics adapter category. It transports identity and frozen canonical state and does not execute targeting, movement, damage, status, move, ability, item, Trainer Feature or AI rules.
