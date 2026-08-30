# 0448 Lucario — Aura Sentinel: Resonance Ronin

Status: ARTISTIC FAIL
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the active one-model artistic slice. Historical Aura Sentinel passes and Resonance Ronin V1–V5 are superseded or rejected. V6 is the current production candidate. It remains unapproved and must not be merged as accepted art unless the owner explicitly approves the exact current Blockbench evidence set.

## Authority boundary

Presentation only. Cobblemon supplies model, texture, animation, poser, resolver and rendering surfaces. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and outcomes. No Cobblemon/Minecraft battle-state authority is used.

## Exact official source

Cobblemon 1.7.3 Fabric for Minecraft 1.21.1, Modrinth version `kF7CvxTo`, JAR `Cobblemon-fabric-1.7.3+1.21.1.jar`.

JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`.
JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`.
Official model SHA-256 `ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9` with 87 bones.
Official normal texture SHA-256 `98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a`.
Official shiny texture SHA-256 `b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d`.
Animation SHA-256 `ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563`.
Poser SHA-256 `7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d`.
Base resolver SHA-256 `a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81`.
Model license SHA-256 `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`.

The mandatory dossier `docs/cobblemon-skin-reference-dossiers/0448_lucario.json` remains `REFERENCE READY` with three complete eligible custom-geometry Lucario references across two external Cobblemon-pack projects. They remain `STUDY_ONLY`; no external geometry, UVs, textures, palettes, markings, motifs or distinctive costume designs are copied.

## Why V5 failed

The V5 exact Blockbench contact sheet was opened before starting V6. The rear mantle collapsed visually into a black backpack, the broad lower panels read as oversized blue shorts from front/three-quarter, and small purple accents behaved as disconnected accessory islands. Gameplay-scale fantasy was weak. V5 therefore remained `ARTISTIC FAIL` despite passing its technical review.

## V6 visual direction

V6 rebuilds all ten Ouros cosmetic groups instead of incrementally stacking more pieces. The current signature system is a thin scalloped left shoulder veil made from progressively rotated overlapping shells. That contour feeds a rear resonance veil descending from the left scapula toward the hip. The chest is reduced to one diagonal sash plus two short lower braces so the biological chest spike and center torso remain open. Three long rear-biased coat tails extend the silhouette downward without covering the front thighs.

The former large rear hardware is gone. Arm and shin treatments are reduced to thin bands/plates, and the tail gets only a small clasp. The face, muzzle, ears, aura sensors, hands, feet, chest spike and biological tail remain visually dominant.

## Geometry contract

Current deterministic builder: `tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v6.py`.

V6 starts from the exact immutable 87-bone official Lucario prefix, discards all previous Ouros cosmetic groups, then appends ten newly authored `ouros_*` groups. No official parent, pivot, rotation, cube, locator, UV or ordering entry is modified.

Production bones: 97. Official bones: 87. Cosmetic bones: 10. Cosmetic cubes: 30. Current model SHA-256 `e129caedac65afde398f65da55a55c3674e5891f602e1816d0bcbf768e4cbbb8`.

Attachment thresholds remain `anchorGap=1.5` and `pieceGap=1.0`; these thresholds are not relaxed for V6.

## Texture and material contract

Biological normal and shiny textures remain byte-identical to their official Cobblemon counterparts. `bodyTexelRework: NONE`.

The V6 accessory overlay uses twelve reserved alpha-zero texels and adds explicit dark/mid/facing values for indigo cloth, lacquer, metal, antique gold and aura cyan. Individual cosmetic cube faces use darker occluded values and lighter facing/top values rather than one uniform color per piece.

Overlay SHA-256 `9ba1c3c0c6e1ab7075f432a248fa4b1a9137bb2b1d8906f1cc3fcbe1f61dc8ee`.
Resolver SHA-256 `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`.

## Runtime routing and variants

The resolver keeps the official `cobblemon:lucario` poser and the existing `ouros_aura_sentinel` presentation aspect. Normal and shiny route the same V6 cosmetics over their exact official biological textures.

Cobblemon 1.7.3 exposes one standard Lucario geometry on this resolver path. There is no male/female geometry split here. Mega Lucario remains outside this cosmetic slice.

## Professional review contract

Manifest: `docs/cobblemon-skin-review-manifests/0448_lucario.json`.

Every final human head must reproduce exact production bytes, verify the current official Cobblemon source, prove original-bone equality and cosmetic attachment, then render the production model in pinned Blockbench 5.1.6 with the exact matched official camera.

Official review states are `animation.lucario.ground_idle` and `animation.lucario.battle_idle` at t=0.35. A dedicated walking image is not fabricated because this official Lucario path uses procedural locomotion rather than a dedicated Bedrock walking clip.

Required evidence includes official reference three-quarter, hero three-quarter, battle-ready three-quarter, front/back/side structural views, 160 px gameplay scale, contact sheet, PNG hashes and exact-head review contract.

Green CI is technical evidence only. Current art status remains `ARTISTIC FAIL` until the exact V6 Blockbench PNGs are opened and visually judged. If a future candidate passes internal visual QA, its maximum pre-owner state is `OWNER REVIEW REQUIRED`.
