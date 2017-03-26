package com.yima.camera;

import com.yima.decoder.CaptureActivityHandler;
import com.yima.listener.PluginResultListener;

import android.content.Context;
import android.content.Intent;

public class YiMaDecoder {
	private Context context;
	private static YiMaDecoder instance;
	private CaptureActivityHandler handler;

	public static YiMaDecoder getInstance(Context context) {
		if (instance == null) {
			synchronized (YiMaDecoder.class) {
				if (instance == null) {
					instance = new YiMaDecoder(context);
				}
			}
		}
		return instance;
	}

	private YiMaDecoder(Context context) {
		this.context = context;
		handler = CaptureActivityHandler.getInstance();
		handler.setContext(context);
	}

	/**
	 * 增加扫码结果返回监听器
	 * 
	 * @param listener
	 */
	public void addResultListener(PluginResultListener listener) {
		if (handler != null) {
			handler.setPluginResultListener(listener);
		}
	}

	/**
	 * 开启扫码
	 */
	public void scanBarcode() {
		if (context != null) {
			Intent intent = new Intent(context, CaptureActivity.class);
			context.startActivity(intent);
		}
	}
	
	public void onDestroy() {
		context = null;
		handler = null;
		instance = null;
	}
}
