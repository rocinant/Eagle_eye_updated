package com.kevin.huang.mobilemocap;

import android.content.res.Resources;
import android.view.View;

public abstract class AccessoryController {

	protected MainActivity mHostActivity;

	public AccessoryController(MainActivity activity) {
		mHostActivity = activity;
	}

	protected View findViewById(int id) {
		return mHostActivity.findViewById(id);
	}

	protected Resources getResources() {
		return mHostActivity.getResources();
	}

	void accessoryAttached() {
		onAccesssoryAttached();
	}

	abstract protected void onAccesssoryAttached();

}