# Minecraft RPG Tooling Plan

This is the product execution queue for the playable AutoPTU Cobblemon RPG.

It is not a compatibility report. It lists the tools, commands, screens, world interactions, server services, persistence domains, and recovery utilities that must exist inside Minecraft.

## Mandatory task rule

Every AutoPTU-Cobblemon-RPG work run must read this file after the short read-only AutoPTU-Java/AutoPTU inspection and before selecting work.

1. Pick the first safe `NEXT` item.
2. If it is blocked by upstream authority, record the blocker and immediately choose the next safe Minecraft item.
3. A normal run must change production code. SHA refreshes, matrix edits, docs, compatibility booleans, and PR watches do not count as the slice when a safe product item exists.
4. When an item ships, change its status here and add its PR/commit reference.
5. Minecraft may request, persist, present, animate, and interact. It must not invent PTU legality, RNG, damage, statuses, abilities, Trainer Features, capture results, or battle outcomes.
6. Clients submit requests and selections only. The server re-resolves player identity, party, inventory, target, eligibility, progression, and canonical state.

## Status

- `LIVE`: merged and usable.
- `NEXT`: highest-priority safe implementation.
- `TODO`: required but not implemented.
- `BLOCKED`: dependency or upstream authority missing.
- `DEV_ONLY`: test/build/debug surface, not final gameplay.

## Namespace doctrine

- `/autoptu ...` = player/RPG bootstrap and fallback tools.
- `/autoptu admin ...` = operator/debug/recovery tools.
- `/ouros ...` = world-authoring/build-review tools.

Slash commands are not the final UX. Normal play should move to screens, keybinds, NPC dialogue, right-click interactions, blocks, menus, world triggers, and contextual prompts backed by the same server-authoritative services.

---

# Current live tools

| ID | Status | Tool | Purpose |
|---|---|---|---|
| CUR-001 | LIVE | authenticated player provisioning | Creates/loads the minimal canonical Trainer from the authenticated Minecraft UUID. |
| CUR-002 | DEV_ONLY | `/autoptu testbattle bulbasaur|charmander|squirtle` | Visible 1v1 battle harness. Must be replaced by the real canonical world loop. |
| CUR-003 | LIVE | `/autoptu healparty` | Merged via PR #198. Restores persistent canonical party HP only. |
| CUR-004 | DEV_ONLY | `/ouros world cedar_meadow` | Places the Cedar Meadow wildlife prototype. |
| CUR-005 | DEV_ONLY | `/ouros build meridian_canopy_gym` | Places Meridian for build review. |
| CUR-006 | DEV_ONLY | `/ouros build grand_palace` | Places the Grand Palace for build review. |
| CUR-007 | LIVE | `/autoptu starter list` | PR #202, commit `e86b1d2144a1faa35be19bb408f1e301033c4863`. Shows only server-configured starter choices. |
| CUR-008 | LIVE | `/autoptu starter choose <species>` | PR #203, commit `fb74ac9470ceaf25c13ab02337038ef3b75e2b3d`. Persists one server-authoritative starter and party binding. |
| CUR-009 | LIVE | `/autoptu party` | PR #204, commit `ab484b9ebc753668a1271bae27e9f56395584bb1`. Shows the durable canonical party without trusting Cobblemon stats. |

---

# P0 execution queue — first complete RPG loop

Work these point by point.

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P0-001 | LIVE | Starter catalogue + `/autoptu starter list` | Shipped via PR #202 / commit `e86b1d2144a1faa35be19bb408f1e301033c4863`; server exposes only configured starter choices. |
| P0-002 | LIVE | `/autoptu starter choose <species>` | Shipped via PR #203 / commit `fb74ac9470ceaf25c13ab02337038ef3b75e2b3d`; one-time choice creates a canonical Pokémon, assigns ownership, persists it, and puts it in the persistent party. Duplicate claims fail closed. |
| P0-003 | LIVE | `/autoptu party` | Shipped via PR #204 / commit `ab484b9ebc753668a1271bae27e9f56395584bb1`; shows canonical slot order, species, level, HP when available, and status summary. |
| P0-004 | NEXT | `/autoptu pokemon <slot>` | Shows a detailed canonical Pokémon summary. |
| P0-005 | TODO | Healing station interaction | A real block/NPC/facility calls the same server healing service as `/autoptu healparty`. |
| P0-006 | TODO | Server-owned wild encounter table | Zone/context selects a server-owned wild blueprint without using Cobblemon stats as PTU truth. |
| P0-007 | TODO | World encounter trigger | Walking/interacting in a configured context can request a wild encounter. |
| P0-008 | TODO | Party-to-encounter handoff | Active canonical party + wild blueprint become an immutable reservation. |
| P0-009 | TODO | Normal player-vs-wild battle start | World encounter starts AutoPTU-Java using persistent canonical actors. |
| P0-010 | TODO | Battle choice UI | Displays authoritative legal choices and submits only choice IDs/targets. |
| P0-011 | TODO | Normal semantic battle playback | Movement, attacks, HP, statuses, faint and result are projected from authoritative events/results. |
| P0-012 | TODO | Post-battle commit | Supported authoritative HP/status/injury/item changes commit exactly once to durable RPG state. |
| P0-013 | TODO | Return-to-world transition | Battle session/reservations clean up and the player resumes world control. |
| P0-014 | TODO | Reconnect/restart recovery | No duplicate items, lost party state, or stranded battle session after disconnect/restart. |
| P0-015 | TODO | `/autoptu status` | Shows Trainer loaded, party count, save revision, current encounter/battle, and actionable blockers. |

---

# Player command catalogue

These commands are bootstrap/fallback surfaces. Each must call a reusable server service so UI/world interactions can replace the command later.

## Trainer and onboarding

| ID | Status | Command/service |
|---|---|---|
| CMD-001 | TODO | `/autoptu status` |
| CMD-002 | TODO | `/autoptu trainer` |
| CMD-003 | TODO | `/autoptu trainer skills` |
| CMD-004 | TODO | `/autoptu trainer classes` |
| CMD-005 | TODO | `/autoptu trainer features` |
| CMD-006 | LIVE | `/autoptu starter list` — PR #202 / `e86b1d2144a1faa35be19bb408f1e301033c4863` |
| CMD-007 | LIVE | `/autoptu starter choose <species>` — PR #203 / `fb74ac9470ceaf25c13ab02337038ef3b75e2b3d` |

## Party and Pokémon

| ID | Status | Command/service |
|---|---|---|
| CMD-020 | LIVE | `/autoptu party` — PR #204 / `ab484b9ebc753668a1271bae27e9f56395584bb1` |
| CMD-021 | TODO | `/autoptu party lead <slot>` |
| CMD-022 | TODO | `/autoptu party move <from> <to>` |
| CMD-023 | NEXT | `/autoptu pokemon <slot>` |
| CMD-024 | TODO | `/autoptu box` |
| CMD-025 | TODO | `/autoptu box deposit <partySlot>` |
| CMD-026 | TODO | `/autoptu box withdraw <boxSlot>` |
| CMD-027 | TODO | `/autoptu pokemon nickname <slot> <name>` with server validation |

## Care

| ID | Status | Command/service |
|---|---|---|
| CMD-040 | LIVE | `/autoptu healparty` — PR #198 |
| CMD-041 | TODO | `/autoptu care status` |
| CMD-042 | TODO | `/autoptu rest` |

## Bag, items and equipment

| ID | Status | Command/service |
|---|---|---|
| CMD-060 | TODO | `/autoptu bag` |
| CMD-061 | TODO | `/autoptu bag inspect <item>` |
| CMD-062 | TODO | `/autoptu use <item> [target]` |
| CMD-063 | TODO | `/autoptu held equip <partySlot> <item>` |
| CMD-064 | TODO | `/autoptu held remove <partySlot>` |

## Economy and crafting

| ID | Status | Command/service |
|---|---|---|
| CMD-080 | TODO | `/autoptu money` |
| CMD-081 | TODO | `/autoptu shop list` |
| CMD-082 | TODO | `/autoptu shop buy <offer> [qty]` |
| CMD-083 | TODO | `/autoptu shop sell <item> [qty]` |
| CMD-084 | TODO | `/autoptu cancraft <recipe>` |
| CMD-085 | TODO | `/autoptu craft <recipe> [qty]` |

## Quests, journal and travel

| ID | Status | Command/service |
|---|---|---|
| CMD-100 | TODO | `/autoptu journal` |
| CMD-101 | TODO | `/autoptu quests` |
| CMD-102 | TODO | `/autoptu quest <id>` |
| CMD-103 | TODO | `/autoptu quest track <id>` |
| CMD-104 | TODO | `/autoptu travel` |
| CMD-105 | TODO | `/autoptu travel <destination>` |
| CMD-106 | TODO | `/autoptu interact` debug/fallback for the targeted registered world interaction |

## Encounter and battle fallback

| ID | Status | Command/service |
|---|---|---|
| CMD-120 | TODO | `/autoptu encounter status` |
| CMD-121 | TODO | `/autoptu battle status` |
| CMD-122 | TODO | `/autoptu battle choices` |
| CMD-123 | TODO | `/autoptu battle choose <choiceId> [target]` |
| CMD-124 | BLOCKED | `/autoptu battle forfeit` until upstream owns/validates the outcome |
| CMD-125 | TODO | `/autoptu battle spectate <battleId>` |
| CMD-126 | DEV_ONLY | Current `/autoptu testbattle ...`; later move to `/autoptu admin battle demo ...` |

---

# Party, storage and capture systems

| ID | Status | System |
|---|---|---|
| PARTY-001 | TODO | Party screen/menu. |
| PARTY-002 | TODO | Persistent lead-slot mutation. |
| PARTY-003 | TODO | Persistent party reorder. |
| PARTY-004 | TODO | Persistent box/storage aggregate. |
| PARTY-005 | TODO | PC/storage terminal world interaction. |
| PARTY-006 | TODO | Deposit/withdraw atomic ownership-safe mutations. |
| PARTY-007 | BLOCKED | Capture request legality/RNG/result until upstream authority exists. |
| PARTY-008 | BLOCKED | Successful capture ownership commit until authoritative capture result exists. |

---

# World interaction catalogue

These should become the normal gameplay path.

| ID | Status | Interaction |
|---|---|---|
| WORLD-001 | TODO | First-join Trainer/onboarding screen. |
| WORLD-002 | TODO | Starter-selection screen with Pokémon preview. |
| WORLD-003 | TODO | Party HUD and party management screen. |
| WORLD-004 | TODO | Pokémon summary screen. |
| WORLD-005 | TODO | Healing machine/nurse/healer NPC. |
| WORLD-006 | TODO | PC/storage terminal. |
| WORLD-007 | TODO | Shop counter/NPC and buy/sell menu. |
| WORLD-008 | TODO | Crafting workstation/menu. |
| WORLD-009 | TODO | NPC dialogue interaction and dialogue screen. |
| WORLD-010 | TODO | Quest-giver/quest-object interaction. |
| WORLD-011 | TODO | Trainer challenge interaction. |
| WORLD-012 | TODO | Wild Pokémon contextual encounter interaction. |
| WORLD-013 | TODO | Grass/region movement encounter trigger. |
| WORLD-014 | TODO | Cave encounter trigger. |
| WORLD-015 | TODO | Water/fishing encounter trigger. |
| WORLD-016 | TODO | Interactive chests, switches, doors, terminals and shrines through `canInteract`. |
| WORLD-017 | TODO | Fast-travel point. |
| WORLD-018 | TODO | Inn/rest point. |
| WORLD-019 | TODO | Move tutor/relearner NPC shell. |
| WORLD-020 | TODO | Gym/league registration desk. |
| WORLD-021 | TODO | Gates/doors controlled by canonical badge/key/progression state. |
| WORLD-022 | TODO | Camp interaction. |
| WORLD-023 | TODO | Discovery/location trigger. |
| WORLD-024 | TODO | Persistent world-event objects. |
| WORLD-025 | TODO | Ambient Pokémon behavior framework that never becomes PTU stat truth. |

---

# Battle UX and presentation catalogue

| ID | Status | Tool |
|---|---|---|
| BUI-001 | TODO | Battle HUD: turn owner, action budget, HP/status, event log. |
| BUI-002 | TODO | Legal move/action menu from authoritative choice set. |
| BUI-003 | TODO | Grid targeting overlay from authoritative legal tiles/targets. |
| BUI-004 | TODO | Party switch menu from authoritative switch choices. |
| BUI-005 | TODO | Battle item menu from authoritative item choices. |
| BUI-006 | TODO | Trainer Feature menu from authoritative usable Features. |
| BUI-007 | TODO | Battle camera framing. |
| BUI-008 | TODO | Semantic move animation registry: event -> visuals/sound only. |
| BUI-009 | TODO | Damage/HP feedback and nameplates. |
| BUI-010 | TODO | Status icon/particle/text registry. |
| BUI-011 | TODO | Faint/KO presentation. |
| BUI-012 | TODO | Winner/loser/result screen. |
| BUI-013 | TODO | Post-battle result/reward screen. |
| BUI-014 | TODO | Spectator mode. |
| BUI-015 | TODO | Semantic battle trace/replay evidence. |
| BUI-016 | TODO | Battle soft-lock detection and safe-checkpoint recovery. |

---

# NPC, quest, progression and social systems

| ID | Status | System |
|---|---|---|
| RPG-001 | TODO | Server-owned NPC identity and dialogue-state framework. |
| RPG-002 | TODO | Quest journal persistence. |
| RPG-003 | TODO | Quest objective event processing. |
| RPG-004 | TODO | Idempotent quest reward commit. |
| RPG-005 | TODO | Trainer XP/level/progression persistence. |
| RPG-006 | TODO | Pokémon XP/progression/evolution-choice persistence shell. |
| RPG-007 | BLOCKED | PTU level-up/evolution/move-learning legality until upstream contract exists. |
| RPG-008 | TODO | NPC relationships/reputation. |
| RPG-009 | TODO | Faction reputation. |
| RPG-010 | TODO | Rival identity/history/story flags. |
| RPG-011 | TODO | Trainer records: wins/losses/badges/tournaments. |
| RPG-012 | TODO | World story flags and choice consequences. |
| RPG-013 | TODO | Mail/message system with idempotent rewards. |
| RPG-014 | TODO | Calendar/world-time hooks for events. |

---

# Facilities and economy

| ID | Status | Facility/service |
|---|---|---|
| FAC-001 | TODO | Pokémon Center/healer backed by canonical healing service. |
| FAC-002 | TODO | Shop catalogue/stock/price service. |
| FAC-003 | TODO | Canonical wallet/currency transactions. |
| FAC-004 | TODO | Crafting recipe registry. |
| FAC-005 | TODO | Atomic ingredient reservation/consumption/output commit. |
| FAC-006 | TODO | Move tutor/relearner service shell. |
| FAC-007 | TODO | Inn/rest service. |
| FAC-008 | TODO | Fast-travel unlock and destination service. |
| FAC-009 | TODO | Daycare/nursery persistence shell. |
| FAC-010 | TODO | Gym/league challenge registration and records. |

---

# Server-authoritative service boundaries required under UI/commands

| ID | Status | Boundary |
|---|---|---|
| SVC-001 | TODO | `canPerform(player, action, context)` |
| SVC-002 | TODO | `canInteract(player, object, context)` |
| SVC-003 | TODO | `canUse(player, item, target, context)` |
| SVC-004 | TODO | `canCraft(player, recipe, context)` |
| SVC-005 | TODO | `canTravel(player, destination, context)` |
| SVC-006 | TODO | `canStartEncounter(player, source)` |
| SVC-007 | TODO | `canStartBattle(player, reservation)` |
| SVC-008 | TODO | `canManageParty(player, mutation)` |
| SVC-009 | TODO | `canManageStorage(player, mutation)` |
| SVC-010 | TODO | `canAcceptQuest(player, quest)` |
| SVC-011 | TODO | `canAdvanceQuest(player, event)` |
| SVC-012 | TODO | `canClaimReward(player, source)` |
| SVC-013 | LIVE/PARTIAL | Item reservation/commit/rollback infrastructure exists; expand to complete RPG inventory use. |
| SVC-014 | LIVE/PARTIAL | Encounter reservation infrastructure exists; wire it to normal world encounters. |
| SVC-015 | LIVE/PARTIAL | Battle outcome commit infrastructure exists; wire it to the normal battle loop. |
| SVC-016 | TODO | Currency transaction commit/idempotency. |
| SVC-017 | TODO | Quest reward commit/idempotency. |
| SVC-018 | TODO | Persistent world-object mutation/idempotency. |
| SVC-019 | TODO | Reconnect/restart active-session recovery. |

---

# `/autoptu admin` catalogue

These are required for operations, testing and recovery. They must never be normal progression paths.

| ID | Status | Command |
|---|---|---|
| ADM-001 | TODO | `/autoptu admin player inspect <player>` |
| ADM-002 | TODO | `/autoptu admin player validate <player>` |
| ADM-003 | TODO | `/autoptu admin pokemon inspect <pokemonId>` |
| ADM-004 | TODO | `/autoptu admin party inspect <player>` |
| ADM-005 | TODO | `/autoptu admin inventory inspect <player>` |
| ADM-006 | TODO | `/autoptu admin quest inspect <player> [quest]` |
| ADM-007 | TODO | `/autoptu admin encounter inspect <player>` |
| ADM-008 | TODO | `/autoptu admin battle inspect <battleId>` |
| ADM-009 | TODO | `/autoptu admin battle demo <species> <opponent>`; eventual home for current testbattle. |
| ADM-010 | TODO | `/autoptu admin encounter spawn <table|blueprint>` |
| ADM-011 | TODO | `/autoptu admin heal <player>` |
| ADM-012 | TODO | `/autoptu admin grant starter <player> <species>` |
| ADM-013 | TODO | `/autoptu admin grant item <player> <item> [qty]` |
| ADM-014 | TODO | `/autoptu admin grant currency <player> <amount>` |
| ADM-015 | TODO | `/autoptu admin state validate [player]` |
| ADM-016 | TODO | `/autoptu admin state dump <player>` with safe/redacted output. |
| ADM-017 | TODO | `/autoptu admin reservations <player>` |
| ADM-018 | TODO | `/autoptu admin recover player <player>` |
| ADM-019 | TODO | `/autoptu admin recover battle <battleId>` |
| ADM-020 | TODO | `/autoptu admin rollback battle <battleId>` only to a durable safe checkpoint. |
| ADM-021 | TODO | `/autoptu admin featuregates` |
| ADM-022 | TODO | `/autoptu admin evidence battle <battleId>` |
| ADM-023 | TODO | `/autoptu admin reload rpg-config` |

---

# Persistence domains

| Status | Domain |
|---|---|
| LIVE | Canonical authenticated player aggregate. |
| LIVE | Canonical Pokémon aggregate. |
| LIVE | Canonical encounter-profile/party-selection persistence. |
| LIVE | Canonical item-instance/reservation infrastructure. |
| TODO | Explicit long-term active-party + box/storage aggregate if encounter profile is not sufficient. |
| LIVE | Starter/onboarding claim state via PR #203 / `fb74ac9470ceaf25c13ab02337038ef3b75e2b3d`. |
| TODO | Trainer presentation/profile data. |
| TODO | Trainer XP/level/progression. |
| TODO | Pokémon XP/progression/evolution choices. |
| TODO | Canonical inventory quantities and wallet. |
| TODO | Quest journal/objectives/reward claims. |
| TODO | NPC relationships/factions/rivals. |
| TODO | Badges/league/tournament records. |
| TODO | Discovery/world-event flags. |
| TODO | Fast-travel unlocks/checkpoints. |
| TODO | Active encounter session journal. |
| TODO | Active battle checkpoint/recovery journal. |
| TODO | Post-battle result commit ledger/idempotency keys. |

---

# Mechanics Minecraft must not invent

When any of these are incomplete upstream, skip them and continue with another safe item above.

- Complete forced movement, push, pull, knockback, interception and interaction-driven movement.
- Unverified damage modifiers or stateful damage hooks.
- Incomplete status lifecycle rules.
- Ability rules not executed/emitted authoritatively by AutoPTU-Java.
- Held-item or consumable battle rules not executed/emitted authoritatively by AutoPTU-Java.
- Trainer Feature/perk rules not executed/emitted authoritatively by AutoPTU-Java.
- Capture legality/RNG/outcome until an authoritative contract exists.
- PTU evolution, level-up and move-learning legality until an authoritative contract exists.
- Tactical AI policy until upstream owns it.
- Any battle hit, crit, damage, target legality, resource consumption or result supplied as trusted client truth.

---

# First playable RPG milestone

This milestone is complete only when a fresh player can do all of the following in one normal Minecraft world without dev-only setup commands:

1. Join.
2. Load/create persistent Trainer state.
3. Choose a starter from a server-owned list.
4. See the starter in a persistent party.
5. Inspect and heal the party through normal Minecraft UI/world interaction.
6. Walk into a configured encounter context.
7. Trigger a server-owned wild encounter.
8. Start an AutoPTU-Java battle from the persistent party.
9. Choose legal actions through Minecraft UI.
10. See movement, attacks, HP loss, statuses/fainting when authoritative, and winner/loser presentation.
11. Exit to the world.
12. See authoritative post-battle state persisted.
13. Disconnect/reconnect or restart and retain the same canonical RPG state.

Until this loop exists, compatibility-watch work is secondary to a safe item in this file.
