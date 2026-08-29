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

    public static final Block PTU_RECOVERY_BED = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(1.5F).nonOpaque());
    public static final Item PTU_RECOVERY_BED_ITEM = new BlockItem(PTU_RECOVERY_BED, new Item.Settings());
    public static final Block CEDAR_MART_COUNTER = new Block(AbstractBlock.Settings.copy(Blocks.BARREL).strength(2.5F));
    public static final Item CEDAR_MART_COUNTER_ITEM = new BlockItem(CEDAR_MART_COUNTER, new Item.Settings());
    public static final Block ITEM_STORAGE_TERMINAL = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0F));
    public static final Item ITEM_STORAGE_TERMINAL_ITEM = new BlockItem(ITEM_STORAGE_TERMINAL, new Item.Settings());
    public static final Block CRAFTING_WORKSTATION = new Block(AbstractBlock.Settings.copy(Blocks.SMITHING_TABLE).strength(3.5F));
    public static final Item CRAFTING_WORKSTATION_ITEM = new BlockItem(CRAFTING_WORKSTATION, new Item.Settings());

    /** Dedicated field-camp identity. Durable camp results and quality remain server authoritative. */
    public static final Block FIELD_CAMP = new Block(AbstractBlock.Settings.copy(Blocks.CAMPFIRE).strength(2.0F).nonOpaque());
    public static final Item FIELD_CAMP_ITEM = new BlockItem(FIELD_CAMP, new Item.Settings());

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
        registered = true;
    }
}
