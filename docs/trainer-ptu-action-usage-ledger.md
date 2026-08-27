# Trainer PTU action usage ledger

This document defines how limited-frequency PTU Trainer actions are consumed from normal Minecraft play without turning ordinary Minecraft activity into PTU action economy.

## Gameplay rule

Walking, mining, building, crafting through vanilla systems, exploration, camera movement and other ordinary Minecraft play remain unrestricted. The ledger is consulted only when a player asks the server to create a PTU/RPG consequence through a canonical Trainer Feature/action.

A PTU action cost such as Standard, Swift, Shift or Free remains separate from its usage frequency. Minecraft does not create a global pool of Standard Actions per day. The anti-spam limit is the canonical PTU frequency attached to the selected action: At-Will, Daily, Scene, Encounter, Round or Turn, including xN limits represented as `maxUses`.

## Authority boundary

The client submits an action/feature selection only. It must never supply trusted frequency, `maxUses`, ownership, legality or effect values. Server code re-resolves the authenticated `CanonicalPlayerState`, confirms that the Trainer owns the Feature, and creates a `CanonicalTrainerActionRule` from trusted PTU data before it calls `TrainerActionUsageService`.

The ledger does not implement Trainer Feature effects. It only reserves and records usage. Damage, bonuses, statuses, movement, battle legality, target legality, RNG and results remain AutoPTU-Java authority. A later battle may receive a server-owned entitlement/context produced by an overworld action, but Minecraft must not calculate the battle effect itself.

Cobblemon entities and Cobblemon BattleState are not inputs to this ledger.

## Frequency windows

`AT_WILL` is allowed without a durable reservation. `DAILY` uses the server's persistent monotonic RPG day. `SCENE` and `ENCOUNTER` require a real canonical lifecycle context ID. The adapter must not invent those IDs from chunks, distance, visible entities or Cobblemon battle objects. `ROUND` and `TURN` are rejected with `BATTLE_CORE_OWNED` because AutoPTU-Java owns tactical action economy.

Daily time comes from the Minecraft Overworld only as a clock input. One Minecraft day is 24,000 world ticks. `FileTrainerActionUsageLedger` persists the highest observed Overworld day beneath the active world save. The effective day can only move forward. Sleeping can advance the world into a new Daily window. Reconnect, process restart, dimension travel and moving `/time` backwards cannot restore spent uses. Large forward time jumps enter the later window but do not bank the skipped days as extra charges.

`FabricCanonicalPlayerStoreRuntime` observes the Overworld day at server start and on server ticks. The file ledger avoids a disk write unless the high-water day advances.

## Reservation lifecycle

Limited-frequency actions use reserve, execute, commit semantics. A successful reservation immediately counts against the frequency cap. The PTU/RPG action executor then performs the allowed server-owned consequence. On success it commits the reservation. If execution fails before producing the consequence it releases the pending reservation.

Pending reservations survive restart and still consume the slot. This is intentional: a crash cannot be used to duplicate a Daily, Scene or Encounter use. Each operation has a stable server-side `operationId`; retrying the same operation returns the same reservation instead of spending a second use. Reusing that operation ID for a different canonical action is rejected.

Both pending and committed entries count against the active window. A released pending entry no longer counts. A committed entry cannot be released.

## Current implementation boundary

This slice supplies the durable ledger, feature ownership check, monotonic Minecraft-day bridge and world-scoped repository wiring. It does not yet expose a player command or UI and it does not invent a local PTU Feature catalogue. The next action surface must resolve the selected Feature's frequency and xN limit from trusted server/upstream data before calling the service.

Until a canonical effect executor exists for a given Trainer Feature, recording its frequency is not permission to synthesize the effect in Minecraft. The safe failure mode is to show the action as unavailable or blocked while keeping the usage ledger reusable for Features whose authoritative effect contracts do exist.
