# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokemon variants.

## Mandatory model-source rule

For any Pokemon that already exists in Cobblemon, Ouros MUST begin from the exact original model distributed by the latest stable Cobblemon release used by this project. Do not rebuild anatomy. Do not substitute an older mirror. Do not approximate the model with generic cubes.

Current production target:
- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3
- source of truth: the released Cobblemon 1.7.3 Fabric JAR published on Modrinth, version id `kF7CvxTo`

A skin implementation may only change or append deliberate cosmetic geometry. Every original bone, cube, pivot, locator, hierarchy relationship and animation-facing name must remain unchanged unless a design explicitly requires a local modification and that exception is documented.

## Visual validation rule

The old in-repo Python software renderer is rejected as acceptance evidence.

New assets must be validated through an independent Minecraft model viewer. Blockbench is the primary review viewer because it supports Minecraft Bedrock Entity geometry, bone rotations, per-face UV, model animations and MoLang. The CI review path must load the generated `.geo.json` in Blockbench rather than reimplementing Bedrock transforms in project code.

A preview is evidence only when:
- the exact production `.geo.json` is loaded;
- the exact release texture is applied;
- any accessory texture used in production is applied;
- the model imports without geometry errors;
- the original species silhouette matches the unmodified release model;
- screenshots come from the external viewer, not from concept art or a project-authored geometry renderer.

## 0025 Pikachu — Storm Courier

Status: RESET / NOT IMPLEMENTED

The previous Storm Courier implementation was removed from the clean v2 branch. It used an obsolete Pikachu base and produced misleading review evidence. It must not be restored or used as a template.

If Storm Courier is rebuilt, it must start by extracting the current `pikachu_male.geo.json`, Pikachu textures and Pikachu animation files directly from the official Cobblemon 1.7.3 release JAR. Accessories may then be added on top of that exact model. The first acceptance image must show the untouched release Pikachu and the edited Pikachu side-by-side in the same independent viewer so anatomy drift is immediately visible.

## Authority boundary

All skin/model work is presentation-only. Cobblemon/Minecraft model, animation and rendering systems may be reused. Cobblemon battle state, participants, legality, HP/status, positions and combat authority remain outside this workflow; Ouros/AutoPTU remain authoritative for tactical battle facts.
