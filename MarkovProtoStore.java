package android.server.power.nextapp;

import android.util.AtomicFile;
import android.util.Slog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.server.power.proto.MarkovModelProto; // <-- đổi theo generated package
import android.server.power.proto.MarkovModelProto.MarkovModel;
import android.server.power.proto.MarkovModelProto.MarkovRow;
import android.server.power.proto.MarkovModelProto.MarkovEdge;

public final class MarkovProtoStore {
    private static final String TAG = "NextAppMarkovStore";

    private final AtomicFile mFile;
    private final int mVersion;

    public MarkovProtoStore(File file, int version) {
        mFile = new AtomicFile(file);
        mVersion = version;
    }

    public MarkovModel readOrNull() {
        try (FileInputStream in = mFile.openRead()) {
            return MarkovModel.parseFrom(in);
        } catch (Throwable t) {
            Slog.w(TAG, "read failed, ignoring", t);
            return null;
        }
    }

    public void write(MarkovModel model) {
        FileOutputStream out = null;
        try {
            out = mFile.startWrite();
            model.writeTo(out);
            mFile.finishWrite(out);
        } catch (Throwable t) {
            Slog.e(TAG, "write failed", t);
            if (out != null) mFile.failWrite(out);
        }
    }

    public MarkovModel newEmpty(long nowElapsedMs) {
        return MarkovModel.newBuilder()
                .setVersion(mVersion)
                .setLastUpdateElapsedMs(nowElapsedMs)
                .build();
    }
}
