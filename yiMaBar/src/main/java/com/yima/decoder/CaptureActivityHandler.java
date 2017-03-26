package com.yima.decoder;

import java.util.Vector;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.yima.camera.CameraManager;
import com.yima.camera.CaptureActivity;
import com.yima.listener.PluginResultListener;
import com.yima.listener.YiMaInfo;
import com.yima.view.BarfinderResultPointCallback;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public final class CaptureActivityHandler extends Handler {
	private static final String TAG = CaptureActivityHandler.class.getSimpleName();
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private final float BEEP_VOLUME = 0.90f;
	private CaptureActivity activity;
	private DecodeThread decodeThread;
	private State state;
	private PluginResultListener listener = null;
	private Context context = null;

	public static final int AUTO_FOCUS = 1;
	public static final int RESTART_PREVIEW = AUTO_FOCUS + 1;
	public static final int DECODE_SUCCEEDED = RESTART_PREVIEW + 1;
	public static final int DECODE_FAILED = DECODE_SUCCEEDED + 1;
	public static final int DECODE_QUIT = DECODE_FAILED + 1;
	public static final int REQUEST_DECODE = DECODE_QUIT + 1;

	private static CaptureActivityHandler instance = null;

	public static CaptureActivityHandler getInstance() {
		if (instance == null) {
			synchronized (CaptureActivityHandler.class) {
				if (instance == null) {
					instance = new CaptureActivityHandler();
				}
			}
		}
		return instance;
	}

	private CaptureActivityHandler() {
	};

	public void setPluginResultListener(PluginResultListener listener) {
		this.listener = listener;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}

	public void setActivity(CaptureActivity activity) {
		this.activity = activity;
		decodeThread = new DecodeThread(activity, null, null,
				new BarfinderResultPointCallback(activity.getBarfinderView()));
		decodeThread.start();
		state = State.SUCCESS;
		CameraManager.get().startPreview();
		restartPreviewAndDecode();

		playBeep = true;
		AudioManager audioService = (AudioManager) activity.getSystemService(activity.AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
	}

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	private CaptureActivityHandler(CaptureActivity activity, Vector<BarcodeFormat> decodeFormats, String characterSet) {
		this.activity = activity;
		decodeThread = new DecodeThread(activity, decodeFormats, characterSet,
				new BarfinderResultPointCallback(activity.getBarfinderView()));
		decodeThread.start();
		state = State.SUCCESS;
		CameraManager.get().startPreview();
		restartPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case AUTO_FOCUS:
			if (state == State.PREVIEW) {
				CameraManager.get().requestAutoFocus(this, AUTO_FOCUS);
			}
			break;
		case RESTART_PREVIEW:
			Log.d(TAG, "Got restart preview message");
			restartPreviewAndDecode();
			break;
		case DECODE_SUCCEEDED:
			if (mediaPlayer != null) {
				mediaPlayer.start();
			}
			state = State.SUCCESS;
			activity.finish();
			try {
				Bundle bundle = message.getData();
				Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
				long decodetime = bundle == null ? -1 : bundle.getLong(DecodeThread.DECODE_TIME);
				Result rst = (Result) message.obj;
				YiMaInfo barcodeData = new YiMaInfo();
				barcodeData.barcodeImage = barcode;
				barcodeData.barcodeDecodeTime = decodetime;
				barcodeData.barcodeType = rst.getBarcodeFormat().toString();
				barcodeData.barcodeValue = rst.getText();
				listener.onPluginResult(barcodeData);
			} catch (Exception e) {
			}
			break;
		case DECODE_FAILED:
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), REQUEST_DECODE);
			break;
		}
	}

	public void quitSynchronously() {
		state = State.DONE;
		CameraManager.get().stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), DECODE_QUIT);
		quit.sendToTarget();
		try {
			decodeThread.join();
		} catch (InterruptedException e) {
			// continue
		}

		removeMessages(DECODE_SUCCEEDED);
		removeMessages(DECODE_FAILED);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), REQUEST_DECODE);
			CameraManager.get().requestAutoFocus(this, AUTO_FOCUS);
			activity.drawViewfinder();
		}
	}

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);
			try {
				int musicResId = context.getResources().getIdentifier("beep", "raw", context.getPackageName());
				AssetFileDescriptor file = context.getResources().openRawResourceFd(musicResId);
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (Exception e) {
				mediaPlayer = null;
			}
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

}
