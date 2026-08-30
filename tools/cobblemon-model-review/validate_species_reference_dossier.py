#!/usr/bin/env python3
"""Hard pre-model gate for same-species external skin/model research.

Production geometry must not be generated until at least three distinct external
implementations of the same Pokemon have been inspected from actual resource
files and documented with provenance, license/reuse status and concrete lessons.

This validator does not approve art and does not grant permission to reuse a
third-party asset. It only proves that the mandatory research dossier is complete.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from urllib.parse import urlparse

SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
VALID_REUSE_MODES = {"STUDY_ONLY", "LICENSED_DERIVATIVE_DONOR"}


def require_text(obj: dict, key: str, where: str) -> str:
    value = obj.get(key)
    if not isinstance(value, str) or not value.strip():
        raise SystemExit(f"{where}.{key} must be a non-empty string")
    return value.strip()


def require_url(obj: dict, key: str, where: str) -> str:
    value = require_text(obj, key, where)
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise SystemExit(f"{where}.{key} must be an http(s) URL: {value!r}")
    return value


def validate_asset_files(ref: dict, where: str) -> list[dict]:
    files = ref.get("assetFilesInspected")
    if not isinstance(files, list) or not files:
        raise SystemExit(f"{where}.assetFilesInspected must contain actual inspected asset files")

    normalized: list[dict] = []
    for index, entry in enumerate(files):
        ewhere = f"{where}.assetFilesInspected[{index}]"
        if not isinstance(entry, dict):
            raise SystemExit(f"{ewhere} must be an object")
        path = require_text(entry, "path", ewhere)
        sha = require_text(entry, "sha256", ewhere).lower()
        if not SHA256_RE.fullmatch(sha):
            raise SystemExit(f"{ewhere}.sha256 must be a lowercase 64-char SHA-256")
        kind = require_text(entry, "kind", ewhere)
        normalized.append({"path": path, "sha256": sha, "kind": kind})
    return normalized


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dossier", type=Path)
    parser.add_argument("--expected-species")
    parser.add_argument("--minimum", type=int, default=3)
    args = parser.parse_args()

    if args.minimum < 3:
        raise SystemExit("minimum reference count may not be lower than 3")
    if not args.dossier.is_file():
        raise SystemExit(f"missing dossier: {args.dossier}")

    data = json.loads(args.dossier.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise SystemExit("dossier root must be an object")

    fmt = require_text(data, "format", "dossier")
    if fmt != "ouros.cobblemon-species-reference-dossier.v1":
        raise SystemExit(f"unsupported dossier format: {fmt!r}")

    species = require_text(data, "species", "dossier").lower()
    if args.expected_species and species != args.expected_species.lower():
        raise SystemExit(
            f"species mismatch: expected={args.expected_species.lower()} dossier={species}"
        )

    dex = data.get("nationalDex")
    if not isinstance(dex, int) or dex <= 0:
        raise SystemExit("dossier.nationalDex must be a positive integer")

    references = data.get("references")
    if not isinstance(references, list) or len(references) < args.minimum:
        raise SystemExit(
            f"REFERENCE BLOCKED: need at least {args.minimum} external same-species references; "
            f"found {0 if not isinstance(references, list) else len(references)}"
        )

    ids: set[str] = set()
    fingerprints: set[str] = set()
    projects: set[str] = set()
    complete = 0
    donor_count = 0

    for index, ref in enumerate(references):
        where = f"references[{index}]"
        if not isinstance(ref, dict):
            raise SystemExit(f"{where} must be an object")

        ref_id = require_text(ref, "referenceId", where)
        if ref_id in ids:
            raise SystemExit(f"duplicate referenceId: {ref_id}")
        ids.add(ref_id)

        ref_species = require_text(ref, "species", where).lower()
        if ref_species != species:
            raise SystemExit(f"{where}.species={ref_species!r} does not match dossier species={species!r}")

        project = require_text(ref, "project", where)
        projects.add(project.casefold())
        require_url(ref, "sourceUrl", where)
        require_text(ref, "sourceVersion", where)
        require_text(ref, "implementationName", where)
        require_text(ref, "licenseStatus", where)

        status = require_text(ref, "assetInspectionStatus", where)
        if status != "COMPLETE":
            raise SystemExit(
                f"REFERENCE BLOCKED: {where} assetInspectionStatus={status!r}; actual asset-file inspection is mandatory"
            )

        files = validate_asset_files(ref, where)
        fingerprint = "|".join(sorted(f"{f['kind']}:{f['sha256']}" for f in files))
        if fingerprint in fingerprints:
            raise SystemExit(
                f"{where} duplicates an already-counted asset fingerprint; revisions/screenshots of the same asset do not count twice"
            )
        fingerprints.add(fingerprint)

        lessons = ref.get("techniqueLessons")
        if not isinstance(lessons, list) or len(lessons) < 3 or not all(
            isinstance(item, str) and item.strip() for item in lessons
        ):
            raise SystemExit(f"{where}.techniqueLessons must contain at least three concrete lessons")

        forbidden = ref.get("distinctiveElementsNotToCopy")
        if not isinstance(forbidden, list) or not forbidden or not all(
            isinstance(item, str) and item.strip() for item in forbidden
        ):
            raise SystemExit(f"{where}.distinctiveElementsNotToCopy must be a non-empty list")

        reuse_mode = require_text(ref, "reuseMode", where)
        if reuse_mode not in VALID_REUSE_MODES:
            raise SystemExit(f"{where}.reuseMode must be one of {sorted(VALID_REUSE_MODES)}")

        license_info = ref.get("license")
        if not isinstance(license_info, dict):
            raise SystemExit(f"{where}.license must be an object")
        require_text(license_info, "name", f"{where}.license")
        require_url(license_info, "sourceUrl", f"{where}.license")

        if reuse_mode == "LICENSED_DERIVATIVE_DONOR":
            if license_info.get("allowsDerivatives") is not True:
                raise SystemExit(f"{where}: derivative donor requires license.allowsDerivatives=true")
            if license_info.get("allowsRedistribution") is not True:
                raise SystemExit(f"{where}: derivative donor requires license.allowsRedistribution=true")
            attribution = require_text(ref, "requiredAttribution", where)
            if not attribution:
                raise SystemExit(f"{where}.requiredAttribution is required for derivative donor")
            donor_count += 1
        else:
            if license_info.get("allowsDerivatives") is True and license_info.get("allowsRedistribution") is True:
                # Study-only is still allowed for permissive assets; this is informational only.
                pass

        complete += 1

    if complete < args.minimum:
        raise SystemExit(f"REFERENCE BLOCKED: only {complete} complete references")

    if len(projects) < 2:
        raise SystemExit(
            "REFERENCE BLOCKED: the three references must cover at least two independent external projects"
        )

    report = {
        "status": "PASS",
        "species": species,
        "nationalDex": dex,
        "completeReferenceCount": complete,
        "distinctProjectCount": len(projects),
        "licensedDerivativeDonorCount": donor_count,
        "productionModelingGate": "OPEN",
        "artApproval": "NOT_EVALUATED",
    }
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
