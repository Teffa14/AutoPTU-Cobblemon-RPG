# Multi-target move execution integration boundary

Read-only upstream inspected for this slice:

- AutoPTU-Java main: `14662fb67778e71f2d55fc7a74c43dd9a8b06fa1`
- AutoPTU Python main: `cd4668a1b0e7c995bc12f3768f7b04cfa0f1c896`
- AutoPTU-Java pinned Python oracle for the multi-target contract: `16d228efa63aabecb67fa788959a359aac7f8f03`

AutoPTU-Java #169 freezes a language-neutral ownership contract for ordinary moves with more than one effective target. The Python oracle resolves targets sequentially inside `resolve_move_targets`. Declaration-level action marking, frequency validation, frequency recording and move-used bookkeeping happen once outside that per-target loop.

That contract is evidence about ownership and ordering. It is not evidence that AutoPTU-Java currently executes complete multi-target/AoE damage in the authoritative runtime. AutoPTU-Java #168 supplies authoritative TILE-anchor validation and affected-target expansion. #169 freezes how later execution must spend resources and process targets. The adapter therefore remains fail-closed for AoE execution.

## Compatibility mapping

`CORE_TARGETING` supplies authoritative TILE anchor validation and affected-target expansion. This part is verified for the bounded targeting primitive.

`ACTION_ECONOMY_AND_INITIATIVE` supplies the ownership rule that declaration resources and move-frequency bookkeeping happen once outside the per-target loop. The ownership contract is parity-backed, while full multi-target runtime execution is not yet promoted.

`FULL_STATEFUL_DAMAGE_PIPELINE` is partial and blocking for this feature because each affected combatant still needs a complete authoritative per-target attack/damage resolution before Minecraft can project the result.

`MOVE_SPECIFIC_BEHAVIOR` remains partial. The adapter may transport move identity and authoritative results, but it may not infer or execute missing move-special semantics.

`MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK` remains partial. Once Java emits authoritative per-target semantic events and final state, Minecraft may animate/render them. It may not re-run targeting, action economy, frequency bookkeeping, attack rolls, damage, HP mutation or target ordering.

## Intentionally deferred

Minecraft does not loop through affected targets. It does not mark actions, spend resources, validate or record move frequency, record move usage, roll accuracy, calculate damage, mutate HP or choose target order for an area move. It does not approximate missing Java behavior from the Python implementation.

The compatibility gate reports the frozen ownership contract as available while separately reporting authoritative multi-target damage execution as unavailable. This prevents representative upstream progress from being interpreted as full-category support.

## Next bounded slice

When AutoPTU-Java wires authoritative TILE target expansion into ordinary multi-target move resolution, consume the resulting per-target semantic event stream in the adapter and prove deterministic ordering and one-time declaration bookkeeping with a DTO/mock fixture before enabling any live Cobblemon AoE playback.
