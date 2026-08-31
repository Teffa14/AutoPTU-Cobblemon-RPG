#!/usr/bin/env python3
"""Build the Lucario Aura Sentinel V50 staging candidate from the pinned official JAR.

This script is intentionally pre-production. It proves that V50 can be generated
from the exact current official Cobblemon anatomy without reading rejected Ouros
production assets. It writes only to a caller-selected staging directory.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import urllib.parse
import urllib.request
import zipfile
from io import BytesIO
from pathlib import Path

PROJECT_ID = "MdwFAVRL"
VERSION_ID = "kF7CvxTo"
VERSION_NUMBER = "1.7.3"
MINECRAFT_VERSION = "1.21.1"
LOADER = "fabric"
JAR_FILENAME = "Cobblemon-fabric-1.7.3+1.21.1.jar"
JAR_SHA256 = "f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3"
JAR_SHA512 = "7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e"
MODEL_PATH = "assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json"
MODEL_SHA256 = "ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9"
NORMAL_SHA256 = "98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a"
SHINY_SHA256 = "b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d"
OFFICIAL_BONE_COUNT = 87
PATCH_PATH = Path("tools/cobblemon-model-review/authored-sources/0448_lucario/v50-professional-macroform.patch.json")
USER_AGENT = "Ouros-AutoPTU-Cobblemon-RPG/lucario-v50-staging-builder"


def digest(data: bytes, algorithm: str = "sha256") -> str:
    h = hashlib.new(algorithm)
    h.update(data)
    return h.hexdigest()


def request_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=120) as response:
        return response.read()


def load_release() -> tuple[dict, bytes]:
    raw = request_bytes(f"https://api.modrinth.com/v2/version/{VERSION_ID}")
    version = json.loads(raw.decode("utf-8"))
    if version.get("id") != VERSION_ID or version.get("project_id") != PROJECT_ID:
        raise SystemExit("pinned Modrinth identity mismatch")
    if version.get("version_number") != VERSION_NUMBER or version.get("version_type") != "release":
        raise SystemExit("pinned Cobblemon release metadata drift")
    if MINECRAFT_VERSION not in version.get("game_versions", []) or LOADER not in version.get("loaders", []):
        raise SystemExit("pinned Cobblemon release is not compatible with declared target")

    query = urllib.parse.urlencode({
        "loaders": json.dumps([LOADER]),
        "game_versions": json.dumps([MINECRAFT_VERSION]),
    })
    compatible = json.loads(request_bytes(
        f"https://api.modrinth.com/v2/project/{PROJECT_ID}/version?{query}"
    ).decode("utf-8"))
    stable = [
        entry for entry in compatible
        if entry.get("version_type") == "release" and entry.get("status") == "listed"
    ]
    if not stable:
        raise SystemExit("no listed stable compatible Cobblemon release returned by Modrinth")
    latest = max(stable, key=lambda entry: entry.get("date_published", ""))
    if latest.get("id") != VERSION_ID:
        raise SystemExit(
            f"pinned release is stale: pinned={VERSION_ID} latest={latest.get('id')} "
            f"({latest.get('version_number')})"
        )

    primary = next((entry for entry in version.get("files", []) if entry.get("primary")), None)
    if not isinstance(primary, dict) or primary.get("filename") != JAR_FILENAME:
        raise SystemExit("pinned primary JAR filename drift")
    jar = request_bytes(primary["url"])
    if digest(jar, "sha256") != JAR_SHA256 or digest(jar, "sha512") != JAR_SHA512:
        raise SystemExit("official Cobblemon JAR hash mismatch")
    return version, jar


def unique_asset_by_hash(zf: zipfile.ZipFile, expected_sha256: str, suffix: str) -> tuple[str, bytes]:
    matches: list[tuple[str, bytes]] = []
    for name in zf.namelist():
        if not name.endswith(suffix):
            continue
        data = zf.read(name)
        if digest(data) == expected_sha256:
            matches.append((name, data))
    if len(matches) != 1:
        raise SystemExit(
            f"expected exactly one {suffix} asset for SHA-256 {expected_sha256}, found {len(matches)}"
        )
    return matches[0]


def validate_patch(patch: dict) -> list[dict]:
    if patch.get("format") != "ouros.blockbench-authored-cosmetic-patch.v1":
        raise SystemExit("unexpected authored patch format")
    if patch.get("species") != "lucario" or patch.get("nationalDex") != 448:
        raise SystemExit("authored patch species/dex mismatch")
    baseline = patch.get("officialBaseline")
    if not isinstance(baseline, dict):
        raise SystemExit("authored patch missing officialBaseline")
    required = {
        "modrinthProjectId": PROJECT_ID,
        "modrinthVersionId": VERSION_ID,
        "jarFilename": JAR_FILENAME,
        "jarSha256": JAR_SHA256,
        "jarSha512": JAR_SHA512,
        "modelPath": MODEL_PATH,
        "modelSha256": MODEL_SHA256,
        "normalTextureSha256": NORMAL_SHA256,
        "shinyTextureSha256": SHINY_SHA256,
        "officialBoneCount": OFFICIAL_BONE_COUNT,
    }
    drift = {key: (baseline.get(key), value) for key, value in required.items() if baseline.get(key) != value}
    if drift:
        raise SystemExit("authored patch official baseline drift: " + json.dumps(drift, sort_keys=True))
    bones = patch.get("bones")
    if not isinstance(bones, list) or not bones:
        raise SystemExit("authored patch has no cosmetic bones")
    names: set[str] = set()
    cube_count = 0
    for bone in bones:
        if not isinstance(bone, dict):
            raise SystemExit("authored cosmetic bone must be an object")
        name = bone.get("name")
        if not isinstance(name, str) or not name.startswith("ouros_") or name in names:
            raise SystemExit(f"invalid/duplicate cosmetic bone name: {name!r}")
        names.add(name)
        cubes = bone.get("cubes")
        if not isinstance(cubes, list) or not cubes:
            raise SystemExit(f"cosmetic bone {name} has no cubes")
        cube_count += len(cubes)
    metrics = patch.get("sliceMetrics", {})
    if metrics.get("cosmeticBoneCount") != len(bones) or metrics.get("cosmeticCubeCount") != cube_count:
        raise SystemExit("authored patch metrics do not match cosmetic geometry")
    return bones


def build(repo_root: Path, output_dir: Path) -> dict:
    patch_file = (repo_root / PATCH_PATH).resolve()
    patch = json.loads(patch_file.read_text(encoding="utf-8"))
    cosmetic_bones = validate_patch(patch)

    version, jar = load_release()
    with zipfile.ZipFile(BytesIO(jar)) as zf:
        official_model = zf.read(MODEL_PATH)
        if digest(official_model) != MODEL_SHA256:
            raise SystemExit("official Lucario model hash mismatch")
        normal_path, normal = unique_asset_by_hash(zf, NORMAL_SHA256, ".png")
        shiny_path, shiny = unique_asset_by_hash(zf, SHINY_SHA256, ".png")

    model = json.loads(official_model.decode("utf-8"))
    geometries = model.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1:
        raise SystemExit("official Lucario model must contain exactly one geometry")
    geometry = geometries[0]
    official_bones = geometry.get("bones")
    if not isinstance(official_bones, list) or len(official_bones) != OFFICIAL_BONE_COUNT:
        raise SystemExit(f"official Lucario bone count drift: {len(official_bones) if isinstance(official_bones, list) else None}")

    official_names = {bone.get("name") for bone in official_bones if isinstance(bone, dict)}
    appended: list[dict] = []
    known = set(official_names)
    for bone in cosmetic_bones:
        parent = bone.get("parent")
        if parent not in known:
            raise SystemExit(f"cosmetic bone {bone.get('name')} has unknown parent {parent!r}")
        appended.append(bone)
        known.add(bone["name"])

    geometry["description"]["identifier"] = "geometry.ouros_aura_sentinel_lucario_v50"
    geometry["bones"] = [*official_bones, *appended]
    candidate_bytes = (
        json.dumps(model, ensure_ascii=False, separators=(",", ":")) + "\n"
    ).encode("utf-8")

    output_dir.mkdir(parents=True, exist_ok=True)
    model_out = output_dir / "ouros_aura_sentinel_lucario_v50.geo.json"
    normal_out = output_dir / "official_normal.png"
    shiny_out = output_dir / "official_shiny.png"
    report_out = output_dir / "build-report.json"
    model_out.write_bytes(candidate_bytes)
    normal_out.write_bytes(normal)
    shiny_out.write_bytes(shiny)

    cosmetic_cube_count = sum(len(bone["cubes"]) for bone in appended)
    report = {
        "format": "ouros.lucario-v50-staging-build.v1",
        "species": "lucario",
        "nationalDex": 448,
        "concept": patch["concept"],
        "artStatus": "ARTISTIC FAIL",
        "productionPromotionAllowed": False,
        "promotionBlocker": (
            "Deliberate independent derived normal and shiny paint, production resolver, "
            "professional manifest, registry promotion, full validators and generic Blockbench review are still required."
        ),
        "source": {
            "modrinthProjectId": PROJECT_ID,
            "modrinthVersionId": VERSION_ID,
            "version": VERSION_NUMBER,
            "jarFilename": JAR_FILENAME,
            "jarSha256": JAR_SHA256,
            "jarSha512": JAR_SHA512,
            "modelPath": MODEL_PATH,
            "modelSha256": MODEL_SHA256,
            "normalTexturePathDiscoveredByPinnedHash": normal_path,
            "normalTextureSha256": NORMAL_SHA256,
            "shinyTexturePathDiscoveredByPinnedHash": shiny_path,
            "shinyTextureSha256": SHINY_SHA256,
            "latestCompatibleStableVerified": True,
            "publishedAt": version.get("date_published"),
        },
        "geometry": {
            "officialBoneCount": OFFICIAL_BONE_COUNT,
            "candidateBoneCount": OFFICIAL_BONE_COUNT + len(appended),
            "cosmeticBoneCount": len(appended),
            "cosmeticCubeCount": cosmetic_cube_count,
            "cosmeticGroups": [bone["name"] for bone in appended],
            "candidateModelSha256": digest(candidate_bytes),
        },
        "stagingTextures": {
            "mode": "OFFICIAL_BASELINES_ONLY_NOT_PRODUCTION_PAINT",
            "normalSha256": digest(normal),
            "shinySha256": digest(shiny),
        },
        "authorityBoundary": "PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE",
    }
    report_out.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return report


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(".professional-candidate/0448_lucario/v50"),
        help="staging directory only; do not point this at fabric-adapter production resources",
    )
    args = parser.parse_args()
    root = args.repo_root.resolve()
    output = args.output_dir if args.output_dir.is_absolute() else (root / args.output_dir).resolve()
    production_root = (root / "fabric-adapter/src/main/resources").resolve()
    if output == production_root or production_root in output.parents:
        raise SystemExit("V50 staging builder refuses to write inside production resources")
    report = build(root, output)
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
