# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `28f141be5471e23f660fb2cda09bab02244ee62e`
- AutoPTU Python: `894f66771ca3f0d3c331f86c3ab888cdc38dd6f9`
- AutoPTU-Java Telepathy parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns a generic `PreDamageReactionHookRegistry`. `BuiltinPreDamageReactionHooks` registers the parity-backed Telepathy hook against canonical `BattleRuntimeState`, authoritative ability identity/suppression, team identity, the server-owned out-of-turn decision gate, threatened tiles and `ReactionMovementApplication.escapeThreatenedArea`.

AutoPTU-Java `28f141be5471e23f660fb2cda09bab02244ee62e` invokes that registry from the ordinary authoritative move-resolution path. It constructs reaction context from canonical runtime state, resolves ordinary damage first, applies PRE-damage reactions before post-result damage hooks and final HP mutation, and emits semantic events. A cancelled reaction result prevents later HP/damage-history mutation while ordinary move resources remain core-owned.

The current Python oracle at `894f66771ca3f0d3c331f86c3ab888cdc38dd6f9` still executes Telepathy as a `pre_damage_interrupt`: an allied defender in the affected area may take a legal shift to a safe tile; only a successful escape cancels hit, damage and type effectiveness. Minecraft does not reproduce that rule.

AutoPTU-Java PR #168 is still draft-only upstream work. It revalidates legal TILE anchors and expands authoritative area targets, but explicitly does not execute multi-target damage yet. The adapter therefore does not promote AoE execution or assume that Telepathy is live for ordinary TILE/AoE damage resolution.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project semantic reaction movement and rule-effect events emitted by AutoPTU-Java. The adapter may translate authoritative grid coordinates to world coordinates and animate/render the result.

The adapter must not evaluate Telepathy ownership, suppression, Mold Breaker, team eligibility, affected-area membership, decision gating, safe-tile selection, movement legality, action-economy effects, hit cancellation, damage cancellation, type-effectiveness cancellation, shield ordering, item-bonus ordering or HP mutation ordering. Those remain core responsibilities.

The adapter contract fixture now verifies a generic ordered reaction sequence: `RULE_EFFECT_CUE` followed by authoritative `ENTITY_RELOCATION`, stable combatant identity, frozen-reservation binding, grid-to-world translation and immutable playback/snapshot inputs. The fixture is semantic; it does not contain Telepathy legality or movement rules.

This promotion does not make whole upstream categories complete. COMPLETE_MOVEMENT_BEHAVIOR remains blocking in the executable matrix because the wider forced-movement/interception family is incomplete. FULL_STATEFUL_DAMAGE_PIPELINE, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS and ABILITIES remain partial. Minecraft/Cobblemon/Craftics playback also remains partial until the semantic sequence is exercised through the live Fabric entity backend in a real encounter.

## Compatibility dependencies

This bounded slice depends on CORE_TARGETING for authoritative affected-area semantics, CORE_MOVEMENT_LEGALITY for legal reaction movement, COMPLETE_MOVEMENT_BEHAVIOR for the wider reaction/forced-movement family, ACTION_ECONOMY_AND_INITIATIVE for out-of-turn semantics, FULL_STATEFUL_DAMAGE_PIPELINE for hit/damage/post-hook/HP ordering, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS for reaction phase semantics, ABILITIES for Telepathy legality and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK for projection.

Verified for this slice: generic PRE-damage registry contract; canonical threatened-area context; authoritative reaction-escape primitive for the verified hook; parity-backed Telepathy oracle; Python ordering behavior; ordinary Java runtime registry invocation; cancellation ordering before post-damage hooks and HP/history mutation; semantic rule-effect and relocation projection; stable combatant identity; frozen arena transform; immutable adapter inputs.

Partial for this slice: abilities as a category; full stateful damage; wider reactions/terrain/weather/hazards; Minecraft live presentation; AoE/TILE execution.

Blocking for broader promotion: complete forced movement/interception remains unported upstream; AI tactical scoring remains unported; Java PR #168 does not yet provide multi-target damage execution; additional reaction and ability families are not complete.

## Intentionally deferred

Minecraft does not run the reaction registry and does not special-case Telepathy. It does not calculate affected tiles, select a safe square, spend reaction resources, suppress abilities, cancel damage, execute AoE targets or reorder downstream hooks. It consumes authoritative state/events only.

## Next bounded slice

Carry the same generic reaction sequence through the entity-bound presentation stream and Fabric relocation backend using a mock/live-compatible entity fixture. Verify that the stable combatant ID resolves to the bound Cobblemon entity and that authoritative relocation ordering is preserved without introducing ability-specific branches.
