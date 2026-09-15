package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleHealthDisplayTest {
    @Test void damageLeavesTrailThenConvergesWithoutChangingNumericInput() {
        var display = new BattleHealthDisplay();
        display.accept(100, 100);
        display.accept(40, 100);
        assertEquals(-60, display.change());
        for (int i = 0; i < 8; i++) display.tick();
        assertEquals(1F, display.trail());
        assertTrue(display.displayed() < 1F && display.displayed() > .4F);
        for (int i = 0; i < 80; i++) display.tick();
        assertEquals(.4F, display.displayed());
        assertEquals(.4F, display.trail());
        assertFalse(display.recentImpact());
    }

    @Test void repeatedPacketsDoNotRestartImpactAndHealingStaysBounded() {
        var display = new BattleHealthDisplay();
        display.accept(60, 60);
        display.accept(20, 60);
        display.tick();
        display.accept(20, 60);
        assertEquals(1, display.impactAge());
        display.accept(50, 60);
        assertEquals(30, display.change());
        for (int i = 0; i < 80; i++) {
            display.tick();
            assertTrue(display.displayed() >= 0 && display.displayed() <= 1);
            assertTrue(display.trail() >= display.displayed() && display.trail() <= 1);
        }
        assertEquals(50F / 60, display.displayed());
    }

    @Test void profileResetAndInvalidHealthAreHandled() {
        var display = new BattleHealthDisplay();
        display.accept(60, 60);
        display.accept(10, 60);
        display.accept(120, 120);
        assertEquals(1F, display.displayed());
        assertFalse(display.recentImpact());
        assertThrows(IllegalArgumentException.class, () -> display.accept(-1, 60));
        assertThrows(IllegalArgumentException.class, () -> display.accept(61, 60));
        assertThrows(IllegalArgumentException.class, () -> display.accept(0, 0));
    }
}
