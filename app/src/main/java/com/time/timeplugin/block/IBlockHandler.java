package com.time.timeplugin.block;

import android.util.Log;

/**
 * Created by lvzishen on 11/07/2019
 */
public interface IBlockHandler {

    IBlockHandler DEFAULT = new IBlockHandler() {

        private static final String TAG = "Default-IBlockHandler";

        private static final int BLOCK_THRESHOLD = 100;

        @Override
        public void timingMethod(String method, int cost) {
            if (cost >= threshold()) {
                Log.i(TAG, method + " costed " + cost);
            }
        }

        @Override
        public String dump() {
            return "";
        }

        @Override
        public void clear() {

        }

        @Override
        public int threshold() {
            return BLOCK_THRESHOLD;
        }
    };

    public void timingMethod(String method, int cost);

    public String dump();

    public void clear();

    public int threshold();

}
