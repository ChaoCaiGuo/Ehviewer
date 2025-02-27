/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Debug;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.collection.LruCache;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hippo.Native;
import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.conaco.Conaco;
import com.hippo.database.EhDB;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhEngine;
import com.hippo.ehviewer.client.EhRequestBuilder;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.parser.EventPaneParser;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.image.Image;
import com.hippo.image.ImageBitmap;
import com.hippo.network.StatusCodeException;
import com.hippo.scene.SceneApplication;
import com.hippo.unifile.UniFile;
import com.hippo.util.BitmapUtils;
import com.hippo.util.ClipboardUtil;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.util.ReadableTime;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IntIdGenerator;
import com.hippo.yorozuya.OSUtils;
import com.hippo.yorozuya.SimpleHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import coil.Coil;
import coil.ImageLoader;
import coil.memory.MemoryCache;
import coil.request.CachePolicy;
import dagger.hilt.android.HiltAndroidApp;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@HiltAndroidApp
public class EhApplication extends SceneApplication {

    public static final boolean BETA = false;
    private static final String TAG = EhApplication.class.getSimpleName();
    private static final String KEY_GLOBAL_STUFF_NEXT_ID = "global_stuff_next_id";
    private static final boolean DEBUG_CONACO = false;
    private static final boolean DEBUG_PRINT_NATIVE_MEMORY = false;
    private static final boolean DEBUG_PRINT_IMAGE_COUNT = false;
    private static final long DEBUG_PRINT_INTERVAL = 3000L;

    public static boolean locked = false;

    private static EhApplication instance;

    private final IntIdGenerator mIdGenerator = new IntIdGenerator();
    private final HashMap<Integer, Object> mGlobalStuffMap = new HashMap<>();
    private final List<Activity> mActivityList = new ArrayList<>();
    @Inject EhCookieStore mEhCookieStore;
    @Inject EhClient mEhClient;
    @Inject  EhProxySelector mEhProxySelector;
    @Inject OkHttpClient mOkHttpClient;
    @Inject ImageBitmapHelper mImageBitmapHelper;
    private Conaco<ImageBitmap> mConaco;
    private LruCache<Long, GalleryDetail> mGalleryDetailCache;
    private SimpleDiskCache mSpiderInfoCache;
    @Inject DownloadManager mDownloadManager;
    @Inject Hosts mHosts;
    @Inject FavouriteStatusRouter mFavouriteStatusRouter;
    private boolean initialized = false;

    public static EhApplication getInstance() {
        return instance;
    }

    public static EhCookieStore getEhCookieStore(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mEhCookieStore;
    }

    @NonNull
    public static EhClient getEhClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mEhClient;
    }

    @NonNull
    public static EhProxySelector getEhProxySelector(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mEhProxySelector;
    }

    @NonNull
    public static OkHttpClient getOkHttpClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mOkHttpClient;
    }


    @NonNull
    public static ImageBitmapHelper getImageBitmapHelper(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mImageBitmapHelper;
    }

    private static int getMemoryCacheMaxSize() {
        return Math.min(20 * 1024 * 1024, (int) OSUtils.getAppMaxMemory());
    }

    @NonNull
    public static Conaco<ImageBitmap> getConaco(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mConaco == null) {
            Conaco.Builder<ImageBitmap> builder = new Conaco.Builder<>();
            builder.hasMemoryCache = true;
            builder.memoryCacheMaxSize = getMemoryCacheMaxSize();
            builder.hasDiskCache = true;
            builder.diskCacheDir = new File(context.getCacheDir(), "thumb");
            builder.diskCacheMaxSize = 320 * 1024 * 1024; // 320MB
            builder.okHttpClient = getOkHttpClient(context);
            builder.objectHelper = getImageBitmapHelper(context);
            builder.debug = DEBUG_CONACO;
            application.mConaco = builder.build();
        }
        return application.mConaco;
    }

    @NonNull
    public static LruCache<Long, GalleryDetail> getGalleryDetailCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mGalleryDetailCache == null) {
            // Max size 25, 3 min timeout
            application.mGalleryDetailCache = new LruCache<>(25);
            getFavouriteStatusRouter().addListener((gid, slot) -> {
                GalleryDetail gd = application.mGalleryDetailCache.get(gid);
                if (gd != null) {
                    gd.favoriteSlot = slot;
                }
            });
        }
        return application.mGalleryDetailCache;
    }

    @NonNull
    public static SimpleDiskCache getSpiderInfoCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (null == application.mSpiderInfoCache) {
            application.mSpiderInfoCache = new SimpleDiskCache(
                    new File(context.getCacheDir(), "spider_info"), 20 * 1024 * 1024); // 20M
        }
        return application.mSpiderInfoCache;
    }


    @NonNull
    public static DownloadManager getDownloadManager(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mDownloadManager;
    }

    @NonNull
    public static Hosts getHosts(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mHosts;
    }

    @NonNull
    public static FavouriteStatusRouter getFavouriteStatusRouter() {
        return getFavouriteStatusRouter(getInstance());
    }

    @NonNull
    public static FavouriteStatusRouter getFavouriteStatusRouter(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        return application.mFavouriteStatusRouter;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onCreate() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new EhlifecycleObserver());
        instance = this;

        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                // Always save crash file if onCreate() is not done
                if (!initialized || Settings.getSaveCrashLog()) {
                    Crash.saveCrashLog(instance, e);
                }
            } catch (Throwable ignored) {
            }

            if (handler != null) {
                handler.uncaughtException(t, e);
            }
        });


        Native.initialize();
        ClipboardUtil.initialize(this);
        GetText.initialize(this);
        StatusCodeException.initialize(this);
        Settings.initialize(this);
        ReadableTime.initialize(this);
        AppConfig.initialize(this);
        SpiderDen.initialize(this);
        EhDB.initialize(this);
        EhEngine.initialize();
        BitmapUtils.initialize(this);

        super.onCreate();

        if (EhDB.needMerge()) {
            EhDB.mergeOldDB(this);
        }

        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(Settings.getLocale()));
        AppCompatDelegate.setDefaultNightMode(Settings.getTheme());

        // Do io tasks in new thread
        //noinspection deprecation
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                // Check no media file
                try {
                    UniFile downloadLocation = Settings.getDownloadLocation();
                    if (Settings.getMediaScan()) {
                        CommonOperations.removeNoMediaFile(downloadLocation);
                    } else {
                        CommonOperations.ensureNoMediaFile(downloadLocation);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    ExceptionUtils.throwIfFatal(t);
                }

                // Clear temp files
                try {
                    clearTempDir();
                } catch (Throwable t) {
                    t.printStackTrace();
                    ExceptionUtils.throwIfFatal(t);
                }

                return null;
            }
        }.executeOnExecutor(IoThreadPoolExecutor.getInstance());

        // Check app update
        update();

        // Update version code
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            Settings.putVersionCode(pi.versionCode);
            Settings.putVersionName(pi.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }

        mIdGenerator.setNextId(Settings.getInt(KEY_GLOBAL_STUFF_NEXT_ID, 0));

        if (DEBUG_PRINT_NATIVE_MEMORY || DEBUG_PRINT_IMAGE_COUNT) {
            debugPrint();
        }

        //Coil config:
        var imageLoader = new ImageLoader.Builder(this)
                .memoryCache( new MemoryCache.Builder(this).maxSizePercent(0.3f).build()) //内存缓存占比
                .diskCachePolicy(CachePolicy.ENABLED)  //磁盘缓策略 ENABLED、READ_ONLY、WRITE_ONLY、DISABLED
                .crossfade(true) //淡入淡出
                .crossfade(500)  //淡入淡出时间
                .okHttpClient(getOkHttpClient(this)) //设置OkHttpClient
                .build();
        Coil.setImageLoader(imageLoader);


        initialized = true;
        theDawnOfNewDay();
    }

    private void theDawnOfNewDay() {
        if (!Settings.getRequestNews()) {
            return;
        }
        EhCookieStore store = getEhCookieStore(this);
        HttpUrl eh = HttpUrl.get(EhUrl.HOST_E);

        if (store.contains(eh, EhCookieStore.KEY_IPD_MEMBER_ID) || store.contains(eh, EhCookieStore.KEY_IPD_PASS_HASH)) {
            IoThreadPoolExecutor.getInstance().execute(() -> {
                String referer = EhUrl.REFERER_E;
                Request request = new EhRequestBuilder(EhUrl.HOST_E + "news.php", referer).build();
                Call call = getOkHttpClient(this).newCall(request);
                try {
                    Response response = call.execute();
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        return;
                    }
                    String body = responseBody.string();
                    String html = EventPaneParser.parse(body);
                    if (html != null) {
                        showEventPane(html);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void showEventPane(String html) {
        Activity activity = getTopActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                        .setMessage(Html.fromHtml(html))
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                dialog.setOnShowListener(d -> {
                    final View messageView = dialog.findViewById(android.R.id.message);
                    if (messageView instanceof TextView) {
                        ((TextView) messageView).setMovementMethod(LinkMovementMethod.getInstance());
                    }
                });
                try {
                    dialog.show();
                } catch (Throwable t) {
                    // ignore
                }
            });
        }
    }

    private void clearTempDir() {
        File dir = AppConfig.getTempDir();
        if (null != dir) {
            FileUtils.deleteContent(dir);
        }
        dir = AppConfig.getExternalTempDir();
        if (null != dir) {
            FileUtils.deleteContent(dir);
        }
    }

    private void update() {
        int version = Settings.getVersionCode();
        if (version < 52) {
            Settings.putGuideGallery(true);
        }
    }

    public void clearMemoryCache() {
        if (null != mConaco) {
            mConaco.getBeerBelly().clearMemory();
        }
        if (null != mGalleryDetailCache) {
            mGalleryDetailCache.evictAll();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            clearMemoryCache();
        }
    }

    private void debugPrint() {
        new Runnable() {
            @Override
            public void run() {
                if (DEBUG_PRINT_NATIVE_MEMORY) {
                    Log.i(TAG, "Native memory: " + FileUtils.humanReadableByteCount(
                            Debug.getNativeHeapAllocatedSize(), false));
                }
                if (DEBUG_PRINT_IMAGE_COUNT) {
                    Log.i(TAG, "Image count: " + Image.getImageCount());
                }
                SimpleHandler.getInstance().postDelayed(this, DEBUG_PRINT_INTERVAL);
            }
        }.run();
    }

    public int putGlobalStuff(@NonNull Object o) {
        int id = mIdGenerator.nextId();
        mGlobalStuffMap.put(id, o);
        Settings.putInt(KEY_GLOBAL_STUFF_NEXT_ID, mIdGenerator.nextId());
        return id;
    }

    public boolean containGlobalStuff(int id) {
        return mGlobalStuffMap.containsKey(id);
    }

    public Object getGlobalStuff(int id) {
        return mGlobalStuffMap.get(id);
    }

    public Object removeGlobalStuff(int id) {
        return mGlobalStuffMap.remove(id);
    }

    public void removeGlobalStuff(Object o) {
        mGlobalStuffMap.values().removeAll(Collections.singleton(o));
    }

    public void registerActivity(Activity activity) {
        mActivityList.add(activity);
    }

    public void unregisterActivity(Activity activity) {
        mActivityList.remove(activity);
    }

    @Nullable
    public Activity getTopActivity() {
        if (!mActivityList.isEmpty()) {
            return mActivityList.get(mActivityList.size() - 1);
        } else {
            return null;
        }
    }

    // Avoid crash on some "energy saving" devices
    @Override
    public ComponentName startService(Intent service) {
        try {
            return super.startService(service);
        } catch (Throwable t) {
            t.printStackTrace();
            ExceptionUtils.throwIfFatal(t);
            return null;
        }
    }

    // Avoid crash on some "energy saving" devices
    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        try {
            return super.bindService(service, conn, flags);
        } catch (Throwable t) {
            t.printStackTrace();
            ExceptionUtils.throwIfFatal(t);
            return false;
        }
    }

    // Avoid crash on some "energy saving" devices
    @Override
    public void unbindService(ServiceConnection conn) {
        try {
            super.unbindService(conn);
        } catch (Throwable t) {
            t.printStackTrace();
            ExceptionUtils.throwIfFatal(t);
        }
    }

    // Avoid crash on some "energy saving" devices
    @Override
    public ComponentName startForegroundService(Intent service) {
        try {
            return super.startForegroundService(service);
        } catch (Throwable t) {
            t.printStackTrace();
            ExceptionUtils.throwIfFatal(t);
            return null;
        }
    }

    static class EhlifecycleObserver implements DefaultLifecycleObserver {
        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            locked = true;
        }
    }
}
