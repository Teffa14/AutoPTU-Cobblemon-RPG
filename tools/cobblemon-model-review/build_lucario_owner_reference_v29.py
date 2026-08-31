#!/usr/bin/env python3
"""Compatibility entrypoint for the active Lucario owner-reference manifest.

The manifest historically points at this V29 path. Current production is V31;
keep this tiny forwarder only long enough for deterministic materialization to
rewrite the manifest onto the self-contained V31 builder.
"""
from __future__ import annotations
import importlib.util
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
TARGET=ROOT/'tools/cobblemon-model-review/build_lucario_owner_reference_v31.py'
spec=importlib.util.spec_from_file_location('owner_reference_v31_active',TARGET)
if spec is None or spec.loader is None:
    raise SystemExit('cannot load V31 owner-reference builder')
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
if __name__=='__main__':
    mod.main()
