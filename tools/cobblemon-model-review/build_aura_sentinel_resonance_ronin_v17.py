#!/usr/bin/env python3
"""Compatibility entrypoint for the active Lucario professional manifest.

V17 completed exact-head evidence and was rejected by the unchanged Blockbench
visual floor plus direct art review. The current one-model slice is V18. This
wrapper lets the existing manifest bootstrap V18 once; V18 then rewrites the
manifest builder command to its own script path.
"""
from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V18_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v18.py"
spec = importlib.util.spec_from_file_location("resonance_v18", V18_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V18 builder")
v18 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v18)

if __name__ == "__main__":
    v18.main()
