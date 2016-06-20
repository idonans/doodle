package com.idonans.doodle;

import java.util.List;

/**
 * 涂鸦板的数据，用于涂鸦板的保存和恢复，可用于加载其他涂鸦板的内容. ( json 格式)
 * Created by pengji on 16-6-20.
 */
public class DoodleData {

    /**
     * 涂鸦板数据的版本, 不同版本之间的数据不兼容 (可以借助转换将数据在不同版本之间复制)
     */
    public int version = 1;

    /**
     * 图像的宽度
     */
    public int width;

    /**
     * 图像的高度
     */
    public int height;

    private List<DrawStepData> drawStepDatas;
    private List<DrawStepData> drawStepDatasRedo;

    public static class DrawStepData {

        public BrushData brushData;
        /**
         * 绘画步骤类型
         */
        @DrawStepType
        public int type;
        public List<Float> points;

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
    }

    public @interface DrawStepType {
        /**
         * 空步骤
         */
        int EMPTY = 0;
        /**
         * 画点
         */
        int POINT = 1;
        /**
         * 自由绘制
         */
        int SCRIBBLE = 2;
    }

    public @interface BrushType {
        /**
         * 空画笔
         */
        int EMPTY = 0;
        /**
         * 铅笔
         */
        int PENCIL = 1;
        /**
         * 柳叶笔
         */
        int LEAVES = 2;
    }

}
