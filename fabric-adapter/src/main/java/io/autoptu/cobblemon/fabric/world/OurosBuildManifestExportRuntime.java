package io.autoptu.cobblemon.fabric.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CI/export-only runtime that turns an actual Minecraft build result into browser review data.
 *
 * This deliberately runs the production builder inside a real Fabric server, then reads the final
 * BlockState values back from the world. The browser manifest is therefore downstream of Minecraft
 * placement rather than a second hand-maintained interpretation of the build.
 */
public final class OurosBuildManifestExportRuntime {
    public static final String OUTPUT_PROPERTY = "autoptu.ourosBuildManifestOutput";
    public static final String SUCCESS_MARKER = "AutoPTU exact Ouros build manifest export passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-ouros-build-export");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int MIN_X = -33;
    private static final int MAX_X = 33;
    private static final int MIN_Y = -4;
    private static final int MAX_Y = 22;
    private static final int MIN_Z = -33;
    private static final int MAX_Z = 33;

    private OurosBuildManifestExportRuntime() {}

    public static void registerIfEnabled() {
        String output = System.getProperty(OUTPUT_PROPERTY);
        if (output == null || output.isBlank()) {
            return;
        }
        Path outputPath = Path.of(output).toAbsolutePath().normalize();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> export(server, outputPath));
    }

    private static void export(MinecraftServer server, Path outputPath) {
        ServerWorld world = server.getOverworld();
        if (world == null) {
            throw new IllegalStateException("Overworld is unavailable for Ouros build export");
        }

        // Use a clean, isolated volume high above ordinary terrain so the manifest contains only
        // authored build output. The production builder itself remains unchanged.
        BlockPos origin = new BlockPos(0, 100, 0);
        clearCaptureVolume(world, origin);
        MeridianCanopyGymBuilder.build(world, origin);
        MeridianCanopyGymDetailPass.apply(world, origin);
        MeridianCanopyGymAuthoredGeometryPass.apply(world, origin);

        Manifest manifest = capture(world, origin);
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, GSON.toJson(manifest.json()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write Ouros build manifest to " + outputPath, e);
        }

        LOGGER.info("{}: {} blocks, {} palette states, sha256={}, output={}",
                SUCCESS_MARKER,
                manifest.blockCount(),
                manifest.paletteCount(),
                manifest.hash(),
                outputPath);
    }

    private static void clearCaptureVolume(ServerWorld world, BlockPos origin) {
        BlockState air = net.minecraft.block.Blocks.AIR.getDefaultState();
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                for (int z = MIN_Z; z <= MAX_Z; z++) {
                    world.setBlockState(origin.add(x, y, z), air);
                }
            }
        }
    }

    private static Manifest capture(ServerWorld world, BlockPos origin) {
        Map<String, Integer> paletteIndices = new LinkedHashMap<>();
        List<JsonObject> palette = new ArrayList<>();
        JsonArray blocks = new JsonArray();
        MessageDigest digest = sha256();
        int blockCount = 0;

        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                for (int z = MIN_Z; z <= MAX_Z; z++) {
                    BlockState state = world.getBlockState(origin.add(x, y, z));
                    if (state.isAir()) {
                        continue;
                    }

                    StateSnapshot snapshot = snapshot(state);
                    int paletteIndex = paletteIndices.computeIfAbsent(snapshot.key(), ignored -> {
                        palette.add(snapshot.json());
                        return palette.size() - 1;
                    });

                    JsonArray encoded = new JsonArray();
                    encoded.add(x);
                    encoded.add(y);
                    encoded.add(z);
                    encoded.add(paletteIndex);
                    blocks.add(encoded);
                    blockCount++;

                    String hashLine = x + "," + y + "," + z + "=" + snapshot.key() + "\n";
                    digest.update(hashLine.getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        String hash = hex(digest.digest());
        JsonObject root = new JsonObject();
        root.addProperty("format", "ouros.minecraft.block-manifest.v1");
        root.addProperty("buildId", "meridian_canopy_gym");
        root.addProperty("displayName", "Meridian Canopy Gym");
        root.addProperty("minecraftVersion", "1.21.1");
        root.addProperty("geometryAuthority", "live_server_final_blockstate_scan");
        root.addProperty("geometrySha256", hash);
        root.addProperty("blockCount", blockCount);
        root.add("min", vector(MIN_X, MIN_Y, MIN_Z));
        root.add("max", vector(MAX_X, MAX_Y, MAX_Z));
        root.add("size", vector(MAX_X - MIN_X + 1, MAX_Y - MIN_Y + 1, MAX_Z - MIN_Z + 1));
        JsonArray sources = new JsonArray();
        sources.add("MeridianCanopyGymBuilder");
        sources.add("MeridianCanopyGymDetailPass");
        sources.add("MeridianCanopyGymAuthoredGeometryPass");
        root.add("productionSources", sources);
        JsonArray paletteJson = new JsonArray();
        palette.forEach(paletteJson::add);
        root.add("palette", paletteJson);
        root.add("blocks", blocks);
        return new Manifest(root, blockCount, palette.size(), hash);
    }

    private static StateSnapshot snapshot(BlockState state) {
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        JsonObject json = new JsonObject();
        json.addProperty("id", id);

        List<Map.Entry<Property<?>, Comparable<?>>> entries = new ArrayList<>(state.getEntries().entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().getName()));
        JsonObject properties = new JsonObject();
        StringBuilder key = new StringBuilder(id);
        if (!entries.isEmpty()) {
            key.append('[');
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<Property<?>, Comparable<?>> entry = entries.get(i);
                String propertyName = entry.getKey().getName();
                String valueName = propertyValueName(entry.getKey(), entry.getValue());
                if (i > 0) {
                    key.append(',');
                }
                key.append(propertyName).append('=').append(valueName);
                properties.addProperty(propertyName, valueName);
            }
            key.append(']');
            json.add("properties", properties);
        }
        return new StateSnapshot(key.toString(), json);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(Property property, Comparable value) {
        return property.name(value);
    }

    private static JsonArray vector(int x, int y, int z) {
        JsonArray vector = new JsonArray();
        vector.add(x);
        vector.add(y);
        vector.add(z);
        return vector;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    private record StateSnapshot(String key, JsonObject json) {}

    private record Manifest(JsonObject json, int blockCount, int paletteCount, String hash) {}
}
