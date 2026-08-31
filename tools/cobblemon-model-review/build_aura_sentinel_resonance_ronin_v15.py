#!/usr/bin/env python3
"""Compatibility handoff from rejected V15c to the current V16 builder.

V15c is retained only as a deterministic historical entry point for the one
materializer run whose manifest still names this path. The V16 bootstrap rewrites
the professional manifest to the V16 builder. No V15c geometry is authored here.
"""
from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V16_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v16.py"
spec = importlib.util.spec_from_file_location("resonance_v16", V16_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load V16 builder")
v16 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v16)

if __name__ == "__main__":
    v16.main()
