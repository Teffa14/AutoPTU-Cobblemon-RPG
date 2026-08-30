#!/usr/bin/env python3
"""Compatibility entrypoint for the active Lucario professional manifest.

V16b was retained only long enough to complete exact-head review. The current
one-model slice is V17. This wrapper lets the materializer that was already
configured for V16 bootstrap V17 once; V17 then rewrites the manifest builder
command to its own script path.
"""
from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V17_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v17.py"
spec = importlib.util.spec_from_file_location("resonance_v17", V17_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V17 builder")
v17 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v17)

if __name__ == "__main__":
    v17.main()
