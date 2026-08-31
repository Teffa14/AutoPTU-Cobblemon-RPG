package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-authored quest reward definitions. Clients never provide reward type, amount, or eligibility. */
public final class CanonicalQuestRewardCatalogue {
    public static final CanonicalQuestRewardCatalogue DEFAULT = new CanonicalQuestRewardCatalogue(List.of(
            new Reward("cedar-field-notes", "ouros_credit", 240L, "quest:cedar-field-notes")
    ));

    private final Map<String, Reward> rewards;

    public CanonicalQuestRewardCatalogue(List<Reward> rewards) {
        Objects.requireNonNull(rewards, "rewards");
        Map<String, Reward> indexed = new LinkedHashMap<>();
        for (Reward reward : rewards) {
            Objects.requireNonNull(reward, "reward");
            if (indexed.putIfAbsent(reward.questId(), reward) != null) {
                throw new IllegalArgumentException("duplicate quest reward: " + reward.questId());
            }
        }
        this.rewards = Map.copyOf(indexed);
    }

    public Optional<Reward> reward(String questId) {
        if (questId == null || questId.isBlank()) return Optional.empty();
        return Optional.ofNullable(rewards.get(questId.strip()));
    }

    public record Reward(String questId, String currencyId, long amount, String sourceId) {
        public Reward {
            questId = requireText(questId, "questId");
            currencyId = requireText(currencyId, "currencyId");
            sourceId = requireText(sourceId, "sourceId");
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        }

        public String transactionId() {
            return "quest-reward:" + questId;
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
