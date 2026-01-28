package android.server.power.nextapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NextAppPolicy {
    private final NextAppConfig cfg;

    public NextAppPolicy(NextAppConfig cfg) {
        this.cfg = cfg;
    }

    public NextAppDecision decide(List<ScoredCandidate> scored, NextAppContext ctx) {
        if (scored == null || scored.isEmpty()) {
            return new NextAppDecision(Collections.emptyList(), 0f);
        }
        scored.sort((a, b) -> Float.compare(b.score, a.score));
        float s1 = scored.get(0).score;
        float s2 = scored.size() >= 2 ? scored.get(1).score : 0f;

        float T = cfg.threshold;
        if (ctx != null && ctx.maxPowerMode && ctx.batteryBucket <= 1) {
            T = Math.min(0.85f, T + 0.10f);
        }

        ArrayList<String> chosen = new ArrayList<>();
        if (s1 >= T && (s1 - s2) >= cfg.gapDelta) {
            chosen.add(scored.get(0).pkg);
            if (cfg.prefetchTopK >= 2 && scored.size() >= 2 && s2 >= (T + 0.05f)) {
                chosen.add(scored.get(1).pkg);
            }
        }
        return new NextAppDecision(chosen, s1);
    }

    public static final class ScoredCandidate {
        public final String pkg;
        public final float score;
        public ScoredCandidate(String pkg, float score) { this.pkg = pkg; this.score = score; }
    }
}
