package io.autoptu.cobblemon.battlecore;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleMenuModelTest {
    @Test void mapsCellToExactServerEntryAndKeepsOverlappingTargetsDistinct() {
        var cell = new BattleGridCoordinate(3, 2);
        var first = new BattleChoiceMenuService.Entry("a", "enemy a", BattleChoiceMenuService.EntryKind.ATTACK, cell, "arc", "enemy-a");
        var second = new BattleChoiceMenuService.Entry("b", "enemy b", BattleChoiceMenuService.EntryKind.ATTACK, cell, "arc", "enemy-b");
        var model = new BattleMenuModel(List.of(first, second));
        assertEquals(List.of(first, second), model.at("attack:arc", cell));
        assertTrue(model.at("attack:other", cell).isEmpty());
        assertEquals(second, model.choice("b").orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new BattleMenuModel(List.of(first, first)));
    }

    @Test void noAnchorIsInventedForSelfAndFieldChoices() {
        var self = new BattleChoiceMenuService.Entry("a", "focus", BattleChoiceMenuService.EntryKind.ATTACK, null, "focus", "");
        var model = new BattleMenuModel(List.of(self));
        assertTrue(model.window().isEmpty());
        assertEquals(List.of(self), model.unanchored("attack:focus"));
        assertTrue(model.at("attack:focus", new BattleGridCoordinate(0, 0)).isEmpty());
        assertTrue(model.page("attack:focus", Integer.MAX_VALUE, 8).isEmpty());
    }
}
