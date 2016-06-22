package com.idonans.doodle.dd.v1;

import com.idonans.acommon.lang.Charsets;
import com.idonans.acommon.util.IOUtil;
import com.idonans.doodle.DoodleData;
import com.idonans.doodle.dd.DoodleDataEditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * doodle data editor v1
 * Created by pengji on 16-6-22.
 */
public class DoodleDataEditorV1 extends DoodleDataEditor {

    /**
     * 将 DoodleData 保存到指定文件内，保存成功，返回 true, 否则返回 false.
     */
    public static boolean saveToFile(String filePath, DoodleData doodleData) {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        try {
            if (doodleData == null) {
                throw new NullPointerException("doodle data is null");
            }
            fos = new FileOutputStream(filePath, false);
            osw = new OutputStreamWriter(fos, Charsets.UTF8);
            bw = new BufferedWriter(osw);

            // write doodle data with writer

            // 固定头 dd
            writeLine("dd", bw);
            // version 1
            writeLine("1", bw);
            // width
            writeLine(doodleData.width, bw);
            // height
            writeLine(doodleData.height, bw);
            // background color
            writeLine(doodleData.backgroundColor, bw);

            // write 数据块
            // write 渲染区数据块
            if (doodleData.drawStepDatas != null) {
                for (DoodleData.DrawStepData dsd : doodleData.drawStepDatas) {
                    // 渲染区标识
                    writeLine("DS", bw);
                    // write 步骤块
                    writeLine(dsd.type, bw);
                    if (dsd.points != null && !dsd.points.isEmpty()) {
                        int size = dsd.points.size();
                        if (size % 2 != 0) {
                            throw new IllegalArgumentException("point size not match");
                        }
                        for (int i = 0; i < size; i++) {
                            writeLine(dsd.points.get(i) + "," + dsd.points.get(i + 1), bw);
                            i++;
                        }
                    }
                    // 步骤块结束
                    writeLine("EOS", bw);
                    // write 笔刷块
                    if (dsd.brushData != null) {
                        writeLine(dsd.brushData.type, bw);
                        writeLine(dsd.brushData.color, bw);
                        writeLine(dsd.brushData.size, bw);
                        writeLine(dsd.brushData.alpha, bw);
                    }
                    // 笔刷块结束
                    writeLine("EOB", bw);
                }
            }
            // write redo 区数据块
            if (doodleData.drawStepDatasRedo != null) {
                for (DoodleData.DrawStepData dsd : doodleData.drawStepDatasRedo) {
                    // redo 区标识
                    writeLine("DSR", bw);
                    // write 步骤块
                    writeLine(dsd.type, bw);
                    if (dsd.points != null && !dsd.points.isEmpty()) {
                        int size = dsd.points.size();
                        if (size % 2 != 0) {
                            throw new IllegalArgumentException("point size not match");
                        }
                        for (int i = 0; i < size; i++) {
                            writeLine(dsd.points.get(i) + "," + dsd.points.get(i + 1), bw);
                            i++;
                        }
                    }
                    // 步骤块结束
                    writeLine("EOS", bw);
                    // write 笔刷块
                    if (dsd.brushData != null) {
                        writeLine(dsd.brushData.type, bw);
                        writeLine(dsd.brushData.color, bw);
                        writeLine(dsd.brushData.size, bw);
                        writeLine(dsd.brushData.alpha, bw);
                    }
                    // 笔刷块结束
                    writeLine("EOB", bw);
                }
            }

            // 文件结束
            writeLine("EOD", bw);

            bw.flush();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeQuietly(bw);
            IOUtil.closeQuietly(osw);
            IOUtil.closeQuietly(fos);
        }
        return false;
    }


    /**
     * 解析指定文件为 DoodleData，解析失败返回 null.
     */
    public static DoodleData readFromFile(String filePath) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(filePath);
            isr = new InputStreamReader(fis, Charsets.UTF8);
            br = new BufferedReader(isr);

            String ddLine = readTrueLine(br);
            if (!"dd".equalsIgnoreCase(ddLine)) {
                throw new IllegalArgumentException("dd line not match for file " + filePath);
            }
            String versionLine = readTrueLine(br);
            int version = Integer.parseInt(versionLine);
            if (version != 1) {
                throw new IllegalArgumentException("dd file version not support " + version);
            }

            DoodleData doodleData = new DoodleData();

            int width = Integer.parseInt(readTrueLine(br));
            int height = Integer.parseInt(readTrueLine(br));
            int backgroundColor = Integer.parseInt(readTrueLine(br));
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("width or height error [" + width + ", " + height + "]");
            }
            doodleData.setSize(width, height);
            doodleData.setBackgroundColor(backgroundColor);

            // read 数据块
            doodleData.drawStepDatas = new ArrayList<>();
            doodleData.drawStepDatasRedo = new ArrayList<>();
            do {
                String flagLine = readTrueLine(br);
                if ("EOD".equalsIgnoreCase(flagLine)) {
                    // 文件已经结束
                    return doodleData;
                } else if ("DS".equalsIgnoreCase(flagLine)) {
                    // 渲染区数据块
                    doodleData.drawStepDatas.add(readDrawStepData(br));
                } else if ("DSR".equalsIgnoreCase(flagLine)) {
                    // redo 区数据块
                    doodleData.drawStepDatasRedo.add(readDrawStepData(br));
                } else {
                    throw new IllegalArgumentException("flag line error " + flagLine);
                }
            } while (true);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeQuietly(br);
            IOUtil.closeQuietly(isr);
            IOUtil.closeQuietly(fis);
        }
        return null;
    }

    private static DoodleData.DrawStepData readDrawStepData(BufferedReader br) throws IOException {
        DoodleData.DrawStepData drawStepData = new DoodleData.DrawStepData();
        drawStepData.points = new ArrayList<>();

        // 读取步骤块
        int drawStepType = Integer.parseInt(readTrueLine(br));
        if (drawStepType == DoodleData.DRAW_STEP_TYPE_EMPTY) {
            // 空步骤
            drawStepData.type = DoodleData.DRAW_STEP_TYPE_EMPTY;
        } else if (drawStepType == DoodleData.DRAW_STEP_TYPE_POINT) {
            drawStepData.type = DoodleData.DRAW_STEP_TYPE_POINT;
        } else if (drawStepType == DoodleData.DRAW_STEP_TYPE_SCRIBBLE) {
            drawStepData.type = DoodleData.DRAW_STEP_TYPE_SCRIBBLE;
        } else {
            throw new IllegalArgumentException("draw step type not support " + drawStepType);
        }
        // 读取该步骤块可能包含的坐标点数据
        do {
            String lineDrawStepPointOrEnd = readTrueLine(br);
            assert lineDrawStepPointOrEnd != null;
            if ("EOS".equalsIgnoreCase(lineDrawStepPointOrEnd)) {
                // 步骤结束
                break;
            } else {
                // 解析为坐标点
                String[] points = lineDrawStepPointOrEnd.split(",");
                drawStepData.points.add(Float.valueOf(points[0]));
                drawStepData.points.add(Float.valueOf(points[1]));
            }
        } while (true);

        // 读取笔刷块
        String lineBrushTypeOrEnd = readTrueLine(br);
        assert lineBrushTypeOrEnd != null;
        if (!"EOB".equalsIgnoreCase(lineBrushTypeOrEnd)) {
            DoodleData.BrushData brushData = new DoodleData.BrushData();
            int brushType = Integer.parseInt(lineBrushTypeOrEnd);
            if (brushType == DoodleData.BRUSH_TYPE_EMPTY) {
                brushData.type = DoodleData.BRUSH_TYPE_EMPTY;
            } else if (brushType == DoodleData.BRUSH_TYPE_PENCIL) {
                brushData.type = DoodleData.BRUSH_TYPE_PENCIL;
            } else if (brushType == DoodleData.BRUSH_TYPE_LEAVES) {
                brushData.type = DoodleData.BRUSH_TYPE_LEAVES;
            } else {
                throw new IllegalArgumentException("brush type not support " + brushType);
            }
            brushData.color = Integer.parseInt(readTrueLine(br));
            brushData.size = Float.parseFloat(readTrueLine(br));
            brushData.alpha = Integer.parseInt(readTrueLine(br));

            // 笔刷块结束标识
            String brushEndFlag = readTrueLine(br);
            if (!"EOB".equalsIgnoreCase(brushEndFlag)) {
                throw new IllegalArgumentException("brush end flag error " + brushEndFlag);
            }
            drawStepData.brushData = brushData;
        }

        return drawStepData;
    }

}