# AutoPTU Cobblemon RPG implementation backlog

This file is the implementation queue for the Minecraft RPG. Compatibility work is a guardrail. A run should normally implement the first safe player-visible or persistent item below, add tests/runtime evidence, and update its status.

Status vocabulary: DONE = usable in production code now. IN PROGRESS = open implementation exists. NEXT = highest-priority safe slice. TODO = not started. BLOCKED = requires an upstream AutoPTU-Java contract. DEV = diagnostic/admin surface, not intended as final player UX.

## 1. Player identity and character state

- DONE `/autoptu` server namespace.
- DONE authenticated Minecraft UUID -> stable canonical Trainer id provisioning on join.
- NEXT `/autoptu trainer` show canonical Trainer identity, party summary and progression summary.
- TODO `/autoptu trainer name <name>` validated server-owned Trainer display name.
- TODO `/autoptu trainer sheet` inspect canonical PTU stats/classes/features without trusting client values.
- TODO persistent Trainer progression/XP/level service.
- TODO Trainer class/Feature acquisition service with canPerform validation.
- TODO reconnect/restart recovery test for full Trainer aggregate.

## 2. Starter and party

- IN PROGRESS `/autoptu starter bulbasaur|charmander|squirtle` one-time server-owned starter selection.
- NEXT give selected starter complete server-owned PTU battle bootstrap data from a canonical species/template source; do not derive trusted PTU stats from Cobblemon presentation data.
- TODO bind canonical starter/Pokemon id to a Cobblemon party slot for presentation while preserving canonical ownership.
- TODO `/autoptu party` show canonical active party with HP/status/level.
- TODO `/autoptu party lead <slot>` change active lead with server validation.
- TODO `/autoptu party swap <a> <b>` reorder party.
- TODO `/autoptu party deposit <slot>` move a Pokemon to canonical storage.
- TODO `/autoptu party withdraw <pokemonId>` move a Pokemon from storage to party when capacity allows.
- TODO canonical party aggregate independent from battle encounter profiles; migrate healing/encounter selection to it.
- TODO `/autoptu box` inspect canonical stored Pokemon.
- TODO Cobblemon PC/world UI adapter backed by canonical party/box services.

## 3. Healing and Pokemon condition

- DONE `/autoptu healparty` restores canonical HP only, persists it, verifies ownership and revisions.
- NEXT world healing-station interaction that calls the same canonical healing service.
- TODO `/autoptu heal <slot>` single-Pokemon HP service.
- BLOCKED full status cure service until AutoPTU-Java status cure/tick semantics are complete enough to avoid inventing PTU behavior.
- BLOCKED injury treatment until canonical PTU injury treatment/rest rules are available.
- TODO healing-center NPC/service presentation with dialogue and server-side interaction distance checks.

## 4. Inventory, equipment and item services

- TODO `/autoptu inventory` show canonical server-owned item quantities/reservations.
- TODO `/autoptu use <item> <target>` request canonical item use; server resolves inventory truth and legality.
- TODO `/autoptu giveitem` DEV/admin-only canonical fixture command, permission gated.
- TODO `/autoptu equip <item> <pokemon>` and `/autoptu unequip <pokemon>` using canonical held-item instances.
- TODO `/autoptu craft <recipe>` server-owned recipe eligibility and atomic material reservation/commit.
- TODO `/autoptu recipes` list only recipes the server says the Trainer may know/use.
- TODO world chest/shop/vendor adapters backed by canonical inventory services.
- BLOCKED battle effects of incomplete items remain AutoPTU-Java-owned and must not be reimplemented here.

## 5. World interaction framework

- TODO canonical `canInteract(player, target, context)` service.
- TODO interaction request DTO containing target identity and world position, never trusted outcomes.
- TODO server distance/dimension/permission validation.
- TODO NPC identity registry and persistent NPC state.
- TODO `/autoptu interact <targetId>` DEV parity surface for world right-click interactions.
- TODO dialogue state/session service.
- TODO service NPCs: healer, shopkeeper, professor/starter giver, quest giver, battle Trainer.
- TODO persistent interactable object state for switches, doors, loot, shrines and quest objects.

## 6. Wild encounters

- DONE server-owned wild encounter blueprint, identity and preparation infrastructure exists.
- DONE authenticated player-to-canonical encounter identity binding infrastructure exists.
- TODO biome/time/weather/location encounter-table registry owned by the RPG mod.
- TODO server-owned encounter roll/selection service with deterministic seed and no client outcome authority.
- TODO proximity/grass/world trigger that creates a wild encounter request.
- TODO visible Cobblemon wild actor correlation -> canonical wild Pokemon identity.
- TODO `/autoptu encounter test` DEV command using the same production encounter service.
- TODO normal player-vs-wild handoff replacing the isolated `/autoptu testbattle` harness.
- TODO cleanup/recovery when a player disconnects during encounter preparation.

## 7. Tactical battle entry and arena projection

- DONE first manually playable 1v1 AutoPTU-Java battle harness exists.
- DONE stable BlockPos/GridCoord and world-arena infrastructure exists.
- TODO materialize an arena from the actual world encounter location.
- TODO freeze immutable canonical Trainer/Pokemon snapshots before battle.
- TODO reserve party/items before battle and release/commit them atomically.
- TODO spawn/bind Cobblemon presentation entities from canonical combatant identities.
- TODO transition player camera/input into battle presentation.
- TODO `/autoptu battle state` DEV read-only snapshot view.
- TODO `/autoptu battle cancel` DEV recovery command that never fabricates a battle result.

## 8. Battle controls and playback

- DONE server-side battle action networking foundation exists.
- TODO player move/action selection UI driven only by legal actions supplied by AutoPTU-Java.
- TODO movement target overlay from authoritative legal-grid data.
- TODO target selection for single, area and self effects based on upstream contracts.
- TODO generic BattleEvent playback dispatcher.
- TODO HP change, faint, relocation and reaction playback through semantic events.
- TODO turn/round HUD based on authoritative lifecycle state.
- TODO animation queue with deterministic event ordering.
- BLOCKED forced movement/push/pull/knockback playback requiring incomplete upstream movement authority stays disabled.
- BLOCKED unsupported status/item/ability/Trainer Feature effects must not be synthesized locally.

## 9. Battle result and return to world

- TODO authoritative winner/loser completion record.
- TODO canonical post-battle HP commit to persistent Pokemon aggregates.
- TODO canonical status/injury commit only for upstream-supported lifecycle semantics.
- TODO release inventory/party encounter reservations.
- TODO despawn battle presentation entities and return control to normal world play.
- TODO reconnect/restart recovery for interrupted battles.
- TODO `/autoptu battle recover` DEV command that resumes/rolls back from durable authoritative state, never from client claims.

## 10. Capture

- TODO capture request service and inventory reservation boundary.
- BLOCKED capture probability/outcome remains unavailable until the authoritative engine contract is defined/verified.
- TODO after authoritative success: create canonical owned Pokemon, persist it, send to party or box by capacity.
- TODO Cobblemon capture animation as presentation only.

## 11. Shops, currency and economy

- TODO canonical wallet/currency aggregate.
- TODO `/autoptu wallet` read balance.
- TODO server-owned shop catalog and price service.
- TODO buy transaction with atomic currency/item mutation.
- TODO sell transaction with canonical item ownership checks.
- TODO Minecraft NPC/shop UI adapter.

## 12. Quests and progression

- TODO canonical quest definition registry.
- TODO persistent per-player quest state and revisioning.
- TODO `/autoptu quests` and `/autoptu quest <id>` read current objectives.
- TODO world/NPC quest accept/advance/complete interactions.
- TODO reward transaction for XP/items/currency/Pokemon.
- TODO event-driven objective updates from canonical battle/world events.
- TODO badges/league/faction/reputation progression state.

## 13. Crafting and field services

- TODO `canCraft`, `canUse`, `canPerform`, `canInteract` server-side service family.
- TODO canonical recipe registry and crafting transaction.
- TODO field medicine/camp service boundaries.
- TODO traversal capability checks sourced from canonical Pokemon/Trainer state.
- BLOCKED PTU mechanical modifiers from Features/items remain upstream-owned until verified.

## 14. World simulation

- TODO persistent region/location identity.
- TODO encounter ecology/spawn budgets.
- TODO day/time/weather world-event projection without duplicating battle weather rules.
- TODO roaming Trainers and scheduled NPC state.
- TODO server-owned world events and one-time interactable state.
- TODO dungeon/arena checkpoint persistence.

## 15. Player UI

- TODO Trainer HUD summary.
- TODO party sidebar with canonical HP/status.
- TODO inventory screen.
- TODO quest journal.
- TODO NPC dialogue/service screen.
- TODO battle HUD/action picker.
- TODO result/reward screen.
- TODO error/recovery messaging that distinguishes unavailable, blocked, stale revision and invalid action.

## 16. Admin/development tools

These commands exist to inspect or repair server-owned state. They must require permissions and must never become a client authority path.

- TODO `/autoptu admin player <player>` canonical Trainer dump.
- TODO `/autoptu admin pokemon <id>` canonical Pokemon dump.
- TODO `/autoptu admin party <player>` party dump.
- TODO `/autoptu admin encounter <player>` reservation/encounter dump.
- TODO `/autoptu admin validate <player>` cross-store ownership/revision validation.
- TODO `/autoptu admin recover <player>` explicit recovery workflow from durable state.
- TODO `/autoptu admin spawnencounter <table>` deterministic test encounter.
- TODO `/autoptu admin evidence` export runtime evidence for CI/manual testing.
- TODO destructive reset/remove tools only after backup, permission and audit-log contracts exist.

## 17. Automation selection rule

At the beginning of each implementation run:

1. Refetch main and inspect the whole repository tree plus relevant implementation files.
2. Briefly inspect AutoPTU-Java and the Python oracle only to establish authority boundaries for the candidate slice.
3. Start with the first `NEXT` item that is safe and does not duplicate PTU rules. If blocked, move to the next safe TODO in this file.
4. Make a production Minecraft/persistence/network/UI change. Documentation or compatibility-only changes do not satisfy a run when a safe product slice exists.
5. Add unit/integration/runtime evidence appropriate to the slice.
6. Mark completed work DONE and promote the next bounded dependency to NEXT.
7. Merge only with required checks green.

The target loop remains: authenticated player -> persistent Trainer -> starter -> party -> world interaction -> wild encounter -> AutoPTU-Java battle -> semantic Cobblemon playback -> authoritative result -> persistent post-battle state -> world play resumes.
