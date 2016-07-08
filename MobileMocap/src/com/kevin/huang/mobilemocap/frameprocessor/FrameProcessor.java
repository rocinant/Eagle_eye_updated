package com.kevin.huang.mobilemocap.frameprocessor;

import android.hardware.Camera;

public abstract class FrameProcessor implements IFrameProcessor {
	
	protected android.hardware.Camera.Size mFrameSize;
	protected Camera mCamera;
	
	public android.hardware.Camera.Size getFrameSize() {
		return mFrameSize;
	}
	
	public abstract void ready();
	
	@Override
	public void setCamera(Camera camera) {
		// TODO Auto-generated method stub
		this.mFrameSize = camera.getParameters().getPreviewSize();
		this.mCamera = camera;
		ready();

	}
	

	@Override
	public abstract void onPreviewFrame(byte[] data, Camera camera);

}
