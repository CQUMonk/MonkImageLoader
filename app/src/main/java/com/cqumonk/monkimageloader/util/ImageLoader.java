package com.cqumonk.monkimageloader.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cqumonk.monkimageloader.bean.ImageHolder;
import com.cqumonk.monkimageloader.bean.ImageSize;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by CQUMonk on 2015/9/12.
 * 图片加载类，维护了一份缓存用于存储图片，防止内存溢出
 * 单例
 */
public class ImageLoader {
    //图片缓存
    private LruCache<String,Bitmap> mCache;
    //线程池
    private ExecutorService mThreadPool;
    //线程池大小
    private static final int DEFAULT_THREADPOOL_SIZE=1;
    //Task队列调度方式
    public enum Type{
        FIFO,LIFO;
    }
    private Type mType=Type.LIFO;

    //Task队列
    private LinkedList<Runnable> mTaskQueue;

    //后台线程，用于轮询任务队列
    private Thread mLoopThread;
    private Handler mLoopThreadHandler;//负责发送消息到后台线程，让后台线程取出任务队列中的任务给线程池执行

    //给UI线程发送消息的handler
    static class UIHandler extends Handler{
        WeakReference<ImageLoader> reference=null;
        UIHandler(ImageLoader imageLoader){
            reference=new WeakReference<ImageLoader>(imageLoader);
        }
    };
    private UIHandler mUIHandler;




    private ImageLoader(int threadPoolSize,Type type){
        init(threadPoolSize,type);
    }

    private void init(int threadPoolSize,Type type) {

        //开启轮询线程对任务队列进行轮询
        mLoopThread=new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                //初始化handler，如果轮询时接收到任务，则在此交给线程池处理
                mLoopThreadHandler=new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //给线程池处理

                        mThreadPool.submit(getTaskFromQueue());
                    }
                };

                Looper.loop();
            }
        };

        mLoopThread.start();

        //初始化LRU缓存
        //获取应用最大可用内存
        int maxMemory= (int) Runtime.getRuntime().maxMemory();

        mCache=new LruCache<String,Bitmap>(maxMemory/8){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //返回图片大小
                return value.getRowBytes()*value.getHeight();
            }
        };

        //初始化线程池和任务队列
        mThreadPool= Executors.newFixedThreadPool(threadPoolSize);
        mTaskQueue=new LinkedList<Runnable>();
        mType=type;

    }

    private static ImageLoader _singleIstance=null;
    public static ImageLoader getImageLoader(){
        if (_singleIstance==null){//为了提高效率
            synchronized (ImageLoader.class){
                if (_singleIstance==null){
                    _singleIstance=new ImageLoader(DEFAULT_THREADPOOL_SIZE,Type.LIFO);
                }
            }
        }
        return _singleIstance;
    }


    public void loadImage(String path,  final ImageView imageView){

        imageView.setTag(path);
        if (mUIHandler==null){
            mUIHandler=new UIHandler(_singleIstance){

                @Override
                public void handleMessage(Message msg) {
                    //获取到图片，并在imageview 中设置
                    ImageHolder holder= (ImageHolder) msg.obj;

                    if (holder.imageView.getTag().toString().equals(holder.path)){
                        holder.imageView.setImageBitmap(holder.bitmap);

                    }



                }
            };
        }
        //从cache中取到图片，将其发送给handler处理
        Bitmap bm=getBitmapFromLRUcache(path);
        if (bm!=null){
            Message msg=Message.obtain();
            ImageHolder imgHolder=new ImageHolder();
            imgHolder.path=path;
            imgHolder.bitmap=bm;
            imgHolder.imageView=imageView;

            msg.obj=imgHolder;
            mUIHandler.sendMessage(msg);
        }else {//在cache里面找不到图片,则创建一个task放入队列并通知

            addTask(new Runnable(){
                @Override
                public void run() {
                    //根据路径加载图片，需要对图片进行压缩

                    ImageSize imageSize=getImageViewSize(imageView);

                }
            });

        }
    }

    /**
     * 根据imageView获取适当的压缩尺寸
     * @param imageView
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private ImageSize getImageViewSize(ImageView imageView) {

        ViewGroup.LayoutParams lp=imageView.getLayoutParams();

        DisplayMetrics displayMetrics=imageView.getContext().getResources().getDisplayMetrics();

        //获取imageView实际尺寸以便压缩图片
        int width=imageView.getWidth();
        int height=imageView.getHeight();
        //如果未获取到，则获取它在viewGroup中声明的尺寸
        if(width<=0){
            width=lp.width;
        }
        if (height<=0){
            height=lp.height;
        }
        //如果imageView设置的是WAPcontent-1或者matchparent-2
        if(width<=0){
            width=imageView.getMaxWidth();
        }
        if (height<=0){
            height=imageView.getMaxHeight();//api16以后才有的
        }

        //如果扔获取不到，则令imageView的尺寸等于屏幕的尺寸
        if(width<=0){
            width=displayMetrics.widthPixels;
        }
        if (height<=0){
            height=displayMetrics.heightPixels;
        }

        ImageSize imageSize=new ImageSize();
        imageSize.width=width;
        imageSize.height=height;
        return imageSize;
    }

    private void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        mLoopThreadHandler.sendEmptyMessage(0x123);

    }

    private Bitmap getBitmapFromLRUcache(String path) {

        return mCache.get(path);
    }
    private Runnable getTaskFromQueue(){
        if (mType==Type.FIFO){
            return mTaskQueue.removeFirst();
        }else {
            return mTaskQueue.removeLast();
        }
    }


}

