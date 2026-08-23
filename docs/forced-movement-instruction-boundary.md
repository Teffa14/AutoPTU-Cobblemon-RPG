# Forced movement instruction boundary

Inspected upstream heads:

- AutoPTU-Java: `3ede4a8493738ddc70b2f0eb3959973488f78db9`
- Current AutoPTU Python main inspected read-only: `ff069a928f936f4a1dca54597ef3f85348ea4b0b`
- Python oracle pin used by the Java forced/reaction movement parity workflows: `16d228efa63aabecb67fa788959a359aac7f8f03`

AutoPTU-Java currently exposes two bounded movement primitives relevant to this boundary. `ForcedMovementInstruction` / `ForcedMovementInstructionResolution` identifies PUSH or PULL intent plus a positive distance from canonical move metadata. `ReactionEscapeMovementResolution` chooses the farthest safe destination from already-authoritative reachable and threatened tile inputs, matching the pinned Python Perception/Telepathy hooks. Current Python main still performs those reaction shifts from `movement.legal_shift_tiles` and `targeting.affected_tiles` before mutating the defender position and emitting ability events.

The Fabric production artifact is pinned to the same inspected AutoPTU-Java commit, so the compatibility record describes code that is actually bundled.

Neither primitive is a complete spatial forced-movement engine. The instruction contract does not supply direction, path, collision handling, interception, terrain interaction or final relocation. The reaction escape resolver consumes reachable/threatened candidates but does not itself own the full ability trigger pipeline, reaction ordering, generic forced movement, push/pull/knockback interaction or Minecraft projection.

The permanent compatibility matrix therefore remains unchanged for `COMPLETE_MOVEMENT_BEHAVIOR`: it is blocking. `IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK` continues to inherit that blocking dependency.

Minecraft must not parse move text, choose push/pull direction, recompute reaction escape destinations, infer legal paths, resolve collision/interception/reaction ordering or relocate entities from partial movement primitives. The adapter will consume generic authoritative movement events/state only after AutoPTU-Java owns the complete legality/result contract.
