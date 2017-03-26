package com.yima.decoder;

import java.util.Hashtable;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.yima.camera.CameraManager;
import com.yima.camera.CaptureActivity;
import com.yima.camera.PlanarYUVLuminanceSource;

public final class DecodeHandler extends Handler {

	private final CaptureActivity activity;
	private final MultiFormatReader multiFormatReader;

	DecodeHandler(CaptureActivity activity,
			Hashtable<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case CaptureActivityHandler.REQUEST_DECODE:
			// Log.d(TAG, "Got decode message");
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		//case 0x7f090007:  R.id.quit:
		case CaptureActivityHandler.DECODE_QUIT:
			Looper.myLooper().quit();
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		long start = System.currentTimeMillis();
		Log.d("Barcode Time", "s0 (" + start + " ms):\n");
		Result rawResult = null;
		//Utils.showLog(TAG, "width >> " + width + "  heigth >> " + height);
		//Utils.showLog(TAG, "width x heigth >> " + height * width);
		//Utils.showLog(TAG, " data length >> " + data.length);
		// 竖屏的时候执行这样操作： landscape的时候不需要调用这样的
		if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) 
		{
			byte[] rotatedData = new byte[data.length];
			

			 for (int y = 0; y < height; y++) {
			 for (int x = 0; x < width; x++)
			 rotatedData[x * height + height - y - 1] = data[x + y
			 * width];
			 }
			int tmp = width; // Here we are swapping, that's the difference to
								// #11
			width = height;
			height = tmp;

			data = rotatedData;
		}
		long s1 = System.currentTimeMillis();
		Log.d("Barcode Time", "s1 (" + s1 + " ms):\n");
		
		PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(data, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		long s2 = System.currentTimeMillis();
		Log.d("Barcode Time", "s2 (" + s2 + " ms):\n");
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
			long ss = System.currentTimeMillis();
			Log.d("Barcode Time", "ss (" + ss + " ms):\n");
		} catch (ReaderException re) {
			// continue
		} finally {
			multiFormatReader.reset();
		}

		if (rawResult != null) {
			long end = System.currentTimeMillis();
			Message message = Message.obtain(activity.getHandler(),
					CaptureActivityHandler.DECODE_SUCCEEDED , rawResult);
			Bundle bundle = new Bundle();
			bundle.putParcelable(DecodeThread.BARCODE_BITMAP,
					source.renderCroppedGreyscaleBitmap());
			bundle.putLong(DecodeThread.DECODE_TIME, end - start);
			message.setData(bundle);
			// Log.d(TAG, "Sending decode succeeded message...");
			message.sendToTarget();
		} else {
			Message message = Message.obtain(activity.getHandler(),
					CaptureActivityHandler.DECODE_FAILED);
			message.sendToTarget();
		}
	}

}
