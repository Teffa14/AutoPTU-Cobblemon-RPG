package io.autoptu.cobblemon.fabric;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class NativeEntrypointsTest {
    @Test void onlyModeDispatchersAreAutomaticallyLoaded() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("fabric.mod.json")) {
            assertNotNull(stream);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var entrypoints = json.getAsJsonObject("entrypoints");
            assertEquals(1, entrypoints.getAsJsonArray("main").size());
            assertEquals("io.autoptu.cobblemon.fabric.FabricGameplayEntrypoint", entrypoints.getAsJsonArray("main").get(0).getAsString());
            assertEquals(1, entrypoints.getAsJsonArray("client").size());
            assertEquals("io.autoptu.cobblemon.fabric.client.FabricGameplayClientEntrypoint", entrypoints.getAsJsonArray("client").get(0).getAsString());
            assertTrue(entrypoints.getAsJsonArray("autoptu:experimental_main").size() > 1);
            assertTrue(entrypoints.getAsJsonArray("autoptu:experimental_client").size() > 1);
        }
    }
}
