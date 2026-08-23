# Claim-time WILD preparation correlation

The WILD battle-start path now has an explicit identity-only bridge between server-owned encounter state and the Cobblemon actor that presents it.

Trusted RPG/campaign code must first publish a complete `CanonicalWildEncounterBlueprint` under a canonical encounter ID. Trusted projection/spawn code may then register exactly one opaque external WILD actor ID against that encounter in `WorldScopedWildEncounterCorrelationRegistry`. Neither operation reads PTU values from Cobblemon.

When `BATTLE_STARTED_PRE` later exposes the WILD actor, `PreparingCanonicalWildRosterSource` resolves the canonical encounter ID from that pre-registered correlation. It then asks `ServerOwnedWildEncounterPreparationService` to materialize only the already-published blueprint. `ServerOwnedWildEncounterIdentityBinder` receives the resulting canonical participant/combatant IDs and binds the external Pokemon UUIDs as lookup keys.

The external battle ID, actor UUID and Pokemon UUIDs cannot select species, level, HP, combat stats, moves, abilities, held items, statuses, Trainer state, inventory, legality or outcomes. An actor without a trusted correlation fails closed even when a canonical encounter happens to have an ID equal to that external UUID.

Preparation is retry-safe for the same actor, encounter and side. A side mismatch fails closed; a newly created mismatched provisioning is released. Correlation aliasing is rejected in both directions so one external actor cannot point at two canonical encounters and one canonical encounter cannot silently switch actors.

Both the blueprint registry and the actor-correlation registry are scoped to the active `MinecraftServer` lifecycle. They are intentionally discarded on restart. Durable WILD encounter recovery, expiry/reconciliation and journaling remain future contracts.

This slice maps to `SERVER_OWNED_WILD_ENCOUNTER_PROVISIONING`, whose only upstream capability dependency is `MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK`. It does not authorize movement, damage, status, ability, item, Trainer Feature, terrain, reaction or AI mechanics in Minecraft.
