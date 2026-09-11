package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned preflight for Minecraft-facing item-use requests.
 *
 * <p>This boundary resolves canonical Trainer and item ownership directly from server repositories,
 * rejects unavailable or transaction-locked stacks, and requires the target/context to have been
 * observed by the server. It deliberately does not decide PTU item effects, targeting legality,
 * action economy, RNG, healing, capture, damage, statuses, rewards, or battle outcomes. A successful
 * decision only means that the canonical stack may enter a more specific authoritative use flow.</p>
 */
public final class CanonicalItemUseService {
    private final CanonicalStateRepository playerRepository;
    private final FileCanonicalItemReservationRepository itemRepository;

    public CanonicalItemUseService(
            CanonicalStateRepository playerRepository,
            FileCanonicalItemReservationRepository itemRepository
    ) {
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository");
    }

    public Decision canUse(Request request) {
        Objects.requireNonNull(request, "request");
        String playerId = normalized(request.playerId());
        if (playerId == null) return Decision.denied("canonical player id is required");
        if (playerRepository.findPlayer(playerId).isEmpty()) {
            return Decision.denied("canonical Trainer is not provisioned");
        }

        String itemInstanceId = normalized(request.itemInstanceId());
        if (itemInstanceId == null) return Decision.denied("canonical item instance id is required");
        String targetId = normalized(request.targetId());
        if (targetId == null) return Decision.denied("server item-use target is required");
        String contextId = normalized(request.contextId());
        if (contextId == null) return Decision.denied("server item-use context is required");
        if (!request.serverObservedTarget()) {
            return Decision.denied("item-use target was not observed by the server");
        }
        if (!request.serverObservedContext()) {
            return Decision.denied("item-use context was not observed by the server");
        }

        CanonicalItemInstance item = itemRepository.findItem(itemInstanceId).orElse(null);
        if (item == null) return Decision.denied("canonical item instance does not exist");
        if (!item.ownerPlayerId().equals(playerId)) {
            return Decision.denied("canonical item is not owned by this Trainer");
        }
        if (item.quantity() <= 0) return Decision.denied("canonical item stack is empty");

        FileCanonicalItemReservationRepository.InventoryEntry inventoryEntry = itemRepository
                .findOwnedInventory(playerId)
                .stream()
                .filter(entry -> entry.item().itemInstanceId().equals(itemInstanceId))
                .findFirst()
                .orElse(null);
        if (inventoryEntry == null || inventoryEntry.availableQuantity() <= 0) {
            return Decision.denied("canonical item has no available quantity");
        }
        if (inventoryEntry.transactionLocked()) {
            return Decision.denied("canonical item is locked by an authoritative transaction");
        }

        return Decision.allowed(
                item.itemInstanceId(),
                item.templateId(),
                targetId,
                contextId,
                inventoryEntry.availableQuantity()
        );
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    public record Request(
            String playerId,
            String itemInstanceId,
            String targetId,
            String contextId,
            boolean serverObservedTarget,
            boolean serverObservedContext
    ) {}

    public record Decision(
            boolean allowed,
            String itemInstanceId,
            String itemTemplateId,
            String targetId,
            String contextId,
            int availableQuantity,
            String reason
    ) {
        public static Decision allowed(
                String itemInstanceId,
                String itemTemplateId,
                String targetId,
                String contextId,
                int availableQuantity
        ) {
            return new Decision(
                    true,
                    itemInstanceId,
                    itemTemplateId,
                    targetId,
                    contextId,
                    availableQuantity,
                    "allowed"
            );
        }

        public static Decision denied(String reason) {
            return new Decision(false, null, null, null, null, 0, reason);
        }
    }
}
