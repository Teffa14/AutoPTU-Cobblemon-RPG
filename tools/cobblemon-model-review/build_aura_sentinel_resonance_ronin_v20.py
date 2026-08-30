#!/usr/bin/env python3
"""Superseded compatibility entrypoint.

The active professional manifest previously pointed at V20. Keep this tiny bridge
only so GitHub Actions can materialize V21 once and let V21 bootstrap the manifest
to its canonical builder path. V20 remains artistically rejected and is not used
as the new production design source.
"""
from pathlib import Path
import runpy

TARGET = Path(__file__).with_name("build_aura_sentinel_resonance_ronin_v21b.py")
runpy.run_path(str(TARGET), run_name="__main__")
