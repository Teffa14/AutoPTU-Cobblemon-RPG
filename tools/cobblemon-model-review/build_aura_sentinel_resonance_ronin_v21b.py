#!/usr/bin/env python3
"""Superseded V21 compatibility entrypoint for one deterministic bootstrap."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name("build_aura_sentinel_resonance_ronin_v22.py")), run_name="__main__")
