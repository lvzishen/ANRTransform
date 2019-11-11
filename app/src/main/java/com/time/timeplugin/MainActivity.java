package com.time.timeplugin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.time.timeplugin.block.BlockManager;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.currentTimeMillis();
        loadSoFile();
        int a = test();
        int b = test2();
        Log.i("MainActivity", a + "," + b);
    }

    private int test() {
        int a = 13;
        int b = 5;
        try {
            Log.i("MainActivity", "test");
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return a + b;
    }

    private int test2() {
        int a = 13;
        int b = 5;
        try {
            Log.i("MainActivity", "test");
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return a + b;
    }

    public void loadSoFile() {
        try {
            Log.i("MainActivity", "Loading xxxx.so");
            Thread.sleep(900);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
