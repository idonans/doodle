package com.idonans.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.idonans.acommon.app.CommonFragment;
import com.idonans.acommon.util.ViewUtil;

import java.util.ArrayList;

/**
 * 画刷设置
 * Created by idonans on 16-5-17.
 */
public class BrushSettingFragment extends CommonFragment {

    public interface BrushSettingListener {
        void setBrushColor(int color);

        void setBrushSize(int size);

        void setBrushAlpha(int alpha);

        void setBrushType(int type);
    }

    public static class SimpleBrushSettingListener implements BrushSettingListener {

        @Override
        public void setBrushColor(int color) {
            // ignore
        }

        @Override
        public void setBrushSize(int size) {
            // ignore
        }

        @Override
        public void setBrushAlpha(int alpha) {
            // ignore
        }

        @Override
        public void setBrushType(int type) {
            // ignore
        }
    }

    private BrushSettingListener mEmptyBrushSettingListener = new SimpleBrushSettingListener();

    private BrushSettingListener getBrushSettingListener() {
        Activity activity = getActivity();
        if (activity instanceof BrushSettingListener) {
            return ((BrushSettingListener) activity);
        }

        return mEmptyBrushSettingListener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.brush_setting_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        {
            ArrayList<TextView> views = new ArrayList<>();
            views.add((TextView) ViewUtil.findViewByID(view, R.id.color_red));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.color_black));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.color_white));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.color_yellow));
            for (TextView v : views) {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getBrushSettingListener().setBrushColor(Color.parseColor(((TextView) v).getText().toString()));
                    }
                });
            }
        }

        {
            ArrayList<TextView> views = new ArrayList<>();
            views.add((TextView) ViewUtil.findViewByID(view, R.id.size_10));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.size_20));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.size_40));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.size_80));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.size_160));
            for (TextView v : views) {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getBrushSettingListener().setBrushSize(Integer.valueOf(((TextView) v).getText().toString()));
                    }
                });
            }
        }

        {
            ArrayList<TextView> views = new ArrayList<>();
            views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_10));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_50));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_100));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_150));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_200));
            views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_255));
            for (TextView v : views) {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getBrushSettingListener().setBrushAlpha(Integer.valueOf(((TextView) v).getText().toString()));
                    }
                });
            }
        }
    }

}
