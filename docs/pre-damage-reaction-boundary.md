# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `9819146364b67da51d039c5d380c8a4aa3c378c5`
- AutoPTU Python: `8d7de9f70d301e136672b66f460f9233a463cc7a`
- AutoPTU-Java Telepathy parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java has a generic `PreDamageReactionHookRegistry` contract. `BuiltinPreDamageReactionHooks` registers Telepathy against canonical `BattleRuntimeState`, authoritative ability identity/suppression, team identity, the server-owned out-of-turn decision gate, threatened tiles and `ReactionMovementApplication.escapeThreatenedArea`.

A successful Telepathy reaction emits an authoritative ability `RuleEffectEvent` and cancels the current pre-damage result. The reaction movement itself is applied through the core-owned movement primitive rather than through Minecraft.

The current Python oracle still executes Telepathy as a `pre_damage_interrupt`: an allied defender inside the affected area may shift to a legal safe tile; only a successful escape cancels hit, damage and type effectiveness.

AutoPTU-Java `9819146364b67da51d039c5d380c8a4aa3c378c5` additionally freezes the Python ordering contract around this phase. PRE-damage reactions run only after an ordinary hit result, after ordinary move resolution, before post-result hooks, before attacker item damage bonuses and before final HP mutation. The oracle also fixes interrupt suppression/Unseen Fist and shield placement relative to the PRE-damage and post-result phases.

## Blocking gap

That new ordering evidence is a contract fixture, not Java runtime wiring. The inspected Java ordinary move-resolution path still does not invoke the generic PRE-damage reaction registry or construct the affected area for it.

Therefore this repository must not claim ordinary live Telepathy execution yet. Minecraft must not use the frozen Python ordering contract as permission to execute the missing Java phase itself.

## Adapter rule

Minecraft/Cobblemon/Craftics may project a semantic reaction movement or ability event only after AutoPTU-Java has emitted it as an authoritative result. The adapter may translate grid coordinates to world coordinates and animate/render the event.

The adapter must not evaluate Telepathy ownership, suppression, Mold Breaker, team eligibility, threatened-area membership, decision gating, safe-tile selection, movement legality, action-economy effects, hit cancellation, damage cancellation, type-effectiveness cancellation, shield ordering, item-bonus ordering or HP mutation ordering.

This intentionally keeps COMPLETE_MOVEMENT_BEHAVIOR blocking, FULL_STATEFUL_DAMAGE_PIPELINE partial, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS partial and ABILITIES partial. The presence of parity-backed reaction primitives and pipeline-order evidence does not promote any whole category.

## Compatibility dependencies

This bounded guard depends on CORE_TARGETING for authoritative affected-area semantics, CORE_MOVEMENT_LEGALITY for legal reaction movement, COMPLETE_MOVEMENT_BEHAVIOR for the wider reaction/forced-movement family, ACTION_ECONOMY_AND_INITIATIVE for out-of-turn action semantics, FULL_STATEFUL_DAMAGE_PIPELINE for hit/damage/shield/item/HP ordering, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS for reaction ordering, ABILITIES for Telepathy legality and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK for projection only.

Verified for this slice: the generic PRE-damage registry contract, authoritative reaction-escape movement primitive, Telepathy parity hook and Python phase ordering fixture. Partial: abilities, stateful damage pipeline, reaction family and adapter playback. Blocking: ordinary Java move-resolution invocation of the PRE-damage registry and affected-area construction for that live path.

## Next safe promotion

When AutoPTU-Java inserts the generic PRE-damage reaction registry into the ordinary authoritative Java move-resolution path and tests affected-area construction plus live hook ordering, this repository can add an adapter contract fixture that consumes the resulting semantic movement/event sequence without implementing Telepathy-specific rules.
