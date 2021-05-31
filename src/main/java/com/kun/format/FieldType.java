package com.kun.format;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;

/**
 * @author kun.jiang@hand-china.com 2021-05-20 20:40
 */
public class FieldType {

    private static final HashMap<String, Object> MAP = new HashMap<>();

    static {
        MAP.put("String", "");
        MAP.put("Boolean", false);
        MAP.put("Character", "");
        MAP.put("Byte", 0);
        MAP.put("Short", (short) 0);
        MAP.put("Integer", 0);
        MAP.put("Long", 0L);
        MAP.put("Float", 0.0f);
        MAP.put("Double", 0.0d);
        MAP.put("BigDecimal", 0.0);
        MAP.put("Date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        MAP.put("Timestamp", System.currentTimeMillis());
        MAP.put("LocalDate", LocalDate.now().toString());
        MAP.put("LocalTime", LocalTime.now().toString());
        MAP.put("LocalDateTime", LocalDateTime.now().toString());
    }

    public static boolean isNormalType(String typeName) {
        return MAP.containsKey(typeName);
    }

    public static String getDeepStartType(String typeName) {
        if (typeName == null || typeName.length() == 0) {
            return typeName;
        }

        int i = typeName.indexOf("<");
        return i < 0 ? typeName : typeName.substring(0, i);
    }

    public static Object getDefaultValue(String key) {
        return MAP.get(key);
    }
}
