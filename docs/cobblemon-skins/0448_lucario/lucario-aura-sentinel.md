# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL pending exact-head V21 Blockbench QA.
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the active one-model artistic slice. Owner rejection remains authoritative. CI, Blockbench and validators establish technical evidence only; they cannot approve art.

## Authority boundary

Presentation only. Cobblemon supplies model, texture, animation, poser, resolver and rendering surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and outcomes.

## Official baseline

Cobblemon 1.7.3 Fabric for Minecraft 1.21.1, Modrinth `kF7CvxTo`, JAR `Cobblemon-fabric-1.7.3+1.21.1.jar`.

JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`.
JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`.
Official model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`, 87 bones.
Official normal texture `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`.
Official shiny texture `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`.
Animation `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`.
Poser `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`.
Resolver `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`.
Model license `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`.

The mandatory dossier `docs/cobblemon-skin-reference-dossiers/0448_lucario.json` remains REFERENCE READY with Ruins Style Lucario, Space Style Lucario and Covert Style Lucario, all COMPLETE same-species custom-geometry inspections and all STUDY_ONLY.

## Current V21 production

Canonical builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v21b.py`.

V20 passed the unchanged technical visual floors but direct PNG QA rejected its four-piece half-cloak as stacked dark plates. V21 removes that hanging cascade and dorsal handoff. The exterior design now uses cowl/high collar, a two-facet oblique shoulder wrap, one three-facet diagonal back-to-hip contour mantle, and two small asymmetric hip counterweights. Center back, chest spike, biological tail and legs remain open.

Materialized model SHA-256 `4b2b276a46562ad03ebc8df54691c2a31c982fe79e5307e3c013ab28c3ad2555`. It contains the exact ordered 87 official bones plus 5 `ouros_*` cosmetic bones, 92 total, with 13 cosmetic cubes.

## Derived texture/material pass

V21 introduces a deterministic body repaint instead of using more geometry to manufacture identity.

Normal production texture SHA-256 `80994f4d80bf1014807f5502fe68fe9e37163cb5affd7b88cf774f14f47a1848`, independently derived from official normal `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`.

Shiny production texture SHA-256 `749e426342263281ccc0114c06b82ff8fc5fdbe3c5643932beea4d0060ab8615`, independently derived from official shiny `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`.

The paint preserves dimensions, original UV mappings and alpha semantics. Existing blue biological texels receive local aura-lacquer value ramps. Existing dark biological texels receive indigo/blue-steel occlusion and sparse facing highlights. Cream spikes, white landmarks and red eyes remain readable. Metadata lives in `v21-derived-normal.json` and `v21-derived-shiny.json`; `validate_derived_texture.py` passed during deterministic materialization for both variants.

Accessory overlay SHA-256 `9ba1c3c0c6e1ab7075f432a248fa4b1a9137bb2b1d8906f1cc3fcbe1f61dc8ee`. Production resolver SHA-256 `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`.

No male/female geometry split exists on this official resolver path. Mega Lucario is outside the slice.

## Evidence contract

Blockbench 5.1.6, AppImage SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`, matched camera. Hero uses `animation.lucario.ground_idle`; battle uses `animation.lucario.battle_idle`, both at t=0.35. No dedicated walking PNG is fabricated because this official path uses procedural locomotion.

Technical floors remain unchanged at pixel difference 0.08 and silhouette delta 0.04. This commit binds V21 documentation to the materialized production bytes so the exact-head professional Blockbench review can run. Owner approval remains absent.
