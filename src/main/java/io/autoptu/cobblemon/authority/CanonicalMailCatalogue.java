package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-authored RPG mail. Clients may select message IDs but never author content or rewards. */
public final class CanonicalMailCatalogue {
    public static final CanonicalMailCatalogue DEFAULT = new CanonicalMailCatalogue(List.of(
            new Mail(
                    "ouros-welcome",
                    "Ouros League Office",
                    "Welcome to Ouros",
                    "Your Trainer registration is active. This travel stipend is provided once to help you get started.",
                    new Reward("ouros_credit", 100L, "mail:ouros-welcome"))
    ));

    private final Map<String, Mail> messages;

    public CanonicalMailCatalogue(List<Mail> messages) {
        Objects.requireNonNull(messages, "messages");
        Map<String, Mail> indexed = new LinkedHashMap<>();
        for (Mail message : messages) {
            Objects.requireNonNull(message, "message");
            if (indexed.putIfAbsent(message.mailId(), message) != null) {
                throw new IllegalArgumentException("duplicate canonical mail: " + message.mailId());
            }
        }
        this.messages = Map.copyOf(indexed);
    }

    public List<Mail> messages() {
        return List.copyOf(messages.values());
    }

    public Optional<Mail> message(String mailId) {
        if (mailId == null || mailId.isBlank()) return Optional.empty();
        return Optional.ofNullable(messages.get(mailId.strip()));
    }

    public record Mail(String mailId, String sender, String subject, String body, Reward reward) {
        public Mail {
            mailId = requireText(mailId, "mailId");
            sender = requireText(sender, "sender");
            subject = requireText(subject, "subject");
            body = requireText(body, "body");
        }
    }

    public record Reward(String currencyId, long amount, String sourceId) {
        public Reward {
            currencyId = requireText(currencyId, "currencyId");
            sourceId = requireText(sourceId, "sourceId");
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
