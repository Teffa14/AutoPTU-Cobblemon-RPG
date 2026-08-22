package io.autoptu.cobblemon.fabric.battle;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.ai.RandomBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.authority.BattleEncounterParticipantSnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservation;
import io.autoptu.cobblemon.authority.BattleParticipantKind;
import io.autoptu.cobblemon.authority.CanonicalEncounterPokemonState;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Opt-in production-runtime proof that AutoPTU can preempt a real Cobblemon battle start before
 * Cobblemon registers or launches its battle engine, carry only opaque participant IDs across the
 * adapter boundary, map those IDs to independently server-owned canonical records, and atomically
 * reserve both canonical encounter sides before cancellation is accepted.
 */
public final class CobblemonLiveBattleInterceptionSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveBattleInterceptionSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Cobblemon battle interception smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String LEFT_PARTICIPANT_ID = "live-smoke-wild-left";
    private static final String RIGHT_PARTICIPANT_ID = "live-smoke-wild-right";
    private static final String LEFT_COMBATANT_ID = "live-smoke-pokemon-left";
    private static final String RIGHT_COMBATANT_ID = "live-smoke-pokemon-right";

    private CobblemonLiveBattleInterceptionSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(CobblemonLiveBattleInterceptionSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos origin = world.getSpawnPos().up(2).east(6);
        Species species = PokemonSpecies.INSTANCE.getByName("pikachu");
        if (species == null) throw new IllegalStateException("Cobblemon Pikachu species is unavailable");

        PokemonEntity first = spawnFixture(world, species, origin);
        PokemonEntity second = spawnFixture(world, species, origin.east(3));

        BattlePokemon firstBattlePokemon = new BattlePokemon(
                first.getPokemon(),
                first.getPokemon(),
                new ArrayList<>(),
                new ArrayList<>()
        );
        BattlePokemon secondBattlePokemon = new BattlePokemon(
                second.getPokemon(),
                second.getPokemon(),
                new ArrayList<>(),
                new ArrayList<>()
        );
        PokemonBattleActor firstActor = new PokemonBattleActor(
                first.getPokemon().getUuid(),
                firstBattlePokemon,
                -1.0F,
                new RandomBattleAI()
        );
        PokemonBattleActor secondActor = new PokemonBattleActor(
                second.getPokemon().getUuid(),
                secondBattlePokemon,
                -1.0F,
                new RandomBattleAI()
        );

        // These canonical records are fixed server-owned PTU fixtures. No level, HP, stat, move,
        // ability or item value is copied from the live Cobblemon entities above.
        CobblemonLiveEncounterReservationFixture canonicalFixture = new CobblemonLiveEncounterReservationFixture();
        canonicalFixture.register(
                BattleParticipantKind.WILD,
                LEFT_PARTICIPANT_ID,
                canonicalFixturePokemon(LEFT_COMBATANT_ID, 11, 27, 101L)
        );
        canonicalFixture.register(
                BattleParticipantKind.WILD,
                RIGHT_PARTICIPANT_ID,
                canonicalFixturePokemon(RIGHT_COMBATANT_ID, 13, 31, 202L)
        );

        CobblemonCanonicalEncounterIdentityRegistry identityRegistry = new CobblemonCanonicalEncounterIdentityRegistry();
        identityRegistry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                firstActor.getUuid().toString(),
                LEFT_PARTICIPANT_ID,
                Map.of(first.getPokemon().getUuid().toString(), LEFT_COMBATANT_ID)
        );
        identityRegistry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                secondActor.getUuid().toString(),
                RIGHT_PARTICIPANT_ID,
                Map.of(second.getPokemon().getUuid().toString(), RIGHT_COMBATANT_ID)
        );
        CobblemonBattleStartReservationCoordinator reservationCoordinator =
                new CobblemonBattleStartReservationCoordinator(
                        identityRegistry,
                        canonicalFixture.reservationService()
                );

        AtomicReference<CobblemonBattleStartInterceptor.BattleStartSignal> intercepted = new AtomicReference<>();
        AtomicReference<CobblemonBattleStartReservationCoordinator.ClaimAttempt> claimAttempt = new AtomicReference<>();
        AtomicBoolean postEventObserved = new AtomicBoolean(false);

        ObservableSubscription<BattleStartedEvent.Pre> preSubscription =
                CobblemonBattleStartInterceptor.subscribe(signal -> {
                    if (!intercepted.compareAndSet(null, signal)) {
                        throw new IllegalStateException("battle interception smoke observed more than one pre-start event");
                    }
                    CobblemonBattleStartReservationCoordinator.ClaimAttempt attempt =
                            reservationCoordinator.tryReserve(signal);
                    if (!claimAttempt.compareAndSet(null, attempt)) {
                        throw new IllegalStateException("battle interception smoke created more than one claim attempt");
                    }
                    return attempt.claimed();
                });
        ObservableSubscription<BattleStartedEvent.Post> postSubscription =
                CobblemonEvents.BATTLE_STARTED_POST.subscribe(
                        Priority.HIGHEST,
                        (Consumer<BattleStartedEvent.Post>) ignored -> postEventObserved.set(true)
                );

        try {
            BattleStartResult result = BattleRegistry.startBattle(
                    BattleFormat.Companion.getGEN_9_SINGLES(),
                    new BattleSide(firstActor),
                    new BattleSide(secondActor),
                    true
            );

            CobblemonBattleStartInterceptor.BattleStartSignal signal = intercepted.get();
            if (signal == null) {
                throw new IllegalStateException("Cobblemon BATTLE_STARTED_PRE was not intercepted");
            }
            CobblemonBattleStartReservationCoordinator.ClaimAttempt attempt = claimAttempt.get();
            if (attempt == null || !attempt.claimed()) {
                throw new IllegalStateException("live Cobblemon encounter did not produce a canonical roster reservation");
            }
            if (!(result instanceof ErroredBattleStart)) {
                throw new IllegalStateException("preempted Cobblemon battle unexpectedly returned success");
            }
            UUID battleId = UUID.fromString(signal.cobblemonBattleId());
            if (BattleRegistry.getBattle(battleId) != null) {
                throw new IllegalStateException("preempted Cobblemon battle was registered despite interception");
            }
            if (postEventObserved.get()) {
                throw new IllegalStateException("Cobblemon BATTLE_STARTED_POST fired for a preempted battle");
            }

            List<CobblemonBattleStartInterceptor.ParticipantIdentity> participants = signal.participants();
            if (participants.size() != 2) {
                throw new IllegalStateException("battle interception smoke did not capture both participants");
            }
            assertWildParticipant(participants.get(0), 1, firstActor.getUuid(), first.getPokemon().getUuid());
            assertWildParticipant(participants.get(1), 2, secondActor.getUuid(), second.getPokemon().getUuid());

            BattleEncounterRosterReservation reservation = attempt.reservation();
            if (!reservation.reservationId().equals(CobblemonLiveEncounterReservationFixture.RESERVATION_ID)) {
                throw new IllegalStateException("canonical reservation id mismatch");
            }
            if (reservation.rngSeed() != CobblemonLiveEncounterReservationFixture.RNG_SEED) {
                throw new IllegalStateException("canonical reservation seed mismatch");
            }
            if (canonicalFixture.findReservation(reservation.reservationId()).orElse(null) != reservation) {
                throw new IllegalStateException("canonical reservation repository did not retain the claimed encounter");
            }
            assertCanonicalParticipant(reservation, 1, LEFT_PARTICIPANT_ID, LEFT_COMBATANT_ID, 11, 27);
            assertCanonicalParticipant(reservation, 2, RIGHT_PARTICIPANT_ID, RIGHT_COMBATANT_ID, 13, 31);

            LOGGER.info(
                    "{}: battle={} reservation={} participants={}",
                    SUCCESS_LOG,
                    signal.cobblemonBattleId(),
                    reservation.reservationId(),
                    participants.size()
            );
        } finally {
            CobblemonBattleStartInterceptor.unsubscribe(preSubscription);
            CobblemonEvents.BATTLE_STARTED_POST.unsubscribe(postSubscription);
            canonicalFixture.release(CobblemonLiveEncounterReservationFixture.RESERVATION_ID);
            first.discard();
            second.discard();
        }
    }

    private static CanonicalEncounterPokemonState canonicalFixturePokemon(
            String combatantId,
            int level,
            int hp,
            long revision
    ) {
        return new CanonicalEncounterPokemonState(
                combatantId,
                "cobblemon:pikachu",
                level,
                Set.of("Overland"),
                Set.of(),
                null,
                null,
                new CanonicalHealth(hp, hp),
                null,
                null,
                null,
                null,
                null,
                null,
                revision
        );
    }

    private static void assertWildParticipant(
            CobblemonBattleStartInterceptor.ParticipantIdentity participant,
            int expectedSide,
            UUID expectedActorId,
            UUID expectedPokemonId
    ) {
        if (participant.side() != expectedSide) {
            throw new IllegalStateException("intercepted participant side mismatch");
        }
        if (participant.kind() != CobblemonBattleStartInterceptor.ParticipantKind.WILD) {
            throw new IllegalStateException("intercepted participant kind mismatch");
        }
        if (!participant.actorId().equals(expectedActorId.toString())) {
            throw new IllegalStateException("intercepted actor identity mismatch");
        }
        if (!participant.pokemonIds().equals(List.of(expectedPokemonId.toString()))) {
            throw new IllegalStateException("intercepted Pokemon identity mismatch");
        }
    }

    private static void assertCanonicalParticipant(
            BattleEncounterRosterReservation reservation,
            int side,
            String participantId,
            String combatantId,
            int level,
            int hp
    ) {
        BattleEncounterParticipantSnapshot participant = reservation.participants().stream()
                .filter(candidate -> candidate.side() == side)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("canonical reservation is missing side " + side));
        if (!participant.participantId().equals(participantId)) {
            throw new IllegalStateException("canonical participant id mismatch");
        }
        if (!participant.teamId().equals("battle-side-" + side)) {
            throw new IllegalStateException("canonical team identity was not generated from encounter side");
        }
        if (participant.participantKind() != BattleParticipantKind.WILD) {
            throw new IllegalStateException("canonical participant kind mismatch");
        }
        if (participant.combatants().size() != 1) {
            throw new IllegalStateException("canonical participant roster size mismatch");
        }
        var combatant = participant.combatants().getFirst();
        if (!combatant.combatantId().equals(combatantId)
                || combatant.level() != level
                || combatant.health() == null
                || combatant.health().currentHp() != hp
                || combatant.health().maxHp() != hp) {
            throw new IllegalStateException("canonical combatant fixture was not preserved through reservation");
        }
    }

    private static PokemonEntity spawnFixture(ServerWorld world, Species species, BlockPos position) {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.setAiDisabled(true);
        entity.setPersistent();
        entity.refreshPositionAndAngles(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        if (!world.spawnEntity(entity)) {
            throw new IllegalStateException("failed to spawn live Cobblemon PokemonEntity for battle interception smoke");
        }
        return entity;
    }
}
