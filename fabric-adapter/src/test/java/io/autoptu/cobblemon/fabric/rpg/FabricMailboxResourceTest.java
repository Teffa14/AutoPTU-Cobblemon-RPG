package io.autoptu.cobblemon.fabric.rpg;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricMailboxResourceTest {
    private static final String LOOT_TABLE =
            "/data/autoptu_cobblemon_rpg_fabric_adapter/loot_table/blocks/ouros_mailbox.json";

    @Test
    void mailboxHasSelfDropLootTable() throws IOException {
        var resource = FabricMailboxResourceTest.class.getResourceAsStream(LOOT_TABLE);
        assertNotNull(resource, "registered mailbox must have a block loot table");
        try (resource) {
            String json = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"type\": \"minecraft:block\""));
            assertTrue(json.contains(
                    "\"name\": \"autoptu_cobblemon_rpg_fabric_adapter:ouros_mailbox\""));
        }
    }
}
