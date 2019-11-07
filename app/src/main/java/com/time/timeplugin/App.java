package com.time.timeplugin;

import android.app.Application;
import android.util.Log;

import com.time.timeplugin.block.BlockManager;
import com.time.timeplugin.block.IBlockHandler;
import com.time.timeplugin.block.StacktraceBlockHandler;

/**
 * Created by quinn on 14/09/2018
 */
public class App extends Application {

    private IBlockHandler customBlockManager = new StacktraceBlockHandler(50);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("asd", "asdsad");
        BlockManager.installBlockManager(customBlockManager);
    }

    public IBlockHandler getCustomBlockManager() {
        return customBlockManager;
    }

}
