# Server-owned WILD encounter provisioning

`ServerOwnedWildEncounterProvisioningService` prepares canonical WILD encounter state before Cobblemon battle-start interception. It sits before `ServerOwnedWildEncounterIdentityBinder` and never reads trusted battle values from `PokemonEntity`.

The provisioning input is a server-owned canonical encounter ID, an opaque external WILD actor correlation ID, the encounter side and an ordered list of `WildPokemonBlueprint` values. Each blueprint carries already-decided canonical PTU data: species identity, level, capabilities, ordered status state, combat stats, HP, move identities, base movement, types, ability identities, baseline accuracy/evasion, injuries, optional held-item identity and revision.

The service derives the canonical participant ID and canonical Pokémon IDs from the canonical encounter ID with a stable SHA-256 namespace. The external actor ID does not participate in those IDs. The same canonical encounter ID and blueprint therefore produce the same canonical identities even when the presentation-side actor UUID is different.

The service also derives a deterministic encounter seed from the canonical encounter ID. This seed is recorded with the provisioned encounter for later deterministic encounter-generation composition. It is deliberately not substituted for the shared battle reservation RNG seed yet. `PlayerVsWildEncounterAuthorityService` still owns the current reservation seed boundary.

After provisioning, the same object implements the two narrow contracts needed downstream. As `CanonicalWildRosterSource`, it returns only canonical participant/combatant IDs to the identity binder when battle ID, side and opaque actor correlation are valid. As `CanonicalBattleEncounterRepository`, it exposes only the provisioned WILD `CanonicalEncounterPokemonState` values for authoritative roster reservation.

Provisioning executes no PTU mechanics. It does not calculate targeting, movement, damage, status behavior, ability hooks, item effects, Trainer Features, terrain, reactions or AI policy. The trusted RPG/encounter generator that constructs each blueprint remains a separate server-owned concern. Minecraft/Cobblemon entities may later receive projection from this state, but entity values never overwrite it.

Provisioned state is currently in-memory and explicit `release` removes both actor correlation and canonical combatant lookup. Durable encounter provisioning, expiry/reconciliation, restart recovery and composition of the deterministic encounter seed into the battle reservation remain future slices.

Compatibility mapping: `SERVER_OWNED_WILD_ENCOUNTER_PROVISIONING` depends only on `MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK`, which remains PARTIAL. The slice has no dependency on the BLOCKING complete-movement or tactical-AI categories.
