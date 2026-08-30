# Cobblemon Skin Art Direction — Full Transformation Standard

This document is the visual acceptance contract for Ouros-authored Cobblemon skins. It applies to every new skin and to every legacy skin that is re-audited after issue #308.

## Scope and authority boundary

This workflow is presentation-only. Cobblemon/Minecraft may provide the official Pokemon model, textures, animations, render hooks, networking and presentation systems. They do not decide combatants, legality, HP/status, positions, tactical RNG, damage or battle results. AutoPTU/Ouros remains authoritative for all tactical battle facts.

## Official source or reject

For a Pokemon that exists in Cobblemon, the starting point is the exact model shipped in the latest stable Cobblemon release compatible with the repository Minecraft target.

Current target at the time this standard was adopted:

- Minecraft Java Edition 1.21.1
- Cobblemon 1.7.3 Fabric
- Modrinth version id `kF7CvxTo`
- official file `Cobblemon-fabric-1.7.3+1.21.1.jar`

CI must pin the release id, filename and cryptographic hash of the JAR. Each skin must pin the official model, texture, animation, poser and resolver assets it actually uses. Mirrors, forks, copied repositories, screenshots and manual anatomy reconstructions are not accepted as geometry sources.

Before a new slice starts, verify the current repository target and verify whether Modrinth has a newer stable Cobblemon release compatible with that target. If the official source has advanced, migrate the source before adding new cosmetic work.

A reference render from the current official model is not sufficient. The production edited model itself must be generated from that same extracted official `.geo.json`. Builders must not silently use a repository-stored legacy body as their editable base.

## Anatomy preservation

Original anatomy stays intact underneath the skin.

All original bones must remain JSON-equivalent and in the same order. This includes names, parents, pivots, rotations, cubes, locators, UV data and animation-facing hierarchy. The geometry identifier may change. Additional cosmetic groups must use `ouros_*` names.

Do not rebuild or reinterpret the head, muzzle, eyes, ears, neck, torso, arms, legs, paws, wings, tail or other biological anatomy. A local modification to an original bone is rejected by default and requires a documented exception with explicit visual justification.

If Cobblemon ships separate male, female or form models, derive each Ouros variant from the corresponding official file independently. Never clone one official sex/form model into another.

## Epic or rejected

Technical validity is necessary but is not artistic acceptance.

A skin fails when the first read is still "base Pokemon plus small cuboids". Passing CI, preserving bones and loading in Blockbench do not make that result acceptable.

The intended read is a complete visual transformation while the official Pokemon remains anatomically intact underneath. At normal gameplay distance the player should immediately understand a strong fantasy, class, culture, role or material identity without reading the skin name.

The required design hierarchy is:

1. one strong three-quarter silhouette;
2. one immediately legible fantasy;
3. one to three dominant signature pieces;
4. large connected masses before micro-detail;
5. coherent palette and material breakup across the whole character;
6. deliberate layering and depth;
7. meaningful asymmetry where it strengthens the concept;
8. front, side and rear readability;
9. motion-safe attachment through official animations;
10. secondary hardware and small detail only after the large read works.

Large external silhouette changes are encouraged when coherent. Armor, mantles, cowls, collars, coats, pauldrons, packs, banners, fins, conductor systems, coils, field equipment, ornaments and similar attached forms may extend well beyond the original outline. The species must remain unmistakable and the biological model must remain intact underneath.

The goal is not to maximize the number of accessory cubes. Solve the whole character. Large masses must form an intentional costume/equipment architecture, and smaller details only support that architecture.

## Official biological texture preservation

The exact official Cobblemon normal/shiny biological texture is an immutable baseline for Ouros skins unless a future runtime-specific mechanism is explicitly documented and approved as a separate contract.

For the current pipeline:

- do not repaint occupied biological body texels;
- do not remap original UVs;
- keep the production body texture byte-identical to the exact official source texture extracted from the pinned JAR;
- record the official SHA-256 and prove the production body texture has the same SHA-256;
- use validated transparent/free texels or a separate compatible accessory overlay/atlas region for added geometry;
- preserve official transparency, sex/form behavior and resolver semantics;
- do not copy third-party skins, texture motifs, logos or protected distinctive designs.

Recoloring the biological body is not a substitute for design. The transformation must come from connected geometry, layering, silhouette, material separation on the added equipment and deliberate composition.

Required texture metadata for new and re-audited skins:

- `officialTextureBaselineSha256`
- production texture SHA-256;
- `bodyTexelRework: NONE` under this contract;
- `paletteIntent`;
- `materialIntent`;
- accessory overlay path and validated UV reservation when one is used.

## Physical attachment — no floating pieces

A valid bone parent is necessary but does not prove a cosmetic object is physically or visually attached.

Every large cosmetic system must have a deliberate root/contact mass that joins it to the Pokemon or to another already-attached cosmetic mass. A halo, banner, fin, coat panel, pack, mantle, shoulder system or ornament that visibly hovers near the body fails even when its parent is technically valid.

Automated attachment gates must reject missing parents, cycles, cosmetic chains that do not terminate in an official bone, detached groups and isolated cubes. The structural gate is only a first pass. Because pivots, rotations and animation can expose detachment that bind-pose AABBs cannot detect, Blockbench review must also inspect contact in official idle, battle and locomotion states where those states exist.

Do not weaken an attachment threshold merely to make an existing asset pass. Correct the geometry/root instead.

## Composition reference rule

External reference images may be used only to set the level of ambition and to study broad composition principles such as complete-costume coverage, connected masses, material hierarchy, palette coherence and gameplay-scale readability.

Do not copy geometry, UVs, texture layouts, logos, motifs, costume patterns or distinctive designs from Pokemon Unite, fan mods, servers, skin packs or other third parties. Ouros concepts must remain original.

## Blockbench review contract

Blockbench is the primary independent viewer. Do not use the deprecated project Python renderer as artistic evidence and do not create a replacement homemade renderer.

The exact production `.geo.json`, exact official baseline body texture, exact accessory overlay and exact official animation file must be loaded through the Blockbench Bedrock workflow.

Every accepted review must include an untouched official reference and the Ouros skin using the same species, camera, projection, scale, pose and animation frame. Auto-fit each model independently is not sufficient evidence because it can hide silhouette scale differences.

Minimum evidence when the official species provides equivalent animations:

- `official_reference_three_quarter`
- `hero_three_quarter`
- `battle_ready_three_quarter`
- `walking_three_quarter`
- structural front, left, right and back views when useful.

If the official species does not ship an equivalent battle/walking clip, record that fact and do not fabricate a pose.

Every work pass must expose four clickable current PNGs. Prefer the four views above. If an official state does not exist, substitute an actual current structural/gameplay-scale Blockbench view and record why.

Review metadata must record Cobblemon version, JAR provenance, official model/texture/animation hashes, production/overlay texture hashes, Blockbench version/hash, original/derived bone counts, animation name, frame/time and PNG hashes.

## Gameplay-scale gate

Every artistic review must include a gameplay-scale readability check. Produce a thumbnail from the real Blockbench render with the Pokemon approximately 128–192 px tall while preserving the same view.

Reject the skin if the large concept disappears at that scale, if signature pieces collapse into pixel noise, or if the result again reads primarily as the untouched base Pokemon.

A reviewer must be able to answer yes to all of these:

- Does the first glance change materially from the official Pokemon?
- Is there a signature silhouette?
- Are the dominant pieces recognizable as deliberate objects rather than scattered cuboids?
- Does the material treatment unite the body and equipment without repainting biological texels?
- Does the skin look premium from three-quarter view?
- Is the fantasy understandable without the skin name?
- Is the Pokemon still clearly the original species?
- Are serious clipping, detachment and motion failures absent in tested official animations?

If any answer is no, iterate before PR even if automated checks are green.

## Legacy re-audit

The existing Storm Courier, Aura Sentinel, Rift Warden, Eclipse Herald, Solar Legion, Shadow Tide, Omen Regent and Abyssal Bastion passes remain useful technical baselines for provenance and CI infrastructure. Their previous "EPIC ACCEPTED" labels do not automatically satisfy this standard.

Every legacy skin must be re-audited from its exact current official model, with immutable original bones/body texture, connected large forms, no floating pieces and gameplay-scale readability. A technically valid legacy skin may remain in the repository while its art status is `RE-AUDIT REQUIRED`.

This re-audit applies equally to legacy Absol, Tyranitar, Charizard and other previously authored species; an old edited model cannot be accepted merely because a new official reference image looks correct.

## PR and CI acceptance

A skin PR may be marked ready only after artistic review accepts the real Blockbench evidence from the exact generated PR head. Automated checks must still validate the official JAR/source hashes, exact original-bone preservation, sex/forms, exact official body-texture preservation, resolver/poser/animation paths, added `ouros_*` bones, cosmetic attachment/overlay UV reservations, Playable Test Build and Integration Core CI.

If a bot regenerates production assets, the subsequent evidence and normal CI must run against that regenerated head. Create a later human-authored head when GitHub's security model marks bot-triggered PR workflows `action_required`.

Do not merge with red or unexecuted required checks. Do not claim emissive, particles, material hooks or runtime behavior unless the repository contains real, syntax-valid and runtime-validated support for them.
