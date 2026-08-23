# Spatial status-block playback boundary

AutoPTU-Java main `45feae6161c9b92ccb008a60d9b6e16dcbc0c377` now resolves the spatial status-prevention family for Aroma Veil, Aroma Veil [Errata], Pastel Veil and Sweet Veil through the generic ordered status hook registry. The core decides the blocking combatant, radius, status family, active/fainted state and ability-suppression behavior. A blocked application emits a generic `RuleEffectEvent` with `sourceKind=ability`, the authoritative ability name, the blocking actor ID, the affected target ID, the move ID and `effect=status_block`.

The Minecraft/Cobblemon integration does not implement any of those rules. `BattlePresentationProjector` consumes the existing `rule_effect` stable event contract and produces one `RULE_EFFECT_CUE`. The command subject remains the authoritative blocking actor while `targetId` remains the affected combatant. This distinction matters for spatial sources because the blocker and target can be different entities.

Adapter metadata cannot override that event. Presentation hints such as radius, team, status eligibility or suppression state are ignored by the generic projector. Minecraft may choose how to animate the cue, but it may not decide whether the block was legal or infer a replacement blocker.

This slice does not make the Abilities or complete Status Lifecycle capability complete. AutoPTU-Java still lacks broad ability parity, complete status ticking/cures/durations, several prevention families and related lifecycle interactions. The compatibility matrix therefore keeps both categories PARTIAL.

Validation target: a spatial `status_block` event must preserve source actor, target, move, effect, amount and authoritative HP through the same generic rule-effect playback path already used by other ability effects. No Aroma/Pastel/Sweet-specific production branch is permitted in the adapter.
