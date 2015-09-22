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
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private RelativeLayout mChooseDir;
    private TextView mDirName;
    private TextView mDirCnt;

    private List<Dir> dirList;
    private List<String> picPathList;

    private ProgressDialog mProgressDialog;

    private File mCurrentDir;
    private int mMaxCount;

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

        dirList=new ArrayList<>();
        picPathList=new ArrayList<>();
        mMaxCount=0;
        myHandler=new MyHandler(this);

        //使用contentProvider扫描手机中的图片

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "内存卡未加载", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在加载中...");
        //开启异步线程进行文件遍历工作
        new Thread() {
            @Override
            public void run() {


                Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(imageUri, null,
                        MediaStore.Images.Media.MIME_TYPE + "= ? or" + MediaStore.Images.Media.MIME_TYPE + "= ?",
                        new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> picDirs=new HashSet<String>();

                while (cursor.moveToNext()){
                    //当前图片路径
                    String picPath=getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
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
        }.start();


    }

    private void initEvent() {

    }
    private void bindDataToGridView() {
        if (mCurrentDir==null){
            Toast.makeText(this,"未扫描到任何图片..",Toast.LENGTH_SHORT).show();
            return;
        }
        picPathList= Arrays.asList(mCurrentDir.list());
    }

    


}
