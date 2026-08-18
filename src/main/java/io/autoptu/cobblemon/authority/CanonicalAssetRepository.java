package io.autoptu.cobblemon.authority;

import java.util.Optional;

public interface CanonicalAssetRepository {
    Optional<CanonicalPokemonState> findPokemon(String pokemonId);

    Optional<CanonicalItemInstance> findItem(String itemInstanceId);

    Optional<ItemReservation> findReservation(String reservationId);

    boolean tryReserveItem(ItemReservation reservation);

    boolean commitItemReservation(String reservationId, String playerId);

    boolean releaseItemReservation(String reservationId, String playerId);
}
