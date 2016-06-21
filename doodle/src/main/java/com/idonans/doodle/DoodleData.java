package com.idonans.doodle;

import android.support.annotation.IntDef;

import com.idonans.doodle.brush.Brush;
import com.idonans.doodle.brush.Empty;
import com.idonans.doodle.brush.LeavesPencil;
import com.idonans.doodle.brush.Pencil;
import com.idonans.doodle.drawstep.DrawStep;
import com.idonans.doodle.drawstep.EmptyDrawStep;
import com.idonans.doodle.drawstep.PointDrawStep;
import com.idonans.doodle.drawstep.ScribbleDrawStep;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * 涂鸦板的数据，用于涂鸦板的保存和恢复，可用于加载其他涂鸦板的内容.
 * Created by pengji on 16-6-20.
 */
public class DoodleData {

    /**
     * 第一个版本
     */
    public static final int VERSION_1 = 1;

    /**
     * 涂鸦板数据的版本, 不同版本之间的数据不兼容 (可以借助转换将数据在不同版本之间复制)
     */
    public int version = VERSION_1;

    /**
     * 图像的宽度
     */
    public int width;

    /**
     * 图像的高度
     */
    public int height;

    /**
     * 背景色
     */
    public int backgroundColor;

    public ArrayList<DrawStepData> drawStepDatas;
    public ArrayList<DrawStepData> drawStepDatasRedo;

    public static boolean isVersionSupport(int version) {
        return version == VERSION_1;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setDrawSteps(ArrayList<DrawStep> drawSteps) {
        if (drawSteps == null) {
            this.drawStepDatas = null;
            return;
        }

        this.drawStepDatas = new ArrayList<>(drawSteps.size());
        for (DrawStep drawStep : drawSteps) {
            this.drawStepDatas.add(DrawStepData.create(drawStep));
        }
    }

    public void setDrawStepsRedo(ArrayList<DrawStep> drawStepsRedo) {
        if (drawStepsRedo == null) {
            this.drawStepDatasRedo = null;
            return;
        }

        this.drawStepDatasRedo = new ArrayList<>(drawStepsRedo.size());
        for (DrawStep drawStep : drawStepsRedo) {
            this.drawStepDatasRedo.add(DrawStepData.create(drawStep));
        }
    }

    public static class DrawStepData {

        public BrushData brushData;
        /**
         * 绘画步骤类型
         */
        @DrawStepType
        public int type;
        public ArrayList<Float> points;

        public static DrawStepData create(DrawStep drawStep) {
            if (drawStep == null) {
                return null;
            }

            // 在判断时要注意先区分子类，再区分父类
            if (drawStep instanceof ScribbleDrawStep) {
                ScribbleDrawStep scribbleDrawStep = (ScribbleDrawStep) drawStep;
                DrawStepData drawStepData = new DrawStepData();
                drawStepData.type = DRAW_STEP_TYPE_SCRIBBLE;
                drawStepData.brushData = BrushData.create(scribbleDrawStep.getDrawBrush());
                drawStepData.points = new ArrayList<>(scribbleDrawStep.getAllPoints());
                return drawStepData;
            } else if (drawStep instanceof PointDrawStep) {
                PointDrawStep pointDrawStep = (PointDrawStep) drawStep;
                DrawStepData drawStepData = new DrawStepData();
                drawStepData.type = DRAW_STEP_TYPE_POINT;
                drawStepData.brushData = BrushData.create(pointDrawStep.getDrawBrush());
                drawStepData.points = new ArrayList<>();
                drawStepData.points.add(pointDrawStep.getX());
                drawStepData.points.add(pointDrawStep.getY());
                return drawStepData;
            } else if (drawStep instanceof EmptyDrawStep) {
                DrawStepData drawStepData = new DrawStepData();
                drawStepData.type = DRAW_STEP_TYPE_EMPTY;
                return drawStepData;
            } else {
                throw new IllegalArgumentException("unknown draw step type " + drawStep);
            }
        }

        public DrawStep create() {
            if (this.type == DRAW_STEP_TYPE_SCRIBBLE) {
                ScribbleDrawStep scribbleDrawStep = new ScribbleDrawStep(this.brushData.create(), this.points.get(0), this.points.get(1));
                int size = this.points.size();
                for (int i = 2; i < size; i++) {
                    scribbleDrawStep.toPoint(this.points.get(i), this.points.get(i + 1));
                    i++;
                }
                return scribbleDrawStep;
            } else if (this.type == DRAW_STEP_TYPE_POINT) {
                return new PointDrawStep(this.brushData.create(), this.points.get(0), this.points.get(1));
            } else if (this.type == DRAW_STEP_TYPE_EMPTY) {
                return new EmptyDrawStep();
            } else {
                throw new IllegalArgumentException("unknown draw step type " + this.type);
            }
        }
    }

    public static class BrushData {
        /**
         * 画笔类型
         */
        @BrushType
        public int type;
        /**
         * argb or rgb
         */
        public int color;
        public float size;
        /**
         * 画刷的透明度 [0, 255], 值越大越不透明
         */
        public int alpha;

        public static BrushData create(Brush brush) {
            if (brush == null) {
                return null;
            }

            // 在判断时要注意先区分子类，再区分父类
            if (brush instanceof LeavesPencil) {
                BrushData brushData = new BrushData();
                brushData.type = BRUSH_TYPE_LEAVES;
                brushData.color = brush.color;
                brushData.size = brush.size;
                brushData.alpha = brush.alpha;
                return brushData;
            } else if (brush instanceof Pencil) {
                BrushData brushData = new BrushData();
                brushData.type = BRUSH_TYPE_PENCIL;
                brushData.color = brush.color;
                brushData.size = brush.size;
                brushData.alpha = brush.alpha;
                return brushData;
            } else if (brush instanceof Empty) {
                BrushData brushData = new BrushData();
                brushData.type = BRUSH_TYPE_EMPTY;
                brushData.color = brush.color;
                brushData.size = brush.size;
                brushData.alpha = brush.alpha;
                return brushData;
            } else {
                throw new IllegalArgumentException("unknown brush type " + brush);
            }
        }

        public Brush create() {
            if (this.type == BRUSH_TYPE_LEAVES) {
                return new LeavesPencil(this.color, this.size, this.alpha);
            } else if (this.type == BRUSH_TYPE_PENCIL) {
                return new Pencil(this.color, this.size, this.alpha);
            } else if (this.type == BRUSH_TYPE_EMPTY) {
                return new Empty();
            } else {
                throw new IllegalArgumentException("unknown brush type " + this.type);
            }
        }

    }

    //////
    /**
     * 空步骤
     */
    public static final int DRAW_STEP_TYPE_EMPTY = 1;
    /**
     * 画点
     */
    public static final int DRAW_STEP_TYPE_POINT = 2;
    /**
     * 自由绘制
     */
    public static final int DRAW_STEP_TYPE_SCRIBBLE = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DRAW_STEP_TYPE_EMPTY, DRAW_STEP_TYPE_POINT, DRAW_STEP_TYPE_SCRIBBLE})
    public @interface DrawStepType {
    }

    //////
    /**
     * 空画笔
     */
    public static final int BRUSH_TYPE_EMPTY = 1;
    /**
     * 铅笔
     */
    public static final int BRUSH_TYPE_PENCIL = 2;
    /**
     * 柳叶笔
     */
    public static final int BRUSH_TYPE_LEAVES = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BRUSH_TYPE_EMPTY, BRUSH_TYPE_PENCIL, BRUSH_TYPE_LEAVES})
    public @interface BrushType {
    }

    //////

}
