package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalFastTravelServiceTest {
    private final CanonicalFastTravelService service = new CanonicalFastTravelService(25.0D);

    @Test
    void allowsOnlyObservedNearbyServerAuthoredTravel() {
        var allowed = service.canTravel(new CanonicalFastTravelService.Request(
                "player:test", true, "minecraft:overworld:1:64:1", true, 4.0D,
                "overworld_spawn", true, true));
        assertTrue(allowed.allowed());

        var missingSource = service.canTravel(new CanonicalFastTravelService.Request(
                "player:test", true, "minecraft:overworld:1:64:1", false, 4.0D,
                "overworld_spawn", true, true));
        assertFalse(missingSource.allowed());

        var clientInventedDestination = service.canTravel(new CanonicalFastTravelService.Request(
                "player:test", true, "minecraft:overworld:1:64:1", true, 4.0D,
                "client_coords", false, true));
        assertFalse(clientInventedDestination.allowed());
    }

    @Test
    void rejectsMissingTrainerRangeAndUnavailableDestination() {
        assertFalse(service.canTravel(new CanonicalFastTravelService.Request(
                "player:test", false, "source", true, 1.0D, "spawn", true, true)).allowed());
        assertFalse(service.canTravel(new CanonicalFastTravelService.Request(
                "player:test", true, "source", true, 26.0D, "spawn", true, true)).allowed());
        assertFalse(service.canTravel(new CanonicalFastTravelService.Request(
                "player:test", true, "source", true, 1.0D, "spawn", true, false)).allowed());
    }
}
