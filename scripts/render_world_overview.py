#!/usr/bin/env python3
"""Render a top-down surface-height overview from a generated Minecraft Anvil world.

This is evidence, not a cartographic mockup: every output pixel comes from
WORLD_SURFACE heightmaps stored in generated .mca chunks.
"""
from __future__ import annotations

import argparse
import io
import math
import struct
import sys
import zlib
from pathlib import Path

try:
    import nbtlib
except ImportError as exc:  # pragma: no cover - CI installs it explicitly
    raise SystemExit("nbtlib is required: python -m pip install nbtlib") from exc


def read_chunk_nbt(region_path: Path, sector_offset: int):
    with region_path.open("rb") as fh:
        fh.seek(sector_offset * 4096)
        raw_len = fh.read(4)
        if len(raw_len) != 4:
            return None
        length = struct.unpack(">I", raw_len)[0]
        if length <= 1:
            return None
        compression = fh.read(1)
        payload = fh.read(length - 1)

    if compression == b"\x01":
        import gzip
        payload = gzip.decompress(payload)
    elif compression == b"\x02":
        payload = zlib.decompress(payload)
    elif compression == b"\x03":
        pass
    else:
        return None

    return nbtlib.File.parse(io.BytesIO(payload))


def decode_heightmap(values, count: int = 256, bits: int = 9):
    mask = (1 << bits) - 1
    values_per_long = 64 // bits
    out = []
    longs = [int(v) & ((1 << 64) - 1) for v in values]
    for idx in range(count):
        long_idx = idx // values_per_long
        if long_idx >= len(longs):
            break
        shift = (idx % values_per_long) * bits
        out.append((longs[long_idx] >> shift) & mask)
    return out


def collect_chunks(region_dir: Path):
    chunks = {}
    errors = []
    for region_path in sorted(region_dir.glob("r.*.*.mca")):
        try:
            _, rx_s, rz_s, _ = region_path.name.split(".")
            rx, rz = int(rx_s), int(rz_s)
        except ValueError:
            continue

        with region_path.open("rb") as fh:
            header = fh.read(4096)
        if len(header) != 4096:
            errors.append(f"short header: {region_path}")
            continue

        for slot in range(1024):
            entry = header[slot * 4 : slot * 4 + 4]
            sector_offset = int.from_bytes(entry[:3], "big")
            sector_count = entry[3]
            if sector_offset == 0 or sector_count == 0:
                continue
            local_x = slot % 32
            local_z = slot // 32
            cx = rx * 32 + local_x
            cz = rz * 32 + local_z
            try:
                root = read_chunk_nbt(region_path, sector_offset)
                if root is None:
                    continue
                heightmaps = root.get("Heightmaps") or root.get("heightmaps")
                if heightmaps is None:
                    continue
                surface = (
                    heightmaps.get("WORLD_SURFACE")
                    or heightmaps.get("WORLD_SURFACE_WG")
                    or heightmaps.get("MOTION_BLOCKING")
                )
                if surface is None:
                    continue
                decoded = decode_heightmap(surface)
                if len(decoded) == 256:
                    chunks[(cx, cz)] = decoded
            except Exception as exc:  # keep evidence generation best-effort
                errors.append(f"{region_path.name} chunk {cx},{cz}: {exc}")
    return chunks, errors


def png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    return (
        struct.pack(">I", len(data))
        + chunk_type
        + data
        + struct.pack(">I", zlib.crc32(chunk_type + data) & 0xFFFFFFFF)
    )


def write_png(path: Path, width: int, height: int, rows):
    raw = bytearray()
    for row in rows:
        raw.append(0)  # filter type 0
        raw.extend(row)
    signature = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 0, 0, 0, 0)  # grayscale
    path.write_bytes(
        signature
        + png_chunk(b"IHDR", ihdr)
        + png_chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + png_chunk(b"IEND", b"")
    )


def nearest_scale(rows, factor: int):
    if factor <= 1:
        return rows
    scaled = []
    for row in rows:
        expanded = bytearray()
        for value in row:
            expanded.extend([value] * factor)
        for _ in range(factor):
            scaled.append(bytes(expanded))
    return scaled


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("world", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--scale", type=int, default=3)
    args = parser.parse_args()

    region_dir = args.world / "region"
    args.output.mkdir(parents=True, exist_ok=True)
    if not region_dir.is_dir():
        (args.output / "render-error.txt").write_text(
            f"Region directory not found: {region_dir}\n", encoding="utf-8"
        )
        return 2

    chunks, errors = collect_chunks(region_dir)
    if not chunks:
        (args.output / "render-error.txt").write_text(
            "No generated chunks with a readable surface heightmap were found.\n"
            + "\n".join(errors),
            encoding="utf-8",
        )
        return 3

    min_cx = min(cx for cx, _ in chunks)
    max_cx = max(cx for cx, _ in chunks)
    min_cz = min(cz for _, cz in chunks)
    max_cz = max(cz for _, cz in chunks)
    width = (max_cx - min_cx + 1) * 16
    height = (max_cz - min_cz + 1) * 16

    if width * height > 100_000_000:
        raise SystemExit(f"Refusing unexpectedly huge sparse canvas: {width}x{height}")

    surface_values = [v for heights in chunks.values() for v in heights]
    lo = min(surface_values)
    hi = max(surface_values)
    span = max(1, hi - lo)
    rows = [bytearray([0] * width) for _ in range(height)]

    for (cx, cz), heights in chunks.items():
        ox = (cx - min_cx) * 16
        oz = (cz - min_cz) * 16
        for idx, value in enumerate(heights):
            lx = idx % 16
            lz = idx // 16
            # Keep ungenerated/background pixels black; generated surface is 32..255.
            shade = 32 + round((value - lo) * 223 / span)
            rows[oz + lz][ox + lx] = shade

    scale = max(1, args.scale)
    rendered = nearest_scale(rows, scale)
    out_png = args.output / "ouros-global-world-surface.png"
    write_png(out_png, width * scale, height * scale, rendered)

    manifest = args.output / "ouros-global-world-surface.txt"
    manifest.write_text(
        "\n".join(
            [
                "source=generated Minecraft Anvil WORLD_SURFACE heightmap",
                f"region_dir={region_dir}",
                f"generated_chunks={len(chunks)}",
                f"chunk_bounds={min_cx},{min_cz}..{max_cx},{max_cz}",
                f"native_pixels={width}x{height}",
                f"png_pixels={width * scale}x{height * scale}",
                f"heightmap_raw_range={lo}..{hi}",
                f"parse_warnings={len(errors)}",
            ]
        )
        + "\n",
        encoding="utf-8",
    )
    if errors:
        (args.output / "render-warnings.txt").write_text("\n".join(errors) + "\n", encoding="utf-8")

    print(manifest.read_text(encoding="utf-8"), end="")
    print(f"render={out_png}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
