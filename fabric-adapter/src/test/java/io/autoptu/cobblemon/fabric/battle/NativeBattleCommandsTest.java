package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeBattleCommandsTest {
    @Test void realBattleEntryIsUnderBattleAndNoDemoIsRegistered() {
        var root = NativeCobblemonBattleRuntime.commands().build();
        var battle = root.getChild("battle");
        assertNotNull(battle);
        for (String command : new String[]{"help", "wild", "status", "practice", "menu"}) {
            assertNotNull(battle.getChild(command), command);
            assertNotNull(battle.getChild(command).getCommand(), command);
        }
        assertNull(root.getChild("wild"));
        assertNull(root.getChild("admin"));
        assertNull(root.getChild("testbattle"));
        assertNull(battle.getChild("play"));
        assertNull(battle.getChild("choose"));
    }
}
