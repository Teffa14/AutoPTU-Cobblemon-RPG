package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleGridTransform;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.joml.Vector3f;

import java.util.Objects;

/** Draws a presentation-only tactical grid from a precomputed authoritative visual plan. */
public final class FabricBattleGridVisualRenderer {
    private static final DustParticleEffect GRID = dust(0.10F, 0.48F, 0.95F, 0.42F);
    private static final DustParticleEffect LEGAL_MOVEMENT = dust(0.12F, 0.92F, 0.52F, 0.72F);
    private static final DustParticleEffect LEGAL_ATTACK = dust(0.95F, 0.36F, 0.16F, 0.72F);
    private static final DustParticleEffect SELECTED_MOVEMENT = dust(1.00F, 0.78F, 0.10F, 1.10F);
    private static final DustParticleEffect SELECTED_ATTACK = dust(1.00F, 0.12F, 0.05F, 1.15F);

    private FabricBattleGridVisualRenderer() {}

    public static void render(
            ServerWorld world,
            BattleGridTransform transform,
            BattleChoiceVisualPlan plan,
            boolean committed,
            long animationTick
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(transform, "transform");
        Objects.requireNonNull(plan, "plan");
        if (plan.gridWindow() != null) renderGrid(world, transform, plan.gridWindow());
        plan.shiftDestinations().forEach(anchor -> renderLegalCell(world, transform, anchor, LEGAL_MOVEMENT));
        plan.attackTargets().forEach(anchor -> renderLegalCell(world, transform, anchor, LEGAL_ATTACK));
        if (plan.highlight() != null && plan.highlight().anchor() != null) {
            renderHighlight(world, transform, plan.highlight(), committed, animationTick);
        }
    }

    /** QA/demo hook for a known presentation window. It does not make any cell legal. */
    public static void renderGrid(ServerWorld world, BattleGridTransform transform, BattleChoiceVisualPlan.GridWindow window) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(transform, "transform");
        Objects.requireNonNull(window, "window");
        for (int x = window.minX(); x <= window.maxX(); x++) {
            for (int y = window.minY(); y <= window.maxY(); y++) {
                renderCellOutline(world, transform, new BattleGridCoordinate(x, y), GRID, 1, 0.035D);
            }
        }
    }

    /** QA/demo hook for an already server-declared movement or attack anchor. */
    public static void renderDeclaredAnchor(
            ServerWorld world,
            BattleGridTransform transform,
            BattleGridCoordinate anchor,
            BattleChoiceVisualPlan.HighlightKind kind,
            boolean committed,
            long animationTick
    ) {
        renderHighlight(world, transform,
                new BattleChoiceVisualPlan.Highlight(kind, anchor, "server-declared", kind == BattleChoiceVisualPlan.HighlightKind.ATTACK ? "demo-strike" : null),
                committed, animationTick);
    }

    /** QA/demo hook for a destination emitted by the authoritative movement action space. */
    public static void renderLegalMovementCell(
            ServerWorld world,
            BattleGridTransform transform,
            BattleGridCoordinate anchor
    ) {
        renderLegalCell(world, transform, anchor, LEGAL_MOVEMENT);
    }

    private static void renderLegalCell(
            ServerWorld world,
            BattleGridTransform transform,
            BattleGridCoordinate anchor,
            ParticleEffect particle
    ) {
        renderCellOutline(world, transform, anchor, particle, 2, 0.075D);
        spawn(world, transform, anchor, 0.0D, 0.0D, 0.10D, particle);
    }

    private static void renderHighlight(
            ServerWorld world,
            BattleGridTransform transform,
            BattleChoiceVisualPlan.Highlight highlight,
            boolean committed,
            long animationTick
    ) {
        ParticleEffect particle = highlight.kind() == BattleChoiceVisualPlan.HighlightKind.MOVEMENT
                ? SELECTED_MOVEMENT
                : SELECTED_ATTACK;
        int samples = committed ? 5 : 3;
        double lift = 0.11D + 0.05D * Math.sin(animationTick * 0.45D);
        renderCellOutline(world, transform, highlight.anchor(), particle, samples, lift);

        double pulse = 0.28D + 0.10D * Math.sin(animationTick * 0.35D);
        for (int point = 0; point < 12; point++) {
            double radians = Math.PI * 2.0D * point / 12.0D;
            spawn(world, transform, highlight.anchor(), Math.cos(radians) * pulse, Math.sin(radians) * pulse,
                    lift + (committed ? 0.16D : 0.08D), particle);
        }
        int column = committed ? 6 : 3;
        for (int level = 0; level < column; level++) {
            spawn(world, transform, highlight.anchor(), 0.0D, 0.0D, 0.15D + level * 0.17D, particle);
        }
        if (committed && highlight.kind() == BattleChoiceVisualPlan.HighlightKind.ATTACK) {
            var center = transform.toWorld(highlight.anchor());
            world.spawnParticles(ParticleTypes.FLAME, center.x() + 0.5D, center.y() + 0.45D, center.z() + 0.5D,
                    10, 0.32D, 0.18D, 0.32D, 0.02D);
        }
    }

    private static void renderCellOutline(
            ServerWorld world,
            BattleGridTransform transform,
            BattleGridCoordinate anchor,
            ParticleEffect particle,
            int samplesPerEdge,
            double lift
    ) {
        for (int sample = 0; sample <= samplesPerEdge; sample++) {
            double offset = -0.46D + 0.92D * sample / samplesPerEdge;
            spawn(world, transform, anchor, offset, -0.46D, lift, particle);
            spawn(world, transform, anchor, offset, 0.46D, lift, particle);
            spawn(world, transform, anchor, -0.46D, offset, lift, particle);
            spawn(world, transform, anchor, 0.46D, offset, lift, particle);
        }
    }

    private static void spawn(
            ServerWorld world,
            BattleGridTransform transform,
            BattleGridCoordinate anchor,
            double gridOffsetX,
            double gridOffsetY,
            double lift,
            ParticleEffect particle
    ) {
        var center = transform.toWorld(anchor);
        double x = center.x() + 0.5D + gridOffsetX * transform.gridX().dx() + gridOffsetY * transform.gridY().dx();
        double z = center.z() + 0.5D + gridOffsetX * transform.gridX().dz() + gridOffsetY * transform.gridY().dz();
        world.spawnParticles(particle, x, center.y() + lift, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static DustParticleEffect dust(float red, float green, float blue, float scale) {
        return new DustParticleEffect(new Vector3f(red, green, blue), scale);
    }
}
