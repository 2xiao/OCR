package com.imagerecognition;

import java.util.List;

import me.xiaopan.spear.SpearImageView;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class PhotoAlbumImageAdapter extends BaseAdapter {
    private Context context;
    private List<String> imageUris;
    private int itemWidth = -1;
    private View.OnClickListener itemClickListener;
    private int spanCount = -1;
    private int borderMargin;
    private int middleMargin;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public PhotoAlbumImageAdapter(final Context context, final List<String> imageUris, final OnImageClickListener onImageClickListener, GridView gridView){
        this.context = context;
        this.imageUris = imageUris;
        this.itemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onImageClickListener != null && v.getTag() != null && v.getTag() instanceof ItemViewHolder){
                    onImageClickListener.onImageClick(((ItemViewHolder) v.getTag()).position);
                }
            }
        };
        this.spanCount = gridView.getNumColumns();

        borderMargin = (int) ((context.getResources().getDisplayMetrics().density * 8) + 0.5);
        middleMargin = (int) ((context.getResources().getDisplayMetrics().density * 4) + 0.5);
        int maxScreenWidth = context.getResources().getDisplayMetrics().widthPixels - ((borderMargin * (spanCount+1)));
        itemWidth = maxScreenWidth/spanCount;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return imageUris!=null?imageUris.size():0;
    }

    public List<String> getImageUrlList() {
        return imageUris;
    }

    public interface OnImageClickListener{
        void onImageClick(int position);
    }

	@Override
	public Object getItem(int position) {
		return imageUris.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			convertView = LayoutInflater.from(context).inflate(R.layout.list_item_photo_album_image, parent, false);
			ItemViewHolder itemViewHolder = new ItemViewHolder(convertView);
			itemViewHolder.spearImageView.setOnClickListener(itemClickListener);
	        if(itemWidth != -1){
	            ViewGroup.LayoutParams layoutParams = itemViewHolder.spearImageView.getLayoutParams();
	            layoutParams.width = itemWidth;
	            layoutParams.height = itemWidth;
	            itemViewHolder.spearImageView.setLayoutParams(layoutParams);
	        }
			convertView.setTag(itemViewHolder);
		}
		
		ItemViewHolder itemViewHolder = (ItemViewHolder) convertView.getTag();
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) itemViewHolder.spearImageView.getLayoutParams();
            int remainder = position % spanCount;
            if(remainder == 0){
                marginLayoutParams.leftMargin = borderMargin;
                marginLayoutParams.rightMargin = middleMargin;
            }else if(remainder == spanCount-1){
                marginLayoutParams.leftMargin = middleMargin;
                marginLayoutParams.rightMargin = borderMargin;
            }else{
                marginLayoutParams.leftMargin = middleMargin;
                marginLayoutParams.rightMargin = middleMargin;
            }

            if(position < spanCount){
                marginLayoutParams.topMargin = borderMargin;
                marginLayoutParams.bottomMargin = middleMargin;
            }else if(position >= getCount() - 1 - (remainder == 0?spanCount:remainder)){
                marginLayoutParams.topMargin = middleMargin;
                marginLayoutParams.bottomMargin = borderMargin;
            }else{
                marginLayoutParams.topMargin = middleMargin;
                marginLayoutParams.bottomMargin = middleMargin;
            }
            itemViewHolder.spearImageView.setLayoutParams(marginLayoutParams);

        itemViewHolder.spearImageView.displayImage(imageUris.get(position));
        itemViewHolder.spearImageView.setTag(itemViewHolder);
        itemViewHolder.position = position;
		return convertView;
	}

    private static class ItemViewHolder{
        private SpearImageView spearImageView;
        private int position;
        public ItemViewHolder(View itemView) {
            this.spearImageView = (SpearImageView) itemView.findViewById(R.id.image_photoAlbumImageItem_one);
        }
    }
}
