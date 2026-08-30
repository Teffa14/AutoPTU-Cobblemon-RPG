# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the active one-model artistic slice. Historical Aura Sentinel cowl/shrine-frame passes and Resonance Ronin V1–V4 are superseded or rejected. V5 is a material rework from the exact official Cobblemon Lucario baseline, but it is not artistically approved and must not be merged as accepted art unless the owner explicitly approves the exact current Blockbench evidence set.

## Authority boundary

Presentation only. Cobblemon supplies model, texture, animation, poser, resolver and rendering surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and outcomes. No Cobblemon/Minecraft battle-state authority is used.

## Exact official source

- Cobblemon 1.7.3 Fabric for Minecraft 1.21.1
- Modrinth version `kF7CvxTo`
- JAR `Cobblemon-fabric-1.7.3+1.21.1.jar`
- JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`
- official model `assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json`
- model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`
- official bones: 87
- animation SHA-256 `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`
- poser SHA-256 `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`
- base resolver SHA-256 `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`
- official normal texture SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- official shiny texture SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- model license SHA-256 `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`

The mandatory same-species dossier is `docs/cobblemon-skin-reference-dossiers/0448_lucario.json`. It remains `REFERENCE READY` with three complete eligible custom-geometry Lucario references across two external Cobblemon-pack projects. All three are `STUDY_ONLY` for this workflow; no external geometry, UV, texture, palette, markings or distinctive costume identity are reused.

## V5 visual direction

V4 became too quiet after removing the earlier scaffold and box-armor reads. V5 keeps the cleaner open anatomy but restores authored presence with a single connected left-heavy crescent system.

The dominant reads are:

1. a continuous asymmetric mantle that starts at the left shoulder, overlaps through the rear torso and descends toward the left hip;
2. an open four-plane cuirass that deliberately preserves the biological chest spike and center chest as negative space;
3. three broad descending battle-coat panels that continue the same diagonal rhythm into the lower silhouette.

The design avoids a vertical banner, shrine frame, portal silhouette and repeated straight hardware. The face, muzzle, eyes, ears, aura sensors, chest spike, hands, feet and biological tail remain identifiable and unchanged underneath.

## Geometry contract

Current builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v5.py`.

The deterministic V5 builder first reconstructs the accepted technical V4 baseline from the exact immutable 87-bone official prefix, then replaces only five Ouros cosmetic groups. No official bone is changed, reordered or remapped.

Current generated asset:

- production bones: 97
- original official bones: 87
- cosmetic bones: 10
- cosmetic cubes: 56
- model SHA-256 `8b7c555d34492067587cd17c2921fdc7ef772111a9b99f8a1f32b63bed9b67d4`

V5 materially rewrites:

- `ouros_resonance_shawl` — continuous shoulder/rear/hip crescent mantle
- `ouros_resonance_cuirass` — simplified open four-plane chest framing
- `ouros_resonance_coat` — three broad descending panels with center air
- `ouros_resonance_left_greave` — reduced two-piece tapered shin treatment
- `ouros_resonance_right_greave` — reduced two-piece tapered shin treatment

Other existing Ouros groups remain subordinate support pieces. Every cosmetic group stays parented into the official animated Lucario hierarchy. The professional gate must continue proving original-bone JSON equality plus strict bind-pose attachment limits (`anchorGap=1.5`, `pieceGap=1.0`).

## Texture and material contract

V5 does not repaint biological Lucario texels.

- normal body: byte-identical to official, SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- shiny body: byte-identical to official, SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- body derivation: `OFFICIAL_IDENTICAL`
- accessory overlay: `ouros_aura_sentinel_accessories.png`
- overlay SHA-256 `322520d35d4919c57e52268b2409cab2ed02529b71b67515128500a282b3dd1e`
- overlay size: 128×64
- authored palette reservation: x=80..87, y=63 only

The overlay supplies midnight cloth, indigo, blue steel, silver, antique gold, aura cyan, amethyst and ivory for the cosmetic surfaces. Original biological UVs remain unchanged.

## Runtime routing and variants

The production resolver keeps the official `cobblemon:lucario` poser and the existing `ouros_aura_sentinel` presentation aspect. Normal and shiny use the same V5 cosmetic geometry and overlay over their exact corresponding official body textures.

Cobblemon 1.7.3 exposes one standard Lucario geometry on this resolver path. There is no male/female model split to duplicate. Mega Lucario is outside this cosmetic slice.

Resolver SHA-256: `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`.

## Professional review contract

Manifest: `docs/cobblemon-skin-review-manifests/0448_lucario.json`.

The exact human PR head must reproduce the committed V5 production bytes, download and hash the current official Cobblemon JAR, prove original anatomy and attachment, and render the candidate in pinned Blockbench 5.1.6 with the matched official camera.

Required evidence includes `official_reference_three_quarter.png`, `hero_three_quarter.png`, `battle_ready_three_quarter.png`, structural front/back/side views, `hero_gameplay_160.png` and `contact_sheet.png`.

Official states used by the professional review are `animation.lucario.ground_idle` and `animation.lucario.battle_idle` at 0.35. A dedicated walking PNG is not fabricated because this Lucario path uses procedural locomotion rather than a dedicated Lucario Bedrock walking clip.

Green CI is technical evidence only. V5 remains `ARTISTIC FAIL` until its exact current Blockbench PNGs are opened and pass internal visual QA. Even if internal QA later passes, the maximum state before owner approval is `OWNER REVIEW REQUIRED`.

## Production files

- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/90_ouros_aura_sentinel.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel.png`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png`
- `docs/cobblemon-skin-review-manifests/0448_lucario.json`
- `docs/cobblemon-skin-reference-dossiers/0448_lucario.json`
