# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `ab520743d8d99f06fa28fd4d6fa06a0c4ecd3fee`
- AutoPTU Python: `65702f3816162c804a926c228d54d405f3236a97`
- AutoPTU-Java PRE-damage parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns the generic PRE-damage reaction registry and authoritative invocation path. Current main carries Telepathy, Perception, Perception [Errata], Parry, Sway, and Shell Shield through that seam. Reaction context, ability identity/suppression, out-of-turn decisions, target classification, supported reaction movement, temporary-effect bookkeeping, action economy, combat-stage/status mutation, cancellation, semantic events, and ordinary move resource ownership remain core-owned.

AutoPTU-Java `b6701fcc4e1b0a469bed7e41c4125c47e768ff03` merged the runtime-owned synchronous PRE-damage follow-up execution boundary. `RuntimeMoveResolutionWithFollowUps` installs the core executor and re-enters authoritative move resolution with the original move and RNG while redirected attacker/target identity comes from the reaction hook. The frozen policy keeps PRE-damage reactions enabled during nested resolution and does not spend ordinary action economy or move frequency a second time.

The merged live Sway regression proves end-to-end redirected move execution through that authoritative runtime. Sway still owns the STANDARD spend, `sway_used` and `sway_redirect` bookkeeping, recursion protection, adjacent push selection, nested result, guard cleanup and cancellation of the original hit. Minecraft supplies none of those decisions.

The current Python oracle at inspected main matches the same sequence: reject recursive redirects, require damaging melee and available STANDARD, decide before spending, record usage and redirect guard, resolve the attacker into its own move, clear the guard, choose the first legal adjacent push square from authoritative grid state, emit push, then cancel the original hit/damage/type multiplier.

AutoPTU-Java `ab520743d8d99f06fa28fd4d6fa06a0c4ecd3fee` additionally ports Shell Shield through the same generic PRE-damage registry. Its readiness, Withdrawn status, DEF stage mutation and semantic ability event are stateful core behavior and do not create a Minecraft-side special case.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project semantic events and authoritative state returned by AutoPTU-Java. It may translate canonical grid coordinates to world coordinates and render ordered playback.

Minecraft must not invoke the PRE-damage registry or follow-up executor, construct threatened tiles, classify attacks, evaluate reaction eligibility, consume readiness or usage state, create Sway guards, select escape or push destinations, recursively resolve moves, spend STANDARD, apply combat stages/statuses, mutate HP/history, cancel hit/damage/type effectiveness, or manufacture forced-movement legality.

## Compatibility dependencies

This slice depends on CORE_TARGETING, CORE_MOVEMENT_LEGALITY, COMPLETE_MOVEMENT_BEHAVIOR, ACTION_ECONOMY_AND_INITIATIVE, FULL_TURN_ROUND_LIFECYCLE, FULL_STATEFUL_DAMAGE_PIPELINE, COMPLETE_STATUS_LIFECYCLE, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, MOVE_SPECIFIC_BEHAVIOR, ABILITIES, and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK.

Verified for this slice: generic PRE-damage registry invocation; authoritative effective target kind; supported reaction movement for merged hooks; parity-backed Telepathy, Perception, Perception [Errata], Parry, Sway and Shell Shield hooks; generic runtime-owned follow-up seam; synchronous live follow-up wiring; authoritative adjacent Sway push primitive; frozen follow-up execution policy; current Python Sway behavior; server-owned Shell Shield status/stage mutation; semantic projection boundary; stable combatant identity; immutable adapter inputs.

Partial: complete movement behavior; full turn/round lifecycle; full stateful damage; complete status lifecycle; terrain/weather/hazards/zones/reactions; move-specific behavior; abilities; items; Trainer Features; AI tactical scoring/policy; Minecraft/Cobblemon/Craftics playback.

Blocking for broader category promotion: general push/pull/knockback/interception coverage; complete ability/reaction families; full stateful modifier coverage across abilities/items/terrain; complete status ticking/cure semantics; AI tactical policy; broader live Minecraft entity playback coverage.

## Intentionally deferred

Minecraft does not special-case Sway, Shell Shield, or any other PRE-damage ability. The adapter will not invoke hooks, supply its own executor, choose push squares, replay redirected moves, spend actions, apply statuses/stages, or cancel attacks. Those remain PTU-engine responsibilities.

## Next bounded slice

Prove generic playback ordering for the authoritative Sway sequence through the live-compatible entity presentation boundary: redirect semantic cue, nested authoritative result, push relocation, and original-hit cancellation. Keep the fixture semantic and identity-based so no Sway legality or movement rule is duplicated in Minecraft.
