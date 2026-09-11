package io.autoptu.cobblemon.fabric.rpg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FabricBagRuntimeTest {
    @Test
    void formatsServerObservedBlockTargetWithoutInventingPtuLegality() {
        assertEquals("block:12,64,-5", FabricBagRuntime.blockTargetId(12, 64, -5));
    }
}
