package com.kevin.huang.mobilemocap;

import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;
import android.view.ViewGroup;
import android.widget.TextView;

public class ServoController implements Slider.SliderPositionListener {
	private final int mServoNumber;
	private TextView mLabel;
	private Slider mSlider;
	private MainActivity mActivity;

	public ServoController(MainActivity activity, int servoNumber) {
		mActivity = activity;
		mServoNumber = servoNumber;
	}

	public void attachToView(ViewGroup targetView) {
		mLabel = (TextView) targetView.getChildAt(0);
		SpannableStringBuilder ssb = new SpannableStringBuilder("Servo");
		ssb.append(String.valueOf(mServoNumber));
		ssb.setSpan(new SubscriptSpan(), 5, 6, 0);
		ssb.setSpan(new RelativeSizeSpan(0.7f), 5, 6, 0);
		mLabel.setText(ssb);
		mSlider = (Slider) targetView.getChildAt(1);
		mSlider.setPositionListener(this);
	}

	public void onPositionChange(double value) {
		int v;
		 switch(mServoNumber){
	      case 1:
	        {
	        v = (int) (value * 180);
			mActivity.sendCommand(MainActivity.CmdMoveCameraVert, v);
	        break;
	        }
	      case 2:
	        {
	        v = (int) (value * 180);
	        mActivity.sendCommand(MainActivity.CmdMoveCameraHor, v);
	        break;
	        }
	      case 3:
	        {
	        v = (int) (value * 100);
	        mActivity.sendCommand(MainActivity.CmdMoveForward, v);
	        break;
	        }
	      case 4:
	        {
	        v = (int) (value * 100);
	        mActivity.sendCommand(MainActivity.CmdMoveBackward, v);
	        break;
	        }
	      case 5:
	        {
	        v = (int) (value * 100);
	        mActivity.sendCommand(MainActivity.CmdSpinLeft, v);
	        break;
	        }
	      case 6:
	        {
	        v = (int) (value * 100);
	        mActivity.sendCommand(MainActivity.CmdSpinRight, v);
	        break;
	        }
	}
	}
}

