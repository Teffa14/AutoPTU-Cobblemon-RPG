package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalItemInstance;
import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;
import java.util.UUID;

/** Reusable server-authoritative canonical item grant boundary for operator tools and future UI. */
public final class FabricItemGrantService {
    public static final int MAX_GRANT_QUANTITY = 9999;

    public enum Outcome {
        GRANTED,
        INVALID_ITEM,
        INVALID_QUANTITY,
        MISSING_TRAINER,
        CONFLICT,
        REJECTED
    }

    public record Result(Outcome outcome, String playerId, String templateId, int quantity, String itemInstanceId, String detail) {
        public Result {
            Objects.requireNonNull(outcome, "outcome");
            playerId = playerId == null ? "" : playerId;
            templateId = templateId == null ? "" : templateId;
            itemInstanceId = itemInstanceId == null ? "" : itemInstanceId;
            detail = detail == null ? "" : detail;
        }
    }

    public Result grant(ServerPlayerEntity target, String requestedTemplateId, int quantity) {
        Objects.requireNonNull(target, "target");
        MinecraftServer server = Objects.requireNonNull(target.getServer(), "target server");
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        String templateId = normalize(requestedTemplateId);

        if (quantity <= 0 || quantity > MAX_GRANT_QUANTITY) {
            return new Result(Outcome.INVALID_QUANTITY, playerId, templateId, quantity, "", "quantity outside server grant bounds");
        }
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(server).findPlayer(playerId).isEmpty()) {
            return new Result(Outcome.MISSING_TRAINER, playerId, templateId, quantity, "", "canonical Trainer state does not exist");
        }
        if (!isAuthoredTemplate(templateId)) {
            return new Result(Outcome.INVALID_ITEM, playerId, templateId, quantity, "", "item template is not server-authored");
        }

        String itemInstanceId = "admin-grant:" + UUID.randomUUID();
        CanonicalItemInstance item = new CanonicalItemInstance(itemInstanceId, playerId, templateId, quantity, 0L);
        try {
            if (!FabricCanonicalPlayerStoreRuntime.requireAssetRepository(server).createItemIfAbsent(item)) {
                return new Result(Outcome.CONFLICT, playerId, templateId, quantity, itemInstanceId, "canonical item identity collision");
            }
            return new Result(Outcome.GRANTED, playerId, templateId, quantity, itemInstanceId, "persistent canonical inventory committed");
        } catch (RuntimeException failed) {
            return new Result(Outcome.REJECTED, playerId, templateId, quantity, itemInstanceId, failed.getClass().getSimpleName());
        }
    }

    private static boolean isAuthoredTemplate(String templateId) {
        if (templateId.isBlank()) return false;
        for (String shopId : CanonicalShopCatalogue.DEFAULT.shopIds()) {
            if (CanonicalShopCatalogue.DEFAULT.offers(shopId).stream()
                    .anyMatch(offer -> offer.itemTemplateId().equals(templateId))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase();
    }
}
