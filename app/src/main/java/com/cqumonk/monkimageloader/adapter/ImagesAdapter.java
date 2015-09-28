package com.cqumonk.monkimageloader.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.cqumonk.monkimageloader.R;
import com.cqumonk.monkimageloader.bean.Dir;
import com.cqumonk.monkimageloader.util.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by CQUMonk on 2015/9/25.
 */
public class ImagesAdapter extends BaseAdapter {
    /**
     * 为了节省内存，只存储文件名称的列表
     * @param ctx
     * @param picNameList 图片文件名列表
     * @param parentPath 父目录绝对路径
     */

    private List<String> mPics;
    private String mParentPath;
    private LayoutInflater mInflater;

    static Set<String> mSelectedPics=new HashSet<String>();

    public ImagesAdapter(Context ctx, List<String> picNameList, String parentPath){
        mPics=picNameList;
        mParentPath=parentPath;
        mInflater=LayoutInflater.from(ctx);
    }

    @Override
    public int getCount() {
        if (mPics!=null){
            return mPics.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mPics!=null){
            return mPics.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView==null){
            holder=new ViewHolder();
            convertView=mInflater.inflate(R.layout.gridview_item,parent,false);
            holder.imageButton= (ImageButton) convertView.findViewById(R.id.ib_gridItem_select);
            holder.imageView= (ImageView) convertView.findViewById(R.id.iv_gridItem_pic);
            convertView.setTag(holder);

        }else {
            holder= (ViewHolder) convertView.getTag();
        }

        //重置状态
        holder.imageView.setImageResource(R.mipmap.pictures_no);
        holder.imageView.setColorFilter(null);
        holder.imageButton.setImageResource(R.mipmap.picture_unselected);

        //根据路径加载图片
        final String picPath=mParentPath + '/' + mPics.get(position);
        ImageLoader.getImageLoader(3, ImageLoader.Type.LIFO)
                .loadImage(picPath, holder.imageView);

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //当点击图片时
                //如果已经选中,清除滤镜效果，从集合中删除,去掉选中标识
                if (mSelectedPics.contains(picPath)){
                    holder.imageView.setColorFilter(null);
                    mSelectedPics.remove(picPath);
                    holder.imageButton.setImageResource(R.mipmap.picture_unselected);

                }else {
                    holder.imageView.setColorFilter(Color.parseColor("#77000000"));
                    mSelectedPics.add(picPath);
                    holder.imageButton.setImageResource(R.mipmap.pictures_selected);
                }

            }
        });

        return convertView;
    }


    private class ViewHolder{
        ImageView imageView;
        ImageButton imageButton;
    }


}
