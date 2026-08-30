#!/usr/bin/env python3
"""Prepare pinned official Cobblemon assets and exact production texture for review.

The script downloads the exact Modrinth version declared by the professional
manifest, verifies JAR hashes, extracts official model/animation/texture bytes,
runs anatomy + attachment + derived-texture provenance gates, and emits a runtime
JSON consumed by the generic Blockbench workflow.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
import urllib.request
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

USER_AGENT = "Ouros-AutoPTU-Cobblemon-RPG/professional-skin-review"


def digest_bytes(data: bytes, algorithm: str) -> str:
    h = hashlib.new(algorithm)
    h.update(data)
    return h.hexdigest()


def request_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=120) as response:
        return response.read()


def repo_path(root: Path, raw: str) -> Path:
    p = Path(raw)
    if p.is_absolute() or ".." in p.parts:
        raise SystemExit(f"unsafe repository path: {raw!r}")
    out = (root / p).resolve()
    if root.resolve() not in out.parents and out != root.resolve():
        raise SystemExit(f"repository path escaped root: {raw!r}")
    return out


def run(command: list[str], root: Path) -> None:
    print("+", " ".join(command))
    subprocess.run(command, cwd=root, check=True)


def extract_checked(zf: zipfile.ZipFile, jar_path: str, output: Path, expected_sha256: str) -> None:
    try:
        data = zf.read(jar_path)
    except KeyError as exc:
        raise SystemExit(f"official JAR is missing declared asset: {jar_path}") from exc
    actual = digest_bytes(data, "sha256")
    if actual != expected_sha256:
        raise SystemExit(
            f"official asset hash mismatch for {jar_path}: expected={expected_sha256} actual={actual}"
        )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(data)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument("--workdir", type=Path, default=Path(".professional-review"))
    args = parser.parse_args()

    root = args.repo_root.resolve()
    manifest_path = args.manifest if args.manifest.is_absolute() else root / args.manifest
    workdir = args.workdir if args.workdir.is_absolute() else root / args.workdir
    workdir.mkdir(parents=True, exist_ok=True)
    official_dir = workdir / "official"
    output_dir = workdir / "output"
    official_dir.mkdir(parents=True, exist_ok=True)
    output_dir.mkdir(parents=True, exist_ok=True)

    data = json.loads(manifest_path.read_text(encoding="utf-8"))
    species = data["species"]
    official = data["officialSource"]
    production = data["production"]

    version_id = official["modrinthVersionId"]
    version_bytes = request_bytes(f"https://api.modrinth.com/v2/version/{version_id}")
    version = json.loads(version_bytes.decode("utf-8"))
    if version.get("id") != version_id:
        raise SystemExit("Modrinth version id mismatch")
    if version.get("version_number") != official["version"]:
        raise SystemExit(
            f"Cobblemon version drift: manifest={official['version']} Modrinth={version.get('version_number')}"
        )
    if official["minecraftVersion"] not in version.get("game_versions", []):
        raise SystemExit("declared Minecraft version is not listed by pinned Modrinth release")
    if official["loader"] not in version.get("loaders", []):
        raise SystemExit("declared loader is not listed by pinned Modrinth release")
    files = version.get("files", [])
    primary = next((entry for entry in files if entry.get("primary")), None)
    if not isinstance(primary, dict):
        raise SystemExit("pinned Modrinth release has no primary file")
    if primary.get("filename") != official["jarFilename"]:
        raise SystemExit(
            f"official filename drift: manifest={official['jarFilename']!r} Modrinth={primary.get('filename')!r}"
        )

    jar_bytes = request_bytes(primary["url"])
    jar_sha256 = digest_bytes(jar_bytes, "sha256")
    jar_sha512 = digest_bytes(jar_bytes, "sha512")
    if jar_sha256 != official["jarSha256"]:
        raise SystemExit(f"official JAR SHA-256 mismatch: {jar_sha256}")
    if jar_sha512 != official["jarSha512"]:
        raise SystemExit(f"official JAR SHA-512 mismatch: {jar_sha512}")
    primary_hashes = primary.get("hashes", {})
    if primary_hashes.get("sha512") and primary_hashes["sha512"] != jar_sha512:
        raise SystemExit("Modrinth primary-file SHA-512 does not match downloaded JAR")

    (workdir / "version.json").write_bytes(version_bytes)
    (workdir / "cobblemon.jar.sha256").write_text(jar_sha256 + "\n", encoding="utf-8")

    model_out = official_dir / "official.geo.json"
    animation_out = official_dir / "official.animation.json"
    texture_out = official_dir / "official.png"
    with zipfile.ZipFile(BytesIO(jar_bytes)) as zf:
        extract_checked(zf, official["modelPath"], model_out, official["modelSha256"])
        extract_checked(zf, official["animationPath"], animation_out, official["animationSha256"])
        extract_checked(
            zf,
            official["referenceTexture"]["path"],
            texture_out,
            official["referenceTexture"]["sha256"],
        )

    candidate_model = repo_path(root, production["modelPath"])
    run(
        [
            sys.executable,
            "tools/cobblemon-model-review/validate_original_model.py",
            "--official",
            str(model_out),
            "--candidate",
            str(candidate_model),
        ],
        root,
    )

    attachment = production.get("attachmentGate", {})
    anchor_gap = float(attachment.get("anchorGap", 1.5))
    piece_gap = float(attachment.get("pieceGap", 1.0))
    run(
        [
            sys.executable,
            "tools/cobblemon-model-review/validate_cosmetic_attachment.py",
            "--official",
            str(model_out),
            "--candidate",
            str(candidate_model),
            "--anchor-gap",
            str(anchor_gap),
            "--piece-gap",
            str(piece_gap),
        ],
        root,
    )

    textures = production["textures"]
    body = next(entry for entry in textures if entry["role"] == "BODY")
    body_path = repo_path(root, body["path"])
    if body["derivation"] == "DERIVED_FROM_OFFICIAL":
        metadata_path = repo_path(root, body["derivedMetadataPath"])
        run(
            [
                sys.executable,
                "tools/cobblemon-model-review/validate_derived_texture.py",
                "--official",
                str(texture_out),
                "--derived",
                str(body_path),
                "--metadata",
                str(metadata_path),
                "--expected-official-sha256",
                official["referenceTexture"]["sha256"],
                "--expected-derived-sha256",
                body["sha256"],
            ],
            root,
        )
    elif body["derivation"] == "OFFICIAL_IDENTICAL":
        if body["sha256"] != official["referenceTexture"]["sha256"]:
            raise SystemExit("OFFICIAL_IDENTICAL body texture does not match official reference texture hash")

    composed = Image.open(body_path).convert("RGBA")
    for entry in textures:
        if entry["role"] != "OVERLAY":
            continue
        overlay = Image.open(repo_path(root, entry["path"])).convert("RGBA")
        if overlay.size != composed.size:
            raise SystemExit(
                f"overlay dimensions mismatch for {entry['path']}: body={composed.size} overlay={overlay.size}"
            )
        composed = Image.alpha_composite(composed, overlay)
    composed_path = workdir / "production-composed.png"
    composed.save(composed_path, optimize=True)

    blockbench = data["blockbench"]
    runtime = {
        "species": species,
        "concept": data["concept"],
        "manifest": str(manifest_path.relative_to(root)),
        "officialModel": str(model_out),
        "officialAnimation": str(animation_out),
        "officialTexture": str(texture_out),
        "candidateModel": str(candidate_model),
        "candidateTexture": str(composed_path),
        "blockbench": blockbench,
        "evidence": data["evidence"],
        "outputDir": str(output_dir),
        "ownerApproval": data["ownerApproval"],
        "artStatus": data["artStatus"],
        "officialJar": {
            "versionId": version_id,
            "version": official["version"],
            "filename": official["jarFilename"],
            "sha256": jar_sha256,
            "sha512": jar_sha512,
        },
    }
    runtime_path = workdir / "review-runtime.json"
    runtime_path.write_text(json.dumps(runtime, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(runtime, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
