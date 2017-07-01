package com.imagerecognition;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by X on 15/5/11.
 */
public abstract class ContentGenerator {

    private MainActivity mActivity = null;

    public ContentGenerator(MainActivity activity) {
        mActivity = activity;
    }

    public abstract View generate();
    public abstract View getCameraApertureView();
    public abstract void onPictureToken(Bitmap srcBitmap);

    protected MainActivity getActivity() {
        return mActivity;
    }
}
