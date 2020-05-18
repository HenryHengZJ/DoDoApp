package com.dodomaker.app.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.dodomaker.app.CustomObjectClass.Sticker;
import com.dodomaker.app.R;

import java.util.List;

public class StickerAdapter extends BaseAdapter {

    private Context context;
    private List<Sticker> list;

    private static final String TAG = "StickerAdapter";

    public StickerAdapter(Context context, List<Sticker> list){
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount(){
        // Count the items
        return list == null ? 0 : list.size();
    }
    @Override
    public Object getItem(int position)
    {
        return position;
    }
    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View gridView;
        if (convertView == null)
        {
            gridView = new View(context);
            // get layout from mobile.xml
            gridView = inflater.inflate(R.layout.sticker_row, null);

            ImageView mimageView = (ImageView) gridView.findViewById(R.id.imageView);
            RelativeLayout mRlayout = (RelativeLayout) gridView.findViewById(R.id.Rlayout);

            Sticker mylist = list.get(position);
            final int stickerimage = mylist.getimage();

            Glide.with(context).load(stickerimage)
                    .thumbnail(0.5f)
                    .centerInside()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(mimageView);


        }
        else
        {
            gridView = (View) convertView;
        }
        return gridView;
    }




}
