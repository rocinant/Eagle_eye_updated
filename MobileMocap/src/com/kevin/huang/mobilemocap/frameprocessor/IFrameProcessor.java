package com.kevin.huang.mobilemocap.frameprocessor;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;

public interface IFrameProcessor extends PreviewCallback {
	
	public void onPreviewFrame(byte[] data, Camera camera);
	public void setCamera(Camera camera);
}
