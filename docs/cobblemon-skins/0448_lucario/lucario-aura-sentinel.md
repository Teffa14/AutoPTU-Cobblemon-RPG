# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the active one-model artistic slice. V5 failed internal Blockbench QA. V6 preserved the exact official anatomy and passed source, attachment and reproducibility checks, but failed the matched-camera visual floor with silhouetteDeltaRatio 0.0108 against the required 0.0400. V7 is the current production candidate and remains unapproved until exact current Blockbench evidence is reviewed.

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

The mandatory same-species dossier `docs/cobblemon-skin-reference-dossiers/0448_lucario.json` remains REFERENCE READY with three complete eligible custom-geometry Lucario references across two external projects. All are STUDY_ONLY. V7 uses only general lessons about layered drape, overlapping shells, contour distribution, hierarchy and animation-safe attachment.

## V7 design

Current deterministic builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v7.py`.

V7 rebuilds all Ouros cosmetics while preserving the exact immutable 87-bone official Lucario prefix. The signature piece is a four-stage left shoulder crescent that deliberately extends outside the official arm silhouette. It overlaps into a four-panel diagonal rear resonance cape and then into three long separated rear coat tails. The chest is intentionally quiet: one diagonal sash and one low brace leave the biological chest spike and torso open. Head, arms, shins and tail use thin subordinate accents.

Production bones: 97. Official bones: 87. Cosmetic bones: 10. Current production hashes and cube count are authoritative in `docs/cobblemon-skin-review-manifests/0448_lucario.json` and are reproduced by the V7 builder.

## Texture and material contract

Normal and shiny biological textures remain byte-identical to official; `bodyTexelRework: NONE`. The accessory overlay retains twelve verified reserved alpha-zero texels with separate shadow, mid and facing-plane values for indigo cloth, lacquer, metal, antique gold and aura cyan. Cosmetic faces use darker occluded values and lighter facing/top values. Original biological UVs remain unchanged.

## Runtime and forms

The resolver keeps the official `cobblemon:lucario` poser and routes the same presentation-only V7 cosmetics over exact normal and shiny official biology. Cobblemon 1.7.3 exposes one standard Lucario geometry on this resolver path; no male/female geometry split exists here. Mega Lucario is outside this cosmetic slice.

## Evidence contract

Blockbench 5.1.6 is the required external viewer. Official reference and candidate use the same camera. Review states are `animation.lucario.ground_idle` and `animation.lucario.battle_idle` at t=0.35. A walking PNG is not fabricated because this Lucario path uses procedural locomotion rather than a dedicated Bedrock walking clip.

Green CI proves engineering only. The current status remains ARTISTIC FAIL until the exact V7 PNG set is opened and judged. If internal visual QA succeeds, the maximum pre-owner status is OWNER REVIEW REQUIRED. Only explicit owner approval of the exact head/evidence set can approve the art.
