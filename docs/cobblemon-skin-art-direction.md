# Cobblemon Skin Art Direction — Full Transformation Standard

This document is the visual acceptance contract for Ouros-authored Cobblemon skins. It applies to every new skin and every legacy re-audit.

## Scope and authority boundary

This workflow is presentation-only. Cobblemon/Minecraft may provide the official Pokemon model, textures, animations, render hooks, networking and presentation systems. They do not decide combatants, legality, HP/status, positions, tactical RNG, damage or battle results. AutoPTU/Ouros remains authoritative for all tactical battle facts.

## Owner visual approval is mandatory

Artistic acceptance belongs to the project owner.

CI, Blockbench validity, geometry metrics, cube counts, validators and assistant review can only produce a candidate. They cannot mark a skin `ART ACCEPTED`, `EPIC ACCEPTED`, `FULL TRANSFORMATION ACCEPTED`, ready to merge artistically, or equivalent.

The maximum internal art state before explicit owner approval is `OWNER REVIEW REQUIRED`.

If the owner rejects a model, the exact model becomes `USER REJECTED — REWORK REQUIRED` immediately, even when every technical check is green. Do not infer approval from silence, previous versions, prior concepts or approval of another model. Any later production-asset change invalidates prior artistic approval.

## One-model lock

Once a species becomes the active artistic slice, continue that exact model until either:
- the owner explicitly approves the exact current Blockbench evidence; or
- the owner explicitly tells the workflow to abandon/switch the slice.

Do not skip a rejected model to accumulate more superficially finished skins.

## Mandatory three-reference species gate — no modeling before research

Before creating, regenerating or materially reworking any production skin for a Pokemon, the workflow must inspect **at least three already-made external skins/models for that exact species**. This is a hard pre-modeling gate, not an optional inspiration step.

A valid reference dossier must exist under `docs/cobblemon-skin-reference-dossiers/` and pass `tools/cobblemon-model-review/validate_species_reference_dossier.py` before production geometry work begins.

Each of the three or more references must:
- depict or implement the same Pokemon species being modeled;
- come from a real external Cobblemon pack/modpack/resource pack/server pack or equivalent community implementation;
- be a distinct skin/model implementation, not three screenshots or three versions of the same unchanged asset;
- be studied from the actual model/texture/resource files when those files are publicly and lawfully accessible, not only from marketing screenshots;
- record project/source URL, version when known, asset type, license/provenance status and inspection status;
- identify concrete reusable *techniques* such as contour/wrap, taper, overlap, silhouette distribution, bone grouping, material breakup, painted depth, edge treatment, asymmetry, negative space, animation-safe attachment or full-body detail distribution;
- identify distinctive geometry, markings, motifs, logos, palettes or costume shapes that must **not** be copied.

The dossier must contain at least three references with `assetInspectionStatus: COMPLETE` before a builder may generate production geometry. If fewer than three real assets can be inspected, the species is `REFERENCE BLOCKED` and modeling stops until the gate can be satisfied.

Internet research is mandatory. Search broadly rather than relying on the same two packs for every species. Prefer current, high-quality community examples and inspect multiple independent projects when available.

### External derivative-base use

Third-party assets may be modified or used as a derivative starting point **only when their explicit license grants the required derivative and redistribution rights** and the intended use is compatible with those terms. Record the license, attribution requirements, source version and exact asset provenance before any derivative use.

`ARR`, no-license, unknown-license and licenses that do not clearly permit derivatives/redistribution are **study-only**. They may inform general technique but their geometry, UVs, textures, palettes, markings, motifs and distinctive silhouettes must not be copied into Ouros production.

Even when an external model is permissively licensed, the current official Cobblemon model remains the authoritative biological/anatomical baseline for the species. A licensed external asset can be used as a donor/reference for cosmetic construction or technique, but the final Ouros production model must still preserve the exact current official Cobblemon original bones, hierarchy, pivots, cubes, locators and UV definitions required by this contract.

## Official source or reject

For a Pokemon that exists in Cobblemon, start from the exact model shipped in the latest stable Cobblemon release compatible with the repository target.

Current repository target when this standard was updated:
- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3 Fabric
- Modrinth version id `kF7CvxTo`
- official file `Cobblemon-fabric-1.7.3+1.21.1.jar`

Before each slice verify again that this remains the latest compatible stable release.

CI/metadata must pin:
- release id and filename;
- JAR SHA-256/SHA-512;
- exact official model hash;
- exact official texture hashes;
- relevant animation, poser, resolver and license hashes.

Mirrors, copied repositories, screenshots, old repository-stored bodies and manual anatomy reconstructions are not accepted geometry sources.

A correct current reference render does not prove the edited production model is current. The production model itself must be generated from the exact extracted official `.geo.json`.

## Anatomy preservation

Original Pokemon anatomy stays intact underneath the Ouros transformation.

All original bones must remain JSON-equivalent and in original order, including names, parents, pivots, rotations, cubes, locators and UV definitions. The geometry identifier may change. Ouros-owned additions use `ouros_*` names.

Do not rebuild or reinterpret original anatomy. Do not introduce an alternate body rig.

If Cobblemon ships separate male/female/form geometry, derive each variant independently from its corresponding official source file and validate it against that baseline.

## Full transformation or rejected

Technical validity is necessary but is not artistic quality.

A skin fails when it reads as:
- base Pokemon plus accessories;
- procedural scaffold;
- rectangular cage;
- generic portal frame;
- repeated straight bars;
- stack of orthogonal plates;
- oversized boxy shoulder slabs;
- large geometry added only to increase silhouette;
- generated cuboids around an otherwise unchanged character.

The target is an authored premium character transformation that remains unmistakably the original species.

Required design hierarchy:
1. premium three-quarter silhouette;
2. immediately legible fantasy/class/material identity;
3. one to three dominant signature pieces;
4. large connected masses before small detail;
5. contour/wrap/taper/overlap that responds to anatomy;
6. deliberate negative space;
7. coherent full-body distribution, including lower body when appropriate;
8. material/value hierarchy;
9. meaningful asymmetry where useful;
10. front/side/rear readability;
11. motion-safe attachment;
12. micro-detail only after the large read works.

Large external silhouette changes are allowed and encouraged when coherent. Armor, mantles, coats, collars, cowls, packs, banners, fins, conductor systems, ornaments, shrine pieces, equipment and similar forms may extend far beyond the official outline.

Large geometry is not automatically good geometry. Use rotated/subdivided forms, stepped contours, tapering, overlap, depth changes, negative space and anatomy-aware composition. The goal is to solve the whole character, not maximize cube count.

## Derived texture workflow — recolor and painting are allowed

The official normal/shiny/form textures are immutable **source baselines**, but the production skin may use a deliberate derived texture when recolor/painting materially improves the design.

A derived texture is allowed only when all of the following are true:

1. It starts from the exact official texture extracted from the pinned current compatible JAR.
2. Canvas dimensions remain identical.
3. Original UV mappings remain unchanged.
4. Alpha/transparency semantics remain identical unless a separate runtime contract explicitly proves a safe exception.
5. Normal, shiny and distinct official forms are derived independently from their own exact baselines.
6. Metadata records the official baseline SHA-256 and derived SHA-256.
7. Metadata records which regions were intentionally repainted and why.
8. Species-critical facial/anatomical landmarks remain readable.
9. The result uses real painted value/material structure, not a flat recolor.
10. No third-party texture, palette layout, marking or distinctive motif is copied.

A premium derived texture should use techniques such as:
- local value ramps;
- painted occlusion/shadow where forms meet;
- lighter facing planes;
- controlled edge highlights;
- hue/value variation inside a material;
- distinct treatment for metal, cloth, leather, stone, bone, energy or lacquer;
- selective wear/noise only where it supports material identity;
- deliberate separation between biological surface and equipment.

A one-command hue rotation, flat flood fill, uniform multiply or simple palette swap is not an acceptable final paint treatment.

Use `tools/cobblemon-model-review/validate_derived_texture.py` to prove source hash, dimensions and alpha compatibility. That validator proves compatibility only. It does not approve painting quality.

Accessory-only overlay textures remain valid when appropriate. Builders may choose official body texture + overlay, a derived body texture + overlay, or another documented compatible presentation mechanism.

## Technique study from community packs

External packs may be inspected to learn general modeling and painting technique, including geometry organization, wrap/contour, taper, overlap, negative space, texture depth, material breakup and detail hierarchy.

The technique-study rules are documented in `docs/cobblemon-skin-technique-library.md`.

Important:
- Do not copy or redistribute third-party model or texture files unless an explicit license permits it.
- Do not copy geometry, UVs, texture layouts, palettes, logos, markings, costume motifs or distinctive silhouettes.
- Reimplement only generic techniques in original Ouros assets unless a compatible license explicitly allows derivative use.

Cobbleverse and Cobblemon Delta are explicitly **study-only** references under the current audit. Their assets are not production sources.

The repository contains original neutral `.geo.json` + texture exemplars under `tools/cobblemon-model-review/reference-techniques/` so builders can study concrete technique without copying third-party assets.

## Physical attachment — no floating pieces

A valid bone parent is necessary but does not prove visual attachment.

Every large cosmetic system needs a deliberate contact/root mass connecting it to the Pokemon or to another already-attached cosmetic mass.

Automated gates must reject:
- missing/unknown parents;
- cycles;
- cosmetic chains that do not terminate in official bones;
- detached groups;
- isolated cubes.

Bind-pose validation is only the first layer. Blockbench review must inspect contact under official idle/battle/locomotion states when those states exist. Do not relax thresholds to rescue a bad asset. Fix geometry/root placement.

## Blockbench review contract

Blockbench is the primary independent viewer. Do not use the deprecated in-repo Python renderer as artistic evidence and do not create a replacement homemade renderer.

Review must load:
- exact production `.geo.json`;
- exact official or validated derived production texture;
- exact overlay when used;
- exact official animation file.

Official reference and candidate must use the same species, camera, projection, scale, pose and animation frame.

Minimum evidence when official equivalents exist:
- `official_reference_three_quarter`
- `hero_three_quarter`
- `battle_ready_three_quarter`
- `walking_three_quarter`
- front/left/right/back structural views where useful
- gameplay-scale 128–192 px

If the official species has no equivalent battle/walking clip, record that fact and do not fabricate one.

Every work pass must expose four clickable current PNGs. If expected states do not exist, substitute current structural/gameplay-scale views and explain the limitation.

## Internal QA before owner review

Before presenting a candidate, inspect the real PNGs and answer yes to all:

- Does the first glance materially change from the official Pokemon?
- Is there a signature silhouette rather than generic hardware?
- Do major forms contour/wrap/taper/overlap intentionally?
- Is the design coherent from front, sides and rear?
- Does the transformation continue through enough of the body?
- Does the paint show depth and material hierarchy rather than flat recolor?
- Does the design still read at gameplay scale?
- Is the fantasy understandable without reading the skin name?
- Is the original species still unmistakable?
- Are obvious floating, clipping or motion failures absent?

If any answer is no, status is `ARTISTIC FAIL` and the model must be reworked.

If all answers are yes, status is still only `OWNER REVIEW REQUIRED`.

## Legacy re-audit

Owner feedback on 2026-08-30 rejects the artistic quality of the current Ouros model set.

Until a materially reworked exact evidence set is explicitly approved by the owner, no existing Ouros Cobblemon skin is artistically accepted. Technical provenance and build evidence can remain recorded separately.

Legacy skins must be rebuilt/re-audited from their exact current official models and current texture baselines. Old edited models do not become valid merely because a new reference render is correct.

## PR and CI acceptance

A skin PR may exist as WIP for engineering validation.

Before merge as accepted production art, require:
- owner approval of the exact current Blockbench evidence;
- passing three-reference species dossier gate;
- official JAR/source hash validation;
- original-bone equality;
- correct sex/forms;
- texture provenance validation (official baseline and derived texture when used);
- resolver/poser/animation path validation;
- cosmetic attachment validation;
- Blockbench evidence;
- Playable Test Build;
- Integration Core CI or current replacements.

Green CI means technically valid. It never means artistically approved.

Do not merge with red/unexecuted required checks. Do not merge art solely because checks are green.

## Final report requirement

Every skin pass reports:
- species/concept;
- the three or more same-species external references actually inspected and the concrete techniques learned from each;
- license/reuse status for every external reference;
- exact official release/source hashes;
- original/derived bone counts;
- cosmetic groups;
- signature pieces;
- official/derived/overlay texture hashes and repaint scope;
- official animations used;
- four current clickable Blockbench PNGs;
- concrete artistic evaluation;
- status (`REFERENCE BLOCKED`, `ARTISTIC FAIL`, `USER REJECTED — REWORK REQUIRED`, `OWNER REVIEW REQUIRED`, or owner-approved state);
- validators/tests/build/CI;
- branch/PR/merge state;
- sex/forms;
- blockers;
- next action on the same locked model unless the owner explicitly switches it.
