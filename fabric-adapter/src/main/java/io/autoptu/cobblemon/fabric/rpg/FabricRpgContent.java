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

    /**
     * Distinct authored facility block. Its recipe may consume vanilla/Cobblemon ingredients, but
     * world identity is this registry ID rather than an accidental arrangement of unrelated blocks.
     *
     * Do not copy BedBlock settings here: vanilla bed settings capture BedBlock-only HEAD/FOOT
     * properties and crash when applied to a plain registered block. Bed-like presentation belongs
     * in this mod's model/shape resources, not in inherited vanilla bed gameplay state.
     */
    public static final Block PTU_RECOVERY_BED = new Block(
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                    .strength(1.5F)
                    .nonOpaque()
    );
    public static final Item PTU_RECOVERY_BED_ITEM = new BlockItem(PTU_RECOVERY_BED, new Item.Settings());

    private static boolean registered;

    private FabricRpgContent() {}

    public static void register() {
        if (registered) return;
        Registry.register(Registries.BLOCK, PTU_RECOVERY_BED_ID, PTU_RECOVERY_BED);
        Registry.register(Registries.ITEM, PTU_RECOVERY_BED_ID, PTU_RECOVERY_BED_ITEM);
        registered = true;
    }
}
