#!/usr/bin/env python3
"""Validate pre-production Blockbench-authored cosmetic patch sources.

This validator is a rejection floor only. It cannot approve art. A passing patch
still requires real Blockbench review before production materialization.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

FORMAT = "ouros.blockbench-authored-cosmetic-patch.v1"
ALLOWED_ART_STATES = {"ARTISTIC FAIL", "USER REJECTED — REWORK REQUIRED", "OWNER REVIEW REQUIRED"}


def fail(message: str) -> None:
    raise SystemExit(message)


def vec3(value, where: str) -> list[float]:
    if not isinstance(value, list) or len(value) != 3 or not all(isinstance(v, (int, float)) for v in value):
        fail(f"{where} must be a numeric vec3")
    return [float(v) for v in value]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("patch", type=Path)
    parser.add_argument("--expected-species")
    args = parser.parse_args()

    data = json.loads(args.patch.read_text(encoding="utf-8"))
    if data.get("format") != FORMAT:
        fail(f"unsupported patch format: {data.get('format')!r}")

    species = str(data.get("species", "")).lower()
    if not species:
        fail("patch.species is required")
    if args.expected_species and species != args.expected_species.lower():
        fail(f"species mismatch: expected {args.expected_species.lower()} got {species}")

    if data.get("artStatus") not in ALLOWED_ART_STATES:
        fail("patch.artStatus may not claim owner approval")
    if data.get("reviewState") != "BLOCKBENCH_REVIEW_PENDING":
        fail("pre-production authored patch must remain BLOCKBENCH_REVIEW_PENDING")

    baseline = data.get("officialBaseline")
    if not isinstance(baseline, dict):
        fail("patch.officialBaseline must be an object")
    sha = baseline.get("modelSha256")
    if not isinstance(sha, str) or len(sha) != 64:
        fail("officialBaseline.modelSha256 must be SHA-256")
    if baseline.get("officialBoneCount") != 87 and species == "lucario":
        fail("Lucario authored patch must target the exact 87-bone official baseline")

    rules = data.get("designRules")
    if not isinstance(rules, dict):
        fail("patch.designRules must be an object")
    if rules.get("inheritRejectedCosmeticGeometry") is not False:
        fail("rejected cosmetic geometry may not be inherited")
    if rules.get("dominantRectangularApronFaceAllowed") is not False:
        fail("dominant rectangular apron faces are forbidden")
    if rules.get("stackedCapSlabsAllowed") is not False:
        fail("stacked cap slabs are forbidden")

    bones = data.get("bones")
    if not isinstance(bones, list) or not bones:
        fail("patch.bones must contain cosmetic bones")

    names: set[str] = set()
    size_fingerprints: Counter[tuple[float, float, float]] = Counter()
    total_cubes = 0
    thin_cloth_cubes = 0
    official_roots = 0

    for bi, bone in enumerate(bones):
        where = f"bones[{bi}]"
        if not isinstance(bone, dict):
            fail(f"{where} must be an object")
        name = bone.get("name")
        if not isinstance(name, str) or not name.startswith("ouros_"):
            fail(f"{where}.name must start with ouros_")
        if name in names:
            fail(f"duplicate cosmetic bone name: {name}")
        names.add(name)

        parent = bone.get("parent")
        if not isinstance(parent, str) or not parent:
            fail(f"{where}.parent is required")
        if not parent.startswith("ouros_"):
            official_roots += 1

        vec3(bone.get("pivot"), f"{where}.pivot")
        cubes = bone.get("cubes")
        if not isinstance(cubes, list) or not cubes:
            fail(f"{where}.cubes must be non-empty")

        for ci, cube in enumerate(cubes):
            cwhere = f"{where}.cubes[{ci}]"
            if not isinstance(cube, dict):
                fail(f"{cwhere} must be an object")
            vec3(cube.get("origin"), f"{cwhere}.origin")
            size = vec3(cube.get("size"), f"{cwhere}.size")
            if any(v <= 0 for v in size):
                fail(f"{cwhere}.size must be positive")
            if max(size) > 14:
                fail(f"{cwhere} exceeds the V41 macro-form size ceiling; rebuild instead of using a giant slab")
            if min(size) <= 0.6:
                thin_cloth_cubes += 1
            size_fingerprints[tuple(round(v, 2) for v in size)] += 1
            total_cubes += 1

    repeated_large = [
        (size, count)
        for size, count in size_fingerprints.items()
        if count >= 3 and max(size) >= 4 and min(size) >= 1
    ]
    if repeated_large:
        fail(f"repeated large cuboid pattern detected: {repeated_large}")

    if official_roots < 3:
        fail("patch needs several direct official-bone roots so costume systems move with anatomy")
    if total_cubes < 12:
        fail("patch is too small to represent the required cap + garment macro-form slice")
    if thin_cloth_cubes / total_cubes < 0.45:
        fail("less than 45% of V41 elements are thin cloth-like forms; risk of reverting to box construction")

    metrics = data.get("sliceMetrics", {})
    if metrics.get("cosmeticBoneCount") != len(bones):
        fail("sliceMetrics.cosmeticBoneCount does not match authored bones")
    if metrics.get("cosmeticCubeCount") != total_cubes:
        fail("sliceMetrics.cosmeticCubeCount does not match authored cubes")

    print(json.dumps({
        "status": "PASS",
        "artApproval": "NOT_GRANTED",
        "species": species,
        "cosmeticBoneCount": len(bones),
        "cosmeticCubeCount": total_cubes,
        "thinClothCubeCount": thin_cloth_cubes,
        "officialRootCount": official_roots,
        "blockbenchReview": "REQUIRED_BEFORE_PRODUCTION"
    }, indent=2))


if __name__ == "__main__":
    main()
