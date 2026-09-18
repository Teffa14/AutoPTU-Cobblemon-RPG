package io.autoptu.cobblemon.fabric.client;

import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import io.autoptu.cobblemon.fabric.network.PtuSheetPayloads;
import io.autoptu.cobblemon.fabric.ptu.PtuPokemonBinding;
import io.autoptu.cobblemon.fabric.ptu.PtuStarterPreview;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** A drawer inside the original starter/Summary/PC screen; never opens a second party or starter screen. */
public final class PtuNativeMenuClient {
    private static final Gson JSON = new Gson();
    private static final Map<Screen, Panel> PANELS = new WeakHashMap<>();
    private static int sequence;
    private static final int BACKGROUND = 0xFA17252E, HEADER = 0xFF223843, ACCENT = 0xFF8DC7B8;
    private PtuNativeMenuClient() {}
    private static final class Panel {
        boolean open;
        UUID selected;
        int request;
        long requestedAt;
        int scroll;
        int contentHeight;
        String status = "empty", origin = "existing";
        PtuPokemonBinding sheet;
        String starterKey;
        PtuStarterPreview preview;
    }
    record Bounds(int x, int y, int width, int height) {
        boolean contains(double px, double py) { return px >= x && py >= y && px < x + width && py < y + height; }
    }
    private record Line(Text text, int color) {}

    public static void register() {
        PtuSheetPayloads.register();
        ClientPlayNetworking.registerGlobalReceiver(PtuSheetPayloads.Response.ID, (response, context) -> context.client().execute(() -> {
            var screen = context.client().currentScreen;
            var panel = PANELS.get(screen);
            if (panel == null || !panel.open || response.sequence() != panel.request || !response.pokemonId().equals(panel.selected)) return;
            Pokemon current = selected(screen);
            if (current == null || !current.getUuid().equals(response.pokemonId())) return;
            panel.sheet = null;
            panel.status = response.status();
            panel.origin = response.origin();
            if (response.status().equals("ready")) {
                try {
                    var sheet = JSON.fromJson(response.json(), PtuPokemonBinding.class);
                    if (sheet == null || sheet.schema() != 1 || !sheet.pokemonId().equals(panel.selected)) throw new IllegalArgumentException();
                    panel.sheet = sheet;
                } catch (RuntimeException error) { panel.status = "invalid"; }
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PANELS.clear());
        ClientPlayNetworking.registerGlobalReceiver(PtuSheetPayloads.StarterResponse.ID, (response, context) -> context.client().execute(() -> {
            var screen = context.client().currentScreen;
            var panel = PANELS.get(screen);
            if (!(screen instanceof StarterSelectionScreen starter) || panel == null || !panel.open
                    || panel.request != response.sequence() || !Objects.equals(panel.starterKey, starterKey(starter))
                    || !starter.getCurrentCategory().getName().equals(response.category()) || starter.getCurrentSelection() != response.option()) return;
            panel.preview = null; panel.status = response.status();
            if (response.status().equals("ready")) {
                try {
                    panel.preview = JSON.fromJson(response.json(), PtuStarterPreview.class);
                    if (panel.preview == null || panel.preview.species() == null || panel.preview.abilities() == null) throw new IllegalArgumentException();
                } catch (RuntimeException error) { panel.preview = null; panel.status = "invalid"; }
            }
        }));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof Summary) && !(screen instanceof PCGUI) && !(screen instanceof StarterSelectionScreen)) return;
            Panel panel = PANELS.computeIfAbsent(screen, unused -> new Panel());
            var toggle = ButtonWidget.builder(Text.translatable("autoptu.menu.ptu"), button -> {
                panel.open = !panel.open;
                panel.scroll = 0;
                if (panel.open) request(client, screen, panel, true);
            }).dimensions(Math.max(4, width - 74), 5, 68, 20).build();
            Screens.getButtons(screen).add(toggle);
            // Fabric resets per-screen callbacks on reinitialization (including window resize).
            ScreenEvents.afterRender(screen).register((current, draw, mouseX, mouseY, delta) -> {
                if (panel.open) render(client, current, draw, panel);
            });
            ScreenEvents.afterTick(screen).register(current -> {
                if (!panel.open) return;
                if (current instanceof StarterSelectionScreen starter) {
                    if (!Objects.equals(panel.starterKey, starterKey(starter))) { panel.preview = null; panel.scroll = 0; panel.status = "loading"; }
                    long age = System.nanoTime() / 1_000_000L - panel.requestedAt;
                    if ((panel.preview == null && age > 500) || age > 5000) request(client, current, panel, false);
                    return;
                }
                Pokemon pokemon = selected(current);
                UUID id = pokemon == null ? null : pokemon.getUuid();
                if (!Objects.equals(id, panel.selected)) {
                    panel.sheet = null; panel.selected = id; panel.scroll = 0; panel.status = id == null ? "empty" : "loading";
                }
                long age = System.nanoTime() / 1_000_000L - panel.requestedAt;
                if (id != null && ((panel.sheet == null && age > 500) || age > 5000)) request(client, current, panel, false);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((current, x, y, button) -> {
                if (!panel.open) return true;
                if (x >= current.width - 74 && y <= 25) return true; // native toggle button
                if (!bounds(current.width, current.height).contains(x, y)) { panel.open = false; return true; }
                return false; // never click through the PTU sheet into native moves/release buttons
            });
            ScreenMouseEvents.allowMouseScroll(screen).register((current, x, y, horizontal, vertical) -> {
                if (!panel.open || !bounds(current.width, current.height).contains(x, y)) return true;
                panel.scroll = clampScroll(panel, current, panel.scroll - (int) (vertical * 22)); return false;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((current, x, y, button) ->
                    !panel.open || !bounds(current.width, current.height).contains(x, y));
            ScreenKeyboardEvents.allowKeyPress(screen).register((current, key, scan, modifiers) -> {
                if (!panel.open) return true;
                if (key == GLFW.GLFW_KEY_ESCAPE) panel.open = false;
                else if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_PAGE_DOWN) panel.scroll = clampScroll(panel, current, panel.scroll + 40);
                else if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_PAGE_UP) panel.scroll = clampScroll(panel, current, panel.scroll - 40);
                else if (key == GLFW.GLFW_KEY_HOME) panel.scroll = 0;
                else if (key == GLFW.GLFW_KEY_R) request(client, current, panel, false);
                return false;
            });
        });
    }

    private static Pokemon selected(Screen screen) {
        if (screen instanceof Summary summary) return summary.getSelectedPokemon$common();
        if (screen instanceof PCGUI pc) return pc.getPreviewPokemon$common();
        return null;
    }
    private static void request(MinecraftClient client, Screen screen, Panel panel, boolean opening) {
        if (screen instanceof StarterSelectionScreen starter) {
            long now = System.nanoTime() / 1_000_000L;
            if (!opening && now - panel.requestedAt < 250) return;
            panel.requestedAt = now;
            if (!Objects.equals(panel.starterKey, starterKey(starter))) panel.preview = null;
            panel.starterKey = starterKey(starter);
            if (!ClientPlayNetworking.canSend(PtuSheetPayloads.StarterRequest.ID)) { panel.preview = null; panel.status = "unsupported"; return; }
            panel.request = ++sequence;
            if (panel.preview == null) panel.status = "loading";
            ClientPlayNetworking.send(new PtuSheetPayloads.StarterRequest(starter.getCurrentCategory().getName(), starter.getCurrentSelection(), panel.request));
            return;
        }
        Pokemon pokemon = selected(screen);
        if (pokemon == null) { panel.selected = null; panel.sheet = null; panel.status = "empty"; return; }
        long now = System.nanoTime() / 1_000_000L;
        if (!opening && now - panel.requestedAt < 250) return;
        panel.requestedAt = now;
        if (!ClientPlayNetworking.canSend(PtuSheetPayloads.Request.ID)) { panel.sheet = null; panel.status = "unsupported"; return; }
        panel.selected = pokemon.getUuid();
        panel.request = ++sequence;
        panel.requestedAt = now;
        if (panel.sheet == null || !panel.sheet.pokemonId().equals(panel.selected)) { panel.sheet = null; panel.status = "loading"; }
        ClientPlayNetworking.send(new PtuSheetPayloads.Request(panel.selected, panel.request));
    }
    private static String starterKey(StarterSelectionScreen screen) { return screen.getCurrentCategory().getName() + ":" + screen.getCurrentSelection(); }
    static Bounds bounds(int width, int height) {
        int panelWidth = Math.min(330, Math.max(120, width - 12));
        return new Bounds(Math.max(6, width - panelWidth - 6), 30, panelWidth, Math.max(60, height - 36));
    }
    private static int clampScroll(Panel panel, Screen screen, int scroll) {
        int viewport = Math.max(1, bounds(screen.width, screen.height).height() - 48);
        return Math.clamp(scroll, 0, Math.max(0, panel.contentHeight - viewport));
    }
    private static void render(MinecraftClient client, Screen screen, DrawContext draw, Panel panel) {
        Bounds bounds = bounds(screen.width, screen.height);
        int x = bounds.x(), y = bounds.y(), width = bounds.width(), height = bounds.height();
        draw.getMatrices().push();
        draw.getMatrices().translate(0, 0, 400);
        draw.fill(x, y, x + width, y + height, BACKGROUND);
        draw.fill(x, y, x + width, y + 26, HEADER);
        draw.fill(x, y, x + 2, y + height, ACCENT);
        draw.drawTextWithShadow(client.textRenderer, Text.translatable("autoptu.menu.title"), x + 10, y + 8, ACCENT);
        draw.drawTextWithShadow(client.textRenderer, Text.translatable("autoptu.menu.controls"), x + 10, y + height - 12, 0xFFA8B9BF);
        List<Line> lines = lines(panel);
        int cursor = y + 34 - panel.scroll;
        int start = cursor;
        draw.enableScissor(x + 6, y + 29, x + width - 6, y + height - 18);
        for (var line : lines) {
            for (var wrapped : client.textRenderer.wrapLines(line.text(), width - 24)) {
                draw.drawText(client.textRenderer, wrapped, x + 10, cursor, line.color(), false);
                cursor += 12;
            }
            cursor += 5;
        }
        draw.disableScissor();
        panel.contentHeight = cursor - start;
        panel.scroll = clampScroll(panel, screen, panel.scroll);
        int viewport = height - 48;
        if (panel.contentHeight > viewport) {
            int thumb = Math.max(12, viewport * viewport / panel.contentHeight);
            int top = y + 30 + panel.scroll * Math.max(0, viewport - thumb) / Math.max(1, panel.contentHeight - viewport);
            draw.fill(x + width - 5, top, x + width - 3, top + thumb, ACCENT);
        }
        draw.getMatrices().pop();
    }
    private static List<Line> lines(Panel panel) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Text.translatable("autoptu.menu.scope"), 0xFFE4BD78));
        if (panel.preview != null) {
            var preview = panel.preview;
            lines.add(new Line(Text.translatable("autoptu.menu.preview", preview.species().name()), 0xFFFFFFFF));
            lines.add(new Line(Text.translatable("autoptu.menu.base"), ACCENT));
            for (String stat : List.of("hp", "attack", "defense", "special_attack", "special_defense", "speed")) {
                lines.add(new Line(Text.translatable("autoptu.menu.stat." + stat).append(": " + preview.species().baseStats().get(stat)), 0xFFE5EEF1));
            }
            lines.add(new Line(Text.translatable("autoptu.ptu.types", String.join(" / ", preview.species().types())), 0xFFB1C4CC));
            lines.add(new Line(Text.translatable("autoptu.ptu.pools", String.join(", ", preview.abilities().basic()),
                    String.join(", ", preview.abilities().advanced()), String.join(", ", preview.abilities().high())), 0xFFE5EEF1));
            lines.add(new Line(Text.translatable("autoptu.menu.preview_hint"), 0xFFB1C4CC));
            return lines;
        }
        if (panel.sheet == null) {
            lines.add(new Line(Text.translatable("autoptu.menu.state." + panel.status), 0xFFE5EEF1)); return lines;
        }
        var sheet = panel.sheet;
        lines.add(new Line(Text.literal(sheet.nativeSpecies() + " · " + sheet.nativeForm() + " · Lv " + sheet.level()), 0xFFFFFFFF));
        lines.add(new Line(Text.translatable("autoptu.menu.origin", Text.translatable("autoptu.menu.origin." + panel.origin)), 0xFFB1C4CC));
        if (sheet.species() != null) {
            var species = sheet.species();
            lines.add(new Line(Text.translatable("autoptu.menu.base"), ACCENT));
            for (String stat : List.of("hp", "attack", "defense", "special_attack", "special_defense", "speed")) {
                lines.add(new Line(Text.translatable("autoptu.menu.stat." + stat).append(": " + species.baseStats().get(stat)), 0xFFE5EEF1));
            }
            lines.add(new Line(Text.translatable("autoptu.ptu.types", String.join(" / ", species.types())), 0xFFB1C4CC));
            lines.add(new Line(Text.translatable("autoptu.ptu.capabilities", String.join(", ", species.capabilities())), 0xFFB1C4CC));
            lines.add(new Line(Text.translatable("autoptu.menu.moves"), ACCENT));
            for (var move : sheet.equippedMoves()) {
                if (move.data() == null) { lines.add(new Line(Text.translatable("autoptu.ptu.missing", move.nativeId()), 0xFFE4BD78)); continue; }
                var data = move.data();
                lines.add(new Line(Text.translatable("autoptu.ptu.move", data.name(), data.type(), data.category(), data.ac() == null ? "—" : data.ac(),
                        data.damageBase(), data.baseDice(), data.frequency(), data.range()), 0xFFE5EEF1));
                lines.add(new Line(Text.translatable("autoptu.ptu.learning." + move.learning().name().toLowerCase(Locale.ROOT)), 0xFFE4BD78));
                lines.add(new Line(Text.literal(data.effects()), 0xFFB1C4CC));
            }
            lines.add(new Line(Text.translatable("autoptu.menu.abilities"), ACCENT));
            lines.add(new Line(Text.translatable("autoptu.ptu.pools", String.join(", ", sheet.abilityPools().basic()),
                    String.join(", ", sheet.abilityPools().advanced()), String.join(", ", sheet.abilityPools().high())), 0xFFE5EEF1));
            var ability = sheet.nativeAbility();
            if (ability != null && ability.data() != null) {
                var data = ability.data();
                lines.add(new Line(Text.translatable("autoptu.ptu.ability", data.name(), data.frequency(), data.trigger()), 0xFFE5EEF1));
                lines.add(new Line(Text.literal(data.effect()), 0xFFB1C4CC));
                lines.add(new Line(Text.translatable("autoptu.ptu.ability_boundary"), 0xFFE4BD78));
            }
        }
        for (var issue : sheet.issues()) lines.add(new Line(Text.literal(issue), 0xFFE4BD78));
        return lines;
    }
}
