package io.autoptu.cobblemon.authority;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative restart-safe movement between the active bag and durable item storage. */
public final class CanonicalItemStorageTransferService {
    private final FileCanonicalItemReservationRepository bag;
    private final FileCanonicalItemStorageRepository storage;
    private final FileCanonicalItemStorageTransferRepository transfers;

    public CanonicalItemStorageTransferService(
            FileCanonicalItemReservationRepository bag,
            FileCanonicalItemStorageRepository storage,
            FileCanonicalItemStorageTransferRepository transfers
    ) {
        this.bag = Objects.requireNonNull(bag, "bag");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.transfers = Objects.requireNonNull(transfers, "transfers");
    }

    /** itemKey may be an exact owned stack id or an authored template id. */
    public TransferResult deposit(String transferId, String authenticatedPlayerId, String itemKey, int quantity) {
        String playerId = requireId("authenticatedPlayerId", authenticatedPlayerId);
        String id = requireId("transferId", transferId);
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        FileCanonicalItemStorageTransferRepository.TransferAttempt existing = transfers.find(id).orElse(null);
        if (existing != null) {
            requireRetry(existing, playerId, FileCanonicalItemStorageTransferRepository.Direction.DEPOSIT, quantity);
            return resume(existing);
        }
        CanonicalBagQueryService.ItemInspection inspection = new CanonicalBagQueryService(bag).inspectItem(playerId, itemKey);
        CanonicalBagQueryService.BagEntry source = inspection.entries().stream()
                .filter(entry -> entry.availableQuantity() >= quantity && !entry.transactionLocked())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no unreserved canonical bag stack has the requested quantity"));
        FileCanonicalItemStorageTransferRepository.TransferAttempt attempt = transfers.createIfAbsent(
                new FileCanonicalItemStorageTransferRepository.TransferAttempt(
                        id, playerId, FileCanonicalItemStorageTransferRepository.Direction.DEPOSIT,
                        source.itemInstanceId(), source.templateId(), quantity,
                        FileCanonicalItemStorageTransferRepository.Stage.CREATED));
        return resume(attempt);
    }

    /** Withdrawals select only a server-owned template id and quantity. */
    public TransferResult withdraw(String transferId, String authenticatedPlayerId, String templateId, int quantity) {
        String playerId = requireId("authenticatedPlayerId", authenticatedPlayerId);
        String id = requireId("transferId", transferId);
        String template = requireId("templateId", templateId);
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        FileCanonicalItemStorageTransferRepository.TransferAttempt existing = transfers.find(id).orElse(null);
        if (existing != null) {
            requireRetry(existing, playerId, FileCanonicalItemStorageTransferRepository.Direction.WITHDRAW, quantity);
            if (!existing.templateId().equals(template)) throw new IllegalStateException("transferId already belongs to a different item template");
            return resume(existing);
        }
        CanonicalItemStorageState state = storage.findOrCreate(playerId);
        if (state.quantity(template) < quantity) throw new IllegalStateException("insufficient canonical item storage quantity");
        String bagItemId = "storage-withdraw-" + UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
        FileCanonicalItemStorageTransferRepository.TransferAttempt attempt = transfers.createIfAbsent(
                new FileCanonicalItemStorageTransferRepository.TransferAttempt(
                        id, playerId, FileCanonicalItemStorageTransferRepository.Direction.WITHDRAW,
                        bagItemId, template, quantity, FileCanonicalItemStorageTransferRepository.Stage.CREATED));
        return resume(attempt);
    }

    public CanonicalItemStorageState inspect(String authenticatedPlayerId) {
        return storage.findOrCreate(requireId("authenticatedPlayerId", authenticatedPlayerId));
    }

    public List<TransferResult> recoverPending() {
        return transfers.findPending().stream().map(this::resume).toList();
    }

    private TransferResult resume(FileCanonicalItemStorageTransferRepository.TransferAttempt original) {
        FileCanonicalItemStorageTransferRepository.TransferAttempt attempt = requireAttempt(original.transferId());
        if (attempt.stage() == FileCanonicalItemStorageTransferRepository.Stage.CREATED) {
            if (attempt.direction() == FileCanonicalItemStorageTransferRepository.Direction.DEPOSIT) removeFromBag(attempt);
            else storage.applyDeltaOnce(attempt.playerId(), attempt.transferId(), attempt.templateId(), -attempt.quantity());
            advance(attempt, FileCanonicalItemStorageTransferRepository.Stage.SOURCE_REMOVED);
            attempt = requireAttempt(attempt.transferId());
        }
        if (attempt.stage() == FileCanonicalItemStorageTransferRepository.Stage.SOURCE_REMOVED) {
            if (attempt.direction() == FileCanonicalItemStorageTransferRepository.Direction.DEPOSIT) {
                storage.applyDeltaOnce(attempt.playerId(), attempt.transferId(), attempt.templateId(), attempt.quantity());
            } else {
                addToBag(attempt);
            }
            advance(attempt, FileCanonicalItemStorageTransferRepository.Stage.TARGET_ADDED);
            attempt = requireAttempt(attempt.transferId());
        }
        if (attempt.stage() == FileCanonicalItemStorageTransferRepository.Stage.TARGET_ADDED) {
            if (attempt.direction() == FileCanonicalItemStorageTransferRepository.Direction.DEPOSIT) {
                if (!bag.releaseConsumedReservationLock(reservationId(attempt), attempt.playerId())) {
                    throw new IllegalStateException("failed to release consumed bag transfer lock");
                }
            }
            advance(attempt, FileCanonicalItemStorageTransferRepository.Stage.COMMITTED);
            attempt = requireAttempt(attempt.transferId());
        }
        if (attempt.stage() != FileCanonicalItemStorageTransferRepository.Stage.COMMITTED) {
            throw new IllegalStateException("item storage transfer stopped at non-terminal stage " + attempt.stage());
        }
        return new TransferResult(attempt.direction(), attempt.templateId(), attempt.quantity(),
                attempt.bagItemInstanceId(), storage.findOrCreate(attempt.playerId()).revision());
    }

    private void removeFromBag(FileCanonicalItemStorageTransferRepository.TransferAttempt attempt) {
        String reservationId = reservationId(attempt);
        ItemReservation existing = bag.findReservation(reservationId).orElse(null);
        if (existing == null) {
            CanonicalItemInstance item = bag.findItem(attempt.bagItemInstanceId())
                    .orElseThrow(() -> new IllegalStateException("canonical bag stack disappeared before storage transfer"));
            if (!item.ownerPlayerId().equals(attempt.playerId()) || !item.templateId().equals(attempt.templateId())) {
                throw new IllegalStateException("canonical bag stack no longer matches transfer intent");
            }
            if (!bag.tryReserveItem(new ItemReservation(reservationId, attempt.playerId(), attempt.bagItemInstanceId(),
                    attempt.templateId(), attempt.quantity(), item.revision()))) {
                throw new IllegalStateException("canonical bag stack is no longer available for storage transfer");
            }
        } else if (!existing.playerId().equals(attempt.playerId())
                || !existing.itemInstanceId().equals(attempt.bagItemInstanceId())
                || !existing.itemTemplateId().equals(attempt.templateId())
                || existing.quantity() != attempt.quantity()) {
            throw new IllegalStateException("storage transfer reservation identity mismatch");
        }
        if (!bag.consumeReservationRetainingLock(reservationId, attempt.playerId())) {
            throw new IllegalStateException("failed to consume canonical bag quantity for storage transfer");
        }
    }

    private void addToBag(FileCanonicalItemStorageTransferRepository.TransferAttempt attempt) {
        CanonicalItemInstance target = new CanonicalItemInstance(attempt.bagItemInstanceId(), attempt.playerId(),
                attempt.templateId(), attempt.quantity(), 0L);
        if (bag.createItemIfAbsent(target)) return;
        CanonicalItemInstance existing = bag.findItem(attempt.bagItemInstanceId())
                .orElseThrow(() -> new IllegalStateException("withdraw target item disappeared"));
        if (!existing.equals(target)) throw new IllegalStateException("withdraw target item id collides with different canonical state");
    }

    private void advance(FileCanonicalItemStorageTransferRepository.TransferAttempt attempt,
                         FileCanonicalItemStorageTransferRepository.Stage next) {
        if (!transfers.advance(attempt.transferId(), attempt.stage(), next)) {
            FileCanonicalItemStorageTransferRepository.TransferAttempt current = requireAttempt(attempt.transferId());
            if (current.stage() != next) throw new IllegalStateException("item storage transfer journal changed unexpectedly");
        }
    }

    private FileCanonicalItemStorageTransferRepository.TransferAttempt requireAttempt(String transferId) {
        return transfers.find(transferId).orElseThrow(() -> new IllegalStateException("item storage transfer journal is missing"));
    }
    private static void requireRetry(FileCanonicalItemStorageTransferRepository.TransferAttempt attempt, String playerId,
                                     FileCanonicalItemStorageTransferRepository.Direction direction, int quantity) {
        if (!attempt.playerId().equals(playerId) || attempt.direction() != direction || attempt.quantity() != quantity) {
            throw new IllegalStateException("transferId already belongs to a different immutable transfer intent");
        }
    }
    private static String reservationId(FileCanonicalItemStorageTransferRepository.TransferAttempt attempt) {
        return "item-storage:" + attempt.transferId();
    }
    private static String requireId(String label, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.strip();
    }

    public record TransferResult(FileCanonicalItemStorageTransferRepository.Direction direction, String templateId,
                                 int quantity, String bagItemInstanceId, long storageRevision) {}
}
