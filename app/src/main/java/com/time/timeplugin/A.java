package com.time.timeplugin;
import com.time.timeplugin.CustomThread;
/**
 * 创建日期：2019/11/4 on 11:59
 * 描述:
 * 作者: lvzishen
 */
public class A {

    private void a() {
        new CustomThread(new Runnable() {
            @Override
            public void run() {

            }
        }).start();
    }

}
