package com.imagerecognition;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.xiaopan.easy.android.util.BitmapUtils;
import me.xiaopan.easy.android.util.FileUtils;
import me.xiaopan.easy.android.util.RectUtils;
import me.xiaopan.easy.android.util.ViewAnimationUtils;
import me.xiaopan.easy.android.util.camera.CameraManager;
import me.xiaopan.easy.android.util.camera.CameraManager.CamreaBeingUsedException;
import me.xiaopan.easy.android.util.camera.CameraOptimalSizeCalculator;
import me.xiaopan.easy.java.util.IOUtils;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * 拍名片
 */
public class TakeBusinessCardActivity extends Activity implements CameraManager.CameraCallback, Camera.PictureCallback{
	private boolean readTakePhotos;//准备拍照
	private View cameraApertureView;	//取景框视图
	private View shutterButton;	//快门按钮
	private View userButton;	//使用按钮
	private View remakeButton;	//重拍按钮
	private Button flashModeButton;	//闪光等控制按钮
	private ImageView previewImage;	//预览图
	private SurfaceView surfaceView;	//Surface视图
	private File localCacheFile;	//本地缓存文件
	private Rect cameraApertureRect;	//取景框的位置
	private List<String> supportedFlashModes;	//当前设备支持的闪光模式
	private CameraManager cameraManager;	//相机管理器
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_take_business_card);
		
		surfaceView = (SurfaceView) findViewById(R.id.surface_takeBusinessCard);
		surfaceView.getHolder().addCallback(new Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				Log.d("Test", "surfaceDestroyed");
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				Log.d("Test", "surfaceCreated");
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
					int height) {
				// TODO Auto-generated method stub
				Log.d("Test", "surfaceChanged");
			}
		});
		Log.d("Test", "66");
		cameraApertureView = findViewById(R.id.view_takeBusinessCard_cameraAperture);
		shutterButton = findViewById(R.id.button_takeBusinessCard_shutter);
		userButton = findViewById(R.id.button_takeBusinessCard_use);
		remakeButton = findViewById(R.id.button_takeBusinessCard_remake);
		flashModeButton = (Button) findViewById(R.id.button_takeBusinessCard_flashMode);
		previewImage = (ImageView) findViewById(R.id.image_takeBusinessCard_preview);
		
		onInitListener(savedInstanceState);
		onInitData(savedInstanceState);
	}
	
	public void onInitListener(Bundle savedInstanceState) {
		//点击显示界面的时候对焦
		surfaceView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				cameraManager.autoFocus();
				Log.d("Test", "85");
			}
		});
		Log.d("Test", "88");
		
		//按下拍摄按钮的时候会先对焦，对完焦再拍照
		shutterButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				readTakePhotos = true;
				cameraManager.autoFocus();
				Log.d("Test", "96");
			}
		});
		
		//点击闪光模式按钮，就按照支持的闪光模式依次更新
		flashModeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//新的闪光模式
				String newFlashMode = supportedFlashModes.get((supportedFlashModes.indexOf(flashModeButton.getTag()) + 1) % supportedFlashModes.size());
				setFlashModeImageButton(newFlashMode);
				cameraManager.setFlashMode(newFlashMode);
				Log.d("Test", "108");
			}
		});
		
		//按钮使用按钮后，先对裁剪后的图片进行缩小，然后输出到本地缓存文件中
		userButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(localCacheFile != null && localCacheFile.exists()){
					File file = FileUtils.getFileFromDynamicFilesDir(getBaseContext(), System.currentTimeMillis()+".jpeg");
					if(!file.exists()){
						try {
							file.getParentFile().mkdirs();
							file.createNewFile();
						} catch (IOException e) {
							e.printStackTrace();
							Toast.makeText(getBaseContext(), "创建图像文件失败，请检查您的SD卡是否可用", Toast.LENGTH_LONG).show();
							return;
						}
					}
					localCacheFile.renameTo(file);
					localCacheFile = null;
					//渐隐快门按钮并渐现使用、重拍按钮
					ViewAnimationUtils.visibleViewByAlpha(shutterButton);
					ViewAnimationUtils.invisibleViewByAlpha(userButton);
					ViewAnimationUtils.invisibleViewByAlpha(remakeButton, new AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {}
						@Override
						public void onAnimationRepeat(Animation animation) {}
						@Override
						public void onAnimationEnd(Animation animation) {
							previewImage.setImageDrawable(null);	//将预览视图清空
						}
					});
					
					Intent intent = new Intent(getBaseContext(), DecodeActivity.class);
					intent.putExtra(DecodeActivity.PATH, file.getPath());
					startActivity(intent);
				}else{
					Toast.makeText(getBaseContext(), "保存失败了", Toast.LENGTH_LONG).show();
				}
			}
		});
		
		//按钮重拍按钮后，先释放裁剪后的图片，然后隐藏使用、重拍按钮并显示快门按钮，最后在动画执行完毕之后将预览视图清空
		remakeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//渐隐快门按钮并渐现使用、重拍按钮
				ViewAnimationUtils.visibleViewByAlpha(shutterButton);
				ViewAnimationUtils.invisibleViewByAlpha(userButton);
				ViewAnimationUtils.invisibleViewByAlpha(remakeButton, new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {}
					@Override
					public void onAnimationRepeat(Animation animation) {}
					@Override
					public void onAnimationEnd(Animation animation) {
						previewImage.setImageDrawable(null);	//将预览视图清空
					}
				});
				if(localCacheFile != null && localCacheFile.exists()){
					localCacheFile.delete();
				}
			}
		});
		

		
		findViewById(R.id.button_takeBusinessCard_viewHistory).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getBaseContext(), HistoryActivity.class));
			}
		});
	}

	public void onInitData(Bundle savedInstanceState) {
		/* 初始化相机关机器以及按钮 */
		cameraManager = new CameraManager(this, surfaceView.getHolder(), this);
		Log.d("Test", "189");
		shutterButton.setVisibility(View.VISIBLE);
		userButton.setVisibility(View.INVISIBLE);
		remakeButton.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			cameraManager.openBackCamera(true);
			Log.d("Test", "200");
		} catch (CamreaBeingUsedException e) {
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "打开相机失败", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		cameraManager.release();
		Log.d("Test", "212");
	}

	@Override
	public void onDestroy() {
		cameraManager = null;
		Log.d("Test", "218");
		if(localCacheFile != null && localCacheFile.exists()){
			localCacheFile.delete();
		}
		super.onDestroy();
	}
	
	@Override
	public void onInitCamera(Camera camera) {
		Camera.Parameters cameraParameters = camera.getParameters();
		
		/* 设置闪光模式 */
		supportedFlashModes = new ArrayList<String>(3);
		supportedFlashModes.add(Camera.Parameters.FLASH_MODE_OFF);
		supportedFlashModes.add(Camera.Parameters.FLASH_MODE_ON);
		if(cameraParameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_AUTO)){
			cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			supportedFlashModes.add(Camera.Parameters.FLASH_MODE_AUTO);
			setFlashModeImageButton(Camera.Parameters.FLASH_MODE_AUTO);
		}else{
			cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			setFlashModeImageButton(Camera.Parameters.FLASH_MODE_OFF);
		}
		
		/* 设置预览和输出分辨率 */
		Size[] optimalSizes = new CameraOptimalSizeCalculator().getPreviewAndPictureSize(surfaceView.getWidth(), surfaceView.getHeight(), cameraParameters.getSupportedPreviewSizes(), cameraParameters.getSupportedPictureSizes());
		Log.d("Test", "244");
		cameraParameters.setPreviewSize(optimalSizes[0].width, optimalSizes[0].height);
		cameraParameters.setPictureSize(optimalSizes[1].width, optimalSizes[1].height);
		
		camera.setParameters(cameraParameters);
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
	public void onStartPreview() {
		cameraManager.autoFocus();
		Log.d("Test", "268");
	}

	@Override
	public void onStopPreview() {}
	
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		OutputStream fileOutputStream = null;
		try {
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
				cameraApertureView.getGlobalVisibleRect(cameraApertureViewInSurfaceViewRect);
				Log.d("Test", "295");
				Camera.Size pictureSize = cameraManager.getCamera().getParameters().getPictureSize();
				Log.d("Test", "297");
				cameraApertureRect = RectUtils.mappingRect(cameraApertureViewInSurfaceViewRect, new Point(surfaceView.getWidth(), surfaceView.getHeight()), new Point(pictureSize.width, pictureSize.height), getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
				Log.d("Test", "299");
			}
			Bitmap cutBitmap = Bitmap.createBitmap(srcBitmap, cameraApertureRect.left, cameraApertureRect.top, cameraApertureRect.width(), cameraApertureRect.height());
			srcBitmap.recycle();
			srcBitmap = cutBitmap;
			
			//将图片输出到本地缓存文件中
			localCacheFile = FileUtils.getFileFromDynamicFilesDir(getBaseContext(), "BusinessCardCache.jpeg");
			if(!localCacheFile.exists()){
				try {
					localCacheFile.getParentFile().mkdirs();
					localCacheFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(getBaseContext(), "创建图像文件失败，请检查您的SD卡是否可用", Toast.LENGTH_LONG).show();
					return;
				}
			}
			fileOutputStream = IOUtils.openOutputStream(localCacheFile, false);
			srcBitmap.compress(CompressFormat.JPEG, 100, fileOutputStream);
			fileOutputStream.flush();
			fileOutputStream.close();
			
			//显示到预览图上
			previewImage.setImageURI(Uri.fromFile(localCacheFile));
			
			//渐隐快门按钮并渐现使用、重拍按钮
			ViewAnimationUtils.invisibleViewByAlpha(shutterButton);
			ViewAnimationUtils.visibleViewByAlpha(userButton);
			ViewAnimationUtils.visibleViewByAlpha(remakeButton, new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					cameraManager.startPreview();
					Log.d("Test", "336");
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			if(fileOutputStream != null){
				try {
					fileOutputStream.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					fileOutputStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Toast.makeText(getBaseContext(), "拍摄失败，请重拍", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	/**
	 * 设置闪光模式切换按钮
	 * @param falshMode
	 */
	private void setFlashModeImageButton(String falshMode){
		if(Camera.Parameters.FLASH_MODE_AUTO.equals(falshMode)){
			flashModeButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_flash_auto), null, null, null);
			flashModeButton.setTag(Camera.Parameters.FLASH_MODE_AUTO);
			flashModeButton.setText("自动");
		}else if(Camera.Parameters.FLASH_MODE_OFF.equals(falshMode)){
			flashModeButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_flash_off), null, null, null);
			flashModeButton.setTag(Camera.Parameters.FLASH_MODE_OFF);
			flashModeButton.setText("关闭");
		}else if(Camera.Parameters.FLASH_MODE_ON.equals(falshMode)){
			flashModeButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_flash_on), null, null, null);
			flashModeButton.setTag(Camera.Parameters.FLASH_MODE_ON);
			flashModeButton.setText("打开");
		}
	}
}