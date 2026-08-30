#!/usr/bin/env python3
"""Resonance Ronin V21: compact contour mantle + derived biological paint.

V20 proved that a silhouette-only solution can pass technical floors while still
reading as stacked equipment. V21 removes the four-piece hanging cloak cascade.
The geometry becomes one compact shoulder/back/hip gesture with fewer, more
oblique surfaces. A deterministic paint pass derived independently from the
exact official normal and shiny textures carries the fantasy through Lucario's
existing body without changing UVs, dimensions, alpha semantics, or anatomy.

Presentation only. AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
V20_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v20.py"
spec = importlib.util.spec_from_file_location("resonance_v20", V20_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V20 builder")
v20 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v20)
v1 = v20.v1
v14 = v20.v14
mcube = v20.mcube
write_overlay = v20.write_overlay

BASELINE_DIR = ROOT / "tools/cobblemon-model-review/baselines/0448_lucario"
NORMAL_BASELINE = BASELINE_DIR / "lucario_official_1.7.3.png"
SHINY_BASELINE = BASELINE_DIR / "lucario_official_shiny_1.7.3.png"
NORMAL_META = ROOT / "docs/cobblemon-skins/0448_lucario/v21-derived-normal.json"
SHINY_META = ROOT / "docs/cobblemon-skins/0448_lucario/v21-derived-shiny.json"
DERIVED_VALIDATOR = ROOT / "tools/cobblemon-model-review/validate_derived_texture.py"

RETAINED = {"ouros_resonance_cowl", "ouros_resonance_high_collar"}


def ensure_baselines() -> None:
    BASELINE_DIR.mkdir(parents=True, exist_ok=True)
    if not NORMAL_BASELINE.exists():
        if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256:
            raise SystemExit("cannot bootstrap normal baseline: production BODY is no longer official-identical")
        shutil.copy2(v1.BODY, NORMAL_BASELINE)
    if not SHINY_BASELINE.exists():
        if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256:
            raise SystemExit("cannot bootstrap shiny baseline: production SHINY is no longer official-identical")
        shutil.copy2(v1.SHINY, SHINY_BASELINE)
    if v1.sha256(NORMAL_BASELINE) != v1.OFFICIAL_NORMAL_SHA256:
        raise SystemExit("stored normal baseline hash drifted")
    if v1.sha256(SHINY_BASELINE) != v1.OFFICIAL_SHINY_SHA256:
        raise SystemExit("stored shiny baseline hash drifted")


def _paint_pixel(r: int, g: int, b: int, a: int, x: int, y: int, *, shiny: bool) -> tuple[int, int, int, int]:
    if a == 0:
        return r, g, b, a

    mx = max(r, g, b)
    mn = min(r, g, b)
    sat = mx - mn
    lum = (r * 30 + g * 59 + b * 11) // 100

    # Preserve white/cream spikes, eyes and high-value facial landmarks.
    cream = r > 175 and g > 145 and b < 205
    near_white = r > 205 and g > 205 and b > 205
    red_eye = r > 105 and r > g * 1.35 and r > b * 1.35
    if cream or near_white or red_eye:
        return r, g, b, a

    # Existing blue biological regions become lacquered aura-blue. This is a
    # local value ramp, not a global hue rotation: top/facing texels brighten,
    # lower/interior texels deepen, and narrow coordinate bands receive a
    # restrained cyan edge lift.
    blue_region = b > r * 1.22 and b > g * 1.10 and sat > 28
    if blue_region:
        vertical = max(-18, min(18, 14 - y // 3))
        edge = 12 if ((x + 2 * y) % 17 in (0, 1)) else 0
        if shiny:
            nr = int(r * 0.82) + 8
            ng = int(g * 0.94) + 10 + edge // 2
            nb = int(b * 1.04) + 8 + edge
        else:
            nr = int(r * 0.58) + 8
            ng = int(g * 0.82) + 15 + edge // 2
            nb = int(b * 1.08) + 15 + edge
        nr += vertical // 4
        ng += vertical // 2
        nb += vertical
        return max(0, min(255, nr)), max(0, min(255, ng)), max(0, min(255, nb)), a

    # Dark biological surfaces receive an indigo/blue-steel undertone with
    # painted occlusion toward the atlas bottom and selective facing highlights.
    if lum < 105 and sat < 75:
        occlusion = max(0, (y - 18) // 4)
        highlight = 9 if ((3 * x + y) % 23 in (0, 1, 2)) else 0
        nr = int(r * 0.72) + 10 + highlight // 3
        ng = int(g * 0.76) + 11 + highlight // 2
        nb = int(b * 0.92) + 22 + highlight
        nr -= occlusion // 3
        ng -= occlusion // 3
        nb -= occlusion // 4
        return max(0, min(255, nr)), max(0, min(255, ng)), max(0, min(255, nb)), a

    return r, g, b, a


def derive_texture(source: Path, target: Path, *, shiny: bool) -> None:
    image = Image.open(source).convert("RGBA")
    out = Image.new("RGBA", image.size)
    src = image.load(); dst = out.load()
    for y in range(image.height):
        for x in range(image.width):
            dst[x, y] = _paint_pixel(*src[x, y], x, y, shiny=shiny)
    target.parent.mkdir(parents=True, exist_ok=True)
    out.save(target, format="PNG", optimize=True, compress_level=9)


def write_metadata(path: Path, baseline: Path, derived: Path, *, shiny: bool) -> None:
    payload = {
        "format": "ouros.cobblemon-derived-texture.v1",
        "species": "lucario",
        "variant": "shiny" if shiny else "normal",
        "officialTextureBaseline": str(baseline.relative_to(ROOT)),
        "officialTextureBaselineSha256": v1.sha256(baseline),
        "derivedTexture": derived.name,
        "derivedTextureSha256": v1.sha256(derived),
        "bodyTexelRework": "PAINTED_VALUE_MATERIAL_PASS",
        "paletteIntent": "Preserve Lucario recognition while shifting blue biological surfaces toward deep aura lacquer and dark surfaces toward indigo-blue steel; cream spikes, white highlights and red eyes remain readable.",
        "materialIntent": "Non-uniform local value ramps, lower-surface occlusion, sparse facing-plane highlights and subtle hue/value breakup. No third-party palette, markings or costume motifs are copied.",
        "repaintRegions": [
            "existing blue biological texels",
            "existing dark biological texels",
        ],
        "alphaSemantics": "UNCHANGED",
        "sourceRelease": "Cobblemon 1.7.3 Fabric / Modrinth kF7CvxTo",
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def validate_derived(baseline: Path, derived: Path, metadata: Path, expected: str) -> None:
    subprocess.run([
        sys.executable, str(DERIVED_VALIDATOR),
        "--official", str(baseline),
        "--derived", str(derived),
        "--metadata", str(metadata),
        "--expected-official-sha256", expected,
        "--expected-derived-sha256", v1.sha256(derived),
    ], cwd=ROOT, check=True)


def v21_bones() -> list[dict]:
    retained = [b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained} != RETAINED:
        raise SystemExit("retained cosmetic contract drifted")

    # Compact shoulder root: two intersecting rotated masses hug the official
    # shoulder and begin a diagonal gesture instead of creating a shelf/slab.
    shoulder_wrap = v1.bone("ouros_resonance_v21_shoulder_wrap", "shoulder_right", [-4.0, 30.0, -0.7], [
        mcube((-7.65, 27.85, -3.00), (4.60, 3.15, 2.15), 80, light=82, dark=88,
              pivot=(-4.95, 29.75, -1.75), rotation=(-18, 25, 31)),
        mcube((-8.70, 25.75, -2.15), (3.55, 3.15, 1.20), 81, light=82, dark=88,
              pivot=(-6.15, 28.05, -1.45), rotation=(-12, 31, 18)),
    ])

    # One diagonal back-to-hip ribbon made from three strongly overlapped facets.
    # Length, width, depth and compound rotation change on every step; the center
    # remains open so it does not become a rear rectangle or skirt.
    contour_mantle = v1.bone("ouros_resonance_v21_contour_mantle", "torso3", [-3.0, 27.2, 1.0], [
        mcube((-8.15, 23.10, .75), (5.10, 4.20, 1.05), 80, light=83, dark=88,
              pivot=(-5.15, 26.45, 1.30), rotation=(15, -21, 25)),
        mcube((-7.05, 18.90, 1.05), (4.05, 5.20, .78), 81, light=83, dark=88,
              pivot=(-4.55, 23.20, 1.42), rotation=(11, -14, 12)),
        mcube((-5.35, 14.85, 1.05), (2.75, 4.85, .52), 80, light=82, dark=88,
              pivot=(-3.70, 19.15, 1.30), rotation=(5, -6, -4)),
    ])

    # Two tiny counterweights complete the whole-body composition without turning
    # the lower body into shorts/armor. They sit on opposite sides and leave the
    # official tail and leg silhouette exposed.
    hip_accents = v1.bone("ouros_resonance_v21_hip_accents", "torso", [0, 15.2, .6], [
        mcube((-5.25, 12.85, -2.10), (2.35, 2.45, .46), 84, light=85, dark=80,
              pivot=(-3.75, 14.75, -1.86), rotation=(7, 12, -24)),
        mcube((2.75, 13.60, .70), (1.55, 2.10, .38), 82, light=85, dark=80,
              pivot=(3.35, 15.00, .90), rotation=(-8, -12, 20)),
    ])
    return retained + [shoulder_wrap, contour_mantle, hip_accents]


def build_model() -> int:
    # Rebuild the biological prefix through the existing deterministic chain,
    # then discard all older cosmetics and append only V21 systems.
    v20.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v21_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {"required": True, "approved": False, "approvedHeadSha": None,
                             "evidenceSetSha256": None, "approvalRecord": None}
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 5
    data["production"]["cosmeticBoneCount"] = 5
    data["production"]["cosmeticCubeCount"] = cubes
    body = next(t for t in data["production"]["textures"] if t["role"] == "BODY")
    body["sha256"] = v1.sha256(v1.BODY)
    body["derivation"] = "DERIVED_FROM_OFFICIAL"
    body["officialBaselineSha256"] = v1.OFFICIAL_NORMAL_SHA256
    body["derivedMetadataPath"] = str(NORMAL_META.relative_to(ROOT))
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER": asset["sha256"] = v1.sha256(v1.RESOLVER)
        if asset.get("role") == "SHINY_BODY": asset["sha256"] = v1.sha256(v1.SHINY)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v21.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v21.py"]
    extra_outputs = [
        str(NORMAL_BASELINE.relative_to(ROOT)), str(SHINY_BASELINE.relative_to(ROOT)),
        str(NORMAL_META.relative_to(ROOT)), str(SHINY_META.relative_to(ROOT)),
    ]
    for path in extra_outputs:
        if path not in data["builder"]["outputs"]: data["builder"]["outputs"].append(path)
    if "DERIVED_TEXTURE_PROVENANCE" not in data["technicalChecks"]:
        data["technicalChecks"].append("DERIVED_TEXTURE_PROVENANCE")
    data["qualityIntent"]["signaturePieces"] = [
        "Compact oblique shoulder wrap that begins the silhouette without a horizontal slab",
        "Single dark contour mantle turning from back to hip through three overlapped changing-angle facets",
        "Body-wide aura lacquer repaint that carries material identity through Lucario's existing anatomy",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V21 deletes V20's four-cube hanging half-cloak and two-piece dorsal handoff. Five cosmetic systems total remain: cowl, high collar, a two-piece shoulder wrap, one three-facet diagonal back-to-hip contour mantle, and two small asymmetric hip accents. The center back, chest spike, tail and leg silhouettes stay deliberately open."
    )
    data["qualityIntent"]["paintPlan"] = (
        "Derive normal and shiny independently from their exact Cobblemon 1.7.3 baselines. Existing blue biological texels receive local aura-lacquer value ramps; dark biological texels receive indigo/blue-steel occlusion and sparse facing highlights. Cream spikes, white highlights, red eyes, dimensions, UV layout and alpha semantics remain intact."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the transformation should read through a coherent dark/cobalt material identity across the biological body plus one diagonal shoulder-to-hip gesture. Geometry should no longer read as a hanging rectangular garment or plate stack."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V20 passed pixel/silhouette floors but direct Blockbench QA rejected its four-piece half-cloak as stacked dark plates. V21 reduces exterior geometry and shifts authorship into a validated derived-texture pass rather than inflating silhouette with more cuboids."
    )
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--bootstrap", action="store_true"); args = parser.parse_args()
    ensure_baselines()
    cubes = build_model()
    derive_texture(NORMAL_BASELINE, v1.BODY, shiny=False)
    derive_texture(SHINY_BASELINE, v1.SHINY, shiny=True)
    write_metadata(NORMAL_META, NORMAL_BASELINE, v1.BODY, shiny=False)
    write_metadata(SHINY_META, SHINY_BASELINE, v1.SHINY, shiny=True)
    validate_derived(NORMAL_BASELINE, v1.BODY, NORMAL_META, v1.OFFICIAL_NORMAL_SHA256)
    validate_derived(SHINY_BASELINE, v1.SHINY, SHINY_META, v1.OFFICIAL_SHINY_SHA256)
    write_overlay(v1.OVERLAY)
    v1.build_resolver()
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({
        "status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V21",
        "officialBones":v1.OFFICIAL_BONES,"cosmeticBones":5,"cosmeticCubes":cubes,
        "modelSha256":v1.sha256(v1.MODEL),"normalDerivedSha256":v1.sha256(v1.BODY),
        "shinyDerivedSha256":v1.sha256(v1.SHINY),"overlaySha256":v1.sha256(v1.OVERLAY),
        "bodyTexelRework":"PAINTED_VALUE_MATERIAL_PASS",
        "visualChange":"compact contour mantle + independently derived normal/shiny material pass"
    }, indent=2))

if __name__ == "__main__": main()
