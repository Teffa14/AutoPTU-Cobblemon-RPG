# Ouros Browser Build Review

Status: REQUIRED REVIEW TOOLING

The purpose of this directory is to let a reviewer inspect important Ouros Minecraft spaces without launching Minecraft.

## Current preview

`preview/index.html` contains the first interactive browser review for Meridian Canopy Gym.

It supports orbit, zoom, pan, approach/atrium/arena/service camera presets, a vertical cut control and visibility toggles for roof, landscape and underground service geometry.

The first preview is an architectural/massing representation of the current Java builder. It is not yet an exact block-textured `.schem` render. The distinction must remain visible until export parity exists.

## Required deliverable for future important builds

Every AUTHORED LOCATION, SIGNATURE STRUCTURE or REGIONAL SET PIECE must eventually provide:

- a browser 3D review page;
- at least one approach view;
- at least one interior or sectional view;
- footprint, height and current quality classification;
- traversal and optional-route information;
- a visible list of placeholders or parity gaps;
- an exact `.schem`/`.litematic`/NBT export once the structure pipeline supports it;
- a regional BlueMap view after the build is placed into a reproducible world save.

The preview should be generated from the same structure source as Minecraft. Hand-maintained duplicate geometry is only acceptable as a temporary prototype and must be marked as such.

## Viewer stack researched

### Structure-level review

Craftmatic can parse/write Sponge `.schem`, render exterior/cutaway/floor-plan images, provide a Three.js viewer and export standalone HTML viewers. It is a strong candidate for exact build artifacts once Ouros exports schematic files.

Repository: `tribixbite/craftmatic`

SchemViewer is a fully client-side browser viewer for Litematica and is useful as a second independent inspection surface.

Repository: `LGRY-chan/SchemViewer`

Jopgood's Minecraft Schematic Viewer supports `.schematic`, `.schem` and `.litematic`, layer controls and optimized Three.js rendering for large builds.

Repository: `Jopgood/minecraft-schematic-viewer`

### World-level review

BlueMap reads Minecraft world data and generates interactive 3D browser maps. Once Ouros produces reproducible world saves, it should become the regional inspection surface for settlements, routes, terrain integration and relationships between structures.

Repository: `BlueMap-Minecraft/BlueMap`

## Planned pipeline

The target pipeline is:

`Ouros structure source -> Minecraft placement + exact schematic export -> browser viewer -> static screenshots/cutaways -> regional BlueMap after world placement`

No structure should be called showcase-ready based only on source code review.