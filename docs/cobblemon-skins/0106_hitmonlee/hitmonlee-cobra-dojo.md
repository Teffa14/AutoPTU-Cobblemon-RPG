# 0106 Hitmonlee — Cobra Dojo Striker

Status: `ARTISTIC FAIL — V8 REWORK REQUIRED`.

The previously merged v7 technical pass remains useful as provenance and anatomy-preservation evidence, but the real Blockbench renders were re-audited and rejected artistically. The current production presentation still reads too strongly as base Hitmonlee plus many attached accessories. CI green does not override this rejection.

## Re-audit finding

The front render adds density, but the large-form read is weak. The side render exposes insufficient three-dimensional transformation. The rear loses most of the concept. The headband, shoulder steps, wraps, rails, belt details and small crest add noise without creating one dominant premium silhouette.

The v8 objective is a true full transformation while keeping all official biological bones intact and in order.

## V8 art direction

The next accepted candidate must be driven by large connected masses before secondary detail:

- a substantial martial cowl/hood or cobra-frame that changes the head-and-shoulder outline without replacing the official head;
- a coherent sleeveless war-gi / mantle shell with real side and rear volume, not flat panels;
- larger asymmetric shoulder architecture with one clear signature side;
- broad split coat tails / war-skirt masses that remain attached through official animation;
- integrated strike armor on the telescoping legs, grouped as readable guards instead of repeated thin bands;
- a dominant original cobra motif readable from both front and rear at gameplay scale;
- material breakup that unifies the entire costume rather than isolated gold accents;
- fewer incidental micro-elements where they compete with the main silhouette.

The rejection condition is explicit: if the next review still reads as `Hitmonlee + headband + wraps + shoulder bits`, it fails even if every validator and workflow is green.

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
- model-local license: CC BY-NC 3.0 as distributed beside the official Hitmonlee model in the pinned JAR.

## Anatomy contract

The official Hitmonlee model contains 30 bones. Every future candidate must preserve all 30 JSON-equivalent and in original order. Geometry identifier changes remain allowed. Cosmetic additions must use `ouros_*` bones and parent to official animated bones. No alternate biological rig is allowed.

## Texture contract

Normal and shiny remain independent derivations from their exact official source textures. Original model UV coordinates may not be remapped. V8 may use a broader derived texture treatment if needed for a coherent premium material system, but the official baseline hashes and all intentional body-texel changes must be documented.

## Blockbench acceptance gate

Blockbench 5.1.6 remains the independent primary review tool. V8 cannot be called complete until matched-camera evidence exists for official reference, hero three-quarter, battle-ready three-quarter, walking three-quarter, front, left, right, back and gameplay-scale views.

The artistic review must explicitly assess first-read silhouette, signature pieces, side/rear coherence, gameplay-scale readability, clipping and whether the species remains clearly Hitmonlee.

## Forms

Cobblemon 1.7.3 does not distribute separate male and female Hitmonlee geometry. Normal and shiny use the same official geometry with independent official-source texture derivations.

## Authority boundary

This work is presentation-only. Cobblemon and Minecraft provide model, texture, animation and renderer presentation hooks. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, tactical positions, RNG, damage and tactical outcomes.
