# Ouros Minecraft / Pokémon / RPG Reference Corpus

Status: LIVING IMPLEMENTATION RESEARCH INDEX
Date started: 2026-08-26
Purpose: convert external references into implementation lessons for AutoPTU-Cobblemon-RPG. This file is research provenance, not Ouros canon.

## Operating rule

Every serious Ouros build, dungeon, gym, route, settlement or event should consult this corpus and add new references as work continues. References are used for high-level structure, player experience, technical patterns and quality targets. Do not copy protected maps, scripts, dialogue or distinctive layouts block-for-block.

The corpus is deliberately implementation-facing. Each entry should answer at least one of these questions:

- what does the player see or do;
- what makes the place readable and memorable;
- what makes exploration non-linear;
- what implementation primitive makes the experience possible;
- what should persist after the player leaves;
- what failure mode should Ouros avoid.

## Cobblemon adventure maps and regions

### CobbleKanto

Source: public Cobblemon community release, June 2026.
Reference: https://www.reddit.com/r/cobblemon/comments/1tx1ulc/cobblekanto_a_cobblemon_adventure/

Reported scope includes handcrafted towns, routes, caves, gyms, interiors, custom NPCs, trainer battles, puzzles, hidden paths, rewards and multiplayer-aware progression.

Implementation lessons for Ouros:

- a Pokémon region feels authored when towns, routes, caves and gyms are one connected experience;
- hidden paths need physical geography, not only quest flags;
- interiors matter because repeated facade-only buildings destroy the illusion of place;
- progression must tolerate multiplayer ordering and revisit state;
- trainer battles should live inside broader exploration rather than be the only interaction type.

Tags: COBBLEMON, REGION, GYM, INTERIOR, PUZZLE, NPC, MULTIPLAYER, HIDDEN_PATH.

### Cobblemon Johto / Jond maps

Source: CurseForge public world listing.
Reference: https://www.curseforge.com/minecraft/worlds/cobblemon-johto

The map combines Johto/Kanto geography with spawns, NPCs, story, items, many trainers, shops and multiplayer support.

Implementation lessons:

- use the world as the progression interface instead of relying on menus;
- routes should communicate regional transitions through geometry, encounter ecology and landmarks;
- hundreds of small NPC interactions can make a region feel inhabited when they are location-specific;
- story state needs to survive across an explorable world rather than reset on chunk load.

Tags: COBBLEMON, JOHTO, STORY, NPC, REGION, MULTIPLAYER.

### Cobblemon Penumbra Region

Source: CurseForge public world listing.
Reference: https://www.curseforge.com/minecraft/worlds/cobblemon-penumbra

Public description advertises eight gyms, four elite gyms, exploration, battle and progression in a Minecraft-first adventure.

Implementation lessons:

- gym progression can be embedded inside survival/exploration rather than replacing Minecraft;
- optional service/menu conveniences should not erase physical travel unless intentionally authored;
- elite facilities need spatial escalation, not just higher-level opponents.

Tags: COBBLEMON, GYM, ELITE, PROGRESSION.

### Cobbleverse

Source: CurseForge public modpack listing and current community recommendations.
Reference: https://www.curseforge.com/minecraft/modpacks/cobbleverse-cobblemon

Current public description emphasizes gyms, badges, unique structures, custom music and exploration on Minecraft 1.21.1 / Cobblemon 1.7.3.

Implementation lessons:

- region identity can come from discoverable structures rather than a fully fixed map;
- custom music/audio can reinforce route and landmark identity but should remain presentation state;
- gym discovery is itself an exploration loop when worldgen placement remains legible.

Tags: COBBLEMON, WORLDGEN, GYM, MUSIC, STRUCTURE.

### Cobblemon Routes

Source: Modrinth public mod page.
Reference: https://modrinth.com/mod/cobblemon-routes

The mod generates physical routes and towns, named capture areas and route trainers, and can connect structures from other packs.

Implementation lessons:

- physical route graphs are reusable infrastructure;
- named areas should correspond to actual spatial boundaries;
- structure-to-structure connectivity is a first-class worldgen problem;
- Ouros should eventually generate road/trail/canal/rail links from persistent regional topology rather than isolated structures.

Tags: COBBLEMON, ROUTES, WORLDGEN, CONNECTIVITY, TOWNS.

### Ultimate Pokémon Map 2 / Varuna

Source: Planet Minecraft public listing, August 2026.
Reference: https://www.planetminecraft.com/project/full-gameplay-ultimate-pokemon-map-2-cobblemon-adventure-map-with-npcs-400-npcs-13-towns-10-gyms-varuna-studios/

Public listing describes 13 towns, 10 gyms, 400+ named NPCs, Battle Frontier facilities, an eight-floor tower and fully built interiors.

Implementation lessons:

- important settlements need interior density and social density together;
- large regions require a hierarchy of landmarks, services, ordinary buildings and progression facilities;
- endgame facilities should change traversal rhythm and spatial grammar;
- a region-scale map should remain survival-ready and physically coherent instead of being a theme-park chain of arenas;
- thousands of hours of manual building is evidence that showcase quality cannot be replaced by a tiny procedural rectangle.

Tags: COBBLEMON, PIXELMON_COMPATIBLE, TOWNS, GYMS, NPC_DENSITY, INTERIORS, ENDGAME.

### Wild Kanto / Kosmo Region / Planet Minecraft Cobblemon map catalog

Source: Planet Minecraft Cobblemon tag catalog.
Reference: https://www.planetminecraft.com/projects/tag/cobblemon/

The catalog is a continuing discovery source for custom regions, hubs, landscapes, storage buildings, towns and adventure maps.

Implementation use:

- inspect visual language, scale, terrain blending and regional variety before major builds;
- record only transformed lessons and source links;
- do not import builds unless licensing explicitly allows it and project policy approves the asset.

Tags: DISCOVERY_SOURCE, COBBLEMON, MAPS, BUILDS.

## Pixelmon maps and command-driven regions

### Pixelmon Hoenn

Source: CurseForge public world listing.
Reference: https://www.curseforge.com/minecraft/worlds/pixelmon-hoenn

Public description states that the ORAS-inspired region uses datapacks and tens of thousands of commands, with hundreds of trainers and items, open exploration, Mt. Chimney and ocean trenches.

Implementation lessons:

- Minecraft adventure logic can be deep enough to support an entire Pokémon region;
- commands/datapacks are useful reference technology for triggers, scheduling and state, even though Ouros should prefer typed Java/runtime services where authority matters;
- vertical and environmental extremes create memorable regional traversal;
- gym order can be flexible if progression requirements are encoded explicitly rather than assumed from sequence.

Tags: PIXELMON, HOENN, COMMANDS, DATAPACK, OPEN_WORLD, VERTICALITY.

### Pixelmon Johto

Source: CurseForge public world listing.
Reference: https://www.curseforge.com/minecraft/worlds/pixelmon-johto

Public description reports hundreds of trainers and NPCs, sixteen gyms, route-specific music and story implemented through a very large command/datapack layer.

Implementation lessons:

- route identity can be reinforced by audio, NPCs, encounter ecology and architecture together;
- long progression arcs need durable world flags and replay-safe event handling;
- postgame geography should materially expand the playable world rather than only unlock a menu.

Tags: PIXELMON, JOHTO, COMMANDS, MUSIC, POSTGAME, GYMS.

## Minecraft adventure-map implementation patterns

### Command functions, scoreboards, execute, schedule and GameTest

Source discovery: MinecraftMaps tool catalog and long-running mapmaking practice.
Reference: https://www.minecraftmaps.com/tools

Useful primitive families include execute chains, scoreboards, data/NBT operations, schedule, functions, particles, sounds, triggers and GameTest.

Ouros adaptation:

- use scoreboard-like state only for presentation/debug when typed persistent state is unavailable;
- prefer server-owned Java state for canonical Ouros truth;
- use schedule/timers as inspiration for bounded event queues and recovery-safe delayed actions;
- use GameTest-style automated spatial assertions for structures and interactions where possible;
- command blocks can prototype an event, but production logic should not require hidden command-block spaghetti when a typed runtime is available.

Tags: COMMANDS, FUNCTIONS, SCOREBOARD, SCHEDULE, GAMETEST, EVENT_SYSTEM.

### Custom entity behavior practice

Source: MinecraftCommands community discussions about custom AI.
Reference: https://www.reddit.com/r/MinecraftCommands/comments/ute3an/

Common pattern: disable or constrain vanilla AI and drive selected movement/attacks through explicit state and scheduled steps.

Ouros adaptation:

- a scripted actor must have a server-owned behavior mode;
- pause/resume control should be explicit when authored movement would fight vanilla navigation;
- do not manually simulate PTU combat movement with teleport commands;
- world actors can use authored route following, work routines, feeding, shelter use and scene movement without turning those behaviors into PTU mechanics.

Tags: ENTITY_AI, NO_AI, PATHING, STATE_MACHINE.

## D&D / tabletop dungeon-map lessons

### Loops, multiple paths and vertical connections

Sources:
- D&D Dungeon Master’s Guide mapping guidance, public excerpts.
- Designing Dungeons, chapter on map creation.
- current RPG map design references.

References:
- https://dungeons.hismajestytheworm.games/docs/chapter4/
- https://www.rpgmapeditor.com/guides/dungeon-map-design-basics

Implementation lessons:

- route graphs should contain loops instead of only branch-and-dead-end trees;
- alternate paths need readable clues so choices are informed;
- vertical passages should reconnect floors meaningfully;
- shortcuts discovered from the far side create strong return traversal;
- secret paths are more valuable when they alter navigation rather than only contain loot.

Tags: DND, DUNGEON, LOOPS, VERTICALITY, SHORTCUTS, SECRETS.

### Multi-function rooms

Source: contemporary D&D battle-map design guidance.
Reference: https://1985games.com/blogs/news/how-to-create-immersive-battle-maps-for-dd-campaigns

Reusable principle: a room becomes memorable when architecture, story and gameplay overlap. A library can also be a route puzzle, investigation space and tactical arena.

Ouros adaptation:

- avoid one-purpose gym rooms;
- every important chamber should ideally support at least two of traversal, observation, social interaction, puzzle state, ecology, battle, service, environmental storytelling or later revisit use;
- battle arenas should exist as believable parts of buildings and landscapes.

Tags: DND, MULTIFUNCTION_ROOM, PLAYER_CHOICE, BATTLE_SPACE.

### Functional history before decoration

Source: modern dungeon-map design references.
Reference: https://blacklanternforge.com/blogs/news/the-d-d-dungeon-map-guide-designing-dungeons-that-feel-ancient-dangerous-and-actually-fun

Implementation lessons:

- establish creator, purpose, occupants, later reuse and damage history before dressing rooms;
- route choice becomes meaningful when players can read likely risk/reward from sound, light, tracks, materials, signage or activity;
- environmental storytelling should explain why a space has its current form.

Tags: DND, FUNCTION, HISTORY, ENVIRONMENTAL_STORYTELLING.

## Pokémon gym and challenge design corpus

The existing Narrative repository already contains the conceptual puzzle authority and prior Pokémon Gym research. The implementation corpus adds a Minecraft-facing rule:

A Gym should combine architecture, institutional identity, readable challenge grammar, staff/visitor circulation, Pokémon use, optional observation, battle staging and post-challenge life. It should not be a decorative shell containing a leader at the far wall.

Required reference families for future Gym work:

- official Pokémon Gym puzzles and trials;
- Cobblemon/Pixelmon custom gyms;
- adventure-map dungeon layouts;
- D&D route graphs and spatial puzzles;
- Minecraft redstone/datapack/Java state-machine techniques;
- accessibility and fail-forward patterns already researched in Ouros.

Each Gym implementation should record:

- approach and silhouette;
- public lobby / institutional use;
- challenge route graph;
- trainer/staff circulation;
- maintenance/back-of-house route;
- optional path or secret;
- leader arena transform;
- reset/recovery behavior;
- post-victory/revisit state;
- mechanical dependency classification.

## Player-experience checks

Before calling a location good, walk the intended experience in order:

1. What can the player see before reaching it?
2. What makes them curious enough to deviate from the shortest path?
3. How do they know where entry is without floating instructions?
4. What choices appear in the first minute inside?
5. What changes in scale, elevation, sound or lighting before fatigue sets in?
6. What can they discover that is not mandatory?
7. What does a Pokémon do there that could not happen in a generic empty room?
8. What does an NPC do there when the player is not advancing a quest?
9. What physical evidence tells the player what this place is for?
10. What remembers their visit after they leave?
11. What changes when they return later?
12. Could two players interact with it without corrupting progression?

If those questions do not have strong answers, keep the asset at GAMEPLAY PROTOTYPE status.

## Current implementation priorities produced by this corpus

Priority A: reusable authored-build primitives.

- bounded volume builder;
- material palettes;
- room/wing composition helpers;
- route graph metadata;
- vertical connection helpers;
- hidden/conditional passages;
- persistent interaction anchors;
- actor activity zones;
- structure quality metrics.

Priority B: first large Gym prototype.

The target should be large enough to evaluate actual composition and traversal, but still marked GAMEPLAY PROTOTYPE until screenshots and playtest evidence pass the signature-structure gate.

Priority C: world-event primitives.

- schedule windows;
- crowd or wildlife rerouting;
- NPC work loops;
- persistent Pokémon home/use points;
- block-state transformations driven by world state;
- one-time and recurring events;
- reset and restart recovery.

Priority D: scalable map pipeline.

Long-term showcase assets should migrate from thousands of direct `setBlockState` calls toward structure templates, modular pieces, jigsaw/template pools or another authored asset pipeline. Procedural Java remains appropriate for terrain adaptation, connective tissue and stateful transformations.

## Continuous-ingestion rule

Future research runs should append high-value map, dungeon, Gym, entity-behavior, command/datapack, worldgen and RPG references to this corpus when they produce a new implementation lesson. Do not add links only for volume. Every entry needs an explicit reason it changes Ouros implementation or player experience.
