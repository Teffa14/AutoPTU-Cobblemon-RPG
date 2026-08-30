#!/usr/bin/env python3
"""Fail closed unless Storm Courier v4 is derived from exact official Pikachu models.

This validator is presentation-only. It validates provenance and additive geometry;
it never reads Cobblemon battle state or treats Minecraft/Cobblemon as tactical
authority.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

OFFICIAL_MODEL_SHA256 = {
    "male": "f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83",
    "female": "d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318",
}
EXPECTED_ORIGINAL_BONES = 90


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def geometry(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("minecraft:geometry")
    if not isinstance(entries, list) or len(entries) != 1:
        raise ValueError(f"{path}: expected exactly one minecraft:geometry entry")
    return entries[0]


def validate_pair(sex: str, official_path: Path, derived_path: Path) -> dict:
    actual_hash = sha256(official_path)
    expected_hash = OFFICIAL_MODEL_SHA256[sex]
    if actual_hash != expected_hash:
        raise ValueError(
            f"{sex}: official source SHA-256 mismatch; expected {expected_hash}, got {actual_hash}"
        )

    official = geometry(official_path)
    derived = geometry(derived_path)
    official_bones = official.get("bones")
    derived_bones = derived.get("bones")

    if not isinstance(official_bones, list) or len(official_bones) != EXPECTED_ORIGINAL_BONES:
        raise ValueError(
            f"{sex}: official model must contain {EXPECTED_ORIGINAL_BONES} bones; "
            f"found {len(official_bones) if isinstance(official_bones, list) else 'invalid'}"
        )
    if not isinstance(derived_bones, list) or len(derived_bones) <= EXPECTED_ORIGINAL_BONES:
        raise ValueError(f"{sex}: derived model has no additive Ouros cosmetic geometry")

    # Geometry identifier is the only permitted mutation outside the appended bones.
    official_without_identifier = json.loads(json.dumps(official))
    derived_original_only = json.loads(json.dumps(derived))
    official_without_identifier["description"].pop("identifier", None)
    derived_original_only["description"].pop("identifier", None)
    derived_original_only["bones"] = derived_original_only["bones"][:EXPECTED_ORIGINAL_BONES]

    if official_without_identifier != derived_original_only:
        raise ValueError(
            f"{sex}: original geometry drift detected outside the permitted derived identifier"
        )

    if derived_bones[:EXPECTED_ORIGINAL_BONES] != official_bones:
        raise ValueError(f"{sex}: original bone objects or order changed")

    cosmetic = derived_bones[EXPECTED_ORIGINAL_BONES:]
    names = [bone.get("name") for bone in cosmetic]
    if any(not isinstance(name, str) or not name.startswith("ouros_") for name in names):
        raise ValueError(f"{sex}: every appended cosmetic bone must use the ouros_* namespace")
    if len(names) != len(set(names)):
        raise ValueError(f"{sex}: duplicate cosmetic bone names detected")

    original_names = {bone.get("name") for bone in official_bones}
    for bone in cosmetic:
        parent = bone.get("parent")
        if parent not in original_names and parent not in names:
            raise ValueError(f"{sex}: cosmetic bone {bone.get('name')} has unknown parent {parent}")

    return {
        "sex": sex,
        "officialSha256": actual_hash,
        "originalBonesPreserved": EXPECTED_ORIGINAL_BONES,
        "cosmeticBoneCount": len(cosmetic),
        "cosmeticBones": names,
        "derivedIdentifier": derived.get("description", {}).get("identifier"),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--male-official", type=Path, required=True)
    parser.add_argument("--female-official", type=Path, required=True)
    parser.add_argument("--male-derived", type=Path, required=True)
    parser.add_argument("--female-derived", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    report = {
        "contract": "Storm Courier v4 official-source additive-geometry gate",
        "cobblemon": "1.7.3 Fabric / Minecraft 1.21.1 / Modrinth kF7CvxTo",
        "variants": [
            validate_pair("male", args.male_official, args.male_derived),
            validate_pair("female", args.female_official, args.female_derived),
        ],
    }

    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8")
    print(rendered, end="")


if __name__ == "__main__":
    main()
