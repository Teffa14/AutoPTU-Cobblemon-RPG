# Ouros Implementation Toolkit Research

Status: ACTIVE IMPLEMENTATION REFERENCE
Date: 2026-08-26

This note records implementation tooling that can accelerate Ouros without weakening licensing, Minecraft compatibility or PTU authority.

## Agent skills inspected

### minecraft-modding-workbench

Source: `adhi-jp/agent-skills`, skill `minecraft-modding-workbench`.

Useful practices adopted:

- inspect the actual Fabric workspace and dependency versions before coding;
- verify version-sensitive APIs instead of guessing mappings or symbols;
- deliver runnable feature slices rather than generic advice;
- treat build success as necessary but insufficient for worldgen/resources/runtime behavior;
- use GameTest or equivalent runtime assertions for spatial/resource-heavy work;
- use NBT/structure resources when authored content is more appropriate than generated Java rectangles;
- inspect dependency APIs and existing vanilla patterns before introducing Mixins or custom systems.

The skill expects a dedicated `minecraft-modding` MCP server. That server is not available in the current ChatGPT environment, so no implementation claim may be labelled MCP-verified. Workspace source, dependency metadata and GitHub Actions remain the current verification sources.

### minecraft-ci-release

Source: `Jahrome907/minecraft-agent-skills`, skill `minecraft-ci-release`.

Useful for CI, artifact and release discipline. It does not replace gameplay/world implementation skills. Ouros already has strong runtime smoke, exact build export and browser review jobs, so its main lesson is to keep build/release evidence explicit while implementation work continues.

## Candidate runtime/worldgen dependencies

A dependency is adopted only when a concrete task benefits from it. Popularity alone is not a reason to expand the modpack.

### Lithostitched

Compatibility lane: Fabric 1.21.1.
License lane: MIT.
Potential use: data-driven worldgen conditions, structure/worldgen modifiers, biome injection and terrain-aware placement.

Ouros fit: strong candidate for OI-003/OI-004 if vanilla structure placement becomes too rigid. Prefer vanilla APIs first, then add Lithostitched when it removes a measured limitation.

### Structure Pool API

Compatibility lane: Fabric 1.21.1 releases exist.
License lane: MIT.
Potential use: injecting authored structures into template pools.

Ouros fit: candidate for modular districts or village-adjacent content. It should not replace Ouros-owned topology/state authority.

### Repurposed Structures

Compatibility lane: Fabric 1.21.1.
License lane: LGPL-3.0.
Potential use: code reference for large server-side vanilla-block structure sets, biome variants and structure configuration.

Ouros fit: valuable implementation reference. Any copied/modified source must obey LGPL. Original Ouros assets and layout logic should remain independently authored.

### YUNG's API

Compatibility lane: Fabric 1.21.1 releases exist.
License lane: LGPL-3.0.
Potential use: worldgen support if a concrete YUNG-compatible system is selected.

Ouros fit: optional. Do not add until a task needs its API.

### Litematica

Compatibility lane: Fabric 1.21.1.
License lane: LGPL-3.0.
Potential use: developer-side schematic authoring and visual iteration.

Ouros fit: strong authoring reference/tool. It must not become runtime authority or a required player mod simply to load Ouros locations.

## Candidate decorative/content dependencies

### Beautify: Refabricated

Compatibility lane: Fabric 1.21.1.
License lane: MIT.
Potential use: vanilla-friendly decorative blocks and models.

Ouros fit: strong candidate after OI-005 teaches the exact browser viewer to extract/render approved modded block models and textures. It must not be introduced into signature builds before browser parity exists.

### Decorative Blocks Reborn

Compatibility lane: Fabric 1.21.1.
License lane: MIT.
Potential use: additional architectural/decorative shapes.

Ouros fit: same dependency gate as Beautify. Add only when a design needs its shapes and the runtime/viewer can support them end to end.

## Reference-only structure projects

These can teach scale, composition, traversal, biome integration and detail density. Their assets are not imported.

### When Dungeons Arise

Use: scale, silhouette, multi-floor exploration, landmark composition.
Distribution/license lane: All Rights Reserved for current public distribution.
Decision: reference only. No structure/assets copied.

### Dungeons and Taverns

Use: adventure-density and vanilla-compatible environmental structures.
Distribution/license lane: All Rights Reserved.
Decision: reference only.

### ChoiceTheorem's Overhauled Village

Use: settlement hierarchy, village composition and biome adaptation.
Inspected license metadata: CC BY-NC-ND 4.0 for the relevant source line.
Decision: reference only for transformed lessons. No modified asset redistribution.

### Structory / Terralith family

Use: exploration density, terrain/structure integration and recognizable regional language.
License lane: custom Stardust terms restrict copying portions into another project without permission.
Decision: reference only unless explicit permission is obtained.

### Integrated Dungeons and Structures

Use: interior/detail density and complex encounter spaces.
Current relevant loader lane: NeoForge on 1.21.1.
Distribution lane: All Rights Reserved.
Decision: reference only.

### Structure Gel API

Use: jigsaw/structure authoring concepts.
Current relevant loader lane: Forge/NeoForge rather than our Fabric 1.21.1 target.
Decision: not selected as a runtime dependency.

## Dependency gate for Ouros

Before adding any worldgen/decor mod to production:

1. confirm Fabric 1.21.1 + Java 21 compatibility;
2. record license and redistribution obligations;
3. add it to production smoke and dedicated-server startup;
4. verify client requirement and multiplayer compatibility;
5. verify exact build export can read the resulting BlockStates;
6. extend the browser asset pipeline to its blockstates/models/textures if visual review needs those blocks;
7. confirm the mod does not become a hidden PTU rules engine;
8. prove a concrete Ouros task becomes materially better because of it.

## Immediate implementation decision

OI-001 uses only vanilla Minecraft blocks and project-owned Java first. This keeps the first geometry toolkit easy to test and lets the exact viewer judge the result immediately.

OI-002 moves detailed authored content into Minecraft structure templates/NBT.

OI-003 evaluates vanilla jigsaw/template pools first, with Lithostitched and Structure Pool API as concrete fallback candidates.

OI-005 is the gate before approved decorative mods appear in showcase builds.
