package cn.wanghaomiao.seimi.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JsonUtils 类
 * 描述: FastJson工具类
 * 作者: Longlive
 * 时间: 2017年01月23日 下午 2:16
 */
public class JsonUtils {

    private static final SerializeConfig config;
    private static String dateFormat;

    static {
        config = new SerializeConfig();
        dateFormat = "yyyy-MM-dd HH:mm:ss";
        config.put(Date.class, new SimpleDateFormatSerializer(dateFormat));
        config.put(java.sql.Date.class, new SimpleDateFormatSerializer(dateFormat));
    }

    private static final SerializerFeature[] features = {SerializerFeature.WriteMapNullValue, // 输出空置字段
            SerializerFeature.WriteNullListAsEmpty, // list字段如果为null，输出为[]，而不是null
//            SerializerFeature.WriteNullNumberAsZero, // 数值字段如果为null，输出为0，而不是null
            SerializerFeature.WriteNullBooleanAsFalse, // Boolean字段如果为null，输出为false，而不是null
            SerializerFeature.WriteNullStringAsEmpty // 字符类型字段如果为null，输出为""，而不是null
    };

    public static String toJson(Object object) {
        return JSONObject.toJSONString(object, config, features);
    }

    public static String toJson(Object object, PropertyFilter pFilter) {
        return JSONObject.toJSONString(object, config, pFilter, features);
    }

    /**
     * 用fastjson 将json字符串解析为一个 JavaBean
     * @param jsonString
     * @param cls
     * @return
     */
    public static <T> T getBean(String jsonString, Class<T> cls) {
        T t = null;
        try {
            t = JSON.parseObject(jsonString, cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /**
     * 用fastjson 将json字符串 解析成为一个 List<JavaBean> 及 List<String>
     * @param jsonString
     * @param cls
     * @return
     */
    public static <T> List<T> getBeans(String jsonString, Class<T> cls) {
        List<T> list = new ArrayList<T>();
        try {
            list = JSON.parseArray(jsonString, cls);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return list;
    }

    /**
     * 用fastjson 将jsonString 解析成 List<Map<String,Object>>
     * @param jsonString
     * @return
     */
    public static List<Map<String, Object>> getListMap(String jsonString) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        try {
            // 两种写法
            // list = JSON.parseObject(jsonString, new
            // TypeReference<List<Map<String, Object>>>(){}.getType());
            list = JSON.parseObject(jsonString, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            // TODO: handle exception
        }
        return list;
    }
}
