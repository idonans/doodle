package com.idonans.doodle.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.idonans.acommon.AppContext;
import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.data.StorageManager;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.Threads;
import com.idonans.acommon.util.FileUtil;
import com.idonans.acommon.util.IOUtil;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.DoodleData;
import com.idonans.doodle.DoodleView;
import com.idonans.doodle.dd.DoodleDataEditor;
import com.idonans.doodle.dd.v1.DoodleDataEditorV1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.UUID;

public class MainActivity extends CommonActivity implements ConfirmAspectRadioSizeDialog.OnConfirmListener {

    private static final String TAG = "MainActivity";
    private static final String EXTRA_DOODLE_DATA_KEY = "doodle_data";
    private DoodleView mDoodleView;
    private DoodleActionPanel mDoodleActionPanel;

    private String mDoodleDataKey;

    private static final String EXTRA_PENDING_ASPECT_WIDTH = "aspect_width";
    private static final String EXTRA_PENDING_ASPECT_HEIGHT = "aspect_height";
    private int mPendingAspectWidth;
    private int mPendingAspectHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if (savedInstanceState != null) {
            mDoodleDataKey = savedInstanceState.getString(EXTRA_DOODLE_DATA_KEY);
            mPendingAspectWidth = savedInstanceState.getInt(EXTRA_PENDING_ASPECT_WIDTH);
            mPendingAspectHeight = savedInstanceState.getInt(EXTRA_PENDING_ASPECT_HEIGHT);
        }

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);

        mDoodleActionPanel = new DoodleActionPanel(this, savedInstanceState);
        mDoodleActionPanel.attach(mDoodleView);
        mDoodleActionPanel.setActionListener(new DoodleActionPanel.SimpleActionListener() {
            @Override
            public void saveAsBitmap() {
                MainActivity.this.saveAsBitmap();
            }

            @Override
            public void onBrushChanged() {
                mDoodleView.setBrush(mDoodleActionPanel.createBrush());
            }

            @Override
            public void changeCanvasSizeAspectRadioTo(int aspectWidth, int aspectHeight) {
                mPendingAspectWidth = aspectWidth;
                mPendingAspectHeight = aspectHeight;
                ConfirmAspectRadioSizeDialog dialog = new ConfirmAspectRadioSizeDialog();
                dialog.show(getSupportFragmentManager(), "ConfirmAspectRadioSizeDialog");
            }

            @Override
            public void play() {
                mDoodleView.save(new DoodleView.SaveDataActionCallback() {
                    @Override
                    public void onDataSaved(@Nullable DoodleData doodleData) {
                        DoodleDataAsyncTask.remove("play");
                        DoodleDataAsyncTask.save("play", doodleData, false, new DoodleDataAsyncTask.DoodleDataSaveCallback() {
                            @Override
                            public void onSaveSuccess(final String path) {
                                Threads.runOnUi(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (MainActivity.this.isAvailable()) {
                                            Intent intent = DoodlePlayActivity.start(MainActivity.this, path, false);
                                            MainActivity.this.startActivity(intent);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        // init brush
        mDoodleView.setBrush(mDoodleActionPanel.createBrush());

        // try load history
        if (mDoodleDataKey != null) {
            DoodleDataAsyncTask.load(mDoodleDataKey, new DoodleDataAsyncTask.DoodleDataLoadCallback() {
                @Override
                public void onDoodleDataLoad(DoodleData doodleData) {
                    if (doodleData != null && isAvailable()) {
                        mDoodleView.load(doodleData);
                    }
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mDoodleActionPanel.onSaveInstanceState(outState);

        if (mDoodleDataKey == null) {
            mDoodleDataKey = UUID.randomUUID().toString();
        } else {
            // 删除上一次保存的临时数据
            DoodleDataAsyncTask.remove(mDoodleDataKey);
        }
        outState.putString(EXTRA_DOODLE_DATA_KEY, mDoodleDataKey);

        mDoodleView.save(new DoodleView.SaveDataActionCallback() {
            @Override
            public void onDataSaved(@Nullable DoodleData doodleData) {
                DoodleDataAsyncTask.save(mDoodleDataKey, doodleData);
            }
        });

        outState.putInt(EXTRA_PENDING_ASPECT_WIDTH, mPendingAspectWidth);
        outState.putInt(EXTRA_PENDING_ASPECT_HEIGHT, mPendingAspectHeight);
    }

    private void saveAsBitmap() {
        mDoodleView.saveAsBitmap(new DoodleView.SaveAsBitmapCallback() {
            @Override
            public void onSavedAsBitmap(final Bitmap bitmap) {
                if (bitmap == null) {
                    Toast.makeText(AppContext.getContext(), " bitmap is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AppContext.getContext(), "save to file...", Toast.LENGTH_SHORT).show();
                SaveBitmapToFileAsyncTask.save(bitmap);
            }
        });
    }

    @Override
    protected void onDestroy() {
        CommonLog.d(TAG + " onDestroy");
        super.onDestroy();
        // 正常结束时删除可能存在的临时文件
        if (mDoodleDataKey != null) {
            DoodleDataAsyncTask.remove(mDoodleDataKey);
        }
    }

    @Override
    public void onConfirm(boolean cancel) {
        if (!cancel && mDoodleView != null) {
            if (mPendingAspectWidth > 0 && mPendingAspectHeight > 0) {
                mDoodleView.setAspectRatio(mPendingAspectWidth, mPendingAspectHeight);
            } else {
                throw new IllegalArgumentException("pending aspect radio invalid [" + mPendingAspectWidth + ", " + mPendingAspectHeight + "]");
            }
        }
    }

    private static class DoodleDataAsyncTask {

        public interface DoodleDataLoadCallback {
            void onDoodleDataLoad(DoodleData doodleData);
        }

        public interface DoodleDataSaveCallback {
            void onSaveSuccess(String path);
        }

        // 使用单任务队列确保 save & load 不会冲突
        private static final TaskQueue mTaskQueue = new TaskQueue(1);

        public static void save(final String key, final DoodleData doodleData) {
            save(key, doodleData, null);
        }

        public static void save(final String key, final DoodleData doodleData, final DoodleDataSaveCallback callback) {
            save(key, doodleData, true, callback);
        }

        public static void save(final String key, final DoodleData doodleData, final boolean ignoreEmptyDrawStep, final DoodleDataSaveCallback callback) {
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    if (doodleData == null) {
                        return;
                    }

                    File file = FileUtil.createNewTmpFileQuietly("doodle", ".dd", FileUtil.getPublicPictureDir());
                    if (file == null) {
                        showMessage("save to dd file... fail to create file");
                        return;
                    }

                    if (DoodleDataEditorV1.saveToFile(file.getAbsolutePath(), doodleData, ignoreEmptyDrawStep)) {
                        StorageManager.getInstance().setCache(key, file.getAbsolutePath());
                        if (callback != null) {
                            callback.onSaveSuccess(file.getAbsolutePath());
                        }
                    } else {
                        showMessage("save to dd file... fail to save");
                        // save fail, delete tmp file
                        FileUtil.deleteFileQuietly(file);
                    }
                }
            });
        }

        public static void remove(final String key) {
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    String ddFilePath = StorageManager.getInstance().getCache(key);
                    StorageManager.getInstance().setCache(key, null);
                    FileUtil.deleteFileQuietly(ddFilePath);
                }
            });
        }

        /**
         * callback on ui thread
         */
        public static void load(final String key, final DoodleDataLoadCallback callback) {
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    String ddFilePath = StorageManager.getInstance().getCache(key);
                    if (TextUtils.isEmpty(ddFilePath)) {
                        return;
                    }

                    final DoodleData doodleData;
                    int ddVersion = DoodleDataEditor.getVersion(ddFilePath);
                    if (ddVersion == -1) {
                        showMessage("dd 文件已被破坏");
                        doodleData = null;
                    } else if (ddVersion != 1) {
                        showMessage("dd 文件版本不支持");
                        doodleData = null;
                    } else {
                        doodleData = DoodleDataEditorV1.readFromFile(ddFilePath);
                    }

                    Threads.runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            callback.onDoodleDataLoad(doodleData);
                        }
                    });
                }
            });
        }

        /**
         * callback on ui thread
         */
        public static void loadDdFile(final String fileName, final DoodleDataLoadCallback callback) {
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    File dir = FileUtil.getPublicPictureDir();
                    if (dir == null) {
                        showMessage("fail to get public picture dir (null)");
                        return;
                    }

                    File file = new File(dir, fileName);
                    String ddFilePath = file.getAbsolutePath();

                    final DoodleData doodleData;
                    int ddVersion = DoodleDataEditor.getVersion(ddFilePath);
                    if (ddVersion == -1) {
                        showMessage("dd 文件已被破坏");
                        doodleData = null;
                    } else if (ddVersion != 1) {
                        showMessage("dd 文件版本不支持");
                        doodleData = null;
                    } else {
                        doodleData = DoodleDataEditorV1.readFromFile(ddFilePath);
                    }
                    Threads.runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            callback.onDoodleDataLoad(doodleData);
                        }
                    });
                }
            });
        }

    }

    private static class SaveBitmapToFileAsyncTask {

        private static final TaskQueue mTaskQueue = new TaskQueue(1);

        public static void save(@NonNull final Bitmap bitmap) {
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    File file = FileUtil.createNewTmpFileQuietly("doodle", ".png", FileUtil.getPublicPictureDir());
                    if (file == null) {
                        showMessage("save to file... fail to create file");
                        return;
                    }

                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                        if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                            showMessage("save to file... success " + file.getAbsolutePath());
                        } else {
                            showMessage("save to file... fail to save bitmap to file, compress false");
                            FileUtil.deleteFileQuietly(file);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        showMessage("save to file... fail to save bitmap to file");
                        FileUtil.deleteFileQuietly(file);
                    } finally {
                        IOUtil.closeQuietly(fos);
                    }
                }
            });
        }

    }

    private static void showMessage(final String message) {
        Threads.runOnUi(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AppContext.getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
