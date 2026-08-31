# Cobblemon Skin Studio — Production Runbook

This is the supported path from research to a skin that can be offered for sale. Historical species workflows and procedural artistic builders do not override the current professional contract.

`docs/cobblemon-skin-blockbench-authoring.md` is authoritative for premium geometry authoring. Python is used for deterministic materialization, validation, provenance and evidence plumbing, not for inventing signature macro-forms.

## Source of truth

`docs/cobblemon-skin-registry.json` lists every production skin asset in the repository. The registry is exhaustive: an on-disk `ouros_*.geo.json` without an entry fails CI, and a stale registry entry without an on-disk model also fails.

Lifecycle meanings:

| Lifecycle | Production bytes may change | Heavy Blockbench review | Sale eligible |
| --- | --- | --- | --- |
| `REFERENCE_BLOCKED` | No | No | No |
| `REFERENCE_READY` | No; author and review a new Blockbench source first | No | No |
| `LEGACY_QUARANTINED` | No | No | No |
| `PROFESSIONAL_CANDIDATE` | Yes, with manifest updated in the same PR | Automatic | No |
| `OWNER_APPROVED_RELEASE` | Only if approval is invalidated and state returns to candidate | Required for the reviewed head | Yes |

There is exactly one active slice. The active slice is `0448_lucario`, currently `REFERENCE_READY` after complete inspection of three eligible Lucario skins across two projects. Rejected production bytes remain locked until a new Blockbench-authored source passes visual review and is bound to a professional manifest plus deterministic materializer.

## Stage 1 — research before modeling

Create or complete `docs/cobblemon-skin-reference-dossiers/<dex>_<species>.json`. Run:

```bash
python tools/cobblemon-model-review/validate_species_reference_dossier.py \
  docs/cobblemon-skin-reference-dossiers/0448_lucario.json \
  --expected-species lucario
```

The command must pass without `--allow-blocked`. MODEL and TEXTURE files from at least three eligible same-species Cobblemon-pack custom skins must have been opened and hashed, covering at least two independent projects. Screenshots, shinies, Mega/Gmax/canonical forms, accuracy remodels and standalone generic 3D uploads do not unlock production.

## Stage 2 — author the candidate in Blockbench

Start from the exact model, textures and relevant animation assets extracted from the pinned compatible Cobblemon JAR. Lock the official biological hierarchy from artistic edits and author only `ouros_*` additions.

The first saved source may be a pre-production authored patch or another Blockbench-editable source. It must bind to the exact official model hash and remain `BLOCKBENCH_REVIEW_PENDING` until the macro-form has been inspected in the required front, side, rear, three-quarter and gameplay-scale views.

Do not promote or materialize a candidate because a geometry validator is green. Validators are rejection floors. Rebuild stacked slabs, giant boxes, detached systems, weak contact roots and unreadable gameplay-scale silhouettes in Blockbench before production.

When the visual source survives the authoring loop, create `docs/cobblemon-skin-review-manifests/<dex>_<species>.json` from `_template.json`. The manifest records the committed authored-source path/hash and the deterministic materializer command. Promote the registry entry to `PROFESSIONAL_CANDIDATE` only in the same change that introduces the complete production contract.

The materializer may extract the official baseline, merge or copy the authored `ouros_*` section, generate deterministic texture/resolver metadata and prove stable output bytes. It must not redesign the costume through a procedural cuboid recipe.

Run the local technical floor:

```bash
python tools/cobblemon-model-review/validate_skin_registry.py
python tools/cobblemon-model-review/validate_professional_skin_manifest.py \
  docs/cobblemon-skin-review-manifests/0448_lucario.json
python tools/cobblemon-model-review/run_manifest_builder.py \
  docs/cobblemon-skin-review-manifests/0448_lucario.json
```

Any production-byte change must update the matching manifest in the same PR. A blocked, reference-ready or quarantined production asset cannot change before that promotion contract is complete.

## Stage 3 — automatic exact review

A PR that changes a professional candidate automatically invokes the reusable `Cobblemon Professional Blockbench Review` workflow. It validates the registry and manifest; reruns the deterministic materializer and proves byte reproducibility; downloads the exact Modrinth release and verifies JAR hashes; extracts and verifies official model, texture and animation bytes; validates anatomy, derived texture provenance and cosmetic attachment; downloads the pinned Blockbench binary and verifies its checksum; captures official and candidate images with the same camera and real official animation state; creates gameplay-scale evidence, a contact sheet, PNG hashes, a review contract and evidence-set fingerprint; and uploads the immutable review artifact.

Green CI means reproducible and reviewable. It never means beautiful or approved.

### Evidence retention policy

Blockbench preview PNGs, contact sheets, intermediate pose galleries and other rendered review images are GitHub Actions artifacts, not source files. Do not commit generated galleries under `test-evidence/visual/cobblemon-skins/` or another species-specific preview directory. Old rejected galleries create stale visual noise and can be mistaken for current acceptance evidence.

The repository keeps the durable review contract: professional manifest, production hashes, evidence-set fingerprint, provenance and any owner-approval record. The workflow artifact keeps the exact rendered PNG set for the reviewed head. `validate_skin_registry.py` rejects reintroduced committed legacy galleries.

## Stage 4 — owner-only sale release

The project owner opens the exact PNG artifact and either rejects it or explicitly approves it.

On rejection, set `artStatus` to `USER REJECTED — REWORK REQUIRED`; keep `saleEligible: false`.

On approval, create `docs/cobblemon-skin-owner-approvals/<dex>_<species>.json` from the approval template. Record the exact reviewed Git head, evidence-set SHA-256 and the owner's GitHub APPROVED review id/PR number, then set the manifest to `OWNER APPROVED` and registry lifecycle to `OWNER_APPROVED_RELEASE` with `saleEligible: true`.

Any later production asset change invalidates that approval and returns the entry to candidate state.

## Repository-wide doctor

Before opening or updating a production PR:

```bash
python -m compileall -q tools/cobblemon-model-review
python tools/cobblemon-model-review/validate_skin_registry.py
python tools/cobblemon-model-review/validate_species_reference_dossier.py \
  tools/cobblemon-model-review/fixtures/reference_dossier_validator_pass.json \
  --expected-species testmon
```

Then run the normal Playable Test Build and Integration Core CI. Skin tooling remains presentation-only and never gains battle-state authority.
