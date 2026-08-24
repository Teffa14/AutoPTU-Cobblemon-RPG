# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `52f9194e47cae95e36165b6606f7a88cf430669d`
- AutoPTU Python: `e1feb915fe3497cd099a0b447212755300dff1d8`
- AutoPTU-Java PRE-damage parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns the generic PRE-damage reaction registry and authoritative invocation path. Current main carries Telepathy, Perception, Perception [Errata], and Parry through that seam. Reaction context, ability identity/suppression, team identity, out-of-turn decisions, threatened tiles, effective target kind, supported reaction movement, temporary-effect bookkeeping, cancellation, semantic events, and ordinary move resource ownership remain core-owned.

Current Java main also includes the generic `PreDamageFollowUpMoveExecutor` seam merged in #176. That seam gives PRE-damage hooks a runtime-owned way to request synchronous follow-up move resolution without passing trusted move metadata, RNG, action economy, or outcomes from Minecraft.

The current Python oracle still implements Sway as a melee PRE-damage interrupt: it guards recursive redirect, checks once-per-use and STANDARD availability, decides before spending STANDARD, records `sway_used`, installs `sway_redirect`, emits redirect before nested resolution, resolves the attacker into its own move, clears the guard, selects the first legal adjacent push square from authoritative grid state, emits push, and then cancels the original hit/damage/type multiplier.

Java main does not yet make that Sway contract executable. Open AutoPTU-Java PR #177 adds the Sway hook consumer and a server-owned adjacent push primitive, but it is draft-only and explicitly does not yet install the runtime follow-up executor into the live BattleRuntime context factory. Therefore Sway remains blocked in this adapter.

AutoPTU-Java also supports bounded authoritative multi-target area execution. Representative support does not make full movement, reactions, abilities, move-specific behavior, or the stateful damage category complete.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project semantic events and authoritative state returned by AutoPTU-Java. It may translate canonical grid coordinates to world coordinates and render the result.

Minecraft must not invoke the PRE-damage registry or follow-up executor, construct threatened tiles, derive target kind, classify attacks, evaluate reaction eligibility, consume readiness or usage state, create Sway guards, choose escape or push destinations, recursively resolve moves, spend STANDARD, mutate HP/history, cancel hit/damage/type effectiveness, or manufacture forced-movement legality.

## Compatibility dependencies

This slice depends on CORE_TARGETING, CORE_MOVEMENT_LEGALITY, COMPLETE_MOVEMENT_BEHAVIOR, ACTION_ECONOMY_AND_INITIATIVE, FULL_STATEFUL_DAMAGE_PIPELINE, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, MOVE_SPECIFIC_BEHAVIOR, ABILITIES, and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK.

Verified for this slice: generic PRE-damage registry invocation; authoritative target-kind/threat context; verified reaction movement for merged hooks; parity-backed Telepathy, Perception, Perception [Errata], and Parry; generic runtime-owned PRE-damage follow-up move seam; frozen Python Sway contract; semantic projection boundary; stable combatant identity; immutable adapter inputs.

Partial: complete movement behavior; full stateful damage; full lifecycle; complete status lifecycle; terrain/weather/hazards/zones/reactions; move-specific behavior; abilities; items; Trainer Features; AI tactical scoring/policy; Minecraft/Cobblemon/Craftics playback.

Blocking: authoritative live Sway execution; general push/pull/knockback/interception coverage; complete ability/reaction families; full stateful modifier coverage across abilities/items/terrain; AI tactical policy.

## Intentionally deferred

Minecraft does not special-case Sway or any other PRE-damage ability. The merged generic follow-up seam is infrastructure only until Java main owns the complete Sway consumer, live runtime installation, authoritative push result, and emitted semantic playback.

## Next bounded slice

After AutoPTU-Java merges and live-wires Sway, prove adapter playback of the authoritative redirect, nested semantic result, push relocation, recursion-guard cleanup, and original-hit cancellation without introducing Sway-specific legality in Minecraft.
