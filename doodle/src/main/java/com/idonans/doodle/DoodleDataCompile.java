package com.idonans.doodle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.idonans.acommon.lang.CommonLog;

import java.lang.reflect.Type;

/**
 * doodle data 数据编译
 * Created by pengji on 16-6-20.
 */
public class DoodleDataCompile {

    private static final String TAG = "DoodleDataCompile";

    /**
     * 将 json 格式的数据转换为 DoodleData 对象，如果版本不支持，或者转换失败, 返回 null.
     */
    public static DoodleData valueOf(String json) {
        if (json == null) {
            return null;
        }
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<PreCompile>(){}.getType();
            PreCompile preCompile = gson.fromJson(json, type);
            if (DoodleData.isVersionSupport(preCompile.version)) {
                Type typeDoodleData = new TypeToken<DoodleData>(){}.getType();
                return gson.fromJson(json, typeDoodleData);
            } else {
                CommonLog.d(TAG + ", not support version " + preCompile.version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 提取 doodle view 中的内容，转换为 DoodleData 对象， 如果转换失败，返回 null.
     */
    public static DoodleData valueOf(DoodleView doodleView) {
        if (doodleView == null) {
            return null;
        }

        // TODO
        return null;
    }

    /**
     * 将 DoodleData 对象序列化为 json ，如果失败，返回 null.
     */
    public static String toJson(DoodleData doodleData) {
        if (doodleData == null) {
            return null;
        }
        try {
            Gson gson = new Gson();
            Type typeDoodleData = new TypeToken<DoodleData>(){}.getType();
            return gson.toJson(doodleData, typeDoodleData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class PreCompile {
        public int version = -1;
    }

}
