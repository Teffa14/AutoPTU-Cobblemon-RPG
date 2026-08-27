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

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;

/** Exact live-server exporter for the courtyard-based Ouros Grand Palace V4. */
public final class OurosGrandPalaceV4ManifestExportRuntime {
    public static final String OUTPUT_PROPERTY = "autoptu.ourosGrandPalaceManifestOutput";
    public static final String SUCCESS_MARKER = "AutoPTU exact Ouros Grand Palace manifest export passed";
    public static final String STRUCTURAL_AUDIT_MARKER = "AutoPTU Ouros Grand Palace floating component audit passed";
    public static final String ENVELOPE_AUDIT_MARKER = "AutoPTU Ouros Grand Palace capture envelope audit passed";
    public static final String V4_SHAPE_MARKER = "AutoPTU Ouros Grand Palace V4 anti-box audit passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-ouros-grand-palace-v4-export");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final int MIN_X = OurosGrandPalaceV4Builder.MIN_X;
    private static final int MAX_X = OurosGrandPalaceV4Builder.MAX_X;
    private static final int MIN_Y = OurosGrandPalaceV4Builder.MIN_Y;
    private static final int MAX_Y = OurosGrandPalaceV4Builder.MAX_Y;
    private static final int MIN_Z = OurosGrandPalaceV4Builder.MIN_Z;
    private static final int MAX_Z = OurosGrandPalaceV4Builder.MAX_Z;

    private static final int GUARD_MIN_X = MIN_X - 8;
    private static final int GUARD_MAX_X = MAX_X + 8;
    private static final int GUARD_MIN_Y = MIN_Y;
    private static final int GUARD_MAX_Y = MAX_Y + 8;
    private static final int GUARD_MIN_Z = MIN_Z - 8;
    private static final int GUARD_MAX_Z = MAX_Z + 8;

    private OurosGrandPalaceV4ManifestExportRuntime() {}

    public static void registerIfEnabled() {
        String output = System.getProperty(OUTPUT_PROPERTY);
        if (output == null || output.isBlank()) return;
        Path outputPath = Path.of(output).toAbsolutePath().normalize();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> export(server, outputPath));
    }

    private static void export(MinecraftServer server, Path outputPath) {
        ServerWorld world = server.getOverworld();
        if (world == null) throw new IllegalStateException("Overworld is unavailable for Grand Palace V4 export");

        BlockPos origin = new BlockPos(0, 100, 0);
        clearGuardVolume(world, origin);
        OurosGrandPalaceV4Builder.build(world, origin);
        OurosGrandPalaceV4QualityAudit.Report shape = OurosGrandPalaceV4QualityAudit.assertValid(world, origin);
        LOGGER.info("{}: west/east ground {:.1f}%/{:.1f}%, west/east roof {:.1f}%/{:.1f}%",
                V4_SHAPE_MARKER,
                shape.westGroundOpen() * 100.0, shape.eastGroundOpen() * 100.0,
                shape.westSkyOpen() * 100.0, shape.eastSkyOpen() * 100.0);

        validateCaptureEnvelope(world, origin);
        LOGGER.info(ENVELOPE_AUDIT_MARKER);

        OurosFloatingBlockAudit.Report audit = OurosFloatingBlockAudit.scan(
                world,
                origin,
                new OurosFloatingBlockAudit.Bounds(MIN_X, MAX_X, MIN_Y, MAX_Y, MIN_Z, MAX_Z),
                1
        );
        if (!audit.passed()) {
            throw new IllegalStateException("Ouros Grand Palace V4 contains disconnected floating geometry: "
                    + OurosFloatingBlockAudit.describeFailures(audit));
        }
        LOGGER.info("{}: {} connected components, all {} anchored",
                STRUCTURAL_AUDIT_MARKER, audit.componentCount(), audit.anchoredComponentCount());

        Manifest manifest = capture(world, origin, audit, shape);
        try {
            Path parent = outputPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(outputPath, GSON.toJson(manifest.json()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write Grand Palace V4 manifest to " + outputPath, e);
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
                    boolean inside = x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y
                            && z >= MIN_Z && z <= MAX_Z;
                    if (inside || world.getBlockState(origin.add(x, y, z)).isAir()) continue;
                    if (overflow.size() < 20) {
                        BlockState state = world.getBlockState(origin.add(x, y, z));
                        overflow.add(x + "," + y + "," + z + "=" + Registries.BLOCK.getId(state.getBlock()));
                    }
                }
            }
        }
        if (!overflow.isEmpty()) {
            throw new IllegalStateException("Grand Palace V4 wrote outside the exact viewer envelope: "
                    + String.join("; ", overflow));
        }
    }

    private static Manifest capture(ServerWorld world, BlockPos origin,
                                    OurosFloatingBlockAudit.Report audit,
                                    OurosGrandPalaceV4QualityAudit.Report shape) {
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
                    digest.update((x + "," + y + "," + z + "=" + snapshot.key() + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        String hash = hex(digest.digest());
        JsonObject root = new JsonObject();
        root.addProperty("format", "ouros.minecraft.block-manifest.v1");
        root.addProperty("buildId", "ouros_grand_palace");
        root.addProperty("buildRevision", "courtyard-v4");
        root.addProperty("displayName", "Ouros Grand Palace - Courtyard V4");
        root.addProperty("minecraftVersion", "1.21.1");
        root.addProperty("geometryAuthority", "live_server_final_blockstate_scan");
        root.addProperty("geometrySha256", hash);
        root.addProperty("blockCount", blockCount);
        root.addProperty("captureEnvelopeAudit", "passed");
        root.addProperty("floatingComponentAudit", "passed");
        root.addProperty("antiBoxAudit", "passed");
        root.addProperty("structuralComponentCount", audit.componentCount());
        root.addProperty("anchoredStructuralComponentCount", audit.anchoredComponentCount());
        root.addProperty("westCourtGroundOpenRatio", shape.westGroundOpen());
        root.addProperty("eastCourtGroundOpenRatio", shape.eastGroundOpen());
        root.addProperty("westCourtRoofOpenRatio", shape.westSkyOpen());
        root.addProperty("eastCourtRoofOpenRatio", shape.eastSkyOpen());
        root.add("min", vector(MIN_X, MIN_Y, MIN_Z));
        root.add("max", vector(MAX_X, MAX_Y, MAX_Z));
        root.add("size", vector(MAX_X - MIN_X + 1, MAX_Y - MIN_Y + 1, MAX_Z - MIN_Z + 1));

        JsonArray sources = new JsonArray();
        for (String source : new String[]{
                "OurosGrandPalaceV4Plan",
                "OurosGrandPalaceV4Builder",
                "OurosGrandPalaceV4ArchitecturePass",
                "OurosGrandPalaceV4Rooms",
                "OurosGrandPalaceV4RoofPass",
                "OurosGrandPalaceV4QualityAudit",
                "OurosGrandPalaceBuildKit"
        }) sources.add(source);
        root.add("productionSources", sources);

        JsonArray spaces = new JsonArray();
        for (String name : new String[]{
                "Antechamber", "Audience Chamber", "Themis Hall", "Railing, Tables and Chairs Salon",
                "Cabinet", "Salla Terrena", "Coat of Arms Relief Hall", "Blooming Salon", "Hunting Salon",
                "Library", "Book Cabinet and Globe Room", "Geography Cabinet", "Porcelain Hall", "Marble Salon",
                "Gallery of Art", "Accounting Office", "Music Chamber with Harpsichord", "Blue Salon", "Banquet Hall"
        }) spaces.add(name);
        root.add("authoredSpaces", spaces);

        JsonArray reviewSpaces = new JsonArray();
        addReviewSpace(reviewSpaces, "antechamber", "Antechamber", ANTECHAMBER);
        addReviewSpace(reviewSpaces, "audience", "Audience Chamber", AUDIENCE_CHAMBER);
        addReviewSpace(reviewSpaces, "themis", "Themis Hall", THEMIS_HALL);
        addReviewSpace(reviewSpaces, "railings", "Railings, Tables and Chairs", v4(RAILING_SALON));
        addReviewSpace(reviewSpaces, "cabinet", "Cabinet", v4(CABINET));
        addReviewSpace(reviewSpaces, "salla", "Salla Terrena", v4(SALLA_TERRENA));
        addReviewSpace(reviewSpaces, "relief", "Coat of Arms Relief Hall", v4(COAT_OF_ARMS_HALL));
        addReviewSpace(reviewSpaces, "blooming", "Blooming Salon", v4(BLOOMING_SALON));
        addReviewSpace(reviewSpaces, "hunting", "Hunting Salon", v4(HUNTING_SALON));
        addReviewSpace(reviewSpaces, "library", "Library", v4(LIBRARY));
        addReviewSpace(reviewSpaces, "globe", "Book Cabinet and Globe Room", v4(GLOBE_BOOK_CABINET));
        addReviewSpace(reviewSpaces, "geography", "Geography Cabinet", v4(GEOGRAPHY_CABINET));
        addReviewSpace(reviewSpaces, "porcelain", "Porcelain Hall", v4(PORCELAIN_HALL));
        addReviewSpace(reviewSpaces, "marble", "Marble Salon", MARBLE_SALON);
        addReviewSpace(reviewSpaces, "gallery", "Gallery of Art", v4(GALLERY_OF_ART));
        addReviewSpace(reviewSpaces, "accounting", "Accounting Office", v4(ACCOUNTING_OFFICE));
        addReviewSpace(reviewSpaces, "music", "Music Chamber with Harpsichord", v4(MUSIC_CHAMBER));
        addReviewSpace(reviewSpaces, "blue", "Blue Salon", v4(BLUE_SALON));
        addReviewSpace(reviewSpaces, "banquet", "Banquet Hall", v4(BANQUET_HALL));
        root.add("reviewSpaces", reviewSpaces);

        JsonArray paletteJson = new JsonArray();
        palette.forEach(paletteJson::add);
        root.add("palette", paletteJson);
        root.add("blocks", blocks);
        return new Manifest(root, blockCount, palette.size(), hash);
    }

    private static OurosGrandPalaceBuildKit.Room v4(OurosGrandPalaceBuildKit.Room room) {
        return OurosGrandPalaceV4Plan.physical(room);
    }

    private static void addReviewSpace(JsonArray target, String id, String displayName,
                                       OurosGrandPalaceBuildKit.Room room) {
        JsonObject review = new JsonObject();
        review.addProperty("id", id);
        review.addProperty("name", displayName);
        review.add("min", vector(room.minX(), room.floorY(), room.minZ()));
        review.add("max", vector(room.maxX(), room.ceilingY(), room.maxZ()));
        review.add("focus", vector(room.centerX(), (room.floorY() + room.ceilingY()) / 2, room.centerZ()));
        target.add(review);
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
        result.add(x);
        result.add(y);
        result.add(z);
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