package com.oldschoolnetwork.core;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent UI snapshot; the download service is the only writer. */
public final class ServiceTransferState {
    public static SharedPreferences read(Context context) { return context.getSharedPreferences("ONServiceTransfer", Context.MODE_PRIVATE); }
    public static void write(Context context, String id, String phase, long bytes, long total, String path, String error) {
        read(context).edit().putString("id", id).putString("phase", phase).putLong("bytes", bytes).putLong("total", total)
            .putString("path", path == null ? "" : path).putString("error", error == null ? "" : error).apply();
    }
}
