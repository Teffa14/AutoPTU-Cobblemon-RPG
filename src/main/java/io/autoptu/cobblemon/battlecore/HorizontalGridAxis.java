package io.autoptu.cobblemon.battlecore;

/** Unit horizontal basis direction used to anchor a 2D PTU grid into a Minecraft-like world plane. */
public enum HorizontalGridAxis {
    POSITIVE_X(1, 0),
    NEGATIVE_X(-1, 0),
    POSITIVE_Z(0, 1),
    NEGATIVE_Z(0, -1);

    private final int dx;
    private final int dz;

    HorizontalGridAxis(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    public boolean isPerpendicularTo(HorizontalGridAxis other) {
        if (other == null) return false;
        return dx * other.dx + dz * other.dz == 0;
    }
}
