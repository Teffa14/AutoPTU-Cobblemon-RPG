#!/usr/bin/env python3
"""Compatibility entry point: Lucario active slice moved from V28b to the owner-reference V29 rebuild.

The professional materializer still reaches this path from the previous manifest on the first run after the
owner-directed switch. Delegate once to V29; V29 rewrites the manifest so subsequent runs call it directly.
"""
from __future__ import annotations
import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TARGET = ROOT / "tools/cobblemon-model-review/build_lucario_owner_reference_v29.py"
spec = importlib.util.spec_from_file_location("lucario_owner_reference_v29", TARGET)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Lucario owner-reference V29 builder")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

if __name__ == "__main__":
    mod.main()
