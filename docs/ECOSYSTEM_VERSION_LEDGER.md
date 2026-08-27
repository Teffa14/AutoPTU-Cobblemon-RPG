# Ecosystem version ledger

This file records the versions actually inspected for the living Minecraft/Cobblemon/AutoPTU knowledge program.

A value here is evidence of the last review, not permission to assume it is still current. Every implementation slice that depends on one of these projects must re-check the official project page, changelog and dependency manifest before using the information.

Last broad ecosystem review: **2026-08-28**.

| Source | Review tier | Version observed | Minecraft | Loader | Role in Ouros | Re-check rule |
|---|---|---:|---|---|---|---|
| Minecraft Java Edition | A | 1.21.1 project target | 1.21.1 | server/Fabric target | Vanilla crafting, stations, item/container semantics and world interactions | Verify target has not changed before every dependency/recipe slice |
| Cobblemon | A | 1.7.3 project target | 1.21.1 | Fabric | Pokémon presentation/world content, items, blocks, recipes, UI/networking hooks; never battle-state authority | Verify latest stable and exact project pin before every Cobblemon-facing slice |
| Cobblemon Official Modpack [Fabric] | A | 1.7.3 latest release observed | 1.21.1 | Fabric | Official baseline for supported Cobblemon ecosystem composition and QoL | Check official files/manifest before every corpus refresh |
| COBBLEVERSE - Pokemon Adventure [Cobblemon] | A | 1.7.3 latest release observed | 1.21.1 | Fabric | Primary large-pack reference for RPG UX, structures, QoL, storage, cooking/furniture integrations, world content and companion-mod composition | Check Modrinth version + full dependency manifest before every relevant implementation |
| Cobblemon Delta | A | 2.1.0 latest release observed | 1.21.1 | Fabric | Multiplayer/economy/world-service reference: raids, dungeons, shops/trading/daycare-style systems and broader RPG loops | Check CurseForge files and relations before every relevant implementation |
| Cobblemon Academy 2.0 | A | 2.6.0 latest release observed | 1.21.1 | Fabric | Structured progression, adventure and curated QoL reference | Check CurseForge files/relations before every relevant implementation |
| AutoPTU | A | current main must be inspected per run | n/a | Python reference/oracle | PTU source/reference datasets, Trainer classes, Features, Edges, items and oracle behavior | Record exact SHA in implementation work reports |
| AutoPTU-Java | A | current main must be inspected per run | n/a | Java | Authoritative PTU engine contracts | Record exact SHA and capability state in implementation work reports |
| Cobblemon Expanded | B | 0.2 observed on 1.20.1 | 1.20.1 | Fabric | Historical Vanilla+/Create/storage/QoL pattern reference only | Never treat as target-compatible without a fresh compatibility check |
| Cobblemon Expanded (by Jonathan) | B | project actively referenced; published builds vary by host | 1.20.1/1.21.1 listings observed | Fabric | Towns, riding, gyms, Pokémon-game feature restoration and QoL pattern reference | Re-check exact latest file before citing or adapting a mechanic |
| Creation and Cobblemon | B | latest release observed 2026-05-31 | 1.21.1 | NeoForge | Technology/Create automation pattern reference only | Pattern study only unless an equivalent Fabric-compatible component is separately verified |

## Verified source notes from the 2026-08-28 refresh

- COBBLEVERSE advertises Minecraft 1.21.1, Fabric and Cobblemon 1.7.3. Its current 1.7.3 release page exposes a large dependency manifest. Future research should inspect that manifest component-by-component instead of treating the pack name as one feature source.
- Cobblemon Delta 2.1.0 is the current 1.21.1 Fabric release observed on CurseForge.
- Cobblemon Academy 2.0 2.6.0 is the current 1.21.1 Fabric release observed on CurseForge.
- Cobblemon Official Modpack [Fabric] 1.7.3 is the current 1.21.1 Fabric release observed on CurseForge/Modrinth.
- Older or non-Fabric packs remain useful for design patterns but are not implementation dependencies by default.

## Required per-mod dependency ledger

Large modpacks are aggregates. When a feature is selected for correlation, add the actual component mod to this file or a linked feature matrix with:

- mod name;
- exact version;
- source pack and pack version where observed;
- Minecraft version;
- loader;
- license/source availability when relevant;
- feature used as reference;
- whether Ouros will reuse the dependency, adapt the concept, or implement its own equivalent;
- last checked date.

Do not write “Cobbleverse does X” when the behavior actually comes from a named dependency and that dependency can be identified. Track the real source.
