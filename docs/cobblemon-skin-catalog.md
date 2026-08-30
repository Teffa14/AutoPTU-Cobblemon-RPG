# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokemon variants and their current technical and artistic acceptance state.

The authoritative art contract is `docs/cobblemon-skin-art-direction.md`. Technical validity alone never establishes artistic acceptance.

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

## Official biological texture contract

For the current pipeline the official normal/shiny biological textures are immutable production baselines.

A new or re-audited skin must:

- keep the production body texture byte-identical to the corresponding exact official texture extracted from the pinned JAR;
- prove the SHA-256 equality in CI;
- keep every original UV unchanged;
- keep `bodyTexelRework: NONE`;
- place Ouros accessory material swatches only on verified official alpha-zero/free texels or another explicitly validated compatible accessory mechanism;
- reject any accessory overlay that paints an occupied biological texel;
- preserve official transparency, sex/form and resolver semantics.

Recoloring the biological body is not an artistic shortcut. The transformation must come from connected geometry, layering, silhouette, equipment material separation and composition.

Required metadata includes the official body-texture SHA-256, production body-texture SHA-256, `bodyTexelRework`, palette/material intent and accessory overlay/UV reservation when used.

## Physical attachment — NO FLOATING PIECES

A valid bone parent does not prove an object is actually attached.

Every large cosmetic system needs a deliberate root/contact mass connecting it to the Pokemon or to another already attached cosmetic mass. Automated gates reject missing parents, cycles, cosmetic parent chains that do not end in an official bone, detached groups and isolated cubes.

The automated bind-pose gate is only the first layer. Real Blockbench evidence must also be inspected in official idle, battle and locomotion states when those states exist. If a banner, fin, halo, mantle, coat panel, greave or other part visibly hovers or detaches during motion, the skin fails even with green CI.

Do not relax attachment thresholds to rescue an existing asset. Fix its geometry/root.

## Blockbench and gameplay-scale gate

The old in-repo Python software renderer is rejected as acceptance evidence. Blockbench remains the primary independent viewer.

The review must load the exact production `.geo.json`, exact official body texture, exact accessory overlay and official animation file through the Bedrock workflow. Official reference and skin use the same camera, projection, scale, pose and animation frame. Independent auto-fit is not an accepted comparison.

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
| Lucario | Aura Sentinel | exact 87-bone official source + 10 cosmetic groups / 97 total; prior gates green | USER REJECTED — REWORK REQUIRED | Keep technical baseline; redesign visual model substantially before asking for approval again |
| Gengar | Rift Warden | exact official-source V2 technical work exists on rejected PR #327 | USER REJECTED — REWORK REQUIRED | Do not merge V2; redesign actual geometry, not just add more/larger boxes |
| Mimikyu | Eclipse Herald | historical technical baseline | USER REJECTED — REWORK REQUIRED | Rebuild/rework to premium standard and present exact current Blockbench evidence |
| Charizard | Solar Legion | historical 138-bone technical pass | USER REJECTED — REWORK REQUIRED | Rebuild from exact current Charizard source and redesign visual language |
| Greninja | Shadow Tide | historical normal/Ash technical pass | USER REJECTED — REWORK REQUIRED | Rebuild official forms independently and redesign visual language |
| Absol | Omen Regent | historical 81-bone technical pass | USER REJECTED — REWORK REQUIRED | Rebuild from exact current Absol source and redesign visual language |
| Tyranitar | Abyssal Bastion | historical 69-bone technical pass | USER REJECTED — REWORK REQUIRED | Rebuild from exact current Tyranitar source and redesign visual language |

## 0448 Lucario — Aura Sentinel technical baseline retained, art rejected

Aura Sentinel previously closed several engineering failure modes: it used the exact current official model, preserved anatomy, used the immutable official biological texture and passed attachment/build/runtime gates. Those facts remain valid technical history.

Pinned official source:

- model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`
- animation SHA-256 `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`
- poser SHA-256 `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`
- base resolver SHA-256 `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`
- official/production normal texture SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- official/production shiny texture SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- accessory overlay SHA-256 `7deb8211b976a7c43970ec78a70ccd41f1af0e575c4aab0d3c08b08c2ec4b43a`
- original bones: 87
- derived bones: 97
- cosmetic groups: 10
- cosmetic cubes: 87
- biological body texel rework: `NONE`

Historical Blockbench/gate evidence:

- `Aura Sentinel V4 Current Official Model Review` push run `33303254385` — PASS
- PR run `33303256190` — PASS
- reviewed technical head `21ab68752caebffb1e79d5f444ed72de0bb9bf36`
- artifact `aura-sentinel-v4-current-blockbench-review`, id `9729627010`
- Cobblemon Official Model Review — PASS, run `33303256090`
- Playable Test Build — PASS, run `33303256118`
- Integration Core CI — PASS, run `33303256095`

These passes now mean `TECHNICALLY VALID` only. The owner has rejected the current model-quality level, so the artistic status is `USER REJECTED — REWORK REQUIRED`. No old acceptance wording overrides that decision.

Detailed notes remain at `docs/cobblemon-skins/0448_lucario/lucario-aura-sentinel.md`; any historical acceptance language there is subordinate to this catalog and the current art-direction contract until the species doc is revised.

## 0094 Gengar — Rift Warden

PR #327 was closed unmerged after owner visual rejection. Its exact-source/anatomy/texture/Blockbench/build evidence remains a technical baseline only. V2 must be materially redesigned before another owner review. Do not reopen or merge the same visual model merely because its checks are green.

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
Current artistic status: `USER REJECTED — REWORK REQUIRED`. Rebuild from the exact current official Tyranitar geometry and redesign the fortress fantasy as premium connected wearable architecture rather than block scaffolding.

## Required final report for every future slice

Report the Pokemon/concept, official release and hashes, original bones preserved, cosmetic bones/cubes, signature pieces, body/overlay texture details, official animations used, four clickable current Blockbench PNGs, concrete internal artistic evaluation, validators/tests/build/CI, PR/merge state, sex/form differences, **owner approval state**, and the next slice.

Never report `ART ACCEPTED`, `FULL TRANSFORMATION ACCEPTED`, `EPIC ACCEPTED`, ready-to-merge artistic status, or equivalent without explicit owner approval of the exact current evidence set.
