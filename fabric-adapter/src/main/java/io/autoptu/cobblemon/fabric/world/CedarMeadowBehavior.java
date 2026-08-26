package io.autoptu.cobblemon.fabric.world;

/** Server-owned coarse behavior for the first living-world slice. */
public final class CedarMeadowBehavior {
    private static final int RECOVERY_QUIET_TICKS = 80;
    private static final int ABANDONED_ALARM_QUIET_TICKS = 100;

    public enum State {
        CALM,
        WATCHING,
        ALARMED,
        RECOVERING
    }

    private State state = State.CALM;
    private int quietTicks;

    public State state() {
        return state;
    }

    public State update(double nearestPlayerDistance, boolean playerPresent) {
        if (!playerPresent) {
            quietTicks++;
            int calmThreshold = state == State.RECOVERING
                    ? RECOVERY_QUIET_TICKS
                    : ABANDONED_ALARM_QUIET_TICKS;
            if (quietTicks >= calmThreshold) {
                state = State.CALM;
            }
            return state;
        }

        if (nearestPlayerDistance <= 7.0D) {
            quietTicks = 0;
            state = State.ALARMED;
            return state;
        }

        if (nearestPlayerDistance <= 14.0D) {
            quietTicks = 0;
            state = State.WATCHING;
            return state;
        }

        quietTicks++;
        if (state == State.ALARMED || state == State.WATCHING) {
            state = State.RECOVERING;
        }
        if (quietTicks >= RECOVERY_QUIET_TICKS) {
            state = State.CALM;
        }
        return state;
    }
}
