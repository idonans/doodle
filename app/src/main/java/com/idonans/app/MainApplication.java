package com.idonans.app;

import android.app.Application;
import android.util.Log;

import com.idonans.acommon.App;
import com.squareup.leakcanary.LeakCanary;

/**
 * enter
 * Created by idonans on 16-5-13.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        App.init(new App.Config.Builder()
                .setContext(this)
                .setBuildConfigAdapter(new BuildConfigAdapterImpl())
                .build());
    }

    public static class BuildConfigAdapterImpl implements App.BuildConfigAdapter {

        @Override
        public int getVersionCode() {
            return BuildConfig.VERSION_CODE;
        }

        @Override
        public String getVersionName() {
            return BuildConfig.VERSION_NAME;
        }

        @Override
        public String getLogTag() {
            return BuildConfig.APPLICATION_ID;
        }

        @Override
        public int getLogLevel() {
            return Log.DEBUG;
        }

        @Override
        public boolean isDebug() {
            return BuildConfig.DEBUG;
        }
    }

}
