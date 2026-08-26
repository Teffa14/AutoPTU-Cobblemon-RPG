package io.autoptu.cobblemon.fabric.world.build;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;

/**
 * Loads and places Ouros-owned Minecraft structure templates from mod data resources.
 *
 * Templates are authored world geometry. Their blocks do not imply PTU terrain or battle effects.
 */
public final class OurosStructureTemplatePlacer {
    private OurosStructureTemplatePlacer() {}

    public enum Status {
        PLACED,
        TEMPLATE_MISSING,
        PLACEMENT_FAILED
    }

    public record PlacementResult(
            Status status,
            Identifier templateId,
            BlockPos origin,
            BlockRotation rotation,
            BlockMirror mirror,
            Vec3i transformedSize
    ) {
        public boolean placed() {
            return status == Status.PLACED;
        }
    }

    public static PlacementResult place(
            ServerWorld world,
            Identifier templateId,
            BlockPos origin,
            BlockRotation rotation,
            BlockMirror mirror
    ) {
        if (world == null || templateId == null || origin == null || rotation == null || mirror == null) {
            throw new IllegalArgumentException("world, templateId, origin, rotation and mirror are required");
        }

        Optional<StructureTemplate> templateOptional = world.getStructureTemplateManager().getTemplate(templateId);
        if (templateOptional.isEmpty()) {
            return new PlacementResult(
                    Status.TEMPLATE_MISSING,
                    templateId,
                    origin,
                    rotation,
                    mirror,
                    new Vec3i(0, 0, 0)
            );
        }

        StructureTemplate template = templateOptional.get();
        StructurePlacementData placementData = new StructurePlacementData()
                .setRotation(rotation)
                .setMirror(mirror)
                .setIgnoreEntities(true);

        boolean placed = template.place(
                world,
                origin,
                origin,
                placementData,
                world.getRandom(),
                Block.NOTIFY_ALL
        );

        return new PlacementResult(
                placed ? Status.PLACED : Status.PLACEMENT_FAILED,
                templateId,
                origin,
                rotation,
                mirror,
                template.getRotatedSize(rotation)
        );
    }
}
