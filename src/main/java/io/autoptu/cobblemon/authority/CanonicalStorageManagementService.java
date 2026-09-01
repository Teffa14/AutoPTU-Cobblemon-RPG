package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Server-owned preflight for Minecraft bag/storage mutations. */
public final class CanonicalStorageManagementService {

    public Decision canManage(Request request) {
        Objects.requireNonNull(request, "request");
        String playerId = request.playerId() == null ? "" : request.playerId().trim();
        if (playerId.isEmpty()) return Decision.denied("canonical player id is required");
        if (!request.canonicalTrainerExists()) return Decision.denied("canonical Trainer is not provisioned");
        if (request.mutation() == null) return Decision.denied("storage mutation is required");
        if (request.quantity() <= 0) return Decision.denied("storage quantity must be positive");
        if (request.templateId() == null || request.templateId().isBlank()) return Decision.denied("item template identity is required");
        String templateId = request.templateId().trim();

        if (request.mutation() == Mutation.DEPOSIT) {
            CanonicalBagQueryService.BagSnapshot bag = request.currentBag();
            if (bag == null || !bag.playerId().equals(playerId)) return Decision.denied("bag owner does not match authenticated player");
            if (request.itemInstanceId() == null || request.itemInstanceId().isBlank()) return Decision.denied("bag item identity is required");
            String itemId = request.itemInstanceId().trim();
            CanonicalBagQueryService.BagEntry entry = bag.entries().stream()
                    .filter(candidate -> candidate.itemInstanceId().equals(itemId)).findFirst().orElse(null);
            if (entry == null) return Decision.denied("bag item is no longer owned by authenticated player");
            if (!entry.templateId().equals(templateId)) return Decision.denied("bag item template identity changed on the server");
            if (entry.revision() != request.expectedItemRevision()) return Decision.denied("bag item revision changed on the server");
            if (entry.availableQuantity() != request.expectedAvailableQuantity()) return Decision.denied("bag item quantity changed on the server");
            if (entry.transactionLocked()) return Decision.denied("bag item is transaction locked");
            if (entry.availableQuantity() < request.quantity()) return Decision.denied("bag item quantity is insufficient");
            return Decision.allowed(Mutation.DEPOSIT, itemId, templateId, request.quantity(), entry.revision(), -1L);
        }

        CanonicalItemStorageState storage = request.currentStorage();
        if (storage == null || !storage.playerId().equals(playerId)) return Decision.denied("storage owner does not match authenticated player");
        if (storage.revision() != request.expectedStorageRevision()) return Decision.denied("storage revision changed on the server");
        if (storage.quantity(templateId) != request.expectedStoredQuantity()) return Decision.denied("stored item quantity changed on the server");
        if (storage.quantity(templateId) < request.quantity()) return Decision.denied("stored item quantity is insufficient");
        return Decision.allowed(Mutation.WITHDRAW, null, templateId, request.quantity(), -1L, storage.revision());
    }

    public enum Mutation { DEPOSIT, WITHDRAW }

    public record Request(String playerId, boolean canonicalTrainerExists, Mutation mutation, String itemInstanceId,
                          String templateId, int quantity, long expectedItemRevision, int expectedAvailableQuantity,
                          long expectedStorageRevision, int expectedStoredQuantity,
                          CanonicalBagQueryService.BagSnapshot currentBag, CanonicalItemStorageState currentStorage) {}

    public record Decision(boolean allowed, Mutation mutation, String itemInstanceId, String templateId, int quantity,
                           long itemRevision, long storageRevision, String reason) {
        public static Decision allowed(Mutation mutation, String itemInstanceId, String templateId, int quantity,
                                       long itemRevision, long storageRevision) {
            return new Decision(true, mutation, itemInstanceId, templateId, quantity, itemRevision, storageRevision, "allowed");
        }
        public static Decision denied(String reason) {
            return new Decision(false, null, null, null, 0, -1L, -1L, reason);
        }
    }
}
