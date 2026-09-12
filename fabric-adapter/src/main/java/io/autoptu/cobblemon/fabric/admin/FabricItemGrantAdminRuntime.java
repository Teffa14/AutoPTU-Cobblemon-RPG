package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalItemInstance;
import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/** Operator-only grant of server-authored canonical item templates. */
public final class FabricItemGrantAdminRuntime implements ModInitializer {
    private static final int MAX_GRANT_QUANTITY = 9999;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("grant")
                                        .then(CommandManager.literal("item")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .then(CommandManager.argument("item", StringArgumentType.word())
                                                                .executes(context -> grant(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "player"),
                                                                        StringArgumentType.getString(context, "item"),
                                                                        1))
                                                                .then(CommandManager.argument("qty", IntegerArgumentType.integer(1, MAX_GRANT_QUANTITY))
                                                                        .executes(context -> grant(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "player"),
                                                                                StringArgumentType.getString(context, "item"),
                                                                                IntegerArgumentType.getInteger(context, "qty"))))))))))));
    }

    private static int grant(ServerCommandSource source, String playerName, String itemTemplateId, int quantity) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return 0;
        }

        String templateId = normalize(itemTemplateId);
        if (!isAuthoredTemplate(templateId)) {
            source.sendError(Text.literal("Unknown server-authored canonical item template."));
            return 0;
        }

        String itemInstanceId = "admin-grant:" + UUID.randomUUID();
        CanonicalItemInstance item = new CanonicalItemInstance(itemInstanceId, playerId, templateId, quantity, 0L);
        boolean created;
        try {
            created = FabricCanonicalPlayerStoreRuntime.requireAssetRepository(source.getServer()).createItemIfAbsent(item);
        } catch (RuntimeException failed) {
            source.sendError(Text.literal("Canonical item grant could not be committed safely; no Minecraft inventory fallback was used."));
            return 0;
        }
        if (!created) {
            source.sendError(Text.literal("Canonical item grant identity collided; retry the command."));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Granted " + quantity + " " + templateId
                + " to " + target.getGameProfile().getName()
                + " | canonical instance " + itemInstanceId), true);
        source.sendFeedback(() -> Text.literal(
                "Grant changed only persistent canonical inventory; PTU item legality/effects and Cobblemon gameplay state were not evaluated."), false);
        return 1;
    }

    private static boolean isAuthoredTemplate(String templateId) {
        for (String shopId : CanonicalShopCatalogue.DEFAULT.shopIds()) {
            boolean present = CanonicalShopCatalogue.DEFAULT.offers(shopId).stream()
                    .anyMatch(offer -> offer.itemTemplateId().equals(templateId));
            if (present) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase();
    }
}
