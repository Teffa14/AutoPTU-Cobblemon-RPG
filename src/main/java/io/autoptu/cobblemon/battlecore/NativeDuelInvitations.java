package io.autoptu.cobblemon.battlecore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Consent bookkeeping only. Never owns a team, battle rules or a battle result. */
public final class NativeDuelInvitations {
    public record Invitation(UUID token, UUID challenger, UUID recipient, long expiresAt) {}
    private final Map<UUID, Invitation> byParticipant = new HashMap<>();
    private final Map<UUID, Long> nextChallenge = new HashMap<>();

    public synchronized Invitation invite(UUID challenger, UUID recipient, long now) {
        java.util.Objects.requireNonNull(challenger);
        java.util.Objects.requireNonNull(recipient);
        expire(now);
        if (challenger.equals(recipient)) throw new IllegalArgumentException("self challenge");
        if (byParticipant.containsKey(challenger) || byParticipant.containsKey(recipient)) {
            throw new IllegalStateException("participant already has a pending invitation");
        }
        if (nextChallenge.getOrDefault(challenger, Long.MIN_VALUE) > now) {
            throw new IllegalStateException("challenge cooldown");
        }
        var invitation = new Invitation(UUID.randomUUID(), challenger, recipient, Math.addExact(now, 60_000));
        byParticipant.put(challenger, invitation);
        byParticipant.put(recipient, invitation);
        nextChallenge.put(challenger, Math.addExact(now, 10_000));
        return invitation;
    }

    public synchronized Optional<Invitation> find(UUID participant, long now) {
        expire(now);
        return Optional.ofNullable(byParticipant.get(participant));
    }

    public synchronized Optional<Invitation> accept(UUID recipient, UUID token, long now) {
        expire(now);
        var invitation = byParticipant.get(recipient);
        if (invitation == null || !invitation.recipient().equals(recipient) || !invitation.token().equals(token)) {
            return Optional.empty();
        }
        remove(invitation);
        return Optional.of(invitation);
    }

    public synchronized Optional<Invitation> cancel(UUID participant) {
        var invitation = byParticipant.get(participant);
        if (invitation != null) remove(invitation);
        return Optional.ofNullable(invitation);
    }

    public synchronized void expire(long now) {
        byParticipant.values().removeIf(invitation -> now >= invitation.expiresAt());
        nextChallenge.values().removeIf(expiry -> now >= expiry);
    }

    public synchronized void clear() { byParticipant.clear(); nextChallenge.clear(); }

    private void remove(Invitation invitation) {
        byParticipant.remove(invitation.challenger(), invitation);
        byParticipant.remove(invitation.recipient(), invitation);
    }
}
