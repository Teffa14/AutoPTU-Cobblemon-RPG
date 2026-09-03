package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Fixed server-authored Ouros map anchors. Coordinates are presentation/world-placement facts only. */
public final class CanonicalWorldMapCatalogue {
    public static final CanonicalWorldMapCatalogue DEFAULT = new CanonicalWorldMapCatalogue(List.of(
            new Site("ouros.marea.puerto_bruma", "Puerto Bruma", SiteKind.SETTLEMENT, "minecraft:overworld", 2048, 72, 2048, 54.0D, null),
            new Site("ouros.marea.bruma_market_hall", "Bruma Market Hall", SiteKind.FACILITY, "minecraft:overworld", 2052, 72, 2042, 12.0D, "ouros.marea.puerto_bruma"),
            new Site("ouros.marea.marea_field_office", "Marea Field Office", SiteKind.FACILITY, "minecraft:overworld", 2034, 72, 2040, 10.0D, "ouros.marea.puerto_bruma"),
            new Site("ouros.marea.tideglass_archive", "Tideglass Archive", SiteKind.FACILITY, "minecraft:overworld", 2035, 72, 2061, 10.0D, "ouros.marea.puerto_bruma"),
            new Site("ouros.marea.bruma_battle_yard", "Bruma Battle Yard", SiteKind.FACILITY, "minecraft:overworld", 2070, 72, 2060, 14.0D, "ouros.marea.puerto_bruma"),
            new Site("ouros.marea.ferry_landing", "Puerto Bruma Ferry Landing", SiteKind.FACILITY, "minecraft:overworld", 2066, 69, 2021, 12.0D, "ouros.marea.puerto_bruma"),
            new Site("ouros.marea.clinic", "Puerto Bruma Clinic", SiteKind.FACILITY, "minecraft:overworld", 2055, 72, 2066, 10.0D, "ouros.marea.puerto_bruma"),
            new Site("ouros.marea.sendero_vidrio", "Sendero del Vidrio", SiteKind.ROUTE, "minecraft:overworld", 2056, 77, 2120, 22.0D, null),
            new Site("ouros.marea.sendero_crossing", "Sendero Seasonal Crossing", SiteKind.ROUTE_POINT, "minecraft:overworld", 2072, 79, 2154, 12.0D, "ouros.marea.sendero_vidrio"),
            new Site("ouros.marea.loma_windbreak", "Loma Clara Windbreak", SiteKind.ROUTE_POINT, "minecraft:overworld", 2060, 83, 2190, 14.0D, "ouros.marea.loma_clara"),
            new Site("ouros.marea.loma_clara", "Loma Clara", SiteKind.SETTLEMENT, "minecraft:overworld", 2048, 86, 2224, 44.0D, null),
            new Site("ouros.marea.loma_storehouse", "Loma Clara Cooperative Storehouse", SiteKind.FACILITY, "minecraft:overworld", 2038, 86, 2217, 10.0D, "ouros.marea.loma_clara"),
            new Site("ouros.marea.loma_communal_kitchen", "Loma Clara Communal Kitchen", SiteKind.FACILITY, "minecraft:overworld", 2058, 86, 2218, 10.0D, "ouros.marea.loma_clara"),
            new Site("ouros.marea.loma_field_school", "Loma Clara Field School", SiteKind.FACILITY, "minecraft:overworld", 2038, 86, 2234, 10.0D, "ouros.marea.loma_clara"),
            new Site("ouros.marea.estacion_mirador", "Estacion Mirador", SiteKind.STATION, "minecraft:overworld", 2144, 96, 2160, 30.0D, null),
            new Site("ouros.marea.mirador_weather_mast", "Mirador Weather Mast", SiteKind.FACILITY, "minecraft:overworld", 2152, 96, 2152, 8.0D, "ouros.marea.estacion_mirador"),
            new Site("ouros.marea.mirador_transect", "Mirador Transect Trailhead", SiteKind.ROUTE_POINT, "minecraft:overworld", 2133, 95, 2168, 9.0D, "ouros.marea.estacion_mirador")
    ));

    private final Map<String, Site> sites;

    public CanonicalWorldMapCatalogue(List<Site> sites) {
        Objects.requireNonNull(sites, "sites");
        LinkedHashMap<String, Site> indexed = new LinkedHashMap<>();
        for (Site site : sites) {
            Objects.requireNonNull(site, "site");
            if (indexed.putIfAbsent(site.siteId(), site) != null) {
                throw new IllegalArgumentException("duplicate siteId: " + site.siteId());
            }
        }
        this.sites = Map.copyOf(indexed);
    }

    public Optional<Site> site(String siteId) {
        if (siteId == null || siteId.isBlank()) return Optional.empty();
        return Optional.ofNullable(sites.get(siteId.strip()));
    }

    public List<Site> sites() {
        return List.copyOf(sites.values());
    }

    public List<Site> childrenOf(String parentSiteId) {
        if (parentSiteId == null || parentSiteId.isBlank()) return List.of();
        String normalized = parentSiteId.strip();
        return sites.values().stream().filter(site -> normalized.equals(site.parentSiteId())).toList();
    }

    public enum SiteKind {
        SETTLEMENT,
        FACILITY,
        ROUTE,
        ROUTE_POINT,
        STATION,
        DUNGEON,
        LANDMARK
    }

    public record Site(
            String siteId,
            String displayName,
            SiteKind kind,
            String dimensionId,
            int x,
            int y,
            int z,
            double discoveryRadius,
            String parentSiteId
    ) {
        public Site {
            siteId = requireText(siteId, "siteId");
            displayName = requireText(displayName, "displayName");
            kind = Objects.requireNonNull(kind, "kind");
            dimensionId = requireText(dimensionId, "dimensionId");
            if (!Double.isFinite(discoveryRadius) || discoveryRadius <= 0.0D) {
                throw new IllegalArgumentException("discoveryRadius must be positive and finite");
            }
            if (parentSiteId != null) parentSiteId = requireText(parentSiteId, "parentSiteId");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}