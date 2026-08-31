# 0448 Lucario — Blue/White Maid V40

Status: ARTISTIC FAIL until exact-head Blockbench QA proves otherwise.
Sale eligibility: NOT ELIGIBLE.
Lifecycle: PROFESSIONAL_CANDIDATE.

Lucario remains the one-model lock. Owner rejection remains authoritative; CI and Blockbench can prove technical facts only. V40 is the current production candidate. The branch remains a draft PR and is not merged as accepted art.

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

The hard gate is OPEN with three COMPLETE Lucario custom-geometry references. Ruins Style Lucario and Space Style Lucario come from Lucario Overhaul 1.2.2; Covert Style Lucario comes from CobblemonMoreCosmetics 1.1.71. All remain `STUDY_ONLY`. Only general techniques are retained: animation-parented cloth drape, compound rotations, overlapping shells, continuity through animated regions, silhouette distribution and material hierarchy. No third-party geometry, UV, texture, palette, outfit, logo or distinctive motif is reused.

## V40 material rework

V39 passed the technical pipeline but failed internal visual QA because the cap still read as stacked horizontal slabs and the apron still read as a rigid front board.

V40 keeps every official biological bone JSON-equivalent and ordered. It replaces only Ouros presentation geometry. The cap is rebuilt from angled overlapping cloth lobes around the ears, with asymmetric ribbons. The apron is rebuilt from nested diagonal tiers that widen from the waist to the hem, with overlapping left/right panels, side wraps and a rear under-skirt. The intent is a continuous cloth contour rather than a rectangular plate.

Canonical V40 builder: `tools/cobblemon-model-review/build_lucario_owner_reference_v40.py`.

Current deterministic production model SHA-256 `e7041f36246ed985df8503c511dba08fdfea1d61f54b182328e952d4ae0bbdeb`. It preserves the exact ordered 87 official bones and appends 11 `ouros_*` groups, for 98 bones total and 49 cosmetic cubes.

## V40 textures

Normal production texture SHA-256 `a7b4f287929c8bbad12dbe75f33e7398e50ae88e60d06e2ef1b6a307cfed9e31`, derived independently from the exact official normal baseline. Shiny production texture SHA-256 `9f9a544181c6e4a72a966033d7064f96b75a68913a418af9791ef4d67945d268`, derived independently from the official shiny baseline. Dimensions, official UV layout and alpha semantics remain unchanged. Ear UVs are charcoal, cream landmarks use cool grey/white value structure, and tail UVs use near-charcoal dark teal so the biological tail does not dominate the costume.

Accessory overlay SHA-256 `f87ffc6a78c424c368c8cae25b164711c3c74d8282deb78a4a23833fca48686d`. Production resolver SHA-256 `6a8e2d47ea0fab34cb6bf5955609049f1cc3b8d744ad6c8155333a36eb7be0ba`.

No male/female geometry split exists on the official 1.7.3 resolver path. Mega Lucario remains outside this cosmetic slice.

## Evidence contract

Blockbench 5.1.6, AppImage SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`, matched camera. Hero uses `animation.lucario.ground_idle`; battle uses `animation.lucario.battle_idle`, both at `t=0.35`. No dedicated walking render is fabricated because this Lucario path has no equivalent dedicated Bedrock walking clip.

V40 remains `ARTISTIC FAIL` until the exact current-head PNG set is generated and opened for visual inspection. Green source, anatomy, attachment, texture, build or Blockbench gates cannot grant artistic approval. Only the owner can approve the exact current evidence set.
