# Ouros Build Quality Bar

Status: REQUIRED IMPLEMENTATION STANDARD
Scope: Minecraft/Cobblemon overworld builds, dungeons, settlements, facilities, landmarks and authored environmental spaces.

## Purpose

Ouros must not call a tiny block arrangement a finished structure. Small code-generated test spaces are allowed, but they are prototypes unless they satisfy the relevant quality gate below.

The goal is a world whose physical spaces can stand beside strong Minecraft adventure maps and modern structure/worldgen mods while still serving Ouros narrative systems and PTU/Cobblemon gameplay.

Reference quality patterns inspected include large handcrafted multi-floor structures, hidden rooms, biome-aware placement and layered traversal in When Dungeons Arise, plus configurable multi-room dungeon redesign and worldgen compatibility in YUNG's Better Dungeons. These are reference classes, not content to copy.

## Naming discipline

Use these labels consistently.

MICRO TEST
A minimal area used to prove one technical primitive. It may be ugly. It is never presented as a finished Ouros location.

GAMEPLAY PROTOTYPE
A functional area that proves traversal, spawning, behavior, interaction or battle handoff. It can use placeholder geometry.

AUTHORED LOCATION
A complete location with visual composition, gameplay purpose, environmental integration and repeat-visit value.

SIGNATURE STRUCTURE
A major landmark, dungeon, institution or settlement that should be memorable from silhouette, approach, interior circulation and narrative function.

REGIONAL SET PIECE
A large-scale build or generated district that helps define the identity of a region and supports multiple systems, stories and return visits.

The Cedar Meadow slice is a GAMEPLAY PROTOTYPE. It is not evidence that Ouros has reached authored-location quality.

## Minimum authored-location gate

A location cannot be called finished unless it satisfies all applicable requirements.

### 1. Scale and silhouette

The space must have a readable massing or landscape silhouette from approach distance.

A building should not read as a rectangular shell with detail pasted onto it.

Large locations should use multiple volumes, elevation changes, setbacks, towers, roofs, courtyards, retaining walls, terraces, cliffs, bridges, vegetation masses or other composition devices appropriate to the concept.

The player should be able to recognize an important location from at least one distant approach.

### 2. Exterior composition

Use a deliberate material palette with primary, secondary and accent materials.

Avoid uniform walls and repeated flat facades.

Depth must come from structural logic such as columns, beams, frames, buttresses, balconies, eaves, windows, foundations, machinery, rock layers or vegetation.

Entries must read clearly without relying only on floating text.

Service areas, drainage, loading, waste handling, utilities, paths and rear access should exist where the fiction requires them.

### 3. Interior architecture

Important buildings need interiors designed as spaces rather than boxes behind a facade.

Rooms need purpose, circulation and hierarchy.

Where appropriate include vertical movement, sightline reveals, service corridors, maintenance access, storage, staff-only areas, public areas, transition spaces and alternate circulation.

Signature structures should normally include more than one meaningful floor, wing, chamber sequence or elevation band unless the concept specifically demands otherwise.

### 4. Traversal and discovery

The player should make navigation decisions.

Use loops, overlooks, shortcuts, side paths, optional rooms, locked or conditional access, vertical connections and landmark-based orientation when appropriate.

A dungeon cannot be only a corridor with combat rooms.

A settlement cannot be only a decorative street with inaccessible facades.

A natural site cannot be only terrain decoration with nothing to read, observe or interact with.

### 5. Environmental integration

Terrain must meet the structure intentionally.

Avoid obvious platform cuts, floating foundations, buried doors and pasted-on roads.

Adapt foundations, retaining walls, drainage, vegetation, erosion, paths and approach routes to the local terrain.

Generation must support biome/location constraints and reject unsuitable placements when the concept needs them.

### 6. Environmental storytelling

Physical evidence should communicate history without requiring exposition.

Examples include repairs, abandoned utilities, old foundations, changed routes, faded signage, reused rooms, habitat occupation, maintenance access, patched roofs, former industrial traces, memorial objects and institutional layers.

These details must come from Ouros state or authored history rather than random clutter.

### 7. Gameplay systems

A location should support at least two meaningful gameplay or world systems beyond walking through it when the concept allows.

Examples include persistent Pokemon use, NPC schedules, observation, puzzles, services, access permissions, weather response, resource logistics, research, transport, institutional procedure, battle staging, changing routes or Chronicle callbacks.

Signature locations should usually support several systems and remain useful after their first quest.

### 8. Pokemon ecology

Pokemon placement must reflect authored habitat, time, access, shelter, feeding, social, territorial, nesting, migration or human-associated logic.

Do not scatter species as decoration.

Do not equate loaded entities with population truth.

Important persistent Pokemon should have stable identity and repeatable world relationships where relevant.

### 9. Behavior and movement

Ambient actors need a purpose for being where they are.

Behavior can include patrol, work, feeding, shelter use, observation, route following, opening hours, gathering, avoidance, seasonal presence or context-dependent response.

Vanilla wandering is acceptable only where true random wandering is the intended behavior.

### 10. Interaction density

Major spaces need meaningful points of interaction distributed through the build.

Interaction can be visual, informational, mechanical, social, ecological or tactical.

Do not fill a build with buttons merely to increase count.

### 11. Secrets and optional content

Signature dungeons and landmarks should include optional discoveries when appropriate.

Possible forms include hidden rooms, alternate entries, maintenance spaces, overlooks, old routes, archive fragments, observation points, optional encounters, environmental clues or later-unlocked shortcuts.

Secrets should have spatial or narrative logic.

### 12. Repeat-visit state

Important locations should anticipate return visits.

Possible changes include repaired damage, new staff, different Pokemon use, seasonal variation, changed access, reopened services, new notices, vegetation growth, construction stages, institutional succession, archived battle consequences or altered routes.

A completed quest should not automatically turn a major location into dead scenery.

### 13. Performance and generation safety

Large builds need explicit bounds, placement rules and generation budgets.

Do not perform massive uncontrolled per-tick block edits.

Prefer templates, structure pieces, staged generation, cached geometry or bounded construction jobs as scale increases.

Persistent logic must continue to work when chunks unload.

### 14. Mechanical authority

Minecraft blocks and entity AI present Ouros and AutoPTU truth. They do not invent PTU rules.

A trap-looking floor does not apply damage until an authoritative mechanic says so.

A narrow bridge does not create forced movement by itself.

A Pokemon standing in tall grass does not receive an Accuracy or Evasion bonus unless supported by PTU/Caelo and the engine.

## Signature-structure bar

A structure intended to impress the player must additionally pass a stronger review.

It needs a memorable approach sequence.

It needs at least three visually distinct spatial beats before the primary destination or climax.

It needs at least one strong vertical or sectional idea.

It needs a reason for its architecture or landscape form to exist in the world.

It needs multiple authored viewpoints where the composition reads well.

It needs a functional interior or playable landscape rather than a facade-only shell.

It needs optional content or spatial depth beyond the critical path.

It needs a post-completion state or repeat-visit purpose unless canon explicitly makes it disposable.

It needs screenshots or automated spatial evidence before being described as showcase-ready.

## Regional set-piece bar

Regional set pieces should operate at district, valley, complex, large ruin, major station, industrial facility, port, campus, forest system or equivalent scale.

They should combine terrain shaping, structures, routes, environmental systems and actor behavior.

They should contain several authored sublocations rather than one oversized shell.

They should support multiple stories without resetting to a pristine state after each one.

They should have a coherent material and landscape language that makes the region recognizable.

## Reference-driven workflow

Before making a signature structure, inspect several strong public Minecraft references from different sources.

Extract high-level lessons such as silhouette, scale, room sequencing, structural detailing, terrain blending, lighting, landmarking, secret placement and traversal rhythm.

Do not copy a protected build block-for-block.

Record references and the transformed lessons in implementation notes.

At least one reference should be a high-quality adventure/worldgen structure rather than only a decorative build.

At least one reference should inform functional Minecraft implementation such as structure templates, jigsaw pieces, processors, datapack placement, terrain adaptation or runtime performance.

## Acceptance evidence

A finished authored location should eventually provide evidence covering:

build footprint and height range;
material palette;
approach screenshots;
interior screenshots;
traversal graph or route description;
interactive points;
spawn/behavior zones;
persistence behavior;
chunk unload/reload behavior;
multiplayer behavior when applicable;
performance observations;
worldgen placement constraints;
mechanical dependency classification;
known placeholders or blocked features.

## Hard rejection conditions

Do not approve a supposed showcase structure when any of these are true:

it is primarily a flat box;
it has a decorative facade but no meaningful interior;
it is only a few dozen blocks and is being marketed as a major landmark;
it has no terrain integration;
it repeats one room or corridor without authored variation;
it places Pokemon randomly only to make the area look populated;
it depends on floating labels to explain every space;
it has no purpose after one scripted interaction;
it copies an online build without meaningful transformation and appropriate licensing;
it uses unsupported Minecraft behavior as hidden PTU mechanics;
it cannot recover from chunk unload or server restart where persistence is required.

## Immediate implementation consequence

The next Ouros construction slices should separate reusable primitives from showcase locations.

Small code tests may continue for block placement, spawn binding, schedules, pathing, structure loading and world-state mutation. They must be labelled MICRO TEST or GAMEPLAY PROTOTYPE.

When the project begins its first SIGNATURE STRUCTURE, the work should use a proper authored build pipeline with reference study, large geometry or structure-template support, terrain integration, interiors, traversal, behavior zones and repeat-visit state. It should be evaluated visually before it is called finished.

## Mandatory build-doctrine linkage

`docs/ouros-build-doctrine.md` is mandatory for AUTHORED LOCATION, SIGNATURE STRUCTURE and REGIONAL SET PIECE work.

When this quality bar and the build doctrine overlap, apply the stricter requirement.

In particular, signature work must not inherit an arbitrary project-specific height cap. The exact capture envelope must expand to include the complete authored structure, subject only to Minecraft world limits, placement safety and performance.

SIGNATURE STRUCTURE and REGIONAL SET PIECE approval requires the applicable items in the doctrine's mandatory pre-approval checklist to be satisfied before the work is described as finished or showcase-ready.

Structural CI proves that geometry is legal and connected. It does not prove architectural quality. Browser review must still reject exposed scaffolding silhouettes, weak enclosure, under-detailed surfaces, visually unsupported masses, simplistic arenas, procedural-looking vegetation or other doctrine failures even when automated checks are green.
