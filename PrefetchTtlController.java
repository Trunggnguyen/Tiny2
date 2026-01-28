package android.server.power.nextapp;

public interface PrefetchTtlController {
    /** Called when predictor returns B list; you unsuspend them and start TTL. */
    void onPrefetchChosen(String pkgA, NextAppContext ctxAtA,
                          String pkgB, int ttlMs);

    /** Called when TTL expired and B not opened. */
    void onTtlExpired(String pkgA, NextAppContext ctxAtA, String pkgB);
}
