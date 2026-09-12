package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only structural validation for the RPG state owned by this integration.
 *
 * <p>This service checks identity and aggregate consistency only. It deliberately does not validate
 * PTU legality, combat calculations, item effects, statuses, moves, abilities, progression rules,
 * capture outcomes, or any other battle-engine concern.</p>
 */
public final class CanonicalPlayerStateValidationService {

    public ValidationReport validate(
            String expectedPlayerId,
            CanonicalTrainerSummaryService.Summary trainer,
            CanonicalPartySummary party,
            CanonicalBagQueryService.BagSnapshot bag,
            Optional<FileCanonicalTrainerProgressionRepository.ProgressionState> progression
    ) {
        if (expectedPlayerId == null || expectedPlayerId.isBlank()) {
            throw new IllegalArgumentException("expectedPlayerId must not be blank");
        }
        if (trainer == null) throw new IllegalArgumentException("trainer is required");
        if (bag == null) throw new IllegalArgumentException("bag is required");
        progression = progression == null ? Optional.empty() : progression;

        String playerId = expectedPlayerId.trim();
        ArrayList<Issue> issues = new ArrayList<>();

        if (!playerId.equals(trainer.playerId())) {
            issues.add(error("TRAINER_OWNER_MISMATCH", "Trainer projection belongs to " + trainer.playerId()));
        }

        if (party != null) {
            if (!playerId.equals(party.playerId())) {
                issues.add(error("PARTY_OWNER_MISMATCH", "Party projection belongs to " + party.playerId()));
            }
            validateParty(party, issues);
        }

        if (!playerId.equals(bag.playerId())) {
            issues.add(error("BAG_OWNER_MISMATCH", "Bag projection belongs to " + bag.playerId()));
        }
        validateBag(bag, issues);

        if (progression.isEmpty()) {
            issues.add(warning("PROGRESSION_MISSING", "No persisted Trainer progression record exists yet"));
        } else if (!playerId.equals(progression.get().playerId())) {
            issues.add(error(
                    "PROGRESSION_OWNER_MISMATCH",
                    "Trainer progression belongs to " + progression.get().playerId()));
        }

        return new ValidationReport(playerId, issues);
    }

    private static void validateParty(CanonicalPartySummary party, List<Issue> issues) {
        Set<Integer> slots = new HashSet<>();
        Set<String> pokemonIds = new HashSet<>();
        for (CanonicalPartySummary.Member member : party.members()) {
            if (!slots.add(member.slot())) {
                issues.add(error("PARTY_DUPLICATE_SLOT", "Party slot " + member.slot() + " appears more than once"));
            }
            if (!pokemonIds.add(member.pokemonId())) {
                issues.add(error(
                        "PARTY_DUPLICATE_POKEMON",
                        "Pokemon " + member.pokemonId() + " appears in more than one party slot"));
            }
        }
    }

    private static void validateBag(CanonicalBagQueryService.BagSnapshot bag, List<Issue> issues) {
        Set<String> itemIds = new HashSet<>();
        int quantity = 0;
        int available = 0;
        int reserved = 0;
        int locks = 0;
        for (CanonicalBagQueryService.BagEntry entry : bag.entries()) {
            if (!itemIds.add(entry.itemInstanceId())) {
                issues.add(error(
                        "BAG_DUPLICATE_ITEM_INSTANCE",
                        "Item instance " + entry.itemInstanceId() + " appears more than once"));
            }
            quantity += entry.quantity();
            available += entry.availableQuantity();
            reserved += entry.reservedQuantity();
            if (entry.transactionLocked()) locks++;
        }

        if (quantity != bag.totalQuantity()) {
            issues.add(error("BAG_TOTAL_QUANTITY_MISMATCH", "Bag quantity total does not match its entries"));
        }
        if (available != bag.totalAvailable()) {
            issues.add(error("BAG_TOTAL_AVAILABLE_MISMATCH", "Bag available total does not match its entries"));
        }
        if (reserved != bag.totalReserved()) {
            issues.add(error("BAG_TOTAL_RESERVED_MISMATCH", "Bag reserved total does not match its entries"));
        }
        if (locks != bag.transactionLocks()) {
            issues.add(error("BAG_LOCK_COUNT_MISMATCH", "Bag transaction-lock count does not match its entries"));
        }
    }

    private static Issue error(String code, String message) {
        return new Issue(Severity.ERROR, code, message);
    }

    private static Issue warning(String code, String message) {
        return new Issue(Severity.WARNING, code, message);
    }

    public enum Severity {
        WARNING,
        ERROR
    }

    public record Issue(Severity severity, String code, String message) {
        public Issue {
            if (severity == null) throw new IllegalArgumentException("severity is required");
            if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
            if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
            code = code.trim();
            message = message.trim();
        }
    }

    public record ValidationReport(String playerId, List<Issue> issues) {
        public ValidationReport {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
            playerId = playerId.trim();
            issues = List.copyOf(issues == null ? List.of() : issues);
        }

        public boolean valid() {
            return issues.stream().noneMatch(issue -> issue.severity() == Severity.ERROR);
        }

        public long errorCount() {
            return issues.stream().filter(issue -> issue.severity() == Severity.ERROR).count();
        }

        public long warningCount() {
            return issues.stream().filter(issue -> issue.severity() == Severity.WARNING).count();
        }
    }
}
