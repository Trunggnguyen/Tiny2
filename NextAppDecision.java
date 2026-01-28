package android.server.power.nextapp;

import java.util.List;

public final class NextAppDecision {
    public final List<String> prefetchPkgs; // Bs
    public final float topScore;            // for debugging
    public NextAppDecision(List<String> prefetchPkgs, float topScore) {
        this.prefetchPkgs = prefetchPkgs;
        this.topScore = topScore;
    }
}
