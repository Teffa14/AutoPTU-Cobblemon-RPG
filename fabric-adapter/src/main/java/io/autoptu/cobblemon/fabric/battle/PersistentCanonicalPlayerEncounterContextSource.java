package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalStateRepository;
import io.autoptu.cobblemon.authority.VersionedCanonicalPlayerEncounterProfileRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds the player encounter context from server-owned canonical identity and durable selections.
 *
 * The authenticated Minecraft UUID is used only to resolve a canonical participant identity through
 * the adapter identity registry. Pokemon/item ownership and quantities are deliberately revalidated
 * later by BattleAuthorityService when the reservation is attempted.
 */
public final class PersistentCanonicalPlayerEncounterContextSource
        implements FabricAuthenticatedPlayerContextResolver.CanonicalPlayerEncounterContextSource {
    private final CobblemonCanonicalEncounterIdentityRegistry identityRegistry;
    private final CanonicalStateRepository playerRepository;
    private final VersionedCanonicalPlayerEncounterProfileRepository profileRepository;

    public PersistentCanonicalPlayerEncounterContextSource(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            CanonicalStateRepository playerRepository,
            VersionedCanonicalPlayerEncounterProfileRepository profileRepository
    ) {
        this.identityRegistry = Objects.requireNonNull(identityRegistry, "identityRegistry");
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
    }

    @Override
    public Optional<CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext> resolve(
            UUID authenticatedPlayerUuid
    ) {
        if (authenticatedPlayerUuid == null) return Optional.empty();

        Optional<String> canonicalPlayerId = identityRegistry.resolveParticipantId(
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                authenticatedPlayerUuid.toString()
        );
        if (canonicalPlayerId.isEmpty()) return Optional.empty();

        String playerId = canonicalPlayerId.get();
        if (playerRepository.findPlayer(playerId).isEmpty()) return Optional.empty();

        Optional<CanonicalPlayerEncounterProfile> profileResult = profileRepository.findProfile(playerId);
        if (profileResult.isEmpty()) return Optional.empty();
        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (!profile.playerId().equals(playerId)) return Optional.empty();

        return Optional.of(new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                playerId,
                profile.pokemonIds(),
                profile.consumableQuantities(),
                profile.arena()
        ));
    }
}
