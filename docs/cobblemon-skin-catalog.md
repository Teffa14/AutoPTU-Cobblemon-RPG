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

The compatible stable release must be checked again before every skin slice. A reference render from the current release is not enough: the production edited model itself must be generated from the exact `.geo.json` extracted from that pinned official JAR.

For a Pokemon that already exists in Cobblemon, Ouros MUST begin from that exact original model. Do not rebuild anatomy, substitute a mirror, copy a fork, use screenshots as geometry, approximate the Pokemon with replacement cubes or use a repository-stored legacy body as the editable baseline.

All original bones must remain JSON-equivalent and in the same order unless an explicit documented exception is approved. Geometry identifiers may change. New cosmetic bones must be `ouros_*`. Male, female and official forms must be derived independently from their corresponding official files.

## Presentation-only authority boundary

This workflow never gives battle-state authority to Cobblemon or Minecraft. Cobblemon/Minecraft may provide models, textures, animations and rendering/presentation hooks. AutoPTU/Ouros decides combatants, legality, HP/status, tactical positions, RNG, damage and results.

## Visual acceptance — FULL TRANSFORMATION OR REJECTED

Technical validity is necessary but does not make a good skin.

A skin fails artistic review when it still reads as the base Pokemon with small blocks attached to the head, arms or back. The required result is a complete visual transformation at gameplay distance while the official biological model remains intact underneath.

The design must establish one strong fantasy, a strong three-quarter silhouette, one to three dominant signature pieces, large connected masses, coherent equipment/material treatment, real layering, readable asymmetry where useful, and front/side/rear coherence. Small hardware detail comes only after the large read works.

Large external silhouette pushes are allowed and often desirable. Armor, mantles, cowls, coats, pauldrons, packs, banners, field equipment, fins, conductors, coils and ornaments may substantially change the outer outline provided the Pokemon remains clearly identifiable and the original anatomy is untouched below them.

The goal is not to maximize the number of cubes. Solve the whole character first.

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

Every accepted skin also needs a real gameplay-scale sample with the Pokemon approximately 128–192 px tall. Reject the skin if the concept disappears at that size, signature pieces collapse into noise, or the first read becomes the untouched base Pokemon again.

## Status meanings

`TECHNICALLY VALID` means official-source provenance, anatomy preservation and recorded integration/build gates were satisfied for a historical pass.

`ART RE-AUDIT REQUIRED` means a historical pass is not accepted under the current current-model/full-transformation/no-floating-pieces contract.

`ART ACCEPTED; FINAL PR GATES PENDING` means current real Blockbench evidence has passed human visual review but applicable current-head repository gates are not all green yet.

`FULL TRANSFORMATION ACCEPTED` means the actual current production model was generated from the exact pinned official model, biological texture preservation and attachment gates pass, current real Blockbench evidence has been visually accepted, gameplay-scale read is acceptable, and applicable build/CI gates pass on the reviewed asset head.

## Legacy audit queue

| Pokemon | Concept | Technical baseline | Current art status | Next action |
| --- | --- | --- | --- | --- |
| Pikachu | Storm Courier | existing full-transformation implementation | RE-AUDIT WHEN ACTIVE | Re-prove against exact current male/female source and current no-body-repaint/no-floating contract before treating old acceptance as final |
| Lucario | Aura Sentinel | exact 87-bone official source + 10 connected cosmetic groups / 97 total | FULL TRANSFORMATION ACCEPTED | Merge PR #318 after final documentation head is confirmed not to change reviewed production assets |
| Gengar | Rift Warden | historical pass | ART RE-AUDIT REQUIRED | Rebuild/review from exact current official source before acceptance |
| Mimikyu | Eclipse Herald | historical pass | ART RE-AUDIT REQUIRED | Rebuild/review from exact current official source before acceptance |
| Charizard | Solar Legion | historical 138-bone pass | ART RE-AUDIT REQUIRED | Rebuild from exact current Charizard model; eliminate old-model drift and floating pieces; redo artistic pass |
| Greninja | Shadow Tide | historical normal/Ash pass | ART RE-AUDIT REQUIRED | Rebuild each official form independently and redo artistic pass |
| Absol | Omen Regent | historical 81-bone pass | ART RE-AUDIT REQUIRED | Rebuild from exact current Absol model; eliminate old-model drift and floating pieces; redo artistic pass |
| Tyranitar | Abyssal Bastion | historical 69-bone pass | ART RE-AUDIT REQUIRED | Rebuild from exact current Tyranitar model; eliminate old-model drift and floating pieces; redo fortress transformation |

## 0448 Lucario — Aura Sentinel current correction

Aura Sentinel is the first legacy slice reworked specifically to close both observed failure modes: editing an old body while showing a correct current reference, and technically parented but visually floating cosmetic pieces.

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

The ten cosmetic groups are the connected helm/cowl, mantle, breastplate, dorsal shrine frame, two armguards, rooted split waistcoat, asymmetric relic fin and two leg-following greaves. The v4 correction deliberately removes the old scattered-rod/halo read and adds lower-body transformation rather than concentrating all design weight on the torso.

Current real Blockbench 5.1.6 matched-camera review:

- workflow `Aura Sentinel V4 Current Official Model Review`
- push run `33303254385` — PASS
- PR run `33303256190` — PASS
- reviewed human head `21ab68752caebffb1e79d5f444ed72de0bb9bf36`
- artifact `aura-sentinel-v4-current-blockbench-review`
- artifact id `9729627010`
- artifact digest `sha256:61e2384bedb07478d3db4a3df9b777975f92aeaaa3559e9e087d1c14020bfccf`

The PNG artifact was opened and inspected. The current production candidate is the same modern Lucario anatomy as the official reference. The first read is now a complete shrine-sentinel armor system: cowl/visor, broad shoulder/chest architecture, compact rooted dorsal shrine, asymmetric relic wing, coat masses and greaves. The hero and battle captures keep the equipment connected; side/rear views do not expose obvious hovering islands. At 160 px the silhouette and cyan/gold hierarchy remain legible while Lucario remains unmistakable.

Walking is intentionally not fabricated. In this release Lucario locomotion is poser/procedural (`q.biped_walk` + `q.bimanual_swing`) rather than a dedicated standalone Lucario Bedrock walking animation clip.

On reviewed head `21ab68752caebffb1e79d5f444ed72de0bb9bf36`:

- Aura Sentinel V4 Current Official Model Review — PASS
- Cobblemon Official Model Review — PASS, run `33303256090`
- Playable Test Build — PASS, run `33303256118`
- Integration Core CI — PASS, run `33303256095`
- legacy Aura Sentinel evidence compatibility check — PASS, run `33303256101`

Artistic/technical status: `FULL TRANSFORMATION ACCEPTED` for the exact reviewed production assets above.

Detailed notes: `docs/cobblemon-skins/0448_lucario/lucario-aura-sentinel.md`.

## Other legacy species

### 0094 Gengar — Rift Warden
Historical engineering evidence remains useful, but current artistic status is `ART RE-AUDIT REQUIRED` until rebuilt/re-proved from the exact current official source.

### 0778 Mimikyu — Eclipse Herald
Historical engineering evidence remains useful, but current artistic status is `ART RE-AUDIT REQUIRED` until rebuilt/re-proved from the exact current official source.

### 0006 Charizard — Solar Legion
The historical pass is not accepted as current. Rebuild from the exact current official Charizard geometry and independently verify any official forms/sex differences. The next accepted pass must remove any model-age drift and any visually floating armor/hardware.

### 0658 Greninja — Shadow Tide
Normal Greninja and any official alternate geometry must be independently sourced and validated. No battle-form authority is introduced by the cosmetic workflow.

### 0359 Absol — Omen Regent
The historical pass is not accepted as current. Rebuild from the exact current official Absol geometry and eliminate old-model or floating-piece defects before artistic review.

### 0248 Tyranitar — Abyssal Bastion
The historical pass is not accepted as current. Rebuild from the exact current official Tyranitar geometry and redesign the fortress fantasy as connected wearable architecture before artistic review.

## Required final report for every future slice

Report the Pokemon/concept, official release and hashes, original bones preserved, cosmetic bones/cubes, signature pieces, body/overlay texture details, official animations used, four clickable current Blockbench PNGs, concrete artistic evaluation, validators/tests/build/CI, PR/merge state, sex/form differences and the next slice.

If a claim was not validated against the official JAR or real Blockbench evidence, do not report it as completed.
