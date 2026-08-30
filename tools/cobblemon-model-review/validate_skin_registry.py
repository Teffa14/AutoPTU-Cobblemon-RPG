#!/usr/bin/env python3
"""Validate the single source of truth for every repository Cobblemon skin."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

FORMAT = "ouros.cobblemon-skin-registry.v1"
AUTHORITY = "PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE"
LEGACY_VISUAL_EVIDENCE_ROOT = Path("test-evidence/visual/cobblemon-skins")
LIFECYCLES = {
    "REFERENCE_BLOCKED",
    "REFERENCE_READY",
    "LEGACY_QUARANTINED",
    "PROFESSIONAL_CANDIDATE",
    "OWNER_APPROVED_RELEASE",
}
NON_RELEASE_ART_STATES = {
    "ARTISTIC FAIL",
    "USER REJECTED — REWORK REQUIRED",
    "OWNER REVIEW REQUIRED",
}
MODEL_RE = re.compile(
    r"^fabric-adapter/src/main/resources/assets/(?:autoptu|cobblemon)/bedrock/pokemon/models/"
    r"(?P<slug>\d{4}_[^/]+)/ouros_[^/]+\.geo\.json$"
)


def fail(message: str) -> None:
    raise SystemExit(f"SKIN REGISTRY FAIL: {message}")


def repo_path(root: Path, raw: str, field: str) -> Path:
    value = Path(raw)
    if value.is_absolute() or ".." in value.parts:
        fail(f"{field} must be a safe repository-relative path")
    resolved = (root / value).resolve()
    if resolved != root and root not in resolved.parents:
        fail(f"{field} escapes the repository root")
    return resolved


def production_slugs(root: Path) -> set[str]:
    base = root / "fabric-adapter/src/main/resources/assets"
    result: set[str] = set()
    if not base.is_dir():
        return result
    for path in base.rglob("ouros_*.geo.json"):
        raw = path.relative_to(root).as_posix()
        match = MODEL_RE.match(raw)
        if match:
            result.add(match.group("slug"))
    return result


def run_validator(command: list[str], root: Path, failure: str) -> None:
    result = subprocess.run(command, cwd=root, text=True)
    if result.returncode:
        fail(failure)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--registry",
        type=Path,
        default=Path("docs/cobblemon-skin-registry.json"),
    )
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument("--skip-manifest-validation", action="store_true")
    args = parser.parse_args()

    root = args.repo_root.resolve()
    legacy_visual_root = root / LEGACY_VISUAL_EVIDENCE_ROOT
    if legacy_visual_root.exists():
        legacy_files = sorted(
            path.relative_to(root).as_posix()
            for path in legacy_visual_root.rglob("*")
            if path.is_file()
        )
        if legacy_files:
            preview = legacy_files[:8]
            suffix = "" if len(legacy_files) <= len(preview) else f" (+{len(legacy_files) - len(preview)} more)"
            fail(
                "legacy rejected skin review galleries must not be committed under "
                f"{LEGACY_VISUAL_EVIDENCE_ROOT.as_posix()}; use immutable GitHub Actions review artifacts instead. "
                f"files={preview}{suffix}"
            )

    registry_path = args.registry if args.registry.is_absolute() else root / args.registry
    try:
        data = json.loads(registry_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"cannot read registry: {exc}")

    if data.get("format") != FORMAT:
        fail(f"format must be {FORMAT}")
    if data.get("authorityBoundary") != AUTHORITY:
        fail("authorityBoundary must preserve AutoPTU/Ouros authority")
    if data.get("releasePolicy") != "OWNER_APPROVED_EXACT_HEAD_AND_EVIDENCE_ONLY":
        fail("releasePolicy must require exact owner-approved head and evidence")
    entries = data.get("entries")
    if not isinstance(entries, list) or not entries:
        fail("entries must be a non-empty list")

    by_slug: dict[str, dict] = {}
    for index, entry in enumerate(entries):
        where = f"entries[{index}]"
        if not isinstance(entry, dict):
            fail(f"{where} must be an object")
        slug = entry.get("slug")
        species = entry.get("species")
        dex = entry.get("nationalDex")
        if not isinstance(slug, str) or not re.fullmatch(r"\d{4}_[a-z0-9_]+", slug):
            fail(f"{where}.slug is invalid")
        if slug in by_slug:
            fail(f"duplicate slug {slug}")
        if not isinstance(species, str) or not species or slug.split("_", 1)[1] != species:
            fail(f"{where}.species must match the slug")
        if not isinstance(dex, int) or dex <= 0 or int(slug[:4]) != dex:
            fail(f"{where}.nationalDex must match the slug")
        if not isinstance(entry.get("concept"), str) or not entry["concept"].strip():
            fail(f"{where}.concept must be non-empty")

        lifecycle = entry.get("lifecycle")
        art_status = entry.get("artStatus")
        if lifecycle not in LIFECYCLES:
            fail(f"{where}.lifecycle must be one of {sorted(LIFECYCLES)}")
        if not isinstance(entry.get("saleEligible"), bool):
            fail(f"{where}.saleEligible must be boolean")
        manifest = entry.get("manifest")
        dossier = entry.get("referenceDossier")

        documentation = entry.get("documentation")
        documentation_path = repo_path(root, documentation, f"{where}.documentation") if isinstance(documentation, str) else None
        if documentation_path is None or not documentation_path.is_file():
            fail(f"{where}.documentation must point to an existing file")
        if not isinstance(entry.get("blocker"), str) or not entry["blocker"].strip():
            fail(f"{where}.blocker must explain the current gate")

        if lifecycle == "OWNER_APPROVED_RELEASE":
            if entry["saleEligible"] is not True or art_status != "OWNER APPROVED":
                fail(f"{where}: only OWNER_APPROVED_RELEASE may be sale-eligible")
        else:
            if entry["saleEligible"] is not False:
                fail(f"{where}: non-release lifecycle cannot be sale-eligible")
            if art_status not in NON_RELEASE_ART_STATES:
                fail(f"{where}: invalid non-release artStatus")

        header = "\n".join(documentation_path.read_text(encoding="utf-8").splitlines()[:12])
        if f"Status: {art_status}" not in header:
            fail(f"{where}.documentation must expose the registry artStatus near the top")
        expected_sale_line = "Sale eligibility: ELIGIBLE." if entry["saleEligible"] else "Sale eligibility: NOT ELIGIBLE."
        if expected_sale_line not in header:
            fail(f"{where}.documentation must expose {expected_sale_line!r} near the top")

        if lifecycle in {"PROFESSIONAL_CANDIDATE", "OWNER_APPROVED_RELEASE"}:
            if not isinstance(manifest, str):
                fail(f"{where}: professional lifecycle requires a manifest")
            manifest_path = repo_path(root, manifest, f"{where}.manifest")
            if not manifest_path.is_file():
                fail(f"{where}: manifest does not exist: {manifest}")
            if not args.skip_manifest_validation:
                run_validator(
                    [
                        sys.executable,
                        "tools/cobblemon-model-review/validate_professional_skin_manifest.py",
                        manifest,
                        "--repo-root",
                        str(root),
                    ],
                    root,
                    f"professional manifest failed for {slug}",
                )
        elif manifest is not None:
            fail(f"{where}: blocked/quarantined entries cannot advertise a professional manifest")

        if lifecycle in {"REFERENCE_BLOCKED", "REFERENCE_READY"}:
            if not isinstance(dossier, str):
                fail(f"{where}: REFERENCE_BLOCKED requires a dossier")
            dossier_path = repo_path(root, dossier, f"{where}.referenceDossier")
            if not dossier_path.is_file():
                fail(f"{where}: reference dossier does not exist")
            command = [
                sys.executable,
                "tools/cobblemon-model-review/validate_species_reference_dossier.py",
                str(dossier_path),
                "--expected-species",
                species,
            ]
            if lifecycle == "REFERENCE_BLOCKED":
                command.append("--allow-blocked")
            run_validator(
                command,
                root,
                f"blocked reference dossier failed validation for {slug}",
            )
        elif dossier is not None:
            dossier_path = repo_path(root, dossier, f"{where}.referenceDossier")
            if not dossier_path.is_file():
                fail(f"{where}: reference dossier does not exist")

        by_slug[slug] = entry

    active = data.get("activeSlice")
    if active not in by_slug:
        fail("activeSlice must name exactly one registered skin")
    if by_slug[active]["lifecycle"] not in {"REFERENCE_BLOCKED", "REFERENCE_READY", "PROFESSIONAL_CANDIDATE"}:
        fail("activeSlice must be reference-blocked, reference-ready, or a professional candidate")

    on_disk = production_slugs(root)
    registered = set(by_slug)
    if on_disk != registered:
        missing = sorted(on_disk - registered)
        stale = sorted(registered - on_disk)
        fail(f"registry/on-disk coverage mismatch missing={missing} stale={stale}")

    dossier_dir = root / "docs/cobblemon-skin-reference-dossiers"
    dossiers_on_disk = {
        path.relative_to(root).as_posix()
        for path in dossier_dir.glob("[0-9][0-9][0-9][0-9]_*.json")
    }
    dossiers_registered = {
        entry["referenceDossier"]
        for entry in entries
        if isinstance(entry.get("referenceDossier"), str)
    }
    if dossiers_on_disk != dossiers_registered:
        fail(
            "reference dossier coverage mismatch "
            f"unregistered={sorted(dossiers_on_disk - dossiers_registered)} "
            f"missing={sorted(dossiers_registered - dossiers_on_disk)}"
        )

    print(
        json.dumps(
            {
                "status": "PASS",
                "activeSlice": active,
                "registeredSkins": len(entries),
                "saleEligible": sorted(
                    slug for slug, entry in by_slug.items() if entry["saleEligible"]
                ),
                "professionalCandidates": sorted(
                    slug
                    for slug, entry in by_slug.items()
                    if entry["lifecycle"] == "PROFESSIONAL_CANDIDATE"
                ),
                "referenceReady": sorted(
                    slug
                    for slug, entry in by_slug.items()
                    if entry["lifecycle"] == "REFERENCE_READY"
                ),
                "quarantinedOrBlocked": sorted(
                    slug
                    for slug, entry in by_slug.items()
                    if entry["lifecycle"] in {"LEGACY_QUARANTINED", "REFERENCE_BLOCKED"}
                ),
            },
            indent=2,
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
