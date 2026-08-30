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

The production builder must derive from the official model extracted from the pinned JAR for that run. A repository copy, legacy generated model or earlier Ouros skin may be used only as a comparison artifact, never as the biological source geometry.

## Anatomy preservation

Original anatomy stays intact underneath the skin.

All original bones must remain JSON-equivalent and in the same order. This includes names, parents, pivots, rotations, cubes, locators, UV data and animation-facing hierarchy. The geometry identifier may change. Additional cosmetic groups must use `ouros_*` names.

Do not rebuild or reinterpret the head, muzzle, eyes, ears, neck, torso, arms, legs, paws, wings, tail or other biological anatomy. A local modification to an original bone is rejected by default and requires a documented exception with explicit visual justification.

If Cobblemon ships separate male, female or form models, derive each Ouros variant from the corresponding official file independently. Never clone one official sex/form model into another.

## Artistic acceptance: full transformation or reject

Technical validity is the floor. It is never the artistic goal.

A skin fails when the first read is still "base Pokemon plus accessories", even when the accessories are numerous, technically correct or visually clean. A headband, visor, straps, small backpack, repeated guards, scattered cuboids or a handful of decorative plates do not become a premium transformation merely because more pieces were added.

The work must solve the whole character as a piece of visual design. At normal gameplay distance the player should immediately understand a strong fantasy, class, culture, role or material identity without reading the skin name. The design must feel intentional from the head through the torso, limbs and rear silhouette. Large forms must connect into a coherent costume or equipment language instead of reading as separate props attached to an unchanged Pokemon.

The required design hierarchy is:

1. one strong three-quarter silhouette;
2. one immediately legible fantasy;
3. one to three dominant signature pieces;
4. large connected masses before micro-detail;
5. coherent material breakup across the whole character;
6. deliberate layering and depth;
7. meaningful asymmetry where it strengthens the concept;
8. front, side and rear readability;
9. motion-safe attachment through official animations;
10. secondary hardware and small detail only after the large read works.

Large external silhouette changes are encouraged when coherent. Armor, mantles, cowls, collars, coats, pauldrons, packs, banners, fins, conductor systems, coils, field equipment, ornaments and similar attached forms may extend well beyond the original outline. The species must remain unmistakable and the biological model must remain intact underneath.

Do not solve a weak transformation by adding more small parts. If the concept is weak at gameplay scale, rebuild the major masses, silhouette and hierarchy first.

## Base texture and UV preservation

The official biological texture is immutable unless a future project-wide runtime mechanism explicitly defines and validates a different presentation path.

For the current skin pipeline:

- keep the exact official texture dimensions and UV layout;
- preserve all occupied biological body texels from the pinned official texture;
- do not repaint, recolor or rematerialize the Pokemon body as a shortcut for visual transformation;
- preserve transparency semantics required by the official model and resolver;
- preserve sex/form-specific texture behavior when official assets differ;
- use verified free texels or a validated official-compatible overlay mechanism for Ouros-authored cosmetic materials;
- document overlay paths, UV reservations and palette/material intent;
- reject any cosmetic UV that overlaps occupied official biological texels unless a separately documented runtime-safe mechanism explicitly permits it.

Recoloring the biological body is not a substitute for modeling. The transformation must come primarily from geometry, layering, silhouette, material breakup and composition.

## Cosmetic attachment and no-floating-parts rule

Every cosmetic element must be visually and structurally attached to the official animated model.

A valid parent name alone is not sufficient. A cosmetic part fails when it appears to float, hover, detach, trail incorrectly, intersect implausibly or lose its intended physical connection in any reviewed official animation state.

Each `ouros_*` bone must either parent directly to an official animated bone or reach one through an intentional cosmetic hierarchy. Pivots and offsets must be designed around the actual official parent transform. Orphan bones, missing parents, cycles and detached cosmetic chains are rejected automatically where possible.

Blockbench review must test attachment in at least the official idle, battle and locomotion states that exist for the species. Reviewers must inspect front, side, rear and three-quarter views because floating parts are often hidden from a single camera.

Pieces that are meant to touch the body must maintain believable contact through motion. Pieces that intentionally stand away from the body, such as banners, fins, antennae or field hardware, still require a visibly understandable mounting point, support structure or articulated connection.

## Composition reference rule

External reference images may be used only to set the level of ambition and to study broad composition principles such as complete-costume coverage, connected masses, material hierarchy, palette coherence and gameplay-scale readability.

Do not copy geometry, UVs, texture layouts, logos, motifs, costume patterns or distinctive designs from Pokemon Unite, fan mods, servers, skin packs or other third parties. Ouros concepts must remain original.

## Blockbench review contract

Blockbench is the primary independent viewer. Do not use the deprecated project Python renderer as artistic evidence and do not create a replacement homemade renderer.

The exact production `.geo.json`, exact official baseline texture, exact cosmetic overlay/material assets and exact official animation file must be loaded through the Blockbench Bedrock workflow.

Every accepted review must include an untouched official reference and the Ouros skin using the same species, camera, projection, scale, pose and animation frame. Auto-fit each model independently is not sufficient evidence because it can hide silhouette scale differences.

Minimum evidence when the official species provides equivalent animations:

- `official_reference_three_quarter`
- `hero_three_quarter`
- `battle_ready_three_quarter`
- `walking_three_quarter`
- structural front, left, right and back views when useful

If the official species does not ship an equivalent battle/walking clip, record that fact and do not fabricate a pose.

Review metadata must record Cobblemon version, JAR provenance, official model/texture/animation hashes, cosmetic overlay hashes, Blockbench version/hash, original/derived bone counts, animation name, frame/time and PNG hashes.

## Gameplay-scale gate

Every artistic review must include a gameplay-scale readability check. Produce a thumbnail from the real Blockbench render with the Pokemon approximately 128–192 px tall while preserving the same view.

Reject the skin if the large concept disappears at that scale, if signature pieces collapse into pixel noise, or if the result again reads primarily as the untouched base Pokemon.

A reviewer must be able to answer yes to all of these:

- Does the first glance change materially from the official Pokemon?
- Is there a signature silhouette?
- Are the dominant pieces recognizable as deliberate connected objects rather than scattered cuboids?
- Does the material treatment unite the character and equipment without repainting the biological body?
- Does the skin look premium from three-quarter view?
- Is the fantasy understandable without the skin name?
- Is the Pokemon still clearly the original species?
- Are serious clipping, floating, detachment and motion failures absent in tested official animations?
- Does the rear view still communicate the same design quality as the front?
- Would removing the micro-detail still leave a strong transformation?

If any answer is no, iterate before PR even if automated checks are green.

## Legacy re-audit

The existing Storm Courier, Aura Sentinel, Rift Warden, Eclipse Herald, Solar Legion, Shadow Tide, Omen Regent and Abyssal Bastion passes remain useful technical baselines for provenance and infrastructure only. Their previous artistic labels do not satisfy this standard automatically.

They must be re-audited against the exact current official model, connected large forms, gameplay-scale readability, no-floating-parts motion review and the current base-texture preservation contract. A technically valid legacy skin may remain in the repository while its art status is `RE-AUDIT REQUIRED`.

Storm Courier is a reference overhaul because the old direction clearly exposes the failure mode. Its next accepted version must stop reading as "Pikachu with goggles, straps and a backpack" and instead read as a complete storm-runner/courier transformation. The official male/female Pikachu models remain untouched underneath.

## PR and CI acceptance

A skin PR may be marked ready only after artistic review accepts the real Blockbench evidence. Automated checks must still validate the official JAR/source hashes, exact original-bone preservation, sex/forms, texture dimensions and provenance, resolver/poser/animation paths, added `ouros_*` bones, overlay UV reservations when present, cosmetic parent chains, Playable Test Build and Integration Core CI.

Do not merge with red checks. Do not claim emissive, particles, material hooks or runtime behavior unless the repository contains real, syntax-valid and runtime-validated support for them.
