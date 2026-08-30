#!/usr/bin/env python3
"""Validate and fingerprint Blockbench evidence for a professional skin review.

The visual metrics are intentionally only a technical floor against trivial/no-op
changes. They do not score artistic quality and never grant owner approval.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

from PIL import Image, ImageChops


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def image_difference(official_path: Path, hero_path: Path) -> tuple[float, float | None, dict]:
    official = Image.open(official_path).convert("RGBA")
    hero = Image.open(hero_path).convert("RGBA")
    if official.size != hero.size:
        raise SystemExit(f"matched-camera images differ in size: official={official.size} hero={hero.size}")

    diff = ImageChops.difference(official, hero)
    diff_pixels = list(diff.getdata())
    official_pixels = list(official.getdata())
    hero_pixels = list(hero.getdata())

    official_mask = [pixel[3] > 0 for pixel in official_pixels]
    hero_mask = [pixel[3] > 0 for pixel in hero_pixels]
    union_count = sum(1 for a, b in zip(official_mask, hero_mask, strict=True) if a or b)
    denominator = union_count if union_count else official.width * official.height
    changed = sum(1 for pixel in diff_pixels if max(pixel) > 12)
    pixel_ratio = changed / denominator

    has_transparency = any(not value for value in official_mask) and any(not value for value in hero_mask)
    silhouette_ratio = None
    if has_transparency:
        symmetric = sum(1 for a, b in zip(official_mask, hero_mask, strict=True) if a != b)
        official_foreground = max(1, sum(official_mask))
        silhouette_ratio = symmetric / official_foreground

    return pixel_ratio, silhouette_ratio, {
        "dimensions": [official.width, official.height],
        "comparisonDenominatorPixels": denominator,
        "changedPixelsAboveTolerance": changed,
        "alphaSilhouetteAvailable": has_transparency,
    }


def png_dimensions(path: Path) -> list[int]:
    image = Image.open(path)
    return [image.width, image.height]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--head-sha", required=True)
    args = parser.parse_args()

    if not re.fullmatch(r"[0-9a-f]{40}", args.head_sha.lower()):
        raise SystemExit("--head-sha must be a 40-character commit SHA")

    data = json.loads(args.manifest.read_text(encoding="utf-8"))
    out = args.output_dir
    if not out.is_dir():
        raise SystemExit(f"missing Blockbench output directory: {out}")

    blockbench = data["blockbench"]
    required_names = blockbench["requiredEvidenceNames"]
    evidence = data["evidence"]
    required_files = set(evidence["requiredFiles"])
    generated_here = {
        evidence["reviewContractFile"],
        evidence["pngHashManifestFile"],
        "evidence-set.json",
    }

    missing = [name for name in sorted(required_files - generated_here) if not (out / name).is_file()]
    if missing:
        raise SystemExit(f"BLOCKBENCH EVIDENCE FAIL: required pre-validation files missing: {missing}")

    png_records = []
    for name in sorted(required_names):
        path = out / name
        if not path.is_file():
            raise SystemExit(f"BLOCKBENCH EVIDENCE FAIL: missing required PNG {name}")
        dimensions = png_dimensions(path)
        if "gameplay_" in name:
            expected = blockbench["gameplayResolution"]
            if dimensions != [expected, expected]:
                raise SystemExit(
                    f"BLOCKBENCH EVIDENCE FAIL: {name} must be {expected}x{expected}, got {dimensions}"
                )
        elif dimensions != [1024, 1024]:
            raise SystemExit(f"BLOCKBENCH EVIDENCE FAIL: {name} must be 1024x1024, got {dimensions}")
        png_records.append({"name": name, "sha256": sha256(path), "dimensions": dimensions, "bytes": path.stat().st_size})

    official = out / "official_reference_three_quarter.png"
    hero = out / "hero_three_quarter.png"
    pixel_ratio, silhouette_ratio, comparison_meta = image_difference(official, hero)
    floor = blockbench["technicalVisualFloor"]
    min_pixel = float(floor["minimumPixelDifferenceRatio"])
    min_silhouette = float(floor["minimumSilhouetteDeltaRatio"])
    if pixel_ratio < min_pixel:
        raise SystemExit(
            "BLOCKBENCH VISUAL FLOOR FAIL: candidate is too close to official matched-camera render; "
            f"pixelDifferenceRatio={pixel_ratio:.4f} minimum={min_pixel:.4f}"
        )
    if min_silhouette > 0:
        if silhouette_ratio is None:
            raise SystemExit(
                "BLOCKBENCH VISUAL FLOOR FAIL: silhouette threshold requested but screenshots do not expose an alpha silhouette"
            )
        if silhouette_ratio < min_silhouette:
            raise SystemExit(
                "BLOCKBENCH VISUAL FLOOR FAIL: silhouette change is below declared floor; "
                f"silhouetteDeltaRatio={silhouette_ratio:.4f} minimum={min_silhouette:.4f}"
            )

    hash_lines = [f"{record['sha256']}  {record['name']}" for record in png_records]
    hash_manifest = out / evidence["pngHashManifestFile"]
    hash_manifest.write_text("\n".join(hash_lines) + "\n", encoding="utf-8")

    evidence_set_seed = "\n".join(sorted(f"{record['name']}:{record['sha256']}" for record in png_records)).encode("utf-8")
    evidence_set_sha = hashlib.sha256(evidence_set_seed).hexdigest()

    contract = {
        "format": "ouros.cobblemon-blockbench-evidence.v1",
        "species": data["species"],
        "concept": data["concept"],
        "headSha": args.head_sha.lower(),
        "viewer": "Blockbench",
        "viewerVersion": blockbench["version"],
        "viewerAppImageSha256": blockbench["appImageSha256"],
        "matchedCamera": blockbench["matchedCamera"],
        "gameplayResolution": blockbench["gameplayResolution"],
        "artStatus": data["artStatus"],
        "ownerApproval": data["ownerApproval"],
        "artApprovalGrantedByTooling": False,
        "technicalVisualFloor": {
            "pixelDifferenceRatio": round(pixel_ratio, 6),
            "minimumPixelDifferenceRatio": min_pixel,
            "silhouetteDeltaRatio": None if silhouette_ratio is None else round(silhouette_ratio, 6),
            "minimumSilhouetteDeltaRatio": min_silhouette,
            **comparison_meta,
        },
        "pngs": png_records,
        "evidenceSetSha256": evidence_set_sha,
        "note": "Metrics only reject trivial/no-op visual deltas. They do not evaluate premium art quality or owner acceptance.",
    }
    contract_path = out / evidence["reviewContractFile"]
    contract_path.write_text(json.dumps(contract, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (out / "evidence-set.json").write_text(json.dumps(contract, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    missing_after = [name for name in sorted(required_files) if not (out / name).is_file()]
    if missing_after:
        raise SystemExit(f"BLOCKBENCH EVIDENCE FAIL: required final files missing after contract generation: {missing_after}")

    print(json.dumps(contract, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
