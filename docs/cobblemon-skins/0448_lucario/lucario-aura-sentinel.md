# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the active one-model artistic slice. The owner-wide rejection remains authoritative. No ChatGPT, CI, validator or Blockbench metric may approve this art; the maximum pre-owner state is OWNER REVIEW REQUIRED after exact-head evidence is generated and inspected.

The previous exact V15 head `0bf6e61c00fc01f7d22a55e3ca123534e283725f` passed the hard reference gate, official-source verification, deterministic builder reproduction, original-bone equality, attachment and Blockbench capture, but failed the unchanged matched-camera silhouette floor at `silhouetteDeltaRatio=0.0303` versus the required `0.0400`. V15b materially reauthors the signature contour rather than lowering that threshold.

## Authority boundary

Presentation only. Cobblemon supplies model, texture, animation, poser, resolver and rendering surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and outcomes. No Cobblemon/Minecraft battle-state authority is introduced.

## Official baseline

Cobblemon 1.7.3 Fabric for Minecraft 1.21.1, Modrinth version `kF7CvxTo`, JAR `Cobblemon-fabric-1.7.3+1.21.1.jar`.

JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`.
JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`.
Official model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9`; official bone count 87.
Official normal texture SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`.
Official shiny texture SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`.
Animation SHA-256 `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`.
Poser SHA-256 `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`.
Base resolver SHA-256 `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`.
Model license SHA-256 `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`.

The mandatory same-species dossier `docs/cobblemon-skin-reference-dossiers/0448_lucario.json` is REFERENCE READY with three COMPLETE custom-geometry Lucario references across two external projects: Ruins Style Lucario, Space Style Lucario and Covert Style Lucario. All remain STUDY_ONLY. Only construction techniques are carried forward; geometry, UVs, textures, palettes, outfits, motifs and distinctive silhouettes are not donor assets.

## Current V15b design

Deterministic builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v15.py`.

The materialized V15b model SHA-256 is `30a3d63e55f5ac5918d9f0a718f556cdfee63a22376caed1cb84e2b6672fd03c`. It contains the exact ordered 87-bone official Lucario prefix plus 13 `ouros_*` cosmetic bones, for 100 total bones and 31 cosmetic cubes.

The main authored read is a continuous cowl/collar/shoulder/back/hip mantle arc. A four-stage shoulder-rooted crescent expands from a real shoulder contact mass, then progressively narrows, rotates and recedes. The opposite hip resolves the diagonal with shorter split ribbons and explicit negative space around the biological tail. The face, aura sensors and chest spike remain open. No alternate body rig, rectangular cage, shrine frame, broad portal silhouette or threshold relaxation is introduced.

## Texture and material contract

Normal and shiny biological textures remain byte-identical to official; `bodyTexelRework: NONE`. The accessory overlay SHA-256 is `9ba1c3c0c6e1ab7075f432a248fa4b1a9137bb2b1d8906f1cc3fcbe1f61dc8ee`. Biological UVs are unchanged. The accessory material ramp uses dark occlusion faces, lighter facing planes, antique-gold accents and aura-cyan terminal edges on verified free texels.

## Runtime and forms

The production resolver SHA-256 is `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`. It keeps the official `cobblemon:lucario` poser and routes the same cosmetics over exact normal and shiny official biology. No male/female geometry split exists on this official resolver path. Mega Lucario is outside the slice.

## Evidence contract

Blockbench 5.1.6 is mandatory, AppImage SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`, with matched official camera. Review states are `animation.lucario.ground_idle` and `animation.lucario.battle_idle` at t=0.35. No walking PNG is fabricated because this official path uses procedural locomotion rather than a dedicated Bedrock walking clip.

The visual floor remains `minimumPixelDifferenceRatio: 0.08` and `minimumSilhouetteDeltaRatio: 0.04`. These values are technical minimums only and cannot grant artistic approval. The current human-head review must regenerate exact production evidence after the materializer commit and remains ARTISTIC FAIL until those PNGs are actually inspected.
