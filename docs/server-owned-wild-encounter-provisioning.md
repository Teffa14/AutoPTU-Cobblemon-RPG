# Server-owned WILD encounter provisioning

`CanonicalWildEncounterBlueprintSource` is the trusted boundary for RPG/encounter generation. It resolves an already-decided canonical WILD blueprint from a canonical encounter ID only. Its method does not receive Cobblemon actor/Pokémon UUIDs, `PokemonEntity`, battle objects or other presentation state.

`ServerOwnedWildEncounterPreparationService` queries that source first. Only after a canonical blueprint has been returned does it attach the opaque external WILD actor ID as presentation correlation and delegate to `ServerOwnedWildEncounterProvisioningService`. A source that returns a different canonical encounter ID than requested is rejected to prevent confused-deputy aliasing.

`ServerOwnedWildEncounterProvisioningService` prepares canonical WILD encounter state before Cobblemon battle-start interception. Each `WildPokemonBlueprint` carries already-decided canonical PTU data: species identity, level, capabilities, ordered status state, combat stats, HP, move identities, base movement, types, ability identities, baseline accuracy/evasion, injuries, optional held-item identity and revision.

The provisioner derives canonical participant and Pokémon IDs from the canonical encounter ID with a stable SHA-256 namespace. The external actor ID does not participate in those IDs. The same canonical encounter ID and blueprint therefore produce the same canonical identities even when the presentation-side actor UUID changes.

The provisioner also derives a deterministic encounter seed from the canonical encounter ID. This seed is recorded with the provisioned encounter for later deterministic encounter-generation composition. It is deliberately not substituted for the shared battle reservation RNG seed yet. `PlayerVsWildEncounterAuthorityService` still owns the current reservation seed boundary.

After provisioning, the same object implements the two narrow contracts needed downstream. As `CanonicalWildRosterSource`, it returns only canonical participant/combatant IDs to the identity binder when battle ID, side and opaque actor correlation are valid. As `CanonicalBattleEncounterRepository`, it exposes only the provisioned WILD `CanonicalEncounterPokemonState` values for authoritative roster reservation.

The blueprint source and preparation path execute no PTU mechanics. They do not calculate targeting, movement, damage, status behavior, ability hooks, item effects, Trainer Features, terrain, reactions or AI policy. Concrete encounter generators may populate the source from trusted server-owned RPG state, but Minecraft/Cobblemon entity values never become blueprint inputs or overwrite canonical values.

Provisioned state remains in-memory and explicit `release` removes both actor correlation and canonical combatant lookup. Durable WILD provisioning, expiry/reconciliation, restart recovery, a concrete world/campaign encounter generator, and composition of the deterministic encounter seed into the battle reservation remain future slices.

Compatibility mapping: `SERVER_OWNED_WILD_ENCOUNTER_PROVISIONING` depends only on `MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK`, which remains PARTIAL. The slice has no dependency on the BLOCKING complete-movement or tactical-AI categories.
