package com.cqumonk.monkimageloader.adapter;

/**
 * Created by CQUMonk on 2015/9/25.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqumonk.monkimageloader.R;
import com.cqumonk.monkimageloader.bean.Dir;
import com.cqumonk.monkimageloader.util.ImageLoader;

import java.util.List;

public class ImageDirAdapter extends ArrayAdapter<Dir> {
    private LayoutInflater mInflater;


    public ImageDirAdapter(Context context, List<Dir> objects) {
        super(context, 0, objects);
        mInflater=LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder=null;
        if (convertView==null){
            convertView=mInflater.inflate(R.layout.popwindow_item,parent,false);

            holder=new ViewHolder();
            holder.dirname= (TextView) convertView.findViewById(R.id.tv_popitem_dirname);
            holder.piccnt= (TextView) convertView.findViewById(R.id.tv_popitem_piccnt);
            holder.image= (ImageView) convertView.findViewById(R.id.iv_popitem_image);
            holder.selected= (ImageView) convertView.findViewById(R.id.iv_popitem_selected);

            convertView.setTag(holder);

        }else {
            holder= (ViewHolder) convertView.getTag();
        }
        Dir dir=getItem(position);
        holder.image.setImageResource(R.mipmap.pictures_no);

        ImageLoader.getImageLoader(3, ImageLoader.Type.LIFO).loadImage(dir.getFirstPicPath(),holder.image);
        holder.dirname.setText(dir.getDirName());
        holder.piccnt.setText(dir.getPicCount()+"张");


        return super.getView(position, convertView, parent);
    }

    class ViewHolder{
        ImageView image;
        ImageView selected;
        TextView dirname;
        TextView piccnt;

    }
}