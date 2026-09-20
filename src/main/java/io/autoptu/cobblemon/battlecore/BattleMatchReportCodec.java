package io.autoptu.cobblemon.battlecore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.UUID;

/** Versioned bounded report format shared by private persistence and S2C delivery. */
public final class BattleMatchReportCodec {
    public static final int MAX_BYTES = 262144;
    private static final int MAGIC = 0x41505452;
    private static final int VERSION = 1;

    private BattleMatchReportCodec() {}

    public static byte[] encode(BattleMatchReport report) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeLong(report.matchId().getMostSignificantBits());
                out.writeLong(report.matchId().getLeastSignificantBits());
                out.writeUTF(report.allyName());
                out.writeUTF(report.enemyName());
                out.writeLong(report.startedAt());
                out.writeLong(report.elapsedTicks());
                out.writeInt(report.round());
                out.writeUTF(report.outcome().name());
                statistics(out, report.ally());
                statistics(out, report.enemy());
                out.writeInt(report.omittedEvents());
                out.writeInt(report.events().size());
                for (var event : report.events()) {
                    out.writeLong(event.sequence());
                    out.writeLong(event.tick());
                    out.writeInt(event.round());
                    out.writeUTF(event.kind().name());
                    out.writeUTF(event.side().name());
                    out.writeUTF(event.action());
                    out.writeInt(event.damage());
                    out.writeBoolean(event.targetHp() != null);
                    if (event.targetHp() != null) out.writeInt(event.targetHp());
                    out.writeBoolean(event.critical());
                    cell(out, event.from());
                    cell(out, event.to());
                }
            }
            if (bytes.size() > MAX_BYTES) throw new IllegalArgumentException("report exceeds byte limit");
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    public static BattleMatchReport decode(byte[] bytes) {
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("report exceeds byte limit");
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) throw new IllegalArgumentException("unsupported match report");
            UUID id = new UUID(in.readLong(), in.readLong());
            String allyName = in.readUTF();
            String enemyName = in.readUTF();
            long startedAt = in.readLong();
            long ticks = in.readLong();
            int round = in.readInt();
            var outcome = BattleMatchReport.Outcome.valueOf(in.readUTF());
            var ally = statistics(in);
            var enemy = statistics(in);
            int omitted = in.readInt();
            int count = in.readInt();
            if (count < 0 || count > BattleMatchReport.MAX_EVENTS) throw new IllegalArgumentException("invalid event count");
            var events = new ArrayList<BattleMatchReport.Event>(count);
            for (int i = 0; i < count; i++) {
                long sequence = in.readLong();
                long tick = in.readLong();
                int eventRound = in.readInt();
                var kind = BattleMatchReport.Kind.valueOf(in.readUTF());
                var side = BattleMatchReport.Side.valueOf(in.readUTF());
                String action = in.readUTF();
                int damage = in.readInt();
                Integer hp = in.readBoolean() ? in.readInt() : null;
                boolean critical = in.readBoolean();
                events.add(new BattleMatchReport.Event(sequence, tick, eventRound, kind, side, action, damage, hp, critical, cell(in), cell(in)));
            }
            if (in.available() != 0) throw new IllegalArgumentException("trailing report bytes");
            return new BattleMatchReport(id, allyName, enemyName, startedAt, ticks, round, outcome, ally, enemy, omitted, events);
        } catch (IOException error) {
            throw new IllegalArgumentException("incomplete match report", error);
        }
    }

    private static void statistics(DataOutputStream out, BattleMatchReport.Statistics value) throws IOException {
        out.writeInt(value.attacks());
        out.writeInt(value.hits());
        out.writeInt(value.misses());
        out.writeInt(value.criticals());
        out.writeLong(value.damage());
        out.writeInt(value.shifts());
    }

    private static BattleMatchReport.Statistics statistics(DataInputStream in) throws IOException {
        return new BattleMatchReport.Statistics(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readLong(), in.readInt());
    }

    private static void cell(DataOutputStream out, BattleGridCoordinate cell) throws IOException {
        out.writeBoolean(cell != null);
        if (cell != null) {
            out.writeInt(cell.x());
            out.writeInt(cell.y());
        }
    }

    private static BattleGridCoordinate cell(DataInputStream in) throws IOException {
        return in.readBoolean() ? new BattleGridCoordinate(in.readInt(), in.readInt()) : null;
    }
}
