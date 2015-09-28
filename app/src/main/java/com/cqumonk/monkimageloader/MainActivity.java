package com.cqumonk.monkimageloader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cqumonk.monkimageloader.adapter.ImagesAdapter;
import com.cqumonk.monkimageloader.bean.Dir;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends Activity {
    private GridView mGridView;
    private ImagesAdapter mImagesAdapter;
    private List<String> picNameList;

    private RelativeLayout mChooseDir;
    private TextView mDirName;
    private TextView mDirCnt;

    private List<Dir> dirList;


    private ProgressDialog mProgressDialog;

    //当前目录
    private File mCurrentDir;
    //最大图片数目
    private int mMaxCount;

    private ImageDirListPopWindow mPopupWindow;

    private static final int DATA_LOADED=0X321;

    static class MyHandler extends Handler{
        WeakReference<MainActivity> activityWeakReference=null;
        MyHandler(MainActivity activity){

            activityWeakReference=new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {


            if (msg.what==DATA_LOADED){
                MainActivity mainActivity=activityWeakReference.get();
                if (mainActivity!=null){
                    mainActivity.mProgressDialog.dismiss();
                    //绑定数据到GridView中
                    mainActivity.bindDataToGridView();
                }

            }
            if (msg.what==0x222){
                Toast.makeText(activityWeakReference.get(),"cursor is null",Toast.LENGTH_LONG).show();
            }
            if (msg.what==0x999){
                int cnt=msg.arg1;
                Toast.makeText(activityWeakReference.get(),"cursor cnt is"+cnt,Toast.LENGTH_LONG).show();
            }

        }


    }

    private MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initDatas();
        initEvent();
    }

    private void initViews() {
        mGridView = (GridView) findViewById(R.id.gv_main_pics);
        mChooseDir = (RelativeLayout) findViewById(R.id.rl_main_choose);
        mDirName = (TextView) findViewById(R.id.tv_main_dirname);
        mDirCnt = (TextView) findViewById(R.id.tv_main_dircnt);

    }

    private void initDatas() {

        dirList=new ArrayList<Dir>();
        picNameList =new ArrayList<String>();
        mMaxCount=0;
        myHandler=new MyHandler(this);

        //使用contentProvider扫描手机中的图片
        //Toast.makeText(this,Environment.getExternalStorageState(),Toast.LENGTH_SHORT).show();

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "内存卡未加载", Toast.LENGTH_SHORT).show();
            //Toast.makeText(this,Environment.getExternalStorageState(),Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在加载中...");



        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri= MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                ContentResolver cr = MainActivity.this.getContentResolver();

                Cursor cursor=cr.query(uri,null,
                        MediaStore.Images.Media.MIME_TYPE + "=? or "+ MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[] { "image/jpeg", "image/png" },
                        MediaStore.Images.Media.DATE_MODIFIED);
                if (cursor==null){
                    Log.e("TAG", "cursor is null" + "");
                    myHandler.sendEmptyMessage(0x222);
                }else {
                    Message msg=Message.obtain();
                    msg.what=0x999;
                    msg.arg1=cursor.getCount();
                    myHandler.sendMessage(msg);
                }
                Set<String> picDirs=new HashSet<String>();

                while (cursor.moveToNext()){
                    //当前图片路径
                    String picPath=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //根据图片路径找到其父目录的路径
                    File parentFile=new File(picPath).getParentFile();
                    if (parentFile==null){
                        continue;
                    }
                    //得到父目录的绝对路径，遍历其下图片，使用set存储父目录路径防止重复遍历
                    String absulotePath=parentFile.getAbsolutePath();
                    Dir dir=null;
                    if (picDirs.contains(absulotePath)){
                        continue;
                    }else {
                        //如果该父目录未被遍历,则创建
                        picDirs.add(absulotePath);
                        dir=new Dir();
                        dir.setDirPath(absulotePath);
                        dir.setFirstPicPath(picPath);

                    }

                    if (parentFile.list()==null)
                        continue;

                    //统计当前目录图片数目
                    int picCnt=parentFile.list(new FilenameFilter(){
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".jpg")||filename.endsWith(".jpeg")||filename.endsWith("png")){
                                return true;
                            }
                            return false;
                        }
                    }).length;
                    dir.setPicCount(picCnt);

                    dirList.add(dir);

                    if (picCnt>mMaxCount){
                        mMaxCount=picCnt;
                        mCurrentDir=parentFile;

                    }


                }

                cursor.close();
                myHandler.sendEmptyMessage(DATA_LOADED);
            }


        }).start();

    }

    private void initEvent() {
        mChooseDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //显示popWindow
                mPopupWindow.setAnimationStyle(R.style.dir_popupwindow_animation);
                mPopupWindow.showAsDropDown(mChooseDir,0,0);

                //周围变暗效果
                WindowManager.LayoutParams layoutParams=getWindow().getAttributes();
                layoutParams.alpha=0.3f;
                getWindow().setAttributes(layoutParams);
            }
        });

    }
    private void bindDataToGridView() {
        if (mCurrentDir==null){
            Toast.makeText(this,"未扫描到任何图片..",Toast.LENGTH_SHORT).show();
            return;
        }
        //当前文件夹中图片的文件名列表
        picNameList = Arrays.asList(mCurrentDir.list());

        //为gridview设置数据适配器
        mImagesAdapter=new ImagesAdapter(this,picNameList,mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImagesAdapter);

        //为textView设置文字
        mDirName.setText(mCurrentDir.getName());
        mDirCnt.setText(mMaxCount+"");

        //数据加载完毕后，初始化popupwindow功能
        initPopWindow();
    }

    private void initPopWindow() {

        mPopupWindow=new ImageDirListPopWindow(this,dirList);
        //popWindow消失的效果
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams layoutParams=getWindow().getAttributes();
                layoutParams.alpha=1.0f;
                getWindow().setAttributes(layoutParams);

            }
        });

        mPopupWindow.setSelectedListener(new ImageDirListPopWindow.onDirSelectedListener() {
            @Override
            public void onSelected(Dir dir) {
                mCurrentDir=new File(dir.getDirPath());
                picNameList=Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpg")||filename.endsWith(".jpeg")||filename.endsWith(".png"))
                            return true;

                        return false;
                    }
                }));
                mImagesAdapter=new ImagesAdapter(MainActivity.this,picNameList,dir.getDirPath());
                mGridView.setAdapter(mImagesAdapter);

                mDirCnt.setText(dir.getPicCount()+"");
                mDirName.setText(dir.getDirName());
                mPopupWindow.dismiss();


            }
        });


    }



}
