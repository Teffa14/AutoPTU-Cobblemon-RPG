# Ecosystem knowledge and correlation program

## Purpose

AutoPTU-Cobblemon-RPG must continuously acquire, normalize, correlate and implement useful knowledge from Minecraft, Cobblemon, major Cobblemon modpacks and AutoPTU/PTU into one coherent server-authoritative RPG system.

This is not a one-time research document. It is a permanent engineering workflow. Every implementation slice that touches world interaction, crafting, items, facilities, storage, economy, travel, quests, Trainer capabilities or user experience must re-check the relevant ecosystem sources and update the project knowledge base when new information changes the best design.

## Permanent source layers

The project studies four layers together:

1. **Minecraft Java Edition target** — vanilla stations, recipes, item semantics, block interactions, inventories, containers, crafting, cooking, brewing, smithing, enchanting, repair, farming, redstone, villager/economy patterns and quality-of-life conventions.
2. **Cobblemon target** — Pokémon-facing items, berries, apricorns, machines, blocks, recipes, storage, riding, Pokédex/scanning, breeding/daycare where present, presentation, models, UI and networking. Cobblemon battle-state authority is never consumed.
3. **Large Cobblemon modpacks and companion mods** — proven quality-of-life, content composition, cooking, storage, shops, towns, facilities, exploration, recipe browsers, backpacks, furniture, automation, world structures, economy, multiplayer services and Pokémon-themed production loops.
4. **AutoPTU/PTU** — canonical Trainer skills, Edges, Features, classes, perks where applicable, equipment, consumables, crafting-oriented capabilities and items that Minecraft/Cobblemon do not provide.

The final design is not a copy of any one source. Ouros selects the strongest reusable interaction pattern, preserves PTU identity, and keeps server authority intact.

## Authority boundary

Cobblemon and Minecraft are reusable for presentation, blocks, entities, recipes, stations, menus, inventories, networking hooks and interaction ergonomics.

They are not authority for AutoPTU battle participants, BattleState, HP, statuses, moves, abilities, legality, RNG, damage, capture results or battle outcomes.

Trainer capability values used by Ouros world tasks come from canonical AutoPTU RPG state. A modpack may teach us how an interaction should look or feel, but it must never become the source of canonical Trainer competence or PTU rule truth.

## Mandatory research-before-implementation loop

Before implementing or extending a world-facing system:

1. Revalidate the current project target versions.
2. Revalidate the latest release of every relevant mod/modpack in the study corpus.
3. Inspect changelogs and dependency manifests for changes since the last recorded review.
4. Inventory the relevant Minecraft and Cobblemon actions, recipes, stations, items and UI flows.
5. Inventory equivalent or adjacent implementations from the major modpacks.
6. Cross-reference the AutoPTU/PTU Trainer skills, Edges, Features/classes and item catalogue.
7. Classify every candidate as `REUSE`, `ADAPT`, `REIMPLEMENT`, `PTU_ONLY`, `PRESENTATION_ONLY`, `BLOCKED_AUTHORITY`, or `IGNORE`.
8. Update the correlation matrix and version ledger before or with the implementation PR.
9. Implement one bounded player-visible slice.
10. Validate with unit/integration/runtime evidence and update the matrix with the real implementation status.

Research that does not feed a correlation, decision or implementation should not accumulate as unstructured notes.

## Core study corpus

### Tier A — always inspect

- Minecraft Java Edition 1.21.1 target behavior and data.
- Cobblemon 1.7.3 target behavior and data.
- Cobblemon Official Modpack [Fabric].
- COBBLEVERSE - Pokemon Adventure [Cobblemon].
- Cobblemon Delta.
- Cobblemon Academy 2.0.
- AutoPTU current Python oracle/reference repository.
- AutoPTU-Java current authoritative engine repository.
- AutoPTU/PTU Trainer class, Feature, Edge, skill and item datasets already present in the reference repository.

### Tier B — inspect when relevant

- Cobblemon Expanded variants.
- Cobblemon + Create / technology-oriented packs.
- Storage/backpack integrations used by major Cobblemon packs.
- Recipe browser integrations such as REI/EMI/JEI when present.
- Furniture and functional-station integrations.
- Economy, shop, GTS, Wonder Trade, daycare/breeding, raids, towns, dungeons and quest companion mods used by major packs.
- Performance, minimap, navigation and accessibility/QoL mods that materially improve the RPG loop.

Tier B projects may target older Minecraft versions or a different loader. They are pattern references unless their exact dependency is verified compatible with the project target.

## What must be extracted

For each relevant system or action, record at least:

- source project and exact version reviewed;
- Minecraft/Cobblemon/modpack action or recipe ID when available;
- station/block/menu used;
- inputs and outputs;
- automation support;
- failure behavior;
- persistence behavior;
- multiplayer/server authority behavior;
- primary Trainer skill correlation;
- secondary Trainer skill correlations;
- relevant PTU Edge(s);
- relevant Trainer Feature/class/perk(s);
- PTU item relationship;
- whether the PTU item already exists in Minecraft/Cobblemon/modpacks;
- knowledge gate if any;
- possible graded-quality outcomes;
- exploit/duplication/reroll surface;
- reuse classification;
- Ouros implementation status and PR/commit.

## Trainer capability correlation doctrine

Do not map every recipe to one skill by intuition. Use PTU source data and explicit authored reasoning.

Examples of likely domains that must be verified against source content include:

- Survival: camp cooking, field preparation, gathering, shelter, tracking and wilderness production.
- Technology Education: machinery, electronics, Pokétech, advanced fabrication, repair and automation.
- Medicine Education: treatment, medical supplies and clinical production.
- Pokémon Education: Pokémon-specific biology, care, species products and specialized Pokémon equipment.
- Occult Education: occult materials, relics, supernatural fabrication and occult analysis.
- General Education: broad academic/manual knowledge and non-specialized technical tasks where source support exists.
- Intuition: Chef and other feature paths where PTU specifically uses it.
- Perception, Focus, Guile, Command, Charm, Athletics, Acrobatics, Stealth and Combat: only where the actual PTU content or the world task meaning makes them relevant.

Skills are not the whole character. Edges and Trainer Features/classes must be checked independently because a specialist should gain access, consistency, efficiency or superior outputs that a generic high skill rank may not reproduce.

## Item gap program

The project must maintain a separate view of PTU items that are absent from both Minecraft and Cobblemon.

For each missing PTU item, decide whether to:

- map it to an existing Minecraft/Cobblemon item with equivalent semantics;
- create an Ouros item using existing Minecraft/Cobblemon assets/presentation where safe;
- create a new item, block, station or UI asset;
- keep it unavailable until its authoritative PTU effect exists upstream;
- treat it as world-only/non-battle content when that is safe.

Battle item behavior stays authoritative upstream. A Minecraft item representation must not invent missing PTU battle effects.

## Anti-exploit requirement

Any craft or production action that can consume canonical resources and produce canonical outputs must eventually use durable attempt identity, server-side revalidation, atomic reservation/consumption/output commit and idempotent recovery. Preview screens never pre-roll outcomes.

Major modpacks are studied specifically for QoL and interaction patterns, but convenience must not create duplicate-item, disconnect-reroll, automation bypass or client-trust exploits in Ouros.

## Continuous maintenance rule

A recorded version is a snapshot, never a permanent assumption. Before relying on any source in a new PR, confirm the current latest stable release from the project's official distribution/source page and update `ECOSYSTEM_VERSION_LEDGER.md` when it changed.

When a new major Cobblemon modpack or companion mod becomes a strong reference for RPG UX, crafting, economy, storage, exploration or Pokémon world simulation, add it to the corpus rather than waiting for a separate planning cycle.
