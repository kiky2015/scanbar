package com.yima.camera;

import java.io.IOException;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.util.Log;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

	private static final String TAG = CameraManager.class.getSimpleName();

	// private static final int MIN_FRAME_WIDTH = 240;
	// private static final int MIN_FRAME_HEIGHT = 240;
	// private static final int MAX_FRAME_WIDTH = 480;
	// private static final int MAX_FRAME_HEIGHT = 360;

	private static final int MIN_FRAME_WIDTH = 300;
	private static final int MIN_FRAME_HEIGHT = 360;
	private static final int MAX_FRAME_WIDTH = 1920; // = 1920/2
	private static final int MAX_FRAME_HEIGHT = 1080; // = 1080/2

	// private static final int MAX_FRAME_WIDTH = 560;
	// private static final int MAX_FRAME_HEIGHT = 420;
	// private static final int MAX_FRAME_WIDTH = 600;
	// private static final int MAX_FRAME_HEIGHT = 420;

	private static CameraManager cameraManager;

	static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
	static {
		int sdkInt;
		try {
			// sdkInt = Integer.parseInt(Build.VERSION.SDK);
			sdkInt = android.os.Build.VERSION.SDK_INT; // huxiang changed to
														// reduce initial camera
														// time
		} catch (NumberFormatException nfe) {
			// Just to be safe
			sdkInt = 10000;
		}
		SDK_INT = sdkInt;

	}

	private final Context context;
	private final CameraConfigurationManager configManager;
	private Camera camera;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean initialized = false;
	private boolean previewing;
	private final boolean useOneShotPreviewCallback;
	/**
	 * Preview frames are delivered here, which we pass on to the registered
	 * handler. Make sure to clear the handler so it will only receive one
	 * message.
	 */
	private final PreviewCallback previewCallback;
	/**
	 * Autofocus callbacks arrive here, and are dispatched to the Handler which
	 * requested them.
	 */
	private final AutoFocusCallback autoFocusCallback;

	/**
	 * Initializes this static object with the Context of the calling Activity.
	 * 
	 * @param context
	 *            The Activity which wants to use the camera.
	 */
	public static void init(Context context) {
		if (cameraManager == null) {
			cameraManager = new CameraManager(context);
		}
	}

	/**
	 * Gets the CameraManager singleton instance.
	 * 
	 * @return A reference to the CameraManager singleton.
	 */
	public static CameraManager get() {
		return cameraManager;
	}

	private CameraManager(Context context) {

		this.context = context;
		this.configManager = new CameraConfigurationManager(context);

		useOneShotPreviewCallback = SDK_INT > 3;
		previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
		autoFocusCallback = new AutoFocusCallback();
	}

	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 * 
	 * @param holder
	 *            The surface object which the camera will draw preview frames
	 *            into.
	 * @throws IOException
	 *             Indicates the camera driver failed to open.
	 */
	/*
	 * public void openDriver(SurfaceHolder holder) throws IOException { if
	 * (camera == null) { camera = Camera.open(); if (camera == null) { throw
	 * new IOException(); } camera.setPreviewDisplay(holder); if
	 * (context.getResources().getConfiguration().orientation ==
	 * Configuration.ORIENTATION_PORTRAIT) { camera.setDisplayOrientation(90);
	 * 
	 * // camera.setDisplayOrientation(270);
	 * 
	 * } long currentTimeMillis = System.currentTimeMillis(); if (!initialized)
	 * { initialized = true; configManager.initFromCameraParameters(camera); }
	 * long currentTimeMillis2d = System.currentTimeMillis();
	 * Log.d("openDriver", "initial camera time"+
	 * (currentTimeMillis2d-currentTimeMillis));
	 * configManager.setDesiredCameraParameters(camera);
	 * 
	 * long currentTimeMillis3d = System.currentTimeMillis();
	 * Log.d("openDriver", "set camera time"+
	 * (currentTimeMillis3d-currentTimeMillis2d));
	 * FlashlightManager.enableFlashlight();
	 * 
	 * 
	 * } }
	 */
	public void openDriver(SurfaceHolder holder) throws IOException {
		if (camera == null) {
			camera = Camera.open();
			if (camera == null) {
				throw new IOException();
			}
			camera.setPreviewDisplay(holder);
			if (!initialized) {
				initialized = true;
				configManager.initFromCameraParameters(camera);
			}
			// if (!setDesired) {
			// setDesired = true;
			configManager.setDesiredCameraParameters(camera);
			// }
			FlashlightManager.enableFlashlight();

		}
	}

	/**
	 * Closes the camera driver if still in use.
	 */
	public void closeDriver() {
		if (camera != null) {
			FlashlightManager.disableFlashlight();
			camera.release();
			camera = null;
		}
	}

	/**
	 * Asks the camera hardware to begin drawing preview frames to the screen.
	 */
	public void startPreview() {
		if (camera != null && !previewing) {
			camera.startPreview();
			previewing = true;
		}
	}

	/**
	 * Tells the camera to stop drawing preview frames.
	 */
	public void stopPreview() {
		if (camera != null && previewing) {
			if (!useOneShotPreviewCallback) {
				camera.setPreviewCallback(null);
			}
			camera.stopPreview();
			previewCallback.setHandler(null, 0);
			autoFocusCallback.setHandler(null, 0);
			previewing = false;
		}
	}

	/**
	 * A single preview frame will be returned to the handler supplied. The data
	 * will arrive as byte[] in the message.obj field, with width and height
	 * encoded as message.arg1 and message.arg2, respectively.
	 * 
	 * @param handler
	 *            The handler to send the message to.
	 * @param message
	 *            The what field of the message to be sent.
	 */
	public void requestPreviewFrame(Handler handler, int message) {
		if (camera != null && previewing) {
			previewCallback.setHandler(handler, message);
			if (useOneShotPreviewCallback) {
				camera.setOneShotPreviewCallback(previewCallback);
			} else {
				camera.setPreviewCallback(previewCallback);
			}
		}
	}

	/**
	 * Asks the camera hardware to perform an autofocus.
	 * 
	 * @param handler
	 *            The Handler to notify when the autofocus completes.
	 * @param message
	 *            The message to deliver.
	 */
	public void requestAutoFocus(Handler handler, int message) {
		if (camera != null && previewing) {
			autoFocusCallback.setHandler(handler, message);
			// Log.d(TAG, "Requesting auto-focus callback");
			camera.autoFocus(autoFocusCallback);
		}
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user
	 * where to place the barcode. This target helps with alignment as well as
	 * forces the user to hold the device far enough away to ensure the image
	 * will be in focus.
	 * 
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public Rect getFramingRect() {
		Point screenResolution = configManager.getScreenResolution();
		if (framingRect == null) {
			if (camera == null) {
				return null;
			}

			int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
			int height = findDesiredDimensionInRangeY(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
			Log.d(TAG, "Calculated framing rect: " + framingRect);
		}
		return framingRect;
	}

	private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
		int dim = (int) (resolution / 1.2); // Target 25% of each dimension
		if (dim < hardMin) {
			return hardMin;
		}
		if (dim > hardMax) {
			return hardMax;
		}
		return dim;
	}

	private static int findDesiredDimensionInRangeY(int resolution, int hardMin, int hardMax) {
		int dim = (int) (resolution / 3); // Target 25% of each dimension
		if (dim < hardMin) {
			return hardMin;
		}
		if (dim > hardMax) {
			return hardMax;
		}
		return dim;
	}

	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview
	 * frame, not UI / screen.
	 */
	public Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect rect = new Rect(getFramingRect());
			Point cameraResolution = configManager.getCameraResolution();
			Point screenResolution = configManager.getScreenResolution();

			if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				rect.left = rect.left * cameraResolution.y / screenResolution.x;
				rect.right = rect.right * cameraResolution.y / screenResolution.x;
				rect.top = rect.top * cameraResolution.x / screenResolution.y;
				rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
			} else {
				rect.left = rect.left * cameraResolution.x / screenResolution.x;
				rect.right = rect.right * cameraResolution.x / screenResolution.x;
				rect.top = rect.top * cameraResolution.y / screenResolution.y;
				rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
			}

			framingRectInPreview = rect;
		}
		Log.d(TAG, "framingRectInPreview: " + framingRectInPreview);
		return framingRectInPreview;
	}

	/**
	 * Converts the result points from still resolution coordinates to screen
	 * coordinates.
	 * 
	 * @param points
	 *            The points returned by the Reader subclass through
	 *            Result.getResultPoints().
	 * @return An array of Points scaled to the size of the framing rect and
	 *         offset appropriately so they can be drawn in screen coordinates.
	 */
	/*
	 * public Point[] convertResultPoints(ResultPoint[] points) { Rect frame =
	 * getFramingRectInPreview(); int count = points.length; Point[] output =
	 * new Point[count]; for (int x = 0; x < count; x++) { output[x] = new
	 * Point(); output[x].x = frame.left + (int) (points[x].getX() + 0.5f);
	 * output[x].y = frame.top + (int) (points[x].getY() + 0.5f); } return
	 * output; }
	 */

	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
		Rect rect = getFramingRectInPreview();
		int previewFormat = configManager.getPreviewFormat();
		String previewFormatString = configManager.getPreviewFormatString();
		Log.d(TAG, "previewFormat::" + previewFormat + ">>>previewFormatString::" + previewFormatString);
		switch (previewFormat) {
		// This is the standard Android format which all devices are REQUIRED to
		// support.
		// In theory, it's the only one we should ever care about.
		case PixelFormat.YCbCr_420_SP:
			// This format has never been seen in the wild, but is
			// compatible as we only care
			// about the Y channel, so allow it.
		case PixelFormat.YCbCr_422_SP:
			return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height());
		default:
			// The Samsung Moment incorrectly uses this variant instead of
			// the 'sp' version.
			// Fortunately, it too has all the Y data up front, so we can
			// read it.
			if ("yuv420p".equals(previewFormatString)) {
				return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(),
						rect.height());
			}
		}
		throw new IllegalArgumentException("Unsupported picture format: " + previewFormat + '/' + previewFormatString);
	}

}
