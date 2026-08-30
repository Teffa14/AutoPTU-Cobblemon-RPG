package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Reusable server authority boundary for persistent authored-location discovery. */
public final class CanonicalLocationDiscoveryService {
    private final CanonicalLocationCatalogue catalogue;
    private final CanonicalStateRepository players;
    private final FileCanonicalLocationDiscoveryRepository discoveries;

    public CanonicalLocationDiscoveryService(
            CanonicalLocationCatalogue catalogue,
            CanonicalStateRepository players,
            FileCanonicalLocationDiscoveryRepository discoveries
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.players = Objects.requireNonNull(players, "players");
        this.discoveries = Objects.requireNonNull(discoveries, "discoveries");
    }

    public Decision observe(String playerId, String locationId) {
        if (playerId == null || playerId.isBlank()) return Decision.rejected("playerId is required");
        String owner = playerId.trim();
        if (players.findPlayer(owner).isEmpty()) return Decision.rejected("canonical Trainer is not loaded");
        var location = catalogue.location(locationId).orElse(null);
        if (location == null) return Decision.rejected("location is not server-authored");
        for (int attempt = 0; attempt < 2; attempt++) {
            var current = discoveries.findOrCreate(owner);
            var result = discoveries.discover(owner, location.id(), current.revision());
            if (result.status() == FileCanonicalLocationDiscoveryRepository.Status.DISCOVERED) {
                return new Decision(true, true, location, result.state().revision(), "discovered");
            }
            if (result.status() == FileCanonicalLocationDiscoveryRepository.Status.ALREADY_DISCOVERED) {
                return new Decision(true, false, location, result.state().revision(), "already discovered");
            }
        }
        return Decision.rejected("location discovery changed concurrently");
    }

    public record Decision(
            boolean allowed,
            boolean newlyDiscovered,
            CanonicalLocationCatalogue.Location location,
            long revision,
            String detail
    ) {
        public Decision {
            if (revision < -1L) throw new IllegalArgumentException("revision is invalid");
            if (detail == null || detail.isBlank()) throw new IllegalArgumentException("detail is required");
        }

        public static Decision rejected(String detail) {
            return new Decision(false, false, null, -1L, detail);
        }
    }
}
