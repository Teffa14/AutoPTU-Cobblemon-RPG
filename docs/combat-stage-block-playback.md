# Combat-stage block playback boundary

AutoPTU-Java main `554b97e44fca9736f98704f8db3b1a661c63e93f` now resolves Flower Veil before an authoritative Combat Stage mutation through the generic `CombatStagePreventionHookRegistry`. The core decides whether the change is an external negative stage change, whether the target is eligible, which active non-fainted Flower Veil holder blocks it, the normal or Errata range, and whether the mutation is prevented.

A prevented mutation emits the existing generic `RuleEffectEvent` contract with `sourceKind=ability`, the authoritative Flower Veil name, the blocking holder as source actor, the affected combatant as target, the originating move ID, and `effect=combat_stage_block`.

The Minecraft/Cobblemon integration consumes that event through the existing `rule_effect` playback path. It produces a `RULE_EFFECT_CUE` while preserving source, target, move, effect, amount and authoritative HP. Adapter-side hints about radius, Grass typing, requested delta or applied delta are ignored.

No Flower Veil mechanic is implemented in the mod. Minecraft must not inspect the target type, calculate range, select the ability holder, decide whether a stage drop is external, cancel a mutation, or infer a replacement result. Those decisions remain AutoPTU-Java-owned.

This is bounded playback evidence. `CORE_CALCULATIONS_AND_COMBAT_STATS` remains VERIFIED for the currently proven generic combat-stat infrastructure, while `ABILITIES` and Minecraft/Cobblemon/Craftics playback remain PARTIAL. Flower Veil does not imply support for Big Pecks, Hyper Cutter, Clear Body, Full Metal Body or the wider PTU ability library.
