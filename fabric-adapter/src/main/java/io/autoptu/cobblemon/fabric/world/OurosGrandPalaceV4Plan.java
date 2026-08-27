package io.autoptu.cobblemon.fabric.world;

import java.util.List;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.Room;

/**
 * Physical room plan for the courtyard-based Grand Palace V4.
 *
 * The four double-height ceremonial rooms keep the original central axis. Every side room moves
 * eleven blocks outward. This opens two sixteen-block-wide longitudinal courts between the central
 * corps and the west/east wings while preserving the authored room dimensions and themes.
 */
final class OurosGrandPalaceV4Plan {
    static final int LEFT_SHIFT_X = -11;
    static final int RIGHT_SHIFT_X = 11;

    static final int WEST_WING_MIN_X = -50;
    static final int WEST_WING_MAX_X = -28;
    static final int WEST_COURT_MIN_X = -27;
    static final int WEST_COURT_MAX_X = -12;
    static final int CENTRAL_MIN_X = -11;
    static final int CENTRAL_MAX_X = 11;
    static final int EAST_COURT_MIN_X = 12;
    static final int EAST_COURT_MAX_X = 27;
    static final int EAST_WING_MIN_X = 28;
    static final int EAST_WING_MAX_X = 50;

    private OurosGrandPalaceV4Plan() {}

    static Room shifted(Room room, int dx) {
        return new Room(room.name(), room.minX() + dx, room.minZ(), room.maxX() + dx, room.maxZ(),
                room.floorY(), room.ceilingY());
    }

    static Room physical(Room room) {
        if (isCentral(room)) return room;
        return shifted(room, isLeft(room) ? LEFT_SHIFT_X : RIGHT_SHIFT_X);
    }

    static boolean isCentral(Room room) {
        return room == ANTECHAMBER || room == AUDIENCE_CHAMBER || room == THEMIS_HALL || room == MARBLE_SALON;
    }

    static boolean isLeft(Room room) {
        return room == CABINET || room == BLOOMING_SALON || room == LIBRARY || room == PORCELAIN_HALL
                || room == RAILING_SALON || room == ACCOUNTING_OFFICE || room == GLOBE_BOOK_CABINET
                || room == BANQUET_HALL;
    }

    static List<Room> groundSideRooms() {
        return List.of(
                physical(CABINET), physical(SALLA_TERRENA),
                physical(BLOOMING_SALON), physical(HUNTING_SALON),
                physical(LIBRARY), physical(GEOGRAPHY_CABINET),
                physical(PORCELAIN_HALL), physical(GALLERY_OF_ART)
        );
    }

    static List<Room> upperSideRooms() {
        return List.of(
                physical(RAILING_SALON), physical(COAT_OF_ARMS_HALL),
                physical(ACCOUNTING_OFFICE), physical(MUSIC_CHAMBER),
                physical(GLOBE_BOOK_CABINET), physical(BLUE_SALON),
                physical(BANQUET_HALL)
        );
    }

    static List<Room> ceremonialRooms() {
        return List.of(ANTECHAMBER, AUDIENCE_CHAMBER, THEMIS_HALL, MARBLE_SALON);
    }
}
