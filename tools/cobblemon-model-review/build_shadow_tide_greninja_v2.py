#!/usr/bin/env python3
"""Second artistic pass for Shadow Tide: keep the verified anatomy/UV builder, but make the back silhouette unmistakable."""
from __future__ import annotations
import build_shadow_tide_greninja as b


def executioner_back_frame():
    # One large asymmetric tide-glaive dominates the rear 3/4 read. It remains a chest child,
    # so it follows the official torso animation without introducing a parallel rig.
    return {
        "name": "ouros_shadow_tide_back_frame",
        "parent": "chest",
        "pivot": [0, 23.0, 5.3],
        "cubes": [
            # Structural crossed harness retained as the attachment language.
            b.cube([-0.45, 16.7, 5.6], [0.9, 13.3, 0.9], "steel", [0, 23.35, 6.05], [0, 0, 42]),
            b.cube([-0.45, 16.7, 5.6], [0.9, 13.3, 0.9], "steel", [0, 23.35, 6.05], [0, 0, -42]),
            b.cube([-1.5, 21.7, 5.25], [3.0, 3.0, 0.35], "violet", [0, 23.2, 5.43], [0, 0, 45]),
            b.cube([-0.82, 22.38, 5.05], [1.64, 1.64, 0.18], "glass", [0, 23.2, 5.14], [0, 0, 45]),
            # Signature executioner tide-glaive: tall spine and broad hooked blade.
            b.cube([-10.9, 17.0, 5.45], [1.55, 15.8, 1.45], "abyss", [-10.12, 24.9, 6.18], [0, 0, -22]),
            b.cube([-10.55, 17.35, 5.18], [0.42, 15.0, 0.26], "cyan", [-10.34, 24.85, 5.31], [0, 0, -22]),
            b.cube([-12.4, 30.0, 5.2], [10.7, 1.55, 1.65], "indigo", [-7.05, 30.78, 6.03], [0, 0, 18]),
            b.cube([-13.15, 31.15, 5.3], [8.5, 0.42, 1.18], "steel", [-8.9, 31.36, 5.89], [0, 0, 29]),
            b.cube([-13.7, 32.05, 5.48], [6.3, 0.22, 0.72], "cyan", [-10.55, 32.16, 5.84], [0, 0, 39]),
            # Hooked lower edge gives the weapon a crescent/water-break profile.
            b.cube([-13.0, 25.7, 5.35], [6.5, 0.85, 1.35], "indigo", [-9.75, 26.12, 6.02], [0, 0, -38]),
            b.cube([-13.5, 24.45, 5.48], [4.8, 0.28, 0.82], "foam", [-11.1, 24.59, 5.89], [0, 0, -49]),
            # Smaller counter-fin keeps asymmetry deliberate instead of looking broken.
            b.cube([4.1, 27.9, 5.5], [6.9, 0.72, 1.25], "abyss", [7.55, 28.26, 6.12], [0, 0, 24]),
            b.cube([6.0, 29.0, 5.62], [5.2, 0.22, 0.70], "cyan", [8.6, 29.11, 5.97], [0, 0, 34]),
            b.cube([9.8, 25.6, 5.45], [0.9, 5.8, 1.1], "steel", [10.25, 28.5, 6.0], [0, 0, 18]),
        ],
    }


def heavy_split_mantle():
    # Larger, layered panels remain behind the legs and leave the tongue, hands and knee silhouette free.
    return {
        "name": "ouros_shadow_tide_split_mantle",
        "parent": "waist",
        "pivot": [0, 17.8, 4.2],
        "cubes": [
            b.cube([-7.1, 8.7, 4.7], [6.0, 9.7, 0.58], "indigo", [-4.1, 17.5, 4.99], [-11, 0, 12]),
            b.cube([1.1, 9.9, 4.7], [5.4, 8.5, 0.58], "abyss", [3.8, 17.5, 4.99], [-11, 0, -9]),
            b.cube([-6.8, 8.45, 5.22], [5.2, 0.30, 0.20], "cyan", [-4.2, 8.60, 5.32], [-11, 0, 12]),
            b.cube([1.5, 9.65, 5.22], [4.6, 0.30, 0.20], "foam", [3.8, 9.80, 5.32], [-11, 0, -9]),
            b.cube([-7.7, 16.7, 4.45], [7.1, 1.45, 0.82], "steel", [-4.15, 17.43, 4.86], [0, 0, -7]),
            b.cube([0.6, 16.9, 4.45], [6.5, 1.25, 0.82], "steel", [3.85, 17.53, 4.86], [0, 0, 6]),
            # Side pennants widen the lower silhouette without touching the original leg bones.
            b.cube([-8.6, 12.0, 4.95], [2.5, 5.4, 0.42], "violet", [-7.35, 16.6, 5.16], [-8, 0, 21]),
            b.cube([6.0, 12.7, 4.95], [2.2, 4.8, 0.42], "indigo", [7.1, 16.8, 5.16], [-8, 0, -18]),
            b.cube([-1.15, 14.8, 5.30], [2.3, 2.3, 0.20], "cyan", [0, 15.95, 5.40], [0, 0, 45]),
            b.cube([-0.62, 15.33, 5.12], [1.24, 1.24, 0.12], "glass", [0, 15.95, 5.18], [0, 0, 45]),
        ],
    }

b.EXTRA_BUILDERS = (
    b.cowl,
    b.gorget,
    b.pauldron_right,
    b.pauldron_left,
    b.bracer_right,
    b.bracer_left,
    executioner_back_frame,
    heavy_split_mantle,
)

if __name__ == "__main__":
    b.main()
