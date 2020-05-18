package com.dodomaker.app.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.dodomaker.app.CustomObjectClass.Template;
import com.dodomaker.app.R;

import java.util.List;


public class TemplateAdapter extends RecyclerView.Adapter <TemplateAdapter.ViewHolder> {

    private List<Template> list;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context context;
    private static final String TAG = "ADAPTER";

    public TemplateAdapter(List<Template> list, Context context){
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = mInflater.inflate(R.layout.template_row, parent, false);
        return new ViewHolder(view);

        /*View view = LayoutInflater.from(context).inflate(R.layout.job_row, parent, false);
        MyHolder myHolder = new MyHolder(view);

        // Get the TextView reference from RecyclerView current item
        return myHolder;*/
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        Template mylist = list.get(position);
        final String title = mylist.gettitle();
        final String image = mylist.getimage();

        holder.temp_title.setText("#" + title);

        Glide.with(context).load(image)
                .thumbnail(0.5f)
                .centerCrop()
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.temp_image);
    }

    @Override
    public int getItemCount(){
        // Count the items
        return list.size();
    }


    // "Loading item" ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView temp_title;
        CardView mcardview;
        ImageView temp_image;


        public ViewHolder(View view) {
            super(view);
            temp_image = (ImageView) view.findViewById(R.id.imageView);
            mcardview = (CardView) view.findViewById(R.id.cardview);
            temp_title = (TextView)view.findViewById(R.id.title);
            mcardview.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onTemplateClick(view, getAdapterPosition());
        }
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onTemplateClick(View view, int position);
    }


}




