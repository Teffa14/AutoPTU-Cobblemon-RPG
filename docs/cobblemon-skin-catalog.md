# Cobblemon Skin Catalog

This catalog records Ouros-authored visual Pokemon variants and their current technical and artistic acceptance state.

The authoritative art contract is `docs/cobblemon-skin-art-direction.md`. Issue #308 supersedes the previous assumption that a technically valid accessory-only pass can be called artistically complete.

## Production source target

Current repository target:

- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3 Fabric
- official Modrinth version id `kF7CvxTo`
- official file `Cobblemon-fabric-1.7.3+1.21.1.jar`

The compatible stable release must be checked again before every skin slice. At adoption of the full-transformation standard on 2026-08-30, the official Modrinth 1.21.1/Fabric listing still resolves to Cobblemon 1.7.3.

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

`FULL TRANSFORMATION ACCEPTED` may only be assigned after real Blockbench review, matched-camera evidence, gameplay-scale review, coherent full-surface material treatment where appropriate, and all required CI/build gates pass on the actual reviewed assets.

Previous labels such as `EPIC ACCEPTED` are historical only and do not override this standard.

## Legacy audit queue

| Pokemon | Concept | Technical baseline | Current art status | Next action |
| --- | --- | --- | --- | --- |
| Pikachu | Storm Courier | 98-bone male/female pass validated under prior workflow | OVERHAUL IN PROGRESS | First reference implementation for full-surface transformation |
| Lucario | Aura Sentinel | 95-bone pass previously validated | ART RE-AUDIT REQUIRED | Reassess silhouette, body material treatment and gameplay-scale read |
| Gengar | Rift Warden | 86-bone pass previously validated | ART RE-AUDIT REQUIRED | Reassess connected masses and full-body visual integration |
| Mimikyu | Eclipse Herald | 56-bone pass previously validated | ART RE-AUDIT REQUIRED | Reassess costume coverage and material hierarchy |
| Charizard | Solar Legion | 138-bone pass previously validated | ART RE-AUDIT REQUIRED | Reassess large connected armor forms and body palette integration |
| Greninja | Shadow Tide | normal/Ash independent source models previously validated | ART RE-AUDIT REQUIRED | Reassess full-costume read and gameplay silhouette |
| Absol | Omen Regent | 81-bone v2 Blockbench baseline | ART RE-AUDIT REQUIRED | Finish repository gates only after new artistic pass is accepted |
| Tyranitar | Abyssal Bastion | 69-bone v2 pass previously validated | ART RE-AUDIT REQUIRED | Reassess fortress concept as integrated transformation rather than attached cubes |

## 0025 Pikachu — Storm Courier

Storm Courier is the first mandatory overhaul under issue #308 because it demonstrates the old failure mode most clearly.

Pinned Cobblemon 1.7.3 official inputs:

- male `pikachu_male.geo.json` SHA-256 `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female `pikachu_female.geo.json` SHA-256 `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- official `pikachu.png` SHA-256 `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`
- official `pikachu.animation.json` SHA-256 `d9ca00604978f295ad312d358a06f2655c725b30ac3da73c3637ae160c543384`
- original bones per sex model: 90
- historical epic-v3 derived bones per sex model: 98

Male and female remain independent derivations. The official female tail difference must remain intact.

The previous v3 equipment set — visor, cowl, mantle, harness, pack, pylons and tail hardware — is retained only as a technical starting point. The overhaul is not accepted by merely making those cubes larger. The new pass must visually unify head, torso, equipment and tail through a deliberate storm-runner material system, stronger connected garment/armor masses, dominant power equipment and body-surface recolor/retexturing derived from the exact official UV layout.

The target first read is a complete fantasy storm courier. "Pikachu wearing goggles and a backpack" is an explicit rejection condition.

Detailed source and historical notes remain in `docs/cobblemon-skins/0025_pikachu/pikachu-storm-courier.md`.

## 0448 Lucario — Aura Sentinel

Technical baseline provenance remains recorded in the species document. The historical pass preserved all 87 official bones and appended eight `ouros_*` groups for 95 total. Its previous Blockbench/build acceptance remains useful engineering evidence, but its artistic status is now `ART RE-AUDIT REQUIRED`.

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
