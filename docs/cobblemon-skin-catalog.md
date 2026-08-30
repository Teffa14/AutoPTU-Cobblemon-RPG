# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokemon variants and their current technical and artistic acceptance state.

The authoritative art contract is `docs/cobblemon-skin-art-direction.md`. Technical validity alone never establishes artistic acceptance. The production-engineering contract is `docs/cobblemon-skin-professional-pipeline.md`.

## Owner quality decision — 2026-08-30

The owner rejected the current artistic quality of the model set. Therefore there are currently **no artistically accepted Ouros Cobblemon skins** in this catalog.

Previous CI, source-provenance, anatomy, resolver, Blockbench, Playable Test Build and Integration Core results remain useful technical evidence. They do not imply visual acceptance. A model can return to `FULL TRANSFORMATION ACCEPTED` only after a materially improved exact current model is shown through the required Blockbench evidence and the owner explicitly approves that exact evidence set.

Assistant review and green CI are not owner approval. No future model may self-promote from `OWNER REVIEW REQUIRED` to an accepted artistic state.

## Production source target

Current repository target:

- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3 Fabric
- official Modrinth version id `kF7CvxTo`
- official file `Cobblemon-fabric-1.7.3+1.21.1.jar`
- official JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- official JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`

The compatible stable release must be checked again before every skin slice. A reference render from the current release is not enough: the production edited model itself must be generated from the exact `.geo.json` extracted from that pinned official JAR.

For a Pokemon that already exists in Cobblemon, Ouros MUST begin from that exact original model. Do not rebuild anatomy, substitute a mirror, copy a fork, use screenshots as geometry, approximate the Pokemon with replacement cubes or use a repository-stored legacy body as the editable baseline.

All original bones must remain JSON-equivalent and in the same order unless an explicit documented exception is approved. Geometry identifiers may change. New cosmetic bones must be `ouros_*`. Male, female and official forms must be derived independently from their corresponding official files.

## Presentation-only authority boundary

This workflow never gives battle-state authority to Cobblemon or Minecraft. Cobblemon/Minecraft may provide models, textures, animations and rendering/presentation hooks. AutoPTU/Ouros decides combatants, legality, HP/status, tactical positions, RNG, damage and results.

## Visual acceptance — PREMIUM FULL TRANSFORMATION OR REJECTED

Technical validity is necessary but does not make a good skin.

A skin fails artistic review when it still reads as the base Pokemon with small blocks attached to the head, arms or back. It also fails when the dominant read is a procedural scaffold, rectangular cage, generic armor frame, repeated straight bars, boxy slabs or an arbitrary collection of cuboids around the Pokemon.

The required result is a premium, authored full-character transformation at gameplay distance while the official biological model remains intact underneath. Major forms must have intentional shape language, respond to the Pokemon's anatomy, use connected masses and layered depth, and remain coherent from front, three-quarter, side and rear views.

Large silhouette pushes are allowed, but large geometry alone is not evidence of quality. The goal is not to maximize cube count. Solve the character.

## Biological texture and premium paint contract

The exact official normal/shiny/form textures are immutable **source baselines**, not a prohibition against authored painting.

A production skin may choose either an official-identical body texture or a deliberate texture derived from the exact official baseline. Derived repaint/recolor is explicitly allowed when it materially improves the premium design, provided that it follows `docs/cobblemon-skin-art-direction.md` and passes `validate_derived_texture.py`.

A new or re-audited skin must:

- extract the exact relevant official body texture from the pinned current JAR and record its SHA-256;
- preserve canvas dimensions and every original model UV mapping;
- preserve alpha/transparency semantics unless an explicit reviewed runtime requirement documents a safe exception;
- derive normal, shiny and distinct official forms independently from their corresponding exact baselines;
- record production body-texture SHA-256 and whether the body is `OFFICIAL_IDENTICAL` or `DERIVED_FROM_OFFICIAL`;
- for derived bodies, record repaint regions, palette/material intent and provenance metadata;
- keep species-critical facial/anatomical landmarks readable;
- use painted depth/value/material structure rather than a flat hue rotation, flood fill or uniform multiply;
- keep accessory overlays on a separately validated compatible mechanism when overlays are used;
- never copy third-party texture artwork, UV arrangements, palettes, markings or distinctive motifs unless an explicit compatible derivative license permits that exact use.

Premium painting should use local value ramps, occlusion/shadows where forms meet, lighter facing planes, controlled edge highlights, hue/value variation and material-specific breakup for cloth, leather, metal, stone, bone, lacquer, energy or other authored surfaces.

Historical metadata such as `bodyTexelRework: NONE` remains valid for the old asset it describes. It is **not** a global rule for future skins. The newer derived-texture contract supersedes the old byte-identical-only limitation.

## Physical attachment — NO FLOATING PIECES

A valid bone parent does not prove an object is actually attached.

Every large cosmetic system needs a deliberate root/contact mass connecting it to the Pokemon or to another already attached cosmetic mass. Automated gates reject missing parents, cycles, cosmetic parent chains that do not end in an official bone, detached groups and isolated cubes.

The automated bind-pose gate is only the first layer. Real Blockbench evidence must also be inspected in official idle, battle and locomotion states when those states exist. If a banner, fin, halo, mantle, coat panel, greave or other part visibly hovers or detaches during motion, the skin fails even with green CI.

Do not relax attachment thresholds to rescue an existing asset. Fix its geometry/root.

## Blockbench and gameplay-scale gate

The old in-repo Python software renderer is rejected as acceptance evidence. Blockbench remains the primary independent viewer.

The review must load the exact production `.geo.json`, exact official or validated derived body texture, exact accessory overlay when used and official animation file through the Bedrock workflow. Official reference and skin use the same camera, projection, scale, pose and animation frame. Independent auto-fit is not an accepted comparison.

When official equivalents exist, evidence includes `official_reference_three_quarter`, `hero_three_quarter`, `battle_ready_three_quarter`, `walking_three_quarter` and structural front/left/right/back views. Never fabricate a battle or locomotion pose when Cobblemon does not provide an equivalent animation.

Every work pass must expose four clickable current PNGs. When a dedicated official walking clip does not exist, substitute a current structural/gameplay-scale Blockbench view and record the reason.

Every candidate also needs a real gameplay-scale sample with the Pokemon approximately 128–192 px tall. Reject the skin if the concept disappears at that size, signature pieces collapse into noise, the first read becomes the untouched base Pokemon again, or the silhouette becomes generic rectangular hardware.

## Status meanings

`TECHNICALLY VALID` means official-source provenance, anatomy preservation and recorded integration/build gates were satisfied. It makes no artistic claim.

`ART RE-AUDIT REQUIRED` means a historical pass has not yet been judged under the current source/attachment/premium-shape-language contract.

`USER REJECTED — REWORK REQUIRED` means the owner has explicitly rejected the current visual result. CI and assistant review cannot override this status.

`OWNER REVIEW REQUIRED` means internal technical and visual-preflight gates may be green, but the exact current Blockbench evidence has not yet received explicit owner approval.

`FULL TRANSFORMATION ACCEPTED` means the exact current production model passes all technical gates **and** the owner explicitly approved the exact current Blockbench evidence set. Any later production-asset change invalidates that approval.

## Legacy audit queue

| Pokemon | Concept | Technical baseline | Current art status | Next action |
| --- | --- | --- | --- | --- |
| Pikachu | Storm Courier | existing implementation/evidence retained | USER REJECTED — REWORK REQUIRED | Redesign to premium authored full-character quality; show new current Blockbench evidence for owner approval |
| Lucario | Aura Sentinel | exact 87-bone official source + historical cosmetic work; prior technical gates retained | USER REJECTED — REWORK REQUIRED | Keep exact official baseline; redesign visual model substantially and use the current professional manifest/Blockbench pipeline before owner review |
| Gengar | Rift Warden | exact official-source V2 technical work exists on rejected PR #327 | USER REJECTED — REWORK REQUIRED | Satisfy the current 3-reference Cobblemon-pack gate, then redesign actual geometry rather than add more/larger boxes |
| Mimikyu | Eclipse Herald | historical technical baseline | USER REJECTED — REWORK REQUIRED | Rebuild/rework to premium standard and present exact current Blockbench evidence |
| Charizard | Solar Legion | historical 138-bone technical pass | USER REJECTED — REWORK REQUIRED | Rebuild from exact current Charizard source and redesign visual language |
| Greninja | Shadow Tide | historical normal/Ash technical pass | USER REJECTED — REWORK REQUIRED | Rebuild official forms independently and redesign visual language |
| Absol | Omen Regent | historical 81-bone technical pass | USER REJECTED — REWORK REQUIRED | Rebuild from exact current Absol source and redesign visual language |
| Tyranitar | Abyssal Bastion | historical 69-bone technical pass | USER REJECTED — REWORK REQUIRED | Rebuild from exact current Tyranitar source and redesign the fortress fantasy as premium connected wearable architecture |

## 0448 Lucario — Aura Sentinel technical history retained, art rejected

Aura Sentinel previously closed several engineering failure modes: it used an exact current official model, preserved anatomy, and passed recorded attachment/build/runtime gates for historical heads. Those facts remain technical history, not artistic acceptance.

Pinned historical official source recorded by that slice:

- model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`
- animation SHA-256 `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`
- poser SHA-256 `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`
- base resolver SHA-256 `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`
- historical official/production normal texture SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- historical official/production shiny texture SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- historical accessory overlay SHA-256 `7deb8211b976a7c43970ec78a70ccd41f1af0e575c4aab0d3c08b08c2ec4b43a`
- original bones: 87

Historical Blockbench/gate evidence remains useful only for provenance/regression analysis. A future Lucario production candidate must use the current exact-source/reference/professional-review gates and a current manifest/evidence set.

Current artistic status remains `USER REJECTED — REWORK REQUIRED` until explicit owner approval of a materially improved exact current evidence set.

## 0094 Gengar — Rift Warden

PR #327 was closed unmerged after owner visual rejection. Its exact-source/anatomy/texture/Blockbench/build evidence remains a technical baseline only. V2 must be materially redesigned before another owner review. Do not reopen or merge the same visual model merely because its checks are green.

The current mandatory reference gate must also be satisfied before new Gengar production geometry is generated.

## Other legacy species

### 0778 Mimikyu — Eclipse Herald
Current artistic status: `USER REJECTED — REWORK REQUIRED`.

### 0006 Charizard — Solar Legion
Current artistic status: `USER REJECTED — REWORK REQUIRED`. Rebuild from the exact current official Charizard geometry and independently verify official forms/sex differences before a new visual review.

### 0658 Greninja — Shadow Tide
Current artistic status: `USER REJECTED — REWORK REQUIRED`. Normal Greninja and any official alternate geometry must be independently sourced and validated. No battle-form authority is introduced by the cosmetic workflow.

### 0359 Absol — Omen Regent
Current artistic status: `USER REJECTED — REWORK REQUIRED`. Rebuild from the exact current official Absol geometry before a new visual review.

### 0248 Tyranitar — Abyssal Bastion
Current artistic status: `USER REJECTED — REWORK REQUIRED`. Rebuild from the exact current Tyranitar geometry and redesign the fortress fantasy as premium connected wearable architecture rather than block scaffolding.

## Required final report for every future slice

Report the Pokemon/concept, three or more same-species Cobblemon-pack references actually inspected, official release and hashes, original bones preserved, cosmetic bones/cubes, signature pieces, body/overlay texture derivation and hashes, official animations used, four clickable current Blockbench PNGs, concrete internal artistic evaluation, professional manifest/evidence-set hash, validators/tests/build/CI, PR/merge state, sex/form differences, **owner approval state**, and the next slice.

Never report `ART ACCEPTED`, `FULL TRANSFORMATION ACCEPTED`, `EPIC ACCEPTED`, ready-to-merge artistic status, or equivalent without explicit owner approval of the exact current evidence set.
