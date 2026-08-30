#!/usr/bin/env python3
"""Validate the reproducibility/provenance contract for one production Cobblemon skin.

This is deliberately a technical floor. It can prove exact files, hashes, source
provenance, builder reproducibility metadata, Blockbench evidence requirements and
owner-approval state. It never grants artistic approval.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
SHA512_RE = re.compile(r"^[0-9a-f]{128}$")
SHA40_RE = re.compile(r"^[0-9a-f]{40}$")
FORMAT = "ouros.cobblemon-professional-skin-review.v1"
ALLOWED_ART_STATES = {
    "ARTISTIC FAIL",
    "USER REJECTED — REWORK REQUIRED",
    "OWNER REVIEW REQUIRED",
    "OWNER APPROVED",
}
ALLOWED_DERIVATIONS = {"OFFICIAL_IDENTICAL", "DERIVED_FROM_OFFICIAL", "ACCESSORY_OVERLAY"}
REQUIRED_TECHNICAL_CHECKS = {
    "REFERENCE_DOSSIER",
    "OFFICIAL_SOURCE_HASHES",
    "ORIGINAL_BONE_EQUALITY",
    "COSMETIC_ATTACHMENT",
    "BUILDER_REPRODUCIBILITY",
    "BLOCKBENCH_MATCHED_CAMERA",
    "GAMEPLAY_SCALE_EVIDENCE",
    "PLAYABLE_TEST_BUILD",
    "INTEGRATION_CORE_CI",
}


def die(message: str) -> None:
    raise SystemExit(message)


def require_dict(obj: dict, key: str, where: str) -> dict:
    value = obj.get(key)
    if not isinstance(value, dict):
        die(f"{where}.{key} must be an object")
    return value


def require_list(obj: dict, key: str, where: str) -> list:
    value = obj.get(key)
    if not isinstance(value, list):
        die(f"{where}.{key} must be a list")
    return value


def require_text(obj: dict, key: str, where: str) -> str:
    value = obj.get(key)
    if not isinstance(value, str) or not value.strip():
        die(f"{where}.{key} must be a non-empty string")
    return value.strip()


def require_hash(obj: dict, key: str, where: str, regex: re.Pattern[str]) -> str:
    value = require_text(obj, key, where).lower()
    if not regex.fullmatch(value):
        die(f"{where}.{key} has invalid hash format")
    return value


def safe_repo_path(root: Path, raw: str, where: str) -> Path:
    path = Path(raw)
    if path.is_absolute() or ".." in path.parts:
        die(f"{where} must be a repository-relative path without '..': {raw!r}")
    resolved = (root / path).resolve()
    root_resolved = root.resolve()
    if resolved != root_resolved and root_resolved not in resolved.parents:
        die(f"{where} escapes repository root: {raw!r}")
    return resolved


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_repo_file(root: Path, raw_path: str, expected_sha: str, where: str) -> Path:
    path = safe_repo_path(root, raw_path, where)
    if not path.is_file():
        die(f"{where}: missing repository file {raw_path}")
    actual = sha256(path)
    if actual != expected_sha:
        die(f"{where}: SHA-256 mismatch expected={expected_sha} actual={actual} path={raw_path}")
    return path


def load_candidate_geometry(path: Path) -> tuple[int, list[dict]]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        die(f"production model is not valid UTF-8 JSON: {exc}")
    geometries = payload.get("minecraft:geometry") if isinstance(payload, dict) else None
    if not isinstance(geometries, list) or len(geometries) != 1:
        die("production model must contain exactly one minecraft:geometry entry")
    bones = geometries[0].get("bones") if isinstance(geometries[0], dict) else None
    if not isinstance(bones, list):
        die("production model has no bones list")
    return len(bones), bones


def run_reference_gate(root: Path, dossier_path: Path, species: str) -> None:
    validator = root / "tools/cobblemon-model-review/validate_species_reference_dossier.py"
    if not validator.is_file():
        die(f"missing reference validator: {validator}")
    result = subprocess.run(
        [sys.executable, str(validator), str(dossier_path), "--expected-species", species],
        cwd=root,
        text=True,
    )
    if result.returncode:
        die("professional manifest cannot open production because the strict same-species reference gate failed")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument("--skip-reference-validator", action="store_true")
    args = parser.parse_args()

    root = args.repo_root.resolve()
    manifest_path = args.manifest
    if not manifest_path.is_absolute():
        manifest_path = (root / manifest_path).resolve()
    if not manifest_path.is_file():
        die(f"missing professional skin manifest: {manifest_path}")

    try:
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        die(f"invalid professional manifest JSON: {exc}")
    if not isinstance(data, dict):
        die("professional manifest root must be an object")

    if require_text(data, "format", "manifest") != FORMAT:
        die(f"manifest.format must be {FORMAT}")

    species = require_text(data, "species", "manifest").lower()
    dex = data.get("nationalDex")
    if not isinstance(dex, int) or dex <= 0:
        die("manifest.nationalDex must be a positive integer")
    require_text(data, "concept", "manifest")
    if require_text(data, "authorityBoundary", "manifest") != "PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE":
        die("manifest.authorityBoundary must preserve AutoPTU/Ouros battle authority")

    art_status = require_text(data, "artStatus", "manifest")
    if art_status not in ALLOWED_ART_STATES:
        die(f"manifest.artStatus must be one of {sorted(ALLOWED_ART_STATES)}")

    approval = require_dict(data, "ownerApproval", "manifest")
    if approval.get("required") is not True:
        die("manifest.ownerApproval.required must be true")
    approved = approval.get("approved")
    if not isinstance(approved, bool):
        die("manifest.ownerApproval.approved must be boolean")
    if art_status == "OWNER APPROVED":
        if approved is not True:
            die("OWNER APPROVED requires ownerApproval.approved=true")
        if not SHA40_RE.fullmatch(require_text(approval, "approvedHeadSha", "manifest.ownerApproval").lower()):
            die("ownerApproval.approvedHeadSha must be a 40-char commit SHA")
        require_hash(approval, "evidenceSetSha256", "manifest.ownerApproval", SHA256_RE)
        require_text(approval, "approvalRecord", "manifest.ownerApproval")
    elif approved:
        die("tooling cannot mark ownerApproval.approved=true unless artStatus is OWNER APPROVED")

    dossier_raw = require_text(data, "referenceDossier", "manifest")
    dossier_path = safe_repo_path(root, dossier_raw, "manifest.referenceDossier")
    if not dossier_path.is_file():
        die(f"manifest.referenceDossier missing: {dossier_raw}")
    if not args.skip_reference_validator:
        run_reference_gate(root, dossier_path, species)

    official = require_dict(data, "officialSource", "manifest")
    require_text(official, "modrinthVersionId", "manifest.officialSource")
    require_text(official, "version", "manifest.officialSource")
    require_text(official, "minecraftVersion", "manifest.officialSource")
    require_text(official, "loader", "manifest.officialSource")
    require_text(official, "jarFilename", "manifest.officialSource")
    require_hash(official, "jarSha256", "manifest.officialSource", SHA256_RE)
    require_hash(official, "jarSha512", "manifest.officialSource", SHA512_RE)
    require_text(official, "modelPath", "manifest.officialSource")
    require_hash(official, "modelSha256", "manifest.officialSource", SHA256_RE)
    official_bone_count = official.get("officialBoneCount")
    if not isinstance(official_bone_count, int) or official_bone_count <= 0:
        die("manifest.officialSource.officialBoneCount must be a positive integer")
    reference_texture = require_dict(official, "referenceTexture", "manifest.officialSource")
    require_text(reference_texture, "path", "manifest.officialSource.referenceTexture")
    reference_texture_sha = require_hash(reference_texture, "sha256", "manifest.officialSource.referenceTexture", SHA256_RE)
    require_text(official, "animationPath", "manifest.officialSource")
    require_hash(official, "animationSha256", "manifest.officialSource", SHA256_RE)

    production = require_dict(data, "production", "manifest")
    model_path_raw = require_text(production, "modelPath", "manifest.production")
    model_sha = require_hash(production, "modelSha256", "manifest.production", SHA256_RE)
    model_path = validate_repo_file(root, model_path_raw, model_sha, "manifest.production.modelPath")

    declared_total = production.get("productionBoneCount")
    declared_cosmetic = production.get("cosmeticBoneCount")
    declared_cubes = production.get("cosmeticCubeCount")
    for key, value in (
        ("productionBoneCount", declared_total),
        ("cosmeticBoneCount", declared_cosmetic),
        ("cosmeticCubeCount", declared_cubes),
    ):
        if not isinstance(value, int) or value < 0:
            die(f"manifest.production.{key} must be a non-negative integer")

    actual_total, bones = load_candidate_geometry(model_path)
    if actual_total != declared_total:
        die(f"production bone count drift: manifest={declared_total} actual={actual_total}")
    if declared_total != official_bone_count + declared_cosmetic:
        die(
            "productionBoneCount must equal officialBoneCount + cosmeticBoneCount: "
            f"{declared_total} != {official_bone_count} + {declared_cosmetic}"
        )
    extras = bones[official_bone_count:]
    bad_extra_names = [bone.get("name") for bone in extras if not str(bone.get("name", "")).startswith("ouros_")]
    if bad_extra_names:
        die(f"production model contains appended non-ouros bones: {bad_extra_names}")
    actual_cubes = sum(len(bone.get("cubes", [])) for bone in extras if isinstance(bone, dict) and isinstance(bone.get("cubes"), list))
    if actual_cubes != declared_cubes:
        die(f"cosmetic cube count drift: manifest={declared_cubes} actual={actual_cubes}")

    textures = require_list(production, "textures", "manifest.production")
    if not textures:
        die("manifest.production.textures must contain BODY plus optional OVERLAY entries")
    body_entries = []
    derived_body = False
    for index, entry in enumerate(textures):
        where = f"manifest.production.textures[{index}]"
        if not isinstance(entry, dict):
            die(f"{where} must be an object")
        role = require_text(entry, "role", where)
        if role not in {"BODY", "OVERLAY"}:
            die(f"{where}.role must be BODY or OVERLAY")
        derivation = require_text(entry, "derivation", where)
        if derivation not in ALLOWED_DERIVATIONS:
            die(f"{where}.derivation must be one of {sorted(ALLOWED_DERIVATIONS)}")
        raw_path = require_text(entry, "path", where)
        expected = require_hash(entry, "sha256", where, SHA256_RE)
        validate_repo_file(root, raw_path, expected, f"{where}.path")
        if role == "BODY":
            body_entries.append(entry)
            if derivation == "ACCESSORY_OVERLAY":
                die(f"{where}: BODY cannot use ACCESSORY_OVERLAY derivation")
            if derivation == "OFFICIAL_IDENTICAL" and expected != reference_texture_sha:
                die(f"{where}: OFFICIAL_IDENTICAL BODY hash must equal official reference texture hash")
            if derivation == "DERIVED_FROM_OFFICIAL":
                derived_body = True
                require_hash(entry, "officialBaselineSha256", where, SHA256_RE)
                metadata_raw = require_text(entry, "derivedMetadataPath", where)
                if not safe_repo_path(root, metadata_raw, f"{where}.derivedMetadataPath").is_file():
                    die(f"{where}: missing derived texture metadata {metadata_raw}")
        else:
            if derivation != "ACCESSORY_OVERLAY":
                die(f"{where}: OVERLAY must use ACCESSORY_OVERLAY derivation")
    if len(body_entries) != 1:
        die(f"manifest.production.textures must contain exactly one BODY entry, found {len(body_entries)}")

    builder = require_dict(data, "builder", "manifest")
    if builder.get("deterministic") is not True:
        die("manifest.builder.deterministic must be true")
    script_raw = require_text(builder, "scriptPath", "manifest.builder")
    script_path = safe_repo_path(root, script_raw, "manifest.builder.scriptPath")
    if not script_path.is_file() or script_path.suffix != ".py":
        die("manifest.builder.scriptPath must point to an existing Python builder")
    command = require_list(builder, "command", "manifest.builder")
    if not command or not all(isinstance(item, str) and item for item in command):
        die("manifest.builder.command must be a non-empty argv string list")
    if command[0] not in {"python", "python3", sys.executable}:
        die("manifest.builder.command must execute Python directly (no shell command strings)")
    if len(command) < 2 or command[1] != script_raw:
        die("manifest.builder.command[1] must equal builder.scriptPath")
    outputs = require_list(builder, "outputs", "manifest.builder")
    if model_path_raw not in outputs:
        die("manifest.builder.outputs must include production.modelPath")
    for texture in textures:
        if texture["path"] not in outputs:
            die(f"manifest.builder.outputs must include production texture {texture['path']}")

    blockbench = require_dict(data, "blockbench", "manifest")
    require_text(blockbench, "version", "manifest.blockbench")
    require_hash(blockbench, "appImageSha256", "manifest.blockbench", SHA256_RE)
    if blockbench.get("matchedCamera") is not True:
        die("manifest.blockbench.matchedCamera must be true")
    gameplay_resolution = blockbench.get("gameplayResolution")
    if not isinstance(gameplay_resolution, int) or not 128 <= gameplay_resolution <= 192:
        die("manifest.blockbench.gameplayResolution must be an integer from 128 through 192")
    require_text(blockbench, "heroAnimation", "manifest.blockbench")
    hero_time = blockbench.get("heroAnimationTime")
    if not isinstance(hero_time, (int, float)) or hero_time < 0:
        die("manifest.blockbench.heroAnimationTime must be a non-negative number")
    battle = blockbench.get("battleAnimation")
    if battle is not None and (not isinstance(battle, str) or not battle.strip()):
        die("manifest.blockbench.battleAnimation must be null or non-empty string")
    battle_time = blockbench.get("battleAnimationTime")
    if battle is not None and (not isinstance(battle_time, (int, float)) or battle_time < 0):
        die("manifest.blockbench.battleAnimationTime must be non-negative when battleAnimation exists")
    evidence_names = require_list(blockbench, "requiredEvidenceNames", "manifest.blockbench")
    if len(evidence_names) < 4 or not all(isinstance(item, str) and item.endswith(".png") for item in evidence_names):
        die("manifest.blockbench.requiredEvidenceNames must contain at least four PNG filenames")
    required_name_set = set(evidence_names)
    if "official_reference_three_quarter.png" not in required_name_set:
        die("professional evidence must include official_reference_three_quarter.png")
    if "hero_three_quarter.png" not in required_name_set:
        die("professional evidence must include hero_three_quarter.png")
    if not any("gameplay" in name for name in required_name_set):
        die("professional evidence must include at least one gameplay-scale PNG")
    visual_floor = require_dict(blockbench, "technicalVisualFloor", "manifest.blockbench")
    pixel_floor = visual_floor.get("minimumPixelDifferenceRatio")
    silhouette_floor = visual_floor.get("minimumSilhouetteDeltaRatio")
    if not isinstance(pixel_floor, (int, float)) or not 0.0 <= pixel_floor <= 0.5:
        die("minimumPixelDifferenceRatio must be between 0.0 and 0.5")
    if not isinstance(silhouette_floor, (int, float)) or not 0.0 <= silhouette_floor <= 1.0:
        die("minimumSilhouetteDeltaRatio must be between 0.0 and 1.0")

    evidence = require_dict(data, "evidence", "manifest")
    require_text(evidence, "artifactName", "manifest.evidence")
    require_text(evidence, "reviewContractFile", "manifest.evidence")
    require_text(evidence, "pngHashManifestFile", "manifest.evidence")
    required_files = require_list(evidence, "requiredFiles", "manifest.evidence")
    if not set(evidence_names).issubset(set(required_files)):
        die("manifest.evidence.requiredFiles must include every required Blockbench evidence PNG")

    quality = require_dict(data, "qualityIntent", "manifest")
    lessons = require_list(quality, "referenceLessons", "manifest.qualityIntent")
    if len(lessons) < 3 or not all(isinstance(item, str) and item.strip() for item in lessons):
        die("manifest.qualityIntent.referenceLessons must contain at least three concrete lessons")
    signature = require_list(quality, "signaturePieces", "manifest.qualityIntent")
    if not 1 <= len(signature) <= 3 or not all(isinstance(item, str) and item.strip() for item in signature):
        die("manifest.qualityIntent.signaturePieces must contain one to three dominant pieces")
    require_text(quality, "macroFormPlan", "manifest.qualityIntent")
    require_text(quality, "paintPlan", "manifest.qualityIntent")
    require_text(quality, "gameplayReadGoal", "manifest.qualityIntent")
    anti = require_list(quality, "antiPatternsToReject", "manifest.qualityIntent")
    if len(anti) < 3:
        die("manifest.qualityIntent.antiPatternsToReject must list at least three failure modes")
    if require_text(quality, "thirdPartyReusePolicy", "manifest.qualityIntent") != "TECHNIQUES_ONLY_UNLESS_LICENSED_DERIVATIVE_DONOR":
        die("manifest.qualityIntent.thirdPartyReusePolicy has unsupported value")

    variants = require_dict(data, "variantCoverage", "manifest")
    if variants.get("audited") is not True:
        die("manifest.variantCoverage.audited must be true")
    variant_list = require_list(variants, "variants", "manifest.variantCoverage")
    if not variant_list:
        die("manifest.variantCoverage.variants must document default/sex/forms coverage")
    for index, variant in enumerate(variant_list):
        where = f"manifest.variantCoverage.variants[{index}]"
        if not isinstance(variant, dict):
            die(f"{where} must be an object")
        require_text(variant, "name", where)
        require_text(variant, "coverage", where)

    checks = set(require_list(data, "technicalChecks", "manifest"))
    missing_checks = REQUIRED_TECHNICAL_CHECKS - checks
    if missing_checks:
        die(f"manifest.technicalChecks missing required checks: {sorted(missing_checks)}")
    if derived_body and "DERIVED_TEXTURE_PROVENANCE" not in checks:
        die("derived BODY texture requires DERIVED_TEXTURE_PROVENANCE technical check")

    report = {
        "status": "PASS",
        "species": species,
        "nationalDex": dex,
        "artStatus": art_status,
        "ownerApproval": approved,
        "referenceDossier": dossier_raw,
        "officialBoneCount": official_bone_count,
        "productionBoneCount": actual_total,
        "cosmeticBoneCount": len(extras),
        "cosmeticCubeCount": actual_cubes,
        "productionTextureCount": len(textures),
        "derivedBodyTexture": derived_body,
        "builderReproducibilityDeclared": True,
        "matchedBlockbenchCameraRequired": True,
        "requiredEvidenceCount": len(evidence_names),
        "gameplayResolution": gameplay_resolution,
        "artApprovalGrantedByTooling": False,
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
