package com.cqumonk.monkimageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cqumonk.monkimageloader.bean.ImageHolder;
import com.cqumonk.monkimageloader.bean.ImageSize;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by CQUMonk on 2015/9/12.
 * 图片加载类，维护了一份缓存用于存储图片，防止内存溢出
 * 使用线程池来管理线程，异步完成本地图片的压缩和加载
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

    //同步后台线程handler初始化的信号量
    private Semaphore mSemaphore_LoopThreadHandler=new Semaphore(0);

    //用于同步后台线程与任务队列的信号量，把任务都存储在任务队列中，而非线程池中的队列中
    private Semaphore mSemaphore_ThreadPool;



    //给UI线程发送消息的handler
    static class UIHandler extends Handler{
        WeakReference<ImageLoader> reference=null;
        UIHandler(ImageLoader imageLoader){
            reference=new WeakReference<ImageLoader>(imageLoader);
        }
        @Override
        public void handleMessage(Message msg) {
            //获取到图片，并在imageview 中设置
            ImageHolder holder= (ImageHolder) msg.obj;

            if (holder.imageView.getTag().toString().equals(holder.path)){
                holder.imageView.setImageBitmap(holder.bitmap);
            }
        }
    };
    private UIHandler mUIHandler;




    private ImageLoader(int threadPoolSize,Type type){
        init(threadPoolSize,type);
    }

    private void init(int threadPoolSize,Type type) {

        //创建后台轮询线程并开启，轮询线程负责对任务队列进行轮询，取出任务后交给线程池处理
        mLoopThread=new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                //初始化handler
                mLoopThreadHandler=new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //如果轮询时接收到任务，则从队列中取出任务交给线程池处理

                        mThreadPool.submit(getTaskFromQueue());
                        //每次从队列中取出一个任务后都请求一个信号量
                        try {
                            mSemaphore_ThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //初始化成功后，唤醒UI线程
                mSemaphore_LoopThreadHandler.release();

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
        //根据指定的线程数来初始化后台线程池的信号量，该信号量用于同步任务队列
        mSemaphore_ThreadPool=new Semaphore(threadPoolSize);


    }

    private static ImageLoader _singleIstance=null;
    public static ImageLoader getImageLoader(int threadCounts,Type type){
        if (_singleIstance==null){//为了提高效率
            synchronized (ImageLoader.class){
                if (_singleIstance==null){
                    _singleIstance=new ImageLoader(threadCounts,type);
                }
            }
        }
        return _singleIstance;
    }


    public void loadImage(final String path,  final ImageView imageView){

        imageView.setTag(path);
        if (mUIHandler==null){
            mUIHandler=new UIHandler(_singleIstance);
        }
        //从cache中取到图片，将其发送给handler处理
        Bitmap bm=getBitmapFromLRUcache(path);


        if (bm!=null){
            refreshImageView(path, bm, imageView);
        }else {
        //如果在cache里面找不到图片,则创建一个task放入队列并通知
            addTask(new Runnable(){
                @Override
                public void run() {
                    //根据路径加载图片，需要对图片进行压缩

                    ImageSize imageSize=getImageViewSize(imageView);
                    Bitmap bitmap=decodeImageByPath( path, imageSize.width, imageSize.height);

                    //将图片加入到缓存中
                    addBitmapToCache(path,bitmap);
                    //通知UI线程加载该图片
                    refreshImageView(path, bitmap, imageView);


                    //任务完成，释放信号量，通知后台线程可以去队列中取任务了
                    mSemaphore_ThreadPool.release();

                }
            });

        }
    }

    /**
     * 将图片，路径以及imageView打包发给UI线程，通知UI线程修改imageView中的图片
     * @param path
     * @param bitmap
     * @param imageView
     */
    private void refreshImageView(String path, Bitmap bitmap, ImageView imageView) {
        Message msg=Message.obtain();
        ImageHolder imgHolder=new ImageHolder();
        imgHolder.path=path;
        imgHolder.bitmap=bitmap;
        imgHolder.imageView=imageView;

        msg.obj=imgHolder;
        mUIHandler.sendMessage(msg);
    }

    /**
     * 将压缩后的图片加入到缓存中
     * @param path
     * @param bm
     */
    private void addBitmapToCache(String path,Bitmap bm) {

        if (getBitmapFromLRUcache(path)==null){
            if (bm!=null){

                mCache.put(path,bm);
            }
        }

    }

    /**
     * 根据所需的宽高将路径中的图片解压出来
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeImageByPath(String path, int width, int height) {

        //只把宽高解压出来
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        //获取到图片实际的尺寸
        BitmapFactory.decodeFile(path,options);

        //根据实际宽高和需要压缩的宽高计算出压缩比例
        options.inSampleSize=calculateInSampleSize(options,width,height);

        //根据比例对图片压缩
        options.inJustDecodeBounds=false;
        Bitmap bm=BitmapFactory.decodeFile(path,options);
        return bm;
    }

    /**
     * 根据图片的宽高和请求的宽高计算出压缩比例
     * @param options
     * @param width
     * @param height
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int width, int height) {
        int inSampleSize=1;
        if (options.outWidth>width||height>options.outHeight){
            int widthRatio=Math.round(1.0f*options.outWidth/width);
            int heightRatio=Math.round(1.0f*options.outHeight/height);
            inSampleSize=Math.max(widthRatio,heightRatio);

        }
        return inSampleSize;
    }

    /**
     * 根据imageView获取适当的压缩尺寸
     * @param imageView
     * @return
     */

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
            width=getImageViewFieldValue(imageView,"mMaxWidth");
        }
        if (height<=0){
            //height=imageView.getMaxHeight();//api16以后才有的
            height=getImageViewFieldValue(imageView,"mMaxHeight");
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

    /**
     * 通过反射得到imageView尺寸最大值
     * @param imageView
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(ImageView imageView,String fieldName){
        int val=0;

        try {
            Field field=ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            int fieldVal=field.getInt(imageView);

            if (fieldVal>0&&fieldVal<Integer.MAX_VALUE){
                val=fieldVal;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return val;
    }

    /**
     * 向任务队列中添加任务，通知后台线程去从队列中取出，交给线程池
     * 由于操作任务队列，所以需要进行synchronized同步
     * @param runnable
     */
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);

        //请求信号量，等待后台线程中把handler初始化成功
        try {
            if (mLoopThreadHandler==null)
                mSemaphore_LoopThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

