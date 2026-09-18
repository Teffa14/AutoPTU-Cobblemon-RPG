package io.autoptu.cobblemon.battlecore;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Bounded event recorder with statistics retained even when old detail rows are evicted. */
public final class BattleMatchJournal {
    private final UUID matchId;
    private final String allyName;
    private final String enemyName;
    private final long startedAt;
    private final ArrayDeque<BattleMatchReport.Event> events = new ArrayDeque<>();
    private BattleMatchReport.Statistics ally = BattleMatchReport.Statistics.empty();
    private BattleMatchReport.Statistics enemy = BattleMatchReport.Statistics.empty();
    private BattleMatchReport.Outcome outcome = BattleMatchReport.Outcome.RUNNING;
    private long sequence;
    private long elapsedTicks;
    private int round = 1;
    private int omitted;

    public BattleMatchJournal(UUID matchId, String allyName, String enemyName, long startedAt) {
        this.matchId = Objects.requireNonNull(matchId);
        this.allyName = Objects.requireNonNull(allyName);
        this.enemyName = Objects.requireNonNull(enemyName);
        this.startedAt = startedAt;
        append(BattleMatchReport.Kind.START, BattleMatchReport.Side.NONE, "", 0, null, false, null, null);
        snapshot();
    }

    public synchronized void clock(long elapsedTicks, int round) {
        if (elapsedTicks < this.elapsedTicks || round < this.round) {
            throw new IllegalArgumentException("match clock cannot go backwards");
        }
        if (outcome != BattleMatchReport.Outcome.RUNNING) return;
        this.elapsedTicks = elapsedTicks;
        this.round = round;
    }

    public synchronized void turn(BattleMatchReport.Side side) {
        requireRunning();
        append(BattleMatchReport.Kind.TURN, side, "", 0, null, false, null, null);
    }

    public synchronized void pass(BattleMatchReport.Side side) {
        requireRunning();
        append(BattleMatchReport.Kind.PASS, side, "", 0, null, false, null, null);
    }

    public synchronized void movement(BattleMatchReport.Side side, BattleGridCoordinate from, BattleGridCoordinate to) {
        requireRunning();
        requireCombatant(side);
        var statistics = statistics(side).shift();
        append(BattleMatchReport.Kind.MOVEMENT, side, "Shift", 0, null, false, from, to);
        statistics(side, statistics);
    }

    public synchronized void attack(BattleMatchReport.Side side, String moveId, boolean hit,
                                    boolean critical, int damage, int targetHp,
                                    BattleGridCoordinate from, BattleGridCoordinate target) {
        requireRunning();
        requireCombatant(side);
        var statistics = statistics(side).attack(hit, critical, damage);
        append(hit ? BattleMatchReport.Kind.HIT : BattleMatchReport.Kind.MISS, side, moveId, damage, targetHp, critical, from, target);
        statistics(side, statistics);
    }

    public synchronized void finish(BattleMatchReport.Outcome result) {
        Objects.requireNonNull(result);
        if (result == BattleMatchReport.Outcome.RUNNING) throw new IllegalArgumentException("terminal result required");
        if (outcome != BattleMatchReport.Outcome.RUNNING) return;
        append(BattleMatchReport.Kind.END, BattleMatchReport.Side.NONE, result.name(), 0, null, false, null, null);
        outcome = result;
    }

    public synchronized BattleMatchReport snapshot() {
        return new BattleMatchReport(matchId, allyName, enemyName, startedAt, elapsedTicks, round,
                outcome, ally, enemy, omitted, List.copyOf(events));
    }

    private void append(BattleMatchReport.Kind kind, BattleMatchReport.Side side, String action, int damage,
                        Integer hp, boolean critical, BattleGridCoordinate from, BattleGridCoordinate to) {
        var event = new BattleMatchReport.Event(sequence, elapsedTicks, round, kind, side, action, damage, hp, critical, from, to);
        if (events.size() == BattleMatchReport.MAX_EVENTS) {
            events.removeFirst();
            omitted++;
        }
        events.addLast(event);
        sequence++;
    }

    private void requireRunning() {
        if (outcome != BattleMatchReport.Outcome.RUNNING) throw new IllegalStateException("match report is closed");
    }

    private static void requireCombatant(BattleMatchReport.Side side) {
        if (side == BattleMatchReport.Side.NONE) throw new IllegalArgumentException("combatant side required");
    }

    private BattleMatchReport.Statistics statistics(BattleMatchReport.Side side) {
        return side == BattleMatchReport.Side.ALLY ? ally : enemy;
    }

    private void statistics(BattleMatchReport.Side side, BattleMatchReport.Statistics value) {
        if (side == BattleMatchReport.Side.ALLY) ally = value;
        else enemy = value;
    }
}
