#!/usr/bin/env python3
"""Resonance Ronin V23: one continuous shoulder-back signature wrap.

V22 passed engineering but failed direct art review: crown, waistcoat and arm guards
fragmented the read and the dorsal treatment still resembled attached equipment.
V23 deliberately removes those systems. One compact asymmetric wrap owns the
shoulder/back/hip silhouette while a small chest bridge and restrained derived
paint carry the identity through the body.

Presentation only. AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V22_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py"
spec = importlib.util.spec_from_file_location("resonance_v22", V22_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V22 builder")
v22 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v22)
v1 = v22.v1
mcube = v22.mcube

NORMAL_META = ROOT / "docs/cobblemon-skins/0448_lucario/v23-derived-normal.json"
SHINY_META = ROOT / "docs/cobblemon-skins/0448_lucario/v23-derived-shiny.json"
v22.NORMAL_META = NORMAL_META
v22.SHINY_META = SHINY_META


def shell(origin, size, uv, *, pivot, rotation, light=82, dark=89):
    return mcube(origin, size, uv, light=light, dark=dark, pivot=pivot, rotation=rotation)


def cosmetic_bones() -> list[dict]:
    # One visual system. The first three overlapping volumes hug the official
    # right shoulder and upper back. Their different axes create one tapered
    # outer contour rather than three readable parallel plates.
    shoulder = v1.bone("ouros_v23_signature_shoulder_wrap", "shoulder_right", [-3.4, 29.6, 0.0], [
        shell((-8.15, 27.15, -1.85), (5.25, 1.05, 4.55), 80,
              pivot=(-4.7, 29.5, 0.2), rotation=(17, -18, 28), light=82, dark=89),
        shell((-7.05, 25.65, -0.10), (4.35, 0.82, 5.20), 81,
              pivot=(-4.45, 28.25, 0.85), rotation=(-8, -31, 19), light=81, dark=89),
        shell((-5.75, 24.15, 1.00), (3.45, 0.62, 4.80), 80,
              pivot=(-4.05, 26.75, 1.35), rotation=(-18, -22, 9), light=82, dark=90),
    ])

    # The back continuation overlaps the shoulder volumes and narrows as it
    # descends. It stays close to the official torso so the read is a wrapped
    # mantle/sculptural aura sheath, not a backpack or portal frame.
    back = v1.bone("ouros_v23_signature_back_flow", "torso3", [-2.35, 25.5, 1.2], [
        shell((-5.55, 22.25, 1.15), (3.85, 0.72, 5.55), 81,
              pivot=(-3.75, 25.8, 1.45), rotation=(13, -21, 14), light=81, dark=90),
        shell((-4.55, 18.45, 1.10), (3.05, 0.58, 5.15), 80,
              pivot=(-3.15, 22.5, 1.45), rotation=(7, -13, 5), light=82, dark=90),
        shell((-3.45, 15.05, 0.85), (2.25, 0.45, 4.35), 81,
              pivot=(-2.55, 19.1, 1.25), rotation=(-2, -7, -5), light=81, dark=89),
    ])

    # A forked hip finish creates negative space around the tail and legs. The
    # unequal lengths prevent the old rectangular waistcoat silhouette.
    hip = v1.bone("ouros_v23_signature_hip_fork", "torso", [-1.8, 16.0, 0.4], [
        shell((-4.0, 11.25, 0.25), (1.75, 0.38, 4.85), 80,
              pivot=(-2.55, 16.0, 0.65), rotation=(5, 11, -13), light=82, dark=90),
        shell((-1.15, 12.75, 0.75), (1.30, 0.32, 3.55), 81,
              pivot=(-0.85, 16.0, 0.85), rotation=(-5, -9, 11), light=81, dark=89),
    ])

    # One narrow bridge crosses the upper torso to make the wrap belong to the
    # whole character while preserving the official chest spike and face.
    bridge = v1.bone("ouros_v23_signature_chest_bridge", "torso3", [0.0, 28.0, -2.35], [
        shell((-3.45, 26.55, -3.20), (5.25, 0.26, 1.05), 84,
              pivot=(-0.25, 28.3, -3.0), rotation=(4, 0, -24), light=86, dark=80),
    ])

    return [shoulder, back, hip, bridge]


def paint_pixel(r: int, g: int, b: int, a: int, x: int, y: int, *, shiny: bool):
    if a == 0:
        return r, g, b, a
    mx, mn = max(r, g, b), min(r, g, b)
    sat = mx - mn
    lum = (30*r + 59*g + 11*b) // 100
    cream = r > 170 and g > 135 and b < 205
    white = r > 205 and g > 205 and b > 205
    red = r > 105 and r > g*1.35 and r > b*1.35
    if cream or white or red:
        return r, g, b, a

    blue = b > r*1.20 and b > g*1.08 and sat > 25
    if blue:
        # Keep biological blue recognisably Lucario. V22 over-darkened broad
        # limb regions; V23 uses a smaller value shift plus sparse cool facing
        # highlights, so texture supports rather than replaces the silhouette.
        facing = 5 if ((x + 2*y) % 37 in (0, 1)) else 0
        occ = max(0, (y - 28) // 9)
        if shiny:
            nr = int(r*.86) + 4
            ng = int(g*.91) + 5 + facing//2
            nb = int(b*.96) + 5 + facing
        else:
            nr = int(r*.72) + 5
            ng = int(g*.82) + 7 + facing//2
            nb = int(b*.94) + 8 + facing
        nr -= occ//4; ng -= occ//5; nb -= occ//7
        return *(max(0, min(255, v)) for v in (nr, ng, nb)), a

    if lum < 112 and sat < 82:
        # Indigo material breakup on existing dark biology. Preserve local
        # contrast and introduce tiny edge lights instead of a uniform multiply.
        edge = 5 if ((3*x + y) % 41 in (0, 1)) else 0
        occ = max(0, (y - 22) // 8)
        nr = int(r*.79) + 6 + edge//3 - occ//4
        ng = int(g*.81) + 7 + edge//2 - occ//4
        nb = int(b*.92) + 12 + edge - occ//5
        return *(max(0, min(255, v)) for v in (nr, ng, nb)), a
    return r, g, b, a


def post_patch() -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {
        "required": True, "approved": False, "approvedHeadSha": None,
        "evidenceSetSha256": None, "approvalRecord": None,
    }
    p = data["production"]
    p["productionBoneCount"] = v1.OFFICIAL_BONES + 4
    p["cosmeticBoneCount"] = 4
    p["cosmeticCubeCount"] = sum(len(b.get("cubes", [])) for b in cosmetic_bones())
    b = data["builder"]
    b["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v23.py"
    b["command"] = ["python", b["scriptPath"]]
    b["outputs"] = [x.replace("v22-derived-normal.json", "v23-derived-normal.json")
                    .replace("v22-derived-shiny.json", "v23-derived-shiny.json") for x in b["outputs"]]
    q = data["qualityIntent"]
    q["signaturePieces"] = [
        "Single asymmetric shoulder-to-back signature wrap",
        "Narrowing dorsal flow ending in a forked hip silhouette",
        "Small diagonal chest bridge plus restrained biological material repaint",
    ]
    q["macroFormPlan"] = (
        "V23 removes V22 crown, waistcoat, arm guards and isolated accessory language. "
        "Four contact-rooted systems form one continuous shoulder-back-hip envelope: "
        "three overlapping shoulder shells hand off into three narrowing dorsal shells, "
        "then two unequal hip tips create tail/leg negative space. One narrow chest bridge "
        "connects the asymmetric mass across the torso without covering the official spike."
    )
    q["paintPlan"] = (
        "Normal and shiny are independently derived from exact 1.7.3 baselines. Existing blue "
        "biology receives a restrained cobalt value pass with sparse cool facing highlights; "
        "existing dark biology gets indigo occlusion and edge breakup. Cream spikes, white "
        "landmarks, red eyes, dimensions, UV layout and alpha semantics stay unchanged."
    )
    q["gameplayReadGoal"] = (
        "At 160 px read one diagonal shoulder-back aura sheath with a clear taper and forked "
        "lower negative space, while the face, ears, chest spike, limbs and tail remain Lucario."
    )
    q["iterationNote"] = (
        "V22 engineering passed but direct review found crown/waistcoat/arm fragmentation, a "
        "backpack-like rear mass and insufficient silhouette integration. V23 deletes those "
        "systems rather than adding parts and concentrates silhouette budget into one contiguous wrap."
    )
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    # V22 already owns official-source verification, reference dossier validation,
    # derived texture validation, geometry validation and production writes. Swap
    # only authored presentation functions/metadata paths, then correct manifest
    # provenance to this exact builder.
    v22.cosmetic_bones = cosmetic_bones
    v22.paint_pixel = paint_pixel
    v22.main()
    post_patch()


if __name__ == "__main__":
    main()
