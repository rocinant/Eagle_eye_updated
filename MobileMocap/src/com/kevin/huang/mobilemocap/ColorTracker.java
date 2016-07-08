package com.kevin.huang.mobilemocap;

import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import com.kevin.huang.mobilemocap.frameprocessor.CameraPreview;
import com.kevin.huang.mobilemocap.frameprocessor.ColorFrameProcessedEvent;
import com.kevin.huang.mobilemocap.frameprocessor.ColorFrameProcessor;
import com.kevin.huang.mobilemocap.frameprocessor.Configuration;
import com.kevin.huang.mobilemocap.frameprocessor.ICameraPreviewCallback;
import com.kevin.huang.mobilemocap.frameprocessor.IColorFrameProcessedListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.hardware.Camera.Size;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author yangjianhuang026@gmail.com
 * This activity will track an object based on the color selected by the user.
 */
public class ColorTracker extends Activity implements Runnable, ICameraPreviewCallback, IColorFrameProcessedListener, SurfaceHolder.Callback, OnTouchListener{
	private static final String TAG = "VisualRobot";
	
	private ColorFrameProcessor mFrameProcessor;
	private CameraPreview mPreview;
	private SurfaceView mCameraOverlay;
	private FrameLayout mFrameLayout;
	private ObjectTrackingRobot mRobot;
	
	private Button mTrackButton;
	private TextView mFpsIndicator;
	private long mTotalTime = 0;
	private int mFpsIteration = 0;
	private int mFpsStep = 10;
	private int mDrawIteration = 0;
	private int mDrawStep = 5;
	private boolean mTrackingStarted = false;
	
	private Paint mRectPaint = new Paint();
	private Paint mNoMoveRectPaint = new Paint();
	private DecimalFormat mDecimalFormat = new DecimalFormat("#.##");
	
	private static final String ACTION_USB_PERMISSION = "com.kevin.huang.action.USB_PERMISSION";
	
	private static final byte EvtFrontSonar = 1;
	private static final byte EvtRearSonar = 2;
	
	public static final byte CmdMoveForward = 1;
	public static final byte CmdMoveBackward = 2;
	public static final byte CmdSpinLeft = 3;
	public static final byte CmdSpinRight = 4;
	public static final byte CmdStop = 5;
	public static final byte CmdMoveCameraVert = 6;
	public static final byte CmdMoveCameraHor = 7;
	

	
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	FileUtils fileUtils;
	BufferedWriter bw = null;
	boolean timeFlag=false;
	long timeOfFirstFrame=0;
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "VisualRobot");
			thread.start();
			//enableControls(true);
			Log.d(TAG, "accessory opened");
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		//enableControls(false);

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	//may need to make it full screen
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
        mRobot = new ObjectTrackingRobot(this);
        
        setContentView(R.layout.activity_color_tracker);
        
        mTrackButton = (Button)findViewById(R.id.trackButton);
        mFpsIndicator = (TextView)findViewById(R.id.timeIndicator);
        mFpsIndicator.setTextColor(Color.RED);
        mRectPaint.setARGB(255, 0, 255, 0);
        mRectPaint.setStyle(Style.STROKE);
        mNoMoveRectPaint.setARGB(255, 255, 0, 0);
        mNoMoveRectPaint.setStyle(Style.STROKE);
        
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		
		try {
			fileUtils = new FileUtils(this);
			if (fileUtils.judgeIfSafe2Write (fileUtils.getTxtFilePath ())) {
				bw = new BufferedWriter(new FileWriter(fileUtils.getTxtFile ()));
			} 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public boolean onTouch(View v, MotionEvent event)
	{
    	double ratio = (double)mFrameLayout.getWidth() / mFrameProcessor.getFrameSize().width;
    	int x = (int)event.getX();
    	int y = (int)event.getY();
    	int adjustedX = (int)Math.round(x / ratio);
    	int adjustedY = (int)Math.round(y / ratio);
    	boolean result = mFrameProcessor.onTouch(adjustedX, adjustedY);
    	//v.performClick();
    	mTrackingStarted = true;
    	return result;
	}
      
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void trackButton_Click(View arg) {
    	mTrackButton.setEnabled(false);
    	mFrameProcessor = new ColorFrameProcessor(this);
        mPreview = new CameraPreview(this, mFrameProcessor);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mPreview != null) {
        	mPreview.resume();
        }
        
        if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
        //mRobot.resume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
        	mPreview.pause();
        }
        closeAccessory();
        try{
			if(bw!=null) bw.close();
			MediaScannerConnection.scanFile(fileUtils.context, new String[] { fileUtils.getTxtFile().getAbsolutePath() }, null, null);
		}
		catch(Exception e){
			e.printStackTrace();
		}
        //mRobot.pause();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
        	mPreview.destroy();
        }
        unregisterReceiver(mUsbReceiver);
        try{
			if(bw!=null) bw.close();
			//MediaScannerConnection.scanFile(fileUtils.context, new String[] { fileUtils.getTxtFile().getAbsolutePath() }, null, null);
			MediaScannerConnection mMs = new MediaScannerConnection(fileUtils.context, null);
			mMs.disconnect();
		}
		catch(Exception e){
			e.printStackTrace();
		}
        //mRobot.destroy();
    }
    
    Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EvtFrontSonar:
				Log.d(TAG, ("FrontSonar:" + msg.obj.toString()));
				break;

			case EvtRearSonar:
				Log.d(TAG, ("RearSonar:" + msg.obj.toString()));
				break;

			}
		}
	};
	
	private int getIntFromBytes(byte b1, byte b2, byte b3, byte b4) {
		int value = 0;
		value += b1;
		value <<= 8;
		value += b2;
		value <<= 8;
		value += b3;
		value <<= 8;
		value += b4;
		
		if (value < 0) {
			value += 256;
		}
		
		return value;
	}
	
	private byte[] getBytesFromInt(int i) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (i >> 24);
		bytes[1] = (byte) (i >> 16);
		bytes[2] = (byte) (i >> 8);
		bytes[3] = (byte) i;		
		return bytes;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				switch (buffer[i]) {
				case EvtFrontSonar:
					if (len >= 5) {
						Message m = Message.obtain(mHandler, EvtFrontSonar);
						m.obj = Integer.toString(getIntFromBytes(buffer[i+1], buffer[i+2], buffer[i+3], buffer[i+4]));
						mHandler.sendMessage(m);
					}
					i += 5;
					break;
					
				case EvtRearSonar:
					if (len >= 5) {
						Message m = Message.obtain(mHandler, EvtRearSonar);
						m.obj = Integer.toString(getIntFromBytes(buffer[i+1], buffer[i+2], buffer[i+3], buffer[i+4]));
						mHandler.sendMessage(m);
					}
					i += 5;
					break;


				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
	}
	
	protected void sendCommand(byte command)
    {
		byte[] buffer = new byte[1];
    	buffer[0] = command;
		if (mOutputStream != null){
			try{
				mOutputStream.write(buffer);
			}
			catch(Exception e) {
				Log.e(TAG, "write failed", e);
			}
		}
    }
    
    protected void sendCommand(byte command, int value)
    {
    	byte[] buffer = new byte[5];
		byte[] intAsBytes = getBytesFromInt(value);
    	buffer[0] = command;
    	buffer[1] = intAsBytes[0];
    	buffer[2] = intAsBytes[1];
    	buffer[3] = intAsBytes[2];
    	buffer[4] = intAsBytes[3];
		if (mOutputStream != null){
			try{
				mOutputStream.write(buffer);
			}
			catch(Exception e) {
				Log.e(TAG, "write failed", e);
			}
		}
    }
    
    private class Robot {
    	protected Context context;
    	protected int MaxCameraVertPosition = 180;
    	protected int MaxCameraHorPosition = 180;
    	protected int MinCameraVertPosition = 35;
    	protected int MinCameraHorPosition = 0;
    	protected org.opencv.core.Rect NoMoveRect;
    	
    	protected int CameraVertPosition = 90;
    	protected int CameraHorPosition = 90;
    	
    	protected int forwardSpeed = 0;
    	protected int backwardSpeed = 0;
    	protected int leftSpeed = 0;
    	protected int rightSpeed = 0;
    	
    	public Robot(Context context) {
    		this.context = context;
    	}
    	
    	public void setNoMoveRect(org.opencv.core.Rect rect) {
    		this.NoMoveRect = rect;
    	}
        
        
        public int getCameraVertPosition() {
        	return CameraVertPosition;
        }
        
        public int getCameraHorPosition() {
        	return CameraHorPosition;
        }
        
        public int getMaxCameraHorPosition() {
        	return MaxCameraHorPosition;
        }
        
        public int getMaxCameraVertPosition() {
        	return MaxCameraVertPosition;
        }
        
        public int getMinCameraHorPosition() {
        	return MinCameraHorPosition;
        }
        
        public int getMinCameraVertPosition() {
        	return MinCameraVertPosition;
        }
        
        public int getForwardSpeed() {
        	return forwardSpeed;
        }
        
        public int getLeftSpeed() {
        	return leftSpeed;
        }
        
        public int getRightSpeed() {
        	return rightSpeed;
        }
        
        public int getBackwardSpeed() {
        	return backwardSpeed;
        }
        
        public void MoveForward(int speed) {
        	if (speed >= 0 && speed <= 255)
        	{
        		sendCommand(CmdMoveForward, speed);
        		forwardSpeed = speed;
        	}
        	else if (speed < 0){
        		sendCommand(CmdSpinRight, 0);
        		forwardSpeed = 0;
        	}
        	else {
        		sendCommand(CmdSpinRight, 255);
        		forwardSpeed = 255;
        	}
        }
        
        public void MoveBackward(int speed) {
        	if (speed >= 0 && speed <= 255)
        	{
        		sendCommand(CmdMoveBackward, speed);
        		backwardSpeed = speed;
        	}
        }
        
        public void SpinLeft(int speed) {
        	if (speed >= 0 && speed <= 255)
        	{
        		sendCommand(CmdSpinLeft, speed);
        		leftSpeed = speed;
        	}
        	else if (speed < 0){
        		sendCommand(CmdSpinLeft, 0);
        		leftSpeed = 0;
        	}
        	else {
        		sendCommand(CmdSpinLeft, 255);
        		leftSpeed = 255;
        	}
        }
        
        public void SpinRight(int speed) {
        	if (speed >= 0 && speed <= 255)
        	{
        		sendCommand(CmdSpinRight, speed);
        		rightSpeed = speed;
        	}
        	else if (speed < 0){
        		sendCommand(CmdSpinRight, 0);
        		rightSpeed = 0;
        	}
        	else {
        		sendCommand(CmdSpinRight, 255);
        		rightSpeed = 255;
        	}
        }
        
        public void Stop() {
        	sendCommand(CmdStop);
        }
        
        public void MoveCameraVert(int degrees) {
        	if (degrees >= 0 && degrees <= 180)
        	{
        		sendCommand(CmdMoveCameraVert, degrees);
    	    	CameraVertPosition = degrees;
        	}
        }
        
        public void MoveCameraHor(int degrees) {
        	if (degrees >= 0 && degrees <= 180)
        	{
        		sendCommand(CmdMoveCameraHor, degrees);
    	    	CameraHorPosition = degrees;
        	}
        }
    }
    
    private enum TrackingRobotState {
    	NoMove(0),
    	MoveCameraUpDown(1),
    	DriveLeftRight(2),
    	DriveForwardBackwards(3),
    	Lost(4);
    	
    	public final int id;

    	TrackingRobotState(int id) {
    		this.id = id;
    	}
    }
    
    private class ObjectTrackingRobot extends Robot {
    	private TrackingRobotState robotState = TrackingRobotState.NoMove;
    	private org.opencv.core.Rect objectPosition;
    	private org.opencv.core.Rect lastKnownPosition;
    	public Size frameSize;
    	public org.opencv.core.Rect originalRectangle;
    	private int lostSpeed = 30;
    	private double maxRatio = 1.25;
    	private double minRatio = 0.75;
    	private PIDcontroller  PID;
    	private PIDcontroller  PIDForTurning;
    	/*public double originalRatio = 1.00;
    	private double ratioThreshould = 0.1;
    	private int proportionalTerm = 30;*/
    	
    	private Point objectPoint;
    	//private Point lastObjectPoint;
    	
    	private long timeFrameTaken;
    	private double prepositionDiff;
    	private double preposition;
    	
    	public double orginalArea;
    	public long startTurningTime;
    	private int turningFrame;
    	private final float turningThreshold = 0.009f;
    	private final float wheelWidth = 30.3f;
    	private final float wheelCir = 47.1f;
    	private final float maximumSpeed = 230.0f;
    	private double preTheta;
    	
    	//change to lower cpu to control sonar
/*    	private int frontSonar;
    	private int rearSonar;*/
    	
    	private final int sonarThreshold = 50;
    	private final int robotSpeed = 65;
    	
    	public ObjectTrackingRobot(Context context) {
    		super(context);
    		//PID = new PIDcontroller(0, 0, 0.20f);
    		//PID = new PIDcontroller(0.25f, 0, 0.05f);
    		//PID = new PIDcontroller(0.2, 0, 0);
    		PID = new PIDcontroller(16, 0, 0);
    		PID.enable();
    		timeFrameTaken = System.currentTimeMillis();
    		prepositionDiff = 0.0f;
    		
    		PIDForTurning = new PIDcontroller(0.3*1000*wheelWidth/wheelCir*60*255/maximumSpeed, 0, 0);
    		PIDForTurning.enable();
    		preTheta = 0.0f;
    	}
    	
       	public void  TrackObject(long timeFrameTaken, List<Point> centroidList, double LowestMarkerAreas)
        {
    		
       		
       		if (centroidList.size() == Configuration.TARGET_NUMBER){
       			
       			//remember need to keep the lowest marker consistent
           		double maximumYcoordinator = 0;
           		int indiceYaxisMaximum = 0;
       			for (int i=0; i<centroidList.size(); i++){
    				if ( centroidList.get(i).y > maximumYcoordinator){
    					maximumYcoordinator = centroidList.get(i).y;
    					indiceYaxisMaximum = i;
    					}
    				}
           		this.objectPoint = centroidList.get(indiceYaxisMaximum);
           		
       			/*if ( (LowestMarkerAreas / this.orginalArea) < 0.98f){
       				double theta = Math.acos(LowestMarkerAreas / this.orginalArea);
       		       	//SpinRight( (int)(60*255*theta/(timeFrameTaken - this.timeFrameTaken)*wheelWidth/wheelCir*1000*0.3f/maximumSpeed));
       		       	//SpinRight( (int)(60*255*(++turningFrame)*theta/(timeFrameTaken - this.startTurningTime)*wheelWidth/wheelCir*1000*0.3f/maximumSpeed));
       		       	SpinRight(PIDForTurning.updatePid(getRightSpeed(), 0, (preTheta - theta)/(timeFrameTaken - this.timeFrameTaken), theta - preTheta, (int) (timeFrameTaken - this.timeFrameTaken) ));
       		       	preTheta = theta;
       		       	//this.orginalArea = LowestMarkerAreas;
       			}
       			if ( centroidList.size() == Configuration.TARGET_NUMBER && this.orginalArea > LowestMarkerAreas && ( Math.sqrt( Math.pow(this.orginalArea, 2.0f) - Math.pow(LowestMarkerAreas, 2.0f) ) / this.orginalArea) > turningThreshold){
       				double ratio = Math.sqrt( Math.pow(this.orginalArea, 2.0f) - Math.pow(LowestMarkerAreas, 2.0f) ) / this.orginalArea ;
       		       	SpinRight( (int)(60*255*ratio/(timeFrameTaken - this.timeFrameTaken)*wheelWidth/wheelCir*1000/maximumSpeed));
       		       	this.orginalArea = LowestMarkerAreas;
       			}
       			else{*/
               		/*
           			if (centroidList.size() > 1 && this.originalRatio - ratio > ratioThreshould )
    	    		{
    	    			//the vehicle is turning left should design a PID controller to turn the wheel left
           				//SpinLeft(speed);
           				double speedDiff = proportionalTerm * ( this.originalRatio - ratio ) / (timeFrameTaken - this.timeFrameTaken);
           				SpinLeft((int)speedDiff);
    	    		}
           			else if (centroidList.size() > 1 && ratio - this.originalRatio>  ratioThreshould)
    	    		{
        				//the vehicle is turning right should design a PID controller to turn the wheel right
           				//SpinRight(speed)
           				double speedDiff = proportionalTerm * ( ratio - this.originalRatio ) / (timeFrameTaken - this.timeFrameTaken);
           				SpinRight((int)speedDiff);
    	    		}
           			else{*/
               		//if (centroidList.size() > 0){
    		    		//dead zone keep straight;
    	       		    //the speed control point should be the lowest in Y axis thus have the maximum in Y coordinators
    	           		//thus we need to sort it and find the point that has the maximum Y coordinators
    	           		
    	        		//this.objectPoint = centroidList.get(0);
    	           		//this.lastObjectPoint = this.objectPoint;
    	        		
    	        		
    	        		/*if ((int) this.objectPoint.y*4 > (int)Math.round(this.frameSize.height) / 2) {
    	        			this.MoveCameraVert (90 - (int)(((int) this.objectPoint.y*4 - (int)Math.round(this.frameSize.height) / 2) *(90 - MinCameraVertPosition) / ((int)Math.round(this.frameSize.height)/2)));
    	        			}
    	        		else if ((int) this.objectPoint.y*4 < (int)Math.round(this.frameSize.height) / 2) {
    	        			this.MoveCameraVert (90 + (int)(((int)Math.round(this.frameSize.height) / 2 - (int) this.objectPoint.y*4) *(MaxCameraVertPosition - 90) / ((int)Math.round(this.frameSize.height)/2)));
    	        			}*/
    	        		
    	                /*if ((int) this.objectPoint.x*4 > (int)Math.round(this.frameSize.width) / 2) {
    	        			this.MoveCameraHor(90 + (int)(((int) this.objectPoint.x*4 - (int)Math.round(this.frameSize.height) / 2) *(90 - MinCameraHorPosition) / ((int)Math.round(this.frameSize.width)/2)));
    	        			}
    	        		else if ((int) this.objectPoint.x*4 < (int)Math.round(this.frameSize.width) / 2) {
    	        			this.MoveCameraHor(90 - (int)(((int)Math.round(this.frameSize.width) / 2 - (int) this.objectPoint.x*4) *(MaxCameraHorPosition + 90) / ((int)Math.round(this.frameSize.width)/2)));
    	        			}*/
    	        		
    	        		//MoveForward(PID.updatePid(getForwardSpeed(), (float)this.objectPoint.x*4, this.frameSize.width / 2));
    	
    	        		//MoveForward(PID.updatePid(getForwardSpeed(), 0, (float)( (this.frameSize.width / 2 - this.objectPoint.x*4) / (timeFrameTaken - this.timeFrameTaken) )));
    	        		
    	        		MoveForward(PID.updatePid(getForwardSpeed(), 0, ( ( preposition - this.objectPoint.x*4 ) / (timeFrameTaken - this.timeFrameTaken) ), this.objectPoint.x*4 - preposition, (int) (timeFrameTaken - this.timeFrameTaken) ));
    	        		turningFrame = 0;
    	        		preTheta = 0.0f;
    	        		startTurningTime = timeFrameTaken;
    	        		
    	        		//MoveForward(PID.updatePid( getForwardSpeed(), this.objectPoint.x*4, preposition, this.objectPoint.x*4 - preposition, (int) (timeFrameTaken - this.timeFrameTaken) ));
    	        		/*Log.i(TAG, "Elapse Time: " + (int) (timeFrameTaken - this.timeFrameTaken) );
    	        		Log.i(TAG, "Position Differ: " + ((this.frameSize.width / 2 - this.objectPoint.x*4) - prepositionDiff) );*/
    	        		/*Log.i(TAG, "Position Differ: " + (0- ((this.frameSize.width / 2 - this.objectPoint.x*4) - prepositionDiff)) );*/ 
    	        		/*Log.i(TAG, "PID term:" + PID.pidTerm );
    	        		Log.i(TAG, "PID result:" + (int) PID.m_result );*/
    	        		
    	        		
    	        		
    	        		//this.prepositionDiff = (this.frameSize.width / 2 - this.objectPoint.x*4);
       			//}
       			
       			this.preposition = (this.objectPoint.x*4);
	        	this.timeFrameTaken = timeFrameTaken;
	    		//}
       			
       		}
       		
    		
    		/*if (this.objectPoint == null)
    		{
    			
    		}*/
    		
    		else
    		{
    			//lost();
    			Stop();
    			//this.preposition = 0.0f;
        		this.timeFrameTaken = timeFrameTaken;
    		}
    		
        }
       	
    	public void  TrackObject(org.opencv.core.Rect objectPosition)
        {
    		//Log.i("TAG", this.robotState.toString());
    		
    		this.objectPosition = objectPosition;
    		if (this.objectPosition == null)
    		{
    			this.robotState = TrackingRobotState.Lost;
    		}
    		else
    		{
    			this.lastKnownPosition = this.objectPosition;
    		}
    		
    		switch(robotState)
    		{
    			case NoMove:
    				noMove();
    				break;
    			case MoveCameraUpDown:
    				moveCameraUpDown();
    				break;
    			case DriveLeftRight:
    				driveLeftRight();
    				break;
    			case DriveForwardBackwards:
    				driveForwardBackward();
    				break;
    			case Lost:
    				lost();
    				break;
    		}
        }
    	
    	//Following logic may need to change
    	private void noMove()
    	{
    		this.robotState = TrackingRobotState.NoMove;
    		this.Stop();
    		boolean moveTop = objectPosition.y < this.NoMoveRect.y;
    		boolean moveRight = objectPosition.x + objectPosition.width > this.NoMoveRect.x + this.NoMoveRect.width;
    		boolean moveBottom = objectPosition.y + objectPosition.height > this.NoMoveRect.y + this.NoMoveRect.height;
    		boolean moveLeft = objectPosition.x < this.NoMoveRect.x;
    		double ratio = (double)objectPosition.width / this.originalRectangle.width;
    		
    		if (moveTop || moveBottom)
    		{
    			moveCameraUpDown();
    		}
    		else if (moveLeft || moveRight)
    		{
    			driveLeftRight();
    		}
    		else if (ratio < minRatio || ratio > maxRatio)
    		{
    			driveForwardBackward();
    		}
    	}
    	
    	private void lost()
    	{
    		this.robotState = TrackingRobotState.Lost;
    		if (this.objectPosition == null)
    		{
    			int objectCenter = this.lastKnownPosition.x + (this.lastKnownPosition.width / 2);
    			int frameCenter = (int)Math.round(this.frameSize.width) / 2;
    			
    			if (objectCenter < frameCenter)
    			{
    				this.SpinLeft(lostSpeed);
    			}
    			else
    			{
    				this.SpinRight(lostSpeed);
    			}
    		}
    		else
    		{
    			noMove();
    		}
    	}
    	
    	private void moveCameraUpDown()
    	{
    		this.robotState = TrackingRobotState.MoveCameraUpDown;
    		boolean moveTop = objectPosition.y < this.NoMoveRect.y;
    		boolean moveBottom = objectPosition.y + objectPosition.height > this.NoMoveRect.y + this.NoMoveRect.height;
    		
    		if(moveTop)
        	{
        		if (this.getCameraVertPosition() < this.getMaxCameraVertPosition())
        		{
        			this.MoveCameraVert(this.getCameraVertPosition() + 5);
        		}
        	}
    		else if (moveBottom)
    		{
    			if (this.getCameraVertPosition() > this.getMinCameraVertPosition())
        		{
        			this.MoveCameraVert(this.getCameraVertPosition() - 5);
        		}
    		}
    		else
    		{
    			noMove();
    		}
    	}

    	private void driveLeftRight()
    	{
    		this.robotState = TrackingRobotState.DriveLeftRight;
    		boolean moveRight = objectPosition.x + objectPosition.width > this.NoMoveRect.x + this.NoMoveRect.width;
    		boolean moveLeft = objectPosition.x < this.NoMoveRect.x;
    		
    		if (moveLeft)
    		{
    			this.SpinLeft(robotSpeed);
    		}
    		else if (moveRight)
    		{
    			this.SpinRight(robotSpeed);
    		}
    		else
    		{
    			noMove();
    		}
    	}

    	private void driveForwardBackward()
    	{
    		this.robotState = TrackingRobotState.DriveForwardBackwards;
    		double ratio = (double)objectPosition.width / this.originalRectangle.width;
    		
    		if (ratio < minRatio)
    		{
    			//if (this.frontSonar > sonarThreshold)
    			{
    				this.MoveForward(robotSpeed);
    			}
    			
/*    			else
    			{
    				this.robotState = TrackingRobotState.NoMove;
    			}*/
    			
    		}
    		else if (ratio > maxRatio)
    		{
    			//if (this.rearSonar > sonarThreshold)
    			{
    				this.MoveBackward(robotSpeed);
    			}
    			
/*    			else
    			{
    				this.robotState = TrackingRobotState.NoMove;
    			}*/
    			
    		}
    		else
    		{
    			noMove();
    		}
    	}
    }
    
    private class PointComparator implements Comparator<Integer>
	{
	    private final double[] array;

	    public PointComparator(double[] array)
	    {
	        this.array = array;
	    }

	    public Integer[] createIndexArray()
	    {
	        Integer[] indexes = new Integer[array.length];
	        for (int i = 0; i < array.length; i++)
	        {
	            indexes[i] = i; // Autoboxing
	        }
	        return indexes;
	    }

	    @Override
	    public int compare(Integer index1, Integer index2)
	    {
	         // Autounbox from Integer to int to use as array indexes
	        //return array[index1].compareTo(array[index2]);
	    	return Double.compare(array[index1], array[index2]);
	    }
	}
    
    public void cameraPreviewLoaded() {
    	//This is called after OpenCV library has been initialized
    	mCameraOverlay = new SurfaceView(this);
        mCameraOverlay.getHolder().setFormat(PixelFormat.TRANSPARENT);
    	
        mFrameLayout = (FrameLayout)findViewById(R.id.camera_preview);
    	mFrameLayout.addView(mCameraOverlay);
    	mFrameLayout.addView(mPreview);
    	
    	mCameraOverlay.getHolder().addCallback(this);
    	mPreview.setOnTouchListener(this);
    	
    	mRobot.setNoMoveRect(mFrameProcessor.getNoMoveRect());
    	
    	mPreview.checkCameraFeatures(0);
    	
    	mRobot.preposition = mFrameProcessor.getFrameSize().width / 2;
    }
    
    public void onFrameProcessed(ColorFrameProcessedEvent event) {
    	
    	mDrawIteration++;
    	mFpsIteration++;
    	mTotalTime += event.time;
    	
		if (mFpsIteration == mFpsStep) {
    		double fps = (1000.0 * mFpsStep) / mTotalTime;
    		mFpsIndicator.setText(mDecimalFormat.format(fps));
    		mTotalTime = mFpsIteration = 0;
    	}
		
		if (mTrackingStarted)
		{
			//double ratio = 1.00;
			//need at least two target to calculate the ratio
			/*if (event.CentroidList.size() > 1){
				 //This part we sort the Centroid's y axis and keep tracking the indices
		        double [] pointYaxis = new double [event.CentroidList.size()];
		        for (int i=0; i < event.CentroidList.size(); i++){
		        	pointYaxis [i] = event.CentroidList.get(i).y;
	        	}
		        PointComparator comparator = new PointComparator(pointYaxis);
		        Integer[] indexes = comparator.createIndexArray();
		        Arrays.sort(indexes, comparator);
		        ratio = (event.CentroidList.get(indexes[indexes.length - 1 ]).x > event.CentroidList.get(indexes[indexes.length - 2 ]).x) ? (double)(event.objectPosition.get(indexes[indexes.length - 1 ]).width / event.objectPosition.get(indexes[indexes.length - 2 ]).width) : (double)(event.objectPosition.get(indexes[indexes.length - 2 ]).width / event.objectPosition.get(indexes[indexes.length - 1 ]).width) ;
		        if (mRobot.originalRatio == 1.00) mRobot.originalRatio = ratio;
			}*/
	        
			if (mRobot.originalRectangle == null)
			{
				//need to store the ratio between two markers(both at the bottom maximum y axis and one of them have larger x axis, at the front of the foot)' width at the beginning (objectPosition_x_bigger.width/objectPosition_x_smaller.width) and if this ratio get larger then it means subject is turning right and if this ratio turning smaller then subject is turning left, try adjust the robot to follow it 
				//need sort to find the appropriate one
				mRobot.originalRectangle = event.objectPosition.get(0);
				mRobot.frameSize = mFrameProcessor.getFrameSize();
				mRobot.orginalArea = event.LowestMarkerAreas;
				
				
			}
			
			//mRobot.setNoMoveRect(event.noMove);
			//mRobot.TrackObject(event.objectPosition);
			mRobot.TrackObject(event.timeFrameTaken, event.CentroidList, event.LowestMarkerAreas);
			
			try{
				if (timeFlag == false) {
					timeOfFirstFrame = event.timeFrameTaken;
					timeFlag=true;}
				
				if ( event.CentroidList.size() == Configuration.TARGET_NUMBER ){
				bw.write(String.format("%d ", (event.timeFrameTaken - timeOfFirstFrame) ));
				for (int i=0; i < event.CentroidList.size(); i++){
					//timeOfPreviousFrame = event.timeFrameTaken;
					bw.write(String.format("%f ", (float) ( event.CentroidList.get(i).x*4) ));
					bw.write(String.format("%f ", (float) ( event.CentroidList.get(i).y*4) ));
					}
				bw.write("\r\n");
	            bw.flush();
				}
	            
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
			/*float data [] = new float [event.CentroidList.size()*2];
			for (int i=0; i<data.length; i++){
				data [i] =  (float) round ( event.CentroidList.get(i/2).x, 3);
				data [++i] = (float) round ( event.CentroidList.get(i/2).y, 3);
				data [i] =  (float) ( event.CentroidList.get(i/2).x*4);
				data [++i] = (float) ( event.CentroidList.get(i/2).y*4);
			}
			//fileUtils.write2SDFromFloatFileWriter(data);
			try{
				
				for (float f: data)
		         {
		        	 bw.write(String.format("%f ", f));
		         }
				
				//bw.newLine();
				//bw.write(System.getProperty("line.separator"));
				bw.write("\r\n");
	            bw.flush();
			}
			
			catch(Exception e){
				e.printStackTrace();
			}*/
			
		}
			
		if (mDrawIteration % mDrawStep == 0) {
			if (mTrackingStarted)
			{
				/*if (mRobot.originalRectangle == null)
				{
					mRobot.originalRectangle = event.objectPosition;
					mRobot.frameSize = mFrameProcessor.getFrameSize();
				}*/
				
				//mRobot.setNoMoveRect(event.noMove);
				//mRobot.TrackObject(event.objectPosition);
				//mRobot.TrackObject(event.CentroidList);
				
				if (event.objectPosition != null) {				
					Canvas canvas = mCameraOverlay.getHolder().lockCanvas();
					if (canvas != null) {
						canvas.drawColor(0, Mode.CLEAR);
						
						Iterator<org.opencv.core.Rect> each = event.objectPosition.iterator();
		     	        while (each.hasNext()) {
		     	        	org.opencv.core.Rect rect = each.next();
		     	        	canvas.drawRect(convertRectangle(rect), mRectPaint);
		     	        }
		     	        
						//canvas.drawRect(convertRectangle(event.objectPosition), mRectPaint);
						
						//canvas.drawRect(convertRectangle(mFrameProcessor.getNoMoveRect()), mNoMoveRectPaint);
						//canvas.drawPoint((int) event.CentroidList.get(0).x*4, (int) event.CentroidList.get(0).y*4, mRectPaint);
						//canvas.drawPoint(mFrameLayout.getWidth() / 2, mFrameLayout.getHeight() / 2, mNoMoveRectPaint);
						/*canvas.drawLine(mFrameLayout.getWidth() / 2, mFrameLayout.getHeight() / 2 - 100, mFrameLayout.getWidth() / 2, mFrameLayout.getHeight() / 2 + 100, mNoMoveRectPaint);
						canvas.drawLine(mFrameLayout.getWidth() / 2 -100, mFrameLayout.getHeight() / 2, mFrameLayout.getWidth() / 2 +100, mFrameLayout.getHeight() / 2, mNoMoveRectPaint);
						canvas.drawRect(convertRectangle(mFrameProcessor.getNoMoveRect()), mNoMoveRectPaint);*/
						canvas.drawLine(mFrameProcessor.getFrameSize().width / 2, mFrameProcessor.getFrameSize().height / 2 - 100, mFrameProcessor.getFrameSize().width / 2, mFrameProcessor.getFrameSize().height / 2 + 100, mNoMoveRectPaint);
						canvas.drawLine(mFrameProcessor.getFrameSize().width / 2 -100, mFrameProcessor.getFrameSize().height / 2, mFrameProcessor.getFrameSize().width / 2 +100, mFrameProcessor.getFrameSize().height / 2, mNoMoveRectPaint);
						
						for (int i=0; i < event.CentroidList.size(); i++){
							canvas.drawCircle((float)event.CentroidList.get(i).x*4, (float)event.CentroidList.get(i).y*4, 15, mRectPaint);
		            	}
						//canvas.drawCircle((float)event.CentroidList.get(0).x*4, (float)event.CentroidList.get(0).y*4, 15, mRectPaint);
						
						//Log.i("NOMOVE: " + mFrameProcessor.getNoMoveRect().x + ", " + mFrameProcessor.getNoMoveRect().y + ", " + mFrameProcessor.getNoMoveRect().width + ", " + mFrameProcessor.getNoMoveRect().height, TAG);
						
						mCameraOverlay.getHolder().unlockCanvasAndPost(canvas);
					}
				}
				
			}
			mDrawIteration = 0;
		}
		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		double cameraAspectRatio = (double)mFrameProcessor.getFrameSize().width / mFrameProcessor.getFrameSize().height;
		double screenAspectRatio = (double)mFrameLayout.getWidth() / mFrameLayout.getHeight();
		/*Log.i("Camera: " + mFrameProcessor.getFrameSize().width + " x " + mFrameProcessor.getFrameSize().height, TAG);
		Log.i("Screen: " + mFrameLayout.getWidth() + " x " + mFrameLayout.getHeight(), TAG);*/
		if (cameraAspectRatio < screenAspectRatio) {
			int newWidth = (int)Math.floor(mFrameLayout.getHeight() * cameraAspectRatio);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(newWidth, mFrameLayout.getHeight());
			lp.leftMargin = (mFrameLayout.getWidth() - newWidth) / 2;
			mFrameLayout.setLayoutParams(lp);
		}
		else if (cameraAspectRatio > screenAspectRatio) {
			int newHeight = (int)Math.floor(mFrameLayout.getWidth() / cameraAspectRatio);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mFrameLayout.getWidth(), newHeight);
			lp.topMargin = (mFrameLayout.getHeight() - newHeight) / 2;
			mFrameLayout.setLayoutParams(lp);
		}
		
		/*RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mFrameProcessor.getFrameSize().width, mFrameProcessor.getFrameSize().height);
		mFrameLayout.setLayoutParams(lp);*/
		
		Log.i("Camera: " + mFrameProcessor.getFrameSize().width + " x " + mFrameProcessor.getFrameSize().height, TAG);
		Log.i("Screen: " + mFrameLayout.getWidth() + " x " + mFrameLayout.getHeight(), TAG);
		
		//set CameraOverlay surfaceview's size, CameraOverlay used to draw things on camera preview frame, their size are same
		mCameraOverlay.getHolder().setFixedSize(mFrameProcessor.getFrameSize().width, mFrameProcessor.getFrameSize().height);
		
		Canvas canvas = null;
		while (canvas == null) {
			canvas = mCameraOverlay.getHolder().lockCanvas();
			if (canvas != null) {
				canvas.drawColor(0, Mode.CLEAR);
				//canvas.drawPoint(mFrameLayout.getWidth() / 2, mFrameLayout.getHeight() / 2, mNoMoveRectPaint);
				canvas.drawPoint(mFrameProcessor.getFrameSize().width / 2, mFrameProcessor.getFrameSize().height / 2, mNoMoveRectPaint);
				canvas.drawRect(convertRectangle(mFrameProcessor.getNoMoveRect()), mNoMoveRectPaint);
				//canvas.drawPoint((int) event.CentroidList.get(0).x*4, (int) event.CentroidList.get(0).y*4, mRectPaint);
				canvas.drawPoint(mFrameProcessor.getFrameSize().width / 2, mFrameProcessor.getFrameSize().height / 2, mNoMoveRectPaint);
				canvas.drawLine(mFrameProcessor.getFrameSize().width / 2, mFrameProcessor.getFrameSize().height / 2 - 100, mFrameProcessor.getFrameSize().width / 2, mFrameProcessor.getFrameSize().height / 2 + 100, mNoMoveRectPaint);
				canvas.drawLine(mFrameProcessor.getFrameSize().width / 2 -100, mFrameProcessor.getFrameSize().height / 2, mFrameProcessor.getFrameSize().width / 2 +100, mFrameProcessor.getFrameSize().height / 2, mNoMoveRectPaint);
				mCameraOverlay.getHolder().unlockCanvasAndPost(canvas);
			}
		}
		//don not quite understand why need to invalidate it //通过调用这个方法让系统自动刷新视图  更新整个屏幕区域
		mCameraOverlay.invalidate();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
	//from opencv Rect to android Rect object
	private Rect convertRectangle(org.opencv.core.Rect r) {
    	return new Rect(r.x, r.y, r.x + r.width, r.y + r.height);
    }
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
}
