package com.kevin.huang.mobilemocap;

import android.support.v7.app.ActionBarActivity;
import android.util.Log;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements Runnable{
	
	private SeekBar CameraVertseekBar;
	private SeekBar CameraHorseekBar;
	private SeekBar ForwardseekBar;
	private SeekBar BackwardseekBar;
	private SeekBar LeftseekBar;
	private SeekBar RightseekBar;
	private TextView FrontSonar;
	private TextView RearSonar;
	private Button StopButton;
	private Button CameraButton;
	
	private static final String TAG = "VisualRobot";
	
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
	
	protected int CameraVertPosition = 90;
	protected int CameraHorPosition = 90;
	
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		FrontSonar=(TextView)findViewById(R.id.FrontSonarTextview);
		RearSonar=(TextView)findViewById(R.id.RearSonarTextview);
		
		CameraVertseekBar=(SeekBar)findViewById(R.id.CameraVertseekBar);
		CameraVertseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
		
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					MoveCameraVert(progress);
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//MoveCameraVert(seekBar.getProgress());
			}
		});
		CameraHorseekBar=(SeekBar)findViewById(R.id.CameraHorseekBar);
		CameraHorseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					MoveCameraHor(progress);
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//MoveCameraHor(seekBar.getProgress());
			}
		});
		ForwardseekBar=(SeekBar)findViewById(R.id.ForwardseekBar);
		ForwardseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					MoveForward(progress);
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//MoveForward(seekBar.getProgress());
			}
		});
		BackwardseekBar=(SeekBar)findViewById(R.id.BackwardseekBar);
		BackwardseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					MoveBackward(progress);
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//MoveBackward(seekBar.getProgress());
			}
		});
		LeftseekBar=(SeekBar)findViewById(R.id.LeftseekBar);
		LeftseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					SpinLeft(progress);
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//SpinLeft(seekBar.getProgress());
			}
		});
		RightseekBar=(SeekBar)findViewById(R.id.RightseekBar);
		RightseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					SpinRight(progress);
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//SpinRight(seekBar.getProgress());
			}
		});
		
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		
		StopButton=(Button)findViewById(R.id.StopButton);
		StopButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Stop();
			}
		});
		
		CameraButton=(Button)findViewById(R.id.CameraButton);
		CameraButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent= new Intent(MainActivity.this, ColorTracker.class);
				startActivity(intent);
			}
		});
		//enableControls(false);
	}

	@Override
	public void onResume() {
		super.onResume();

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
	}
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		closeAccessory();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mUsbReceiver);
	}
	
	 Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EvtFrontSonar:
				FrontSonar.setText("FrontSonar:" + msg.obj.toString());
				break;

			case EvtRearSonar:
				RearSonar.setText("RearSonar:" + msg.obj.toString());
				break;

			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
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
	
	protected void enableControls(boolean enable) {
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
    
    public void MoveForward(int speed) {
    	if (speed >= 0 && speed <= 100)
    	{
    		sendCommand(CmdMoveForward, speed);
    	}
    }
    
    public void MoveBackward(int speed) {
    	if (speed >= 0 && speed <= 100)
    	{
    		sendCommand(CmdMoveBackward, speed);
    	}
    }
    
    public void SpinLeft(int speed) {
    	if (speed >= 0 && speed <= 100)
    	{
    		sendCommand(CmdSpinLeft, speed);
    	}
    }
    
    public void SpinRight(int speed) {
    	if (speed >= 0 && speed <= 100)
    	{
    		sendCommand(CmdSpinRight, speed);
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
	    	CameraVertPosition = degrees;
    	}
    }
    
}
