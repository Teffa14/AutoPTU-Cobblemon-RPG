package io.autoptu.cobblemon.authority;

import java.util.Optional;

public interface CanonicalStateRepository {
    Optional<CanonicalPlayerState> findPlayer(String playerId);
}
