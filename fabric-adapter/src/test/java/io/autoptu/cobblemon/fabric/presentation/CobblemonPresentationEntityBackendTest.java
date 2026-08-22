package io.autoptu.cobblemon.fabric.presentation;

import io.autoptu.cobblemon.battlecore.PresentationEntityPlatformBackend;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonPresentationEntityBackendTest {
    @Test
    void implementsPresentationOnlyPlatformBoundary() {
        assertTrue(PresentationEntityPlatformBackend.class
                .isAssignableFrom(CobblemonPresentationEntityBackend.class));

        var methodNames = Arrays.stream(CobblemonPresentationEntityBackend.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> !name.startsWith("lambda$"))
                .sorted()
                .toList();

        assertTrue(methodNames.contains("relocate"));
        assertTrue(methodNames.contains("animateMove"));
        assertTrue(methodNames.contains("projectDisplayedHealth"));
        assertTrue(methodNames.contains("showCue"));
        assertEquals(4, methodNames.stream().distinct().count());
    }
}
