# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokemon variants and their current technical and artistic acceptance state.

The authoritative art contract is `docs/cobblemon-skin-art-direction.md`. Issue #308 supersedes the previous assumption that a technically valid accessory-only pass can be called artistically complete.

## Production source target

Current repository target:

- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3 Fabric
- official Modrinth version id `kF7CvxTo`
- official file `Cobblemon-fabric-1.7.3+1.21.1.jar`
- official JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- official JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`

The compatible stable release must be checked again before every skin slice. The source check for the Aura Sentinel v2 slice on 2026-08-30 still resolved the compatible Minecraft 1.21.1/Fabric target to Cobblemon 1.7.3.

For a Pokemon that already exists in Cobblemon, Ouros MUST begin from the exact original model distributed by the pinned official release. Do not rebuild anatomy, substitute a mirror, copy a fork, use screenshots as geometry or approximate the Pokemon with replacement cubes.

All original bones must remain JSON-equivalent and in the same order unless an explicit documented exception is approved. Geometry identifiers may change. New cosmetic bones must be `ouros_*`. Male, female and official forms must be derived independently from their corresponding official files.

## Presentation-only authority boundary

This workflow never gives battle-state authority to Cobblemon or Minecraft. Cobblemon/Minecraft may provide models, textures, animations and rendering/presentation hooks. AutoPTU/Ouros decides combatants, legality, HP/status, tactical positions, RNG, damage and results.

## Visual acceptance — FULL TRANSFORMATION OR REJECTED

Technical validity is necessary but does not make a good skin.

A skin fails artistic review when it still reads as the base Pokemon with small blocks attached to the head, arms or back. The required result is a complete visual transformation at gameplay distance while the official biological model remains intact underneath.

The design must establish one strong fantasy, a strong three-quarter silhouette, one to three dominant signature pieces, large connected masses, coherent palette/material treatment, real layering, readable asymmetry where useful, and front/side/rear coherence. Small hardware detail comes only after the large read works.

Large external silhouette pushes are allowed and often desirable. Armor, mantles, cowls, coats, pauldrons, packs, banners, field equipment, fins, conductors, coils and ornaments may substantially change the outer outline provided the Pokemon remains clearly identifiable and the original anatomy is untouched below them.

A simple recolor is never a completed skin. Conversely, refusing to recolor the body when the concept requires it is also a failure mode.

## Full-surface texture contract

The old default of preserving every occupied body pixel and painting only a handful of unused texels is retired as an artistic requirement. That constraint systematically produced "base Pokemon + accessory cuboids".

A premium skin may use a full derived texture from the exact official texture. It must keep the exact official dimensions and UV layout, pin the immutable official texture SHA-256, record the derived texture SHA-256, preserve required transparency/form semantics and document the intentional body-texel rework, palette and material plan.

New/re-audited metadata must include:

- `officialTextureBaselineSha256`
- `derivedTexture`
- `derivedTextureSha256`
- `bodyTexelRework`
- `paletteIntent`
- `materialIntent`
- overlay path and UV reservation when a separate accessory overlay is also used

The original model UVs are never remapped to make recoloring easier. Added geometry can use validated overlay/atlas space as appropriate.

## Blockbench and gameplay-scale gate

The old in-repo Python software renderer is rejected as acceptance evidence. Blockbench remains the primary independent viewer.

The review must load the exact production `.geo.json`, official baseline texture, derived texture/overlay and official animations through the Bedrock workflow. Official reference and skin must use the same camera, projection, scale, pose and animation frame. Independent auto-fit is not accepted as a matched comparison because it can hide silhouette-scale differences.

When official equivalents exist, evidence includes `official_reference_three_quarter`, `hero_three_quarter`, `battle_ready_three_quarter`, `walking_three_quarter` and structural front/left/right/back views. Never fabricate a battle or locomotion pose when Cobblemon does not provide an equivalent animation.

Every accepted skin also needs a real Blockbench gameplay-scale readability sample with the Pokemon approximately 128–192 px tall. Reject the skin if the concept disappears at that size, if signature pieces collapse into noise, or if the first read becomes the untouched base Pokemon again.

## Status meanings after issue #308

`TECHNICALLY VALID` means official-source provenance, anatomy preservation and the recorded integration/build gates were satisfied for that historical pass.

`ART RE-AUDIT REQUIRED` means the historical pass was accepted under the older accessory-first visual standard and is not an approved artistic reference under the full-transformation standard.

`ART ACCEPTED; FINAL PR GATES PENDING` means real Blockbench matched-camera/gameplay-scale review has passed, but the final human PR head still needs Playable Test Build, Integration Core CI and other applicable repository gates.

`FULL TRANSFORMATION ACCEPTED` may only be assigned after real Blockbench review, matched-camera evidence, gameplay-scale review, coherent full-surface material treatment where appropriate, and all required CI/build gates pass on the actual reviewed assets.

Previous labels such as `EPIC ACCEPTED` are historical only and do not override this standard.

## Legacy audit queue

| Pokemon | Concept | Technical baseline | Current art status | Next action |
| --- | --- | --- | --- | --- |
| Pikachu | Storm Courier | exact 90-bone male/female source + 8 macro cosmetic groups | FULL TRANSFORMATION ACCEPTED | Merged in PR #309; use as engineering/art-process reference, not as a design template |
| Lucario | Aura Sentinel | exact 87-bone source + 8 macro cosmetic groups / 95 total | FULL TRANSFORMATION ACCEPTED | Merge PR #313 only after the final documentation-only human head remains green; then move to the next re-audit |
| Gengar | Rift Warden | 86-bone pass previously validated | ART RE-AUDIT REQUIRED | Reassess connected masses and full-body visual integration |
| Mimikyu | Eclipse Herald | 56-bone pass previously validated | ART RE-AUDIT REQUIRED | Reassess costume coverage and material hierarchy |
| Charizard | Solar Legion | 138-bone pass previously validated | ART RE-AUDIT REQUIRED | Reassess large connected armor forms and body palette integration |
| Greninja | Shadow Tide | normal/Ash independent source models previously validated | ART RE-AUDIT REQUIRED | Reassess full-costume read and gameplay silhouette |
| Absol | Omen Regent | 81-bone v2 Blockbench baseline | ART RE-AUDIT REQUIRED | Finish repository gates only after new artistic pass is accepted |
| Tyranitar | Abyssal Bastion | 69-bone v2 pass previously validated | ART RE-AUDIT REQUIRED | Reassess fortress concept as integrated transformation rather than attached cubes |

## 0025 Pikachu — Storm Courier

Storm Courier is the first completed implementation of the issue #308 full-transformation standard. PR #309 merged the accepted v4 pass to `main`.

Pinned Cobblemon 1.7.3 official inputs:

- male `pikachu_male.geo.json` SHA-256 `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female `pikachu_female.geo.json` SHA-256 `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- official `pikachu.png` SHA-256 `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`
- official `pikachu.animation.json` SHA-256 `d9ca00604978f295ad312d358a06f2655c725b30ac3da73c3637ae160c543384`
- original bones per sex model: 90
- accepted v4 derived bones per sex model: 98

Male and female remain independent derivations. The official female tail difference remains intact.

The accepted v4 first read is a coherent storm courier system: integrated head/visor assembly, connected mantle/torso suit, chest storm core, expedition power-frame, field pylons and tail conductor system. It also uses full derived normal/shiny textures instead of leaving the original yellow body treatment dominant.

Detailed source and acceptance notes remain in `docs/cobblemon-skins/0025_pikachu/pikachu-storm-courier.md`.

## 0448 Lucario — Aura Sentinel

Aura Sentinel v2 is the second accepted implementation under the full-transformation standard and the first re-audit completed after Storm Courier.

Pinned official source:

- model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`
- animation SHA-256 `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`
- poser SHA-256 `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`
- base resolver SHA-256 `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`
- normal texture SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- shiny texture SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- original bones: 87
- derived bones: 95
- cosmetic groups: 8
- accepted cosmetic cubes: 87

The old accessory-only pass is superseded. An unreviewed 99-cube intermediate candidate was also explicitly rejected as canonical and removed from the final workflow. Production was regenerated deterministically to the human-reviewed 87-cube pass, and both generation and review now fail if that exact candidate contract drifts.

V2 uses full derived normal/shiny textures plus an integrated open-face aura helm/visor, broad mantle/breastplate, dorsal shrine/halo frame, split waistcoat, armguards and relic fin. Normal derived texture SHA-256 is `1cbb1ca7fe260d01a4e0ca7a2f0a28ea424475f856267caf19d0b4276ed19752`; shiny is `7d391c01daba8634a4cfd84cc17f1f37385afe473ed1ff578d989c66fa5cb725`.

The final PR-level real Blockbench 5.1.6 matched-camera review passed on human head `cd759c80ae502648079d0ed06594b8809293d8d6`, run `33286136164`. Artifact `9724492898` has digest `sha256:3bebb334fac699f09c51e0e990bb2be56351cb8af05d71f6e39f380764701353`. The PR-generated PNGs were opened and inspected after the canonical 87-cube assets were restored. The 160 px gameplay sample still reads as an aura knight rather than ordinary Lucario with small accessories.

The final reviewed asset head also passed Cobblemon Official Model Review run `33286136163`, Playable Test Build run `33286136181`, and Integration Core CI run `33286136176`. `ground_idle` and `battle_idle` come from the official Lucario animation file. Walking is intentionally not fabricated because official Lucario locomotion is procedural rather than a dedicated Bedrock walking clip.

Current artistic/technical status: `FULL TRANSFORMATION ACCEPTED`. PR #313 may merge only if the final documentation-only human head retains green repository checks without modifying the reviewed production assets.

Detailed notes: `docs/cobblemon-skins/0448_lucario/lucario-aura-sentinel.md`.

## 0094 Gengar — Rift Warden

Technical baseline provenance remains recorded in the species document. The historical pass preserved all 78 official bones and appended eight `ouros_*` groups for 86 total. Official normal, shiny and Pokemopolis resolver branches were preserved. Artistic status is now `ART RE-AUDIT REQUIRED`.

Detailed notes: `docs/cobblemon-skins/0094_gengar/gengar-rift-warden.md`.

## 0778 Mimikyu — Eclipse Herald

The historical pass preserved all 48 official bones and appended eight cosmetic groups for 56 total. Its previous Blockbench/build evidence remains a technical baseline. Artistic status is now `ART RE-AUDIT REQUIRED`.

Detailed notes: `docs/cobblemon-skins/0778_mimikyu/mimikyu-eclipse-herald.md`.

## 0006 Charizard — Solar Legion

The historical pass preserved all 130 official bones and appended eight cosmetic groups for 138 total while retaining the official normal/shiny/flame presentation branches. Its previous CI evidence remains a technical baseline. Artistic status is now `ART RE-AUDIT REQUIRED`.

Detailed notes: `docs/cobblemon-skins/0006_charizard/charizard-solar-legion.md`.

## 0658 Greninja — Shadow Tide

Normal Greninja and Ash-Greninja remain independently derived from their official source geometries. The technical baseline keeps the battle-state boundary: no Ash/Battle Bond combat authority is invented by this cosmetic workflow. Artistic status is now `ART RE-AUDIT REQUIRED`.

Detailed notes: `docs/cobblemon-skins/0658_greninja/greninja-shadow-tide.md`.

## 0359 Absol — Omen Regent

The historical v2 preserved all 73 official bones and appended eight cosmetic groups for 81 total. Its real Blockbench evidence remains useful technical history, but repository-wide completion must not be used to bypass the new artistic gate. Artistic status is `ART RE-AUDIT REQUIRED`.

Detailed notes: `docs/cobblemon-skins/0359_absol/absol-omen-regent.md`.

## 0248 Tyranitar — Abyssal Bastion

The historical v2 preserved all 61 official bones and appended eight cosmetic groups for 69 total. Its previous official-model, Blockbench, Playable Test Build and Integration Core results remain engineering evidence only. Artistic status is `ART RE-AUDIT REQUIRED` until the fortress fantasy is reworked as a coherent large-scale transformation and passes gameplay-scale review.

Detailed notes: `docs/cobblemon-skins/0248_tyranitar/tyranitar-abyssal-bastion.md`.

## Required final report for every future slice

Report the Pokemon/concept, official release and hashes, original bones preserved, cosmetic bones/cubes, signature pieces, derived texture/overlay details, official animations used, links to Blockbench official/hero/battle/walk/four-view/gameplay-scale evidence, concrete artistic evaluation, validators/tests/build/CI, PR/merge state, sex/form differences and the next slice.

If a claim was not validated against the official JAR or real Blockbench evidence, do not report it as completed.