package com.idonans.doodle.editor;

import android.support.annotation.NonNull;

import com.idonans.acommon.lang.Charsets;
import com.idonans.acommon.util.IOUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

/**
 * doodle data editor, read dd file version etc.
 * Created by pengji on 16-6-22.
 */
public class DoodleDataEditor {

    /**
     * 分析并返回指定 dd 文件的版本，如果分析出错返回 -1
     */
    public static int getVersion(String filePath) {
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
            return Integer.parseInt(versionLine);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeQuietly(br);
            IOUtil.closeQuietly(isr);
            IOUtil.closeQuietly(fis);
        }
        return -1;
    }

    /**
     * 读取一行数据，排除注释行，空行，和行两端的空白字符
     */
    public static String readTrueLine(@NonNull BufferedReader br) throws IOException {
        do {
            String line = br.readLine();
            if (line == null) {
                // 已经读至文件末尾
                return null;
            }

            line = line.trim();
            if (line.startsWith("#")) {
                // 跳过注释行
                continue;
            }
            if (line.isEmpty()) {
                // 跳过空行
                continue;
            }
            return line;
        } while (true);
    }

    /**
     * 写入一行数据(写入时会在末尾追加换行符)
     */
    public static void writeLine(@NonNull Object object, @NonNull Writer writer) throws IOException {
        writer.write(object.toString());
        writer.write("\n");
    }

}
