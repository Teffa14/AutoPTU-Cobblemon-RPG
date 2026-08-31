# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL until exact-head Blockbench QA proves otherwise.
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the one-model lock. Owner rejection remains authoritative; CI and Blockbench only prove technical facts. Current production is Resonance Ronin V24. The branch has been merged safely with current `main` `91bd303306b80774fc0b3c5336321dd34b0d7df9`; no force-push was used.

## Authority boundary

Presentation only. Cobblemon supplies model, texture, animation, poser, resolver and rendering surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.

## Official baseline

Cobblemon 1.7.3 Fabric for Minecraft 1.21.1, Modrinth version `kF7CvxTo`, JAR `Cobblemon-fabric-1.7.3+1.21.1.jar`.

JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`.
JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`.
Official model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`, 87 bones.
Official normal `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`.
Official shiny `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`.
Animation `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`.
Poser `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`.
Resolver `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`.
Model license `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`.

## Same-species custom reference gate

The hard gate is OPEN with three COMPLETE Lucario custom-geometry references. Ruins Style Lucario and Space Style Lucario come from Lucario Overhaul 1.2.2; Covert Style Lucario comes from CobblemonMoreCosmetics 1.1.71. All are `STUDY_ONLY`. Only general techniques are retained: parent-safe drape, compound rotations, overlapping shells, continuity through animated regions, concentrated silhouette density and material hierarchy. No third-party geometry, UV, texture, palette, outfit, logo or distinctive motif is reused.

## V23 rejection and V24 response

V23 passed source, anatomy, attachment and derived-texture validation, but exact Blockbench review measured `silhouetteDeltaRatio=0.0146 < 0.0400`. Direct review showed the body-hugging wrap disappearing behind Lucario at hero and gameplay scale.

V24 keeps the independently derived paint and replaces the hidden wrap with one diagonal shoulder-to-back aura mantle. Two intersecting root shells launch three heavily overlapping narrowing sweep facets; four descending facets continue the same gesture toward the hip and split around tail/legs. A single narrow chest edge connects the gesture without covering the official chest spike. The intent is one continuous silhouette, not a crown, waistcoat, backpack, frame or scattered armor kit.

The first V24 materialization exposed one real attachment defect in the upper sweep tip (`bodyGap=2.6`, `nearestSiblingGap=2.95`). The attachment thresholds were not relaxed. The tip was pulled inward and widened until it overlapped its predecessor, then the candidate was deterministically rematerialized.

Canonical V24 builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v24.py`.

Attachment-fixed production model SHA-256 `2f32f54aa6bee14b6ee7da310728b02f57d01222a8d40c910f4cd77af0e01cfc`. It preserves the exact ordered 87 official bones and appends four `ouros_*` presentation groups, 91 bones total, with 10 cosmetic cubes.

## V24 textures

Normal production texture SHA-256 `5b2c5812a6916bff5eb5fe66827343f98d8ec8fe61a3c4598e8d980550290262`, independently derived from the exact official normal baseline. Shiny production texture SHA-256 `4f5a85766999c27a6b8dcf04ef0770f79b2f79a058ef1457d9c3b5f1d9e6fee5`, independently derived from the exact official shiny baseline. Dimensions, UV layout and alpha semantics remain unchanged. Cream spikes, white landmarks and red eyes remain protected. Blue biology receives restrained cobalt value shaping; dark biology receives indigo occlusion and sparse highlights.

Accessory overlay SHA-256 `9ba1c3c0c6e1ab7075f432a248fa4b1a9137bb2b1d8906f1cc3fcbe1f61dc8ee`. Production resolver SHA-256 `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`.

No male/female geometry split exists on the official 1.7.3 resolver path. Mega Lucario is outside this cosmetic slice.

## Evidence contract

Blockbench 5.1.6, AppImage SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`, matched camera. Hero uses `animation.lucario.ground_idle`; battle uses `animation.lucario.battle_idle`, both at `t=0.35`. No dedicated walking render is fabricated because this Lucario locomotion path is procedural rather than a dedicated Bedrock walking clip.

V24 remains `ARTISTIC FAIL` until the exact current-head PNG set is generated and inspected. Passing technical floors cannot grant artistic approval; only the owner can approve the exact current evidence set.
