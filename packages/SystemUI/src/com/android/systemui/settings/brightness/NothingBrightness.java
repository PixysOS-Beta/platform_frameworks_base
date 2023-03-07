package com.android.systemui.settings.brightness;

import android.util.Log;
import android.util.MathUtils;

public class NothingBrightness {
    private static final String TAG = "NothingBrightness";

    private static final float HALF_PERCENTAGE = 0.5f;
    private static final float HUNDRED_PERCENTAGE = 1.0f;
    private static final float NT_HALF_PERCENTAGE = 0.4f;

    private int mMotionAction = 1;

    public float convertToNTSliderValForAutoBrightness(int sliderValue) {
        float f = sliderValue / 65535.0f;
        return (f <= NT_HALF_PERCENTAGE ? (f / NT_HALF_PERCENTAGE) * HALF_PERCENTAGE : (((f - NT_HALF_PERCENTAGE) / 0.6f) * HALF_PERCENTAGE) + HALF_PERCENTAGE) * 65535.0f;
    }

    public float convertToNTSliderValForManual(int sliderValue) {
        float f = sliderValue / 65535.0f;
        return (f <= HALF_PERCENTAGE ? (f * NT_HALF_PERCENTAGE) / HALF_PERCENTAGE : (((f - HALF_PERCENTAGE) / HALF_PERCENTAGE) * 0.6f) + NT_HALF_PERCENTAGE) * 65535.0f;
    }

    public int calculateSliderVal(float startValue, float endValue, float fraction, int currentValue) {
        int lerpInv = (int) (MathUtils.lerpInv(startValue, endValue, fraction) * 65535.0f);
        if (lerpInv == ((int) convertToNTSliderValForManual(currentValue))) {
            Log.d(TAG, "The value in the slider is equal to the value on the current brightness");
            return -1;
        }
        int convertedValue = (int) convertToNTSliderValForAutoBrightness(lerpInv);
        Log.d(TAG, "animateSliderTo: " + convertedValue);
        return convertedValue;
    }

    public void setMotionAction(int action) {
        this.mMotionAction = action;
    }

    public boolean isSliderTouched() {
        int action = this.mMotionAction;
	// action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN;
	return action == 2 || action == 0;
    }

}
