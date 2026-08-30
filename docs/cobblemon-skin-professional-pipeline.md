# Cobblemon Professional Skin Pipeline

This is the production-engineering contract for premium Ouros Cobblemon skins. It complements `cobblemon-skin-art-direction.md`; it does not replace owner artistic judgment.

## Objective

A sellable skin must be reproducible, source-pinned, license-aware, anatomically compatible, motion-safe, paint-capable, reviewable at gameplay scale and tied to an exact owner-reviewed evidence set.

GitHub CI can enforce the technical floor and produce deterministic evidence. It cannot guarantee taste or premium art by itself. The final artistic decision remains owner-only.

## Non-negotiable authority boundary

The skin pipeline is presentation-only. It may use Cobblemon/Minecraft models, textures, animations, poser/resolver assets, Blockbench and presentation hooks. It must never introduce Cobblemon/Minecraft battle-state authority. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.

## Required pipeline stages

### 0. Exhaustive registry and lifecycle lock

`docs/cobblemon-skin-registry.json` is the single source of truth for every committed Ouros production model. `validate_skin_registry.py` requires exact coverage between registry entries and on-disk `ouros_*.geo.json` assets.

`REFERENCE_BLOCKED`, `REFERENCE_READY`, and `LEGACY_QUARANTINED` production bytes are immutable. `REFERENCE_READY` means research passed but no production contract exists yet. A skin can change only after its strict reference dossier passes, a real professional manifest exists, and the registry promotes it to `PROFESSIONAL_CANDIDATE`. Only `OWNER_APPROVED_RELEASE` may set `saleEligible: true`.

### 1. Same-species Cobblemon-pack research gate

No production geometry starts until the exact species has at least three COMPLETE references that satisfy `docs/cobblemon-skin-reference-eligibility.md`.

Each counted reference must be a real custom-geometry skin from a real Cobblemon pack ecosystem, with MODEL + TEXTURE files opened and hashed. Mega/Gmax/canonical forms, shinies, palette swaps, ordinary accuracy remodels and standalone generic 3D models do not count.

### 2. Exact official baseline extraction

The professional manifest pins:

- Modrinth version id;
- Modrinth project id and stable release channel;
- Cobblemon version;
- Minecraft version and loader;
- primary JAR filename;
- JAR SHA-256 and SHA-512;
- exact official species model path/hash;
- official reference texture path/hash;
- official animation path/hash;
- official poser, resolver and model-license paths/hashes;
- official bone count.

`prepare_professional_review_assets.py` downloads that exact release, verifies hashes, extracts the actual assets and rejects source drift.

It also queries the official Modrinth project at review time and rejects the manifest if its pinned release is no longer the latest listed stable release compatible with the declared Minecraft version and loader.

### 3. Deterministic builder

Every production skin has one declared Python builder and argv in its professional manifest.

`run_manifest_builder.py` reruns that exact builder in GitHub and requires the resulting model and texture bytes to match the committed SHA-256 values. A builder that cannot reproduce committed production bytes is not production-ready.

The same reproducibility contract covers production resolver/routing files; a correct model with stale or hand-edited runtime routing does not pass.

This removes the old pattern where a model could be committed but the repository could no longer prove how to regenerate it.

### 4. Immutable anatomy, flexible authored surface treatment

The current official model remains the biological source of truth.

`validate_original_model.py` requires all official bones/cubes/pivots/locators/UV definitions to remain JSON-equivalent and ordered. Added cosmetic groups are `ouros_*`.

Texture treatment is more flexible than the old accessory-only rule:

- `OFFICIAL_IDENTICAL` keeps the exact official body texture;
- `DERIVED_FROM_OFFICIAL` allows deliberate repaint/recolor from the exact official texture while preserving dimensions/UV/alpha contracts and passing `validate_derived_texture.py`;
- `ACCESSORY_OVERLAY` isolates additional cosmetic material when useful.

Premium paint is expected to use value ramps, occlusion, highlights, hue/value variation and material breakup. Flat hue shifts/flood fills are not a final-quality solution.

### 5. Physical attachment and motion safety

`validate_cosmetic_attachment.py` rejects missing parents, cycles, detached groups and isolated bind-pose pieces.

This is only a structural floor. The professional Blockbench pass must still inspect official animation states. A parent chain that is technically valid but visibly floats, clips or lags in motion fails the art review.

### 6. Matched-camera Blockbench evidence

Blockbench remains the independent viewer. The manifest pins Blockbench version + AppImage SHA-256.

The generic professional review uses the exact official model/texture/animation and the exact committed production model/texture stack. The official reference generates the camera profile; the candidate reuses that same profile.

Minimum evidence bundle:

- `official_reference_three_quarter.png`;
- `hero_three_quarter.png`;
- battle/alternate motion three-quarter when a real official state exists;
- front/back structural views;
- at least one 128-192 px gameplay-scale render;
- `contact_sheet.png`;
- PNG SHA-256 manifest;
- machine-readable review contract.

Never fabricate an animation state that does not exist in the official species assets.

### 7. Automated visual floor, not automated taste

`validate_blockbench_evidence.py` verifies evidence dimensions/hashes and can enforce a low minimum matched-camera pixel/silhouette delta versus the official Pokemon. This prevents no-op or nearly unchanged candidates from pretending to be transformations.

These metrics are intentionally not a beauty score. Passing them does not mean premium quality. The pipeline records `artApprovalGrantedByTooling: false`.

### 8. Professional review manifest on every production change

Every production skin change must update:

`docs/cobblemon-skin-review-manifests/<dex>_<species>.json`

The manifest binds together:

- exact reference dossier;
- source hashes;
- production hashes/counts;
- deterministic builder;
- body/overlay derivation;
- Blockbench configuration;
- required evidence filenames;
- technical visual floor;
- three or more concrete reference lessons;
- one to three signature pieces;
- macro-form plan;
- paint plan;
- gameplay read goal;
- variant/sex/form audit;
- current art status and owner approval state.

`validate_changed_skin_professional_manifests.py` rejects a production asset change when the matching manifest was not updated in the same PR. This prevents stale metadata from blessing a later model revision.

### 9. Owner-only acceptance

Allowed pre-owner states:

- `ARTISTIC FAIL`
- `USER REJECTED — REWORK REQUIRED`
- `OWNER REVIEW REQUIRED`

`OWNER APPROVED` requires an explicit owner approval record tied to the exact Git head and evidence-set SHA-256. Any later production-asset change invalidates that approval.

CI green means technically reproducible and reviewable. It never means artistically accepted.

## GitHub workflow architecture

The pipeline should converge on two reusable layers:

1. **Professional Skin Quality Gate** — lightweight PR gate for manifests, reference contract and deterministic metadata.
2. **Professional Blockbench Review** — reusable/heavy workflow that regenerates the model, downloads the exact official JAR and Blockbench binary, validates source/anatomy/texture/attachment, captures matched-camera evidence, builds the contact sheet and uploads the evidence artifact.

The repository uses only the reusable generic review plus a changed-manifest dispatcher. Species-specific generation/review workflows and rejected one-off builders are legacy and must not be restored. A species is configured exclusively through its professional manifest.

See `docs/cobblemon-skin-studio-runbook.md` for the exact operator workflow and owner release protocol.

## Art-quality failure modes that still require human judgment

Even a perfectly green technical pipeline must be rejected when the result reads as:

- base Pokemon plus accessories;
- box armor or cuboid scaffolding;
- generic portal/cage silhouette;
- repeated bars/slabs;
- disconnected rear hardware;
- flat or muddy repaint;
- detail noise with no macro-form hierarchy;
- weak rear/side composition;
- fantasy that disappears at gameplay scale;
- a distinctive third-party design copied too literally.

The workflow exists to make these problems easier to see and harder to hide, not to replace the owner deciding whether the result is actually premium.
