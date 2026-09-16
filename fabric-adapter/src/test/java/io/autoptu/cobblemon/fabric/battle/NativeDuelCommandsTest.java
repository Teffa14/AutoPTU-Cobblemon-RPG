package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeDuelCommandsTest {
    @Test void acceptingRequiresExplicitTokenAndChallengeRequiresRecipient() {
        var root = NativeCobblemonDuelRuntime.commands().build();
        var duel = root.getChild("duel");
        assertNotNull(duel.getCommand());
        assertNull(duel.getChild("accept").getCommand());
        assertNotNull(duel.getChild("accept").getChild("token").getCommand());
        assertNull(duel.getChild("challenge").getCommand());
        assertNotNull(duel.getChild("challenge").getChild("player").getCommand());
        assertNotNull(duel.getChild("cancel").getCommand());
        assertNull(root.getChild("accept"));
    }
}
