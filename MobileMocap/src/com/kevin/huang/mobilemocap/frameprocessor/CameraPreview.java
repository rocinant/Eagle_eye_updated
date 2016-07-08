package com.kevin.huang.mobilemocap.frameprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * @author yangjianhuang026@gmail.com
 * This class opens the camera on the Android and draws a preview of the camera on the screen.
 * It will initialize the Android OpenCV library and calls cameraPreviewLoaded on the context
 * when the library and the camera are ready to be used. This class processes frames from the
 * camera preview and passes them to the IFrameProcessor for processing.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, LoaderCallbackInterface {
	private static final String TAG = "CameraTest::CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private IFrameProcessor mFrameProcessor;
    private ICameraPreviewCallback mContext;
    
    private float PREVIEW_SIZE_FACTOR = 1.30f;
    
    // Whether the active camera is front-facing.
 	// If so, the camera view should be mirrored.
 	public boolean mIsCameraFrontFacing;
 	// The number of cameras on the device.
 	public int mNumCameras;
 	// get Camera parameters
 	public Camera.Parameters params;
 	// If so, the camera view should be mirrored.
  	public boolean mIsAutofocusEnable = false;
  	//camera focal length
  	public float focalLength;
  	//camera preview size
  	public Camera.Size previewSize;
    //camera preview FPS range (frame per second)
  	public int [] previewFpsRange = new int [2];
  	//supported camera preview FPS range (frame per second)
    public List<int []> SupportedPreviewFpsRange = new ArrayList <int []> ();
    //supported camera preview FPS range (frame per second)
    public List<Camera.Size> supportPreviewSize = new ArrayList <Camera.Size> ();
    private int bufferSize;
    
    public CameraPreview(ICameraPreviewCallback context, IFrameProcessor frameProcessor) {
    	super((Context)context);
        mContext = context;
        mFrameProcessor = frameProcessor;
        if (!loadOpenCV()) {
        	Log.e("OpenCV could not be loaded!", TAG);
        }
    
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
        	Log.i(TAG, "surfaceCreated");
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            
            /*for (int i = 0; i < 1; i++) {
            	mCamera.addCallbackBuffer(new byte[bufferSize]);
            };           
            mCamera.setPreviewCallbackWithBuffer(mFrameProcessor);*/
            
            //this.getHolder().setFixedSize(width, height);
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    	 // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        if(mCamera!=null){
        	mCamera.stopPreview();
        	mCamera.setPreviewCallback(null);

        	mCamera.release();
        	mCamera = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            //mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            /*mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();*/
            /*Size previewSize = mCamera.getParameters().getPreviewSize();
            int previewFormat = mCamera.getParameters().getPreviewFormat();
            int bytesPerPixel = ImageFormat.getBitsPerPixel(previewFormat) / 8;
            int bufferSize = (int)(previewSize.width * previewSize.height * bytesPerPixel * 1.5); //TODO: Don't know why I need this 1.5
*/            
            for (int i = 0; i < 1; i++) {
            	mCamera.addCallbackBuffer(new byte[bufferSize]);
            };
            
            mCamera.setPreviewCallbackWithBuffer(mFrameProcessor);

        } catch (Exception e){
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
    
    private boolean loadOpenCV() {
    	return OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, (Context)mContext, this);
    }
    
    /*
     * This method is called after the OpenCV library is initialized.
     */
	public void onManagerConnected(int status) {
		if (openCamera()) {
			// Install a SurfaceHolder.Callback so we get notified when the
	        // underlying surface is created and destroyed.
	        mHolder = getHolder();
	        mHolder.addCallback(this);
	        // deprecated setting, but required on Android versions prior to 3.0
	        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        	mFrameProcessor.setCamera(mCamera);
        	try {
    			Thread.sleep(1000);
    		} catch (Exception e) { }
        	mContext.cameraPreviewLoaded();
		}
	}

	public void onPackageInstall(InstallCallbackInterface callback) {
		// TODO Auto-generated method stub
		
	}
	
	private boolean openCamera() {
		Log.i(TAG, "openCamera");
    	if (mCamera == null) {
			if (Camera.getNumberOfCameras() > 0) {
				try {
		    		mCamera = Camera.open(0);
				}
				catch (Exception e) {
					Log.e("Could not access camera", TAG);
					return false;
				}
			}
    	}
    	
    	checkCameraFeatures(0);
    	setCameraFeatures(0, 0, SupportedPreviewFpsRange.size()-1, supportPreviewSize.size()-1);
    	setCamFocusMode();
    	
    	return true;
    }
    
	/*
     * This method check the interested camera feature availability, should be called after getting an instance of the Camera object via Camera.open(mCameraIndex) 
     * parameters are the index of the active camera;
     */
	public void checkCameraFeatures(int mCameraIndex) {
		
		// determine if a camera is on the front or back of the device, and the orientation of the image
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(mCameraIndex, cameraInfo);
		mIsCameraFrontFacing =(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT);
		mNumCameras = Camera.getNumberOfCameras();
		
		params = mCamera.getParameters();

		List<String> focusModes = params.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
		  // Auto focus mode is supported
			mIsAutofocusEnable = true;
		}
		
		focalLength = params.getFocalLength();
		previewSize = params.getPreviewSize();
		params.getPreviewFpsRange(previewFpsRange);
		SupportedPreviewFpsRange = params.getSupportedPreviewFpsRange();
		supportPreviewSize = params.getSupportedPreviewSizes();
		
		Log.i(TAG, "Camera Numbers: " + mNumCameras);
		Log.i(TAG, "Focal Length: " + String.format("%f", focalLength));
		Log.i(TAG, "Camera Preview Size: " + previewSize.width + " x " + previewSize.height);
		Log.i(TAG, "Current Fps Min: " + previewFpsRange[0]);
		Log.i(TAG, "Current Fps Max: " + previewFpsRange[1]);
		for(int i=0; i<SupportedPreviewFpsRange.size(); i++){
			Log.i(TAG, i + "Support Fps Min: " + SupportedPreviewFpsRange.get(i)[0]);
			Log.i(TAG, i + "Support Fps Max: " + SupportedPreviewFpsRange.get(i)[1]);
		}
		for(int i=0; i<supportPreviewSize.size(); i++){
			Log.i(TAG, i + "Support Preview Size: " + supportPreviewSize.get(i).width + " x " + supportPreviewSize.get(i).height);
		}
		
		//Size previewSize = mCamera.getParameters().getPreviewSize();
        int previewFormat = mCamera.getParameters().getPreviewFormat();
        int bytesPerPixel = ImageFormat.getBitsPerPixel(previewFormat) / 8;
        bufferSize = (int)(previewSize.width * previewSize.height * bytesPerPixel * 1.5); //TODO: Don't know why I need this 1.5
		
	}
	
	//If you want to set a specific size for your camera preview, set in the surfaceChanged() method.
	//When setting preview size, you must use values from getSupportedPreviewSizes(). 
	//Do not set arbitrary values in the setPreviewSize() method.
	/*
     * This method set the interested camera feature availability, should be called after getting an instance of the Camera object via Camera.open(mCameraIndex) 
     * and used on Camera.getParameters(); 
     * first need to call checkCameraFeatures to fill the mNumCameras, SupportedPreviewFpsRange, and supportPreviewSize;
     */
	public void setCameraFeatures(int mCameraIndex, int pixel_format, int SupportedPreviewFpsRangeIndex, int supportPreviewSizeIndex) {

		/*if (mNumCameras > 1) mCamera = Camera.open(mCameraIndex);
		else mCamera = Camera.open(0);*/
		
		// get Camera parameters
		params = mCamera.getParameters();
		// changing the returned Camera parameter object and then setting it back into the camera object
		//params.setPreviewFormat(pixel_format);
		//params.setPreviewFormat(ImageFormat.YV12);
		//params.setPreviewFpsRange(SupportedPreviewFpsRange.get(SupportedPreviewFpsRangeIndex)[0], SupportedPreviewFpsRange.get(SupportedPreviewFpsRangeIndex)[1]);
		params.setPreviewFpsRange(SupportedPreviewFpsRange.get(SupportedPreviewFpsRangeIndex)[0], SupportedPreviewFpsRange.get(SupportedPreviewFpsRangeIndex)[1]);
		params.setPreviewSize(supportPreviewSize.get(supportPreviewSizeIndex).width, supportPreviewSize.get(supportPreviewSizeIndex).height);
		// set back Camera parameters
		mCamera.setParameters(params);
		
	}
	
	private Size getOptimalSize() {
	    Camera.Size result = null;
	    final Camera.Parameters parameters = mCamera.getParameters();
	    for (final Camera.Size size : parameters.getSupportedPreviewSizes()) {
	    	//getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels
	        if (size.width <= getWidth() * PREVIEW_SIZE_FACTOR && size.height <= getHeight() * PREVIEW_SIZE_FACTOR) {
	            if (result == null) {
	                result = size;
	            } else {
	                final int resultArea = result.width * result.height;
	                final int newArea = size.width * size.height;

	                if (newArea > resultArea) {
	                    result = size;
	                }
	            }
	        }
	    }
	    if (result == null) {
	        result = parameters.getSupportedPreviewSizes().get(0);
	    }
	    return result;
	}
	
	private void setOptimalSize() {
		final Camera.Parameters params = mCamera.getParameters();
		final Size size = getOptimalSize();
		params.setPreviewSize(size.width, size.height);
		mCamera.setParameters(params);
	}
	
	private void setCamFocusMode(){

	    if(null == mCamera) {
	        return;
	     }

	    /* Set Auto focus */ 
	    Camera.Parameters parameters = mCamera.getParameters();
	    List<String>    focusModes = parameters.getSupportedFocusModes();
	    if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
	        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);   
	    } else 
	    if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
	        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
	    }   

	    mCamera.setParameters(parameters);
	}
	
	public static int getDisplayWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay(); 
        int width = display.getWidth(); 

        return width;
    }

    public static int getDisplayHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay(); 
        int height = display.getHeight(); 

        return height;
    }
    
    private void releaseResources() {
    	if (mCamera != null) {
    		mCamera.stopPreview();
    		//mCamera.addCallbackBuffer(null);
    		mCamera.setPreviewCallbackWithBuffer(null);
    		CameraPreview.this.getHolder().removeCallback(CameraPreview.this);
    		mCamera.release();
    		mCamera = null;
    		this.mHolder=null;
    	}
    }
    
    public void resume() {
        openCamera();
    }
    
    public void pause() {
        releaseResources();
    }
   
    public void destroy() {
    	releaseResources();
    }

	public void onPackageInstall(int operation, InstallCallbackInterface callback) {
		// TODO Auto-generated method stub
		
	}
}
