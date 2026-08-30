# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL pending exact-head V22 Blockbench QA.
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the active one-model slice. Owner rejection remains authoritative. Tooling can prove technical validity but cannot grant artistic approval.

## Authority boundary

Presentation only. Cobblemon supplies model, textures, animation, poser, resolver and rendering surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.

## Official baseline

Cobblemon 1.7.3 Fabric for Minecraft 1.21.1, Modrinth `kF7CvxTo`, JAR `Cobblemon-fabric-1.7.3+1.21.1.jar`.

JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`.
JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`.
Official model `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`, 87 bones.
Official normal `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`.
Official shiny `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`.
Animation `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`.
Poser `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`.
Resolver `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`.
Model license `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`.

The hard same-species dossier remains OPEN with three COMPLETE custom-geometry Lucario references: Ruins Style, Space Style and Covert Style. All remain STUDY_ONLY; only general techniques were used.

## V21 rejection and V22 response

V21 introduced the valid derived-texture pipeline but exact-head Blockbench evidence failed `silhouetteDeltaRatio=0.0221 < 0.0400`. Direct QA also found electric-blue thigh masses and a legacy cowl/plate read. V22 removes all V21 legacy cosmetic groups instead of stacking more parts on them.

Canonical V22 builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py`.

Materialized production model SHA-256 `ff2593ae46059a58b566ff576ecabcf638c629a90f72c4ca67246aba1afde6f1`. It preserves the exact ordered 87 official bones and appends 6 `ouros_*` groups, 93 total, with 12 cosmetic cubes/planes.

V22 systems: open-face thin temple crown; asymmetric two-shell mantle root; three-plane diagonal dorsal mantle crest with broken-crescent negative space; single breast sash leaving the chest spike open; two unequal split waistcoat tails; shallow arm guards attached to official animation hierarchy.

## V22 texture pass

Normal production texture SHA-256 `cea10228d7cde9aaba3c15c40de5ac24a79ed30275a865617d22abb352c338ee`, independently derived from the exact official normal baseline.

Shiny production texture SHA-256 `afc12aa7ab9c62ffefa51e96c03c3f282bea7f8116493d283f257815a49e487c`, independently derived from the exact official shiny baseline.

The repaint keeps dimensions, UV layout and alpha semantics. Existing blue biology is shifted to restrained deep cobalt with local value ramps and sparse facing highlights; dark biology receives indigo occlusion. Cream spikes, white landmarks and red eyes remain readable. Metadata is recorded in `v22-derived-normal.json` and `v22-derived-shiny.json` and validated by `validate_derived_texture.py`.

Accessory overlay SHA-256 `9ba1c3c0c6e1ab7075f432a248fa4b1a9137bb2b1d8906f1cc3fcbe1f61dc8ee`. Production resolver SHA-256 `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`.

No male/female geometry split exists on this official resolver path. Mega Lucario is outside this cosmetic slice.

## Evidence contract

Blockbench 5.1.6, AppImage SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`, matched camera. Hero uses `animation.lucario.ground_idle`; battle uses `animation.lucario.battle_idle`, both at t=0.35. No walking render is fabricated because the official Lucario locomotion path is procedural rather than a dedicated Bedrock walking clip.

Technical floors remain unchanged: pixel difference 0.08 and silhouette delta 0.04. This human commit binds the V22 production bytes to a fresh exact-head Blockbench run. Owner approval remains absent.
