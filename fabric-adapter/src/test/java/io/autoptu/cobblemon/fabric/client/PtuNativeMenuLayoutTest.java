package io.autoptu.cobblemon.fabric.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PtuNativeMenuLayoutTest {
    @Test void fitsScaledMinecraftScreensAndLeavesHeaderToggleAccessible() {
        for (int width : new int[]{320, 426, 640, 960, 1280}) {
            for (int height : new int[]{180, 240, 360, 720}) {
                var bounds = PtuNativeMenuClient.bounds(width, height);
                assertTrue(bounds.x() >= 0);
                assertTrue(bounds.y() >= 26);
                assertTrue(bounds.x() + bounds.width() <= width);
                assertTrue(bounds.y() + bounds.height() <= height);
                assertFalse(bounds.contains(width - 20, 15));
                assertTrue(bounds.contains(bounds.x() + 10, bounds.y() + 10));
            }
        }
    }
}
