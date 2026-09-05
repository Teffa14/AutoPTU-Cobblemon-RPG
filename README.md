# AutoPTU Cobblemon RPG

Server-authoritative Minecraft/Cobblemon integration layer for PTU.

This repository is the only writable Minecraft/Cobblemon integration project. `Teffa14/AutoPTU-Java` and `Teffa14/AutoPTU` are read-only upstream/reference repositories for this project.

## Authority boundary

AutoPTU-Java owns battle legality, calculations, lifecycle and outcomes. Minecraft, Fabric and Cobblemon own world projection, entities, networking, animation and rendering. Client packets are intents only. Cobblemon entity state must never become the source of truth for PTU stats, HP, moves, abilities, inventory, modifiers, legality or results.

Out-of-combat Trainer, Pokémon, item and progression state is also server-authoritative. Persistent player/profile/Pokémon/item state is stored below the active world save. Clients and platform entities may supply authenticated identity or presentation correlation only.

## Wild ecology architecture

Wild ecology is a global reusable RPG subsystem. Marea, Cedar Meadow, Fletchling, or any other authored region/species may be used as fixtures, smoke content, population data or QA scenarios, but they are not the production architecture.

All new wild-ecology work must move toward a generic data-driven runtime that can support every approved WILD population in every region without one Java behavior implementation per species or map. Existing `MareaWild*`-style production code is migration debt and must not become the pattern for new work.

The intended separation is `WildPopulationDefinition -> HabitatProfile -> WildBehaviorProfile -> CapabilityProfile -> Spawn/Population Runtime -> Generic Ambient AI -> Encounter Binding`. Missing authoritative WILD blueprints may block activation of a particular species, but must not block generic ecology infrastructure. Unsupported species fail closed rather than borrowing PTU facts from Cobblemon.

`WORLD-013` is therefore the extraction/generalization path for world-wide visible-wild ecology, not an endless Marea/Fletchling polish item. Marea-specific work is lower priority unless it directly extracts, proves or migrates reusable infrastructure. See `docs/WILD_ECOLOGY_ARCHITECTURE.md` for the permanent design and review rules.

## First playable battle test

The repository now contains a deliberately bounded manual 1v1 graphical test. A player can choose Bulbasaur, Charmander or Squirtle with `/autoptu testbattle <pokemon>` and watch the selected Cobblemon entity fight a server-spawned Pikachu. AutoPTU-Java owns the demo attack RNG, hit/miss result, damage, action consumption and authoritative HP mutation. The Fabric adapter projects a short lunge, displayed HP/nameplates and the final winner/loser message.

This first vertical uses fixed server-owned combat inputs rather than pretending that general `RuntimeCombatantState` materialization is complete. It does not run statuses, abilities, items, Trainer Features, terrain, forced movement, tactical scoring, rewards or campaign commits. The lunge is presentation only and is not PTU movement legality. See `docs/first-playable-battle-test.md` for the exact install/test procedure and limitations.

## Runtime validation policy

Integration work prioritizes vertical runtime tests. DTO/unit coverage remains required, but a feature is not considered live merely because it compiles.

Every CI run preserves Gradle/JUnit results, complete authority-test output, dedicated-server logs, run metadata and runtime acceptance markers as a GitHub Actions artifact. Graphical tests write MP4/PNG evidence under `test-evidence/visual/`. The current production smoke remains a `nogui` dedicated server, so it produces server evidence rather than fabricated video. See `docs/test-evidence.md`.

Current dedicated-server evidence includes:

- Production-remapped Fabric 1.21.1 + Cobblemon 1.7.3 startup.
- Server-side `PokemonEntity` UUID lookup used only for presentation identity.
- Authoritative relocation projected to a live Cobblemon entity and verified from server position.
- Authoritative positive HP projection mirrored to a live Cobblemon entity.
- Real `BattleRegistry.startBattle` interception through public `BATTLE_STARTED_PRE`, cancellation before registry insertion and proof that `BATTLE_STARTED_POST` does not fire.
- Identity-only side/actor/Pokémon handoff with canonical participant/combatant mapping and opposing-roster reservation.
- Real `MinecraftServer` player-manager authentication boundary that fails closed for malformed/offline PLAYER identities before canonical PTU state is queried.
- World-scoped durable Trainer, encounter profile, Pokémon and item/reservation repositories.
- Two-process restart smoke that reopens the same Minecraft world and verifies Trainer/profile/Pokémon/item state and an active item reservation from fresh repository instances.

The Minecraft/Cobblemon/Craftics category remains PARTIAL. The manual battle test is a bounded graphical playback harness, not yet the normal authenticated PLAYER-vs-WILD campaign path. Complete semantic playback and full entity lifecycle remain pending.

## Player-versus-wild authority path

`PlayerVsWildEncounterAuthorityService` composes player-owned Trainer/Pokémon/item/arena reservation with the owner-neutral encounter roster reservation under one server-issued reservation ID and RNG seed. The player participant and ordered roster must match exactly. If the encounter lock fails after player assets were reserved, the player reservation is compensated before denial is returned.

`FabricAuthenticatedPlayerContextResolver` verifies that the external PLAYER UUID belongs to a currently connected server player before a canonical context source can run. `PersistentCanonicalPlayerEncounterContextSource` reads that context from world-scoped durable state. `PersistentCanonicalPlayerPokemonIdentityBinder` validates the canonical player/profile/Pokémon ownership path before pairing opaque Cobblemon Pokémon UUIDs with canonical Pokémon IDs.

For WILD actors, trusted RPG/campaign logic first registers a complete canonical blueprint in `WorldScopedCanonicalWildEncounterBlueprintRegistry`, which is owned by the active Fabric world lifecycle and implements `CanonicalWildEncounterBlueprintSource`. Registration is create-only by canonical encounter ID so a later presentation correlation cannot replace PTU values. `ServerOwnedWildEncounterPreparationService` resolves that trusted blueprint before the opaque WILD actor UUID is attached, then `ServerOwnedWildEncounterProvisioningService` prepares canonical participant/combatant identities and `CanonicalEncounterPokemonState` values. The provisioner also derives a deterministic encounter-generation seed from the canonical encounter ID. Cobblemon identity remains correlation data only and does not influence canonical Pokémon IDs or PTU values.

`ServerOwnedWildEncounterIdentityBinder` later pairs the ordered opaque WILD Pokémon UUIDs from `BATTLE_STARTED_PRE` with the already-provisioned canonical roster. Exact cardinality and registry alias checks are required. Missing provisioning fails closed and leaves Cobblemon untouched.

The WILD blueprint registry is intentionally lifecycle-scoped rather than restart-durable. Durable WILD recovery requires a separate journaling/reconciliation contract. The provisioning seed is also intentionally separate from the later shared battle-reservation RNG seed. Neither boundary is inferred by the adapter.

## Persistent authority

`FabricCanonicalPlayerStoreRuntime` binds canonical state to `<world>/autoptu/canonical-state`. The runtime exposes durable player, encounter-profile, Pokémon and item/reservation repositories plus the live world's in-memory canonical WILD blueprint registry. File repositories use schema versions, revision checks, OS/process locking, forced writes and atomic replacement where required by their contracts.

Persistent selection is never treated as permission. `BattleAuthorityService` re-resolves canonical ownership, revisions and item quantities when a reservation is attempted.

Cross-aggregate transactions, journaling, partial-commit recovery, reservation expiry/reconciliation and durable WILD encounter recovery remain pending.

## Compatibility policy

`UpstreamCompatibilityMatrix` is the permanent support checklist. `CurrentUpstreamCompatibilityInspection` records the exact upstream heads inspected during the latest integration slice. `IntegrationFeatureCompatibility` maps every bounded adapter feature to the upstream capability categories it is permitted to consume.

Representative support never promotes a whole category. Base movement does not authorize forced movement. Arithmetic/stateful damage fragments do not authorize missing ability/item/terrain modifiers. Stored statuses do not authorize missing tick/cure/immunity behavior. Minecraft must consume generic authoritative state/events and never duplicate PTU mechanics.

The generic hook/registry architecture in AutoPTU-Java remains the intended path for move specials, abilities, items, statuses, terrain/weather/hazards, Trainer Features, reactions and forced movement.

## Near-term vertical ladder

Completed with dedicated-server evidence: live Cobblemon entity projection, battle-start preemption, identity capture/mapping, canonical opposing-roster reservation, Fabric PLAYER authentication boundary, world-scoped canonical persistence and restart recovery.

Completed with contract/integration fixtures: player-versus-wild authority composition, persistent authenticated player context, canonical PLAYER Pokémon identity binding, preprovisioned WILD identity binding, server-owned WILD encounter provisioning, trusted blueprint resolution, and a world-lifecycle-scoped create-only registry that supplies those blueprints without trusting `PokemonEntity` values.

The first manual graphical battle harness is the next validation rung: choose a bounded server-owned Pokémon scenario, spawn two live Cobblemon entities, invoke the pinned AutoPTU-Java authoritative move runtime, project attack motion/HP, and visibly terminate with a winner/loser. After that succeeds on a real client, the same playback pieces can be moved behind the normal authenticated PLAYER-vs-WILD reservation path rather than the test command.

General runtime combatant materialization follows only when every required `RuntimeCombatantState` input can be supplied authoritatively. Missing PTU rules stay in AutoPTU-Java rather than being recreated in the Minecraft adapter.
