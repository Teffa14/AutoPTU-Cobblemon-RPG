package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalTrainerProfileService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProfileRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Minecraft-visible profile card and bounded cosmetic selection over server-owned Trainer identity. */
public final class FabricTrainerProfileRuntime {
    private static final int TOP_SLOT_COUNT = 27;
    private static final int[] TITLE_SLOTS = {10, 11, 12};
    private static final int[] THEME_SLOTS = {14, 15, 16};

    private FabricTrainerProfileRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("trainer")
                                .then(CommandManager.literal("profile")
                                        .executes(context -> show(context.getSource()))
                                        .then(CommandManager.literal("title")
                                                .then(CommandManager.argument("titleId", StringArgumentType.word())
                                                        .executes(context -> chooseTitle(context.getSource(), StringArgumentType.getString(context, "titleId")))))
                                        .then(CommandManager.literal("theme")
                                                .then(CommandManager.argument("themeId", StringArgumentType.word())
                                                        .executes(context -> chooseTheme(context.getSource(), StringArgumentType.getString(context, "themeId"))))))
                                .then(CommandManager.literal("card")
                                        .executes(context -> openCard(context.getSource()))))));
    }

    private static int show(ServerCommandSource source) {
        var context = resolve(source);
        if (context == null) return 0;
        var snapshot = context.service().inspect(context.playerId());
        context.player().sendMessage(Text.literal("Trainer Profile — " + snapshot.titleDisplayName()
                + " — card " + snapshot.cardThemeId()
                + " — revision " + snapshot.revision()), false);
        context.player().sendMessage(Text.literal("Titles: " + String.join(", ", CanonicalTrainerProfileService.authoredTitles().keySet())
                + " — themes: " + String.join(", ", CanonicalTrainerProfileService.authoredCardThemes())), false);
        return 1;
    }

    private static int openCard(ServerCommandSource source) {
        var context = resolve(source);
        if (context == null) return 0;
        openCard(context.player());
        return 1;
    }

    public static void openCard(ServerPlayerEntity player) {
        var context = resolve(player);
        if (context == null) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new TrainerCardScreenHandler(syncId, inventory, context),
                Text.literal("Ouros Trainer Card")
        ));
    }

    private static int chooseTitle(ServerCommandSource source, String titleId) {
        var context = resolve(source);
        if (context == null) return 0;
        try {
            var snapshot = context.service().chooseTitle(context.playerId(), titleId);
            context.player().sendMessage(Text.literal("Trainer title set to " + snapshot.titleDisplayName() + "."), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal("Unknown server-authored Trainer title: " + titleId));
            return 0;
        }
    }

    private static int chooseTheme(ServerCommandSource source, String themeId) {
        var context = resolve(source);
        if (context == null) return 0;
        try {
            var snapshot = context.service().chooseCardTheme(context.playerId(), themeId);
            context.player().sendMessage(Text.literal("Trainer card theme set to " + snapshot.cardThemeId() + "."), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal("Unknown server-authored Trainer card theme: " + themeId));
            return 0;
        }
    }

    private static Context resolve(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU Trainer profile must be requested by an authenticated player."));
            return null;
        }
        Context context = resolve(player);
        if (context == null) source.sendError(Text.literal("Canonical Trainer state is not loaded."));
        return context;
    }

    private static Context resolve(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return null;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) return null;
        return new Context(player, playerId, new CanonicalTrainerProfileService(
                new FileCanonicalTrainerProfileRepository(canonicalStateRoot(player))));
    }

    private static Path canonicalStateRoot(ServerPlayerEntity player) {
        return player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }

    private static final class TrainerCardScreenHandler extends GenericContainerScreenHandler {
        private final SimpleInventory displayInventory;
        private final Context context;
        private CanonicalTrainerProfileService.Snapshot displayed;
        private final List<Map.Entry<String, String>> titles;
        private final List<String> themes;

        private TrainerCardScreenHandler(int syncId, PlayerInventory inventory, Context context) {
            this(syncId, inventory, context, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private TrainerCardScreenHandler(int syncId, PlayerInventory inventory, Context context, SimpleInventory displayInventory) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, displayInventory, 3);
            this.displayInventory = displayInventory;
            this.context = context;
            this.titles = new ArrayList<>(CanonicalTrainerProfileService.authoredTitles().entrySet());
            this.titles.sort(Map.Entry.comparingByKey());
            this.themes = CanonicalTrainerProfileService.authoredCardThemes().stream().sorted(Comparator.naturalOrder()).toList();
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity clickingPlayer) {
            return clickingPlayer == context.player() && !context.player().isRemoved() && context.player().getServer() != null;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != context.player() || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                context.player().closeHandledScreen();
                return;
            }

            CanonicalTrainerProfileService.Snapshot current = context.service().inspect(context.playerId());
            if (displayed == null || current.revision() != displayed.revision()) {
                refresh();
                context.player().sendMessage(Text.literal("Trainer profile changed on the server. The card was refreshed."), true);
                return;
            }

            int titleIndex = indexOf(TITLE_SLOTS, slotIndex);
            if (titleIndex >= 0 && titleIndex < titles.size()) {
                var selected = titles.get(titleIndex);
                context.service().chooseTitle(context.playerId(), selected.getKey());
                context.player().sendMessage(Text.literal("Trainer title set to " + selected.getValue() + "."), true);
                refresh();
                return;
            }

            int themeIndex = indexOf(THEME_SLOTS, slotIndex);
            if (themeIndex >= 0 && themeIndex < themes.size()) {
                String selected = themes.get(themeIndex);
                context.service().chooseCardTheme(context.playerId(), selected);
                context.player().sendMessage(Text.literal("Trainer card theme set to " + selected + "."), true);
                refresh();
            }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        private void refresh() {
            displayed = context.service().inspect(context.playerId());
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);

            displayInventory.setStack(4, named(Items.NAME_TAG.getDefaultStack(),
                    displayed.titleDisplayName() + " | theme " + displayed.cardThemeId() + " | rev " + displayed.revision()));
            for (int i = 0; i < titles.size() && i < TITLE_SLOTS.length; i++) {
                var title = titles.get(i);
                boolean selected = title.getKey().equals(displayed.titleId());
                ItemStack icon = (selected ? Items.LIME_DYE : Items.PAPER).getDefaultStack();
                displayInventory.setStack(TITLE_SLOTS[i], named(icon, (selected ? "SELECTED | " : "Title | ") + title.getValue()));
            }
            for (int i = 0; i < themes.size() && i < THEME_SLOTS.length; i++) {
                String theme = themes.get(i);
                boolean selected = theme.equals(displayed.cardThemeId());
                ItemStack icon = (selected ? Items.LIME_DYE : Items.MAP).getDefaultStack();
                displayInventory.setStack(THEME_SLOTS[i], named(icon, (selected ? "SELECTED | " : "Theme | ") + theme));
            }
            displayInventory.setStack(22, named(Items.COMPASS.getDefaultStack(), "Choose a server-authored title or card theme"));
            displayInventory.markDirty();
            sendContentUpdates();
        }
    }

    private static int indexOf(int[] values, int target) {
        for (int i = 0; i < values.length; i++) if (values[i] == target) return i;
        return -1;
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }

    private record Context(ServerPlayerEntity player, String playerId, CanonicalTrainerProfileService service) { }
}
