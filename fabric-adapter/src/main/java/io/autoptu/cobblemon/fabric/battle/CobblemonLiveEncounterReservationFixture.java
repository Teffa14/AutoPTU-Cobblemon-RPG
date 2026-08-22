package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleCombatantAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterParticipantSnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterRosterRepository;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservation;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservationService;
import io.autoptu.cobblemon.authority.BattleParticipantKind;
import io.autoptu.cobblemon.authority.CanonicalBattleEncounterRepository;
import io.autoptu.cobblemon.authority.CanonicalBattlePokemonView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Isolated server-owned canonical repository used only by the opt-in production runtime smoke.
 * It deliberately contains no Minecraft/Cobblemon state or derivation logic.
 */
final class CobblemonLiveEncounterReservationFixture
        implements CanonicalBattleEncounterRepository, BattleEncounterRosterRepository {
    static final String RESERVATION_ID = "live-smoke-encounter-reservation";
    static final long RNG_SEED = 73021L;

    private final Map<String, CanonicalBattlePokemonView> canonical = new HashMap<>();
    private final Map<String, BattleEncounterRosterReservation> reservations = new HashMap<>();
    private final Set<String> lockedCombatants = new HashSet<>();

    void register(BattleParticipantKind kind, String participantId, CanonicalBattlePokemonView state) {
        if (kind == null || participantId == null || participantId.isBlank() || state == null) {
            throw new IllegalArgumentException("canonical smoke fixture identity is required");
        }
        String key = key(kind, participantId, state.pokemonId());
        if (canonical.putIfAbsent(key, state) != null) {
            throw new IllegalStateException("canonical smoke fixture is already registered");
        }
    }

    BattleEncounterRosterReservationService reservationService() {
        return new BattleEncounterRosterReservationService(
                this,
                this,
                () -> RESERVATION_ID,
                () -> RNG_SEED
        );
    }

    @Override
    public Optional<CanonicalBattlePokemonView> findCombatant(
            BattleParticipantKind participantKind,
            String participantId,
            String combatantId
    ) {
        return Optional.ofNullable(canonical.get(key(participantKind, participantId, combatantId)));
    }

    @Override
    public Optional<BattleEncounterRosterReservation> findReservation(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public synchronized boolean tryReserve(BattleEncounterRosterReservation reservation) {
        if (reservation == null || reservations.containsKey(reservation.reservationId())) return false;

        for (BattleEncounterParticipantSnapshot participant : reservation.participants()) {
            for (BattleCombatantAuthoritySnapshot combatant : participant.combatants()) {
                CanonicalBattlePokemonView live = canonical.get(key(
                        combatant.participantKind(),
                        combatant.participantId(),
                        combatant.combatantId()
                ));
                if (live == null
                        || live.revision() != combatant.revision()
                        || lockedCombatants.contains(combatant.combatantId())) {
                    return false;
                }
            }
        }

        reservation.participants().stream()
                .flatMap(participant -> participant.combatants().stream())
                .forEach(combatant -> lockedCombatants.add(combatant.combatantId()));
        reservations.put(reservation.reservationId(), reservation);
        return true;
    }

    @Override
    public synchronized boolean release(String reservationId) {
        BattleEncounterRosterReservation reservation = reservations.remove(reservationId);
        if (reservation == null) return false;
        reservation.participants().stream()
                .flatMap(participant -> participant.combatants().stream())
                .forEach(combatant -> lockedCombatants.remove(combatant.combatantId()));
        return true;
    }

    private static String key(BattleParticipantKind kind, String participantId, String combatantId) {
        return kind + "|" + participantId.strip() + "|" + combatantId.strip();
    }
}
