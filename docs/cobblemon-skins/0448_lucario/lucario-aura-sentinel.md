# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the active one-model artistic slice. V5 failed internal visual QA. V6 failed the matched-camera silhouette floor. V7 cleared the floor but its lower panels read as oversized shorts and its cape fragmented into detached-looking tiles. V8 became too visually close to base, with pixelDifferenceRatio 0.0725 below the unchanged 0.0800 floor. V9 cleared both technical floors at pixelDifferenceRatio 0.113329 and silhouetteDeltaRatio 0.044911, but direct inspection rejected it artistically because the signature haori read as a lateral chain of rectangular plates and the front still read as Lucario plus pieces. V10 rendered correctly in Blockbench but failed the unchanged silhouette floor at silhouetteDeltaRatio 0.0350 versus the required 0.0400. V11 is the current production candidate and remains ARTISTIC FAIL until its exact current Blockbench evidence is generated and inspected.

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

The mandatory same-species dossier `docs/cobblemon-skin-reference-dossiers/0448_lucario.json` remains REFERENCE READY with three complete custom-geometry Lucario references across two external projects. Ruins Style Lucario, Space Style Lucario and Covert Style Lucario are all STUDY_ONLY. Only generic techniques are carried forward; their geometry, UVs, textures, palettes, outfits, motifs and distinctive silhouettes are not donor assets.

## V11 design

Current deterministic builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v11.py`.

V11 responds to V10's measured silhouette failure without relaxing any threshold. The primary read is one shoulder-rooted resonance crest built from broad overlapping rotated surfaces that change scale and angle, connected into a rear mantle that narrows through the torso and splits into unequal tails with centre negative space. The front remains sparse: open-face cowl, diagonal lapel and obi, with Lucario's face and biological chest spike unobstructed. It does not introduce a skirt, shorts, cage, portal frame, repeated bar system or alternate body rig.

The builder reconstructs the exact official 87-bone Lucario prefix and appends 9 `ouros_*` cosmetic bones. Current materialized V11 production is 96 bones and 21 cosmetic cubes. Production model SHA-256 is `5e998aef0facc95fc155282943c6212d0fdcbdf9bfbd6e7c55a82cd60d71fd4a`.

## Texture and material contract

Normal and shiny biological textures remain byte-identical to official; `bodyTexelRework: NONE`. The accessory overlay SHA-256 is `9ba1c3c0c6e1ab7075f432a248fa4b1a9137bb2b1d8906f1cc3fcbe1f61dc8ee`, using verified alpha-zero texels for authored cloth/equipment material breakup. Biological UVs are unchanged.

## Runtime and forms

The resolver keeps the official `cobblemon:lucario` poser and routes the same V11 presentation cosmetics over exact normal and shiny official biology. No male/female geometry split exists on this official resolver path. Mega Lucario is outside the slice.

## Evidence contract

Blockbench 5.1.6 is mandatory with matched official camera. Review states are `animation.lucario.ground_idle` and `animation.lucario.battle_idle` at t=0.35. No walking PNG is fabricated because this path uses procedural locomotion rather than a dedicated Bedrock walking clip.

The visual floor remains `minimumPixelDifferenceRatio: 0.08` and `minimumSilhouetteDeltaRatio: 0.04`. It can reject trivial transformation but cannot grant artistic approval. Green CI is engineering evidence only. If internal QA succeeds, the maximum pre-owner state is OWNER REVIEW REQUIRED. Only explicit owner approval of the exact head/evidence set can approve the art.
