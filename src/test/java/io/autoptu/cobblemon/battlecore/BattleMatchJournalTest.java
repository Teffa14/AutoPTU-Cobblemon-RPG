package io.autoptu.cobblemon.battlecore;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleMatchJournalTest {
    @Test void recordsBothSidesAndRoundTripsUnicode() {
        var journal = new BattleMatchJournal(UUID.randomUUID(), "Pokémon 雪", "Rival", 1);
        journal.clock(20, 1);
        journal.movement(BattleMatchReport.Side.ALLY, new BattleGridCoordinate(1, 1), new BattleGridCoordinate(2, 1));
        journal.attack(BattleMatchReport.Side.ALLY, "eléctrico", true, true, 12, 48, null, null);
        journal.attack(BattleMatchReport.Side.ENEMY, "strike", false, false, 0, 60, null, null);
        journal.clock(55, 2);
        journal.finish(BattleMatchReport.Outcome.VICTORY);
        var report = journal.snapshot();
        assertEquals(report, BattleMatchReportCodec.decode(BattleMatchReportCodec.encode(report)));
        assertEquals(12, report.ally().damage());
        assertEquals(1, report.ally().criticals());
        assertEquals(1, report.ally().shifts());
        assertEquals(1, report.enemy().misses());
        assertEquals(55, report.elapsedTicks());
    }

    @Test void boundedDetailRetainsCumulativeStatistics() {
        var journal = new BattleMatchJournal(UUID.randomUUID(), "Ally", "Rival", 1);
        for (int i = 0; i < 300; i++) {
            journal.clock(i, 1);
            journal.attack(BattleMatchReport.Side.ALLY, "strike", true, false, 2, 60, null, null);
        }
        var report = journal.snapshot();
        assertEquals(256, report.events().size());
        assertEquals(45, report.omittedEvents());
        assertEquals(300, report.ally().attacks());
        assertEquals(600, report.ally().damage());
        assertEquals(report, BattleMatchReportCodec.decode(BattleMatchReportCodec.encode(report)));
    }

    @Test void finishedJournalCannotBeRewrittenAndMalformedReportsFail() {
        var journal = new BattleMatchJournal(UUID.randomUUID(), "Ally", "Rival", 1);
        journal.finish(BattleMatchReport.Outcome.DEFEAT);
        journal.finish(BattleMatchReport.Outcome.CLOSED);
        assertEquals(BattleMatchReport.Outcome.DEFEAT, journal.snapshot().outcome());
        assertThrows(IllegalStateException.class, () -> journal.pass(BattleMatchReport.Side.ALLY));
        byte[] bytes = BattleMatchReportCodec.encode(journal.snapshot());
        assertThrows(IllegalArgumentException.class, () -> BattleMatchReportCodec.decode(Arrays.copyOf(bytes, bytes.length - 1)));
        assertThrows(IllegalArgumentException.class, () -> BattleMatchReportCodec.decode(Arrays.copyOf(bytes, bytes.length + 1)));
        assertThrows(IllegalArgumentException.class, () -> BattleMatchReportCodec.decode(new byte[BattleMatchReportCodec.MAX_BYTES + 1]));
    }
}
