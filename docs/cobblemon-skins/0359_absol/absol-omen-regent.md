# 0359 Absol — Omen Regent

Status: EPIC V2 ACCEPTED IN BLOCKBENCH — repository-wide PR gates pending.

## Official source

Omen Regent is derived from the exact Absol assets shipped in the official Cobblemon 1.7.3 Fabric JAR for Minecraft 1.21.1, Modrinth version `kF7CvxTo`. The inspection workflow also queried the current Fabric/1.21.1 release list before generation and confirmed this version remained the latest stable compatible release at the time of the slice.

Pinned SHA-256 values:
- model: `ed8b82b60caaeb0ee8b97597b5bd194a52d52d671da7bbc0eb35aa8dd864d462`
- animation: `3e706148f3159fb60b51acd8221d4a1292306a8a780ab4b69a874eb72700ece4`
- poser: `d47d56606e412b794261ee3b8220d80b93da576f030f40e5b28eebe7ad8f06be`
- resolver: `41048e936f18c108331a0d739388fef686a8f01be72cbf70e4a146603aa3e832`
- normal texture: `9f0ce5dcf5ccc6bcb24e6821f2ee7c8dbd572c0e9ce8634a548d1ec8b1fc26e7`
- shiny texture: `633051ce706888f14ff59afda37e22d4d82debab5716445c3ae1d603fc97a6a1`

The exact JAR inventory contains one Absol geometry, one poser, one resolver, one animation file and normal/shiny textures. No species-local male/female or alternate-form geometry is distributed under `0359_absol`, so the cosmetic has one geometry derivative and preserves the resolver's two official presentation branches.

Compact source inspection evidence is retained under `docs/cobblemon-skins/0359_absol/official-inspection/`. The skin does not copy geometry from Unite, fan mods, servers or other third-party cosmetics. Upstream Cobblemon source/license obligations continue to apply; no separate species-local license file appeared in the Absol asset inventory.

## Anatomy contract

The official model contains 73 bones. Omen Regent V2 keeps all 73 bones JSON-equivalent and in the same order, then appends eight `ouros_*` cosmetic groups for 81 total bones. `validate_original_model.py` verifies this directly against `absol.geo.json` extracted from the pinned JAR.

The eight cosmetic groups are an open omen crown, armored gorget/core, asymmetric left and right pauldrons, a wide split mantle, a rear eclipse frame, a tail-root reliquary and a large asymmetric rear relic. V2 contains more than 55 cosmetic cubes. Head, face, biological horn, fur, torso, legs, feet and tail remain the exact Cobblemon anatomy beneath these additions.

## Epic V2 visual pass

The first generated pass was structurally valid but artistically rejected after inspecting real Blockbench screenshots. It read as Absol plus thin hardware; the rear standards were too small and the three-quarter silhouette did not clear the project's epic-skin threshold.

V2 deliberately increases hierarchy and mass. Shoulder plates extend well beyond the body, the split mantle creates a broad lower silhouette, the rear frame becomes a thick broken eclipse instead of two thin standards, and the asymmetric relic creates a strong diagonal read behind the natural horn. Purple aura insets and gold/silver edge hardware provide secondary contrast while dark cloth/armor remains the dominant material family.

Direct Blockbench review accepted V2. The official reference and skin use the same model scale, camera, `animation.absol.ground_idle` clip and frame. The skin remains unmistakably Absol because the red eyes, muzzle, white head, natural horn, fur, quadruped stance and tail remain visible. `animation.absol.cry` was also rendered and keeps cosmetic groups attached to their intended animated parents without severe detachment.

The official poser defines standing with `ground_idle` and walking with `ground_idle` plus procedural `q.quadruped_walk`. The official Bedrock animation file does not contain a dedicated walking or battle-idle clip. Accordingly, this evidence set does not fabricate `walking_three_quarter` or `battle_ready_three_quarter`; those omissions are explicit in pose metadata.

## UV and resolver contract

The official texture size is 128×128. Generation computes the original model's UV occupancy before assigning material swatches. Omen Regent uses eight texels proven outside that footprint; every non-transparent pixel in the Ouros overlay must equal that reserved set.

The production resolver preserves the two official branches: normal and `shiny`. It prepends only the `ouros_omen_regent` aspect, points both branches to the derived geometry and adds the transparent accessory layer. The official `cobblemon:absol` poser remains authoritative for presentation. No extra form, battle state or combat rule is introduced.

## Evidence

Real preview evidence is produced by pinned Blockbench 5.1.6, SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

The accepted V2 evidence directory is `test-evidence/visual/cobblemon-skins/0359_absol/omen-regent-real-poses/` and contains the matched official reference, hero three-quarter, cry three-quarter and hero front/left/right/back views plus pose metadata and PNG hashes.

## Authority boundary

Omen Regent is presentation-only. It does not select combatants, interpret Cobblemon battle participants, determine legality, alter HP/status, choose positions, generate tactical RNG, calculate damage or decide outcomes. AutoPTU/Ouros remains the tactical authority.
