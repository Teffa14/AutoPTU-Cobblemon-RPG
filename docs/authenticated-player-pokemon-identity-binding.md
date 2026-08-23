# Authenticated player Pokemon identity binding

The live player battle path needs a mapping from Cobblemon UUIDs to canonical AutoPTU identities before `BATTLE_STARTED_PRE` can be claimed. That mapping is identity-only.

`PersistentCanonicalPlayerPokemonIdentityBinder` accepts the external player actor UUID and the ordered external Pokemon UUIDs captured by the Cobblemon pre-start event. It first requires the player UUID to be a currently connected Minecraft session. It derives the stable canonical player identity from that authenticated UUID, requires the durable canonical player aggregate and encounter profile, requires exact roster cardinality, then resolves every profile Pokemon ID through the durable canonical Pokemon repository and verifies ownership.

Only after those checks pass does the binder install the ordered external-Pokemon-UUID to canonical-Pokemon-ID mapping in `CobblemonCanonicalEncounterIdentityRegistry`. The registry can refresh the mapping for the same canonical participant when the durable party changes between encounters. Alias protection still prevents an external Pokemon UUID or canonical combatant ID from being assigned to another participant.

The boundary never reads species, level, HP, stats, status values, moves, abilities, held items, equipment, inventory, legality or outcomes from `PokemonEntity` or Cobblemon battle state. Those values remain server-owned canonical state and are re-resolved by the existing authority services.

The production `CobblemonPlayerVsWildClaimCoordinator.persistentWorld` performs this binding before canonical encounter-context lookup. Wild/NPC canonical identity provisioning remains separate and pending; this slice does not invent a wild Pokemon from Cobblemon entity values.

Compatibility mapping: `AUTHENTICATED_PLAYER_CONTEXT_RESOLUTION`, depending only on `MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK` (PARTIAL). The feature has no dependency on forced movement or tactical AI and does not broaden any partial battle-engine category.
