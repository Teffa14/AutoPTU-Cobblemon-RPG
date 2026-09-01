package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Canonical Ouros NPC identity and ordinary-world placement metadata. */
public final class CanonicalNpcCatalogue {
    public static final CanonicalNpcCatalogue DEFAULT = new CanonicalNpcCatalogue(List.of(
            new Npc(
                    "ouros.npc.mara_veyra", "Mara Veyra", "Marea Field Office coordinator",
                    "ouros.marea.puerto_bruma", "ouros.marea.marea_field_office",
                    List.of("Commander", "Survivalist"),
                    "corviknight", "Kite",
                    "Coordinates field reports, route checks, wildlife incidents and practical assistance."
            ),
            new Npc(
                    "ouros.npc.ivo_serrat", "Ivo Serrat", "Bruma Market Hall cook and purchasing lead",
                    "ouros.marea.puerto_bruma", "ouros.marea.bruma_market_hall",
                    List.of("Chef", "Hobbyist"),
                    "greedent", "Pepa",
                    "Runs purchasing and communal-kitchen work while tracking substitutions and supplier irregularities."
            ),
            new Npc(
                    "ouros.npc.nerea_sol", "Dr. Nerea Sol", "Estacion Mirador lead field researcher",
                    "ouros.marea.estacion_mirador", "ouros.marea.estacion_mirador",
                    List.of("Researcher", "Chronicler"),
                    "heliolisk", "Lumen",
                    "Maintains longitudinal ecological and weather observations and revises conclusions when evidence changes."
            ),
            new Npc(
                    "ouros.npc.taro_min", "Taro Min", "Tideglass Archive custodian and interviewer",
                    "ouros.marea.puerto_bruma", "ouros.marea.tideglass_archive",
                    List.of("Chronicler", "Mentor"),
                    "noctowl", "Margin",
                    "Preserves route surveys, editions and contradictory testimony without treating memory as automatic fact."
            ),
            new Npc(
                    "ouros.npc.sela_orrin", "Sela Orrin", "Bruma Battle Yard manager and senior Trainer",
                    "ouros.marea.puerto_bruma", "ouros.marea.bruma_battle_yard",
                    List.of("Ace Trainer", "Duelist"),
                    "falinks", "Rook",
                    "Runs local training and audited battles while treating rematches as evidence of change."
            ),
            new Npc(
                    "ouros.npc.lia_morn", "Lia Morn", "Puerto Bruma dock coordinator",
                    "ouros.marea.puerto_bruma", "ouros.marea.ferry_landing",
                    List.of("Commander", "Rider"),
                    "pelipper", "Gale",
                    "Coordinates berths, arrival records and unloading windows."
            ),
            new Npc(
                    "ouros.npc.mina_cors", "Mina Cors", "Ferry pilot and operator",
                    "ouros.marea.puerto_bruma", "ouros.marea.ferry_landing",
                    List.of("Rider", "Survivalist"),
                    "floatzel", "Wake",
                    "Runs short coastal ferry services and records practical route observations."
            ),
            new Npc(
                    "ouros.npc.oren_vale", "Oren Vale", "Puerto Bruma clinic practitioner",
                    "ouros.marea.puerto_bruma", "ouros.marea.clinic",
                    List.of("Medic"),
                    "audino", "Mell",
                    "Handles routine care and actual care cases without inventing mechanical healing outcomes."
            ),
            new Npc(
                    "ouros.npc.teo_lark", "Teo Lark", "Repairer and tool maintainer",
                    "ouros.marea.puerto_bruma", "ouros.marea.puerto_bruma",
                    List.of("Hobbyist", "Researcher"),
                    "magnemite", "Pin",
                    "Maintains ordinary carts, lamps, field instruments and fixtures."
            ),
            new Npc(
                    "ouros.npc.alba_rios", "Alba Rios", "Loma Clara producer and cooperative delegate",
                    "ouros.marea.loma_clara", "ouros.marea.loma_clara",
                    List.of("Survivalist", "Chef"),
                    "appletun", "Miga",
                    "Manages one mixed-crop holding and represents one producer voice in cooperative meetings."
            ),
            new Npc(
                    "ouros.npc.brin_havel", "Brin Havel", "Loma Clara cooperative storehouse clerk",
                    "ouros.marea.loma_clara", "ouros.marea.loma_storehouse",
                    List.of("Commander", "Hobbyist"),
                    "munchlax", "Ledger",
                    "Maintains intake, dispatch preparation and storage records."
            ),
            new Npc(
                    "ouros.npc.jo_venn", "Jo Venn", "Loma Clara field-school instructor",
                    "ouros.marea.loma_clara", "ouros.marea.loma_field_school",
                    List.of("Mentor", "Researcher"),
                    "budew", "Sprig",
                    "Teaches observation, cultivation records and safe field practice."
            ),
            new Npc(
                    "ouros.npc.ema_rey", "Ema Rey", "Estacion Mirador observation technician",
                    "ouros.marea.estacion_mirador", "ouros.marea.estacion_mirador",
                    List.of("Researcher", "Backpacker"),
                    "minccino", "Dust",
                    "Checks instruments, performs transects and prepares field notes under project protocols."
            ),
            new Npc(
                    "ouros.npc.pia_min", "Pia Min", "Tideglass archive assistant and document courier",
                    "ouros.marea.puerto_bruma", "ouros.marea.tideglass_archive",
                    List.of("Chronicler", "Backpacker"),
                    "fletchling", "Redline",
                    "Handles circulation work, copies, deliveries and source retrieval."
            ),
            new Npc(
                    "ouros.npc.jace_orrin", "Jace Orrin", "Bruma Battle Yard junior Trainer",
                    "ouros.marea.puerto_bruma", "ouros.marea.bruma_battle_yard",
                    List.of("Athlete", "Ace Trainer"),
                    "machop", "Knuckle",
                    "Assists training sessions, maintains yard fixtures and pursues stronger competition."
            )
    ));

    private final Map<String, Npc> npcs;

    public CanonicalNpcCatalogue(List<Npc> npcs) {
        Objects.requireNonNull(npcs, "npcs");
        LinkedHashMap<String, Npc> indexed = new LinkedHashMap<>();
        for (Npc npc : npcs) {
            Objects.requireNonNull(npc, "npc");
            if (indexed.putIfAbsent(npc.npcId(), npc) != null) {
                throw new IllegalArgumentException("duplicate npcId: " + npc.npcId());
            }
        }
        this.npcs = Map.copyOf(indexed);
    }

    public Optional<Npc> npc(String npcId) {
        if (npcId == null || npcId.isBlank()) return Optional.empty();
        return Optional.ofNullable(npcs.get(npcId.strip()));
    }

    public List<Npc> npcs() {
        return List.copyOf(npcs.values());
    }

    public List<Npc> residentsOf(String homeSiteId) {
        if (homeSiteId == null || homeSiteId.isBlank()) return List.of();
        String normalized = homeSiteId.strip();
        return npcs.values().stream().filter(npc -> npc.homeSiteId().equals(normalized)).toList();
    }

    public record Npc(
            String npcId,
            String displayName,
            String role,
            String homeSiteId,
            String workSiteId,
            List<String> classConcepts,
            String companionSpeciesId,
            String companionName,
            String ordinaryResponsibility
    ) {
        public Npc {
            npcId = requireText(npcId, "npcId");
            displayName = requireText(displayName, "displayName");
            role = requireText(role, "role");
            homeSiteId = requireText(homeSiteId, "homeSiteId");
            workSiteId = requireText(workSiteId, "workSiteId");
            classConcepts = copyTextList(classConcepts, "classConcepts");
            if (classConcepts.isEmpty()) throw new IllegalArgumentException("classConcepts cannot be empty");
            companionSpeciesId = requireText(companionSpeciesId, "companionSpeciesId");
            companionName = requireText(companionName, "companionName");
            ordinaryResponsibility = requireText(ordinaryResponsibility, "ordinaryResponsibility");
        }
    }

    private static List<String> copyTextList(List<String> values, String field) {
        Objects.requireNonNull(values, field);
        return values.stream().map(value -> requireText(value, field)).toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
