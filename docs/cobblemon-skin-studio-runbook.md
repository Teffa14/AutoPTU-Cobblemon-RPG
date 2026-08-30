# Cobblemon Skin Studio — Production Runbook

This is the only supported path from research to a skin that can be offered for sale. Historical species workflows and builders were removed because they duplicated logic, encoded stale art decisions, and could bypass the current professional contract.

## Source of truth

`docs/cobblemon-skin-registry.json` lists every production skin asset in the repository. The registry is exhaustive: an on-disk `ouros_*.geo.json` without an entry fails CI, and a stale registry entry without an on-disk model also fails.

Lifecycle meanings:

| Lifecycle | Production bytes may change | Heavy Blockbench review | Sale eligible |
| --- | --- | --- | --- |
| `REFERENCE_BLOCKED` | No | No | No |
| `REFERENCE_READY` | No; author a new builder/manifest first | No | No |
| `LEGACY_QUARANTINED` | No | No | No |
| `PROFESSIONAL_CANDIDATE` | Yes, with manifest updated in the same PR | Automatic | No |
| `OWNER_APPROVED_RELEASE` | Only if approval is invalidated and state returns to candidate | Required for the reviewed head | Yes |

There is exactly one active slice. As of 2026-08-30 it is `0448_lucario`, now `REFERENCE_READY` after complete inspection of three eligible Lucario skins across two projects. The rejected production bytes remain locked until a new deterministic builder and professional manifest are authored.

## Stage 1 — research before modeling

Create or complete `docs/cobblemon-skin-reference-dossiers/<dex>_<species>.json`. Run:

```bash
python tools/cobblemon-model-review/validate_species_reference_dossier.py \
  docs/cobblemon-skin-reference-dossiers/0448_lucario.json \
  --expected-species lucario
```

The command must pass without `--allow-blocked`. MODEL and TEXTURE files from three independent, eligible same-species Cobblemon-pack skins must have been opened and hashed. Screenshots, shinies, Mega/Gmax/canonical forms, accuracy remodels, and standalone generic 3D uploads do not unlock production.

## Stage 2 — author a deterministic candidate

Start from assets extracted from the exact pinned compatible Cobblemon JAR. Add one deterministic Python builder. It must produce all committed model and texture bytes with stable hashes; it may not fetch unpinned anatomy or depend on manual Blockbench saves.

Create `docs/cobblemon-skin-review-manifests/<dex>_<species>.json` from `_template.json`. Fill every provenance, official-source, builder, texture, animation, variant, quality-intent, and evidence field. Promote the registry entry to `PROFESSIONAL_CANDIDATE` and point it to that manifest.

Run the local technical floor:

```bash
python tools/cobblemon-model-review/validate_skin_registry.py
python tools/cobblemon-model-review/validate_professional_skin_manifest.py \
  docs/cobblemon-skin-review-manifests/0448_lucario.json
python tools/cobblemon-model-review/run_manifest_builder.py \
  docs/cobblemon-skin-review-manifests/0448_lucario.json
```

Any production-byte change must update the matching manifest in the same PR. A blocked or quarantined entry cannot change production bytes at all.

## Stage 3 — automatic exact review

A PR that changes a professional candidate automatically invokes the reusable `Cobblemon Professional Blockbench Review` workflow. It:

1. validates the registry and manifest;
2. reruns the builder and proves byte reproducibility;
3. downloads the exact Modrinth release and verifies JAR SHA-256/SHA-512;
4. extracts and verifies official model, texture, and animation bytes;
5. validates anatomy, derived texture provenance, and cosmetic attachment;
6. downloads the pinned Blockbench AppImage and verifies its SHA-256;
7. captures official and candidate images with the same Blockbench camera and official animation state;
8. creates gameplay-scale evidence, a contact sheet, PNG hashes, a review contract, and an evidence-set fingerprint;
9. uploads the immutable review artifact.

Green CI means reproducible and reviewable, never beautiful or approved.

### Evidence retention policy

Blockbench preview PNGs, contact sheets, intermediate pose galleries, and other rendered review images are **GitHub Actions artifacts**, not source files. Do not commit generated galleries under `test-evidence/visual/cobblemon-skins/` or another species-specific preview directory. Old rejected galleries create stale visual noise, can be mistaken for current acceptance evidence, and duplicate immutable workflow artifacts.

The repository keeps the durable review contract: professional manifest, production hashes, evidence-set fingerprint, provenance, and any owner-approval record. The workflow artifact keeps the exact rendered PNG set for the reviewed head. `validate_skin_registry.py` rejects reintroduced committed legacy galleries so that old rejected art cannot silently become the visible reference again.

## Stage 4 — owner-only sale release

The project owner opens the exact PNG artifact and either rejects it or explicitly approves it.

On rejection, set `artStatus` to `USER REJECTED — REWORK REQUIRED`; keep `saleEligible: false`.

On approval, create `docs/cobblemon-skin-owner-approvals/<dex>_<species>.json` from the approval template. Record the exact reviewed Git head, evidence-set SHA-256, and the owner's GitHub APPROVED review id/PR number, then set the manifest to `OWNER APPROVED` and registry lifecycle to `OWNER_APPROVED_RELEASE` with `saleEligible: true`.

The validator verifies that the approval record matches the manifest, the approved commit exists, and no production model or texture changed after that head. GitHub CI also fetches the named review and proves it is an `APPROVED` review authored by the repository owner for that exact commit. `CODEOWNERS` assigns every production/release-control path to the owner. Any asset change invalidates the approval and must return the entry to `PROFESSIONAL_CANDIDATE`.

## Repository-wide doctor

Before opening a PR:

```bash
python -m compileall -q tools/cobblemon-model-review
python tools/cobblemon-model-review/validate_skin_registry.py
python tools/cobblemon-model-review/validate_species_reference_dossier.py \
  tools/cobblemon-model-review/fixtures/reference_dossier_validator_pass.json \
  --expected-species testmon
```

Then run the normal Playable Test Build and Integration Core CI. Skin tooling remains presentation-only and never gains battle-state authority.
