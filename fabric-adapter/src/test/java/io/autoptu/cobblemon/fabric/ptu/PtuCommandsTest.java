package io.autoptu.cobblemon.fabric.ptu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PtuCommandsTest {
    @Test void exposesPlayerOwnedDataCommandsWithoutBattleReplacementOrArbitraryPlayerTargets() {
        var root = PtuPokemonDataRuntime.commands().build();
        var ptu = root.getChild("ptu");
        assertNotNull(ptu);
        for (var command : new String[]{"party", "target", "move", "ability", "learnset", "sync", "status"}) {
            assertNotNull(ptu.getChild(command), command);
        }
        assertNull(root.getChild("battle"));
        assertNull(root.getChild("starter"));
        assertNull(ptu.getChild("party").getChild("player"));
        assertNotNull(ptu.getChild("party").getChild("slot"));
        assertNull(ptu.getChild("party").getCommand());
    }
}
