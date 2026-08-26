# Ouros Implementation Program

Status: ACTIVE EXECUTION PLAN
Date: 2026-08-26
Scope: AutoPTU-Cobblemon-RPG runtime implementation fed by the existing Ouros Narrative research corpus.

## Product target

The research phase has already established a broad model of how a persistent Pokémon world can work. The implementation phase now turns that model into explorable Minecraft/Cobblemon systems.

A task is valuable when it creates one or more of these outputs:

- a visible authored place;
- a reusable building/worldgen primitive;
- persistent Pokémon or NPC behavior;
- a real spawn/habitat rule;
- a puzzle or physical mechanism;
- a service, institution or schedule;
- a world event or durable aftermath;
- a battle staging/handoff primitive;
- a repeat-visit state change;
- automated evidence that the result survives server restart and multiplayer use.

Pure planning without a corresponding implementation target is no longer a normal deliverable.

## Authority inputs

The writable runtime is this repository.

`Teffa14/AutoPTU-Cobblemon-Narrative`, especially branch `agent/pass-53-evolution-life-stage`, is the source of world research, provenance, design authorities and non-canon candidates. Research and proposals do not become Ouros canon merely because they are implemented as a prototype.

`Teffa14/AutoPTU-Java` remains the read-only authority for PTU battle legality and results.

Minecraft/Fabric/Cobblemon owns world presentation, geometry, entity materialization, interaction and playback. It must not recreate missing PTU rules.

## Research-to-runtime map

The Narrative corpus is large enough that implementation should consume it by system family rather than by pass number.

### Authored places and the built world

Relevant research includes architecture and adaptive reuse, civic governance/public works, archives and museums, botanical gardens, battle institutions, education, care facilities, nurseries, commerce, hospitality, transport, utilities, mining, waterways, irrigation, structural inspection, logistics and institutional succession.

Runtime targets:

- structure templates and modular authored pieces;
- believable interiors and back-of-house spaces;
- terrain-aware placement;
- staff/public/service circulation;
- opening hours and operational states;
- maintenance, repair and adaptive-reuse variants;
- signage, notices and wayfinding;
- persistent service availability;
- repeat-visit building state.

### Wild Pokémon ecology and individual life

Relevant research includes wild collectives, spatial ecology/home range, territory, migration, orientation/homing, diel activity, dormancy/torpor, courtship, vigilance/alarm networks, camouflage/mimicry, molting, nesting, rehabilitation/release, telemetry, acoustic monitoring, community science, fisheries, fungi, grazing, fire ecology, drought, alpine ecology, coastal systems, coral systems, vector ecology and species-specific environmental relationships.

Runtime targets:

- habitat volumes and ecological zones;
- spawn eligibility by location/time/world state;
- persistent individual identity for important Pokémon;
- home/use points and route following;
- feeding, shelter, lookout, courtship, rest and work-like behavior modes;
- seasonal and diel presence;
- group materialization that does not confuse loaded entities with population truth;
- observation records and revisitable field sites;
- world-state-driven withdrawal, relocation and recovery.

### NPCs, relationships and institutions

Relevant research includes social bonds, mentorship, clubs, learning, agreements/mediation, credentials, volunteering, performance careers, media, communication networks, antagonist agency, civic institutions and professional succession.

Runtime targets:

- persistent NPC identity;
- schedules and work loops;
- institutional roles and permissions;
- dialogue selected from server-owned state;
- memory of observable prior events;
- staff replacement/succession;
- crowds and event attendance;
- information propagation without treating publication as truth.

### Exploration, puzzles, dungeons and investigation

Relevant research includes mission/dungeon grammar, challenge-state authority, archaeology, cartography, anomalous spaces, case/evidence systems, caves/mines, ruins, archives, environmental observation and field research.

Runtime targets:

- route graphs with loops and shortcuts;
- vertical connections;
- persistent puzzle state;
- block mechanisms driven by server state;
- secrets and optional paths;
- clue/observation persistence;
- access permissions;
- safe reset/recovery;
- environmental storytelling through physical state rather than exposition.

### Events, hazards, public memory and long-term change

Relevant research includes public memory, crises/rescue/recovery, wildfire, climate baselines, air quality, seismic events, contaminated land, infrastructure disruptions, festivals, civic response, communication outages and long-term monitoring.

Runtime targets:

- world process scheduler independent of chunk uptime;
- event phases and due times;
- closures, detours and service disruption;
- shelters/crowd routing;
- block-state transformation sets;
- notices and public information state;
- recovery/reconstruction phases;
- Chronicle records and repeat-visit aftermath;
- no technical restart interpreted as in-world time reversal.

### Food, agriculture, production and logistics

Relevant research includes agriculture, hospitality, food culture, material culture, crafting economy, irrigation, grazing, fisheries, markets, storefront continuity, courier/parcel systems and transport.

Runtime targets:

- farms and production sites as functioning places;
- stock/inventory batches separate from PTU item definitions;
- delivery schedules;
- venue opening/service state;
- harvest and maintenance presentation tied to authored world clocks;
- loading docks, storage, workshops and service corridors that justify building form.

### Formal battles and Pokémon challenge institutions

Relevant research includes battle institutions/challenge circuits, Gym design, contest/performance culture and encounter implementation contracts.

Runtime targets:

- physical staging areas and believable arenas;
- public lobby and challenge circulation;
- staff/trainer preparation spaces;
- battle participant binding;
- static arena snapshots for AutoPTU;
- authoritative battle-result ingestion;
- post-battle institutional state;
- rematch/revisit history without inventing combat bonuses.

### Persistent scene execution

`design/ouros-runtime-scene-world-execution-contract.md` defines the intended execution pattern:

world process -> trigger -> scene state machine -> world effects -> optional AutoPTU handoff -> result ingest -> durable aftermath.

Runtime targets:

- persistent process IDs;
- actor bindings;
- zones/triggers;
- optimistic concurrency;
- idempotent effects;
- reconnect/restart recovery;
- exactly-once state mutation where required.

## Build pipeline decision

The current Meridian Canopy Gym proved that large quantities of direct `setBlockState` calls do not guarantee good architecture.

The production direction is now:

1. Use Java geometry primitives for terrain adaptation, curves, organic forms, connective tissue and stateful mutations.
2. Use Minecraft structure templates/NBT for detailed authored rooms, facade modules, props and repeatable pieces.
3. Use jigsaw/template pools when a complex should be assembled from authored modules.
4. Keep major landmarks intentionally authored rather than fully random.
5. Export the final block states from a real Fabric + Cobblemon server to the browser review site.
6. Reject showcase status until the exact Minecraft viewer passes visual review.

Direct box filling remains acceptable for MICRO TESTS and early blockouts only.

## External implementation toolkit research

### Agent/modding skill research

`minecraft-modding-workbench` by adhi-jp is a useful implementation reference for Fabric/NeoForge project inspection, NBT, dependency API lookup, worldgen/resource validation and GameTest discipline.

Its preferred `minecraft-modding` MCP server is not installed in the current ChatGPT environment, so the project must not claim MCP-verified facts. The useful workflow rules are still adopted: inspect the actual workspace first, verify version-specific APIs, prefer runtime slices over generic advice, and use GameTest/resource validation for world-heavy work.

The public `minecraft-ci-release` skill is useful for release/CI discipline but does not solve gameplay or world construction. Existing Ouros CI already covers much of that role.

The ChatGPT plugin search did not expose a Minecraft/worldgen-specific installed plugin. Figma appeared as a possible design tool, but it is not required for the current implementation path.

### Worldgen and structure libraries worth evaluating

Lithostitched

- Fabric 1.21.1 support verified from current public release metadata.
- MIT license.
- Data-driven worldgen additions include structure conditions, template/worldgen modifiers, biome injection and terrain-shaping helpers.
- Strong candidate for Ouros regional structure placement and terrain-aware conditions.

Structure Pool API

- Fabric 1.21.1 release exists.
- MIT license.
- Focused API for injecting structures into template pools.
- Candidate when Ouros needs to extend village/structure pools without custom invasive code.

Repurposed Structures

- Fabric 1.21.1 supported.
- LGPL-3.0.
- Useful source/code reference for server-side vanilla-block structures, biome variants and structure configuration.
- Reuse must comply with LGPL; original Ouros structures should remain independently authored.

YUNG's API

- Fabric 1.21.1 release exists.
- LGPL-3.0.
- Useful worldgen/library reference and possible dependency if a YUNG component becomes valuable.
- Do not add only because it is popular; add when a concrete task requires its API.

Litematica

- Fabric 1.21.1 exists.
- LGPL-3.0.
- Strong development-authoring reference/tool for schematics and creative iteration.
- It should not become required runtime authority.

### Building-block dependencies worth evaluating

Beautify: Refabricated

- Fabric 1.21.1.
- MIT license.
- Vanilla-styled decorative blocks and custom models.
- Strong candidate for a controlled Ouros decoration palette once the exact browser viewer can also extract/render modded block models and textures.

Decorative Blocks Reborn

- Fabric 1.21.1.
- MIT license.
- Candidate for additional structural/decorative shapes.

Any new block mod becomes a client-and-server dependency and therefore requires an explicit dependency decision, production smoke coverage and browser-viewer resource extraction before its blocks are used in showcase locations.

### Reference-only projects

When Dungeons Arise

- High-value reference for scale, silhouette and dungeon composition.
- Public distribution is All Rights Reserved. Do not copy its structures/assets.

Dungeons and Taverns

- High-value reference for vanilla-compatible environmental structures and adventure density.
- All Rights Reserved. Reference only.

ChoiceTheorem's Overhauled Village

- Fabric 1.21.1 exists.
- Source metadata declares CC BY-NC-ND 4.0 for the inspected line.
- Reference only for Ouros adaptation; do not derive or redistribute modified assets.

Structory / Terralith family

- Excellent references for world integration and exploration density.
- Their custom license permits learning/reference but restricts copying portions into another project without permission.
- Reference only unless explicit permission is obtained.

Structure Gel API

- Useful conceptual reference for jigsaw authoring.
- Current public support is Forge/NeoForge rather than Fabric 1.21.1 and its distribution is restrictive.
- Not selected for the Ouros Fabric runtime.

Integrated Dungeons and Structures

- Excellent detail-density reference.
- Current 1.21.1 target is NeoForge and the project is All Rights Reserved.
- Reference only.

## Execution waves

### Wave A: authored-build foundation

OI-001 Authored Build Toolkit

Create reusable Java primitives for circles/ellipses, rings, cylinders, arches, tapered supports, roof profiles, curves, organic trunks/branches, canopy clusters, retaining walls and bounded terrain operations. Add deterministic tests for generated coordinate sets. Apply the primitives to one visible section of Meridian Canopy Gym.

OI-002 Structure Template Asset Pipeline

Load authored `.nbt` structure templates from mod resources through Minecraft's structure-template system. Add placement transforms, rotation/mirror, processors and deterministic server tests. The exact browser exporter must see the final placed blocks without special handling.

OI-003 Modular/Jigsaw Composition

Add a small Ouros-owned template-pool/jigsaw proof using authored modules. Evaluate vanilla jigsaw first, then Lithostitched or Structure Pool API only if they remove a concrete limitation. Prove multiple valid layouts and placement bounds.

OI-004 Terrain Fitting and Approach Builder

Create bounded terrain sampling, foundation fitting, retaining, path grading, drainage and vegetation-edge helpers. Structures must reject or adapt unsuitable placement rather than flattening a giant rectangle.

OI-005 Modded Resource Viewer Support

Teach the exact Minecraft browser viewer to extract blockstates, models and textures from approved mod jars in the same production smoke set. This is required before Beautify/Decorative Blocks or another content mod can be used in reviewed builds.

OI-006 Spatial GameTest / Build Metrics

Add automated checks for accessibility, doorway clearance, reachable route anchors, floor separation, forbidden floating volumes, placement bounds and required signature-location anchors. Tests do not judge beauty; they catch structural mistakes before visual review.

### Wave B: rebuild Meridian Canopy Gym as the first signature candidate

OI-101 Site and approach redesign

Replace the artificial square platform with a landscaped botanical/civic campus approach. Include terrain transition, paths, retaining, water, service access, distant silhouette and real specimen trees.

OI-102 Conservatory atrium and botanical core

Build a major sectional interior with a large authored tree, greenhouse structure, multiple elevations, hanging walkways, understory planting, water management and observation/use areas.

OI-103 Gym challenge route

Create a persistent challenge state with at least one loop, one shortcut, multiple elevations, optional observation content and a recoverable physical mechanism. The challenge must be readable without floating labels.

OI-104 Leader arena and spectator institution

Build a clearly legible Gym climax with an actual battle floor, leader/trainer staging, spectators, seating/overlooks, entrances, safety/service circulation and backstage preparation space. Arena geometry must be separately snapshot-able for AutoPTU.

OI-105 Interior life and decoration

Populate reception, lockers, storage, staff rooms, maintenance, archives/records, public waiting, signage, utilities, planters and institutional props. Use a deliberate material/decor palette and avoid random clutter.

OI-106 Gym actors and repeat-visit state

Add staff schedules, visitor use, persistent Pokémon use zones, opening/closed state, post-challenge changes and at least one return-visit callback.

### Wave C: living-world behavior foundation

OI-201 Habitat and spawn policy engine

Separate ecological population/world state from loaded entities. Spawn eligibility must read authored habitat, time, process state and species policy.

OI-202 Persistent Pokémon world identity

Extend the durable Pokémon identity model from encounter binding into named overworld actors with materialize/dematerialize recovery.

OI-203 Behavior graph runtime

Implement reusable overworld modes such as REST, FEED, OBSERVE, PATROL, WORK, SHELTER, DISPLAY, WATCH, WARN, WITHDRAW and RETURN_HOME. These are presentation/world behaviors and do not create PTU combat effects.

OI-204 Daily/seasonal scheduler

Run actor/institution/ecology schedules from persistent world time rather than chunk uptime. Support offline recomputation and restart recovery.

OI-205 Group behavior and shared signals

Implement bounded group presentation for authored collectives, including lookout/alarm propagation and coordinated world withdrawal without pretending Minecraft pathfinding is PTU tactical AI.

OI-206 Observation and field-record interactions

Turn the research/observation layers into actual world interaction points that can record what the player observed and support later callbacks.

### Wave D: first connected district and route

OI-301 Regional topology model

Represent named places, paths, roads, service routes, waterways and later rail/ferry connections as a server-owned graph.

OI-302 Meridian botanical/research district

Build several authored sublocations around the Gym so it belongs to a district rather than standing alone.

OI-303 Route ecosystem slice

Create a route with habitat transitions, shelter/use points, environmental clues, at least one optional path and multiple non-battle reasons to stop.

OI-304 Public services slice

Implement one clinic/service facility, one shop/hospitality venue and one transport or civic service with operating state and staff/use logic.

OI-305 Information and notices

Physical signs, public notices and dialogue should reflect service closures, events and discoveries without making presentation state the canonical source.

### Wave E: puzzles, events and durable aftermath

OI-401 Generic persistent mechanism runtime

Server-owned doors, switches, rotating/raising structures, conditional routes and idempotent block-transform operations.

OI-402 Scene/world process scheduler

Implement the Narrative runtime scene contract with revisioned scene instances, triggers, queued world effects and restart recovery.

OI-403 Crisis/event vertical slice

Run one noncombat world event that changes routes, actor behavior and physical presentation, then enters a persistent recovery phase.

OI-404 Public-memory callback

A later NPC/service/location must react to a prior observable event through durable state rather than a one-shot quest flag.

### Wave F: AutoPTU world integration

OI-501 Authenticated wild battle handoff

Move the current graphical battle playback pieces behind the normal authenticated PLAYER-vs-WILD authority path.

OI-502 Arena snapshot contract

Convert an authored world battle location into a stable AutoPTU arena snapshot without inferring PTU terrain effects from Minecraft blocks.

OI-503 Semantic aftermath projection

Consume allowed AutoPTU result fields once, then update scene/world state and presentation idempotently.

OI-504 Institution battle flow

Connect challenge registration, staging, leader battle, result, public/institutional record and rematch availability without fabricating PTU progression math.

## Task order

The active order begins with OI-001 through OI-006 because better content cannot scale on the current box-oriented builder.

The first signature candidate is Meridian Canopy Gym because the exact server viewer already gives rapid visual feedback. It is intentionally disposable as a design if the rebuild cannot reach the quality bar.

Behavior work begins as soon as reusable spatial zones and better authored locations exist. It does not wait for every structure tool to be perfect.

AutoPTU battle integration continues in parallel where upstream capability exists, but world builders must not block on missing advanced combat families when a noncombat/reduced implementation can be authoritative.

## Definition of done for implementation tasks

Every implementation issue must state:

- the player-visible or reusable result;
- authority boundaries;
- persistence/restart behavior when applicable;
- exact files or assets created;
- automated tests or runtime evidence;
- browser review evidence for authored geometry;
- known placeholders;
- license/provenance for any external code or asset used.

A task is not complete because a document says the system exists.

## Immediate active task

OI-001 begins now.

The first output is a reusable geometry library plus a visible Meridian test application. The intent is to prove that Ouros can construct curved, layered, organic and structurally legible forms without hand-writing another sequence of rectangular fills.
