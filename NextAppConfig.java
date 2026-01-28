package android.server.power.nextapp;

public final class NextAppConfig {
    // Markov
    public int markovTopMPerA = 50;
    public int candidateTopN = 20;
    public float markovDecay = 0.9995f; // per update (simple)

    // Policy
    public int prefetchTopK = 1;
    public float threshold = 0.70f;
    public float gapDelta = 0.10f;
    public int ttlMs = 30_000;

    // LR optional
    public boolean enableLr = true;
    public int hashDimPow2 = 16;  // 2^16
    public float lr = 0.05f;
    public float l2 = 1e-6f;
    public int hardNegPerPos = 5;

    // Persistence
    public int checkpointEveryNUpdates = 300;

    public boolean enable = true;
}
