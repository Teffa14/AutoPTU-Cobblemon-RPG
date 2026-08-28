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
7. Normal wild encounters must originate from visible roaming Pokemon actors registered by AutoPTU. Never create invisible random encounters from walking on grass, entering caves, swimming, fishing context alone, or other movement-only rolls. Environment/context may select which canonical wild actors are provisioned into the world, but combat starts only from an explicit visible actor interaction/engagement.
8. Cobblemon Pokemon entities may be used only as rendered/walking presentation actors. AutoPTU must never trust or read their Pokemon payload, species, level, HP, moves, statuses, ownership, BattleState, battle participants, RNG, faint/capture/healing eligibility or results. Presentation data is projected one-way from AutoPTU canonical state.

## Status

- `LIVE`: merged and usable.
- `NEXT`: highest-priority safe implementation.
- `NEXT/PARTIAL`: highest-priority safe implementation with a production subset already shipped on the current bounded PR/commit; continue the same item before advancing the queue.
- `TODO`: required but not implemented.
- `BLOCKED`: dependency or upstream authority missing.
- `BLOCKED/PARTIAL`: a production subset is live, but the remaining authoritative contract is blocked upstream; immediately advance another safe Minecraft item.
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
| CUR-010 | LIVE | `/autoptu pokemon <slot>` | PR #207, commit `b3fed8380f801222d6c549f1695b8bb98789a135`. Shows detailed durable canonical Pokemon state while leaving unavailable PTU inputs unavailable. |
| CUR-011 | LIVE | healing station block interaction | PR #209, commit `81ca566e645f749e7cb6b23cd0714dd91f706094`. Right-clicking an authored station invokes canonical persistent party HP healing. |
| CUR-012 | LIVE | server-owned wild encounter table | PR #211, implementation commit `705d4ab98a2e61b967c59447c957be765e7009e5`. Zone/context selects and freezes an already-authored canonical WILD roster without reading Cobblemon gameplay state. |
| CUR-013 | LIVE | visible wild actor interaction boundary | Registered visible Pokemon presentation actors can submit an encounter request by entity UUID -> AutoPTU-owned world binding. The adapter never reads Cobblemon Pokemon gameplay data. |
| CUR-014 | LIVE | `/autoptu party lead <slot>` | PR #219, commit `e0149f97939aec2926d6b828c00851eb86a6a538`. Promotes the selected durable canonical party member to lead with server-side slot resolution and optimistic concurrency. |
| CUR-015 | LIVE | authoritative battle choice menu/fallback | PR #220, implementation commit `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`. Displays only fresh AutoPTU-Java legal-choice stable keys and submits the exact revalidated choice without client-supplied battle scope or PTU legality. |
| CUR-016 | LIVE/PARTIAL | Fabric semantic battle playback runtime | PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` projects authoritative move animation, HP and relocation. PR #222 / `0d261c11027a0f715aee3fac8c6bd5adaa1fd9e4` adds visible authoritative `status_skip` particles/action-bar cues; semantic faint/result remain upstream-contract work. |
| CUR-017 | LIVE/PARTIAL | capability-sensitive world task assessment | Shipped via PR #226 / commit `2de8d720428b98a6d7375e793a89b5ff20e9c14c`. Server-authored graded quality curves and `/autoptu cancraft <recipe>` read only persistent canonical Trainer skill ranks; preview performs no RNG and consumes no materials. |
| CUR-018 | LIVE | crafting workstation interaction | PR #227 adds the physical workstation, PR #228 adds server-owned ingredient/output contracts, PR #229 adds the restart-safe craft transaction, PR #230 adds canonical material readiness, PR #231 / `1648de45c7cd58ab6a2232d7a0c7e744cf5b986a` adds normal-world server-observed ingredient deposits, PR #233 / `f12bca736699739fc9cdd67bedf640cc3b4f6f10` wires durable workstation crafting, PR #235 / `a4f763dade77a5aaa9f315eb7b8f3d369363afd3` adds explicit fallback recipe requests, and PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` adds the normal workstation recipe-selection surface: server-authored understood recipes are shown, canonical material readiness controls the craft affordance, and selection is revalidated through the existing server-authoritative craft route. PR #238 / implementation commit `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8` adds the durable Minecraft-stack-to-canonical handoff journal, persisted withdrawal checkpoint, reconnect reconciliation, and idempotent canonical receipt. |
| CUR-019 | LIVE | Ouros RPG calendar/world-event surface | PR #239 / implementation commit `7b888c957f1e4457dd3c0c5d86286879afde27ad`. Players can inspect the current durable calendar with `/autoptu calendar`; online players receive server-authored day-transition/event announcements. Stable event keys are world hooks only and carry no PTU rules or rewards by themselves. |

---

# P0 execution queue — first complete RPG loop

Work these point by point.

| ID | Status | Minecraft implementation | Done when |
|---|---|---|---|
| P0-001 | LIVE | Starter catalogue + `/autoptu starter list` | Shipped via PR #202 / commit `e86b1d2144a1faa35be19bb408f1e301033c4863`; server exposes only configured starter choices. |
| P0-002 | LIVE | `/autoptu starter choose <species>` | Shipped via PR #203 / commit `fb74ac9470ceaf25c13ab02337038ef3b75e2b3d`; one-time choice creates a canonical Pokémon, assigns ownership, persists it, and puts it in the persistent party. Duplicate claims fail closed. |
| P0-003 | LIVE | `/autoptu party` | Shipped via PR #204 / commit `ab484b9ebc753668a1271bae27e9f56395584bb1`; shows canonical slot order, species, level, HP when available, and status summary. |
| P0-004 | LIVE | `/autoptu pokemon <slot>` | Shipped via PR #207 / commit `b3fed8380f801222d6c549f1695b8bb98789a135`; shows an ownership-safe detailed canonical Pokémon summary and reports missing optional PTU inputs as unavailable. |
| P0-005 | LIVE | Healing station interaction | Shipped via PR #209 / commit `81ca566e645f749e7cb6b23cd0714dd91f706094`; a real authored Minecraft block signature calls the same canonical healing service as `/autoptu healparty`, with server-side distance/context checks. |
| P0-006 | LIVE | Server-owned wild encounter table | Shipped via PR #211 / implementation commit `705d4ab98a2e61b967c59447c957be765e7009e5`; exact server-owned zone/context selects and freezes an already-authored complete canonical WILD blueprint, with deterministic RPG/world selection separate from battle RNG and no Cobblemon gameplay-state inputs. |
| P0-007 | BLOCKED | Visible roaming wild encounter trigger | Visible actor interaction/request binding is live via PR #214, but normal ecology cannot complete this item until a trusted server-authored source can publish the complete canonical WILD blueprint before the Cobblemon presentation actor is revealed. The adapter must not derive missing PTU stats, moves, HP, statuses, abilities or legality from Cobblemon species/entity data. |
| P0-008 | BLOCKED | Party-to-encounter handoff | Core immutable handoff service shipped in PR #216 / implementation commit `167b61471893e9b21d9b2630dd65960117178939`: it freezes the authenticated canonical party, consumables, visible actor identity, world context and exact server-owned wild blueprint without rereading mutable client/Cobblemon state. Normal world wiring is blocked only on P0-007 publishing that exact blueprint. |
| P0-009 | BLOCKED | Normal player-vs-wild battle start | The battle-start boundary exists, but the normal world path cannot start AutoPTU-Java until P0-007 publishes the trusted complete WILD blueprint and P0-008 can wire that exact immutable handoff. Do not substitute Cobblemon BattleState or entity Pokémon data. |
| P0-010 | LIVE | Battle choice UI | Shipped via PR #220 / implementation commit `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`; the server binds player -> reservation/actor, displays only a fresh authoritative legal-choice set, accepts only a stable choice ID, re-fetches the action space, and executes the exact still-legal choice. |
| P0-011 | BLOCKED/PARTIAL | Normal semantic battle playback | PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` adds attack animation, HP and relocation projection. PR #222 / `0d261c11027a0f715aee3fac8c6bd5adaa1fd9e4` makes authoritative `status_skip` visible. Faint/result presentation is blocked until AutoPTU-Java emits an explicit authoritative semantic faint/result contract; Minecraft must not infer either from HP or local state. |
| P0-012 | TODO | Post-battle commit | Supported authoritative HP/status/injury/item changes commit exactly once to durable RPG state. |
| P0-013 | TODO | Return-to-world transition | Battle session/reservations clean up and the player resumes world control. |
| P0-014 | TODO | Reconnect/restart recovery | No duplicate items, lost party state, or stranded battle session after disconnect/restart. |
| P0-015 | LIVE | `/autoptu status` | Verified live on main `843e71a1fb6e9bc6cd1272342432cff7804d8dbe`; shows Trainer loaded, party count, save revision, current encounter/battle summary, and actionable blockers from server-owned state. |

---

# Player command catalogue

These commands are bootstrap/fallback surfaces. Each must call a reusable server service so UI/world interactions can replace the command later.

## Trainer and onboarding

| ID | Status | Command/service |
|---|---|---|
| CMD-001 | LIVE | `/autoptu status` — verified on main `843e71a1fb6e9bc6cd1272342432cff7804d8dbe` |
| CMD-002 | TODO | `/autoptu trainer` |
| CMD-003 | TODO | `/autoptu trainer skills` |
| CMD-004 | TODO | `/autoptu trainer classes` |
| CMD-005 | TODO | `/autoptu trainer features` |
| CMD-006 | LIVE | `/autoptu starter list` — PR #202 / `e86b1d2144a1faa35be19bb408f1e301033c4863` |
| CMD-007 | LIVE | `/autoptu starter choose <species>` — PR #203 / `fb74ac9470ceaf25c13ab02337038ef3b75e2b3d` |
| CMD-008 | BLOCKED/PARTIAL | `/autoptu trainer actions` — PR #224. Server-owned monotonic RPG day and Daily reservation are live; the final action list is blocked on authoritative PTU action-cost/frequency definitions and must not classify every Feature as Daily. |

## Party and Pokémon

| ID | Status | Command/service |
|---|---|---|
| CMD-020 | LIVE | `/autoptu party` — PR #204 / `ab484b9ebc753668a1271bae27e9f56395584bb1` |
| CMD-021 | LIVE | `/autoptu party lead <slot>` — PR #219 / `e0149f97939aec2926d6b828c00851eb86a6a538` |
| CMD-022 | TODO | `/autoptu party move <from> <to>` |
| CMD-023 | LIVE | `/autoptu pokemon <slot>` — PR #207 / `b3fed8380f801222d6c549f1695b8bb98789a135` |
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
| CMD-084 | LIVE | `/autoptu cancraft <recipe>` — PR #226 adds Trainer capability assessment, PR #228 adds server-owned workstation/ingredient/output contracts, and PR #230 reads only the authenticated player's unreserved durable canonical item stacks to report exact required/available/missing materials. It performs no reservation, RNG, or consumption. |
| CMD-085 | LIVE | `/autoptu craft <recipe> [qty]` — PR #229 supplies the durable server transaction, PR #231 supplies normal server-authoritative ingredient acquisition, PR #233 / `f12bca736699739fc9cdd67bedf640cc3b4f6f10` wires normal workstation execution, and PR #235 / `a4f763dade77a5aaa9f315eb7b8f3d369363afd3` adds the explicit fallback command. PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` exposes that same request path from the workstation selector. The client supplies only a recipe key and bounded quantity; the server re-resolves Trainer identity, authored recipe, knowledge, canonical materials, quality roll, reservations and output. |

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
| CMD-122 | LIVE | `/autoptu battle choices` — PR #220 / `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`; reads a fresh authoritative action space for the server-bound battle actor. |
| CMD-123 | LIVE | `/autoptu battle choose <choiceId>` — PR #220 / `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`; re-fetches legal choices and executes only the exact still-present authoritative stable key. |
| CMD-124 | BLOCKED | `/autoptu battle forfeit` until upstream owns/validates the outcome |
| CMD-125 | TODO | `/autoptu battle spectate <battleId>` |
| CMD-126 | DEV_ONLY | Current `/autoptu testbattle ...`; later move to `/autoptu admin battle demo ...` |

---

# Party, storage and capture systems

| ID | Status | System |
|---|---|---|
| PARTY-001 | TODO | Party screen/menu. |
| PARTY-002 | LIVE | Persistent lead-slot mutation — PR #219 / `e0149f97939aec2926d6b828c00851eb86a6a538`. |
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
| WORLD-005 | LIVE | Healing machine/nurse/healer interaction — PR #209 / `81ca566e645f749e7cb6b23cd0714dd91f706094`. |
| WORLD-006 | TODO | PC/storage terminal. |
| WORLD-007 | TODO | Shop counter/NPC and buy/sell menu. |
| WORLD-008 | LIVE | Crafting workstation/menu. PR #227 adds the physical workstation, PR #228 shows server-owned ingredient/output contracts, PR #229 supplies the restart-safe transaction, PR #230 shows canonical material readiness, PR #231 adds server-observed ingredient deposit, PR #233 / `f12bca736699739fc9cdd67bedf640cc3b4f6f10` wires durable normal-use crafting, PR #235 / `a4f763dade77a5aaa9f315eb7b8f3d369363afd3` adds explicit server-authoritative recipe selection through `/autoptu craft`, and PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` makes normal workstation use show an explicit selector with ready/unready state derived from canonical materials. PR #238 / implementation commit `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8` makes ingredient transfer crash-recoverable through a durable cross-store journal and reconnect recovery. |
| WORLD-009 | TODO | NPC dialogue interaction and dialogue screen. |
| WORLD-010 | TODO | Quest-giver/quest-object interaction. |
| WORLD-011 | TODO | Trainer challenge interaction. |
| WORLD-012 | LIVE | Visible wild Pokémon contextual encounter interaction boundary — PR #214. A registered roaming actor is the encounter surface and carries an AutoPTU-owned encounter identity without reading Cobblemon gameplay state. |
| WORLD-013 | BLOCKED | Region/grass ecology provisioning for visible roaming wild Pokémon. Population/presence must be backed by a trusted server-authored complete canonical WILD blueprint source before reveal; it never creates invisible movement encounters. |
| WORLD-014 | TODO | Cave ecology provisioning for visible roaming wild Pokémon; no movement-only encounter roll. |
| WORLD-015 | TODO | Water/fishing ecology provisioning for visible/swimming/fishable wild actors; no context-only battle trigger. |
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
| BUI-002 | LIVE | Legal move/action menu from authoritative choice set — PR #220 / `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`; server-bound scope plus fresh stable-key revalidation, with no Minecraft legality calculation. |
| BUI-003 | TODO | Grid targeting overlay from authoritative legal tiles/targets. |
| BUI-004 | TODO | Party switch menu from authoritative switch choices. |
| BUI-005 | TODO | Battle item menu from authoritative item choices. |
| BUI-006 | TODO | Trainer Feature menu from authoritative usable Features. |
| BUI-007 | TODO | Battle camera framing. |
| BUI-008 | LIVE/PARTIAL | Semantic move animation registry: PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` routes authoritative move IDs through the generic Fabric presentation gateway; richer move-specific visuals remain presentation-only follow-up work. |
| BUI-009 | LIVE/PARTIAL | Damage/HP feedback and nameplates: PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` projects authoritative target HP/damage into the bound Cobblemon entity; HUD/nameplate polish remains TODO. |
| BUI-010 | LIVE/PARTIAL | Status particle/text registry: PR #222 / `0d261c11027a0f715aee3fac8c6bd5adaa1fd9e4` renders authoritative `status_skip` as a generic particle cue plus nearby action-bar text copied from the semantic event. Broader status lifecycle cues remain upstream-contract work. |
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
| RPG-014 | LIVE | Calendar/world-time hooks for events. PR #224 supplies the durable monotonic RPG day. PR #239 / implementation commit `7b888c957f1e4457dd3c0c5d86286879afde27ad` projects that day into server-authored Ouros year/season/week/day coordinates, publishes stable authored world-event keys, announces forward day transitions/events in Minecraft, and exposes `/autoptu calendar` as a fallback view. Calendar hooks grant no PTU frequency, Feature cost, battle legality, RNG, or outcomes. |
| RPG-015 | BLOCKED/PARTIAL | Trainer PTU world-action usage ledger. PR #224 persists per-Trainer/per-action Daily usage and exposes a server-only atomic reservation boundary. Concrete Standard/Swift/etc action costs and Daily/Scene/Encounter policy remain blocked on authoritative PTU definitions; Minecraft must not synthesize them. |

---

# Facilities and economy

| ID | Status | Facility/service |
|---|---|---|
| FAC-001 | LIVE | Pokémon Center/healer backed by canonical healing service — PR #209 / `81ca566e645f749e7cb6b23cd0714dd91f706094`. |
| FAC-002 | TODO | Shop catalogue/stock/price service. |
| FAC-003 | TODO | Canonical wallet/currency transactions. |
| FAC-004 | LIVE | Crafting recipe registry. PR #226 adds capability-sensitive task recipes, PR #227 exposes them through the physical workstation, and PR #228 completes the initial server-owned recipe contract with workstation IDs, canonical ingredients, and quality-specific canonical outputs. |
| FAC-005 | LIVE | Restart-safe craft transaction — PR #229. A durable attempt freezes canonical ingredient reservations and Trainer-derived Ouros quality odds before the roll, persists one outcome before consumption, retains ingredient locks through partial consumption, creates a deterministic canonical output exactly once, and resumes safely after reconnect/restart/retry. |
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
| SVC-004 | LIVE/PARTIAL | `canCraft(player, recipe, context)` — PR #226 resolves Trainer capability, PR #228 supplies server-owned material/output contracts, PR #229 supplies exactly-once mutation, PR #230 adds side-effect-free canonical material eligibility, PR #231 supplies server-observed authored-ingredient acquisition, PR #233 wires that authority into normal workstation execution, PR #235 exposes explicit player recipe requests, and PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` exposes server-owned readiness and selection through normal workstation interaction while preserving server revalidation. Cross-store acquisition recovery remains future work. |
| SVC-005 | TODO | `canTravel(player, destination, context)` |
| SVC-006 | TODO | `canStartEncounter(player, source)` |
| SVC-007 | TODO | `canStartBattle(player, reservation)` |
| SVC-008 | TODO | `canManageParty(player, mutation)` |
| SVC-009 | TODO | `canManageStorage(player, mutation)` |
| SVC-010 | TODO | `canAcceptQuest(player, quest)` |
| SVC-011 | TODO | `canAdvanceQuest(player, event)` |
| SVC-012 | TODO | `canClaimReward(player, source)` |
| SVC-013 | LIVE/PARTIAL | Item reservation/commit/rollback infrastructure exists. PR #229 adds schema-compatible consumed-but-retained reservations so multi-item craft recovery cannot expose partially consumed stacks before the durable transaction commit; expand to complete RPG inventory use. |
| SVC-014 | LIVE/PARTIAL | Encounter reservation infrastructure exists; wire it to normal world encounters. |
| SVC-015 | LIVE/PARTIAL | Battle outcome commit infrastructure exists; wire it to the normal battle loop. |
| SVC-016 | TODO | Currency transaction commit/idempotency. |
| SVC-017 | TODO | Quest reward commit/idempotency. |
| SVC-018 | TODO | Persistent world-object mutation/idempotency. |
| SVC-019 | TODO | Reconnect/restart active-session recovery. |
| SVC-020 | BLOCKED/PARTIAL | Server-only PTU world-action usage reservation. PR #224 atomically caps canonical Daily usage by Trainer/action/RPG-day and rejects unknown canonical Trainers before consumption; Scene/Encounter/turn/round integration waits on authoritative PTU lifecycle/policy contracts. |
| SVC-021 | NEXT/PARTIAL | `assessWorldTask(player, task, context)` — PR #226 / `2de8d720428b98a6d7375e793a89b5ff20e9c14c` reads canonical Trainer capabilities and applies server-authored Ouros task quality curves; PR #227 adds the normal-world crafting workstation consumer. Reuse this boundary for cooking, technology, medicine, research, occultism, survival, repairs and other non-battle world interactions. |
| SVC-022 | LIVE | `craftWorldTask(attemptId, player, recipe, quantity)` — PR #229 persists the plan and frozen quality odds before rolling, resolves one server outcome, consumes canonical reservations recoverably, emits one deterministic canonical output, and is retry/restart safe. PR #233 invokes this boundary from the normal world workstation; PR #235 invokes it from explicit `/autoptu craft` requests with server-generated attempt IDs; PR #236 exposes that same authority through the workstation selector. |
| SVC-023 | LIVE | `assessCraftMaterials(player, recipe, quantity)` — PR #230 aggregates only owned, unreserved durable canonical item stacks and reports required/available/missing quantities without reserving, rolling, or consuming. |
| SVC-024 | LIVE | `depositCraftIngredient(player, serverObservedStack)` — PR #231 / `1648de45c7cd58ab6a2232d7a0c7e744cf5b986a` accepts only authored recipe ingredient templates, persists them into stable per-player/template canonical stacks through revision CAS, and is invoked from a server-observed held Minecraft stack. PR #238 / implementation commit `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8` adds PREPARED/WITHDRAWN/CANONICAL_APPLIED/COMMITTED journaling, forces server-owned player inventory persistence after withdrawal, reconciles pending transfers on reconnect, and uses an idempotent canonical handoff receipt so retries cannot double-credit. |

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
| NEXT/PARTIAL | Canonical inventory quantities and wallet. PR #231 persists authored crafting ingredient quantities in canonical item stacks, PR #233 consumes those canonical quantities into exactly-once durable craft outputs from the normal workstation, PR #235 exposes the same canonical mutation through explicit recipe fallback requests, and PR #236 exposes recipe selection/readiness through the normal workstation. PR #238 / implementation commit `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8` closes the crash-recoverable Minecraft-to-canonical ingredient handoff. General bag semantics, transfer/use/equipment, and wallet remain TODO/NEXT. |
| LIVE | Durable Minecraft-to-canonical crafting ingredient handoff journal — PR #238 / implementation commit `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8`. Pending withdrawals survive restart/reconnect, reconcile only against server-persisted inventory state, and canonical retries are idempotent. |
| TODO | Quest journal/objectives/reward claims. |
| TODO | NPC relationships/factions/rivals. |
| TODO | Badges/league/tournament records. |
| TODO | Discovery/world-event flags. |
| TODO | Fast-travel unlocks/checkpoints. |
| TODO | Active encounter session journal. |
| TODO | Active battle checkpoint/recovery journal. |
| TODO | Post-battle result commit ledger/idempotency keys. |
| NEXT/PARTIAL | Trainer PTU Daily action usage and monotonic RPG-day state — PR #224; stored under the server world save and durable through reconnect/restart. |
| LIVE | Durable world-task/craft attempt ledger — PR #229. Stable attempt IDs persist frozen quality odds, planned canonical ingredient reservations, one resolved outcome, deterministic output identity, and exactly-once commit/recovery state. PR #233 binds this ledger to normal-world workstation craft requests; PR #235 binds explicit command requests to the same ledger. |

---

# Mechanics Minecraft must not invent

When any of these are incomplete upstream, skip them and continue with another safe item above.

- Complete forced movement, push, pull, knockback, interception and interaction-driven movement.
- Unverified damage modifiers or stateful damage hooks.
- Incomplete status lifecycle rules.
- Ability rules not executed/emitted authoritatively by AutoPTU-Java.
- Held-item or consumable battle rules not executed/emitted authoritatively by AutoPTU-Java.
- Trainer Feature/perk rules, action costs, frequency classification, usage limits or effects not executed/emitted or otherwise supplied as authoritative PTU content.
- Capture legality/RNG/outcome until an authoritative contract exists.
- PTU evolution, level-up and move-learning legality until an authoritative contract exists.
- Tactical AI policy until upstream owns it.
- Any battle hit, crit, damage, target legality, resource consumption or result supplied as trusted client truth.
- Any Cobblemon Pokemon/BattleState/gameplay field used as canonical encounter or battle input.

World-task probability curves for Ouros-authored non-battle activities are Minecraft RPG content, not PTU battle rules. They may consume canonical Trainer capability ranks, but must not claim to be PTU skill-check formulas or synthesize missing Trainer Feature effects.

---

# First playable RPG milestone

This milestone is complete only when a fresh player can do all of the following in one normal Minecraft world without dev-only setup commands:

1. Join.
2. Load/create persistent Trainer state.
3. Choose a starter from a server-owned list.
4. See the starter in a persistent party.
5. Inspect and heal the party through normal Minecraft UI/world interaction.
6. See wild Pokemon physically roaming appropriate world habitats.
7. Approach/interact with a visible wild Pokemon and create a server-owned canonical encounter from that actor's AutoPTU binding.
8. Start an AutoPTU-Java battle from the persistent party and that visible wild actor's canonical blueprint.
9. Choose legal actions through Minecraft UI.
10. See movement, attacks, HP loss, statuses/fainting when authoritative, and winner/loser presentation.
11. Exit to the world.
12. See authoritative post-battle state persisted.
13. Disconnect/reconnect or restart and retain the same canonical RPG state.

Until this loop exists, compatibility-watch work is secondary to a safe item in this file.
