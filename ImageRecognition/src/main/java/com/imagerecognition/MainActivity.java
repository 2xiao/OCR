package com.imagerecognition;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.xiaopan.easy.android.util.BitmapUtils;
import me.xiaopan.easy.android.util.FileUtils;
import me.xiaopan.easy.android.util.RectUtils;
import me.xiaopan.easy.android.util.ViewAnimationUtils;
import me.xiaopan.easy.android.util.camera.CameraManager;
import me.xiaopan.easy.android.util.camera.CameraOptimalSizeCalculator;
import me.xiaopan.easy.android.util.camera.CameraManager.CamreaBeingUsedException;
import me.xiaopan.easy.java.util.IOUtils;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Toast;

/**
 * Created by X on 15/5/11.
 */
public class MainActivity extends Activity implements CameraManager.CameraCallback, Camera.PictureCallback {

    private boolean readTakePhotos = false;//准备拍照
	private List<String> supportedFlashModes;	//当前设备支持的闪光模式

    private ViewPager mPager = null;
    private SurfaceView surfaceView;	//Surface视图
    private Rect cameraApertureRect;	//取景框的位置

    private CameraManager cameraManager;	//相机管理器

    private ContentGenerator mScanGenerator = null;
    private ContentGenerator mIDCardGenerator = null;
    private ContentGenerator mBankCardGenerator = null;
    
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.txt_scan:
                    mPager.setCurrentItem(0, true);
                    break;

                case R.id.txt_ic:
                    mPager.setCurrentItem(1, true);
                    break;

                case R.id.txt_other1:
                    mPager.setCurrentItem(2, true);
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new ContentPagerAdapter());
        surfaceView = (SurfaceView) findViewById(R.id.surface_takeBusinessCard);
        initTxtSwitchers();
        initCamera();
    }

	private void initCamera() {
		/* 初始化相机关机器以及按钮 */
        cameraManager = new CameraManager(this, surfaceView.getHolder(), this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			cameraManager.openBackCamera(true);
		} catch (CamreaBeingUsedException e) {
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "打开相机失败", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	private void initTxtSwitchers() {
        findViewById(R.id.txt_scan).setOnClickListener(mOnClickListener);
        findViewById(R.id.txt_ic).setOnClickListener(mOnClickListener);
        findViewById(R.id.txt_other1).setOnClickListener(mOnClickListener);
    }
    
    private ContentGenerator getGenerator(int position) {
    	switch (position) {
    		case 0:
    			if (mScanGenerator == null) {
    				mScanGenerator = new ScanViewGenerator(MainActivity.this);
    			}
    			return mScanGenerator;
    			
    		case 1:
    			if (mIDCardGenerator == null) {
    				mIDCardGenerator = new IDCardViewGenerator(MainActivity.this);
    			}
    			return mIDCardGenerator;
    			
    		case 2:
    			if (mBankCardGenerator == null) {
    				mBankCardGenerator = new BankCardViewGenerator(MainActivity.this);
    			}
    			return mBankCardGenerator;

		default:
			return null;
		}
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    private class ContentPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ContentGenerator generator = getGenerator(position);
            if (generator == null) {
            	return null;
            }
            View view = generator.generate();
            container.addView(view);
            return view;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }

    @Override
    public void onInitCamera(Camera camera) {
        Camera.Parameters cameraParameters = camera.getParameters();

		/* 设置闪光模式 */
        supportedFlashModes = new ArrayList<String>(3);
        supportedFlashModes.add(Camera.Parameters.FLASH_MODE_OFF);
        supportedFlashModes.add(Camera.Parameters.FLASH_MODE_ON);

		/* 设置预览和输出分辨率 */
        Camera.Size[] optimalSizes = new CameraOptimalSizeCalculator().getPreviewAndPictureSize(surfaceView.getWidth(), surfaceView.getHeight(), cameraParameters.getSupportedPreviewSizes(), cameraParameters.getSupportedPictureSizes());
        Log.d("Test", "232");
        cameraParameters.setPreviewSize(optimalSizes[0].width, optimalSizes[0].height);
        cameraParameters.setPictureSize(optimalSizes[1].width, optimalSizes[1].height);

        camera.setParameters(cameraParameters);
    }

    @Override
    public void onStartPreview() {
        cameraManager.autoFocus();
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if(success){
            if(readTakePhotos){
                readTakePhotos = false;
                cameraManager.takePicture(null, null, this);
                Log.d("Test", "257");
            }
        }else{
            cameraManager.autoFocus();
            Log.d("Test", "261");
        }
    }

    @Override
    public void onStopPreview() {

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        /* 初始化源图，如果预览方向有旋转，就将图片转过来 */
        Bitmap srcBitmap = null;
        if(cameraManager.getDisplayOrientation() != 0){
            Bitmap sourceBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            srcBitmap = BitmapUtils.rotate(sourceBitmap, cameraManager.getDisplayOrientation());
            Log.d("Test", "283");
            sourceBitmap.recycle();
        }
        else{
            srcBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        /* 根据取景框对原图进行截取，只要取景框内的部分 */
        if(cameraApertureRect == null){
            Rect cameraApertureViewInSurfaceViewRect = new Rect();
            Log.d("Test", "293");
            getGenerator(mPager.getCurrentItem()).getCameraApertureView().getGlobalVisibleRect(cameraApertureViewInSurfaceViewRect);
            Log.d("Test", "295");
            Camera.Size pictureSize = cameraManager.getCamera().getParameters().getPictureSize();
            Log.d("Test", "297");
            cameraApertureRect = RectUtils.mappingRect(cameraApertureViewInSurfaceViewRect, new Point(surfaceView.getWidth(), surfaceView.getHeight()), new Point(pictureSize.width, pictureSize.height), getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
            Log.d("Test", "299");
        }
        Bitmap cutBitmap = Bitmap.createBitmap(srcBitmap, cameraApertureRect.left, cameraApertureRect.top, cameraApertureRect.width(), cameraApertureRect.height());
        srcBitmap.recycle();
        srcBitmap = cutBitmap;
        getGenerator(mPager.getCurrentItem()).onPictureToken(srcBitmap);
    }

    public void setReadTakePhotes(boolean needTake) {
        readTakePhotos = needTake;
    }
}
