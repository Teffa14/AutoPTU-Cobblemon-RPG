# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `28f141be5471e23f660fb2cda09bab02244ee62e`
- AutoPTU Python: `01a9b1c70af504b77f5b8441f7283d5957987190`
- AutoPTU-Java Telepathy parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns a generic `PreDamageReactionHookRegistry`. `BuiltinPreDamageReactionHooks` registers the parity-backed Telepathy hook against canonical `BattleRuntimeState`, authoritative ability identity/suppression, team identity, the server-owned out-of-turn decision gate, threatened tiles and `ReactionMovementApplication.escapeThreatenedArea`.

AutoPTU-Java `28f141be5471e23f660fb2cda09bab02244ee62e` now invokes that registry from the ordinary authoritative move-resolution path. It constructs the reaction context from canonical runtime state, resolves ordinary damage first, applies PRE-damage reactions before post-result damage hooks and final HP mutation, and emits the resulting semantic events. A cancelled reaction result prevents the later HP/damage-history mutation while still consuming the ordinary move resource in core.

The current Python oracle at `01a9b1c70af504b77f5b8441f7283d5957987190` still executes Telepathy as a `pre_damage_interrupt`: an allied defender in the affected area may take a legal shift to a safe tile; only a successful escape cancels hit, damage and type effectiveness. Minecraft does not reproduce that rule.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project the semantic reaction movement and rule-effect events emitted by AutoPTU-Java. The adapter may translate authoritative grid coordinates to world coordinates and animate/render the result.

The adapter must not evaluate Telepathy ownership, suppression, Mold Breaker, team eligibility, affected-area membership, decision gating, safe-tile selection, movement legality, action-economy effects, hit cancellation, damage cancellation, type-effectiveness cancellation, shield ordering, item-bonus ordering or HP mutation ordering. Those remain core responsibilities.

This promotion verifies the ordinary PRE-damage runtime seam. It does not promote whole upstream categories to complete. COMPLETE_MOVEMENT_BEHAVIOR remains partial because the wider forced-movement/reaction family is incomplete. FULL_STATEFUL_DAMAGE_PIPELINE remains partial because verified PRE-damage ordering does not establish every ability/item/status/terrain modifier. TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS remains partial. ABILITIES remains partial because Telepathy is representative, not complete.

## Compatibility dependencies

This bounded slice depends on CORE_TARGETING for authoritative affected-area semantics, CORE_MOVEMENT_LEGALITY for legal reaction movement, COMPLETE_MOVEMENT_BEHAVIOR for the wider reaction/forced-movement family, ACTION_ECONOMY_AND_INITIATIVE for out-of-turn semantics, FULL_STATEFUL_DAMAGE_PIPELINE for hit/damage/post-hook/HP ordering, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS for reaction phase semantics, ABILITIES for Telepathy legality and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK for projection.

Verified for this slice: generic PRE-damage registry contract; canonical threatened-area context; authoritative reaction-escape movement primitive; parity-backed Telepathy hook; Python ordering oracle; ordinary Java runtime registry invocation; cancellation before post-damage hooks and HP/history mutation; semantic event emission.

Partial for this slice: abilities as a category; complete movement behavior; full stateful damage; the wider reactions/terrain/weather/hazard family; Minecraft playback beyond existing generic semantic-event projection.

Blocking for broader promotion: additional reaction hooks and move/ability families are not established as complete; forced movement is still separately gated; Minecraft still requires concrete playback fixtures for authoritative reaction movement sequences before claiming polished in-game presentation.

## Intentionally deferred

Minecraft does not run the reaction registry and does not special-case Telepathy. It does not calculate affected tiles, select a safe square, spend reaction resources, suppress abilities, cancel damage or reorder downstream hooks. The adapter consumes authoritative state/events only.

## Next bounded slice

Add a concrete adapter contract fixture for an authoritative PRE-damage reaction event sequence: stable combatant identity, grid-to-world movement projection, rule-effect playback ordering and immutable post-action snapshot reconciliation. Keep the fixture semantic and generic so later abilities can reuse it without Minecraft-owned PTU rules.
