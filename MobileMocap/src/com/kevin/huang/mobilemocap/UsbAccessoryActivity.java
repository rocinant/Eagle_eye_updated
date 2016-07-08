package com.kevin.huang.mobilemocap;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/* This Activity does nothing but receive USB_DEVICE_ATTACHED events from the
 * USB service and springboards to the main Gallery activity
 */
public final class UsbAccessoryActivity extends Activity {

	static final String TAG = "VisualRobot";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = VisualRobotLaunch.createIntent(this);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "unable to start VisualRobot activity", e);
		}
		finish();
	}
}
