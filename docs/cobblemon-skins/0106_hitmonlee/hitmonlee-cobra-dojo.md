# 0106 Hitmonlee — Cobra Dojo Striker

Status: USER REJECTED — REWORK REQUIRED
Sale eligibility: NOT ELIGIBLE.

This document is retained only as technical/provenance history. Any historical acceptance language below is superseded: the owner rejected the current art, no professional manifest certifies it, and its production assets are locked until the registry gates are satisfied.


Cobra Dojo Striker is an original Ouros presentation-only Hitmonlee variant built from the exact official Cobblemon 1.7.3 Hitmonlee presentation source. Its visual direction is an elite black-and-gold martial-arts kick specialist. It does not include third-party logos, wordmarks, text, or copied costume insignia.

## Visual system

Three large reads define the variant at gameplay distance:

- a high cobra-hood cowl around the official eye and shoulder plane;
- a connected sleeveless gi/champion shell with broad shoulder mass, lapels, wide belt and split sash;
- an articulated strike-guard system covering each stage of Hitmonlee's telescoping leg chain and both feet.

The left foot includes an asymmetric extended fang rail. Forearm guards and controlled gold edging support the primary masses without becoming the main read.

The reviewed rear view is intentionally broad but simpler than the front and three-quarter treatment. Future refinement may articulate the rear cowl more strongly, but the v1 matched-camera review still passes the repository's current full-transformation floor.

## Exact official baseline

- Minecraft: `1.21.1`
- Cobblemon: `1.7.3` Fabric
- Modrinth version id: `kF7CvxTo`
- official JAR SHA-256: `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- official model: `assets/cobblemon/bedrock/pokemon/models/0106_hitmonlee/hitmonlee.geo.json`
- official model SHA-256: `f92b067c781f9210dec305860bdb09141efad5c238396ac7988c4bbbed82840d`
- official normal texture SHA-256: `cbf05271a594e788e7173fd3e34ee32b35488781dd97b7e100455fe57d919574`
- official shiny texture SHA-256: `a677824d9be92b46ff6359ce58118e67fce1078632908cfc0ac80816e7c1c065`
- official animation SHA-256: `d9f2b9e3c01ace9773368584fd034b015276a085d6ccb8d5624967bb7d9e8b7c`
- official resolver SHA-256: `67b3432a004786b43f523b367a8f20dceba7353f4949dd3472ffcb8aa9036319`
- model-local license file SHA-256: `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`
- model-local license: CC BY-NC 3.0, as distributed beside the official Hitmonlee model in the pinned JAR.

The source-inspection and review workflows verify that Cobblemon 1.7.3 is still the latest stable Fabric release compatible with Minecraft 1.21.1 before accepting the pinned source.

## Anatomy contract

The official Hitmonlee model contains 30 bones. All 30 remain JSON-equivalent and in the same order in the production derivative. The only allowed original-model metadata change is the geometry identifier.

The derivative appends 13 `ouros_*` cosmetic bones and 94 cosmetic cubes, for 43 total bones. There is no replacement anatomy rig. Every cosmetic group is parented to an official animated Hitmonlee bone.

The leg guards are intentionally split across `leg_left2`, `leg_left3`, `leg_left4`, `leg_right2`, `leg_right3`, `leg_right4`, and the two official foot bones. This preserves the official telescoping-leg motion rather than treating the leg equipment as one static shell.

## Texture contract

Normal and shiny are derived independently from the corresponding official 64x64 textures. The official UV layout and original model UV coordinates are unchanged.

The normal derivative SHA-256 is `64d2ec2a641e57862b12259bc18d75dff9053cefdaaae1981f9d27b7ac2f6c99`.

The shiny derivative SHA-256 is `b86cc4ec9abeda1edd4deed190fd8e187059c25d14c2a7608ca32fa51ff70ee9`.

Ten original transparent texels at row 63, verified unused by the official geometry, are reserved as material swatches for the appended cosmetic geometry. The normal and shiny metadata files record this alpha-footprint change explicitly.

## Blockbench acceptance

The historical evidence came from Blockbench 5.1.6 AppImage SHA-256 `c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e`.

Workflow run `33289138725` loaded the exact production model and asserted 43 Blockbench bones before capturing the candidate. The official reference asserted 30 bones. Official reference and candidate use the same Blockbench camera profile.

Official animation clips reviewed:

- `animation.hitmonlee.ground_idle` at 0.35 s for reference and hero;
- `animation.hitmonlee.battle_idle` at 0.35 s;
- `animation.hitmonlee.ground_walk` at 0.25 s.

The evidence includes official/hero/battle/walk three-quarter views, front/left/right/back structural views, shiny three-quarter, and 160 px gameplay-scale captures. Exact PNG hashes and the visual QA decision are recorded in `cobra-dojo-v1-art-review.json`.

## Forms

Cobblemon 1.7.3 does not distribute separate male and female Hitmonlee geometry. Normal and shiny use the same exact official geometry and independent official-source texture derivations.

## Authority boundary

This work is presentation-only. Cobblemon and Minecraft provide model, texture, animation and renderer presentation hooks. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and tactical outcomes.
