#!/usr/bin/env python3
"""Build a deterministic contact sheet from current Blockbench evidence PNGs."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

DEFAULT_PANELS = [
    ("official_reference_three_quarter.png", "OFFICIAL 3/4"),
    ("hero_three_quarter.png", "HERO 3/4"),
    ("battle_ready_three_quarter.png", "BATTLE 3/4"),
    ("hero_front.png", "FRONT"),
    ("hero_back.png", "BACK"),
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--gameplay-name")
    args = parser.parse_args()

    source = args.input_dir
    panels = list(DEFAULT_PANELS)
    gameplay = args.gameplay_name
    if gameplay:
        panels.append((gameplay, "GAMEPLAY SCALE"))
    else:
        detected = sorted(path.name for path in source.glob("*gameplay*.png"))
        if detected:
            panels.append((detected[0], "GAMEPLAY SCALE"))

    loaded = []
    for filename, label in panels:
        path = source / filename
        if not path.is_file():
            continue
        loaded.append((Image.open(path).convert("RGBA"), label, filename))
    if len(loaded) < 4:
        raise SystemExit(f"contact sheet requires at least four evidence panels; found {len(loaded)}")

    cell = 512
    label_h = 42
    cols = 3
    rows = (len(loaded) + cols - 1) // cols
    canvas = Image.new("RGBA", (cols * cell, rows * (cell + label_h)), (24, 24, 24, 255))
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()

    for index, (image, label, filename) in enumerate(loaded):
        col = index % cols
        row = index // cols
        x = col * cell
        y = row * (cell + label_h)
        thumb = image.copy()
        thumb.thumbnail((cell, cell), Image.Resampling.LANCZOS)
        px = x + (cell - thumb.width) // 2
        py = y + (cell - thumb.height) // 2
        canvas.alpha_composite(thumb, (px, py))
        draw.rectangle((x, y + cell, x + cell, y + cell + label_h), fill=(16, 16, 16, 255))
        draw.text((x + 10, y + cell + 7), label, font=font, fill=(255, 255, 255, 255))
        draw.text((x + 10, y + cell + 22), filename[:72], font=font, fill=(180, 180, 180, 255))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(args.output, optimize=True)
    print(args.output)


if __name__ == "__main__":
    main()
