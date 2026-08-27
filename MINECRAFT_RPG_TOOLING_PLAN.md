# Minecraft RPG Tooling Plan

This file is the implementation queue for the playable AutoPTU Cobblemon RPG.

It is not a compatibility report. It is a product backlog for things a player can do, see, use, interact with, or persist inside Minecraft.

## Task execution rule

Every AutoPTU-Cobblemon-RPG work run must read this file after the read-only upstream inspection and before choosing its slice.

1. Pick the first safe `NEXT` item in the execution queue.
2. If that item is blocked by AutoPTU-Java authority, record the blocker and immediately pick the next safe Minecraft item.
3. A normal run must implement production code. Updating this file, SHAs, tests, docs, or compatibility flags alone does not count when a safe product item exists.
4. When a slice ships, update its status and add the PR/commit reference here.
5. Do not implement PTU legality, RNG, damage, status rules, move rules, abilities, Trainer Features, capture outcomes, or battle outcomes in Minecraft when AutoPTU-Java is the authority.
6. Minecraft owns world interaction, authenticated requests, persistent RPG state, presentation, networking, entities, UI, NPCs, facilities, encounter triggers, and playback of authoritative results.
7. Client packets are requests only. The server re-resolves identity, party, inventory, progression, targets, eligibility, and canonical state.

## Status legend

- `LIVE`: merged and usable in the normal mod/runtime.
- `IN_PR`: implemented in an open PR and awaiting merge/validation.
- `NEXT`: highest-priority safe implementation candidate.
- `TODO`: required, not implemented yet.
- `BLOCKED`: requires an upstream authority contract or another dependency first.
- `DEV_ONLY`: useful build/test/debug tool, not part of the final player loop.

## Command and interaction doctrine

`/autoptu` is the player/RPG runtime namespace.

`/autoptu admin` is the operator/debug/recovery namespace. Player progression must never depend on admin commands.

`/ouros` remains the world-authoring/build-review namespace.

Slash commands are bootstrap and fallback tools. A finished RPG should replace most player-facing commands with screens, keybinds, right-click interactions, NPC dialogue, blocks, menus, and contextual prompts while keeping the same server-authoritative services underneath.

---

# Current in-game inventory

| ID | Status | Tool | Purpose |
|---|---|---|---|
| CUR-001 | LIVE | automatic authenticated player provisioning | Creates/loads the minimal canonical Trainer aggregate from the authenticated Minecraft UUID. |
| CUR-002 | DEV_ONLY | `/autoptu testbattle bulbasaur|charmander|squirtle` | First visible 1v1 battle harness. Must be replaced by the canonical world-driven battle loop. |
| CUR-003 | IN_PR | `/autoptu healparty` | PR #198. Restores canonical party HP only, server-side and persistent. |
| CUR-004 | DEV_ONLY | `/ouros world cedar_meadow` | Builds the authored Cedar Meadow wildlife prototype. |
| CUR-005 | DEV_ONLY | `/ouros build meridian_canopy_gym` | Places the Meridian structure for visual/traversal review. |
| CUR-006 | DEV_ONLY | `/ouros build grand_palace` | Places the Grand Palace structure for visual/traversal review. |

---

# Execution queue

The task should work down this list point by point. It may skip a blocked item, but it must continue to another safe item in the same run.

## P0 — First complete persistent RPG loop

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P0-001 | NEXT | Starter catalogue service + `/autoptu starter list` | Server exposes only allowed starter choices from server-owned configuration. Client cannot submit arbitrary canonical Pokémon data. |
| P0-002 | TODO | `/autoptu starter choose <species>` | One-time starter choice creates a canonical Pokémon aggregate, assigns ownership, writes it durably, and creates/updates the player's persistent party. Duplicate starter claims fail closed. |
| P0-003 | TODO | `/autoptu party` | Player can inspect the server-owned current party in Minecraft with species, level, HP, status summary and stable slot order. |
| P0-004 | TODO | `/autoptu pokemon <slot>` | Player can inspect one canonical Pokémon in detail without trusting Cobblemon entity stats as truth. |
| P0-005 | TODO | Healing station world interaction | A block/NPC/facility interaction calls the same canonical party-healing service as `/autoptu healparty`; command becomes fallback/debug. |
| P0-006 | TODO | Server-owned wild encounter table | World/zone configuration selects species/encounter blueprint on the server. Minecraft biome/entity data may select a table but may not invent PTU stats. |
| P0-007 | TODO | World encounter trigger | Walking/interacting in an encounter zone can create a prepared server-owned wild encounter without a test command. |
| P0-008 | TODO | Canonical player-party to encounter handoff | The active party and wild blueprint create a reservation using durable canonical identities and immutable battle inputs. |
| P0-009 | TODO | Normal world-driven battle start | Replace the species-selecting test harness with authenticated player -> party lead -> wild encounter -> AutoPTU-Java runtime. |
| P0-010 | TODO | Battle choice UI/fallback commands | Minecraft displays authoritative legal choices and sends only choice identifiers/targets back to the server. |
| P0-011 | TODO | Semantic battle playback | Normal battle path visibly projects movement, attacks, HP, status cues, fainting and winner/loser from authoritative BattleEvents/results. |
| P0-012 | TODO | Post-battle canonical commit | Authoritative resulting HP/status/injury/item consumption that is supported by upstream gets committed to durable canonical state exactly once. |
| P0-013 | TODO | Return-to-world cleanup | Battle entities/session/reservations cleanly close and the player returns to normal world control with persistent result state. |
| P0-014 | TODO | Reconnect/restart battle recovery | Disconnect/restart cannot silently duplicate items, lose canonical party state, or strand the player in a dead battle session. |
| P0-015 | TODO | `/autoptu status` | One concise player-facing health check: Trainer loaded, party count, active encounter/battle, canonical save revision and any actionable blocker. |

## P1 — Party, storage, capture, inventory, NPCs and progression

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P1-001 | TODO | Party screen/menu | UI replaces `/autoptu party` for ordinary play. |
| P1-002 | TODO | `/autoptu party lead <slot>` | Server changes the lead Pokémon with ownership/party validation. |
| P1-003 | TODO | `/autoptu party move <from> <to>` | Reorders persistent party slots atomically. |
| P1-004 | TODO | Persistent storage/box service | Pokémon outside the active party remain durable and owned. |
| P1-005 | TODO | `/autoptu box` | Lists storage without exposing hidden server-only fields. |
| P1-006 | TODO | `/autoptu box deposit <partySlot>` | Moves a canonical Pokémon from party to storage with party-size/eligibility validation. |
| P1-007 | TODO | `/autoptu box withdraw <boxSlot>` | Moves an owned canonical Pokémon to party if a slot is available. |
| P1-008 | TODO | PC/storage world interaction | Real block/NPC UI uses the same party/storage services. |
| P1-009 | BLOCKED | Capture request flow | Minecraft can target/animate a capture attempt only after upstream owns legality/RNG/result contracts. |
| P1-010 | BLOCKED | Captured Pokémon commit | A successful authoritative capture transfers/creates durable ownership exactly once. |
| P1-011 | TODO | `/autoptu bag` | Lists canonical server-owned inventory and quantities. |
| P1-012 | TODO | `/autoptu bag inspect <item>` | Shows safe item metadata and allowed contexts. |
| P1-013 | TODO | `/autoptu use <item> [target]` | Sends a server request; server validates ownership, quantity, context, target and authoritative effect contract. |
| P1-014 | TODO | Held-item equip service | Equip/unequip uses canonical item instances and canonical Pokémon ownership. |
| P1-015 | TODO | `/autoptu held equip <partySlot> <item>` | Bootstrap command over the held-item service. |
| P1-016 | TODO | `/autoptu held remove <partySlot>` | Returns/removes a held item through canonical inventory mutation. |
| P1-017 | TODO | Ground/world item pickup | World pickup creates/mutates canonical inventory server-side rather than trusting vanilla stack truth. |
| P1-018 | TODO | NPC interaction framework | Server-owned NPC identity, dialogue state, service/quest hooks and interaction distance validation. |
| P1-019 | TODO | Dialogue UI | Contextual NPC dialogue choices; clients submit choice IDs only. |
| P1-020 | TODO | Trainer battle challenge | NPC battle setup uses canonical NPC roster and the normal battle handoff. |
| P1-021 | TODO | `/autoptu quests` | Lists active/completed server-owned quests. |
| P1-022 | TODO | `/autoptu quest <id>` | Displays objective state and rewards without granting them client-side. |
| P1-023 | TODO | `/autoptu quest track <id>` | Sets HUD tracking preference; quest truth stays on server. |
| P1-024 | TODO | Quest world triggers | NPC/object/location/battle events advance objectives idempotently. |
| P1-025 | TODO | Quest reward commit | Server grants canonical rewards exactly once. |
| P1-026 | TODO | Trainer progression state | XP/level/features/skills become durable RPG state with versioned mutations. |
| P1-027 | TODO | Pokémon progression state | Experience/level/evolution/move-learning inputs survive restart and later feed battle snapshots. |
| P1-028 | BLOCKED | Level-up rule application | Rule choices/effects that belong to PTU must come from upstream contracts, not Minecraft approximations. |
| P1-029 | TODO | `/autoptu journal` | Player-facing summary of quests, discoveries, factions/relationships and major progression. |

## P2 — Facilities, economy, crafting and world services

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P2-001 | TODO | Pokémon Center/healer facility | Real world service with interaction range, cooldown/cost rules if configured, and canonical healing mutation. |
| P2-002 | TODO | Shop service | Server-owned catalogue, stock/price policy and canonical purchase/sale transactions. |
| P2-003 | TODO | `/autoptu money` | Shows canonical currency/wallet. |
| P2-004 | TODO | `/autoptu shop list` | Bootstrap/debug catalogue view for the current shop context. |
| P2-005 | TODO | `/autoptu shop buy <offer> [qty]` | Server validates shop context, stock, currency and inventory capacity. |
| P2-006 | TODO | `/autoptu shop sell <item> [qty]` | Server validates ownership and sale eligibility. |
| P2-007 | TODO | Shop NPC/menu | Replaces ordinary use of slash shop commands. |
| P2-008 | TODO | Crafting recipe registry | Server-owned recipes and prerequisites. |
| P2-009 | TODO | `/autoptu cancraft <recipe>` | Reports authoritative eligibility and missing prerequisites. |
| P2-010 | TODO | `/autoptu craft <recipe> [qty]` | Atomic ingredient reservation/consumption and canonical output creation. |
| P2-011 | TODO | Crafting station interaction | World block/menu uses canCraft/craft services. |
| P2-012 | TODO | Move tutor/relearner service | NPC/facility requests server-owned move-change eligibility. |
| P2-013 | BLOCKED | PTU move-learning legality | Any PTU-specific learn/replace legality stays upstream until exposed authoritatively. |
| P2-014 | TODO | Inn/rest service | Persistent world rest checkpoint and allowed non-battle recovery service. |
| P2-015 | TODO | Fast travel service | Server-owned unlocked destinations, interaction requirement and safe teleport execution. |
| P2-016 | TODO | `/autoptu travel` | Lists unlocked fast-travel destinations. |
| P2-017 | TODO | `/autoptu travel <destination>` | Requests validated server travel when context permits. |
| P2-018 | TODO | Gate/badge/key interaction service | Doors/regions can require canonical progression without trusting vanilla inventory/client flags. |
| P2-019 | TODO | Daycare/nursery persistence shell | Server owns deposited Pokémon and time/progression bookkeeping; PTU effects remain gated. |
| P2-020 | TODO | League/gym registration service | Persistent challenge eligibility, badges/results and rematch state. |

## P3 — Battle usability and presentation

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P3-001 | TODO | Battle HUD | Turn owner, action budget, HP/status, legal-action affordances and event log are visible without chat spam. |
| P3-002 | TODO | `/autoptu battle status` | Fallback summary of authoritative battle/session state. |
| P3-003 | TODO | `/autoptu battle choices` | Fallback list of authoritative legal choices. |
| P3-004 | TODO | `/autoptu battle choose <choiceId> [target]` | Generic fallback action submission; no local legality calculation. |
| P3-005 | TODO | Grid targeting overlay | Renders authoritative legal tiles/targets and sends selected coordinates/identity only. |
| P3-006 | TODO | Party switch UI | Shows only authoritative switch choices. |
| P3-007 | TODO | Battle item UI | Shows only authoritative item choices/reservations. |
| P3-008 | TODO | Trainer Feature UI | Shows only upstream-authoritative usable features. |
| P3-009 | TODO | Battle camera framing | Camera follows active combatants without changing battle state. |
| P3-010 | TODO | Move animation registry | Semantic event type -> visual/sound animation. No move rules in renderer. |
| P3-011 | TODO | Damage/HP feedback | Nameplates/HUD/particles mirror authoritative HP events. |
| P3-012 | TODO | Status visual registry | Server-authoritative status events map to icons/particles/text. |
| P3-013 | TODO | Faint/KO presentation | Authoritative faint result visibly removes/recalls/disables the actor as appropriate. |
| P3-014 | TODO | Winner/loser presentation | Clear result UI and transition back to world. |
| P3-015 | TODO | Spectator mode | Read-only battle viewing without ability to submit actions. |
| P3-016 | TODO | Battle replay evidence | Persist semantic event trace suitable for deterministic debugging/replay where supported. |
| P3-017 | TODO | Battle soft-lock recovery | Detect invalid/empty action-state dead ends and recover from the last safe authoritative checkpoint without fabricating a result. |

## P4 — Exploration, encounters and living world

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P4-001 | TODO | Encounter-zone registry | Named server-owned regions map to encounter tables and world rules. |
| P4-002 | TODO | Grass/terrain encounter trigger | Movement through configured world material/region can request an encounter. |
| P4-003 | TODO | Cave encounter trigger | Location/zone based encounter requests. |
| P4-004 | TODO | Water/fishing encounter trigger | World interaction chooses a server-owned encounter table; PTU battle truth remains upstream. |
| P4-005 | TODO | Visible roaming wild actor linkage | A Cobblemon entity can correlate to a server-owned wild blueprint without becoming stat truth. |
| P4-006 | TODO | Wild actor despawn/recovery | Correlation cleanup cannot leak reservations or duplicate encounters. |
| P4-007 | TODO | NPC schedules/patrols | World behavior and presentation only; combat stats/rules stay canonical. |
| P4-008 | TODO | Interactive world objects | Chests, switches, doors, terminals, shrines and quest objects route through canInteract. |
| P4-009 | TODO | `canInteract` service | Validates player identity, distance, state, quest/progression requirements and object state server-side. |
| P4-010 | TODO | World event state | Durable one-time/repeating events with versioned server-owned outcomes. |
| P4-011 | TODO | Discovery/location journal | Entering authored landmarks records server-owned discoveries. |
| P4-012 | TODO | Camp interaction | Safe contextual rest, party summary and save-state services without inventing PTU healing rules. |
| P4-013 | TODO | Ambient Pokémon behavior framework | Presentation/world simulation remains separate from canonical battle stats and outcomes. |

## P5 — Social, factions and long-form RPG state

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P5-001 | TODO | NPC relationship/reputation state | Durable server-owned relationship values and flags. |
| P5-002 | TODO | Faction reputation | Persistent reputation gates dialogue, services and quests. |
| P5-003 | TODO | Rival state | Persistent rival identity, encounter history and story flags. |
| P5-004 | TODO | Trainer records | Wins/losses/badges/tournaments/major encounters persisted from authoritative outcomes. |
| P5-005 | TODO | World story flags | Server-owned narrative decisions survive restart/reconnect. |
| P5-006 | TODO | Choice consequences | Dialogue/quest choices mutate only server-owned validated state. |
| P5-007 | TODO | Mail/message system | NPC/system messages and rewards are durable and idempotent. |
| P5-008 | TODO | Calendar/world-time hooks | Events can react to Minecraft time while persistent RPG state controls eligibility. |

---

# Required `/autoptu` player command catalogue

These commands are bootstrap/fallback surfaces. The service underneath is the real requirement.

## Core/player

- [ ] `/autoptu status`
- [ ] `/autoptu trainer`
- [ ] `/autoptu trainer skills`
- [ ] `/autoptu trainer classes`
- [ ] `/autoptu trainer features`
- [ ] `/autoptu starter list`
- [ ] `/autoptu starter choose <species>`
- [ ] `/autoptu party`
- [ ] `/autoptu party lead <slot>`
- [ ] `/autoptu party move <from> <to>`
- [ ] `/autoptu pokemon <slot>`
- [ ] `/autoptu box`
- [ ] `/autoptu box deposit <partySlot>`
- [ ] `/autoptu box withdraw <boxSlot>`

## Care

- [ ] `/autoptu healparty` — implemented in PR #198, pending merge at time of this file's creation.
- [ ] `/autoptu care status`
- [ ] `/autoptu rest`

## Bag/items/economy

- [ ] `/autoptu bag`
- [ ] `/autoptu bag inspect <item>`
- [ ] `/autoptu use <item> [target]`
- [ ] `/autoptu held equip <partySlot> <item>`
- [ ] `/autoptu held remove <partySlot>`
- [ ] `/autoptu money`
- [ ] `/autoptu shop list`
- [ ] `/autoptu shop buy <offer> [qty]`
- [ ] `/autoptu shop sell <item> [qty]`
- [ ] `/autoptu cancraft <recipe>`
- [ ] `/autoptu craft <recipe> [qty]`

## Quests/world

- [ ] `/autoptu journal`
- [ ] `/autoptu quests`
- [ ] `/autoptu quest <id>`
- [ ] `/autoptu quest track <id>`
- [ ] `/autoptu travel`
- [ ] `/autoptu travel <destination>`
- [ ] `/autoptu interact` — debug/fallback request for the targeted registered interaction.

## Encounter/battle fallback

- [ ] `/autoptu encounter status`
- [ ] `/autoptu battle status`
- [ ] `/autoptu battle choices`
- [ ] `/autoptu battle choose <choiceId> [target]`
- [ ] `/autoptu battle forfeit` — only if upstream owns/validates the outcome.
- [ ] `/autoptu battle spectate <battleId>`

The existing `/autoptu testbattle ...` stays `DEV_ONLY` and should eventually move under `/autoptu admin battle demo` or be removed once the real loop is stable.

---

# Required world interactions and UI

These are equally important as slash commands and should eventually be the normal way to play.

- [ ] First-join Trainer/onboarding screen.
- [ ] Starter-selection screen with Pokémon preview and server-owned choice list.
- [ ] Party HUD and party management screen.
- [ ] Pokémon summary screen.
- [ ] PC/storage terminal.
- [ ] Healing machine / nurse / healer NPC.
- [ ] Shop counter/NPC and buy/sell menu.
- [ ] Crafting workstation/menu.
- [ ] NPC dialogue screen with server-owned dialogue node IDs.
- [ ] Quest journal and tracked-objective HUD.
- [ ] Trainer battle challenge interaction.
- [ ] Wild Pokémon contextual interaction/encounter trigger.
- [ ] Encounter-zone movement triggers.
- [ ] Battle HUD.
- [ ] Battle movement/target selection overlay.
- [ ] Battle move selection menu.
- [ ] Battle item selection menu.
- [ ] Battle Trainer Feature selection menu.
- [ ] Party switch menu.
- [ ] Faint/winner/loser presentation.
- [ ] Post-battle reward/result screen.
- [ ] Capture animation/UI when authoritative capture is available.
- [ ] Fast travel point.
- [ ] Inn/rest point.
- [ ] Gym/league registration desk.
- [ ] Move tutor/relearner NPC.
- [ ] Gates/doors/objects controlled by canonical progression.
- [ ] Quest interactables and world-event objects.
- [ ] Notifications for save, rewards, progression, failures and server-authority rejection reasons.

---

# Server-side RPG services that must exist under the UI

Every UI/command/world interaction should call one of these server-authoritative boundaries rather than mutating state directly.

- [ ] `canPerform(player, action, context)`
- [ ] `canInteract(player, object, context)`
- [ ] `canUse(player, item, target, context)`
- [ ] `canCraft(player, recipe, context)`
- [ ] `canTravel(player, destination, context)`
- [ ] `canStartEncounter(player, encounterSource)`
- [ ] `canStartBattle(player, reservation)`
- [ ] `canManageParty(player, requestedMutation)`
- [ ] `canManageStorage(player, requestedMutation)`
- [ ] `canAcceptQuest(player, quest)`
- [ ] `canAdvanceQuest(player, event)`
- [ ] `canClaimReward(player, rewardSource)`
- [ ] item reservation/commit/rollback
- [ ] encounter reservation/commit/rollback
- [ ] battle result commit/idempotency
- [ ] currency transaction commit/idempotency
- [ ] quest reward commit/idempotency
- [ ] persistent world-object mutation/idempotency
- [ ] reconnect/restart session recovery

---

# `/autoptu admin` operator and recovery tools

These are required to operate, test and recover a persistent RPG, but they are never normal player progression paths.

- [ ] `/autoptu admin player inspect <player>`
- [ ] `/autoptu admin player validate <player>`
- [ ] `/autoptu admin pokemon inspect <pokemonId>`
- [ ] `/autoptu admin party inspect <player>`
- [ ] `/autoptu admin inventory inspect <player>`
- [ ] `/autoptu admin quest inspect <player> [quest]`
- [ ] `/autoptu admin encounter inspect <player>`
- [ ] `/autoptu admin battle inspect <battleId>`
- [ ] `/autoptu admin battle demo <species> <opponent>` — eventual home for the current test battle harness.
- [ ] `/autoptu admin encounter spawn <table|blueprint>`
- [ ] `/autoptu admin heal <player>`
- [ ] `/autoptu admin grant starter <player> <species>`
- [ ] `/autoptu admin grant item <player> <item> [qty]`
- [ ] `/autoptu admin grant currency <player> <amount>`
- [ ] `/autoptu admin state validate [player]`
- [ ] `/autoptu admin state dump <player>` — safe/redacted canonical diagnostic output.
- [ ] `/autoptu admin reservations <player>`
- [ ] `/autoptu admin recover player <player>`
- [ ] `/autoptu admin recover battle <battleId>`
- [ ] `/autoptu admin rollback battle <battleId>` — only to an explicit durable safe checkpoint; never invent an outcome.
- [ ] `/autoptu admin featuregates`
- [ ] `/autoptu admin evidence battle <battleId>`
- [ ] `/autoptu admin reload rpg-config`

---

# Persistence domains required for the complete RPG

Existing durable state should be extended, not replaced by Cobblemon/vanilla client truth.

- [x] Canonical authenticated player aggregate.
- [x] Canonical Pokémon aggregate persistence.
- [x] Canonical player encounter profile/party-selection persistence.
- [x] Canonical item-instance/reservation infrastructure.
- [ ] Explicit active-party + box/storage aggregate if encounter profile is not sufficient as the long-term party model.
- [ ] Starter/onboarding claim state.
- [ ] Trainer identity/presentation profile.
- [ ] Trainer XP/level/progression.
- [ ] Pokémon XP/progression/evolution choices.
- [ ] Canonical inventory quantities/wallet.
- [ ] Quest journal/objectives/reward claims.
- [ ] NPC relationships/factions/rivals.
- [ ] Badges/league/tournament records.
- [ ] Discovery/world-event flags.
- [ ] Fast-travel unlocks/checkpoints.
- [ ] Shop/service transaction state where needed.
- [ ] Active encounter session journal.
- [ ] Active battle session/checkpoint/recovery journal.
- [ ] Post-battle result commit ledger/idempotency keys.

---

# Upstream-gated mechanics that Minecraft must not invent

When these are incomplete upstream, work on another item in this file instead of approximating them in the mod.

- Complete forced movement, push, pull, knockback, interception and interaction-driven movement.
- Any unverified damage modifier or stateful damage hook.
- Incomplete status lifecycle behavior.
- Ability rules not emitted/executed authoritatively by AutoPTU-Java.
- Held-item/consumable battle rules not emitted/executed authoritatively by AutoPTU-Java.
- Trainer Feature/perk rules not emitted/executed authoritatively by AutoPTU-Java.
- Capture legality/RNG/outcomes until there is an authoritative contract.
- Evolution/move-learning/level-up PTU legality until there is an authoritative contract.
- AI tactical policy until upstream owns the policy.
- Any battle result, hit result, crit, damage, status, resource consumption or legal-target decision supplied by the Minecraft client.

---

# Definition of the first playable RPG milestone

The milestone is complete only when a fresh player can do this in one normal Minecraft world without dev-only setup commands:

1. Join the server/world.
2. Load/create the persistent Trainer.
3. Choose a starter from a server-owned list.
4. See the starter in a persistent party.
5. Inspect and heal that party through normal world UI/interaction.
6. Walk into a configured encounter context.
7. Trigger a server-owned wild encounter.
8. Enter an AutoPTU-Java battle using the persistent party.
9. Choose legal battle actions through Minecraft UI.
10. See movement, attacks, HP loss, fainting and the winner/loser in Minecraft.
11. Exit the battle back into the world.
12. See the authoritative post-battle Pokémon/item state still present.
13. Disconnect/reconnect or restart the server and retain the same canonical RPG state.

Until this loop exists, compatibility-watch work must remain secondary to a safe item from this file.
