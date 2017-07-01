package com.imagerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import me.xiaopan.easy.android.util.FileUtils;
import me.xiaopan.easy.android.util.ViewAnimationUtils;
import me.xiaopan.easy.java.util.IOUtils;

/**
 * Created by X on 15/5/11.
 */
public class ScanViewGenerator extends ContentGenerator{

    private View cameraApertureView;	//取景框视图
    private View shutterButton;	//快门按钮
    private View userButton;	//使用按钮
    private View remakeButton;	//重拍按钮
    private Button flashModeButton;	//闪光等控制按钮
    private ImageView previewImage;	//预览图
    private File localCacheFile;	//本地缓存文件

    public ScanViewGenerator(MainActivity activity) {
        super(activity);
    }

    @Override
    public View generate() {
        View view = View.inflate(getActivity(), R.layout.item_scan, null);
        Log.d("Test", "60");
        cameraApertureView = view.findViewById(R.id.view_takeBusinessCard_cameraAperture);
        shutterButton = view.findViewById(R.id.button_takeBusinessCard_shutter);
        userButton = view.findViewById(R.id.button_takeBusinessCard_use);
        remakeButton = view.findViewById(R.id.button_takeBusinessCard_remake);
        flashModeButton = (Button) view.findViewById(R.id.button_takeBusinessCard_flashMode);
        previewImage = (ImageView) view.findViewById(R.id.image_takeBusinessCard_preview);
        view.findViewById(R.id.button_takeBusinessCard_viewHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startActivity(new Intent(getActivity(), HistoryActivity.class));
            }
        });
        onInitListener();
        onInitData();
        return view;
    }

    @Override
    public View getCameraApertureView() {
        return cameraApertureView;
    }

    @Override
    public void onPictureToken(Bitmap srcBitmap) {
        OutputStream fileOutputStream = null;
        try {
            //将图片输出到本地缓存文件中
            localCacheFile = FileUtils.getFileFromDynamicFilesDir(getActivity(), "BusinessCardCache.jpeg");
            if(!localCacheFile.exists()){
                try {
                    localCacheFile.getParentFile().mkdirs();
                    localCacheFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "创建图像文件失败，请检查您的SD卡是否可用", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            fileOutputStream = IOUtils.openOutputStream(localCacheFile, false);
            srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            //显示到预览图上
            previewImage.setImageURI(Uri.fromFile(localCacheFile));

            //渐隐快门按钮并渐现使用、重拍按钮
            ViewAnimationUtils.invisibleViewByAlpha(shutterButton);
            ViewAnimationUtils.visibleViewByAlpha(userButton);
            ViewAnimationUtils.visibleViewByAlpha(remakeButton, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    getActivity().getCameraManager().startPreview();
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
                Toast.makeText(getActivity(), "拍摄失败，请重拍", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onInitListener() {
        Log.d("Test", "87");

        //按下拍摄按钮的时候会先对焦，对完焦再拍照
        shutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().setReadTakePhotes(true);
                getActivity().getCameraManager().autoFocus();
                Log.d("Test", "95");
            }
        });

        //点击闪光模式按钮，就按照支持的闪光模式依次更新
        flashModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //新的闪光模式
//                String newFlashMode = supportedFlashModes.get((supportedFlashModes.indexOf(flashModeButton.getTag()) + 1) % supportedFlashModes.size());
//                setFlashModeImageButton(newFlashMode);
//                cameraManager.setFlashMode(newFlashMode);
                Log.d("Test", "107");
            }
        });

        //按钮使用按钮后，先对裁剪后的图片进行缩小，然后输出到本地缓存文件中
        userButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (localCacheFile != null && localCacheFile.exists()) {
                    File file = FileUtils.getFileFromDynamicFilesDir(getActivity(), System.currentTimeMillis() + ".jpeg");
                    if (!file.exists()) {
                        try {
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "创建图像文件失败，请检查您的SD卡是否可用", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    localCacheFile.renameTo(file);
                    localCacheFile = null;
                    //渐隐快门按钮并渐现使用、重拍按钮
                    ViewAnimationUtils.visibleViewByAlpha(shutterButton);
                    ViewAnimationUtils.invisibleViewByAlpha(userButton);
                    ViewAnimationUtils.invisibleViewByAlpha(remakeButton, new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            previewImage.setImageDrawable(null);    //将预览视图清空
                        }
                    });

                    Intent intent = new Intent(getActivity(), DecodeActivity.class);
                    intent.putExtra(DecodeActivity.PATH, file.getPath());
                    getActivity().startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "保存失败了", Toast.LENGTH_LONG).show();
                }
            }
        });

        //按钮重拍按钮后，先释放裁剪后的图片，然后隐藏使用、重拍按钮并显示快门按钮，最后在动画执行完毕之后将预览视图清空
        remakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //渐隐快门按钮并渐现使用、重拍按钮
                ViewAnimationUtils.visibleViewByAlpha(shutterButton);
                ViewAnimationUtils.invisibleViewByAlpha(userButton);
                ViewAnimationUtils.invisibleViewByAlpha(remakeButton, new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        previewImage.setImageDrawable(null);    //将预览视图清空
                    }
                });
                if (localCacheFile != null && localCacheFile.exists()) {
                    localCacheFile.delete();
                }
            }
        });
    }

    public void onInitData() {
        Log.d("Test", "187");
        shutterButton.setVisibility(View.VISIBLE);
        userButton.setVisibility(View.INVISIBLE);
        remakeButton.setVisibility(View.INVISIBLE);
    }

    /**
     * 设置闪光模式切换按钮
     * @param falshMode
     */
    private void setFlashModeImageButton(String falshMode){
        if(Camera.Parameters.FLASH_MODE_AUTO.equals(falshMode)){
            flashModeButton.setCompoundDrawablesWithIntrinsicBounds(getActivity().getResources().getDrawable(R.drawable.ic_flash_auto), null, null, null);
            flashModeButton.setTag(Camera.Parameters.FLASH_MODE_AUTO);
            flashModeButton.setText("自动");
        }else if(Camera.Parameters.FLASH_MODE_OFF.equals(falshMode)){
            flashModeButton.setCompoundDrawablesWithIntrinsicBounds(getActivity().getResources().getDrawable(R.drawable.ic_flash_off), null, null, null);
            flashModeButton.setTag(Camera.Parameters.FLASH_MODE_OFF);
            flashModeButton.setText("关闭");
        }else if(Camera.Parameters.FLASH_MODE_ON.equals(falshMode)){
            flashModeButton.setCompoundDrawablesWithIntrinsicBounds(getActivity().getResources().getDrawable(R.drawable.ic_flash_on), null, null, null);
            flashModeButton.setTag(Camera.Parameters.FLASH_MODE_ON);
            flashModeButton.setText("打开");
        }
    }
}
