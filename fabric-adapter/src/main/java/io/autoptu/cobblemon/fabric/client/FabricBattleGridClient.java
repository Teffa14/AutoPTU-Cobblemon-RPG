package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleGridTransform;
import io.autoptu.cobblemon.fabric.network.FabricBattleGridPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedHashMap;
import java.util.Map;

/** Smooth local rendering of bounded server-authored battle frames, independent of particle settings. */
public final class FabricBattleGridClient implements ClientModInitializer {
    private static final Map<BattleArenaSnapshot, Frame> FRAMES = new LinkedHashMap<>();
    private static long clientTick;

    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleGridPayload.ID, (payload, context) -> {
            if (payload.plan().gridWindow() == null && payload.plan().highlight() == null) {
                FRAMES.remove(payload.arena());
                return;
            }
            if (FRAMES.size() >= 8 && !FRAMES.containsKey(payload.arena())) FRAMES.remove(FRAMES.keySet().iterator().next());
            FRAMES.put(payload.arena(), new Frame(payload, clientTick));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FRAMES.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTick++;
            FRAMES.entrySet().removeIf(entry -> client.world == null || clientTick - entry.getValue().receivedTick() > 30
                    || !client.world.getRegistryKey().getValue().toString().equals(entry.getKey().dimensionId()));
        });
        WorldRenderEvents.AFTER_ENTITIES.register(FabricBattleGridClient::render);
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            var client = MinecraftClient.getInstance();
            if (FRAMES.isEmpty() || client.options.hudHidden || client.currentScreen != null) return;
            Frame frame = FRAMES.values().stream().reduce((a, b) -> b).orElseThrow();
            var selected = frame.payload().plan().highlight();
            String heading = selected == null ? "BATTLEFIELD  /  B: actions"
                    : (frame.payload().committed() ? "CONFIRMED  /  " : "SELECTED  /  ")
                    + (selected.kind() == BattleChoiceVisualPlan.HighlightKind.MOVEMENT ? "Movement" : selected.moveId());
            int x = 12, y = context.getScaledWindowHeight() - 74;
            context.fill(x - 5, y - 5, x + 258, y + 30, 0xCC101A29);
            context.fill(x - 5, y - 5, x - 3, y + 30, 0xFF66D9EB);
            context.drawText(client.textRenderer, heading, x + 3, y + 1, 0xFFF2F7FC, false);
            context.drawText(client.textRenderer, "Green: move   Gold: selected   Red: attack", x + 3, y + 15, 0xFF9DB8C9, false);
        });
    }

    private static void render(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        if (matrices == null || context.consumers() == null || FRAMES.isEmpty()) return;
        Vec3d camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        try {
            for (Frame frame : FRAMES.values()) {
                var payload = frame.payload();
                if (!context.world().getRegistryKey().getValue().toString().equals(payload.arena().dimensionId())) continue;
                var transform = BattleGridTransform.from(payload.arena());
                var plan = payload.plan();
                var window = plan.gridWindow();
                VertexConsumer fills = context.consumers().getBuffer(RenderLayer.getDebugFilledBox());
                if (window != null) {
                    for (int ix = 0; ix < window.width(); ix++) {
                        for (int iy = 0; iy < window.height(); iy++) {
                            var cell = new BattleGridCoordinate(window.minX() + ix, window.minY() + iy);
                            Box tile = cellBox(transform, cell, 0.04, 0.035);
                            float[] color = plan.shiftDestinations().contains(cell) ? new float[]{0.16F, 0.85F, 0.58F}
                                    : plan.attackTargets().contains(cell) ? new float[]{1F, 0.38F, 0.20F} : new float[]{0.17F, 0.40F, 0.60F};
                            filled(matrices, fills, tile, color, plan.shiftDestinations().contains(cell) || plan.attackTargets().contains(cell) ? 0.22F : 0.07F);
                        }
                    }
                }
                var highlight = plan.highlight();
                float[] selectedColor = highlight != null && highlight.kind() == BattleChoiceVisualPlan.HighlightKind.MOVEMENT
                        ? new float[]{1F, 0.78F, 0.15F} : new float[]{1F, 0.22F, 0.18F};
                double time = clientTick + context.tickCounter().getTickDelta(false);
                if (highlight != null && highlight.anchor() != null) {
                    Box cell = cellBox(transform, highlight.anchor(), 0.02, 0.065);
                    float pulse = (float) (0.35 + 0.10 * Math.sin(time * 0.18));
                    filled(matrices, fills, cell, selectedColor, pulse);
                }
                VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
                if (window != null) {
                    // Shared edges are drawn once. Constant-height ribbons keep the grid readable in perspective.
                    for (int ix = 0; ix <= window.width(); ix++) {
                        Vec3d a = point(transform, window.minX() - 0.5 + ix, window.minY() - 0.5, 0.055);
                        Vec3d b = point(transform, window.minX() - 0.5 + ix, window.maxY() + 0.5, 0.055);
                        line(matrices, lines, a, b, 0.34F, 0.80F, 0.91F, 0.65F);
                    }
                    for (int iy = 0; iy <= window.height(); iy++) {
                        Vec3d a = point(transform, window.minX() - 0.5, window.minY() - 0.5 + iy, 0.055);
                        Vec3d b = point(transform, window.maxX() + 0.5, window.minY() - 0.5 + iy, 0.055);
                        line(matrices, lines, a, b, 0.34F, 0.80F, 0.91F, 0.65F);
                    }
                }
                if (highlight != null && highlight.anchor() != null) {
                    Box tile = cellBox(transform, highlight.anchor(), 0.01, 0.10);
                    WorldRenderer.drawBox(matrices, lines, tile, selectedColor[0], selectedColor[1], selectedColor[2], 1F);
                    Vec3d center = point(transform, highlight.anchor().x(), highlight.anchor().y(), 0.15);
                    double radius = 0.30 + 0.035 * Math.sin(time * 0.18);
                    for (int i = 0; i < 32; i++) {
                        double a = i * Math.PI / 16, b = (i + 1) * Math.PI / 16;
                        line(matrices, lines, center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                                center.add(Math.cos(b) * radius, 0, Math.sin(b) * radius), selectedColor[0], selectedColor[1], selectedColor[2], 1F);
                    }
                    if (payload.actorOrigin() != null) {
                        Vec3d source = point(transform, payload.actorOrigin().x(), payload.actorOrigin().y(), 0.16);
                        Vec3d delta = center.subtract(source);
                        // Dashed intent connector, not a pathfinding or affected-area claim.
                        for (int dash = 0; dash < 12; dash++) {
                            line(matrices, lines, source.add(delta.multiply(dash / 12.0)), source.add(delta.multiply((dash + 0.58) / 12.0)),
                                    selectedColor[0], selectedColor[1], selectedColor[2], 0.85F);
                        }
                    }
                }
            }
        } finally { matrices.pop(); }
    }

    private static Box cellBox(BattleGridTransform transform, BattleGridCoordinate cell, double inset, double lift) {
        var world = transform.toWorld(cell);
        return new Box(world.x() + inset, world.y() + lift, world.z() + inset,
                world.x() + 1 - inset, world.y() + lift + 0.015, world.z() + 1 - inset);
    }

    private static Vec3d point(BattleGridTransform transform, double x, double y, double lift) {
        return new Vec3d(transform.origin().x() + 0.5 + x * transform.gridX().dx() + y * transform.gridY().dx(),
                transform.origin().y() + lift,
                transform.origin().z() + 0.5 + x * transform.gridX().dz() + y * transform.gridY().dz());
    }

    private static void filled(MatrixStack matrices, VertexConsumer consumer, Box box, float[] color, float alpha) {
        WorldRenderer.renderFilledBox(matrices, consumer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                color[0], color[1], color[2], alpha);
    }

    private static void line(MatrixStack matrices, VertexConsumer consumer, Vec3d a, Vec3d b, float r, float g, float blue, float alpha) {
        Vec3d normal = b.subtract(a).normalize();
        var entry = matrices.peek();
        consumer.vertex(entry.getPositionMatrix(), (float) a.x, (float) a.y, (float) a.z).color(r, g, blue, alpha)
                .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.vertex(entry.getPositionMatrix(), (float) b.x, (float) b.y, (float) b.z).color(r, g, blue, alpha)
                .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private record Frame(FabricBattleGridPayload payload, long receivedTick) {}
}
