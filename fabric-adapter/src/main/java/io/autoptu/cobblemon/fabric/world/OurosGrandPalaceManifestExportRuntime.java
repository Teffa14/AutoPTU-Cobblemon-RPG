package io.autoptu.cobblemon.fabric.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.autoptu.cobblemon.fabric.world.build.OurosFloatingBlockAudit;
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

/** Exact live-server exporter for the OI-107 Ouros Grand Palace. */
public final class OurosGrandPalaceManifestExportRuntime {
    public static final String OUTPUT_PROPERTY = "autoptu.ourosGrandPalaceManifestOutput";
    public static final String SUCCESS_MARKER = "AutoPTU exact Ouros Grand Palace manifest export passed";
    public static final String STRUCTURAL_AUDIT_MARKER = "AutoPTU Ouros Grand Palace floating component audit passed";
    public static final String ENVELOPE_AUDIT_MARKER = "AutoPTU Ouros Grand Palace capture envelope audit passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-ouros-grand-palace-export");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final int MIN_X = OurosGrandPalace.MIN_X;
    private static final int MAX_X = OurosGrandPalace.MAX_X;
    private static final int MIN_Y = OurosGrandPalace.MIN_Y;
    // The authored roof lanterns intentionally exceed the original prototype envelope.
    // Review bounds follow the building instead of clipping or shrinking the landmark.
    private static final int MAX_Y = 48;
    private static final int MIN_Z = OurosGrandPalace.MIN_Z;
    private static final int MAX_Z = OurosGrandPalace.MAX_Z;

    private static final int GUARD_MIN_X = MIN_X - 7;
    private static final int GUARD_MAX_X = MAX_X + 7;
    private static final int GUARD_MIN_Y = MIN_Y;
    private static final int GUARD_MAX_Y = MAX_Y + 8;
    private static final int GUARD_MIN_Z = MIN_Z - 7;
    private static final int GUARD_MAX_Z = MAX_Z + 7;

    private OurosGrandPalaceManifestExportRuntime() {}

    public static void registerIfEnabled() {
        String output = System.getProperty(OUTPUT_PROPERTY);
        if (output == null || output.isBlank()) return;
        Path outputPath = Path.of(output).toAbsolutePath().normalize();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> export(server, outputPath));
    }

    private static void export(MinecraftServer server, Path outputPath) {
        ServerWorld world = server.getOverworld();
        if (world == null) throw new IllegalStateException("Overworld is unavailable for Grand Palace export");

        BlockPos origin = new BlockPos(0, 100, 0);
        clearGuardVolume(world, origin);
        OurosGrandPalace.build(world, origin);

        validateCaptureEnvelope(world, origin);
        LOGGER.info(ENVELOPE_AUDIT_MARKER);

        OurosFloatingBlockAudit.Report audit = OurosFloatingBlockAudit.scan(
                world,
                origin,
                new OurosFloatingBlockAudit.Bounds(MIN_X, MAX_X, MIN_Y, MAX_Y, MIN_Z, MAX_Z),
                1
        );
        if (!audit.passed()) {
            throw new IllegalStateException("Ouros Grand Palace contains disconnected floating geometry: "
                    + OurosFloatingBlockAudit.describeFailures(audit));
        }
        LOGGER.info("{}: {} connected components, all {} anchored",
                STRUCTURAL_AUDIT_MARKER, audit.componentCount(), audit.anchoredComponentCount());

        Manifest manifest = capture(world, origin, audit);
        try {
            Path parent = outputPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(outputPath, GSON.toJson(manifest.json()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write Grand Palace manifest to " + outputPath, e);
        }

        LOGGER.info("{}: {} blocks, {} palette states, sha256={}, output={}",
                SUCCESS_MARKER, manifest.blockCount(), manifest.paletteCount(), manifest.hash(), outputPath);
    }

    private static void clearGuardVolume(ServerWorld world, BlockPos origin) {
        BlockState air = net.minecraft.block.Blocks.AIR.getDefaultState();
        for (int x = GUARD_MIN_X; x <= GUARD_MAX_X; x++) {
            for (int y = GUARD_MIN_Y; y <= GUARD_MAX_Y; y++) {
                for (int z = GUARD_MIN_Z; z <= GUARD_MAX_Z; z++) {
                    world.setBlockState(origin.add(x, y, z), air);
                }
            }
        }
    }

    private static void validateCaptureEnvelope(ServerWorld world, BlockPos origin) {
        List<String> overflow = new ArrayList<>();
        for (int x = GUARD_MIN_X; x <= GUARD_MAX_X; x++) {
            for (int y = GUARD_MIN_Y; y <= GUARD_MAX_Y; y++) {
                for (int z = GUARD_MIN_Z; z <= GUARD_MAX_Z; z++) {
                    boolean inside = x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y && z >= MIN_Z && z <= MAX_Z;
                    if (inside || world.getBlockState(origin.add(x, y, z)).isAir()) continue;
                    if (overflow.size() < 20) {
                        overflow.add(x + "," + y + "," + z + "="
                                + Registries.BLOCK.getId(world.getBlockState(origin.add(x, y, z)).getBlock()));
                    }
                }
            }
        }
        if (!overflow.isEmpty()) {
            throw new IllegalStateException("Grand Palace wrote blocks outside the exact viewer envelope; first entries: "
                    + String.join("; ", overflow));
        }
    }

    private static Manifest capture(ServerWorld world, BlockPos origin, OurosFloatingBlockAudit.Report audit) {
        Map<String, Integer> paletteIndices = new LinkedHashMap<>();
        List<JsonObject> palette = new ArrayList<>();
        JsonArray blocks = new JsonArray();
        MessageDigest digest = sha256();
        int blockCount = 0;

        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                for (int z = MIN_Z; z <= MAX_Z; z++) {
                    BlockState state = world.getBlockState(origin.add(x, y, z));
                    if (state.isAir()) continue;
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
                    digest.update((x + "," + y + "," + z + "=" + snapshot.key() + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        String hash = hex(digest.digest());
        JsonObject root = new JsonObject();
        root.addProperty("format", "ouros.minecraft.block-manifest.v1");
        root.addProperty("buildId", "ouros_grand_palace");
        root.addProperty("displayName", "Ouros Grand Palace - OI-107 Reference Suite");
        root.addProperty("minecraftVersion", "1.21.1");
        root.addProperty("geometryAuthority", "live_server_final_blockstate_scan");
        root.addProperty("geometrySha256", hash);
        root.addProperty("blockCount", blockCount);
        root.addProperty("captureEnvelopeAudit", "passed");
        root.addProperty("floatingComponentAudit", "passed");
        root.addProperty("structuralComponentCount", audit.componentCount());
        root.addProperty("anchoredStructuralComponentCount", audit.anchoredComponentCount());
        root.add("min", vector(MIN_X, MIN_Y, MIN_Z));
        root.add("max", vector(MAX_X, MAX_Y, MAX_Z));
        root.add("size", vector(MAX_X - MIN_X + 1, MAX_Y - MIN_Y + 1, MAX_Z - MIN_Z + 1));

        JsonArray sources = new JsonArray();
        sources.add("OurosGrandPalace");
        sources.add("OurosGrandPalaceBuildKit");
        sources.add("OurosGrandPalaceCeremonialRooms");
        sources.add("OurosGrandPalaceSalonRooms");
        sources.add("OurosGrandPalaceUpperRooms");
        root.add("productionSources", sources);

        JsonArray spaces = new JsonArray();
        for (String name : new String[]{
                "Antechamber", "Audience Chamber", "Themis Hall", "Railing, Tables and Chairs Salon",
                "Cabinet", "Salla Terrena", "Coat of Arms Relief Hall", "Blooming Salon", "Hunting Salon",
                "Library", "Book Cabinet and Globe Room", "Geography Cabinet", "Porcelain Hall", "Marble Salon",
                "Gallery of Art", "Accounting Office", "Music Chamber with Harpsichord", "Blue Salon", "Banquet Hall"
        }) spaces.add(name);
        root.add("authoredSpaces", spaces);

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
                if (i > 0) key.append(',');
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
        JsonArray result = new JsonArray();
        result.add(x); result.add(y); result.add(z);
        return result;
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
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    private record StateSnapshot(String key, JsonObject json) {}
    private record Manifest(JsonObject json, int blockCount, int paletteCount, String hash) {}
}