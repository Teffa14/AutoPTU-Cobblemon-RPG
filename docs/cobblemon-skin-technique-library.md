# Cobblemon Skin Technique Library

This library is an internal training/reference surface for Ouros-authored Cobblemon cosmetics. It exists to improve modeling and texture technique. It is not a catalog of third-party assets to copy.

## Why this exists

Previous Ouros passes proved source provenance, bone preservation, attachment and CI, but repeatedly failed the visual bar. The recurring failure was procedural construction: large rectangles, cages, frames, bars and accessory stacks around an otherwise unchanged Pokemon. The corrective workflow studies how strong community packs solve shape language, wrapping, layering, texture depth and material separation, then reimplements those *general techniques* in original Ouros work.

## External study sources

### Cobbleverse resource pack

Public repository inspected: `ALERder/cobbleverse-resourcepack`.

The repository exposes a packaged resource pack but no explicit redistribution license was found in the repository root during this audit. Treat the actual third-party model and texture files as **study-only**. Do not copy them into this repository and do not derive Ouros geometry or textures from their bytes.

Permitted use:
- inspect bone grouping and attachment strategies;
- study how silhouette changes are distributed across the whole character;
- study geometry density and where complexity is spent;
- study contour, wrap, taper, overlap and negative-space techniques;
- study texture value ramps, edge accents, painted occlusion and material breakup at a generic technique level.

Forbidden use:
- copying geometry, UVs, textures, palettes, logos, markings or distinctive costume motifs;
- tracing a model or recreating a third-party silhouette one-to-one;
- redistributing the packaged resource pack without permission.

### Cobblemon Delta

Public Modrinth/CurseForge project inspected: `Cobblemon Delta`.

The project is distributed as **All Rights Reserved**. Its files are therefore **study-only** for this workflow. Do not commit, redistribute, extract into production, or use any Delta model/texture as an Ouros source asset.

Permitted use is limited to high-level technique study: geometry organization, surface hierarchy, texture depth, material readability, large-to-small detail distribution and how a custom Pokemon remains legible at gameplay scale.

### Official Cobblemon

Official Cobblemon remains the only authoritative source for Pokemon anatomy, original bones, UV layout, forms, poser/resolver behavior and baseline textures. Every production skin starts from the exact current compatible official JAR.

## Internal original technique exemplars

The files under `tools/cobblemon-model-review/reference-techniques/` are **original Ouros-authored neutral mannequins**, not Pokemon and not copies of third-party models. They exist so builders can open concrete `.geo.json` + texture examples in Blockbench and reuse the *technique*, not the specific shape.

### `contour_wrap_reference`

Files:
- `contour_wrap_reference.geo.json`
- `contour_wrap_reference.png`

Teaches:
- armor/equipment that follows a torso contour rather than enclosing it in a box;
- multiple rotated/tapered plates that overlap into one readable mass;
- collar pieces that bridge into the body instead of hovering;
- texture ramps with dark core, lighter facing planes, edge accents and restrained microvariation.

Use this reference when a builder starts producing flat shoulder slabs or disconnected armor bars.

### `layered_asymmetry_reference`

Files:
- `layered_asymmetry_reference.geo.json`
- `layered_asymmetry_reference.png`

Teaches:
- one stable central mass plus one deliberate asymmetric signature system;
- stepped/rotated geometry with changing size instead of repeated identical bars;
- controlled negative space;
- lower-body continuation so the skin does not become head/back-only;
- material/value hierarchy that remains readable without depending on tiny details.

Use this reference when a design is becoming a symmetrical cage, portal frame or scaffold.

## Texture technique contract

Recoloring and repainting can be a legitimate part of a premium Ouros skin. It must be a **derived texture workflow**, not a flat hue shift.

Required production behavior:

1. Start from the exact official normal/shiny/form texture extracted from the pinned compatible Cobblemon JAR.
2. Preserve canvas dimensions and every original UV mapping.
3. Preserve alpha/transparency semantics unless a separate runtime contract explicitly proves a safe exception.
4. Process normal, shiny and distinct official forms independently.
5. Record the official baseline SHA-256 and the derived texture SHA-256.
6. Record what anatomical/material regions were intentionally repainted and why.
7. Use painted value structure: local shadows/occlusion, lit planes, controlled edge accents, palette variation and material-specific breakup.
8. Do not use a one-command hue rotation, flat fill or uniform multiply as the final texture treatment.
9. Do not copy third-party textures, palette layouts, markings or distinctive motifs.
10. Validate source/dimensions/alpha with `validate_derived_texture.py`.
11. Treat the validator as compatibility only. The owner still judges paint quality from real Blockbench evidence.

A strong derived texture should make geometry and anatomy read together. It may recolor selected biological regions when the concept needs it, but it must preserve species recognition and avoid painting over important facial/anatomical landmarks into visual noise.

## Builder workflow

Before modeling a production skin:

1. Open the official current model and texture in Blockbench.
2. Open both internal technique exemplars.
3. Identify the active model's three largest silhouette problems.
4. Sketch the solution in geometry terms: contour/wrap, overlap, taper, asymmetry, negative space, lower-body continuation.
5. Decide which parts require geometry and which require paint.
6. Build the largest connected mass first.
7. Add the signature piece second.
8. Add texture depth and material breakup before micro-detail.
9. Test front/side/back/three-quarter and gameplay scale.
10. If the result reads as scaffold/box armor, discard the macro-form and rebuild it. Do not hide the problem with more detail.

## Acceptance

These reference models are teaching assets, not an artistic quality ceiling. A production Pokemon must exceed them substantially in authorship, species-specific form language, texture finish and integration.

No external study source can grant acceptance. No internal reference model can grant acceptance. Current production Blockbench PNGs still require explicit owner approval.
