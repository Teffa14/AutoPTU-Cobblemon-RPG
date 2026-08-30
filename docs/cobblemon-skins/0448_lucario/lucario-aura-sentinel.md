# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: OWNER REVIEW REQUIRED
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

This is the current active Lucario skin candidate. Historical Aura Sentinel cowl/shrine-frame passes are rejected and do not define this design. This candidate is a new deterministic build from the immutable official Lucario bone prefix plus newly authored Ouros geometry.

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

The strict same-species reference dossier is `docs/cobblemon-skin-reference-dossiers/0448_lucario.json`. It is `REFERENCE READY` after full model/texture inspection of three eligible Lucario custom skins across two external Cobblemon-pack projects. Their geometry, textures, palettes and distinctive costume identities are study-only; this candidate copies none of them.

## New visual direction

Resonance Ronin replaces the rejected shrine-sentinel language with a mobile ceremonial combat silhouette. The design is intentionally diagonal and wrapped rather than rectangular.

The three dominant reads are:

1. an asymmetric resonance shawl that wraps both shoulders into the torso;
2. one sweeping dorsal resonance banner that rises diagonally from the left back instead of forming a portal, cage or backpack frame;
3. a split battle coat that carries the transformation through the hips and legs.

A low open circlet, tapered cuirass, forearm vambraces, shin greaves and a partial tail guard support those macro systems. The face, muzzle, eyes, ears, aura sensors, chest spike, hands, feet and biological tail remain identifiable and are not replaced.

## Geometry contract

Builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin.py`.

The builder deliberately discards every historical `ouros_*` bone before authoring the new candidate. It preserves only the immutable first 87 official Lucario bones, JSON-equivalent and in official order.

Current generated asset:

- production bones: 97
- cosmetic bones: 10
- cosmetic cubes: 72
- model SHA-256 `9631f8c763aea2df061e2798690d34485f43ef64495d9dd128902ff956afaa5a`

Cosmetic groups:

- `ouros_resonance_circlet`
- `ouros_resonance_shawl`
- `ouros_resonance_cuirass`
- `ouros_resonance_banner`
- `ouros_resonance_coat`
- `ouros_resonance_left_vambrace`
- `ouros_resonance_right_vambrace`
- `ouros_resonance_left_greave`
- `ouros_resonance_right_greave`
- `ouros_resonance_tail_guard`

Every group is parented into an official animated Lucario bone. The professional gate must still prove exact original-bone equality and the strict bind-pose attachment limits (`anchorGap=1.5`, `pieceGap=1.0`) against the downloaded official JAR.

## Texture and material contract

Biological body repainting is forbidden.

- normal body: byte-identical to official, SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`
- shiny body: byte-identical to official, SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`
- `bodyTexelRework: NONE`
- accessory overlay: `ouros_aura_sentinel_accessories.png`
- overlay SHA-256 `322520d35d4919c57e52268b2409cab2ed02529b71b67515128500a282b3dd1e`
- overlay size: 128×64
- authored palette reservation: x=80..87, y=63 only

The equipment palette separates midnight cloth, indigo, blue steel, silver, antique gold, aura cyan, amethyst and ivory. The overlay is transparent outside the reserved accessory texels. Original biological UVs remain unchanged.

## Runtime routing and variants

The production resolver keeps the official `cobblemon:lucario` poser and the existing `ouros_aura_sentinel` aspect for compatibility. Normal and shiny route to the same new geometry and accessory overlay while using their exact corresponding official body textures.

Cobblemon 1.7.3 exposes one standard Lucario geometry on this resolver path. There is no male/female model split to duplicate. Mega Lucario is outside this cosmetic slice.

Resolver SHA-256: `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`.

## Professional review contract

Manifest: `docs/cobblemon-skin-review-manifests/0448_lucario.json`.

The exact PR head must reproduce production bytes, download and hash the current official Cobblemon JAR, validate official anatomy and attachment, and render the candidate in Blockbench 5.1.6 with the same camera used for the official reference.

Required evidence includes:

- `official_reference_three_quarter.png`
- `hero_three_quarter.png`
- `battle_ready_three_quarter.png`
- `hero_front.png`
- `hero_back.png`
- `hero_gameplay_160.png`
- `contact_sheet.png`

Official animation states are `animation.lucario.ground_idle` and `animation.lucario.battle_idle` at 0.35. Walking is not fabricated because Lucario locomotion is procedural rather than a dedicated Lucario Bedrock walking clip.

Green CI is only the technical floor. The PNG artifact must be opened and inspected. The candidate remains `OWNER REVIEW REQUIRED` and not sale-eligible until the repository owner explicitly approves the exact reviewed head and evidence fingerprint.

## Production files

- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/90_ouros_aura_sentinel.json`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel.png`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png`
- `fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png`
- `docs/cobblemon-skin-review-manifests/0448_lucario.json`
- `docs/cobblemon-skin-reference-dossiers/0448_lucario.json`
