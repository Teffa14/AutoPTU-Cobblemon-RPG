# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokemon variants and their current technical and artistic acceptance state.

The authoritative art contract is `docs/cobblemon-skin-art-direction.md`. Technical validity alone never establishes artistic acceptance.

## Production source target

Current repository target:

- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3 Fabric
- official Modrinth version id `kF7CvxTo`
- official file `Cobblemon-fabric-1.7.3+1.21.1.jar`
- official JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- official JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`

The compatible stable release must be checked again before every skin slice. The Rift Warden V2 source check queried the official Modrinth Cobblemon project for stable Fabric releases compatible with Minecraft 1.21.1 and again resolved the current repository target to Cobblemon 1.7.3.

For a Pokemon that already exists in Cobblemon, Ouros MUST generate the production edited model from the exact original `.geo.json` extracted from that pinned official JAR. Do not rebuild anatomy, substitute a mirror, copy a fork, use screenshots as geometry, approximate the Pokemon with replacement cubes or use a repository-stored legacy body as the editable baseline.

All original bones must remain JSON-equivalent and in the same order unless an explicit documented exception is approved. Geometry identifiers may change. New cosmetic bones must be `ouros_*`. Male, female and official forms must be derived independently from their corresponding official files.

## Presentation-only authority boundary

This workflow never gives battle-state authority to Cobblemon or Minecraft. Cobblemon/Minecraft may provide models, textures, animations and rendering/presentation hooks. AutoPTU/Ouros decides combatants, legality, HP/status, tactical positions, RNG, damage and results.

## Visual acceptance — FULL TRANSFORMATION OR REJECTED

Technical validity is necessary but does not make a good skin.

A skin fails artistic review when it still reads as the base Pokemon with small blocks attached to the head, arms or back. The required result is a complete visual transformation at gameplay distance while the official biological model remains intact underneath.

The design must establish one strong fantasy, a strong three-quarter silhouette, one to three dominant signature systems, large connected masses, coherent equipment/material treatment, real layering, readable asymmetry where useful, and front/side/rear coherence. Small hardware detail comes only after the large read works.

Large external silhouette pushes are allowed and often desirable. Armor, mantles, cowls, coats, pauldrons, packs, banners, field equipment, fins, conductors, coils, dimensional frames and ornaments may substantially change the outer outline provided the Pokemon remains clearly identifiable and the original anatomy is untouched below them.

The goal is not to maximize the number of cubes. Solve the whole character first.

## Official biological texture contract

For the current pipeline the official normal/shiny biological textures are immutable production baselines.

A new or re-audited skin must:

- keep the production body texture byte-identical to the corresponding exact official texture extracted from the pinned JAR, or reference that exact official resource directly;
- prove SHA-256 equality in CI when a production copy exists;
- keep every original UV unchanged;
- keep `bodyTexelRework: NONE`;
- place Ouros accessory material swatches only on verified official alpha-zero/free texels or another explicitly validated compatible accessory mechanism;
- reject any accessory overlay that paints an occupied biological texel;
- preserve official transparency, sex/form and resolver semantics.

Recoloring the biological body is not an artistic shortcut. Transformation must come from connected geometry, layering, silhouette, equipment material separation and composition.

## Physical attachment — NO FLOATING PIECES

A valid bone parent does not prove an object is actually attached.

Every large cosmetic system needs a deliberate root/contact mass connecting it to the Pokemon or to another already attached cosmetic mass. Automated gates reject missing parents, cycles, cosmetic parent chains that do not end in an official bone, detached groups and isolated cubes.

The automated bind-pose gate is only the first layer. Real Blockbench evidence must also be inspected in official idle, battle and locomotion states when those states exist. Do not relax attachment thresholds to rescue an existing asset; correct its geometry/root.

## Blockbench and gameplay-scale gate

Blockbench is the primary independent viewer. The deprecated project Python renderer is not artistic acceptance evidence.

The review must load the exact production `.geo.json`, exact official body texture, exact accessory overlay and official animation file through the Bedrock workflow. Official reference and skin use the same camera, projection, scale, pose and animation frame. Independent auto-fit is not an accepted comparison.

When official equivalents exist, evidence includes `official_reference_three_quarter`, `hero_three_quarter`, `battle_ready_three_quarter`, `walking_three_quarter` and structural front/left/right/back views. Never fabricate a battle or locomotion pose when Cobblemon does not provide an equivalent animation.

Every work pass must expose at least four current PNGs and every accepted skin needs a real gameplay-scale sample with the Pokemon approximately 128–192 px tall. Reject the skin if the concept disappears at that size, signature pieces collapse into noise, or the first read becomes the untouched base Pokemon again.

## Status meanings

`TECHNICALLY VALID` means official-source provenance, anatomy preservation and recorded integration/build gates were satisfied for a historical pass.

`ART RE-AUDIT REQUIRED` means a historical pass is not accepted under the current current-model/full-transformation/no-floating-pieces/no-body-repaint contract.

`ART ACCEPTED; FINAL PR GATES PENDING` means current real Blockbench evidence has passed human visual review but applicable current-head repository gates are not all green yet.

`FULL TRANSFORMATION ACCEPTED` means the actual production model was generated from the exact pinned official model, biological texture preservation and attachment gates pass, current real Blockbench evidence was visually accepted, gameplay-scale read is acceptable, and applicable build/CI gates pass on the reviewed asset head.

## Current audit queue

| Pokemon | Concept | Technical baseline | Current art status | Next action |
| --- | --- | --- | --- | --- |
| Pikachu | Storm Courier | historical male/female full-transformation pass | RE-AUDIT WHEN ACTIVE | Re-prove exact male/female source and rebuild under current no-body-repaint/no-floating contract |
| Lucario | Aura Sentinel V4 | exact 87-bone official source + 10 connected cosmetics / 97 total | FULL TRANSFORMATION ACCEPTED | Merged in PR #318; keep as current strict-contract reference |
| Gengar | Rift Warden V2 | exact 78-bone official source + 9 connected cosmetics / 87 total | ART ACCEPTED; FINAL PR GATES PENDING | Open scoped PR for the exact reviewed asset, run Blockbench/Playable/Integration/current-model gates, merge only green |
| Mimikyu | Eclipse Herald | historical pass | ART RE-AUDIT REQUIRED | Rebuild/review from exact current official source after active Gengar slice closes |
| Charizard | Solar Legion | historical 138-bone pass | ART RE-AUDIT REQUIRED | Rebuild from exact current Charizard source; eliminate model drift/floating pieces |
| Greninja | Shadow Tide | historical normal/Ash pass | ART RE-AUDIT REQUIRED | Rebuild each official form independently and redo artistic pass |
| Absol | Omen Regent | historical 81-bone pass | ART RE-AUDIT REQUIRED | Rebuild from exact current Absol source and redo full transformation |
| Tyranitar | Abyssal Bastion | historical 69-bone pass | ART RE-AUDIT REQUIRED | Rebuild fortress concept as connected wearable architecture |

## 0448 Lucario — Aura Sentinel V4

Aura Sentinel V4 is the first merged legacy correction under the current immutable-body/connected-geometry workflow.

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

The accepted macro-form is the connected cowl/visor, mantle, breastplate, compact dorsal shrine, asymmetric relic wing, split waistcoat, armguards and greaves. Real matched-camera Blockbench and gameplay-scale evidence passed, then PR #318 merged as commit `b558882ad2b27877a18c1328651eb106d16653b9` after Playable Test Build and Integration Core CI remained green on the final human documentation head.

Detailed notes: `docs/cobblemon-skins/0448_lucario/lucario-aura-sentinel.md`.

## 0094 Gengar — Rift Warden V2

Rift Warden V2 is the active re-audit and was rebuilt from the exact current official Gengar model rather than the old repository candidate.

Pinned official source:

- model SHA-256 `57449f9653a403a783efdffa3195eb6948aceb855411f4e77caaa9c29175ad38`
- animation SHA-256 `68a8bf920086c6dc368a8ffb5c449aedb314aab03a2df7bdde10bd61ea0cdb9f`
- base resolver SHA-256 `aeecefe6571d99bc9ab38a3b22af5e34769b346ed9858335292c82209ee95afc`
- normal texture SHA-256 `7aba3220a0007d5ac3f36bc51611e485a3bedf330319fa474b907fc5b3f77b65`
- normal emissive SHA-256 `c1b53aac2c0c48c417bade392198a65cb79fa76b78ad17907b3a10a6ce944d87`
- shiny texture SHA-256 `1a0821422f8bfbe43a02132c658e48213e6adedbc1b5854f125683951deaeb21`
- shiny emissive SHA-256 `835b1ee1221ce476655cccb366d9143952555604c37824bf0be03a28b55695c8`
- Pokemopolis layer SHA-256 `715f050dcb0daf3ef3fecb6ecc9d8bd878861c903bcb4d6e7b1d93ceb6732463`
- original bones: 78
- derived bones: 87
- cosmetic groups: 9
- cosmetic cubes: 75
- body texel rework: `NONE`
- accessory overlay SHA-256 `cd5d2b5e8eb31478ef0a4429bc5b7db13eb6561dac7176fc90e0b9f8eca15725`

The nine connected groups form a deep open-face cowl, broad fortress mantle, thick dorsal dimensional gate, dominant asymmetric relic wing, lower split shroud, two large armguards and two wrist guards. The resolver continues to reference the exact official biological textures and official normal/shiny emissive plus Pokemopolis presentation layers. The overlay uses only eight validated alpha-zero official texels at x=0..7, y=127.

Current real Blockbench 5.1.6 matched-camera review:

- run `33304154999` — PASS
- reviewed human head `f4a94e6f5a1c5d733c32fa57709a4db51d2700ec`
- artifact `rift-warden-v2-current-blockbench-review`
- artifact id `9729915611`
- digest `sha256:086b89158dae2a515701ce19cdac968e22209433d7808b161246ea6c5d892ab4`

The artifact PNGs were opened and inspected. Compared with official Gengar, V2 has a materially taller/wider first read, strong portal-crown silhouette, real side/rear depth and one obvious asymmetric relic-wing signature. The lower shroud and arm systems carry the transformation beyond the head/back. At 160 px the fortress/rift fantasy remains visible. Official `ground_idle`, `air_idle` and `air_fly` captures remain coherent without catastrophic detachment. The strict attachment gate passes all nine cosmetic groups.

No battle pose is fabricated because the official Gengar Bedrock animation file has no dedicated battle clip. No walking pose is fabricated because it has no walking clip; official `air_fly` is used as the locomotion evidence.

Current status: `ART ACCEPTED; FINAL PR GATES PENDING`.

Detailed notes: `docs/cobblemon-skins/0094_gengar/gengar-rift-warden.md`.

## Remaining legacy species

### 0778 Mimikyu — Eclipse Herald
Historical engineering evidence remains useful, but current artistic status is `ART RE-AUDIT REQUIRED` until rebuilt/re-proved from the exact current official source.

### 0006 Charizard — Solar Legion
The historical pass is not accepted as current. Rebuild from the exact current official Charizard geometry and independently verify any official forms/sex differences. The next accepted pass must remove model-age drift and visually floating hardware.

### 0658 Greninja — Shadow Tide
Normal Greninja and any official alternate geometry must be independently sourced and validated. No battle-form authority is introduced by the cosmetic workflow.

### 0359 Absol — Omen Regent
The historical pass is not accepted as current. Rebuild from the exact current official Absol geometry and eliminate old-model/floating-piece defects before artistic review.

### 0248 Tyranitar — Abyssal Bastion
The historical pass is not accepted as current. Rebuild from the exact current official Tyranitar geometry and redesign the fortress fantasy as connected wearable architecture before artistic review.

## Required final report for every future slice

Report the Pokemon/concept, official release and hashes, original bones preserved, cosmetic bones/cubes, signature pieces, body/overlay texture details, official animations used, four current Blockbench PNGs, concrete artistic evaluation, validators/tests/build/CI, PR/merge state, sex/form differences and the next slice selected from the new repository state.

If a claim was not validated against the official JAR or real Blockbench evidence, do not report it as completed.
