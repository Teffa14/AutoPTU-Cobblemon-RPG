package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.battlecore.BattleMatchReport;
import io.autoptu.cobblemon.battlecore.BattleMatchReportCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/** World-scoped last practice report, independent of canonical campaign storage. */
public final class FabricBattleReportStore {
    private FabricBattleReportStore() {}

    private static Path file(MinecraftServer server, UUID playerId) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("autoptu")
                .resolve("practice-reports").resolve(playerId + ".bin");
    }

    public static Optional<BattleMatchReport> load(MinecraftServer server, UUID playerId) throws IOException {
        Path target = file(server, playerId);
        if (!Files.exists(target)) return Optional.empty();
        if (Files.size(target) > BattleMatchReportCodec.MAX_BYTES) throw new IOException("practice report exceeds size limit");
        try {
            return Optional.of(BattleMatchReportCodec.decode(Files.readAllBytes(target)));
        } catch (IllegalArgumentException corrupt) {
            throw new IOException("practice report is corrupt or from an unsupported version", corrupt);
        }
    }

    public static void save(MinecraftServer server, UUID playerId, BattleMatchReport report) throws IOException {
        byte[] bytes = BattleMatchReportCodec.encode(report);
        Path target = file(server, playerId);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), playerId + "-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
