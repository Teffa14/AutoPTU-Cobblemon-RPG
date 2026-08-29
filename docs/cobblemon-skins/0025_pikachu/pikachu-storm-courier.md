# Pikachu — Storm Courier (Cobblemon 1.7.3 clean rebuild)

## Source of truth

This implementation was rebuilt from zero after the previous Storm Courier model was rejected.

The anatomical source is the exact Pikachu geometry extracted from the official Cobblemon 1.7.3 Fabric JAR (`Cobblemon-fabric-1.7.3+1.21.1.jar`, Modrinth version id `kF7CvxTo`), not a mirror and not an Ouros reconstruction.

Pinned official geometry hashes:

- male `pikachu_male.geo.json`: `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female `pikachu_female.geo.json`: `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- base texture `pikachu.png`: `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`

Both official gender models contain 90 bones. The clean rebuild preserves all 90 original bones exactly at the JSON-object level, in the same order, including all cubes, pivots, parents, rotations, locators and UV definitions. The female model keeps the official female `tail3` UV instead of being coerced to the male model.

## Cosmetic geometry

Only five bones are appended:

- `ouros_storm_visor` → parent `head_angle`
- `ouros_storm_harness` → parent `torso2`
- `ouros_storm_pack` → parent `torso2`
- `ouros_storm_antenna` → parent `torso2`
- `ouros_storm_tail_clamp` → parent `tail2`

The design adds a translucent storm visor with a dark/copper frame, crossed leather courier harness, brass/glass clasp, compact canvas weather pack, copper storm sigil, short storm antenna and a local tail clamp. No body part is replaced or approximated.

The original `cobblemon:pikachu` poser remains authoritative for presentation. Accessories inherit transforms from existing animated parents. No Ouros replacement animation rig exists.

## Texture contract

The resolver continues to reference Cobblemon's own base/shiny and emissive textures. Ouros supplies only `ouros_storm_courier_accessories.png`, a transparent 128×64 layer.

Accessory materials use eight texels on row `y=63`. Static UV analysis against the exact official geometry confirms that this row is outside the UV footprint of all original cubes. Therefore the accessory layer does not repaint Pikachu's body.

## Validation

`tools/cobblemon-model-review/validate_original_model.py` rejects any drift in the 90 official bones. `tools/cobblemon-model-review/build_storm_courier.py` deterministically derives both production models from official JAR inputs.

Visual acceptance uses Blockbench 5.1.6. The rejected project-authored Python Bedrock renderer is not acceptance evidence. The review workflow must render the untouched official model and the derived Storm Courier model through Blockbench and compare the four views.

## Authority boundary

This is presentation-only. It does not use or alter Cobblemon battle-state authority, participants, legality, HP/status, tactical positions or combatant selection. Ouros/AutoPTU remains authoritative for battle facts.
