package com.idonans.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
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
import com.idonans.doodle.brush.Brush;
import com.idonans.doodle.brush.Pencil;
import com.idonans.doodle.dd.DoodleDataEditor;
import com.idonans.doodle.dd.v1.DoodleDataEditorV1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.UUID;

public class MainActivity extends CommonActivity implements BrushSettingFragment.BrushSettingListener {

    private static final String TAG = "MainActivity";
    private static final String EXTRA_DOODLE_DATA_KEY = "doodle_data";
    private DoodleView mDoodleView;
    private ViewGroup mDoodleActionPanel;
    private View mUndo;
    private View mRedo;
    private View mSetBrush;

    private int mAspectType = 1;

    private String mDoodleDataKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if (savedInstanceState != null) {
            mDoodleDataKey = savedInstanceState.getString(EXTRA_DOODLE_DATA_KEY);
        }

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
        mDoodleView.setBrush(new Pencil(Color.BLACK, 50, 255));

        mDoodleView.setCanvasBackgroundColor(0x55ffff00);

        mDoodleActionPanel = ViewUtil.findViewByID(this, R.id.doodle_action_panel);
        mUndo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.undo);
        mRedo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.redo);

        View viewSetAspect = ViewUtil.findViewByID(mDoodleActionPanel, R.id.set_aspect);
        viewSetAspect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAspectType == 1) {
                    mAspectType = 0;
                    mDoodleView.setAspectRatio(3, 4);
                } else {
                    mAspectType = 1;
                    mDoodleView.setAspectRatio(1, 1);
                }
            }
        });

        mUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodleView.undo();
            }
        });

        mRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodleView.redo();
            }
        });

        syncUndoRedoStatus();

        mDoodleView.setDoodleBufferChangedListener(new DoodleView.DoodleBufferChangedListener() {
            @Override
            public void onDoodleBufferChanged(boolean canUndo, boolean canRedo) {
                CommonLog.d(TAG + " onDoodleBufferChanged canUndo:" + canUndo + ", canRedo:" + canRedo);
                mUndo.setEnabled(canUndo);
                mRedo.setEnabled(canRedo);
            }
        });

        mSetBrush = ViewUtil.findViewByID(mDoodleActionPanel, R.id.set_brush);
        mSetBrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBrushSetting();
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

    private void disableDoodleAction() {
        mUndo.setEnabled(false);
        mRedo.setEnabled(false);
    }

    private void syncUndoRedoStatus() {
        disableDoodleAction();
        mDoodleView.canUndo(new DoodleView.ActionCallback() {
            @Override
            public void onActionResult(boolean success) {
                mUndo.setEnabled(success);
            }
        });
        mDoodleView.canRedo(new DoodleView.ActionCallback() {
            @Override
            public void onActionResult(boolean success) {
                mRedo.setEnabled(success);
            }
        });
    }

    private void showBrushSetting() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.content_panel, new BrushSettingFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void setBrushColor(int color) {
        Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(brush.cloneWithColor(color));
    }

    @Override
    public void setBrushAlpha(int alpha) {
        Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(brush.cloneWithAlpha(alpha));
    }

    @Override
    public void setBrushSize(int size) {
        Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(brush.cloneWithSize(size));
    }

    @Override
    public void setBrushType(int type) {
        // TODO
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mDoodleDataKey == null) {
            mDoodleDataKey = UUID.randomUUID().toString();
        }
        outState.putString(EXTRA_DOODLE_DATA_KEY, mDoodleDataKey);

        mDoodleView.save(new DoodleView.SaveDataActionCallback() {
            @Override
            public void onDataSaved(@Nullable DoodleData doodleData) {
                DoodleDataAsyncTask.save(mDoodleDataKey, doodleData);
            }
        });
    }

    private DoodleData mDoodleDataSaved;

    @Override
    public void saveDoodleData() {
        mDoodleView.save(new DoodleView.SaveDataActionCallback() {
            @Override
            public void onDataSaved(@Nullable DoodleData doodleData) {
                mDoodleDataSaved = doodleData;
            }
        });
    }

    @Override
    public void restoreDoodleData() {
        mDoodleView.load(mDoodleDataSaved);
    }

    @Override
    public void saveAsBitmap() {
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
                    // remove old
                    StorageManager.getInstance().setCache(key, null);

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
