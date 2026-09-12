# Battle-shape visual evidence

The operator-only `shapeviz` lab produces real Minecraft/Cobblemon client
evidence for four presentation grammars: Ranged, AoE, Blast, and Line. It does
not resolve or infer PTU geometry, targets, legality, hit state, damage, or
outcomes. Every scene is an explicit `DEV_ONLY / QA VISUAL ONLY` fixture.

The capture client relocates the fixture away from normal spawn content, orients
the attacker and targets toward one another, uses an elevated diagonal camera,
and hides the normal first-person HUD. Nine frames are retained for each shape:

- Ranged shows a narrow moving projectile and a localized impact;
- AoE shows a circular ground boundary, inner ring, spokes, and pulse;
- Blast shows widening side rails, cross-sections, and a moving front;
- Line shows a constant-width corridor, parallel rails, and crossbars.

The Battle Visual Evidence workflow uploads all 36 source frames plus a
four-frame overview and three-frame motion strip for each shape. Reviewers must
inspect the overview for at-a-glance silhouette separation and the strips for
motion continuity. A green compile or screenshot-count gate is not visual
approval.

Cobblemon ActionEffects remain the audiovisual layer where available. The
project-owned particle geometry is presentation scaffolding around authored QA
fixtures and must never be promoted into battle authority.
