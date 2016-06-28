package com.idonans.doodle.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.idonans.acommon.util.ViewUtil;

/**
 * Created by pengji on 16-6-27.
 */
public class ConfirmAspectRadioSizeDialog extends BottomSheetDialogFragment {

    public interface OnConfirmListener {
        void onConfirm(boolean cancel);
    }

    private static final OnConfirmListener EMPTY_CONFIRM_LISTENER = new OnConfirmListener() {
        @Override
        public void onConfirm(boolean cancel) {
            // ignore
        }
    };

    private OnConfirmListener getConfirmListener() {
        Activity activity = getActivity();
        if (activity instanceof OnConfirmListener) {
            return (OnConfirmListener) activity;
        }
        return EMPTY_CONFIRM_LISTENER;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_confirm_aspect_radio_size, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        View btnOk = ViewUtil.findViewByID(view, R.id.btn_ok);
        View btnCancel = ViewUtil.findViewByID(view, R.id.btn_cancel);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getConfirmListener().onConfirm(false);
                dismiss();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getConfirmListener().onConfirm(true);
                dismiss();
            }
        });
    }

}
