package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalItemUseService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Read-only player bag fallback backed only by durable canonical item state. */
public final class FabricBagRuntime {
    private static final String USE_PREFLIGHT_CONTEXT = "bag_inspection";

    private FabricBagRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("bag")
                                .executes(context -> show(context.getSource()))
                                .then(CommandManager.literal("inspect")
                                        .then(CommandManager.argument("item", StringArgumentType.word())
                                                .executes(context -> inspect(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "item"))))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        return showPlayerBag(player);
    }

    /**
     * Reusable server-side bag projection for command/bootstrap surfaces.
     * The caller supplies only the authenticated server player; canonical inventory truth is always re-read here.
     */
    static int showPlayerBag(ServerPlayerEntity player) {
        if (!hasCanonicalTrainer(player)) return 0;

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalBagQueryService.BagSnapshot bag = service(player).inspect(playerId);
        player.sendMessage(Text.literal("AutoPTU bag"), false);
        if (bag.entries().isEmpty()) {
            player.sendMessage(Text.literal("Canonical inventory is empty."), false);
            return 1;
        }
        for (CanonicalBagQueryService.BagEntry entry : bag.entries()) {
            player.sendMessage(renderInteractiveEntry(entry), false);
        }
        player.sendMessage(Text.literal(formatTotals(bag)), false);
        return 1;
    }

    /** Open the first server-authoritative bag page. */
    static int openPlayerBagScreen(ServerPlayerEntity player) {
        return openPlayerBagScreen(player, 0);
    }

    /**
     * Normal-player bag surface. Every page transition re-reads canonical inventory, clamps the requested page
     * against current server state and builds a fresh read-only slot-to-canonical-instance mapping.
     */
    static int openPlayerBagScreen(ServerPlayerEntity player, int requestedPage) {
        if (!hasCanonicalTrainer(player)) return 0;

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalBagQueryService.BagSnapshot bag = service(player).inspect(playerId);
        if (bag.entries().isEmpty()) {
            player.sendMessage(Text.literal("Canonical inventory is empty."), true);
            return 1;
        }

        int page = FabricCanonicalBagScreenHandler.clampPage(requestedPage, bag.entries().size());
        int pageCount = FabricCanonicalBagScreenHandler.pageCount(bag.entries().size());
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignoredPlayer) -> new FabricCanonicalBagScreenHandler(
                        syncId,
                        playerInventory,
                        bag.entries(),
                        page),
                Text.literal("AutoPTU Bag " + (page + 1) + "/" + pageCount)));
        return 1;
    }

    /** Revalidate one server-selected canonical stack without executing any item effect. */
    static int inspectItemUse(ServerPlayerEntity player, String itemInstanceId) {
        if (!hasCanonicalTrainer(player)) return 0;

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalBagQueryService.ItemInspection inspection = service(player).inspectItem(playerId, itemInstanceId);
        CanonicalBagQueryService.BagEntry entry = inspection.entries().stream()
                .filter(candidate -> candidate.itemInstanceId().equals(itemInstanceId))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            player.sendMessage(Text.literal("That canonical stack changed. Reopen the bag."), true);
            return 0;
        }

        CanonicalItemUseService.Decision decision = useService(player).canUse(new CanonicalItemUseService.Request(
                playerId,
                entry.itemInstanceId(),
                playerId,
                USE_PREFLIGHT_CONTEXT,
                true,
                true
        ));
        player.sendMessage(Text.literal(formatUsePreflight(entry.itemInstanceId(), decision)), true);
        return decision.allowed() ? 1 : 0;
    }

    private static int inspect(ServerCommandSource source, String itemKey) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalBagQueryService.ItemInspection inspection = service(player).inspectItem(playerId, itemKey);
        if (!inspection.found()) {
            source.sendError(Text.literal("No canonical bag item matches '" + inspection.requestedKey() + "'."));
            return 0;
        }
        for (String line : formatInspectionLines(inspection)) player.sendMessage(Text.literal(line), false);

        CanonicalItemUseService useService = useService(player);
        for (CanonicalBagQueryService.BagEntry entry : inspection.entries()) {
            CanonicalItemUseService.Decision decision = useService.canUse(new CanonicalItemUseService.Request(
                    playerId,
                    entry.itemInstanceId(),
                    playerId,
                    USE_PREFLIGHT_CONTEXT,
                    true,
                    true
            ));
            player.sendMessage(Text.literal(formatUsePreflight(entry.itemInstanceId(), decision)), false);
        }
        player.sendMessage(Text.literal(
                "Use preflight validates canonical ownership/availability only; authored effects and PTU legality remain separate authority."), false);
        return 1;
    }

    private static ServerPlayerEntity requireCanonicalPlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU bag must be requested by an authenticated player."));
            return null;
        }
        if (!hasCanonicalTrainer(player)) return null;
        return player;
    }

    private static boolean hasCanonicalTrainer(ServerPlayerEntity player) {
        if (player.getServer() == null) return false;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            player.sendMessage(Text.literal("Canonical Trainer state is not loaded."), true);
            return false;
        }
        return true;
    }

    private static CanonicalBagQueryService service(ServerPlayerEntity player) {
        return new CanonicalBagQueryService(
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()));
    }

    private static CanonicalItemUseService useService(ServerPlayerEntity player) {
        return new CanonicalItemUseService(
                FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()));
    }

    private static Text renderInteractiveEntry(CanonicalBagQueryService.BagEntry entry) {
        MutableText line = Text.literal(formatEntryLine(entry));
        Text checkUse = Text.literal(" [check use]").styled(style -> style
                .withColor(Formatting.AQUA)
                .withUnderline(true)
                .withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/autoptu bag inspect " + entry.itemInstanceId())));
        return line.append(checkUse);
    }

    static String formatUsePreflight(String itemInstanceId, CanonicalItemUseService.Decision decision) {
        if (decision.allowed()) {
            return "Use preflight | stack " + itemInstanceId
                    + " | ready | available " + decision.availableQuantity();
        }
        return "Use preflight | stack " + itemInstanceId + " | blocked | " + decision.reason();
    }

    static List<String> formatLines(CanonicalBagQueryService.BagSnapshot bag) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU bag");
        if (bag.entries().isEmpty()) {
            lines.add("Canonical inventory is empty.");
            return List.copyOf(lines);
        }

        for (CanonicalBagQueryService.BagEntry entry : bag.entries()) {
            lines.add(formatEntryLine(entry));
        }
        lines.add(formatTotals(bag));
        return List.copyOf(lines);
    }

    private static String formatEntryLine(CanonicalBagQueryService.BagEntry entry) {
        StringBuilder line = new StringBuilder()
                .append(entry.templateId())
                .append(" x").append(entry.quantity())
                .append(" | available ").append(entry.availableQuantity());
        if (entry.reservedQuantity() > 0) {
            line.append(" | reserved ").append(entry.reservedQuantity());
        }
        if (entry.reservationConsumed()) {
            line.append(" | transaction lock after consumption");
        } else if (entry.transactionLocked()) {
            line.append(" | transaction lock");
        }
        line.append(" | stack ").append(entry.itemInstanceId());
        return line.toString();
    }

    private static String formatTotals(CanonicalBagQueryService.BagSnapshot bag) {
        return "Totals: quantity " + bag.totalQuantity()
                + ", available " + bag.totalAvailable()
                + ", reserved " + bag.totalReserved()
                + ", locks " + bag.transactionLocks();
    }

    static List<String> formatInspectionLines(CanonicalBagQueryService.ItemInspection inspection) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU bag inspect " + inspection.requestedKey());
        for (CanonicalBagQueryService.BagEntry entry : inspection.entries()) {
            StringBuilder line = new StringBuilder()
                    .append(entry.templateId())
                    .append(" | stack ").append(entry.itemInstanceId())
                    .append(" | quantity ").append(entry.quantity())
                    .append(" | available ").append(entry.availableQuantity())
                    .append(" | revision ").append(entry.revision());
            if (entry.reservedQuantity() > 0) {
                line.append(" | reserved ").append(entry.reservedQuantity());
            }
            if (entry.transactionLocked()) {
                line.append(" | reservation ").append(entry.reservationId());
                if (entry.reservationConsumed()) line.append(" (consumed, lock retained)");
            }
            lines.add(line.toString());
        }
        if (!inspection.exactInstanceMatch() && inspection.entries().size() > 1) {
            lines.add("Template totals: quantity " + inspection.totalQuantity()
                    + ", available " + inspection.totalAvailable()
                    + ", reserved " + inspection.totalReserved());
        }
        return List.copyOf(lines);
    }
}
