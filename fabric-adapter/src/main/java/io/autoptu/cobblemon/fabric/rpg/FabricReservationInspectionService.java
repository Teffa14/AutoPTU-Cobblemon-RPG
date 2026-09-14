package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Objects;

/**
 * Reusable read-only projection of server-owned item reservation state.
 *
 * <p>The authenticated Minecraft UUID is the only identity input. Persistent player, item-instance,
 * reservation and transaction identifiers are deliberately omitted from the returned DTOs. This
 * service never creates or repairs inventory state and never evaluates PTU item legality/effects,
 * battle outcomes, RNG, damage, statuses or action economy.</p>
 */
public final class FabricReservationInspectionService {
    private FabricReservationInspectionService() {
    }

    public static InspectionResult inspect(MinecraftServer server, ServerPlayerEntity target) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(target, "target");

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        String playerName = target.getGameProfile().getName();
        try {
            var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(server);
            if (playerRepository.findPlayer(playerId).isEmpty()) {
                return InspectionResult.failure(playerName, "CANONICAL_TRAINER_MISSING");
            }

            CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(server))
                    .inspect(playerId);

            List<ReservationView> reservations = bag.entries().stream()
                    .filter(CanonicalBagQueryService.BagEntry::transactionLocked)
                    .map(entry -> new ReservationView(
                            entry.templateId(),
                            entry.reservedQuantity(),
                            entry.quantity(),
                            entry.availableQuantity(),
                            entry.reservationConsumed(),
                            entry.revision()))
                    .toList();

            return InspectionResult.success(
                    playerName,
                    bag.totalReserved(),
                    bag.transactionLocks(),
                    reservations);
        } catch (RuntimeException inconsistentState) {
            return InspectionResult.failure(playerName, "REPOSITORY_CONSISTENCY_READ_FAILED");
        }
    }

    public record InspectionResult(
            String playerName,
            long totalReserved,
            long transactionLocks,
            List<ReservationView> reservations,
            String errorCode
    ) {
        private static InspectionResult success(
                String playerName,
                long totalReserved,
                long transactionLocks,
                List<ReservationView> reservations
        ) {
            return new InspectionResult(
                    playerName,
                    totalReserved,
                    transactionLocks,
                    List.copyOf(reservations),
                    null);
        }

        private static InspectionResult failure(String playerName, String errorCode) {
            return new InspectionResult(playerName, 0, 0, List.of(), errorCode);
        }

        public boolean success() {
            return errorCode == null;
        }
    }

    public record ReservationView(
            String templateId,
            long reservedQuantity,
            long stackQuantity,
            long availableQuantity,
            boolean consumed,
            long itemRevision
    ) {
    }
}
