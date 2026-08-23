# Server-owned WILD blueprint publication

`ServerOwnedWildEncounterBlueprintPublisher` is the boundary between trusted RPG/campaign encounter state and the active world's create-only WILD blueprint registry.

The publisher receives only a canonical encounter ID. It asks a `CanonicalWildEncounterBlueprintSource` for an already-decided blueprint and copies that blueprint into `WorldScopedCanonicalWildEncounterBlueprintRegistry`. The source never receives a Cobblemon battle ID, WILD actor UUID, Pokemon UUID, `PokemonEntity`, Minecraft entity state or client payload.

The blueprint therefore has to exist as trusted server-owned RPG state before presentation correlation begins. Species, level, HP, combat stats, moves, abilities, status state, held items and other PTU values cannot be derived from the later Cobblemon encounter.

Publication is fail-closed. A missing source record returns false and writes nothing. A source that answers with a different canonical encounter ID is rejected. The world registry remains create-only, so a second publication cannot silently replace the first canonical decision.

After publication, `ServerOwnedWildEncounterPreparationService` can resolve the world-scoped blueprint by canonical encounter ID. Only at that later step may an opaque external WILD actor UUID be attached as presentation correlation before `ServerOwnedWildEncounterProvisioningService` creates deterministic canonical participant/combatant identities.

This slice does not decide how campaign logic generates encounters. It provides the integration boundary through which that logic can publish an already-authoritative result. It also does not make WILD blueprints durable across restart, compose the provisioning seed into the later battle reservation seed, or execute targeting, movement, damage, status, ability, item, Trainer Feature or AI rules.
