# Wild Ecology Architecture

## Permanent product rule

Wild ecology is a world-wide reusable RPG system. It must never be implemented as a Marea-only or Fletchling-only subsystem.

Marea, Cedar Meadow, Fletchling, or any other authored region/species may be used as fixtures, runtime-smoke content, reference populations, or QA scenarios. They are not the production architecture.

All new wild-ecology production work must move toward a generic data-driven runtime that can support every approved WILD population in every region without adding one Java behavior class per species, population, or map.

## Target architecture

The intended production flow is:

`WildPopulationDefinition -> HabitatProfile -> WildBehaviorProfile -> CapabilityProfile -> Spawn/Population Runtime -> Generic Ambient AI -> Encounter Binding`

The implementation may use different concrete type names, but it must preserve those separations of concern.

### WildPopulationDefinition

Defines authored population identity and membership without embedding PTU battle rules in Minecraft. It may describe population size, authored spawn/projection sites, activation policy, migration/seasonal projection, group relationship, and references to server-owned canonical WILD blueprints.

### HabitatProfile

Defines where a population may exist and what Minecraft world conditions are relevant to presentation/ecology. Examples include biome/region, dimension, horizontal leash, elevation, surface/underground/water/air context, time window, weather window, migration stop, and authored points of interest.

Habitat data may constrain presentation and world presence. It must not manufacture PTU species facts, stats, moves, HP, abilities, statuses, legality, RNG, or battle results.

### WildBehaviorProfile

Defines reusable ambient-behavior parameters such as idle/roam cadence, watch/alarm distances, separation, cohesion, solitary/group behavior, territorial presentation where explicitly authored, route lookahead, rest behavior, and environmental point-of-interest preferences.

Behavior profiles are data/configuration consumed by generic runtime code. New species must not require copying or subclassing a region-specific runtime unless genuinely new reusable behavior semantics are being introduced.

### CapabilityProfile

Describes world-presentation locomotion modes and other sourced world capabilities needed by ecology. Examples may include terrestrial, swimming, flying, levitating, burrowing, jumping, climbing, or other supported modes.

Capability-driven world behavior must use authoritative project data. Cobblemon `PokemonEntity` gameplay payload is presentation-only and must not be read as PTU authority.

### Spawn/Population Runtime

Owns activation, reconciliation, replacement, persistence/recovery policy, projection and despawn rules for approved visible wild populations.

It must be reusable across regions and species.

### Generic Ambient AI

Owns presentation locomotion and ambient state such as CALM, WATCHING, ALARMED, RECOVERING, idle, roam, separation, group cohesion, path continuity, obstacle avoidance, yield behavior, points of interest and similar non-combat world behavior.

Minecraft native navigation/pathfinding should be reused where safe.

Ambient AI is not PTU tactical AI. It must not implement PTU initiative, combat movement legality, targeting, damage, move effects or battle decisions.

### Encounter Binding

The visible world actor retains a server-owned canonical encounter identity. An encounter begins from that visible actor and resolves through the existing authoritative reservation/provisioning boundary.

The same canonical identity must survive ordinary roaming, navigation, migration, unload/reload and other presentation changes according to the selected persistence contract.

## Global requirements

The reusable ecology system must eventually support, as data and authoritative source coverage become available:

- all approved species and forms;
- all authored regions and habitats;
- terrestrial, aquatic, aerial, levitating and underground populations where supported;
- solitary actors, pairs, packs, herds, flocks, schools and other authored grouping models;
- species-appropriate actor size and collision geometry;
- population density and distribution;
- spawn, activation, reconciliation and despawn;
- idle and roaming behavior;
- separation and collision avoidance;
- group cohesion where authored;
- native pathfinding and safe fallback steering;
- habitat leashes;
- terrain-aware routing;
- vertical separation;
- environmental points of interest;
- player awareness and alarm/flee presentation;
- migration/seasonal projection where authored;
- persistent or reproducible encounter identity;
- encounter reservation and transition into AutoPTU-Java battle authority.

## No species-specific production architecture

Do not add new production architecture with names or logic shaped like:

- `MareaWild*`
- `FletchlingWild*`
- `if (species == Fletchling)`
- one ambient controller per region;
- one navigation implementation per species.

Existing region/species-specific production code is migration debt. Do not expand that pattern.

When touching that code, prefer extracting reusable interfaces/services/configuration into generic wild-ecology components. Marea-specific data may then become one input profile/fixture to those components.

A species-specific adapter is acceptable only when a sourced mechanic genuinely cannot be represented by an existing reusable behavior/capability primitive. Even then, implement the reusable primitive first when practical.

## Blueprint availability must not block infrastructure

Missing authoritative WILD blueprints may block activation of a specific species/population. They must not block construction of generic ecology infrastructure.

The correct fail-closed behavior is:

1. build the generic population/habitat/behavior/capability runtime;
2. activate only populations whose canonical WILD data is complete and trusted;
3. leave unsupported species unprovisioned rather than fabricating data from Cobblemon;
4. add new species by supplying authoritative data/configuration, not by cloning runtime code.

## WORLD-013 direction

`WORLD-013` must be treated as the extraction/generalization path for global visible-wild ecology, not as an endless Marea polish item.

The existing Marea/Fletchling work is valuable as tested behavior and migration input. Future WORLD-013 work should prioritize converting that behavior into generic reusable components and validating them with multiple profiles/fixtures.

Marea-specific polish that does not advance the reusable architecture is lower priority than genericization.

When the generic runtime is established, Marea should remain only an authored population/profile and QA fixture using the same production services as every other region.

## Validation requirement

A generic wild-ecology feature is not considered generalized because a shared class exists.

Validation must demonstrate reuse across at least two materially different fixtures/profiles when source data permits, for example:

- different regions;
- different population identities;
- different actor sizes;
- different movement modes;
- solitary versus grouped behavior;
- different habitat geometry.

Where authoritative species data is not yet available, use non-gameplay test fixtures that vary habitat/behavior inputs without fabricating PTU facts. Real world activation still requires complete canonical WILD blueprints.

## Authority boundary

AutoPTU-Java remains authoritative for PTU battle legality, tactical movement, targeting, RNG, calculations, damage, statuses, abilities, items, Trainer Features, action economy and battle results.

AutoPTU-Cobblemon-RPG owns server-authoritative persistent/world RPG state and Minecraft projection.

The wild ecology runtime may decide where and how a visible presentation actor idles, roams, navigates, avoids obstacles, reacts to nearby players or uses authored habitat context. It may not decide PTU combat outcomes.

Cobblemon entities remain identity/presentation actors unless server-owned canonical state explicitly provisions RPG data.

## Review rule

For every future wild-ecology PR, ask:

1. Does this behavior work for any qualifying wild population, or only Marea/Fletchling?
2. Is species/region variation represented as data/profile/configuration instead of copied runtime code?
3. Does the change preserve canonical encounter identity?
4. Does it avoid reading Cobblemon gameplay state as authority?
5. Does it keep PTU combat rules in AutoPTU-Java?
6. Does it move existing `MareaWild*` production debt toward generic services rather than expanding it?

If the answer to the first or sixth question is no, the change needs redesign unless there is a documented exceptional mechanic.