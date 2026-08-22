package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobblemonBattleStartInterceptorTest {
    @Test
    void exposesOnlyOpaqueBattleIdentityToClaimHandler() throws Exception {
        Method claim = CobblemonBattleStartInterceptor.ClaimHandler.class
                .getDeclaredMethod("tryClaim", CobblemonBattleStartInterceptor.BattleStartSignal.class);

        assertEquals(boolean.class, claim.getReturnType());
        assertEquals(1, claim.getParameterCount());
        assertEquals(CobblemonBattleStartInterceptor.BattleStartSignal.class, claim.getParameterTypes()[0]);
    }

    @Test
    void normalizesAndValidatesOpaqueBattleIdentity() {
        var signal = new CobblemonBattleStartInterceptor.BattleStartSignal("  battle-123  ");
        assertEquals("battle-123", signal.cobblemonBattleId());

        assertThrows(IllegalArgumentException.class,
                () -> new CobblemonBattleStartInterceptor.BattleStartSignal("   "));
        assertThrows(IllegalArgumentException.class,
                () -> new CobblemonBattleStartInterceptor.BattleStartSignal(null));
    }
}
