# Mirror Armor reflection playback boundary

AutoPTU-Java main `967b16237c6ea93a939bd4acbbe67da979885a60` now resolves Mirror Armor combat-stage reflection through the generic `CombatStagePreventionHookRegistry` and `CombatStageMutationService`. The core decides whether the requested change is a negative external stage change, checks authoritative ability suppression, prevents recursive Mirror Armor re-entry, applies the reflected delta to the original attacker, blocks the original target mutation, and emits the authoritative semantic events.

The relevant Python oracle contract is frozen by AutoPTU-Java from `auto_ptu/rules/battle_state.py`. The current AutoPTU main inspected for this run is `8489d95778c0fa13cb47853e77d8ae7a90cb3b64`; AutoPTU-Java's Mirror Armor parity gate pins Python oracle commit `16d228efa63aabecb67fa788959a359aac7f8f03` for the exact reflection contract.

The Minecraft/Cobblemon integration consumes the emitted `RuleEffectEvent` through the existing generic `rule_effect` playback path. For Mirror Armor the event carries `sourceKind=ability`, `sourceName=Mirror Armor`, the reflecting combatant as actor, the original attacker as target, the originating move ID, `effect=reflect`, the requested negative delta as amount, and authoritative actor HP. The adapter presents that event only.

Minecraft does not decide whether Mirror Armor triggers. It does not inspect the requested stat, calculate or apply the reflected stage change, check ability suppression, prevent recursion, mutate combat stages, cancel the original mutation, or synthesize a reflection result. Those operations remain entirely inside AutoPTU-Java.

Capability impact remains bounded. `CORE_CALCULATIONS_AND_COMBAT_STATS` stays VERIFIED for the generic combat-stage mutation and prevention infrastructure already proven. `ABILITIES` stays PARTIAL because Mirror Armor is one parity-backed ability contract and does not establish complete ability support. `MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK` stays PARTIAL because this slice proves semantic playback only and does not complete authenticated campaign battle flow, general runtime combatant materialization, full entity lifecycle or full battle playback.

The regression fixture intentionally supplies fake adapter hints for requested delta, stat and recursive suppression. The presentation projector must discard those hints and preserve only the authoritative event payload. This prevents later Minecraft code from treating presentation metadata as PTU legality or state.
