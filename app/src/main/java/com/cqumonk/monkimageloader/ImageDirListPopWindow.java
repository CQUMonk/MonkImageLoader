package com.cqumonk.monkimageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.cqumonk.monkimageloader.bean.Dir;
import com.cqumonk.monkimageloader.adapter.ImageDirAdapter;

import java.util.List;

/**
 * Created by CQUMonk on 2015/9/25.
 */
public class ImageDirListPopWindow extends PopupWindow {

    private int mWidth;
    private int mHeight;

    private ListView mListView;
    private List<Dir> mDirList;

    private View mConvertView;


    public ImageDirListPopWindow(Context ctx,List<Dir> dirList){

        //计算popwindow尺寸
        getWidthAndHeight(ctx);
        mConvertView= LayoutInflater.from(ctx).inflate(R.layout.popwindow_listview,null);
        mDirList=dirList;
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        //点击外部可以消失
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction()==MotionEvent.ACTION_OUTSIDE){
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView(ctx);
        initEvent();
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectedListener!=null){
                    mSelectedListener.onSelected(mDirList.get(position));
                }
            }
        });
    }

    private void initView(Context context) {
        mListView= (ListView) mConvertView.findViewById(R.id.lv_popwindow_picdirs);
        mListView.setAdapter(new ImageDirAdapter(context,mDirList));
    }

    private void getWidthAndHeight(Context ctx) {
        DisplayMetrics metrics=new DisplayMetrics();
        WindowManager wm= (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        wm.getDefaultDisplay().getMetrics(metrics);
        mWidth=metrics.widthPixels;
        mHeight= (int) (metrics.heightPixels*0.7);
    }


    public interface onDirSelectedListener{
        void onSelected(Dir dir);
    }
    private onDirSelectedListener mSelectedListener;

    public void setSelectedListener(onDirSelectedListener mSelectedListener) {
        this.mSelectedListener = mSelectedListener;
    }

}
