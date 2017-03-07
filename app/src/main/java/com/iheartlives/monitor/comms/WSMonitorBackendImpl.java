package com.iheartlives.monitor.comms;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.iheartlives.monitor.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Talks to the monitoring service
 */
public class WSMonitorBackendImpl implements MonitorBackend {

    private enum State {
        INVALID,
        CONNECTING,
        READY,
        CLOSING,
        PAUSED
    }

    private static final int WHAT_CONNECT = 1;
    private static final int WHAT_DISCONNECT = 2;
    private static final int WHAT_SEND_MESSAGE = 3;

    public static final String TAG = "Monitor:Backend";

    /**
     * Access to this member must always be locked with STATE_LOCK
     */
    private State mState = State.INVALID;
    private final Object STATE_LOCK = new Object[0];

    private Context mContext;
    private ClientListener mListener;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Handler mMainThreadHandler;
    private WebSocketListener mWebSocketlistener;
    private WebSocket mSocket;
    private final Gson mGson = new Gson();

    public static MonitorBackend get() {
        return new WSMonitorBackendImpl();
    }

    @Override
    public void init(Context context, ClientListener listener) {
        synchronized (STATE_LOCK) {
            switch (mState) {
                case INVALID:
                    mState = State.CONNECTING;
                    break;
                default:
                    Log.w(TAG, String.format("Cannot call init() in state '%s'.", mState.name()));
                    return;
            }
        }

        if (listener == null || context == null) {
            throw new IllegalArgumentException("Need non-null params");
        }
        mContext = context.getApplicationContext();
        mListener = listener;

        mMainThreadHandler = new Handler(Looper.getMainLooper());
        mHandlerThread = new HandlerThread("monitor-ws-conn");
        mHandlerThread.start();
        mHandler = new WebsocketMessageHandler(mHandlerThread.getLooper());

        // Start async connect
        mHandler.sendEmptyMessage(WHAT_CONNECT);
    }

    @Override
    public void sendMessage(Message message) {
        synchronized (STATE_LOCK) {
            if (mState != State.READY) {
                throw new IllegalStateException("Not ready, wait for onReady callback");
            }
        }
        // Send message
        android.os.Message.obtain(mHandler, WHAT_SEND_MESSAGE, message).sendToTarget();
    }

    @Override
    public void close() {
        closeInternal(true);
    }

    @Override
    public boolean ready() {
        synchronized (STATE_LOCK) {
            return mState == State.READY;
        }
    }

    private void closeInternal(boolean clientCalled) {
        if (clientCalled) {
            Log.i(TAG, "Close initiated");
        }
        synchronized (STATE_LOCK) {
            switch (mState) {
                case READY:
                case PAUSED:
                case CONNECTING:
                    mState = State.CLOSING;
                    break;
                default:
                    Log.w(TAG, String.format("Cannot call close() in state '%s'.", mState.name()));
                    return;
            }
        }

        mHandler.sendEmptyMessage(WHAT_DISCONNECT);
    }

    private void reset() {
        mHandlerThread.quitSafely();
        mListener = null;
        mHandlerThread = null;
        mHandler = null;
        mSocket = null;
        mWebSocketlistener = null;
    }

    /**
     * Listener implementation that receives messages from the websocket
     */
    private class MonitorBackendWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            Log.i(TAG, "onOpen");
            synchronized (STATE_LOCK) {
                mState = State.READY;
            }
            mMainThreadHandler.post(() -> mListener.onReady());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            Log.i(TAG, "onClosing");
            // Just call close() on the backend interface as though the client did
            closeInternal(false);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.i(TAG, "onClosed");
            super.onClosed(webSocket, code, reason);
            synchronized (STATE_LOCK) {
                if (mState != State.CLOSING) {
                    throw new IllegalStateException("Unexpected state in close()");
                }
                mState = State.INVALID;
            }
            ClientListener l = mListener; // Save to local cos reset() will kill the ref
            mMainThreadHandler.post(l::onComplete);
            // This is a terminal call
            reset();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.i(TAG, String.format("onFailure (%s)", t.getMessage()));
            super.onFailure(webSocket, t, response);

            ClientListener l = mListener; // Save to local cos reset() will kill the ref
            mMainThreadHandler.post(l::onError);
            // This is a terminal call
            reset();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.d(TAG, String.format("Got message: %s", text));
            try {
                Message message = mGson.fromJson(text, Message.class);
                mMainThreadHandler.post(() -> mListener.onMessage(message));
            } catch (JsonParseException jpe) {
                Log.w(TAG, "Could not parse message with Gson, skipping", jpe);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
            Log.e(TAG, "Got raw bytes, don't know how to handle them.");
        }
    }

    /**
     * Handler class that drives ws comms
     */
    private class WebsocketMessageHandler extends Handler {
        public WebsocketMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case WHAT_CONNECT: {
                    Log.i(TAG, "Attempting to connect");
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(mContext.getString(R.string.ws_host))
                            .build();
                    mWebSocketlistener = new MonitorBackendWebSocketListener();
                    mSocket = client.newWebSocket(request, mWebSocketlistener);
                    break;
                }
                case WHAT_DISCONNECT: {
                    mSocket.close(1000, "Client done");
                    break;
                }
                case WHAT_SEND_MESSAGE: {
                    Message message = (Message) msg.obj;
                    Log.i(TAG, String.format("Sending a message of type '%s'", message.type));
                    mSocket.send(mGson.toJson(message));
                }
            }
        }
    }
}
