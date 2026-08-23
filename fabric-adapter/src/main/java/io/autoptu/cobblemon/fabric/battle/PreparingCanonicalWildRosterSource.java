package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.Optional;

/**
 * Claim-time adapter that materializes an already-published canonical WILD blueprint after trusted
 * server code has correlated that encounter with an opaque Cobblemon actor ID.
 *
 * Cobblemon contributes only battle-side and external actor identity. The canonical encounter ID
 * comes from the server-owned correlation registry, and every PTU value comes from the blueprint
 * source used by {@link ServerOwnedWildEncounterPreparationService}.
 */
public final class PreparingCanonicalWildRosterSource
        implements ServerOwnedWildEncounterIdentityBinder.CanonicalWildRosterSource {

    private final WorldScopedWildEncounterCorrelationRegistry correlations;
    private final ServerOwnedWildEncounterPreparationService preparationService;

    public PreparingCanonicalWildRosterSource(
            WorldScopedWildEncounterCorrelationRegistry correlations,
            ServerOwnedWildEncounterPreparationService preparationService
    ) {
        this.correlations = Objects.requireNonNull(correlations, "correlations");
        this.preparationService = Objects.requireNonNull(preparationService, "preparationService");
    }

    public static PreparingCanonicalWildRosterSource fromWorldRuntime(
            MinecraftServer server,
            ServerOwnedWildEncounterPreparationService preparationService
    ) {
        Objects.requireNonNull(server, "server");
        return new PreparingCanonicalWildRosterSource(
                FabricCanonicalPlayerStoreRuntime.requireWildEncounterCorrelationRegistry(server),
                preparationService
        );
    }

    @Override
    public Optional<ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster> resolve(
            String cobblemonBattleId,
            int side,
            String externalWildActorId
    ) {
        if (cobblemonBattleId == null || cobblemonBattleId.isBlank()
                || side < 0
                || externalWildActorId == null || externalWildActorId.isBlank()) {
            return Optional.empty();
        }
        String actorId = externalWildActorId.strip();
        Optional<String> encounterId = correlations.resolveCanonicalEncounterId(actorId);
        if (encounterId.isEmpty()) return Optional.empty();

        ServerOwnedWildEncounterProvisioningService provisioner = preparationService.provisioningService();
        Optional<ServerOwnedWildEncounterProvisioningService.ProvisionedWildEncounter> existing =
                provisioner.findByExternalActor(actorId);
        if (existing.isPresent()) {
            ServerOwnedWildEncounterProvisioningService.ProvisionedWildEncounter provisioned = existing.get();
            if (!provisioned.canonicalEncounterId().equals(encounterId.get()) || provisioned.side() != side) {
                return Optional.empty();
            }
            return Optional.of(provisioned.roster());
        }

        Optional<ServerOwnedWildEncounterProvisioningService.ProvisionedWildEncounter> prepared =
                preparationService.prepare(encounterId.get(), actorId);
        if (prepared.isEmpty()) return Optional.empty();
        ServerOwnedWildEncounterProvisioningService.ProvisionedWildEncounter provisioned = prepared.get();
        if (provisioned.side() != side) {
            provisioner.release(actorId);
            return Optional.empty();
        }
        return Optional.of(provisioned.roster());
    }
}
