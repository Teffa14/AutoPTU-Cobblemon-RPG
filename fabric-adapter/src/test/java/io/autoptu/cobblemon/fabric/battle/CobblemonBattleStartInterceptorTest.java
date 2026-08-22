package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonBattleStartInterceptorTest {
    @Test
    void claimHandlerReceivesOnlyAdapterOwnedIdentityDto() throws Exception {
        Method claim = CobblemonBattleStartInterceptor.ClaimHandler.class
                .getDeclaredMethod("tryClaim", CobblemonBattleStartInterceptor.BattleStartSignal.class);

        assertEquals(boolean.class, claim.getReturnType());
        assertEquals(1, claim.getParameterCount());
        assertEquals(CobblemonBattleStartInterceptor.BattleStartSignal.class, claim.getParameterTypes()[0]);

        assertRecordContainsNoCobblemonOrMinecraftTypes(CobblemonBattleStartInterceptor.BattleStartSignal.class);
        assertRecordContainsNoCobblemonOrMinecraftTypes(CobblemonBattleStartInterceptor.ParticipantIdentity.class);
    }

    @Test
    void normalizesAndValidatesOpaqueBattleAndParticipantIdentities() {
        var participant = new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1,
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                "  actor-1  ",
                List.of(" pokemon-1 ", "pokemon-2")
        );
        var signal = new CobblemonBattleStartInterceptor.BattleStartSignal(
                "  battle-123  ",
                List.of(participant)
        );

        assertEquals("battle-123", signal.cobblemonBattleId());
        assertEquals("actor-1", signal.participants().getFirst().actorId());
        assertEquals(List.of("pokemon-1", "pokemon-2"), signal.participants().getFirst().pokemonIds());

        assertThrows(IllegalArgumentException.class,
                () -> new CobblemonBattleStartInterceptor.BattleStartSignal("   ", List.of(participant)));
        assertThrows(IllegalArgumentException.class,
                () -> new CobblemonBattleStartInterceptor.BattleStartSignal("battle", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CobblemonBattleStartInterceptor.ParticipantIdentity(
                        3,
                        CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                        "actor",
                        List.of("pokemon")));
        assertThrows(IllegalArgumentException.class,
                () -> new CobblemonBattleStartInterceptor.ParticipantIdentity(
                        1,
                        CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                        "actor",
                        List.of("pokemon", "pokemon")));
    }

    private static void assertRecordContainsNoCobblemonOrMinecraftTypes(Class<?> recordType) {
        assertTrue(recordType.isRecord());
        for (RecordComponent component : recordType.getRecordComponents()) {
            String signature = component.getGenericType().getTypeName();
            assertTrue(!signature.startsWith("com.cobblemon.") && !signature.startsWith("net.minecraft."),
                    () -> recordType.getSimpleName() + " leaked platform type through " + component.getName());
        }
        assertTrue(Arrays.stream(recordType.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .noneMatch(type -> type.getName().startsWith("com.cobblemon.") || type.getName().startsWith("net.minecraft.")));
    }
}
