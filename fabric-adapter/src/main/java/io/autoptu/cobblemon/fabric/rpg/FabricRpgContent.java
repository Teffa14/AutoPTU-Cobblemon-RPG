package io.autoptu.cobblemon.fabric.rpg;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Namespaced Minecraft content owned by AutoPTU-Cobblemon-RPG. */
public final class FabricRpgContent {
    public static final String MOD_ID = "autoptu_cobblemon_rpg_fabric_adapter";
    public static final Identifier PTU_RECOVERY_BED_ID = Identifier.of(MOD_ID, "ptu_recovery_bed");
    public static final Identifier CEDAR_MART_COUNTER_ID = Identifier.of(MOD_ID, "cedar_mart_counter");
    public static final Identifier ITEM_STORAGE_TERMINAL_ID = Identifier.of(MOD_ID, "item_storage_terminal");
    public static final Identifier CRAFTING_WORKSTATION_ID = Identifier.of(MOD_ID, "crafting_workstation");
    public static final Identifier FIELD_CAMP_ID = Identifier.of(MOD_ID, "field_camp");
    public static final Identifier GYM_LEAGUE_REGISTRATION_DESK_ID = Identifier.of(MOD_ID, "gym_league_registration_desk");
    public static final Identifier CEDAR_BADGE_GATE_ID = Identifier.of(MOD_ID, "cedar_badge_gate");
    public static final Identifier OUROS_MAILBOX_ID = Identifier.of(MOD_ID, "ouros_mailbox");

    public static final Block PTU_RECOVERY_BED = new Block(
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(1.5F).nonOpaque());
    public static final Item PTU_RECOVERY_BED_ITEM = new BlockItem(PTU_RECOVERY_BED, new Item.Settings());

    public static final Block CEDAR_MART_COUNTER = new Block(
            AbstractBlock.Settings.copy(Blocks.BARREL).strength(2.5F));
    public static final Item CEDAR_MART_COUNTER_ITEM = new BlockItem(CEDAR_MART_COUNTER, new Item.Settings());

    public static final Block ITEM_STORAGE_TERMINAL = new Block(
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0F));
    public static final Item ITEM_STORAGE_TERMINAL_ITEM = new BlockItem(ITEM_STORAGE_TERMINAL, new Item.Settings());

    public static final Block CRAFTING_WORKSTATION = new Block(
            AbstractBlock.Settings.copy(Blocks.SMITHING_TABLE).strength(3.5F));
    public static final Item CRAFTING_WORKSTATION_ITEM = new BlockItem(CRAFTING_WORKSTATION, new Item.Settings());

    public static final Block FIELD_CAMP = new Block(
            AbstractBlock.Settings.copy(Blocks.BARREL).strength(2.0F).nonOpaque());
    public static final Item FIELD_CAMP_ITEM = new BlockItem(FIELD_CAMP, new Item.Settings());

    public static final Block GYM_LEAGUE_REGISTRATION_DESK = new Block(
            AbstractBlock.Settings.copy(Blocks.CARTOGRAPHY_TABLE).strength(2.5F));
    public static final Item GYM_LEAGUE_REGISTRATION_DESK_ITEM = new BlockItem(GYM_LEAGUE_REGISTRATION_DESK, new Item.Settings());

    public static final Block CEDAR_BADGE_GATE = new Block(
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0F).nonOpaque());
    public static final Item CEDAR_BADGE_GATE_ITEM = new BlockItem(CEDAR_BADGE_GATE, new Item.Settings());

    /** Physical authored inbox surface. Mail/reward truth remains in CanonicalMailService. */
    public static final Block OUROS_MAILBOX = new Block(
            AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).strength(3.0F));
    public static final Item OUROS_MAILBOX_ITEM = new BlockItem(OUROS_MAILBOX, new Item.Settings());

    private static boolean registered;

    private FabricRpgContent() {}

    public static void register() {
        if (registered) return;
        Registry.register(Registries.BLOCK, PTU_RECOVERY_BED_ID, PTU_RECOVERY_BED);
        Registry.register(Registries.ITEM, PTU_RECOVERY_BED_ID, PTU_RECOVERY_BED_ITEM);
        Registry.register(Registries.BLOCK, CEDAR_MART_COUNTER_ID, CEDAR_MART_COUNTER);
        Registry.register(Registries.ITEM, CEDAR_MART_COUNTER_ID, CEDAR_MART_COUNTER_ITEM);
        Registry.register(Registries.BLOCK, ITEM_STORAGE_TERMINAL_ID, ITEM_STORAGE_TERMINAL);
        Registry.register(Registries.ITEM, ITEM_STORAGE_TERMINAL_ID, ITEM_STORAGE_TERMINAL_ITEM);
        Registry.register(Registries.BLOCK, CRAFTING_WORKSTATION_ID, CRAFTING_WORKSTATION);
        Registry.register(Registries.ITEM, CRAFTING_WORKSTATION_ID, CRAFTING_WORKSTATION_ITEM);
        Registry.register(Registries.BLOCK, FIELD_CAMP_ID, FIELD_CAMP);
        Registry.register(Registries.ITEM, FIELD_CAMP_ID, FIELD_CAMP_ITEM);
        Registry.register(Registries.BLOCK, GYM_LEAGUE_REGISTRATION_DESK_ID, GYM_LEAGUE_REGISTRATION_DESK);
        Registry.register(Registries.ITEM, GYM_LEAGUE_REGISTRATION_DESK_ID, GYM_LEAGUE_REGISTRATION_DESK_ITEM);
        Registry.register(Registries.BLOCK, CEDAR_BADGE_GATE_ID, CEDAR_BADGE_GATE);
        Registry.register(Registries.ITEM, CEDAR_BADGE_GATE_ID, CEDAR_BADGE_GATE_ITEM);
        Registry.register(Registries.BLOCK, OUROS_MAILBOX_ID, OUROS_MAILBOX);
        Registry.register(Registries.ITEM, OUROS_MAILBOX_ID, OUROS_MAILBOX_ITEM);
        registered = true;
    }
}
