# Forced movement instruction boundary

Inspected upstream heads:

- AutoPTU-Java: `7de79dcd30b241d439724050fb24ee893a7c5c63`
- Current AutoPTU Python main inspected read-only: `1c673eb676fdeca71ee55e1de8a90b8f7d2cbcf3`
- Python oracle pin used by the Java forced-movement parity workflow: `16d228efa63aabecb67fa788959a359aac7f8f03`

AutoPTU-Java now has a parity-backed `ForcedMovementInstruction` / `ForcedMovementInstructionResolution` contract that identifies `PUSH` or `PULL` plus a positive distance from canonical move metadata. The current Python main exposes the same `forced_movement_instruction` behavior.

This does not resolve spatial forced movement. No authoritative path, direction, collision handling, interception, reaction ordering, knockback interaction, terrain interaction or final relocation is produced by this contract.

The permanent compatibility matrix therefore remains unchanged for `COMPLETE_MOVEMENT_BEHAVIOR`: it is blocking. `IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK` continues to inherit that blocking dependency.

Minecraft must not parse move text itself or convert the instruction into entity motion. The adapter will wait for upstream authoritative movement resolution and consume generic semantic events/state when that contract exists. This keeps PTU legality in AutoPTU-Java and prevents the Fabric/Cobblemon layer from manufacturing push/pull rules.
