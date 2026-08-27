# Trainer capability -> Minecraft world task resolution

## Purpose

Minecraft world tasks must react to the canonical Trainer that the authenticated player has built. Cooking, technical fabrication, medicine, Pokemon research, occult investigation, survival work, repairs and similar RPG interactions must not collapse into identical outcomes for every character.

This layer is Ouros world simulation. It may consume canonical Trainer identity and capability values that already exist in persistent RPG state. It does not redefine PTU skills, Trainer Features, battle legality, battle RNG, damage, statuses, action economy or other AutoPTU-Java authority.

## Authority boundary

The server owns the complete task definition, the authenticated Trainer lookup, capability lookup, eligibility, probability distribution, attempt identity, material reservation, RNG seed/result, quality result and output commit.

Clients may request a recipe/task ID and quantity. Clients may never supply trusted skill ranks, classes, Features, modifiers, probability, quality, material truth, eligibility or outcomes.

A world task definition references canonical capability IDs. The task resolver reads those values from `CanonicalPlayerState`. It must never infer a missing PTU capability from Minecraft XP, vanilla statistics, Cobblemon Pokemon state or client data.

If AutoPTU-Java later publishes an authoritative PTU contract for a task, that task must delegate to the upstream contract rather than retaining a competing Minecraft calculation.

## Graded outcomes

Crafting should normally protect experimentation while preserving specialist value. Safe recipes use graded quality rather than a binary success/failure wall. A novice can often produce an improvised or inefficient result; a trained specialist shifts probability toward standard and excellent results.

The first generic quality vocabulary is:

- `IMPROVISED`: usable but intentionally less efficient than the intended product.
- `STANDARD`: the intended normal product.
- `EXCELLENT`: a superior result when the recipe explicitly defines a superior output/effect.

A recipe may define a hard knowledge requirement only when the task is conceptually impossible without that knowledge. Hard gates must be explicit authored content, not an implicit penalty for being a non-specialist.

Probability curves belong to the Ouros task/recipe definition. They are not presented as PTU skill-check formulas. The resolver chooses the applicable authored curve from the Trainer's canonical capability rank.

## Anti-farming and anti-reroll rules

`canCraft` is informational. It may show requirements and probability bands, but it never pre-rolls or reveals the next committed outcome.

A real crafting attempt must create a server-owned durable attempt ID before outcome resolution. Ingredient reservation, outcome resolution, consumption and output delivery must be one idempotent workflow. Repeating the same request, disconnecting, reconnecting or restarting the server must not create a new roll, duplicate output or restore already-committed materials.

When the atomic attempt/commit slice ships, the durable attempt record must contain enough information to resume or reconcile an interrupted craft without rerolling it.

## First playable slice

The first slice exposes `/autoptu cancraft <recipe>` through a reusable server service. It reads the authenticated player's persistent canonical Trainer, resolves the relevant canonical skill rank, and reports whether the task is understood plus the current improvised/standard/excellent distribution.

Initial recipes are server-authored examples for Survival, Technology Education and Occult Education. They do not consume materials yet. `/autoptu craft` remains blocked on the durable ingredient/output transaction so the project does not ship a rerollable or duplication-prone implementation.

The same service is intended for the future crafting workstation/menu. Slash commands remain fallback/bootstrap surfaces.
