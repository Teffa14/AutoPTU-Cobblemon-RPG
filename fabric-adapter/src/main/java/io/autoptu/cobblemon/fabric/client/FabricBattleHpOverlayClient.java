package io.autoptu.cobblemon.fabric.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.network.FabricBattleHpPresentationPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-owned visual overlay for server-authoritative PTU current HP. */
public final class FabricBattleHpOverlayClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-battle-hp-overlay");
    private static final Map<UUID, Integer> CURRENT_HP = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                FabricBattleHpPresentationPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    CURRENT_HP.put(payload.entityUuid(), payload.currentHp());
                    LOGGER.info("AutoPTU authoritative HP received entity={} hp={}", payload.entityUuid(), payload.currentHp());
                }));

        WorldRenderEvents.AFTER_ENTITIES.register(context -> render(context.matrixStack(), context.camera()));
    }

    private static void render(MatrixStack matrices, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.textRenderer == null) return;

        for (var entry : CURRENT_HP.entrySet()) {
            Entity entity = findEntity(client, entry.getKey());
            if (!(entity instanceof PokemonEntity pokemon) || entity.isRemoved()) continue;

            double dx = pokemon.getX() - camera.getPos().x;
            double dy = pokemon.getY() + pokemon.getHeight() + 0.55D - camera.getPos().y;
            double dz = pokemon.getZ() - camera.getPos().z;
            if (dx * dx + dy * dy + dz * dz > 4096.0D) continue;

            String label = "PTU HP " + entry.getValue();
            matrices.push();
            matrices.translate(dx, dy, dz);
            matrices.multiply(camera.getRotation());
            matrices.scale(0.025F, -0.025F, 0.025F);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float x = -client.textRenderer.getWidth(label) / 2.0F;
            client.textRenderer.draw(
                    Text.literal(label),
                    x,
                    0.0F,
                    0xFFFFFFFF,
                    false,
                    matrix,
                    contextVertexConsumers(),
                    net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,
                    0x66000000,
                    0x00F000F0);
            matrices.pop();

            LOGGER.debug("AutoPTU rendered authoritative HP entity={} hp={}", entry.getKey(), entry.getValue());
        }
    }

    private static net.minecraft.client.render.VertexConsumerProvider.Immediate contextVertexConsumers() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    private static Entity findEntity(MinecraftClient client, UUID uuid) {
        for (Entity entity : client.world.getEntities()) {
            if (entity.getUuid().equals(uuid)) return entity;
        }
        return null;
    }
}
