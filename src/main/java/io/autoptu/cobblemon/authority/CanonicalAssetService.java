package io.autoptu.cobblemon.authority;

import java.util.Optional;
import java.util.function.Supplier;

public final class CanonicalAssetService {
    private final CanonicalAssetRepository repository;
    private final Supplier<String> reservationIds;

    public CanonicalAssetService(CanonicalAssetRepository repository, Supplier<String> reservationIds) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (reservationIds == null) {
            throw new IllegalArgumentException("reservationIds must not be null");
        }
        this.repository = repository;
        this.reservationIds = reservationIds;
    }

    public Optional<CanonicalPokemonState> findOwnedPokemon(String playerId, String pokemonId) {
        if (playerId == null || playerId.isBlank() || pokemonId == null || pokemonId.isBlank()) {
            return Optional.empty();
        }
        return repository.findPokemon(pokemonId)
                .filter(pokemon -> pokemon.ownerPlayerId().equals(playerId));
    }

    public ReservationDecision reserveItem(String playerId, String itemInstanceId, int quantity) {
        if (playerId == null || playerId.isBlank()
                || itemInstanceId == null || itemInstanceId.isBlank()
                || quantity <= 0) {
            return ReservationDecision.deny("invalid_request");
        }

        CanonicalItemInstance item = repository.findItem(itemInstanceId).orElse(null);
        if (item == null) {
            return ReservationDecision.deny("unknown_item");
        }
        if (!item.ownerPlayerId().equals(playerId)) {
            return ReservationDecision.deny("item_not_owned");
        }
        if (item.quantity() < quantity) {
            return ReservationDecision.deny("insufficient_quantity");
        }

        String reservationId = reservationIds.get();
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalStateException("reservation id supplier returned blank id");
        }

        ItemReservation reservation = new ItemReservation(
                reservationId,
                playerId,
                item.itemInstanceId(),
                item.templateId(),
                quantity,
                item.revision());

        if (!repository.tryReserveItem(reservation)) {
            return ReservationDecision.deny("state_changed_or_already_reserved");
        }
        return ReservationDecision.allow(reservation);
    }

    public ReservationDecision commitReservation(String playerId, String reservationId) {
        return finishReservation(playerId, reservationId, true);
    }

    public ReservationDecision releaseReservation(String playerId, String reservationId) {
        return finishReservation(playerId, reservationId, false);
    }

    private ReservationDecision finishReservation(String playerId, String reservationId, boolean commit) {
        if (playerId == null || playerId.isBlank() || reservationId == null || reservationId.isBlank()) {
            return ReservationDecision.deny("invalid_request");
        }

        ItemReservation reservation = repository.findReservation(reservationId).orElse(null);
        if (reservation == null) {
            return ReservationDecision.deny("unknown_reservation");
        }
        if (!reservation.playerId().equals(playerId)) {
            return ReservationDecision.deny("reservation_not_owned");
        }

        boolean changed = commit
                ? repository.commitItemReservation(reservationId, playerId)
                : repository.releaseItemReservation(reservationId, playerId);
        if (!changed) {
            return ReservationDecision.deny("reservation_conflict");
        }
        return ReservationDecision.allow(reservation);
    }
}
