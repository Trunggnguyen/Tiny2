package android.server.power.nextapp;

import java.util.*;

public final class MarkovTransitionTable {
    private final int mTopM;
    private final float mDecay;

    // A -> row
    private final HashMap<String, Row> mRows = new HashMap<>();

    public MarkovTransitionTable(int topM, float decay) {
        mTopM = topM;
        mDecay = decay;
    }

    public void update(String srcA, String dstB) {
        Row row = mRows.get(srcA);
        if (row == null) {
            row = new Row(mTopM, mDecay);
            mRows.put(srcA, row);
        }
        row.update(dstB);
    }

    public List<String> topN(String srcA, int n) {
        Row row = mRows.get(srcA);
        if (row == null) return Collections.emptyList();
        return row.topN(n);
    }

    public void putRow(String srcA, List<Edge> edges) {
        Row row = new Row(mTopM, mDecay);
        for (Edge e : edges) row.put(e.dst, e.weight);
        row.prune();
        mRows.put(srcA, row);
    }

    public Map<String, List<Edge>> snapshotForPersist() {
        HashMap<String, List<Edge>> out = new HashMap<>();
        for (Map.Entry<String, Row> e : mRows.entrySet()) {
            out.put(e.getKey(), e.getValue().snapshotEdgesSorted());
        }
        return out;
    }

    // ===== inner =====
    public static final class Edge {
        public final String dst;
        public final float weight;
        public Edge(String dst, float weight) { this.dst = dst; this.weight = weight; }
    }

    private static final class Row {
        private final int maxSize;
        private final float decay;
        private final HashMap<String, Float> counts = new HashMap<>();

        Row(int maxSize, float decay) {
            this.maxSize = maxSize;
            this.decay = decay;
        }

        void update(String dst) {
            // simple: decay only touched entry (cheap). You can also periodic global decay.
            float v = counts.getOrDefault(dst, 0f);
            counts.put(dst, v * decay + 1f);
            prune();
        }

        void put(String dst, float w) {
            counts.put(dst, w);
        }

        void prune() {
            if (counts.size() <= maxSize) return;
            ArrayList<Map.Entry<String, Float>> es = new ArrayList<>(counts.entrySet());
            es.sort(Comparator.comparingDouble(Map.Entry::getValue)); // asc
            int remove = counts.size() - maxSize;
            for (int i = 0; i < remove; i++) counts.remove(es.get(i).getKey());
        }

        List<String> topN(int n) {
            ArrayList<Map.Entry<String, Float>> es = new ArrayList<>(counts.entrySet());
            es.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            int k = Math.min(n, es.size());
            ArrayList<String> out = new ArrayList<>(k);
            for (int i = 0; i < k; i++) out.add(es.get(i).getKey());
            return out;
        }

        List<Edge> snapshotEdgesSorted() {
            ArrayList<Map.Entry<String, Float>> es = new ArrayList<>(counts.entrySet());
            es.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            ArrayList<Edge> out = new ArrayList<>(es.size());
            for (Map.Entry<String, Float> e : es) out.add(new Edge(e.getKey(), e.getValue()));
            return out;
        }
    }
}
