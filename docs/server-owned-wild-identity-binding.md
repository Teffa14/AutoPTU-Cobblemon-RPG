# Server-owned wild encounter identity binding

`ServerOwnedWildEncounterIdentityBinder` closes the WILD side of the identity-only Cobblemon pre-start boundary without making a live `PokemonEntity` authoritative PTU state.

The input from `BATTLE_STARTED_PRE` is limited to the Cobblemon battle ID, encounter side, WILD actor UUID and ordered Pokémon UUIDs. These values are correlation keys. The binder does not read species, level, HP, stats, moves, abilities, held items, statuses, legality or outcomes from Cobblemon.

A separate `CanonicalWildRosterSource` must have server-owned encounter data ready before the claim. It resolves the battle/side/actor identity to a `CanonicalWildRoster` containing only the canonical participant ID and ordered canonical Pokémon IDs. The canonical Pokémon battle values must already exist in an authority repository independently of the Cobblemon entity.

Binding succeeds only when the external and canonical roster cardinalities match exactly. The ordered external Pokémon UUIDs are then paired with the ordered canonical IDs and installed through `CobblemonCanonicalEncounterIdentityRegistry.registerOrReplace`. Existing registry alias protection prevents one external Pokémon UUID or canonical combatant ID from being silently shared across participants.

`CobblemonPlayerVsWildClaimCoordinator` can compose this binder before canonical participant resolution. Failure to find a preprovisioned WILD roster, a cardinality mismatch or an identity conflict causes the claim to fail closed. The Cobblemon battle remains untouched because cancellation still occurs only after the full authoritative reservation succeeds.

This contract deliberately does not define how a wild encounter receives species, level, moves, abilities, HP, items, status state or other PTU values. That is the next server-owned provisioning boundary. Those values must come from canonical RPG/encounter generation data, not from the live Cobblemon entity.

The slice depends only on the partial Minecraft/Cobblemon/Craftics adapter category. It transports identity and does not execute targeting, movement, damage, status, move, ability, item, Trainer Feature or AI rules.
