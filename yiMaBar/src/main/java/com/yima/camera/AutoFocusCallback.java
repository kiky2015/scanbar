package com.yima.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public final class AutoFocusCallback implements Camera.AutoFocusCallback {

	private static final String TAG = AutoFocusCallback.class.getSimpleName();

	private static final long AUTOFOCUS_INTERVAL_MS = 500L; // huxiang add from
															// 1500L to 500L

	private Handler autoFocusHandler;
	private int autoFocusMessage;

	void setHandler(Handler autoFocusHandler, int autoFocusMessage) {
		this.autoFocusHandler = autoFocusHandler;
		this.autoFocusMessage = autoFocusMessage;
	}

	public void onAutoFocus(boolean success, Camera camera) {
		if (autoFocusHandler != null) {
			Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
			// Simulate continuous autofocus by sending a focus request every
			// AUTOFOCUS_INTERVAL_MS milliseconds.
			// Log.d(TAG, "Got auto-focus callback; requesting another");
			autoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);
			autoFocusHandler = null;
		} else {
			Log.d(TAG, "Got auto-focus callback, but no handler for it");
		}
	}

}
