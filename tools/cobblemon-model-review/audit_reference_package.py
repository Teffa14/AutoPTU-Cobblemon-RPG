#!/usr/bin/env python3
"""Inspect a public external skin package without copying its assets into production.

The tool emits metadata/hashes only. It never extracts or commits third-party model or
texture bytes. This is intended for STUDY_ONLY / license-audit reference dossiers.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

MODEL_SUFFIXES = (".geo.json", ".bbmodel", ".model.json")
TEXTURE_SUFFIXES = (".png",)
ANIMATION_SUFFIXES = (".animation.json",)
AUX_TOKENS = ("resolver", "poser", "species", "feature")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def classify(path: str) -> str:
    low = path.lower()
    if low.endswith(MODEL_SUFFIXES):
        return "MODEL"
    if low.endswith(TEXTURE_SUFFIXES):
        return "TEXTURE"
    if low.endswith(ANIMATION_SUFFIXES):
        return "ANIMATION"
    if low.endswith(".json") and any(token in low for token in AUX_TOKENS):
        return "AUX_JSON"
    if any(token in low for token in ("license", "copying", "notice")):
        return "LICENSE"
    return "OTHER"


def json_identifiers(data: bytes) -> dict:
    try:
        parsed = json.loads(data.decode("utf-8"))
    except Exception:
        return {}
    text = json.dumps(parsed, separators=(",", ":"))
    identifiers = sorted(set(re.findall(r"(?:geometry|animation)\.[A-Za-z0-9_.:-]+", text)))
    aspects = sorted(set(re.findall(r'"aspects":\[(.*?)\]', text)))
    return {
        "identifiers": identifiers[:50],
        "aspectFragments": aspects[:50],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("package", type=Path)
    parser.add_argument("--species", required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    raw = args.package.read_bytes()
    species = args.species.casefold()
    records = []
    with zipfile.ZipFile(args.package) as zf:
        for info in zf.infolist():
            if info.is_dir():
                continue
            path = info.filename
            low = path.casefold()
            kind = classify(path)
            # Species assets plus package license/provenance files are relevant.
            if species not in low and kind != "LICENSE":
                continue
            data = zf.read(info)
            record = {
                "path": path,
                "kind": kind,
                "size": len(data),
                "sha256": sha256(data),
            }
            if kind in {"MODEL", "ANIMATION", "AUX_JSON"}:
                record.update(json_identifiers(data))
            records.append(record)

    report = {
        "format": "ouros.external-reference-package-audit.v1",
        "project": args.project,
        "sourceVersion": args.version,
        "sourceUrl": args.source_url,
        "species": args.species.lower(),
        "packageFilename": args.package.name,
        "packageSha256": sha256(raw),
        "matchingFiles": sorted(records, key=lambda r: (r["kind"], r["path"])),
        "modelCount": sum(r["kind"] == "MODEL" for r in records),
        "textureCount": sum(r["kind"] == "TEXTURE" for r in records),
        "animationCount": sum(r["kind"] == "ANIMATION" for r in records),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
