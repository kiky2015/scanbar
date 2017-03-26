package com.yima.camera;

import java.io.IOException;

import com.yima.decoder.CaptureActivityHandler;
import com.yima.view.BarfinderView;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class CaptureActivity extends Activity implements Callback {

	private CaptureActivityHandler handler;
	private boolean hasSurface;
	private PowerManager powerManager = null;
	private WakeLock wakeLock = null;
	private BarfinderView barfinderView = null;
	private SurfaceView surfaceView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		RelativeLayout layout = new RelativeLayout(getApplication());
		barfinderView = new BarfinderView(getApplication());
		RelativeLayout.LayoutParams ltparam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		surfaceView = new SurfaceView(getApplication());
		layout.addView(surfaceView, ltparam);
		layout.addView(barfinderView, ltparam);
		setContentView(layout);

		CameraManager.init(getApplication());
		hasSurface = false;
		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
	}

	protected void onResume() {
		super.onResume();

		wakeLock.acquire();
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		wakeLock.release();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (handler == null) {
			handler = CaptureActivityHandler.getInstance();
			handler.setActivity(this);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	public BarfinderView getBarfinderView() {
		return barfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		barfinderView.drawViewfinder();
	}

}
