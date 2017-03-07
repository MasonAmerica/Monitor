package com.iheartlives.monitor.comms;

import android.content.Context;

/**
 * Interface to the monitoring service
 */

public interface MonitorBackend {
    void init(Context context, ClientListener listener);
    void sendMessage(Message message);
    void close();
    boolean ready();

    interface ClientListener {
        void onReady();
        void onPause();
        void onResume();
        void onMessage(Message message);
        void onError();
        void onComplete();
    }
}
