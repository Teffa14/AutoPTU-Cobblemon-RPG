# Pikachu — Storm Courier (Cobblemon 1.7.3 premium pass)

## Source of truth

Storm Courier is derived from the exact Pikachu geometry distributed in the official Cobblemon 1.7.3 Fabric JAR (`Cobblemon-fabric-1.7.3+1.21.1.jar`, Modrinth version id `kF7CvxTo`). It is not derived from a mirror, an old model, screenshots or an Ouros reconstruction.

Pinned official hashes:

- male `pikachu_male.geo.json`: `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female `pikachu_female.geo.json`: `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- base texture `pikachu.png`: `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`
- official `pikachu.animation.json`: `d9ca00604978f295ad312d358a06f2655c725b30ac3da73c3637ae160c543384`

Both official gender models contain 90 bones. The derived models preserve all 90 original bones exactly at the JSON-object level and in the same order, including cubes, pivots, parents, rotations, locators and UV definitions. Male and female are derived independently so the official female tail remains intact.

## Premium cosmetic geometry

Exactly four Ouros bones are appended. No original Cobblemon bone is rewritten:

- `ouros_storm_goggles` → parent `head_angle`
- `ouros_storm_harness` → parent `torso2`
- `ouros_storm_pack` → parent `torso2`
- `ouros_storm_tail_clamp` → parent `tail2`

The premium pass increases detail inside those same four cosmetic bones instead of adding anatomy or unrelated silhouette pieces.

### Goggles

The goggles use two translucent glass lenses, charcoal lower/side frames, copper brow pieces and bridge, a brass nose keeper, leather temple straps and small brass rivets. They stay on the official eye line and do not replace the eyes, muzzle or forehead.

### Harness

The harness is real three-dimensional geometry rather than a painted line. Two crossed front straps, a lower belt, side wraps, shoulder hardware and a layered brass/glass weather clasp follow `torso2`. The body remains visible between the straps.

### Expedition pack

The pack remains compact but is deliberately readable from three-quarter and rear views. It has a canvas body, reinforced lower section, leather flap, side pockets, rolled weather cloth, retention straps, brass buckles and a small copper/brass lightning mark. It does not replace or inflate Pikachu's torso.

### Tail clamp

The clamp is fitted locally to `tail2` as a shallow copper collar with charcoal rails, brass grounding hub, translucent indicator and two small leather keepers. It remains hardware on the official flat tail plane rather than becoming a new tail shape.

## Texture contract

The resolver keeps Cobblemon's base/shiny and emissive texture references. Ouros supplies only `ouros_storm_courier_accessories.png`, a transparent 128×64 overlay containing the eight material swatches used by the added geometry.

Those eight swatches occupy `x=0..7, y=63`. CI verifies against the exact official model that these texels are outside the UV footprint of all original Pikachu cubes, so the overlay does not repaint Pikachu's body.

## Animation and presentation

The original `cobblemon:pikachu` poser remains authoritative for presentation. The four accessory bones inherit transforms from official animated parents; there is no replacement Ouros body rig.

Blockbench review imports the official `pikachu.animation.json` through Blockbench's Bedrock animation codec. Acceptance previews use official `ground_idle`, `battle_idle` and `ground_walk` states where applicable. No project-authored renderer is accepted as visual evidence.

## Validation

`tools/cobblemon-model-review/validate_original_model.py` rejects any drift in the 90 official bones. `tools/cobblemon-model-review/build_storm_courier.py` deterministically derives both production gender models from the pinned official JAR inputs.

Visual acceptance uses pinned Blockbench 5.1.6 with the exact production geometry, official texture and Ouros overlay. Review must include the untouched official Pikachu reference plus matched-camera Storm Courier views. Front/left/right/back evidence is used for structural inspection; three-quarter hero, battle-ready and walking evidence is used for presentation review.

## Authority boundary

This asset is presentation-only. It does not use or alter Cobblemon battle-state authority, participants, legality, HP/status, tactical positions, combatant selection, damage or RNG. Ouros/AutoPTU remains authoritative for tactical battle facts.
