package com.largeevent.management.image;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

/**
 * 图片加载工具类
 */
public class ImageLoader {
    
    /**
     * 加载普通图片
     * @param context 上下文
     * @param url 图片URL
     * @param imageView ImageView控件
     */
    public static void loadImage(Context context, String url, ImageView imageView) {
        Glide.with(context)
                .load(url)
                .into(imageView);
    }
    
    /**
     * 加载圆形图片
     * @param context 上下文
     * @param url 图片URL
     * @param imageView ImageView控件
     */
    public static void loadCircleImage(Context context, String url, ImageView imageView) {
        RequestOptions options = RequestOptions.circleCropTransform();
        Glide.with(context)
                .load(url)
                .apply(options)
                .into(imageView);
    }
    
    /**
     * 加载圆角图片
     * @param context 上下文
     * @param url 图片URL
     * @param imageView ImageView控件
     * @param radius 圆角半径
     */
    public static void loadRoundImage(Context context, String url, ImageView imageView, int radius) {
        RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(radius));
        Glide.with(context)
                .load(url)
                .apply(options)
                .into(imageView);
    }
    
    /**
     * 清除图片缓存
     * @param context 上下文
     */
    public static void clearCache(Context context) {
        Glide.get(context).clearMemory();
        new Thread(() -> Glide.get(context).clearDiskCache()).start();
    }
}