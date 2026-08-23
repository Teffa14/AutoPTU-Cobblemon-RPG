# Forced movement and reaction movement boundary

Inspected upstream heads:

- AutoPTU-Java: `aefc058328a9217d634477835a4851d521aaeccb`
- Current AutoPTU Python main inspected read-only: `29a8e62e24c3e58233ca2c8154a30d796099f90a`
- Python oracle pin used by the Java forced/reaction movement parity workflows: `16d228efa63aabecb67fa788959a359aac7f8f03`

AutoPTU-Java now owns an authoritative reaction movement application boundary. `ReactionMovementApplication.escapeThreatenedArea` derives legal reachability from canonical runtime state and the combatant movement profile, applies the selected destination to the authoritative combatant, does not consume normal SHIFT action economy, and emits a semantic `ShiftResolvedEvent`. This is materially stronger than the earlier destination-only resolver and is safe for adapter playback after the upstream ability/reaction pipeline invokes it.

Current Python main still implements the corresponding Perception and Telepathy behavior by computing `targeting.affected_tiles`, obtaining `movement.legal_shift_tiles`, moving the defender to the farthest safe tile, and cancelling the incoming hit/damage when the reaction succeeds. That behavior remains the read-only oracle for semantic parity.

Generic forced movement remains incomplete. `ForcedMovementInstruction` / `ForcedMovementInstructionResolution` identifies PUSH or PULL intent plus distance from canonical move metadata, but it does not own direction, path, collision resolution, interception, terrain interaction, reaction ordering, or final relocation. Therefore the permanent `COMPLETE_MOVEMENT_BEHAVIOR` capability stays blocking and `IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK` remains disabled.

The Fabric production artifact is pinned to the inspected AutoPTU-Java commit above. Minecraft may consume authoritative reaction movement state and `ShiftResolvedEvent` for projection/playback. Minecraft must not parse move text, choose push/pull direction, derive a path from instruction distance, resolve collision/interception/reaction ordering, or relocate entities from generic forced-movement instructions.

Capability classification for this slice:

- Core movement legality: verified for the upstream reaction movement path used here.
- Complete movement behavior: partial; reaction escape application is authoritative, generic forced movement is still blocking.
- Terrain/weather/hazards/zones/forced movement/reactions: partial; the reaction movement seam exists, but the full reaction and generic forced-movement stack is not complete.
- Abilities: partial; Python Perception/Telepathy behavior is understood, while the Java generic pre-damage reaction registry is still under active upstream work.
- Minecraft/Cobblemon/Craftics adapter and playback: partial; semantic SHIFT playback is permitted only when emitted by authoritative Java state transitions.
