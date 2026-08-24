# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `ab29df99b0ac884805cb90d115818ad92c62a35d`
- AutoPTU Python: `65702f3816162c804a926c228d54d405f3236a97`
- AutoPTU-Java PRE-damage parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns the generic PRE-damage reaction registry and authoritative invocation path. Current main carries Telepathy, Perception, Perception [Errata], Parry, and Sway through that seam. Reaction context, ability identity/suppression, team identity, out-of-turn decisions, threatened tiles, effective target kind, supported reaction movement, temporary-effect bookkeeping, cancellation, semantic events, and ordinary move resource ownership remain core-owned.

Current Java main includes the generic `PreDamageFollowUpMoveExecutor` seam, the parity-backed Sway consumer, `ReactionPushApplication.pushToFirstOpenAdjacent`, and the frozen PRE-damage follow-up execution policy from #178. The frozen policy records the authoritative synchronous re-entry contract: the original move is reused with redirected attacker/target identity, ordinary action economy and move frequency are not spent a second time, PRE-damage reactions remain available for the nested result, and nested-resolution errors are swallowed in Python order.

The current Python oracle at inspected main still implements the same Sway sequence: reject recursive redirects, require a damaging melee path and available STANDARD action, decide before spending, record `sway_used`, install `sway_redirect`, emit redirect before nested move resolution, resolve the attacker into its own move, clear the guard, choose the first legal adjacent push square from authoritative grid state, emit push, then cancel the original hit/damage/type multiplier.

Sway is still not executable end-to-end through ordinary Java `BattleRuntime` resolution on main. `RuntimePreDamageReactionContextFactory` constructs the live PRE-damage context without a live follow-up executor. AutoPTU-Java draft PR #179 contains proposed runtime wiring and an end-to-end Sway regression, but unmerged PR code is not treated as available authority. The adapter must therefore keep Sway fail-closed until equivalent executable wiring reaches Java main.

AutoPTU-Java also supports bounded authoritative multi-target area execution. Representative support does not make complete movement, reactions, abilities, move-specific behavior, or the stateful damage category complete.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project semantic events and authoritative state returned by AutoPTU-Java. It may translate canonical grid coordinates to world coordinates and render the result.

Minecraft must not invoke the PRE-damage registry or follow-up executor, construct threatened tiles, derive target kind, classify attacks, evaluate reaction eligibility, consume readiness or usage state, create or mutate Sway guards, choose escape or push destinations, recursively resolve moves, spend STANDARD, mutate HP/history, cancel hit/damage/type effectiveness, or manufacture forced-movement legality.

## Compatibility dependencies

This slice depends on CORE_TARGETING, CORE_MOVEMENT_LEGALITY, COMPLETE_MOVEMENT_BEHAVIOR, ACTION_ECONOMY_AND_INITIATIVE, FULL_STATEFUL_DAMAGE_PIPELINE, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, MOVE_SPECIFIC_BEHAVIOR, ABILITIES, and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK.

Verified for this slice: generic PRE-damage registry invocation; authoritative target-kind/threat context; verified reaction movement for merged hooks; parity-backed Telepathy, Perception, Perception [Errata], Parry, and Sway hooks; generic runtime-owned PRE-damage follow-up move seam; authoritative adjacent reaction push primitive; frozen follow-up execution policy; current Python Sway behavior; semantic projection boundary; stable combatant identity; immutable adapter inputs.

Partial: complete movement behavior; full stateful damage; full lifecycle; complete status lifecycle; terrain/weather/hazards/zones/reactions; move-specific behavior; abilities; items; Trainer Features; AI tactical scoring/policy; Minecraft/Cobblemon/Craftics playback.

Blocking for this slice: merged live `BattleRuntime` follow-up-executor wiring for Sway; general push/pull/knockback/interception coverage; complete ability/reaction families; full stateful modifier coverage across abilities/items/terrain; AI tactical policy.

## Intentionally deferred

Minecraft does not special-case Sway or any other PRE-damage ability. The adapter will not invoke the Sway hook directly, supply a follow-up executor, choose the push square, replay the redirected move, spend STANDARD, or cancel the original attack. Those remain PTU-engine responsibilities.

## Next bounded slice

When executable follow-up wiring equivalent to AutoPTU-Java draft PR #179 merges to Java main, prove adapter playback of the authoritative Sway redirect, nested semantic result, push relocation, recursion-guard cleanup, and original-hit cancellation without introducing Sway-specific legality in Minecraft.
