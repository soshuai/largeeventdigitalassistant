package com.largeevent.management;

import androidx.multidex.MultiDexApplication;

/**
 * 应用程序入口类
 * 继承 MultiDexApplication 以支持超过 64K 方法数
 */
public class LargeEventApplication extends MultiDexApplication {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // 应用初始化逻辑
    }
}
