package io.autoptu.cobblemon.fabric.world;

/** Server-owned coarse behavior for the first living-world slice. */
public final class CedarMeadowBehavior {
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
            if (quietTicks >= 100) {
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
        if (quietTicks >= 80) {
            state = State.CALM;
        }
        return state;
    }
}
