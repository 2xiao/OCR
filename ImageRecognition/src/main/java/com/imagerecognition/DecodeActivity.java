package com.imagerecognition;

import java.io.File;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DecodeActivity extends Activity{
	public static final String PATH = "path";
	private TessOCR mTessOCR;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_decode);
		
		ImageView imageView = (ImageView) findViewById(R.id.image_decode_image);
		final TextView resultTextView = (TextView) findViewById(R.id.text_decode_result);
		final View progressBar = findViewById(R.id.layout_decode_progresds);
		
		final String path = getIntent().getStringExtra(PATH);
		
		imageView.setImageURI(Uri.fromFile(new File(path)));

		mTessOCR = new TessOCR();
		new AsyncTask<Integer, Integer, String>() {
			@Override
			protected String doInBackground(Integer... params) {
				Bitmap bitmap = BitmapFactory.decodeFile(path);
				final String result = mTessOCR.getOCRResult(bitmap);
				if(bitmap != null){
					bitmap.recycle();
				}
				return result;
			}
			
			protected void onPostExecute(String result) {
				progressBar.setVisibility(View.GONE);
				resultTextView.setText(result);
			};
		}.execute(0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTessOCR.onDestroy();
	}
}
