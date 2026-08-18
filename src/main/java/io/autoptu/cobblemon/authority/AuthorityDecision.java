package io.autoptu.cobblemon.authority;

import java.util.List;

public record AuthorityDecision(boolean allowed, List<String> reasons, long stateRevision) {
    public AuthorityDecision {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static AuthorityDecision allow(long revision) {
        return new AuthorityDecision(true, List.of(), revision);
    }

    public static AuthorityDecision deny(List<String> reasons, long revision) {
        return new AuthorityDecision(false, reasons, revision);
    }
}
