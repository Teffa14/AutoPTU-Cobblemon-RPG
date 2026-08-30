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
| CUR-020 | LIVE | durable physical field camp setup | PR #241 / implementation head `fcc356e4f2260caf4e7d142a23a4c03d5bf02d23` persists one server-owned Ouros camp result keyed by dimension/block position. PR #297 / merge `397760893fabfcc0cf037a83dc1b98c8e07e8b57` replaces the campfire-over-barrel authority signature with `autoptu_cobblemon_rpg_fabric_adapter:field_camp`; durable camp quality/result authority remains in SVC-025. |
| CUR-021 | LIVE | canonical bag read surface | PR #243 / implementation head `a5667c5b6f482f3aae73c77694a47467fb4c7db1`. `/autoptu bag` projects only durable server-owned item stacks, available quantity, active reservation quantity and retained transaction locks for the authenticated Trainer. |
| CUR-022 | LIVE | canonical wallet read surface | PR #245 / implementation head `a1ba9c1d5cddb18f9e05e275536194654f19c03f`. `/autoptu money` resolves the authenticated Trainer to a world-save-scoped canonical wallet, creates a durable zero-balance wallet on first read, and reports server-owned currency, balance and revision without inventing starter funds, shop prices or rewards. |
| CUR-023 | LIVE | server-authored shop catalogue read surface | PR #247 / implementation head `36870beb75976891d0d4ebbe2f1edfaf78423e36` adds authored offers. PR #250 / implementation head `748a57efc20678962f65efad9d4feb7450bafd54` adds world-save-scoped remaining stock with revision CAS; `/autoptu shop` now shows persistent current stock against the authored cap. PTU item effects and replenishment policy remain outside this read surface. |
| CUR-024 | LIVE | restart-safe canonical shop purchase fallback | PR #251 / implementation head `05c46f1b309988cec67ab8caf2ddb44a76dfecbe`. `/autoptu shop buy <offer> [qty]` resolves authored price/currency/template plus durable wallet/stock on the server, journals the cross-store operation, grants one deterministic canonical bag item and resumes pending purchases on restart. |
| CUR-025 | LIVE | canonical Pokemon box read surface | PR #252 / implementation head `272ce7c6273cef95f64413432c8aef7163a05bfa`. `/autoptu box` reads a world-save-scoped owner-only boxed Pokemon aggregate, validates each reference against canonical Pokemon ownership, and fails closed on party/storage overlap. |
| CUR-026 | LIVE | crash-recoverable party/box transfer fallback | PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767`. `/autoptu box deposit <partySlot>` and `/autoptu box withdraw <boxSlot>` resolve the source slot and Pokemon ownership on the server, journal source removal/target addition, and resume incomplete transfers on server start without duplicating or losing the Pokemon. |
| CUR-027 | LIVE | restart-safe canonical shop sale fallback | PR #254 / implementation head `07637aa6fdefb14e3a42e36788bab7fc9c35a38e`. `/autoptu shop sell <item> [qty]` accepts only an authored item template and quantity selection, reserves/consumes an owned canonical stack, credits the canonical wallet through an idempotent receipt, journals each stage, and resumes pending sales on server start. Retail stock is not replenished implicitly. |
| CUR-028 | LIVE | physical canonical Cobblemon PC terminal | PR #255 / implementation head `1dd990700e40205cf495779585cec87507c9d2c4`. Right-clicking the real `cobblemon:pc` opens a server-authored party/box menu; every deposit/withdraw re-resolves canonical slot/ownership and delegates to the existing restart-safe transfer service without reading Cobblemon PC or Pokemon gameplay state. |
| CUR-029 | LIVE | physical canonical Cedar Mart counter | PR #256 established the server-authored buy/sell menu. PR #280 replaces the incidental emerald-over-barrel identity with the explicit namespaced `autoptu_cobblemon_rpg_fabric_adapter:cedar_mart_counter`; durable wallet, stock, bag and transaction services remain server-authoritative. |
| CUR-030 | LIVE/PARTIAL | physical canonical NPC dialogue | PR #257 / implementation head `0d62ec169c122749b5360e4c7fd6a1435c16a725`. Cedar Meadow provisions a persistent Cedar Ranger villager presentation actor bound one-way to server-authored dialogue; right-click opens a server-side option menu and every selection revalidates entity UUID, canonical NPC tag, range, Trainer state and authored option before showing the response. Normal world provisioning beyond the dev Cedar Meadow placement remains follow-up work. |
| CUR-031 | LIVE | physical canonical quest-giver acceptance | PR #258 / implementation head `a60a4084660a87e5315e754d08684c834b7921a8`. The Cedar Ranger exposes the server-authored `cedar-field-notes` offer; selecting it accepts that exact quest once into the authenticated Trainer's durable world-save journal. Repeat selection is idempotent and objective completion/rewards remain separate systems. |
| CUR-032 | LIVE | canonical quest journal read surface | PR #259 / implementation head `40b4a2de256b5822d044a91f4c44cb8a92924bd8`. `/autoptu journal`, `/autoptu quests` and `/autoptu quest <id>` project only the authenticated Trainer's durable accepted entries plus server-authored quest metadata; unknown or unaccepted detail requests fail closed and no objective/reward state is inferred. |
| CUR-033 | LIVE | persistent tracked quest selection | PR #260 / implementation head `d2d554d67c418881280df02f7ed848bc53b23220`. `/autoptu quest track <id>` selects exactly one already-accepted server-authored quest for the authenticated Trainer, persists the pointer through journal revision CAS, and exposes tracked state through the existing journal projection without advancing objectives or granting rewards. |
| CUR-034 | LIVE | restart-safe canonical item storage fallback | PR #261 / implementation head `a9f7882d3da7cf8b7767903d03be35ee5cd582f2`. `/autoptu storage` projects owner-scoped stored quantities; deposit/withdraw move opaque canonical item quantities between the active bag and storage through a durable staged journal, keeping stored items outside bag sale/crafting/reservation reads and recovering pending transfers on server start. |
| CUR-035 | LIVE | physical canonical item storage terminal | PR #262 established the server-authored bag/storage menu. PR #290 / merge `48a9ddf17abbf96804e469d725e0c11e8f835ddb` replaces the iron-block-over-barrel signature with `autoptu_cobblemon_rpg_fabric_adapter:item_storage_terminal`; every transfer still revalidates canonical state and delegates to SVC-037. |
| CUR-036 | LIVE | server-authored first-join Trainer onboarding | PR #263 / implementation head `c9b54bcd4db742c5d1f3f3e049ca43271ee5293a`. After authenticated provisioning, a player with a canonical Trainer but no persistent encounter profile gets the server-side Ouros onboarding screen; existing party-bearing players are not interrupted, and no Trainer/PTU/starter truth is accepted from the client. |
| CUR-037 | LIVE | canonical party management screen | PR #265 / implementation head `a9f1926bc8d92cadda43155d3a713a3fae04d82b`. `/autoptu party manage` opens a server-authored 9x3 party view from durable canonical state; every member click revalidates party revision and Pokemon identity before delegating lead selection to the existing canonical lead service. Stale screens refresh and no Cobblemon gameplay state is trusted. |
| CUR-038 | LIVE | canonical Pokémon summary screen | PR #267 / implementation head `2270c21384d01b61fb3fc6f4b76d7f405ec38a40`. Right-clicking a member in the normal Ouros Party screen opens the same server-authored 9x6 read-only summary used by `/autoptu pokemon <slot>`, after party revision/Pokémon identity revalidation. The view consumes only `CanonicalPokemonDetail`; canonical level, HP, statuses, injuries, types, abilities, moves, combat stats, movement, accuracy/evasion, capabilities, held-item presence and revision are projected, while XP remains explicitly unavailable until durable server-owned XP exists. |
| CUR-039 | LIVE | physical canonical Trainer challenge request | PR #268 / implementation head `2a48165f5011f41dcdffb1766add7ce2a4a39df8`. The Cedar Ranger dialogue exposes an authored field-spar challenge; the server revalidates physical NPC identity/range, authenticated Trainer state, challenge-to-NPC binding and a non-empty canonical party before producing an immutable request snapshot with party identities and revision. Minecraft does not start or resolve the PTU battle. |
| CUR-040 | LIVE | RPG actor/world authority protection | PR #269 protects Pokemon/canonical-NPC presentation actors from Minecraft damage authority. PR #270 / merge `2437d32f02c727dc30c0c61ae3157125983424a1` restores ordinary Minecraft mining/building/logging/digging and world mutation outside explicit temporary battle/quest/script protection scopes. |
| CUR-041 | LIVE | Authored world-object interaction gate | PR #271 / merge `01886d9f6b427284870bcf22016b968858b1559a` adds server-authoritative `canInteract` gating for explicitly authored chests, switches, doors, terminals and shrines while leaving ordinary vanilla objects untouched. |
| CUR-042 | LIVE | namespaced PTU recovery bed | PR #279 replaces the vanilla-bed + gold-marker signature with `autoptu_cobblemon_rpg_fabric_adapter:ptu_recovery_bed`. Its datapack recipe consumes any vanilla bed plus Cobblemon Healing Machine; the block restores Minecraft player health and delegates persistent party HP to the canonical healing service. Canonical status/injury recovery remains upstream-blocked. |

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
| P0-011 | BLOCKED/PARTIAL | Normal semantic battle playback | PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` adds attack animation, HP and relocation projection. PR #222 / `0d261c11027a0f715aee3fac8c6bd5adaa1fd9e4` makes authoritative `status_skip` visible. PR #248 / `6d589c7c53e768bacd6901b892745bfe99fc24b9` improves visible attack readability with target-facing lunges, generic target sweep cues and authoritative non-zero damage impact feedback. Faint/result presentation remains blocked until AutoPTU-Java emits an explicit authoritative semantic faint/result contract; Minecraft must not infer either from HP or local state. |
| P0-012 | BLOCKED/PARTIAL | Post-battle commit | PR #305 / merge `c90a703479417fe84f8b0405532281310e5af1a2` persists one trusted engine-authored canonical Pokémon HP/status/injury final state against owner + frozen revision. PR #310 / merge `cc7f4409662ca5580c038d3200ff4e398a11dbdc` adds a durable PREPARED/COMMITTED multi-Pokémon/item transaction journal, reuses the canonical item outcome receipt plus Pokémon revision-CAS services, survives retry/restart without double application, validates the complete frozen roster/transcript before mutation, and fails closed on conflicting payloads. Normal battle-loop invocation remains blocked until AutoPTU-Java exposes an explicit authoritative battle-finished/final-state handoff; Minecraft must not infer terminal outcome from HP or local state. |
| P0-013 | BLOCKED | Return-to-world transition | Session/reservation cleanup and player-control release require an authoritative terminal battle signal/result handoff. Minecraft must not treat zero choices, HP, entity disappearance or local playback state as battle completion. |
| P0-014 | NEXT/PARTIAL | Reconnect/restart recovery | PR #310 / merge `cc7f4409662ca5580c038d3200ff4e398a11dbdc` adds durable PREPARED post-battle transaction discovery and idempotent replay after repository recreation. Continue by wiring pending authoritative transaction recovery into the real Fabric server startup/reconnect lifecycle; active stranded battle-session resolution remains bounded by authoritative battle-finished/session semantics. |
| P0-015 | LIVE | `/autoptu status` | Verified live on main `843e71a1fb6e9bc6cd1272342432cff7804d8dbe`; shows Trainer loaded, party count, save revision, current encounter/battle summary, and actionable blockers from server-owned state. |
| P0-016 | LIVE | Minecraft actor/world authority guard | PR #269 / implementation head `4ca41451e32e485d9b493ce674eb2cfb69a03dab`; Minecraft damage cannot decide Pokemon/canonical-NPC HP or death, and a loaded canonical Trainer cannot directly mine/build/use buckets/start fire as a bypass around authored RPG interactions. |

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
| CMD-028 | LIVE | `/autoptu party manage` — PR #265 / implementation head `a9f1926bc8d92cadda43155d3a713a3fae04d82b`; opens the server-authored canonical party screen and accepts only a revalidated party-slot lead selection. |
| CMD-022 | TODO | `/autoptu party move <from> <to>` |
| CMD-023 | LIVE | `/autoptu pokemon <slot>` — PR #207 / `b3fed8380f801222d6c549f1695b8bb98789a135` |
| CMD-024 | LIVE | `/autoptu box` — PR #252 / implementation head `272ce7c6273cef95f64413432c8aef7163a05bfa`; shows only the authenticated Trainer's durable canonical boxed Pokemon and revision, with ownership and party-overlap validation. |
| CMD-025 | LIVE | `/autoptu box deposit <partySlot>` — PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767`; the server resolves the current party slot, canonical ownership and box membership, refuses to remove the last active party Pokemon, and commits through a restart-recoverable transfer journal. |
| CMD-026 | LIVE | `/autoptu box withdraw <boxSlot>` — PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767`; the server resolves the current box slot and canonical ownership, then moves that Pokemon into the active party through the same recoverable authority boundary. |
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
| CMD-060 | LIVE | `/autoptu bag` — PR #243 / implementation head `a5667c5b6f482f3aae73c77694a47467fb4c7db1`; reads only the authenticated player's durable canonical item stacks and reports quantity, available quantity, active reservation quantity and retained transaction locks. |
| CMD-061 | LIVE | `/autoptu bag inspect <item>` — PR #244 / implementation head `74c9341ab8cc13e3445f133220e8fd0763dbbfdc`; resolves only owner-scoped canonical state, preferring an exact item-instance ID and otherwise aggregating the authored template ID, with quantity, availability, reservation/retained-lock detail and revision. |
| CMD-062 | BLOCKED | `/autoptu use <item> [target]` until AutoPTU-Java supplies authoritative item-use legality/effects. |
| CMD-063 | BLOCKED | `/autoptu held equip <partySlot> <item>` until AutoPTU-Java supplies authoritative held-item legality/effects. |
| CMD-064 | BLOCKED | `/autoptu held remove <partySlot>` until AutoPTU-Java supplies authoritative held-item legality/effects. |
| CMD-065 | LIVE | `/autoptu storage` — PR #261 / implementation head `a9f7882d3da7cf8b7767903d03be35ee5cd582f2`; reads only the authenticated Trainer's durable canonical stored item quantities and revision. |
| CMD-066 | LIVE | `/autoptu storage deposit <item> [qty]` — PR #261 / implementation head `a9f7882d3da7cf8b7767903d03be35ee5cd582f2`; the server resolves an owned unreserved bag stack, retains a transaction lock while moving quantity out of the bag, journals the transfer and recovers it on restart. |
| CMD-067 | LIVE | `/autoptu storage withdraw <item> [qty]` — PR #261 / implementation head `a9f7882d3da7cf8b7767903d03be35ee5cd582f2`; the server validates stored quantity, journals source removal and creates one deterministic canonical bag stack exactly once. |

## Economy and crafting

| ID | Status | Command/service |
|---|---|---|
| CMD-080 | LIVE | `/autoptu money` — PR #245 / implementation head `a1ba9c1d5cddb18f9e05e275536194654f19c03f`; reads only the authenticated Trainer's durable world-save-scoped wallet and reports server-owned currency, balance and revision. |
| CMD-081 | LIVE | `/autoptu shop list [shop]` — PR #247 / implementation head `36870beb75976891d0d4ebbe2f1edfaf78423e36` exposes authored offers; PR #250 / `748a57efc20678962f65efad9d4feb7450bafd54` adds durable current remaining stock and revision-backed persistence. No trusted client price, currency, template or stock truth is accepted. |
| CMD-082 | LIVE | `/autoptu shop buy <offer> [qty]` — PR #251 / implementation head `05c46f1b309988cec67ab8caf2ddb44a76dfecbe`; accepts only offer/quantity selection, then re-resolves the authenticated Trainer, authored price/currency/template, durable wallet and current stock. Wallet debit/refund, stock depletion and deterministic canonical item grant are idempotent and restart-recoverable through a durable purchase journal. |
| CMD-083 | LIVE | `/autoptu shop sell <item> [qty]` — PR #254 / implementation head `07637aa6fdefb14e3a42e36788bab7fc9c35a38e`; accepts only template/quantity selection. The server selects an owned unreserved canonical stack, freezes an explicit authored sell price/currency, journals reservation/consumption/wallet credit, and recovers pending sales after restart without implicit stock replenishment. |
| CMD-084 | LIVE | `/autoptu cancraft <recipe>` — PR #226 adds Trainer capability assessment, PR #228 adds server-owned workstation/ingredient/output contracts, and PR #230 reads only the authenticated player's unreserved durable canonical item stacks to report exact required/available/missing materials. It performs no reservation, RNG, or consumption. |
| CMD-085 | LIVE | `/autoptu craft <recipe> [qty]` — PR #229 supplies the durable server transaction, PR #231 supplies normal server-authoritative ingredient acquisition, PR #233 / `f12bca736699739fc9cdd67bedf640cc3b4f6f10` wires durable workstation crafting, PR #235 / `a4f763dade77a5aaa9f315eb7b8f3d369363afd3` adds the explicit fallback command. PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` exposes that same request path from the workstation selector. The client supplies only a recipe key and bounded quantity; the server re-resolves Trainer identity, authored recipe, knowledge, canonical materials, quality roll, reservations and output. |

## Quests, journal and travel

| ID | Status | Command/service |
|---|---|---|
| CMD-100 | LIVE | `/autoptu journal` — PR #259 / implementation head `40b4a2de256b5822d044a91f4c44cb8a92924bd8`; owner-scoped read-only durable journal projection. |
| CMD-101 | LIVE | `/autoptu quests` — PR #259 / implementation head `40b4a2de256b5822d044a91f4c44cb8a92924bd8`; lists only accepted canonical quest entries enriched from the server-authored catalogue. |
| CMD-102 | LIVE | `/autoptu quest <id>` — PR #259 / implementation head `40b4a2de256b5822d044a91f4c44cb8a92924bd8`; resolves authored metadata only for a quest already present in the authenticated Trainer's journal and fails closed otherwise. |
| CMD-103 | LIVE | `/autoptu quest track <id>` — PR #260 / implementation head `d2d554d67c418881280df02f7ed848bc53b23220`; persists one already-accepted server-authored quest as the authenticated Trainer's tracked quest through revision-CAS journal state. Unknown/unaccepted IDs fail closed and repeat tracking is idempotent. |
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
| PARTY-001 | LIVE | Party screen/menu — PR #265 / implementation head `a9f1926bc8d92cadda43155d3a713a3fae04d82b`. The server projects only durable canonical party members and revalidates party revision plus Pokemon identity before lead mutation; stale GUI state refreshes instead of applying to a different Pokemon. |
| PARTY-002 | LIVE | Persistent lead-slot mutation — PR #219 / `e0149f97939aec2926d6b828c00851eb86a6a538`. |
| PARTY-003 | TODO | Persistent party reorder. |
| PARTY-004 | LIVE | Persistent box/storage aggregate. PR #252 / implementation head `272ce7c6273cef95f64413432c8aef7163a05bfa` adds the owner-scoped durable box; PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767` closes durable movement between the active party and box with ownership checks, revisioned source/target mutations and restart recovery. |
| PARTY-005 | LIVE | PC/storage terminal world interaction — PR #255 / implementation head `1dd990700e40205cf495779585cec87507c9d2c4`. The real Cobblemon PC is presentation-only; interaction opens an AutoPTU server-authored 9x6 party/box menu with paging, range/block revalidation and stale-slot checks before invoking SVC-031. |
| PARTY-006 | LIVE | Deposit/withdraw atomic ownership-safe mutations — PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767`. Source removal and destination addition are journaled as separate idempotent stages; server restart resumes any incomplete transfer and terminal validation requires the Pokemon to exist in exactly the intended aggregate. It applies no PTU battle or capture rule. |
| PARTY-007 | BLOCKED | Capture request legality/RNG/result until upstream authority exists. |
| PARTY-008 | BLOCKED | Successful capture ownership commit until authoritative capture result exists. |

---

# World interaction catalogue

These should become the normal gameplay path.

| ID | Status | Interaction |
|---|---|---|
| WORLD-001 | LIVE | First-join Trainer/onboarding screen — PR #263 / implementation head `c9b54bcd4db742c5d1f3f3e049ca43271ee5293a`. After Minecraft authentication and canonical provisioning, the server opens onboarding only when that UUID-derived Trainer exists and has no persistent encounter profile/starter party; every interaction revalidates the same server-owned state and no PTU or starter truth comes from the client. |
| WORLD-002 | LIVE | Starter-selection screen with Pokémon preview — PR #264 / implementation head `3a233a43dc8223ee22b25cb2336a5b49233ed8a2`. First-join onboarding opens a server-authored 9x3 selector backed only by the canonical starter catalogue. A starter click creates a temporary one-way Cobblemon model preview from that server-authored species, and confirmation revalidates the same choice through `CanonicalStarterSelectionService` before creating the persistent canonical starter/party. No Cobblemon gameplay field is read back as RPG truth. |
| WORLD-003 | LIVE | Party HUD and party management screen. PR #265 / implementation head `a9f1926bc8d92cadda43155d3a713a3fae04d82b` adds the server-authored 9x3 management screen with stale-state revalidation and canonical lead selection. PR #266 / implementation head `9b176c875b1c784cf35399fe8ced2ed684f7c292` adds a recurring server-authored party boss-bar HUD from `CanonicalPartySummary` and normal sneak-right-click access from the physical Cobblemon PC, with block/range revalidation. The dedicated boss-bar surface does not replace transient action-bar gameplay cues, and Cobblemon remains presentation-only. |
| WORLD-004 | LIVE | Pokémon summary screen — PR #267 / implementation head `2270c21384d01b61fb3fc6f4b76d7f405ec38a40`. Right-clicking a member in the normal Ouros Party screen opens a server-authored 9x6 read-only detail view after stale party identity/revision revalidation; `/autoptu pokemon <slot>` opens the same fallback surface. The view consumes only `CanonicalPokemonDetail`, leaves XP unavailable because no durable server-owned XP field exists, and never reads Cobblemon gameplay state. |
| WORLD-005 | LIVE | Healing machine/nurse/healer interaction — PR #209 / `81ca566e645f749e7cb6b23cd0714dd91f706094` established canonical healing. PR #273 / merge `16468f2cd43ee90ad6bc04d18fabc7441ca43190` now uses Cobblemon's real Healing Machine as the physical world surface and consumes the interaction before Cobblemon party/HP state can become RPG authority. |
| WORLD-006 | LIVE | Physical PC/storage terminal — PR #255 / implementation head `1dd990700e40205cf495779585cec87507c9d2c4`. Right-clicking `cobblemon:pc` opens the canonical party/box selector; the native Cobblemon PC store is never read or written as RPG truth. |
| WORLD-007 | LIVE | Shop counter and buy/sell menu — PR #256 / implementation head `b483936f4bae43dd8c1eca77a1ecbd2d8ee8a6f6` established the canonical UI and transactions. PR #280 migrates the physical surface to `autoptu_cobblemon_rpg_fabric_adapter:cedar_mart_counter`, so arbitrary emerald-over-barrel arrangements no longer become canonical shops. |
| WORLD-008 | LIVE | Crafting workstation/menu. PR #227 adds the physical workstation, PR #228 shows server-owned ingredient/output contracts, PR #229 supplies the restart-safe transaction, PR #230 shows canonical material readiness, PR #231 adds server-observed ingredient deposit, PR #233 / `f12bca736699739fc9cdd67bedf640cc3b4f6f10` wires durable normal-use crafting, PR #235 / `a4f763dade77a5aaa9f315eb7b8f3d369363afd3` adds explicit server-authoritative recipe selection through `/autoptu craft`, PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` makes normal workstation use show an explicit selector, PR #238 / `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8` makes ingredient transfer crash-recoverable, and PR #294 / merge `ab4e45e785fc09a5a1ed796b0f23f4761654080e` migrates the physical surface to `autoptu_cobblemon_rpg_fabric_adapter:crafting_workstation`. |
| WORLD-009 | LIVE/PARTIAL | NPC dialogue interaction and dialogue screen — PR #257 / implementation head `0d62ec169c122749b5360e4c7fd6a1435c16a725`. Cedar Meadow provisions the authored Cedar Ranger as a persistent villager presentation actor; right-click opens a 9x3 server-side menu backed by the canonical dialogue catalogue, and option selection revalidates entity identity, range, Trainer state and authored option. Quest/progression actions remain WORLD-010 and the Cedar Meadow world itself is still dev-placed. |
| WORLD-010 | LIVE/PARTIAL | Quest-giver/quest-object interaction. PR #258 / implementation head `a60a4084660a87e5315e754d08684c834b7921a8` makes the physical Cedar Ranger expose one authored quest offer through the existing dialogue UI; the server revalidates NPC identity, authenticated Trainer and quest/giver binding before an idempotent durable acceptance commit. Generic quest objects and objective-event processing remain follow-up work. |
| WORLD-011 | BLOCKED/PARTIAL | Trainer challenge interaction — PR #268 / implementation head `2a48165f5011f41dcdffb1766add7ce2a4a39df8`. The physical Cedar Ranger now offers a server-authored field-spar option and validates Trainer/NPC/challenge/party state into an immutable challenge request. The remaining Trainer-vs-Trainer battle-start handoff is blocked until the authoritative AutoPTU battle contract can accept the opponent side without Minecraft inventing combatants, legality or outcomes. |
| WORLD-012 | LIVE | Visible wild Pokémon contextual encounter interaction boundary — PR #214. A registered roaming actor is the encounter surface and carries an AutoPTU-owned encounter identity without reading Cobblemon gameplay state. |
| WORLD-013 | BLOCKED | Region/grass ecology provisioning for visible roaming wild Pokémon. Population/presence must be backed by a trusted server-authored complete canonical WILD blueprint source before reveal; it never creates invisible movement encounters. |
| WORLD-014 | TODO | Cave ecology provisioning for visible roaming wild Pokémon; no movement-only encounter roll. |
| WORLD-015 | TODO | Water/fishing ecology provisioning for visible/swimming/fishable wild actors; no context-only battle trigger. |
| WORLD-016 | LIVE | Interactive chests, switches, doors, terminals and shrines through `canInteract` — PR #271 / merge `01886d9f6b427284870bcf22016b968858b1559a`. The current gold-footprint authoring shortcut remains live for previously authored objects, but permanent facility identity must migrate to explicit mod IDs or persisted authored-object bindings under WORLD-028. Ordinary Minecraft objects remain vanilla. |
| WORLD-017 | LIVE | Minecraft-native fast-travel point — PR #273 / merge `16468f2cd43ee90ad6bc04d18fabc7441ca43190`. Sneak-use a vanilla Lodestone with an empty main hand to submit a server-authoritative travel request; the current server-authored destination is the Overworld spawn and Minecraft's own player teleport primitive performs the move. Normal lodestone/compass behavior remains vanilla. |
| WORLD-018 | LIVE/PARTIAL | PTU recovery bed — PR #275/#278 supplied the first inn/rest surface; PR #279 replaces the accidental vanilla-bed + marker signature with the explicit namespaced `autoptu_cobblemon_rpg_fabric_adapter:ptu_recovery_bed` block/item. Its datapack recipe combines any vanilla bed with Cobblemon Healing Machine. Using it restores Minecraft player health and delegates persistent party HP restoration to the canonical healing service. Canonical statuses/injuries remain unchanged until AutoPTU-Java exposes authoritative recovery semantics. |
| WORLD-019 | TODO | Move tutor/relearner NPC shell. |
| WORLD-020 | TODO | Gym/league registration desk. |
| WORLD-021 | TODO | Gates/doors controlled by canonical badge/key/progression state. |
| WORLD-022 | LIVE | Camp interaction. PR #240 adds the original assessment surface, PR #241 / `fcc356e4f2260caf4e7d142a23a4c03d5bf02d23` adds the durable server-owned Ouros camp result, and PR #297 / merge `397760893fabfcc0cf037a83dc1b98c8e07e8b57` replaces campfire-over-barrel authority with `autoptu_cobblemon_rpg_fabric_adapter:field_camp`. |
| WORLD-023 | TODO | Discovery/location trigger. |
| WORLD-024 | TODO | Persistent world-event objects. |
| WORLD-025 | TODO | Ambient Pokémon behavior framework that never becomes PTU stat truth. |
| WORLD-026 | LIVE | Physical item storage terminal/menu — PR #262 established the server-authored bag/storage menu. PR #290 / merge `48a9ddf17abbf96804e469d725e0c11e8f835ddb` migrates the physical surface to `autoptu_cobblemon_rpg_fabric_adapter:item_storage_terminal`; stored items remain excluded from active bag sale/crafting/reservation reads and all movement continues through SVC-037. |
| WORLD-027 | LIVE | RPG actor/world authority protection — PR #269 protects Pokemon and canonical NPC presentation actors from Minecraft damage authority. PR #270 / merge `2437d32f02c727dc30c0c61ae3157125983424a1` narrows terrain protection to explicit temporary interaction footprints, so normal Minecraft mining, digging, logging, building, buckets and fire remain available elsewhere. |
| WORLD-028 | LIVE | Permanent composite facility identity migration complete. Cedar Mart migrated in PR #280; item storage in PR #290 / merge `48a9ddf17abbf96804e469d725e0c11e8f835ddb`; crafting workstation in PR #294 / merge `ab4e45e785fc09a5a1ed796b0f23f4761654080e`; field camp in PR #297 / merge `397760893fabfcc0cf037a83dc1b98c8e07e8b57`. Native unique primitives such as Cobblemon PC/Healing Machine and Minecraft Lodestone remain reusable where identity is already unambiguous. |

---

# Battle UX and presentation catalogue

| ID | Status | Tool |
|---|---|---|
| BUI-001 | LIVE/PARTIAL | Battle HUD: PR #249 / implementation head `9636cffa27abff4e22660db67b6af5da8d3b94f3` adds a normal in-world boss-bar HUD for an active server-bound battle actor. It refreshes from the same fresh AutoPTU-Java legal-choice source as BUI-002 and shows only canonical actor identity plus current legal-choice count. Turn/action-budget, HP/status and event-log fields remain follow-up work when their authoritative projections are available; zero choices are not reinterpreted locally as turn state. |
| BUI-002 | LIVE | Legal move/action menu from authoritative choice set — PR #220 / `3c71ccc4355d3b5c7cd0e9dfbd2340f2ab136b89`; server-bound scope plus fresh stable-key revalidation, with no Minecraft legality calculation. |
| BUI-003 | TODO | Grid targeting overlay from authoritative legal tiles/targets. |
| BUI-004 | TODO | Party switch menu from authoritative switch choices. |
| BUI-005 | TODO | Battle item menu from authoritative item choices. |
| BUI-006 | TODO | Trainer Feature menu from authoritative usable Features. |
| BUI-007 | TODO | Battle camera framing. |
| BUI-008 | LIVE/PARTIAL | Semantic move animation registry: PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` routes authoritative move IDs through the generic Fabric presentation gateway. PR #248 / `6d589c7c53e768bacd6901b892745bfe99fc24b9` adds target-facing attack presentation and a generic sweep cue at the already-resolved target; richer move-specific visuals remain presentation-only follow-up work. |
| BUI-009 | LIVE/PARTIAL | Damage/HP feedback and nameplates: PR #221 / `22871146a187d9dc54f687112ff8483ac1f39067` projects authoritative target HP/damage into the bound Cobblemon entity. PR #248 / `6d589c7c53e768bacd6901b892745bfe99fc24b9` adds generic particles and impact sound only when the authoritative projection reports non-zero damage; HUD/nameplate polish remains TODO. |
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
| RPG-001 | LIVE/PARTIAL | Server-owned NPC identity and dialogue-state framework. PR #257 / implementation head `0d62ec169c122749b5360e4c7fd6a1435c16a725` adds a validated canonical NPC dialogue catalogue, one-way physical entity binding and server-authored option resolution. Conditional/persistent dialogue state remains follow-up work. |
| RPG-002 | LIVE | Quest journal persistence. PR #258 / implementation head `a60a4084660a87e5315e754d08684c834b7921a8` adds owner-scoped world-save persistence with revision-CAS acceptance, atomic replacement, idempotent repeats and dedicated-server restart verification. PR #259 / implementation head `40b4a2de256b5822d044a91f4c44cb8a92924bd8` adds an owner-scoped read-only projection that resolves persisted IDs against server-authored quest metadata. PR #260 / implementation head `d2d554d67c418881280df02f7ed848bc53b23220` adds one durable tracked-quest pointer with schema-v1 journal compatibility, revision-CAS switching and idempotent repeat selection. Objective processing and rewards remain RPG-003/RPG-004. |
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
| FAC-002 | LIVE/PARTIAL | Shop catalogue/stock/price service. PR #247 / implementation head `36870beb75976891d0d4ebbe2f1edfaf78423e36` adds the reusable authored catalogue/query boundary. PR #250 / implementation head `748a57efc20678962f65efad9d4feb7450bafd54` adds world-save-scoped remaining stock and CAS depletion primitives. PR #251 / implementation head `05c46f1b309988cec67ab8caf2ddb44a76dfecbe` adds restart-safe exactly-once buy orchestration. PR #254 / implementation head `07637aa6fdefb14e3a42e36788bab7fc9c35a38e` adds explicit authored sell prices plus restart-safe item consumption/wallet credit. PR #256 / implementation head `b483936f4bae43dd8c1eca77a1ecbd2d8ee8a6f6` adds the physical server-authored buy/sell counter UI. Replenishment policy remains TODO. |
| FAC-003 | LIVE | Canonical wallet/currency transactions. PR #245 / implementation head `a1ba9c1d5cddb18f9e05e275536194654f19c03f` establishes durable owner-scoped wallet state, first-read provisioning, revision-CAS persistence and a read-only player projection. PR #246 / implementation head `cf4f1c389bdb814c3067d5266dfda63337aa9bbd` adds server-owned credit/debit commits with balance and idempotency receipt written in one atomic replacement, retry/restart safety, insufficient-funds rejection, immutable transaction identity and schema-v1 wallet migration. Shop offers and rewards remain separate authored services. |
| FAC-004 | LIVE | Crafting recipe registry. PR #226 adds capability-sensitive task recipes, PR #227 exposes them through the physical workstation, and PR #228 completes the initial server-owned recipe contract with workstation IDs, canonical ingredients, and quality-specific canonical outputs. |
| FAC-005 | LIVE | Restart-safe craft transaction — PR #229. A durable attempt freezes canonical ingredient reservations and Trainer-derived Ouros quality odds before the roll, persists one outcome before consumption, retains ingredient locks through partial consumption, creates a deterministic canonical output exactly once, and resumes safely after reconnect/restart/retry. |
| FAC-006 | TODO | Move tutor/relearner service shell. |
| FAC-007 | LIVE/PARTIAL | PTU recovery bed service — PR #279. Explicit namespaced block/item plus datapack recipe are live; Minecraft player health and canonical party HP can be restored. Canonical status/injury/wound recovery remains blocked until AutoPTU-Java provides authoritative semantics and mutation contracts. |
| FAC-008 | TODO | Fast-travel unlock and destination service. |
| FAC-009 | TODO | Daycare/nursery persistence shell. |
| FAC-010 | TODO | Gym/league challenge registration and records. |

---

# Server-authoritative service boundaries required under UI/commands

| ID | Status | Boundary |
|---|---|---|
| SVC-001 | TODO | `canPerform(player, action, context)` |
| SVC-002 | LIVE | `canInteract(player, object, context)` — PR #271 / merge `01886d9f6b427284870bcf22016b968858b1559a`. The server validates canonical Trainer existence, authored object identity/type and interaction range before allowing the Minecraft-facing interaction; it supplies no PTU legality, RNG, rewards, progression or battle outcome. |
| SVC-003 | TODO | `canUse(player, item, target, context)` |
| SVC-004 | LIVE/PARTIAL | `canCraft(player, recipe, context)` — PR #226 resolves Trainer capability, PR #228 supplies server-owned material/output contracts, PR #229 supplies exactly-once mutation, PR #230 adds side-effect-free canonical material eligibility, PR #231 supplies server-observed authored-ingredient acquisition, PR #233 wires that authority into normal workstation execution, PR #235 exposes explicit player recipe requests, and PR #236 / `38a3b230a5bb9ca4a3d9313065678b3a617afaa1` exposes server-owned readiness and selection through normal workstation interaction while preserving server revalidation. Cross-store acquisition recovery remains future work. |
| SVC-005 | LIVE/PARTIAL | `canTravel(player, destination, context)` — PR #273 / merge `16468f2cd43ee90ad6bc04d18fabc7441ca43190`. Validates canonical Trainer, observed source point, range, server-authored destination and availability before Minecraft performs the teleport. Current destination catalogue is the Overworld spawn only; discovery/unlock persistence remains follow-up. |
| SVC-006 | TODO | `canStartEncounter(player, source)` |
| SVC-007 | TODO | `canStartBattle(player, reservation)` |
| SVC-008 | TODO | `canManageParty(player, mutation)` |
| SVC-009 | TODO | `canManageStorage(player, mutation)` |
| SVC-010 | LIVE/PARTIAL | `canAcceptQuest(player, quest)` — PR #258 validates that the quest is server-authored, is offered by the revalidated canonical NPC and is not trusted from client metadata. Conditional story/prerequisite eligibility remains future quest-state work. |
| SVC-011 | TODO | `canAdvanceQuest(player, event)` |
| SVC-012 | TODO | `canClaimReward(player, source)` |
| SVC-013 | LIVE/PARTIAL | Item reservation/commit/rollback infrastructure exists. PR #229 adds schema-compatible consumed-but-retained reservations so multi-item craft recovery cannot expose partially consumed stacks before the durable transaction commit; expand to complete RPG inventory use. |
| SVC-014 | LIVE/PARTIAL | Encounter reservation infrastructure exists; wire it to normal world encounters. |
| SVC-015 | LIVE/PARTIAL | Battle outcome commit infrastructure exists. PR #310 / merge `cc7f4409662ca5580c038d3200ff4e398a11dbdc` adds a crash-recoverable group commit over the complete frozen Pokémon roster plus trusted item consumptions, with durable PREPARED/COMMITTED state and idempotent replay. Normal battle-loop invocation remains blocked on the authoritative final-state handoff. |
| SVC-016 | LIVE | `credit/debit(transactionId, player, amount, source)` — PR #246 / implementation head `cf4f1c389bdb814c3067d5266dfda63337aa9bbd`. The server writes the wallet value, revision and immutable transaction receipt atomically; duplicate retries/restarts return the committed result without moving currency again, conflicting reuse fails closed, and insufficient debits do not mutate state. Client-authored balances, prices and rewards are not accepted here. |
| SVC-017 | TODO | Quest reward commit/idempotency. |
| SVC-018 | TODO | Persistent world-object mutation/idempotency. |
| SVC-019 | NEXT/PARTIAL | Reconnect/restart active-session recovery. PR #310 / merge `cc7f4409662ca5580c038d3200ff4e398a11dbdc` provides durable pending post-battle transaction discovery/replay. Continue with Fabric server-start/reconnect wiring; do not synthesize terminal battle state for an active unresolved session. |
| SVC-020 | BLOCKED/PARTIAL | Server-only PTU world-action usage reservation. PR #224 atomically caps canonical Daily usage by Trainer/action/RPG-day and rejects unknown canonical Trainers before consumption; Scene/Encounter/turn/round integration waits on authoritative PTU lifecycle/policy contracts. |
| SVC-021 | LIVE/PARTIAL | `assessWorldTask(player, task, context)` — PR #226 / `2de8d720428b98a6d7375e793a89b5ff20e9c14c` reads canonical Trainer capabilities and applies server-authored Ouros task quality curves; PR #227 adds the normal-world crafting workstation consumer; PR #240 adds a physical Survival-based field camp consumer; PR #241 freezes that assessment into a durable server-owned camp result. Continue reusing this boundary for cooking, technology, medicine, research, occultism, repairs and other non-battle world interactions. |
| SVC-022 | LIVE | `craftWorldTask(attemptId, player, recipe, quantity)` — PR #229 persists the plan and frozen quality odds before rolling, resolves one server outcome, consumes canonical reservations recoverably, emits one deterministic canonical output, and is retry/restart safe. PR #233 invokes this boundary from the normal world workstation; PR #235 invokes it from explicit `/autoptu craft` requests with server-generated attempt IDs; PR #236 exposes that same authority through the workstation selector. |
| SVC-023 | LIVE | `assessCraftMaterials(player, recipe, quantity)` — PR #230 aggregates only owned, unreserved durable canonical item stacks and reports required/available/missing quantities without reserving, rolling, or consuming. |
| SVC-024 | LIVE | `depositCraftIngredient(player, serverObservedStack)` — PR #231 / `1648de45c7cd58ab6a2232d7a0c7e744cf5b986a` accepts only authored recipe ingredient templates, persists them into stable per-player/template canonical stacks through revision CAS, and is invoked from a server-observed held Minecraft stack. PR #238 / implementation commit `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8` adds PREPARED/WITHDRAWN/CANONICAL_APPLIED/COMMITTED journaling, forces server-owned player inventory persistence after withdrawal, reconciles pending transfers on reconnect, and uses an idempotent canonical handoff receipt so retries cannot double-credit. |
| SVC-025 | LIVE | `establishFieldCamp(attemptId, campId, player, task)` — PR #241 / implementation head `fcc356e4f2260caf4e7d142a23a4c03d5bf02d23`. The server derives physical camp identity, freezes canonical Trainer capability plus authored Ouros quality odds before resolution, persists one result exactly once, and reuses it after retry/reconnect/restart without creating PTU Feature policy or battle state. |
| SVC-026 | LIVE | `inspectBag(player)` / `inspectItem(player, itemKey)` — PR #243 / implementation head `a5667c5b6f482f3aae73c77694a47467fb4c7db1` supplies the owner-scoped durable bag projection; PR #244 / implementation head `74c9341ab8cc13e3445f133220e8fd0763dbbfdc` adds exact stack/template inspection with reservation and revision detail. Neither boundary applies item legality or effects. |
| SVC-027 | LIVE | `inspectWallet(player)` — PR #245 / implementation head `a1ba9c1d5cddb18f9e05e275536194654f19c03f`; resolves only owner-scoped durable canonical wallet state and exposes currency, balance and revision without performing a transaction. |
| SVC-028 | LIVE | `inspectShop(player, shopId)` — PR #247 / implementation head `36870beb75976891d0d4ebbe2f1edfaf78423e36` supplies the authored catalogue; PR #250 / `748a57efc20678962f65efad9d4feb7450bafd54` adds server-owned durable remaining-stock/revision projection. It performs no wallet/item mutation and accepts no trusted client price, stock, currency or template data. |
| SVC-029 | LIVE | `purchase(purchaseId, player, shopId, offerId, quantity)` — PR #251 / implementation head `05c46f1b309988cec67ab8caf2ddb44a76dfecbe`. The server freezes authored offer truth, journals the attempt, debits/refunds currency with immutable receipts, depletes stock with its own idempotency receipt, grants a deterministic canonical item exactly once, and resumes incomplete attempts on server start. No PTU item effect is applied. |
| SVC-030 | LIVE | `inspectPokemonStorage(player)` — PR #252 / implementation head `272ce7c6273cef95f64413432c8aef7163a05bfa`. Resolves only the authenticated Trainer's durable box identities through canonical Pokemon ownership and rejects party/storage overlap; performs no deposit, withdraw or PTU mutation. |
| SVC-031 | LIVE | `transferPokemonStorage(transferId, player, direction, sourceSlot)` — PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767`. The server re-resolves source membership and canonical ownership, journals source removal and target addition, uses revision-CAS writes, resumes pending transfers at server start and verifies terminal party/box exclusivity. It applies no PTU battle or capture rule. |
| SVC-032 | LIVE | `sell(saleId, player, shopId, itemTemplateId, quantity)` — PR #254 / implementation head `07637aa6fdefb14e3a42e36788bab7fc9c35a38e`. The server resolves explicit authored sell price/currency, selects and reserves an owned canonical stack, consumes it behind a retained transaction lock, credits currency exactly once, releases the lock only after durable credit, and resumes incomplete attempts on server start. Retail stock/replenishment and PTU item behavior are not inferred. |
| SVC-033 | LIVE/PARTIAL | `dialogue(npcId)` / `dialogue.option(optionId)` — PR #257 / implementation head `0d62ec169c122749b5360e4c7fd6a1435c16a725`. The server owns canonical NPC identity, opening text, labels and responses; Fabric binds a physical presentation actor one-way and revalidates the authored option on every click. Conditional/persistent dialogue-state mutation remains future work. |
| SVC-034 | LIVE | `acceptQuest(player, npcId, questId)` — PR #258 / implementation head `a60a4084660a87e5315e754d08684c834b7921a8`. The server resolves authored quest/giver truth, writes one owner-scoped ACCEPTED entry through revisioned atomic persistence and returns the existing entry on duplicate retry. It grants no reward, XP, item, PTU action or battle effect. |
| SVC-035 | LIVE | `inspectQuestJournal(player)` / `inspectQuest(player, questId)` — PR #259 / implementation head `40b4a2de256b5822d044a91f4c44cb8a92924bd8`. The server reads only the owner-scoped durable journal, resolves every stored quest ID against the authored catalogue, rejects unknown/unaccepted detail requests and performs no objective, reward, XP or PTU mutation. |
| SVC-036 | LIVE | `trackQuest(player, questId)` — PR #260 / implementation head `d2d554d67c418881280df02f7ed848bc53b23220`. The server accepts only a server-authored quest already present in the authenticated Trainer's durable journal, persists one tracked quest through revision CAS, and treats repeated selection as idempotent. It does not advance objectives, claim rewards or apply PTU behavior. |
| SVC-037 | LIVE | `inspectItemStorage(player)` / `transferItemStorage(transferId, player, direction, item, quantity)` — PR #261 / implementation head `a9f7882d3da7cf8b7767903d03be35ee5cd582f2`. The server resolves bag ownership/reservation state or stored quantity, journals CREATED/SOURCE_REMOVED/TARGET_ADDED/COMMITTED, uses idempotent storage receipts and retained bag locks, resumes pending transfers on startup and never interprets item effects. |
| SVC-038 | LIVE | `requestTrainerChallenge(player, npcId, challengeId)` — PR #268 / implementation head `2a48165f5011f41dcdffb1766add7ce2a4a39df8`. Resolves only server-authored challenge/NPC identity plus authenticated Trainer and persistent party state, then returns an immutable party identity/revision snapshot. It performs no PTU battle-start legality, RNG, action, damage, result or reward mutation. |

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
| LIVE | Explicit long-term active-party + box/storage aggregate. PR #252 / implementation head `272ce7c6273cef95f64413432c8aef7163a05bfa` adds the owner-scoped durable box; PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767` adds crash-recoverable exclusive movement between that box and the active-party encounter profile. |
| LIVE | Starter/onboarding claim state via PR #203 / `fb74ac9470ceaf25c13ab02337038ef3b75e2b3d`. |
| TODO | Trainer presentation/profile data. |
| TODO | Trainer XP/level/progression. |
| TODO | Pokémon XP/progression/evolution choices. |
| BLOCKED/PARTIAL | Canonical inventory quantities and wallet. PR #231 persists authored crafting ingredient quantities in canonical item stacks, PR #233 consumes those canonical quantities into exactly-once durable craft outputs from the normal workstation, PR #235 exposes the same canonical mutation through explicit recipe fallback requests, and PR #236 exposes recipe selection/readiness through the normal workstation. PR #238 closes the crash-recoverable Minecraft-to-canonical ingredient handoff. PR #243 adds owner-scoped bag reads; PR #244 adds exact stack/template inspection. PR #245 adds durable wallet state and `/autoptu money`; PR #246 adds atomic exactly-once currency mutation receipts. PR #247 adds the authored shop catalogue; PR #250 adds durable mutable shop stock. PR #251 adds durable exactly-once purchases. PR #254 adds restart-safe canonical sales. PR #261 / implementation head `a9f7882d3da7cf8b7767903d03be35ee5cd582f2` adds owner-scoped long-term item storage plus crash-recoverable bag/storage transfers, so stored quantities are outside bag sale/crafting/reservation reads. Remaining item use and held-item equipment are blocked on authoritative PTU item behavior. |
| LIVE | Durable canonical item storage and transfer journal — PR #261 / implementation head `a9f7882d3da7cf8b7767903d03be35ee5cd582f2`. Stored template quantities, revision and idempotency receipts persist owner-scoped under the world save; transfer intent and CREATED/SOURCE_REMOVED/TARGET_ADDED/COMMITTED stages recover on server start without duplication or loss. |
| LIVE | Durable Minecraft-to-canonical crafting ingredient handoff journal — PR #238 / implementation commit `317cb76f879315fc9cc0f496c5fbd3b4ceaf74f8`. Pending withdrawals survive restart/reconnect, reconcile only against server-persisted inventory state, and canonical retries are idempotent. |
| LIVE | Durable canonical wallet transaction receipts — PR #246 / implementation head `cf4f1c389bdb814c3067d5266dfda63337aa9bbd`. Balance, wallet revision and immutable transaction identity are persisted atomically; retry/restart cannot double-apply a committed credit or debit. |
| LIVE | Durable canonical shop stock — PR #250 / implementation head `748a57efc20678962f65efad9d4feb7450bafd54`. Remaining quantity and revision persist per authored shop/offer under the world save; stale CAS and implicit replenishment fail closed. |
| LIVE | Durable canonical shop purchase journal — PR #251 / implementation head `05c46f1b309988cec67ab8caf2ddb44a76dfecbe`. Frozen purchase intent and stage persist under the world save; deterministic wallet, stock and item identities let startup recovery resume without double charge, double depletion or duplicate item grant. |
| LIVE | Durable canonical shop sale journal — PR #254 / implementation head `07637aa6fdefb14e3a42e36788bab7fc9c35a38e`. Frozen item instance/revision, authored price/currency and CREATED/ITEM_RESERVED/ITEM_CONSUMED/WALLET_CREDITED/COMMITTED stages persist under the world save; startup recovery cannot consume or credit the same sale twice. |
| LIVE | Durable canonical party/box transfer journal — PR #253 / implementation head `525df330861bb12d1c06a5d9077edf84eb026767`. Transfer identity, direction, canonical Pokemon ID and CREATED/SOURCE_REMOVED/TARGET_ADDED/COMMITTED stage persist under the world save so startup recovery can finish an interrupted move without duplication or loss. |
| LIVE/PARTIAL | Quest journal/objectives/reward claims. PR #258 / implementation head `a60a4084660a87e5315e754d08684c834b7921a8` persists owner-scoped accepted quest identity/state/revision and proves the entry across a real dedicated-server restart. PR #259 / implementation head `40b4a2de256b5822d044a91f4c44cb8a92924bd8` exposes that same durable journal through owner-scoped fallback reads without creating objective or reward state. PR #260 / implementation head `d2d554d67c418881280df02f7ed848bc53b23220` adds one durable tracked-quest pointer, schema-v1 migration compatibility and revisioned idempotent switching. Objective progress and reward claims remain TODO under RPG-003/RPG-004. |
| TODO | NPC relationships/factions/rivals. |
| TODO | Badges/league/tournament records. |
| TODO | Discovery/world-event flags. |
| TODO | Fast-travel unlocks/checkpoints. |
| TODO | Active encounter session journal. |
| TODO | Active battle checkpoint/recovery journal. |
| LIVE/PARTIAL | Post-battle result commit ledger/idempotency keys — PR #305 adds single-Pokémon final-state CAS persistence; PR #310 / merge `cc7f4409662ca5580c038d3200ff4e398a11dbdc` adds a durable whole-roster/item PREPARED/COMMITTED journal and retry/restart replay. Automatic creation from a normal battle remains blocked on the authoritative final-state handoff. |
| BLOCKED/PARTIAL | Trainer PTU Daily action usage and monotonic RPG-day state — PR #224; stored under the server world save and durable through reconnect/restart. Final Scene/Encounter/turn/round frequency semantics remain blocked on authoritative PTU lifecycle/policy contracts. |
| LIVE | Durable world-task/craft attempt ledger — PR #229. Stable attempt IDs persist frozen quality odds, planned canonical ingredient reservations, one resolved outcome, deterministic output identity, and exactly-once commit/recovery state. PR #233 binds this ledger to normal-world workstation craft requests; PR #235 binds explicit command requests to the same ledger. |
| LIVE | Durable physical field-camp result ledger — PR #241 / implementation head `fcc356e4f2260caf4e7d142a23a4c03d5bf02d23`. Camp identity, establishing Trainer, frozen Survival-derived Ouros quality distribution and committed result persist under the Minecraft world save and are reused after restart. |

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
