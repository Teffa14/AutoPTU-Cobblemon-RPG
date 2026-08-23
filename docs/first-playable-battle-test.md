# First playable AutoPTU battle test

This build is the first manual graphical battle proof for AutoPTU inside Minecraft/Cobblemon. It is intentionally narrow. Its purpose is to prove that a player can enter Minecraft, choose one of three Pokémon test scenarios, watch two real Cobblemon entities attack, see authoritative HP fall, and reach a visible winner/loser state.

## Required client

- Minecraft 1.21.1
- Java 21
- Fabric Loader 0.17.2
- Fabric API 0.116.11+1.21.1
- Fabric Language Kotlin 1.13.6+kotlin.2.2.20
- Cobblemon 1.7.3+1.21.1
- `AutoPTU-Cobblemon-RPG-playable-test.jar` from the `Playable Test Build` GitHub Actions artifact

Put all required mods in the same Fabric `mods` directory. This test does not require a separate AutoPTU-Java jar: the exact inspected upstream core commit is compiled read-only and nested into the playable mod artifact.

## Start the battle

Enter a single-player world or a Fabric server where you can run commands. On login the mod prints the available test command.

Choose one scenario:

```text
/autoptu testbattle bulbasaur
/autoptu testbattle charmander
/autoptu testbattle squirtle
```

The selected Pokémon fights a server-spawned Pikachu automatically.

## Expected visible result

Two real Cobblemon Pokémon appear near the player. Each has a visible `HP x/30` nameplate. Turns alternate automatically. The attacker lunges toward the target as a presentation cue and returns to its presentation anchor. AutoPTU-Java performs the accuracy roll, damage resolution, action consumption and HP mutation. Misses are possible. On a hit, the target's displayed Cobblemon HP and nameplate mirror the authoritative result.

The pinned deterministic demo has also been exercised directly against the embedded core: its current sequence resolves 14 damage, 12 damage and 16 damage, ending the battle on the third landed attack. This check exists only to ensure the first manual build cannot stall because of zero-damage fixture inputs.

When authoritative target HP reaches zero, the player receives a `BATTLE OVER` message containing the winner and loser. The entities remain briefly so the KO state can be seen, then the test cleans them up.

## Authority boundary

The command selects a server-owned demo scenario only. It does not read the selected Cobblemon entity's level, stats, HP, moves, abilities, items or battle state as PTU truth.

The current playable harness uses fixed server-owned combat inputs because general `RuntimeCombatantState` materialization is still blocked on unresolved dynamic movement/evasion/damage-modifier inputs. The fixed test inputs are passed into the current AutoPTU-Java `BattleRuntime.applyAuthoritativeMove` contract. The upstream core owns RNG, hit/miss, damage, authoritative HP mutation and action consumption.

The visible lunge is animation only. It is not a PTU Shift, forced movement, push, pull, knockback or interception decision. The entity returns to the same presentation anchor after the cue.

## Compatibility matrix entry

This playable vertical is pinned to read-only AutoPTU-Java `967b16237c6ea93a939bd4acbbe67da979885a60` and Python AutoPTU `9df36aeae4bcbef49fd5edb658b51d68bd45fa71` for this validation run.

It consumes `CORE_CALCULATIONS_AND_COMBAT_STATS` VERIFIED for the bounded arithmetic/stat contracts, `ACTION_ECONOMY_AND_INITIATIVE` VERIFIED for `ActionBudget` consumption, `FULL_STATEFUL_DAMAGE_PIPELINE` PARTIAL only through the currently verified direct `BattleRuntime.applyAuthoritativeMove` path, and `MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK` PARTIAL for live entity presentation. The demo does not consume either BLOCKING category.

The inspected Java head also adds Mirror Armor reflection through the generic combat-stage prevention hook path. That remains representative ability support only. `ABILITIES` stays PARTIAL and Mirror Armor is disabled in this demo rather than reproduced in Fabric.

## Deliberately disabled for this milestone

This test does not execute statuses, abilities, held-item effects, Trainer Features, terrain, weather, hazards, reactions, forced movement, tactical AI scoring, XP, loot, capture, campaign progression or durable battle outcomes. It also does not claim that the full stateful damage pipeline or lifecycle is complete.

The first test uses a command rather than the normal player party/encounter UI. That is deliberate. It gives us a reproducible graphical vertical to validate entity playback and the embedded authoritative core before normal encounter UX is allowed to depend on it.

## What to record when testing

If the test fails, keep `logs/latest.log` from the Minecraft instance and record the screen from just before running the command through the failure. Useful failures include the command not appearing, a Pokémon failing to spawn, entities not lunging, HP not changing, a crash, an endless battle, or no winner being announced.
