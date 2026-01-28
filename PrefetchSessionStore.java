package android.server.power.nextapp;

import java.util.*;

public final class PrefetchSessionStore {
    private final HashMap<String, Session> lastSessionByA = new HashMap<>();

    public void put(Session s) {
        lastSessionByA.put(s.pkgA, s);
    }

    public Session get(String pkgA) {
        return lastSessionByA.get(pkgA);
    }

    public void remove(String pkgA) {
        lastSessionByA.remove(pkgA);
    }

    public static final class Session {
        public final String pkgA;
        public final NextAppContext ctxAtA;
        public final long tsElapsed;
        public final List<String> candidates;
        public final List<String> chosen;

        public Session(String pkgA, NextAppContext ctxAtA, long tsElapsed,
                       List<String> candidates, List<String> chosen) {
            this.pkgA = pkgA;
            this.ctxAtA = ctxAtA;
            this.tsElapsed = tsElapsed;
            this.candidates = candidates == null ? Collections.emptyList() : new ArrayList<>(candidates);
            this.chosen = chosen == null ? Collections.emptyList() : new ArrayList<>(chosen);
        }
    }
}
