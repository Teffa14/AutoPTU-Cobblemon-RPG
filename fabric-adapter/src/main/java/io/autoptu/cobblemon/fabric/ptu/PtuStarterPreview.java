package io.autoptu.cobblemon.fabric.ptu;

/** No UUID, grant, ability roll or state mutation: this describes a server-configured starter option. */
public record PtuStarterPreview(String catalogRevision, PtuDataCatalog.Species species,
                                PtuDataCatalog.Pools abilities, Integer configuredLevel) {}
