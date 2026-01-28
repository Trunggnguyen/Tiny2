package android.server.power.nextapp;

public final class NextAppServiceFacade {
    private final NextAppPredictor predictor;

    public NextAppServiceFacade(NextAppPredictor predictor) {
        this.predictor = predictor;
    }

    // 1) Called when A is allowed to run (unsuspended)
    public NextAppDecision suggestPrefetchApps(String pkgA, NextAppContext ctx, long nowElapsedMs) {
        return predictor.onAllowedToRun(pkgA, ctx, nowElapsedMs);
    }

    // 2) Called when foreground changes (A->B)
    public void reportForegroundTransition(String prevA, String nowB, NextAppContext ctxAtPrevA, long nowElapsedMs) {
        predictor.onForegroundChanged(prevA, nowB, ctxAtPrevA, nowElapsedMs);
    }

    // 3) TTL outcome
    public void reportPrefetchOutcome(String pkgA, String pkgB, NextAppContext ctxAtA,
                                      boolean used, long nowElapsedMs) {
        predictor.onPrefetchOutcome(pkgA, pkgB, ctxAtA, used, nowElapsedMs);
    }
}
