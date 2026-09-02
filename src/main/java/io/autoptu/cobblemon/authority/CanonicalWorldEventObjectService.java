package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Server authority boundary for durable authored world-event object activation. */
public final class CanonicalWorldEventObjectService {
    public static final String SHRINE_EVENT_KEY = "ouros_shrine_awakened";
    public static final String SWITCH_EVENT_KEY = "ouros_switch_latched";
    public static final String DOOR_EVENT_KEY = "ouros_door_opened";

    private final CanonicalStateRepository players;
    private final FileCanonicalWorldEventObjectRepository events;

    public CanonicalWorldEventObjectService(
            CanonicalStateRepository players,
            FileCanonicalWorldEventObjectRepository events
    ) {
        this.players = Objects.requireNonNull(players, "players");
        this.events = Objects.requireNonNull(events, "events");
    }

    public Decision activateShrine(String playerId, String objectId) {
        return activate(playerId, objectId, SHRINE_EVENT_KEY, "world event activated", "world event already active");
    }

    public Decision activateSwitch(String playerId, String objectId) {
        return activate(playerId, objectId, SWITCH_EVENT_KEY, "world switch latched", "world switch already latched");
    }

    public Decision activateDoor(String playerId, String objectId) {
        return activate(playerId, objectId, DOOR_EVENT_KEY, "world door opened", "world door already open");
    }

    private Decision activate(
            String playerId,
            String objectId,
            String eventKey,
            String activatedDetail,
            String alreadyActiveDetail
    ) {
        if (playerId == null || playerId.isBlank()) return Decision.rejected("playerId is required");
        if (objectId == null || objectId.isBlank()) return Decision.rejected("objectId is required");
        String owner = playerId.trim();
        String object = objectId.trim();
        if (players.findPlayer(owner).isEmpty()) return Decision.rejected("canonical Trainer is not loaded");

        var persisted = events.find(object);
        if (persisted.isPresent() && !eventKey.equals(persisted.orElseThrow().eventKey())) {
            return Decision.rejected("world object already belongs to another canonical event");
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            var current = events.findOrCreate(object, eventKey);
            var result = events.activate(object, eventKey, current.revision());
            if (result.status() == FileCanonicalWorldEventObjectRepository.Status.ACTIVATED) {
                return new Decision(true, true, result.state(), activatedDetail);
            }
            if (result.status() == FileCanonicalWorldEventObjectRepository.Status.ALREADY_ACTIVE) {
                return new Decision(true, false, result.state(), alreadyActiveDetail);
            }
        }
        return Decision.rejected("world event object changed concurrently");
    }

    public record Decision(
            boolean allowed,
            boolean newlyActivated,
            FileCanonicalWorldEventObjectRepository.State state,
            String detail
    ) {
        public Decision {
            if (detail == null || detail.isBlank()) throw new IllegalArgumentException("detail is required");
        }

        public static Decision rejected(String detail) {
            return new Decision(false, false, null, detail);
        }
    }
}
