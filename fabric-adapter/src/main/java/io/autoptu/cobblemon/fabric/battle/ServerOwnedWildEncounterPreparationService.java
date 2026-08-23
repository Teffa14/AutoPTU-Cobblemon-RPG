package io.autoptu.cobblemon.fabric.battle;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves trusted RPG encounter data before attaching an opaque Cobblemon WILD actor correlation.
 *
 * The canonical blueprint source never receives the external actor identifier. That identifier is
 * supplied only after canonical RPG values have been resolved, then passed to the existing provisioner
 * as presentation correlation data.
 */
public final class ServerOwnedWildEncounterPreparationService {
    private final CanonicalWildEncounterBlueprintSource blueprintSource;
    private final ServerOwnedWildEncounterProvisioningService provisioningService;

    public ServerOwnedWildEncounterPreparationService(
            CanonicalWildEncounterBlueprintSource blueprintSource,
            ServerOwnedWildEncounterProvisioningService provisioningService
    ) {
        this.blueprintSource = Objects.requireNonNull(blueprintSource, "blueprintSource");
        this.provisioningService = Objects.requireNonNull(provisioningService, "provisioningService");
    }

    public Optional<ServerOwnedWildEncounterProvisioningService.ProvisionedWildEncounter> prepare(
            String canonicalEncounterId,
            String externalWildActorId
    ) {
        String encounterId = requireId(canonicalEncounterId, "canonicalEncounterId");
        String actorId = requireId(externalWildActorId, "externalWildActorId");

        Optional<CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint> resolved =
                blueprintSource.resolve(encounterId);
        if (resolved.isEmpty()) return Optional.empty();

        CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint blueprint = resolved.get();
        if (!encounterId.equals(blueprint.canonicalEncounterId())) {
            throw new IllegalStateException("blueprint source returned a different canonical encounter id");
        }

        return Optional.of(provisioningService.provision(
                encounterId,
                actorId,
                blueprint.side(),
                blueprint.pokemon()
        ));
    }

    public ServerOwnedWildEncounterProvisioningService provisioningService() {
        return provisioningService;
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
