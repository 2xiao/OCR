package com.imagerecognition;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.GridView;

import com.imagerecognition.PhotoAlbumImageAdapter.OnImageClickListener;

public class HistoryActivity extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final GridView gridView = new GridView(getBaseContext());
		gridView.setNumColumns(2);
		setContentView(gridView);
		
		new AsyncTask<Integer, Integer, List<String>>() {
			@Override
			protected List<String> doInBackground(Integer... params) {
				File dir = getExternalFilesDir(null); 
				if(dir != null && dir.exists()){
					File[] files = dir.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String filename) {
							return filename.endsWith(".jpeg");
						}
					});
					if(files != null && files.length > 0){
						List<String> paths = new ArrayList<String>();
						for(File file : files){
							paths.add(file.getPath());
						}
						return paths;
					}
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(final List<String> result) {
				super.onPostExecute(result);
				gridView.setAdapter(new PhotoAlbumImageAdapter(getBaseContext(), result, new OnImageClickListener() {
					@Override
					public void onImageClick(int position) {
						Intent intent = new Intent(getBaseContext(), DecodeActivity.class);
						intent.putExtra(DecodeActivity.PATH, result.get(position));
						startActivity(intent);
					}
				}, gridView));
			}
		}.execute(0);
	}
}
