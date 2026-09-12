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
9. Minecraft damage must never decide HP or death for Pokemon presentation actors or canonical NPC actors. Normal Minecraft mining, digging, logging, building, buckets and fire-starting remain available by default. A battle, quest or scripted interaction may temporarily protect only an explicitly registered world footprint whose mutation would invalidate that active interaction.
10. Reuse Minecraft, Cobblemon and compatible mod infrastructure before creating custom equivalents. Prefer native blocks, entities, screens, registries, structures, pathfinding, interaction hooks and algorithms for presentation/platform behavior; replace only the authority or RPG state they cannot safely own.
11. Permanent Ouros-specific facilities and items must have explicit namespaced mod identity and data-driven assets/recipes. Never use an incidental multi-block combination as the authoritative identity of a permanent RPG facility. Native single-block identities such as Cobblemon PC/Healing Machine or Minecraft Lodestone may be reused when their identity is already unambiguous. Authored structures may still use many blocks visually, but server authority must come from a mod registry ID or an explicit persisted authoring binding, not pattern coincidence.

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

## Authored content identity doctrine

- Ouros-specific permanent facilities use `autoptu_cobblemon_rpg_fabric_adapter:*` blocks/items or an explicit persisted authored-object binding.
- Recipes, loot tables, tags and other tunable content belong in datapacks/resources rather than hard-coded block compositions.
- Multi-block arrangements may describe decoration, structures or temporary world context, but they do not become RPG identity merely because those blocks happen to touch.
- Existing composite signatures are migration debt. New gameplay must not add more of them.

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
| CUR-011 | LIVE | Cobblemon Healing Machine interaction | PR #209 established canonical persistent party HP healing. PR #273 / merge `16468f2cd43ee90ad6bc04d18fabc7441ca43190` replaces the custom lodestone/iron/sea-lantern imitation with Cobblemon's real Healing Machine block for model, block state and presentation while AutoPTU remains authoritative for persistent HP. |
| CUR-012 | LIVE | server-owned wild encounter table | PR #211, implementation commit `705d4ab98a2e61b967c59447c957be765e7009e5`. Zone/context selects and freezes an already-authored canonical WILD roster without reading Cobblemon gameplay state. |
| CUR-013 | LIVE | visible wild actor interaction boundary | Registered visible Pokemon presentation actors can submit an encounter request by entity UUID -> AutoPTU-owned world binding. The adapter never reads Cobblemon Pokemon gameplay state. |
| CUR-014 | LIVE | `/autoptu party lead <slot>` | PR #219, commit `e0149f97939aec2926d6b828c00851eb86a6a538`. Promotes the selected durable canonical party member to lead with server-side slot resolution and optimistic concurrency. |
| CUR-015 | LIVE | authoritative battle choice menu/fallback | PR #220, implementation commit `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`. Displays only fresh AutoPTU-Java legal-choice stable keys and submits the exact revalidated choice without client-supplied battle scope or PTU legality. |
| CUR-016 | LIVE/PARTIAL | Fabric semantic battle playback runtime | PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` projects authoritative move animation, HP and relocation. PR #222 / `0d261c11027a0f715aee3fac8c6bd5adaa1fd9e4` adds visible authoritative `status_skip` particles/action-bar cues. PR #248 / `6d589c7c53e768bacd6901b892745bfe99fc24b9` adds target-facing attack presentation, generic move sweep cues and authoritative non-zero damage particles/sound without local combat inference; semantic faint/result remain upstream-contract work. |
| CUR-017 | LIVE/PARTIAL | capability-sensitive world task assessment | Shipped via PR #226 / commit `2de8d720428b98a6d7375e793a89b5ff20e9c14c`. Server-authored graded quality curves and `/autoptu cancraft <recipe>` read only persistent canonical Trainer skill ranks; preview performs no RNG and consumes no materials. |
| CUR-018 | LIVE | crafting workstation interaction | PR #227 adds the physical workstation, PR #228 adds server-owned ingredient/output contracts, PR #229 adds the restart-safe craft transaction, PR #230 adds canonical material readiness, PR #231 / `1648de45c7cd58ab6a2232d7a0c7e744cf5b986a` adds normal-world server-observed ingredient deposits, PR #233 / `f12bca736699739fc9cdd67bedf640cc3b4f6f10` wires durable workstation crafting, PR #235 / `a4f763dade77a5aaa9f315eb7b8f3d369363afd3` adds explicit fallback recipe requests, and PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` adds the normal workstation recipe-selection surface. PR #238 / `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8` adds restart-safe Minecraft-stack handoff. PR #294 / merge `ab4e45e785fc09a5a1ed796b0f23f4761654080e` gives the workstation explicit namespaced block/item identity. |
| CUR-019 | LIVE | Ouros RPG calendar/world-event surface | PR #239 / implementation commit `7b888c957f1e4457dd3c0c5d86286879afde27ad`. Players can inspect the current durable calendar with `/autoptu calendar`; online players receive server-authored day-transition/event announcements. Stable event keys are world hooks only and carry no PTU rules or rewards by themselves. |
| CUR-020 | LIVE | durable physical field camp setup | PR #241 / implementation head `fcc356e4f2260caf4e7d142a23a4c03d5bf02d23` persists one server-owned Ouros camp result keyed by dimension/block position. PR #297 / merge `397760893fabfcc0cf037a83dc1b98c8e07e8b57` replaces campfire-over-barrel authority with `autoptu_cobblemon_rpg_fabric_adapter:field_camp`; durable camp quality/result authority remains in SVC-025. |
| CUR-021 | LIVE | canonical bag read surface | PR #243 / implementation head `a5667c5b6f482f3aae73c77694a47467fb4c7db1`. `/autoptu bag` projects only durable server-owned item stacks, available quantity, active reservation quantity and retained transaction locks for the authenticated Trainer. |
| CUR-022 | LIVE | canonical wallet read surface | PR #245 / implementation head `a1ba9c1d5cddb18f9e05e275536194654f19c03f`. `/autoptu money` resolves the authenticated Trainer to a world-save-scoped canonical wallet, creates a durable zero-balance wallet on first read, and reports server-owned currency, balance and revision without inventing starter funds, shop prices or rewards. |
| CUR-023 | LIVE | server-authored shop catalogue read surface | PR #247 / implementation head `36870beb75976891d0d4ebbe1987acbb435bd1` adds authored offers; PR #250 / implementation head `748a57efc20678962f65efad9d4feb7450bafd54` adds durable current remaining stock and revision-backed persistence. No trusted client price, currency, template or stock truth is accepted. |

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
| P0-007 | LIVE/PARTIAL | Visible roaming wild encounter trigger | PR #457 / merge `0aa144c0415509e3d92b3191379b294539223e2c` adds the first complete server-authored Marea Fletchling WILD blueprint and publishes it before actor reveal. PR #458 / production head `d5ecc49760e954baf1d6da23f92345c379de704f` makes normal Fabric startup idempotently provision/rebind that visible actor without a dev build command and revalidates the exact world-registry blueprint again before a right-click can create the durable encounter request. Broader habitat populations remain follow-up ecology work. |
| P0-008 | LIVE | Party-to-encounter handoff | Core immutable handoff service shipped in PR #216 / implementation commit `167b61471893e9b21d9b2630dd65960117178939`. PR #459 / implementation head `90da2c68083f776a15261b65023d3e44557a8dc0` wires the durable normal-world visible-wild request into that boundary. |
| P0-009 | BLOCKED | Normal player-vs-wild battle start | P0-008 is ready, but the current battle-start/integration-readiness boundary still requires AutoPTU-Java-authoritative movement profile, dynamic accuracy/evasion flags and damage modifiers that are not present in the current reservation contract. Minecraft/Cobblemon must not derive or fabricate those PTU inputs. |
| P0-010 | LIVE | Battle choice UI | Shipped via PR #220 / implementation commit `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`; the server binds player -> reservation/actor, displays only a fresh authoritative legal-choice set, accepts only a stable choice ID, re-fetches the action space, and executes the exact still-legal choice. |
| P0-011 | BLOCKED/PARTIAL | Normal semantic battle playback | PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` adds attack animation, HP and relocation projection. PR #222 / `0d261c11027a0f715aee3fac8c6bd5adaa1fd9e4` makes authoritative `status_skip` visible. PR #248 / `6d589c7c53e768bacd6901b892745bfe99fc24b9` improves visible attack readability. Faint/result presentation remains blocked until AutoPTU-Java emits an explicit authoritative semantic faint/result contract. |
| P0-012 | BLOCKED/PARTIAL | Post-battle commit | PR #305 / merge `c90a703479417fe84f8b0405532281310e5af1a2` persists trusted engine-authored final state. PR #310 / merge `cc7f4409662ca5580c038d3200ff4e398a11dbdc` adds durable transaction recovery. Normal invocation remains blocked until AutoPTU-Java exposes an explicit final-state handoff. |
| P0-013 | BLOCKED | Return-to-world transition | Requires authoritative terminal battle signal/result handoff. |
| P0-014 | BLOCKED/PARTIAL | Reconnect/restart recovery | Active battle recovery remains blocked until durable authoritative battle snapshots/outcomes and terminal semantics exist. |
| P0-015 | LIVE | `/autoptu status` | Verified live on main `843e71a1fb6e9bc6cd1272342432cff7804d8dbe`. |
| P0-016 | LIVE | Minecraft actor/world authority guard | PR #269 / implementation head `4ca41451e32e485d9b493ce674eb2cfb69a03dab`. |

---

# `/autoptu admin` catalogue

These are required for operations, testing and recovery. They must never be normal progression paths.

| ID | Status | Command |
|---|---|---|
| ADM-001 | LIVE/PARTIAL | `/autoptu admin player inspect <player>` — PR #616 / implementation head `9ca83ad66a86218e422d1d526dbffbe56ea5f155`; permission-level-2 operators resolve an online Minecraft player to the canonical UUID-derived Trainer and read Trainer, progression, party and bag/reservation summaries from server-owned repositories without mutating RPG or PTU state. Offline identity resolution remains follow-up work. |
| ADM-002 | LIVE/PARTIAL | `/autoptu admin player validate <player>` — PR #617 / implementation head `50779e71e4f7a78e4394676e02d162d3ff286492`; permission-level-2 operators resolve an online Minecraft player through the server-owned UUID-derived canonical identity and perform read-only structural consistency checks across Trainer, party, bag/reservation and persisted progression projections. Owner mismatches, duplicate structural identities and aggregate bag/lock inconsistencies fail visibly; missing progression is surfaced as a warning rather than becoming invented PTU/progression policy. Offline identity resolution and broader whole-save validation remain follow-up work. |
| ADM-003 | LIVE/PARTIAL | `/autoptu admin pokemon inspect <pokemonId>` — PR #618 / implementation head `530bb13fad084e622709ed7f8b06574553f49768`; permission-level-2 operators resolve the supplied Pokemon ID directly against the server-owned canonical Pokemon repository and inspect owner, species, level, revision, canonical HP/status/injury, available combat inputs, move IDs, capabilities and held-item identity without mutating state or reading Cobblemon Pokemon gameplay data. Whole-save lookup/indexing and broader repair tooling remain follow-up work. |
| ADM-004 | LIVE/PARTIAL | `/autoptu admin party inspect <player>` — PR #619 / implementation head `b1b3675ee9ce7ff0178f86e2e7c8ea9bdb0ad6f1`; permission-level-2 operators resolve an online Minecraft player to the server-owned canonical UUID identity and inspect the persisted party revision plus each slot's canonical Pokemon ID, species, level, HP/status and Pokemon revision without mutating RPG/PTU state or reading Cobblemon Pokemon gameplay data. Offline identity resolution remains follow-up work. |
| ADM-005 | LIVE/PARTIAL | `/autoptu admin inventory inspect <player>` — PR #621 / implementation head `2b3ae282896afb775879a5b67a972968818ac8bc`; permission-level-2 operators resolve an online Minecraft player to the server-owned UUID-derived canonical identity and inspect every canonical bag stack plus aggregate quantity, availability, reservations and transaction locks without mutating RPG/PTU state or trusting Cobblemon inventory/gameplay data. Offline identity resolution remains follow-up work. |
| ADM-006 | LIVE/PARTIAL | `/autoptu admin quest inspect <player> [quest]` — PR #623 / implementation head `11df1e6898ed9b9921643323867acc5474db781c`; permission-level-2 operators resolve an online Minecraft player to the server-owned UUID-derived canonical identity and inspect the durable canonical quest journal, authored quest metadata and persisted objective progress without observing objective events, advancing quests, claiming rewards, mutating progression/PTU state or trusting Cobblemon gameplay data. Offline identity resolution remains follow-up work. |
| ADM-007 | LIVE/PARTIAL | `/autoptu admin encounter inspect <player>` — PR #624 / implementation head `0420f6786b7739f7f0b47d62180d05ddb18f370c`; permission-level-2 operators resolve an online Minecraft player to the UUID-derived canonical Trainer and inspect only the durable active visible-world encounter request: canonical encounter identity, visible actor correlation, authored zone/context and server-observed dimension/position/tick. The command is read-only and does not derive or mutate battle-start legality, combatants, RNG, action economy, damage, statuses or outcomes. Offline identity resolution remains follow-up work. |
| ADM-008 | LIVE/PARTIAL | `/autoptu admin battle inspect <battleId>` — PR #625 / implementation head `734ec7ff92ed507dd653196102a38626d845dfa0`; permission-level-2 operators inspect only the active server-owned opaque battle binding, resolving the authenticated participant/canonical identity, bound actor and authoritative legal-choice count when available. Durable/offline battle inspection remains unavailable until an authoritative persisted battle session/checkpoint exists; Minecraft reconstructs no turn, HP, faint, result, RNG, legality or PTU outcome. |
| ADM-009 | LIVE/PARTIAL | `/autoptu admin battle demo <species> <opponent>` — PR #627 / implementation head `e94816e783f32978bce51a18c8097301d7416bbf`; permission-level-2 operators can launch the existing narrow server-owned AutoPTU-Java-backed 1v1 development battle harness with explicit Cobblemon presentation species. The legacy `/autoptu testbattle ...` alias remains temporarily permission-gated for operator/evidence compatibility. The demo does not read or commit canonical party, inventory, progression, persistent HP or campaign results, and Minecraft does not own battle legality, RNG, damage, action economy or outcomes. |
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
| ADM-022 | LIVE/PARTIAL | `/autoptu admin evidence battle <reservationId>` — PR #378 / merge `e261c7e04b24b9bf61b6b2effb8e897cf9e0870f`; permission-level-2 operators can inspect the last 20 exact semantic events already recorded for an active authoritative reservation. Mapping this surface to the server-generated opaque battle ID and durable/exportable evidence retention remain follow-up work. |
| ADM-023 | TODO | `/autoptu admin reload rpg-config` |

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
- Canonical status, injury, wound or PTU recovery semantics not supplied by AutoPTU-Java. Minecraft facilities may expose the interaction and recover Minecraft-owned player state, but they must not erase or synthesize canonical PTU conditions locally.

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
