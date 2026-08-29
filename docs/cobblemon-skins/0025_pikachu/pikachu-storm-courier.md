# Pikachu — Storm Courier (Cobblemon 1.7.3 epic v3)

## Source of truth

Storm Courier is derived from the exact Pikachu geometry distributed in the official Cobblemon 1.7.3 Fabric JAR (`Cobblemon-fabric-1.7.3+1.21.1.jar`, Modrinth version id `kF7CvxTo`). It is not derived from a mirror, an old model, screenshots or an Ouros reconstruction.

Pinned official hashes:

- male `pikachu_male.geo.json`: `f8ea21f6821d49e8a358f05d43562312a0e018e883f1354aa1445d2a0b432c83`
- female `pikachu_female.geo.json`: `d49ba9bce368fed677832685f57a0ca3e7a00a6014639f1e79dbb0b749ed4318`
- base texture `pikachu.png`: `df0b0b2029e0cb51ace2fd7d65ce94fc6a7bf1a4681722bf20aa22edd2cc3c8e`
- official `pikachu.animation.json`: `d9ca00604978f295ad312d358a06f2655c725b30ac3da73c3637ae160c543384`

Both official gender models contain 90 bones. The derived models preserve all 90 original bones exactly at the JSON-object level and in the same order, including cubes, pivots, parents, rotations, locators and UV definitions. Male and female are derived independently so the official female tail remains intact.

## Epic v3 cosmetic geometry

Epic v3 intentionally abandons the earlier four-piece limitation. The skin must read as a major variant at gameplay distance while keeping Pikachu's anatomy untouched.

Exactly eight Ouros bones are appended. No original Cobblemon bone is rewritten:

- `ouros_storm_goggles` → parent `head_angle`
- `ouros_storm_cowl` → parent `head_angle`
- `ouros_storm_mantle` → parent `torso2`
- `ouros_storm_harness` → parent `torso2`
- `ouros_storm_pack` → parent `torso2`
- `ouros_storm_coils` → parent `torso2`
- `ouros_storm_tail_clamp` → parent `tail2`
- `ouros_storm_tail_vanes` → parent `tail2`

The result is 98 bones total: the same 90 official Pikachu bones plus eight attached cosmetic groups.

### Storm visor

The goggles are now a heavy storm visor rather than simple eyewear. Large translucent lenses, charcoal frames, copper bridge and brows, brass lightning accents, leather/navy retention and an asymmetric weather lens create a strong face signature. The original eyes, muzzle and forehead geometry remain untouched underneath.

### Open storm cowl

The cowl is built around the back and sides of the official head. A rear shell, side rails, crown wings and lower jawline guards create a more aggressive silhouette while leaving the face and ears open. It frames Pikachu rather than replacing its head.

### Shoulder mantle

The mantle adds layered navy/copper/brass pauldrons beyond the torso silhouette and two split rear storm tabs. This is the primary silhouette push in side and three-quarter views. The official arms remain separate and continue to animate through their original bones.

### Heavy storm harness

The chest harness keeps crossed straps but increases their physical depth and visual hierarchy. The central element is now a large brass/glass storm core rather than a small clasp. Shoulder locks connect the harness visually to the mantle, with utility pouches remaining secondary.

### Expedition pack

The pack is larger and more deliberate while remaining bounded around the official torso. It includes a reinforced canvas body, navy lower section, leather flap, rolled stormcloth, twin retention straps, rear lightning sigil, storm vial and folded route case. The asymmetry is intentional and should remain readable in hero three-quarter views.

### Twin storm-field pylons

Two field-coil pylons rise behind the shoulders from the pack area. Each uses charcoal structure, copper/brass rings and a translucent storm cell near the top. These pylons are the strongest new silhouette element and are meant to make Storm Courier recognizable instantly from a distance without altering Pikachu's ears or head.

### Tail grounding system

The grounding clamp is heavier than the premium pass and remains fitted to `tail2`. A second `ouros_storm_tail_vanes` group adds segmented conductor plates and side fins along the official tail plane. The tail itself remains the exact Cobblemon tail geometry underneath.

## Texture contract

The resolver keeps Cobblemon's base/shiny and emissive texture references. Ouros supplies only `ouros_storm_courier_accessories.png`, a transparent 128×64 overlay containing the eight material swatches used by the added geometry.

Those eight swatches occupy `x=0..7, y=63`. CI verifies against the exact official model that these texels are outside the UV footprint of all original Pikachu cubes, so the overlay does not repaint Pikachu's body.

## Animation and presentation

The original `cobblemon:pikachu` poser remains authoritative for presentation. All eight accessory bones inherit transforms from official animated parents; there is no replacement Ouros body rig.

Blockbench review imports the official `pikachu.animation.json` through Blockbench's Bedrock animation codec. Acceptance previews use official `ground_idle`, `battle_idle` and `ground_walk` states. No project-authored renderer is accepted as visual evidence.

## Acceptance criteria for epic v3

Epic v3 is accepted only if all of the following are true:

- all 90 original bones remain JSON-equivalent and in original order;
- exactly eight expected `ouros_*` bones are appended;
- male and female models remain independently derived;
- front, left, right and back Blockbench review shows no accidental anatomical replacement;
- hero three-quarter view reads materially more aggressively than the previous premium pass;
- the cowl, mantle and pylons remain attached in official `ground_idle`, `battle_idle` and `ground_walk` frames;
- no severe clipping or detached geometry appears in those accepted frames;
- Playable Test Build and Integration Core CI pass.

The previous 94-bone premium evidence remains historical evidence only. The epic-v3 branch must generate and commit a new 98-bone Blockbench evidence set before merge.

## Validation

`tools/cobblemon-model-review/validate_original_model.py` rejects any drift in the 90 official bones. `tools/cobblemon-model-review/build_storm_courier.py` deterministically derives both production gender models from the pinned official JAR inputs.

Visual acceptance uses pinned Blockbench 5.1.6 with the exact production geometry, official texture and Ouros overlay. Review includes the untouched official Pikachu reference plus matched-camera Storm Courier views. Front/left/right/back evidence is used for structural inspection; three-quarter hero, battle-ready and walking evidence is used for presentation review.

## Authority boundary

This asset is presentation-only. It does not use or alter Cobblemon battle-state authority, participants, legality, HP/status, tactical positions, combatant selection, damage or RNG. Ouros/AutoPTU remains authoritative for tactical battle facts.
