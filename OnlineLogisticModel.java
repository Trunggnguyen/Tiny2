package android.server.power.nextapp;

import java.nio.charset.StandardCharsets;

public final class OnlineLogisticModel {
    private final int DIM;
    private final float[] w;
    private float b;

    private final float lr;
    private final float l2;

    public OnlineLogisticModel(int hashDimPow2, float lr, float l2) {
        this.DIM = 1 << hashDimPow2;
        this.w = new float[DIM];
        this.lr = lr;
        this.l2 = l2;
    }

    public float score(String A, String B, NextAppContext ctx) {
        float z = b;
        z += dot("A=" + A);
        z += dot("B=" + B);
        z += dot("A*B=" + A + "#" + B);

        if (ctx != null) {
            z += dot("T=" + ctx.timeBucket);
            z += dot("S=" + (ctx.screenOn ? 1 : 0));
            z += dot("N=" + ctx.netType);
            z += dot("L=" + ctx.launchType);
            z += dot("BB=" + ctx.batteryBucket);
            z += dot("MP=" + (ctx.maxPowerMode ? 1 : 0));

            z += dot("A*T=" + A + "#" + ctx.timeBucket);
            z += dot("B*T=" + B + "#" + ctx.timeBucket);
            z += dot("A*L=" + A + "#" + ctx.launchType);
            z += dot("B*L=" + B + "#" + ctx.launchType);
        }
        return sigmoid(z);
    }

    public void update(String A, String B, NextAppContext ctx, int label01) {
        float p = score(A, B, ctx);
        float grad = (p - label01); // dL/dz

        b -= lr * grad;

        upd("A=" + A, grad);
        upd("B=" + B, grad);
        upd("A*B=" + A + "#" + B, grad);

        if (ctx != null) {
            upd("T=" + ctx.timeBucket, grad);
            upd("S=" + (ctx.screenOn ? 1 : 0), grad);
            upd("N=" + ctx.netType, grad);
            upd("L=" + ctx.launchType, grad);
            upd("BB=" + ctx.batteryBucket, grad);
            upd("MP=" + (ctx.maxPowerMode ? 1 : 0), grad);

            upd("A*T=" + A + "#" + ctx.timeBucket, grad);
            upd("B*T=" + B + "#" + ctx.timeBucket, grad);
            upd("A*L=" + A + "#" + ctx.launchType, grad);
            upd("B*L=" + B + "#" + ctx.launchType, grad);
        }
    }

    private void upd(String feat, float grad) {
        int idx = hashToIndex(feat, 0x1234ABCD);
        int sgn = hashToSign(feat, 0xBEEF1234);
        float wi = w[idx];
        wi -= lr * (grad * sgn + l2 * wi);
        w[idx] = wi;
    }

    private float dot(String feat) {
        int idx = hashToIndex(feat, 0x1234ABCD);
        int sgn = hashToSign(feat, 0xBEEF1234);
        return w[idx] * sgn;
    }

    private int hashToIndex(String s, int seed) {
        int h = murmur3_32(s.getBytes(StandardCharsets.UTF_8), seed);
        return (h & 0x7fffffff) & (DIM - 1);
    }

    private int hashToSign(String s, int seed) {
        int h = murmur3_32(s.getBytes(StandardCharsets.UTF_8), seed);
        return ((h & 1) == 0) ? 1 : -1;
    }

    private static float sigmoid(float z) {
        if (z >= 10f) return 0.9999546f;
        if (z <= -10f) return 0.0000454f;
        return (float)(1.0 / (1.0 + Math.exp(-z)));
    }

    // Minimal Murmur3 32-bit for bytes
    private static int murmur3_32(byte[] data, int seed) {
        int h1 = seed;
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        int len = data.length;
        int i = 0;
        while (i + 4 <= len) {
            int k1 = (data[i] & 0xff)
                    | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16)
                    | ((data[i + 3] & 0xff) << 24);
            i += 4;

            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        int k1 = 0;
        int rem = len - i;
        if (rem == 3) k1 ^= (data[i + 2] & 0xff) << 16;
        if (rem >= 2) k1 ^= (data[i + 1] & 0xff) << 8;
        if (rem >= 1) k1 ^= (data[i] & 0xff);
        if (rem > 0) {
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;
            h1 ^= k1;
        }

        h1 ^= len;
        h1 ^= (h1 >>> 16);
        h1 *= 0x85ebca6b;
        h1 ^= (h1 >>> 13);
        h1 *= 0xc2b2ae35;
        h1 ^= (h1 >>> 16);
        return h1;
    }
}
