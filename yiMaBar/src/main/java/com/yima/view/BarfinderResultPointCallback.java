package com.yima.view;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

public class BarfinderResultPointCallback implements ResultPointCallback {

	private final BarfinderView barfinderView;

	public BarfinderResultPointCallback(BarfinderView barfinderView) {
	    this.barfinderView = barfinderView;
	 }

	public void foundPossibleResultPoint(ResultPoint point) {
		barfinderView.addPossibleResultPoint(point);
	}

}
