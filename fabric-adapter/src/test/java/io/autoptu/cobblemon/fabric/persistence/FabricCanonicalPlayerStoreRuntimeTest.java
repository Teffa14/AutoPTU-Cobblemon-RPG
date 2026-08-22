package io.autoptu.cobblemon.fabric.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricCanonicalPlayerStoreRuntimeTest {
    @TempDir
    Path tempDir;

    @Test
    void canonicalStorageLivesUnderTheWorldSaveRoot() {
        assertEquals(
                tempDir.resolve("autoptu").resolve("canonical-state").normalize(),
                FabricCanonicalPlayerStoreRuntime.storageRoot(tempDir)
        );
    }
}
