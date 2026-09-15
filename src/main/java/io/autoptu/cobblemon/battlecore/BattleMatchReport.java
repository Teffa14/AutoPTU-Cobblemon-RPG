package io.autoptu.cobblemon.battlecore;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Read-only practice-match report; never an inventory or campaign outcome transaction. */
public record BattleMatchReport(UUID matchId, String allyName, String enemyName, long startedAt,
                                long elapsedTicks, int round, Outcome outcome, Statistics ally,
                                Statistics enemy, int omittedEvents, List<Event> events) {
    public static final int MAX_EVENTS = 256;

    public BattleMatchReport {
        Objects.requireNonNull(matchId, "matchId");
        allyName = text(allyName, 128);
        enemyName = text(enemyName, 128);
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(ally, "ally");
        Objects.requireNonNull(enemy, "enemy");
        if (startedAt < 0 || elapsedTicks < 0 || round < 1 || omittedEvents < 0) {
            throw new IllegalArgumentException("invalid match report counters");
        }
        events = List.copyOf(events);
        if (events.size() > MAX_EVENTS) throw new IllegalArgumentException("report exceeds event limit");
        long previous = -1;
        for (Event event : events) {
            if (event.sequence() <= previous) throw new IllegalArgumentException("events are not ordered");
            previous = event.sequence();
        }
    }

    public enum Outcome {
        RUNNING,
        VICTORY,
        DEFEAT,
        CLOSED,
        ERROR
    }

    public enum Side {
        ALLY,
        ENEMY,
        NONE
    }

    public enum Kind {
        START,
        TURN,
        MOVEMENT,
        ATTACK,
        HIT,
        MISS,
        PASS,
        END
    }

    public record Statistics(int attacks, int hits, int misses, int criticals, long damage, int shifts) {
        public Statistics {
            if (attacks < 0 || hits < 0 || misses < 0 || criticals < 0 || damage < 0 || shifts < 0) {
                throw new IllegalArgumentException("negative statistics");
            }
            if (hits + (long) misses != attacks || criticals > hits) {
                throw new IllegalArgumentException("inconsistent attack statistics");
            }
        }

        public static Statistics empty() {
            return new Statistics(0, 0, 0, 0, 0, 0);
        }

        public Statistics attack(boolean hit, boolean critical, int damage) {
            if (damage < 0 || (!hit && (critical || damage != 0))) {
                throw new IllegalArgumentException("invalid attack result");
            }
            return new Statistics(Math.incrementExact(attacks), hits + (hit ? 1 : 0),
                    misses + (hit ? 0 : 1), criticals + (critical ? 1 : 0), Math.addExact(this.damage, damage), shifts);
        }

        public Statistics shift() {
            return new Statistics(attacks, hits, misses, criticals, damage, Math.incrementExact(shifts));
        }
    }

    public record Event(long sequence, long tick, int round, Kind kind, Side side, String action,
                        int damage, Integer targetHp, boolean critical, BattleGridCoordinate from,
                        BattleGridCoordinate to) {
        public Event {
            if (sequence < 0 || tick < 0 || round < 1 || damage < 0) throw new IllegalArgumentException("invalid event counters");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(side, "side");
            action = text(action, 256);
            if (targetHp != null && targetHp < 0) throw new IllegalArgumentException("negative target HP");
            if (kind == Kind.MISS && (critical || damage > 0)) throw new IllegalArgumentException("miss cannot deal damage");
            if (kind == Kind.MOVEMENT && (from == null || to == null)) throw new IllegalArgumentException("movement endpoints required");
        }
    }

    private static String text(String value, int max) {
        Objects.requireNonNull(value);
        if (value.length() > max) throw new IllegalArgumentException("report text exceeds limit");
        return value;
    }
}
