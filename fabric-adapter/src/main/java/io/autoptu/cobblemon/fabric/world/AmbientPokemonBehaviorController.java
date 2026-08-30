package io.autoptu.cobblemon.fabric.world;

/**
 * Server-owned coarse ambient presentation state for roaming Pokemon actors.
 *
 * This controller consumes only Minecraft-observed proximity and authored presentation thresholds.
 * It does not read or derive Pokemon species stats, HP, moves, statuses, abilities, battle state,
 * encounter legality or any PTU outcome. Callers may translate the returned state into Minecraft
 * navigation/look-control presentation only.
 */
public final class AmbientPokemonBehaviorController {
    public record Profile(
            double watchDistance,
            double alarmDistance,
            int recoveryQuietUpdates,
            int abandonedAlarmQuietUpdates
    ) {
        public Profile {
            if (!Double.isFinite(watchDistance) || watchDistance <= 0.0D) {
                throw new IllegalArgumentException("watchDistance must be finite and positive");
            }
            if (!Double.isFinite(alarmDistance) || alarmDistance <= 0.0D || alarmDistance > watchDistance) {
                throw new IllegalArgumentException("alarmDistance must be finite, positive and <= watchDistance");
            }
            if (recoveryQuietUpdates <= 0 || abandonedAlarmQuietUpdates <= 0) {
                throw new IllegalArgumentException("quiet update thresholds must be positive");
            }
        }
    }

    public enum State {
        CALM,
        WATCHING,
        ALARMED,
        RECOVERING
    }

    private final Profile profile;
    private State state = State.CALM;
    private int quietUpdates;

    public AmbientPokemonBehaviorController(Profile profile) {
        if (profile == null) throw new IllegalArgumentException("profile is required");
        this.profile = profile;
    }

    public State state() {
        return state;
    }

    public State update(double nearestPlayerDistance, boolean playerPresent) {
        if (!playerPresent) {
            quietUpdates++;
            int calmThreshold = state == State.RECOVERING
                    ? profile.recoveryQuietUpdates()
                    : profile.abandonedAlarmQuietUpdates();
            if (quietUpdates >= calmThreshold) state = State.CALM;
            return state;
        }

        if (!Double.isFinite(nearestPlayerDistance) || nearestPlayerDistance < 0.0D) {
            throw new IllegalArgumentException("nearestPlayerDistance must be finite and non-negative when a player is present");
        }

        if (nearestPlayerDistance <= profile.alarmDistance()) {
            quietUpdates = 0;
            state = State.ALARMED;
            return state;
        }

        if (nearestPlayerDistance <= profile.watchDistance()) {
            quietUpdates = 0;
            state = State.WATCHING;
            return state;
        }

        quietUpdates++;
        if (state == State.ALARMED || state == State.WATCHING) state = State.RECOVERING;
        if (quietUpdates >= profile.recoveryQuietUpdates()) state = State.CALM;
        return state;
    }
}