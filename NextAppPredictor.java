package android.server.power.nextapp;

import android.util.Slog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.server.power.proto.MarkovModelProto.MarkovModel;
import android.server.power.proto.MarkovModelProto.MarkovRow;
import android.server.power.proto.MarkovModelProto.MarkovEdge;

public final class NextAppPredictor {
    private static final String TAG = "NextAppPredictor";
    private static final int MODEL_VERSION = 1;

    private final NextAppConfig cfg;
    private final MarkovTransitionTable markov;
    private final MarkovProtoStore markovStore;
    private final PrefetchSessionStore sessions = new PrefetchSessionStore();
    private final NextAppPolicy policy;

    private final OnlineLogisticModel lr; // may be null
    private int updatesSinceCheckpoint = 0;

    public NextAppPredictor(NextAppConfig cfg, MarkovProtoStore markovStore) {
        this.cfg = cfg;
        this.markovStore = markovStore;
        this.markov = new MarkovTransitionTable(cfg.markovTopMPerA, cfg.markovDecay);
        this.policy = new NextAppPolicy(cfg);
        this.lr = cfg.enableLr ? new OnlineLogisticModel(cfg.hashDimPow2, cfg.lr, cfg.l2) : null;
    }

    // ===== Lifecycle =====

    public void loadMarkovFromDiskOrEmpty(long nowElapsedMs) {
        MarkovModel m = markovStore.readOrNull();
        if (m == null || m.getVersion() != MODEL_VERSION) {
            Slog.i(TAG, "No markov or version mismatch; starting empty");
            return;
        }
        for (MarkovRow row : m.getRowsList()) {
            ArrayList<MarkovTransitionTable.Edge> edges = new ArrayList<>();
            for (MarkovEdge e : row.getEdgesList()) {
                edges.add(new MarkovTransitionTable.Edge(e.getDstPkg(), e.getWeight()));
            }
            markov.putRow(row.getSrcPkg(), edges);
        }
        Slog.i(TAG, "Loaded markov rows=" + m.getRowsCount());
    }

    public void checkpointMarkovToDisk(long nowElapsedMs) {
        Map<String, List<MarkovTransitionTable.Edge>> snap = markov.snapshotForPersist();
        MarkovModel.Builder b = MarkovModel.newBuilder()
                .setVersion(MODEL_VERSION)
                .setLastUpdateElapsedMs(nowElapsedMs);

        for (Map.Entry<String, List<MarkovTransitionTable.Edge>> e : snap.entrySet()) {
            MarkovRow.Builder rb = MarkovRow.newBuilder().setSrcPkg(e.getKey());
            for (MarkovTransitionTable.Edge edge : e.getValue()) {
                rb.addEdges(MarkovEdge.newBuilder()
                        .setDstPkg(edge.dst)
                        .setWeight(edge.weight)
                        .build());
            }
            b.addRows(rb.build());
        }
        markovStore.write(b.build());
        updatesSinceCheckpoint = 0;
    }

    // ===== Main APIs =====

    /** Call when your mechanism allows pkgA to run (unsuspend). */
    public NextAppDecision onAllowedToRun(String pkgA, NextAppContext ctx, long nowElapsedMs) {
        if (!cfg.enable) return new NextAppDecision(List.of(), 0f);

        List<String> candidates = markov.topN(pkgA, cfg.candidateTopN);

        ArrayList<NextAppPolicy.ScoredCandidate> scored = new ArrayList<>(candidates.size());
        for (String pkgB : candidates) {
            float s;
            if (lr != null) {
                s = lr.score(pkgA, pkgB, ctx);
            } else {
                // If no LR, approximate score from Markov rank: 1.0, 0.9, 0.8 ...
                // Better: compute normalized probability if you store sums; keeping simple here.
                s = 0.5f; // neutral; policy threshold should be tuned if LR disabled
            }
            scored.add(new NextAppPolicy.ScoredCandidate(pkgB, s));
        }

        NextAppDecision d = policy.decide(scored, ctx);

        sessions.put(new PrefetchSessionStore.Session(
                pkgA, ctx, nowElapsedMs, candidates, d.prefetchPkgs
        ));

        return d;
    }

    /** Call when actual foreground transition happens prev=A -> now=B */
    public void onForegroundChanged(String prevA, String nowB, NextAppContext ctxAtPrevA, long nowElapsedMs) {
        if (!cfg.enable) return;
        if (prevA == null || nowB == null || prevA.equals(nowB)) return;

        // update Markov always
        markov.update(prevA, nowB);
        updatesSinceCheckpoint++;

        // LR training
        if (lr != null) {
            lr.update(prevA, nowB, ctxAtPrevA, 1); // positive

            PrefetchSessionStore.Session s = sessions.get(prevA);
            if (s != null) {
                int neg = 0;
                for (String cand : s.candidates) {
                    if (cand.equals(nowB)) continue;
                    lr.update(prevA, cand, s.ctxAtA, 0);
                    neg++;
                    if (neg >= cfg.hardNegPerPos) break;
                }
            }
        }

        maybeCheckpoint(nowElapsedMs);
    }

    /** Call when prefetch outcome known (TTL expired or user opened) */
    public void onPrefetchOutcome(String pkgA, String pkgB, NextAppContext ctxAtA, boolean used, long nowElapsedMs) {
        if (!cfg.enable || lr == null) return;
        lr.update(pkgA, pkgB, ctxAtA, used ? 1 : 0);
        updatesSinceCheckpoint++;
        maybeCheckpoint(nowElapsedMs);
    }

    private void maybeCheckpoint(long nowElapsedMs) {
        if (updatesSinceCheckpoint >= cfg.checkpointEveryNUpdates) {
            checkpointMarkovToDisk(nowElapsedMs);
        }
    }
}
