# Crafting and capability correlation matrix

## Purpose

This is the living cross-system matrix for Minecraft crafting/production actions, Cobblemon crafting/production actions, major Cobblemon modpack QoL/production patterns, Trainer skills, Edges, Features/classes/perks and AutoPTU/PTU items.

The matrix must grow with implementation. It is not enough to catalogue recipes. Every row should answer what a character is doing, what knowledge matters, what specialist content protects, what can be reused from the ecosystem and what Ouros must add.

## Classification vocabulary

- `REUSE`: use an existing Minecraft/Cobblemon/mod component directly when compatible and safe.
- `ADAPT`: reuse the interaction/station/UI pattern but apply Ouros canonical state and rules.
- `REIMPLEMENT`: build an Ouros equivalent because the source cannot be used directly or does not preserve authority.
- `PTU_ONLY`: PTU item/action has no Minecraft/Cobblemon equivalent and needs an Ouros representation.
- `PRESENTATION_ONLY`: source asset/UI/entity may be reused but not its gameplay authority.
- `BLOCKED_AUTHORITY`: implementation waits on authoritative PTU behavior.
- `IGNORE`: not useful to the Ouros design.

## Required row schema

| Field | Meaning |
|---|---|
| Action family | Human-readable activity, not only recipe ID |
| Source | Minecraft, Cobblemon, exact mod/modpack component, AutoPTU/PTU |
| Exact version reviewed | Version from the ecosystem ledger |
| Station/UI | Crafting table, furnace, campfire, stove, machine, menu, etc. |
| Inputs/outputs | Materials and resulting item/state |
| Primary skill | Canonical Trainer skill most directly relevant |
| Secondary skills | Other verified skills that may modify context |
| Edge(s) | Relevant PTU Edge content |
| Feature/class/perk | Specialist content that changes access, efficiency, quality or output |
| Knowledge gate | Whether conceptual knowledge is required to attempt |
| Quality model | Improvised/standard/excellent or task-specific outputs |
| Automation | Whether vanilla/mod automation can perform the operation and what Trainer competence means for automation |
| PTU item relation | Existing equivalent, partial equivalent or missing PTU-only item |
| Authority | Ouros world rule, upstream PTU rule, presentation-only or blocked |
| Exploit surface | Reroll, duplication, automation bypass, disconnect/restart, client trust |
| Decision | REUSE/ADAPT/etc. |
| Ouros status | Research/TODO/NEXT/LIVE/BLOCKED and PR/commit |

## Minecraft production families to inventory completely

The detailed recipe-level inventory should be generated/maintained from the target version rather than hand-maintained when practical. At minimum correlate these families:

| Family | Vanilla surfaces to include | Initial Trainer-correlation questions |
|---|---|---|
| Basic crafting | 2x2 player crafting, crafting table, shaped/shapeless recipes | General Education, Technology Education, Survival or no skill depending on task complexity |
| Smelting | furnace | Does ordinary refining need competence, or only advanced/specialized materials? |
| Food cooking | furnace, smoker, campfire | Survival versus Chef/Basic Cooking/Intuition specialist path; quality and nutrition outputs |
| Blast processing | blast furnace | Technology Education/material knowledge; industrial quality/efficiency |
| Stone fabrication | stonecutter | General Education/Technology depending on engineered complexity |
| Smithing/upgrading | smithing table | Technology Education, material knowledge, specialist fabrication Features if PTU supports them |
| Repair/customization | anvil | Technology Education or relevant equipment specialist; durability/efficiency quality |
| Brewing | brewing stand | Medicine Education, Pokémon Education, Occult Education depending on product semantics |
| Enchanting | enchanting table | Occult Education and explicit supernatural knowledge gates |
| Cartography | cartography table | General Education, Survival, Perception/Travel specialist content |
| Loom/textiles | loom | General Education plus Fashionista/related specialist content when PTU source supports it |
| Fletching | fletching table and related recipes even where vanilla UI is limited | Survival/Combat/Technology according to authored task |
| Farming/gathering | crops, animals, fishing, composting, beekeeping | Survival, Pokémon Education where Pokémon products are involved, specialist nature paths |
| Redstone/electronics | redstone components and machines | Technology Education; advanced designs may require hard knowledge gates |
| Construction | blocks, scaffolding, structural components | General Education/Technology/Survival depending on engineered versus field construction |
| Fireworks/explosives | fireworks and explosive production | Technology Education and explicit safety/knowledge gates |
| Dyes/cosmetics | dyes, decorative production | General Education, Survival gathering, Fashionista where applicable |

Simple Minecraft recipes do not automatically need a roll. The competence system exists where character knowledge, specialist identity, quality, risk or efficiency creates meaningful RPG differentiation.

## Cobblemon families to inventory completely

Inventory the target Cobblemon version's actual recipe/data files and runtime interactions. At minimum cover:

- Poké Ball and component production;
- apricorn growing, harvesting and processing;
- berries and berry-derived items;
- medicines and healing-related items;
- held items and Pokémon equipment that have world production paths;
- evolution and form-related items where craftable/processable;
- fossils and restoration-related stations/items;
- Pokémon food and consumables;
- machines, PCs, healers and functional blocks;
- rods, lures and encounter-support equipment;
- decorative Pokémon blocks/items where a specialist correlation is meaningful;
- any 1.7.3 recipe/station additions not present in earlier versions.

Cobblemon's existing recipe or block may be reused for world presentation/interaction, but any effect that crosses into AutoPTU battle authority must be delegated or blocked rather than inferred from Cobblemon state.

## Major modpack correlation queue

### COBBLEVERSE

Treat as the primary large-pack UX/content laboratory. Inspect its exact 1.7.3 dependency manifest and attribute each feature to the component mod when possible. Correlate at least:

- cooking/stove/food extensions;
- furniture with functional interactions;
- recipe browser support and custom recipe categories;
- backpacks/storage/PC extensions;
- shops/Poké Mart patterns;
- travel/navigation/minimap QoL;
- Pokémon riding and exploration QoL;
- custom ores/material production;
- pouches and specialized storage;
- structures, loot and progression gates;
- decorative Pokémon item/block presentation;
- any Create/automation-style production dependencies if present.

### Cobblemon Delta

Correlate economy and multiplayer world services in addition to direct crafting:

- shops/economy;
- GTS/trading/Wonder Trade patterns;
- daycare/breeding support;
- raids and reward loops;
- dungeons/Area Zero/Ultra Space progression;
- bounties/quests;
- held-item information and discovery UX;
- storage/service NPC or facility interactions.

### Cobblemon Academy 2.0

Correlate curated progression and player guidance:

- gated facilities;
- teaching/tutorial UX;
- structured recipe/item progression;
- quest/adventure progression;
- QoL that reduces friction without flattening specialist roles.

### Cobblemon Official Modpack [Fabric]

Use as the baseline of what the official Cobblemon ecosystem considers compatible and expected for 1.7.3. Compare every large-pack addition against this baseline to identify what is Cobblemon core, official-pack QoL or third-party enhancement.

### Tier B packs

Cobblemon Expanded variants and Cobblemon+Create/technology packs are pattern references. Mine them for storage, automation, machinery and missing-Pokémon-feature ideas, but do not introduce old-version or NeoForge dependencies into the Fabric target without explicit compatibility validation.

## AutoPTU/PTU source correlation

The reference repository already contains structured or semi-structured sources including:

- `TRAINER_CLASS_CATALOG.md`;
- `TRAINER_CLASS_TREES.md`;
- `reports/trainer_class_catalog.json`;
- `reports/trainer_class_graph.json`;
- `reports/pdf_trainer_content.json`;
- `files/... - Features Data.csv`;
- `files/... - Edges Data.csv`;
- `files/... - Item Data.csv`;
- `files/... - Inv Data.csv`;
- `ITEM_LOG.md`;
- PTU rulebook/audit source text where needed for verification.

These must be used to build a canonical cross-reference rather than guessing from class names.

## Initial specialist domains already identified

| Domain | Skill layer | Specialist content to inspect first | World actions/items to correlate |
|---|---|---|---|
| Cooking/nutrition | Survival, Intuition where PTU says so | Basic Cooking, Chef and its Features | Minecraft food cooking, Cobblemon food, modpack stoves/cooking systems, PTU food items |
| Technology/fabrication | Technology Education | technology-oriented classes/Features/Edges from source data | Redstone, machines, Pokétech, advanced Poké Ball components, repairs, automation |
| Medicine | Medicine Education | Medic and medical Features/Edges | healing supplies, medicine production, treatment stations, PTU medical items |
| Pokémon science/care | Pokémon Education | Researcher/care-oriented Trainer content | Pokémon-specific consumables, breeding/care products, diagnostics, species materials |
| Occult fabrication | Occult Education | Arcanist, Hex Maniac, occult Features/Edges | enchanting-like work, relics, supernatural lures/materials, PTU occult items |
| Wilderness | Survival | Backpacker, Hunter, Traveler and related content | camp crafting, gathering, lures, field food, shelter, navigation supplies |
| Fashion/cosmetics | relevant social skills plus explicit class rules | Fashionista | loom/dyes/clothing/accessories/perfume and PTU fashion items |
| General fabrication | General Education | Hobbyist and broad-learning content | common manuals, generic tools, simple construction and broad craft tasks |

This table is a starting index only. Exact modifiers/access rules must come from PTU source content.

## PTU-only item gap table

Build this table from the AutoPTU item catalogue and keep it exhaustive over time.

| PTU item | PTU category/effect authority | Minecraft equivalent | Cobblemon equivalent | Major-pack equivalent | Representation decision | Crafting correlation | Status |
|---|---|---|---|---|---|---|---|
| _populate from item catalogue_ | | | | | | | RESEARCH |

Do not collapse two items solely because their names are similar. Compare semantics, use context, durability/charges, target and battle/world effects.

## Implementation order

1. Build machine-readable inventories of Minecraft 1.21.1 and Cobblemon 1.7.3 production actions/recipes.
2. Build machine-readable Trainer skill/Edge/Feature/class indices from AutoPTU reference data.
3. Build the PTU item gap index.
4. Add modpack/component-mod patterns and exact dependency attribution.
5. Correlate families first, then individual recipes/items where differentiation matters.
6. Feed approved correlations into `WorldTaskCatalogue`, `canCraft`, crafting transactions, stations and UI.
7. Re-run the version ledger and correlation checks whenever target mods or source packs update.
