# Cobblemon same-species reference eligibility

This document is the authoritative eligibility contract for the mandatory three-reference gate. It corrects the earlier broad wording `skin/model`.

## What counts

A counted reference must be an external **custom skin/costume/themed variant of the exact same base species** whose authored visual identity materially changes the model geometry. It must be something a player would recognize as a different skin of that Pokemon, not merely a canonical form or recolor.

A mandatory reference must also come from a **real Cobblemon implementation ecosystem**: resource pack, datapack/resource-pack pair, modpack asset pack, server resource pack or equivalent package that actually targets Cobblemon. Standalone marketplace sculpts, generic 3D models, Sketchfab/Meshy-only uploads and other viewers can be useful secondary visual study, but they do not satisfy the mandatory three-reference gate by themselves.

To count, every reference must satisfy all of these conditions:

- `referenceClass: CUSTOM_GEOMETRY_SKIN`;
- `canonicalRelation: NON_CANONICAL_CUSTOM_SKIN`;
- `sourceEcosystem: COBBLEMON_PACK`;
- exact same base species as the Ouros target;
- materially different custom geometry, not only texture changes;
- a custom visual identity such as costume, armor, role, culture, themed equipment, non-canonical transformation or similarly authored skin treatment;
- actual model file inspection (`.geo.json`, `.bbmodel` or equivalent pack model format) and actual production texture inspection;
- `assetInspectionStatus: COMPLETE` only after those real files are inspected and hashed;
- at least three concrete geometry/texture/animation lessons;
- license/reuse mode documented separately from artistic usefulness.

A reference may be a custom non-canonical form implemented by a community pack **only if it functions visually as a skin and materially changes geometry**. The fact that a pack calls something a `form` is not sufficient by itself.

## Candidate staging is not reference completion

Internet research will often find a promising costume before its source files can be inspected. Keep those discoveries under a dossier's `candidateReferences` array. **Only `references` entries are counted by the hard gate.**

A candidate intended for the mandatory gate must declare `sourceEcosystemCandidate: COBBLEMON_PACK`. A generic standalone 3D upload can be tracked as secondary inspiration elsewhere, but it is not a candidate for one of the three mandatory Cobblemon-pack references.

A candidate can record a public project page, changelog, server skin listing or interactive 3D viewer, but it stays uncounted until the actual MODEL and TEXTURE files are lawfully accessible, inspected and SHA-256 hashed.

An interactive 3D viewer is useful discovery evidence because it can prove that an authored 3D presentation exists. It is still **not** `assetInspectionStatus: COMPLETE` when the underlying model and texture files from a Cobblemon pack cannot be obtained for direct inspection. Triangle/vertex counts, screenshots, thumbnails and viewer metadata do not substitute for the source model and texture.

Recommended candidate fields:

- `candidateId`
- `species`
- `candidateClass: CUSTOM_GEOMETRY_SKIN_CANDIDATE`
- `canonicalRelationCandidate: NON_CANONICAL_CUSTOM_SKIN`
- `sourceEcosystemCandidate: COBBLEMON_PACK`
- `implementationName`
- `project`, `sourceUrl`, `sourceVersion`
- `discoveryEvidence`
- `geometryMateriallyChangedStatus: PROVEN | UNPROVEN`
- `customVisualIdentityStatus: PROVEN | UNPROVEN`
- `assetInspectionStatus: PENDING`
- `whyNotCounted`
- preliminary license/provenance notes

When real MODEL + TEXTURE inspection proves eligibility, move the candidate into `references` and replace discovery-only statements with exact file paths, hashes and concrete lessons.

## What never counts

The following may be useful for other research, but they do **not** satisfy the three-skin gate:

- shiny or alternate shiny textures;
- texture-only recolors, palette swaps or marking swaps;
- official/canonical Mega Evolution;
- official/canonical Gigantamax/Dynamax appearance;
- official regional forms, sex differences, seasonal forms or other canonical Pokemon forms;
- canonical battle transformations;
- a community remodel whose purpose is only to replace/improve the ordinary canonical base model without creating a distinct skin identity;
- the untouched official Cobblemon model;
- standalone generic 3D/marketplace/viewer models that are not actual Cobblemon-pack implementations;
- three screenshots, renders or gallery images;
- three revisions of one skin;
- one skin repackaged by several modpacks;
- a model for a different species, evolution or pre-evolution.

Examples: Mega Gengar is not a Gengar skin reference. A better Gengar shiny is not a Gengar skin reference. A normal-Gengar accuracy remodel is not a Gengar skin reference. A custom `Overtime Gengar` costume distributed in a real Cobblemon pack with materially different geometry can count after its real model and texture files are inspected.

## Required dossier fields

Each counted reference records:

- `referenceClass: CUSTOM_GEOMETRY_SKIN`
- `canonicalRelation: NON_CANONICAL_CUSTOM_SKIN`
- `sourceEcosystem: COBBLEMON_PACK`
- `geometryMateriallyChanged: true`
- `customVisualIdentity: true`
- `implementationName`
- `project`, `sourceUrl`, `sourceVersion`
- `assetInspectionStatus: COMPLETE`
- inspected MODEL and TEXTURE entries with SHA-256
- license and `reuseMode`
- `techniqueLessons`
- `distinctiveElementsNotToCopy`

`validate_species_reference_dossier.py` enforces these fields. If fewer than three eligible references are complete, production remains `REFERENCE BLOCKED`.

## Reuse and donor rule

Eligibility as a quality reference does not grant reuse rights. `ARR`, unknown, no-license or unclear custom licenses remain `STUDY_ONLY`. A counted skin may become `LICENSED_DERIVATIVE_DONOR` only when its explicit license permits both derivatives and redistribution under compatible terms.

Even for a licensed donor, Ouros production must preserve the exact current official Cobblemon biological model contract required by the project. Donor assets can inform or seed cosmetic construction; they do not replace the official biological source of truth.
