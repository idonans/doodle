package com.idonans.app;

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

public class MainActivity extends CommonActivity {

    private static final String TAG = "MainActivity";
    private static final String EXTRA_DOODLE_DATA_KEY = "doodle_data";
    private DoodleView mDoodleView;
    private DoodleActionPanel mDoodleActionPanel;

    private String mDoodleDataKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if (savedInstanceState != null) {
            mDoodleDataKey = savedInstanceState.getString(EXTRA_DOODLE_DATA_KEY);
        }

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);

        mDoodleActionPanel = new DoodleActionPanel(getWindow().getDecorView(), savedInstanceState);
        mDoodleActionPanel.attach(mDoodleView);
        mDoodleActionPanel.setActionListener(new DoodleActionPanel.SimpleActionListener() {
            @Override
            public void saveAsBitmap() {
                MainActivity.this.saveAsBitmap();
            }

            @Override
            public void onSizeAlphaColorChanged() {
                // TODO change current brush size, alpha, color.
            }
        });

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
        } else {
            // for test load dd file
            DoodleDataAsyncTask.loadDdFile("doodle1466581651277.dd", new DoodleDataAsyncTask.DoodleDataLoadCallback() {
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

    private static class DoodleDataAsyncTask {

        public interface DoodleDataLoadCallback {
            void onDoodleDataLoad(DoodleData doodleData);
        }

        // 使用单任务队列确保 save & load 不会冲突
        private static final TaskQueue mTaskQueue = new TaskQueue(1);

        public static void save(final String key, final DoodleData doodleData) {
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

                    if (DoodleDataEditorV1.saveToFile(file.getAbsolutePath(), doodleData)) {
                        StorageManager.getInstance().setCache(key, file.getAbsolutePath());
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
