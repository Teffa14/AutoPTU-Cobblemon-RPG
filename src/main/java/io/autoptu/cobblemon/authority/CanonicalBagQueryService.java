package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;

/** Read-only canonical bag projection. It never decides item legality or effects. */
public final class CanonicalBagQueryService {
    private final FileCanonicalItemReservationRepository repository;

    public CanonicalBagQueryService(FileCanonicalItemReservationRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.repository = repository;
    }

    public BagSnapshot inspect(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }

        ArrayList<BagEntry> entries = new ArrayList<>();
        int totalQuantity = 0;
        int totalAvailable = 0;
        int totalReserved = 0;
        int transactionLocks = 0;

        for (FileCanonicalItemReservationRepository.InventoryEntry stored
                : repository.findOwnedInventory(playerId)) {
            CanonicalItemInstance item = stored.item();
            ItemReservation reservation = stored.reservation();
            int reserved = stored.reservedQuantity();
            int available = stored.availableQuantity();
            if (stored.transactionLocked()) transactionLocks++;

            entries.add(new BagEntry(
                    item.itemInstanceId(),
                    item.templateId(),
                    item.quantity(),
                    available,
                    reserved,
                    reservation == null ? null : reservation.reservationId(),
                    stored.reservationConsumed(),
                    item.revision()
            ));
            totalQuantity += item.quantity();
            totalAvailable += available;
            totalReserved += reserved;
        }

        return new BagSnapshot(
                playerId.trim(),
                List.copyOf(entries),
                totalQuantity,
                totalAvailable,
                totalReserved,
                transactionLocks
        );
    }

    public record BagSnapshot(
            String playerId,
            List<BagEntry> entries,
            int totalQuantity,
            int totalAvailable,
            int totalReserved,
            int transactionLocks
    ) {
        public BagSnapshot {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
            entries = entries == null ? List.of() : List.copyOf(entries);
            if (totalQuantity < 0 || totalAvailable < 0 || totalReserved < 0 || transactionLocks < 0) {
                throw new IllegalArgumentException("bag totals must not be negative");
            }
        }
    }

    public record BagEntry(
            String itemInstanceId,
            String templateId,
            int quantity,
            int availableQuantity,
            int reservedQuantity,
            String reservationId,
            boolean reservationConsumed,
            long revision
    ) {
        public BagEntry {
            if (itemInstanceId == null || itemInstanceId.isBlank()) throw new IllegalArgumentException("itemInstanceId must not be blank");
            if (templateId == null || templateId.isBlank()) throw new IllegalArgumentException("templateId must not be blank");
            if (quantity < 0 || availableQuantity < 0 || reservedQuantity < 0) {
                throw new IllegalArgumentException("bag quantities must not be negative");
            }
            if (availableQuantity + reservedQuantity > quantity) {
                throw new IllegalArgumentException("available plus reserved cannot exceed quantity");
            }
            if (reservationConsumed && (reservationId == null || reservationId.isBlank())) {
                throw new IllegalArgumentException("consumed reservation requires reservationId");
            }
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }

        public boolean transactionLocked() {
            return reservationId != null && !reservationId.isBlank();
        }
    }
}
