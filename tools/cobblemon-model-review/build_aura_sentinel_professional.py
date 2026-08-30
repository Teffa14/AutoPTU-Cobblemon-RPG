#!/usr/bin/env python3
"""Stable entrypoint for the Aura Sentinel professional builder."""
from __future__ import annotations
import hashlib
import importlib.util
from pathlib import Path

IMPL = Path(__file__).with_name("_aura_sentinel_professional_impl.py")
spec = importlib.util.spec_from_file_location("aura_sentinel_professional_impl", IMPL)
impl = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(impl)

_original_find = impl.find_path_by_hash

def _find_official_asset(zf, expected: str) -> str:
    if expected == impl.AUX_HASHES["MODEL_LICENSE"]:
        path = "assets/cobblemon/bedrock/pokemon/models/0448_lucario/license"
        actual = hashlib.sha256(zf.read(path)).hexdigest()
        if actual != expected:
            raise SystemExit(f"official Lucario model-license drift: expected={expected} actual={actual}")
        return path
    return _original_find(zf, expected)

impl.find_path_by_hash = _find_official_asset

if __name__ == "__main__":
    impl.main()
