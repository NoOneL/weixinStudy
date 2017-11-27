package com.ego.core.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Convert object to json string.
 *
 * Json java string	java.lang.String
 *
 * number	java.lang.Number
 *
 * true|false java.lang.Boolean
 *
 * null null
 *
 * array	java.util.List
 *
 * object	java.util.Map
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UtilJson {

    private static int convertDepth = 8;

    public static void setConvertDepth(int convertDepth) {
        if (convertDepth < 2) {
            throw new IllegalArgumentException("convert depth can not less than 2.");
        }
        UtilJson.convertDepth = convertDepth;
    }

    public static String map2Json(Map map, int depth) {
        if (map == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        Iterator it = map.entrySet().iterator();

        sb.append('{');
        while (it.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }

            Map.Entry entry = (Map.Entry) it.next();
            storeKeyValue(String.valueOf(entry.getKey()), entry.getValue(), sb, depth);
        }
        sb.append('}');
        return sb.toString();
    }

    private static String storeKeyValue(String key, Object value, StringBuilder sb, int depth) {
        sb.append('\"');
        if (key == null) {
            sb.append("null");
        } else {
            escape(key, sb);
        }
        sb.append('\"').append(':');

        sb.append(toJson(value, depth));

        return sb.toString();
    }

    public static String listToJson(List list, int depth) {
        if (list == null) {
            return "null";
        }

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        Iterator iter = list.iterator();

        sb.append('[');
        while (iter.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }

            Object value = iter.next();
            if (value == null) {
                sb.append("null");
                continue;
            }
            sb.append(toJson(value, depth));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters
     * (U+0000 through U+001F).
     */
    private static String escape(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        escape(s, sb);
        return sb.toString();
    }

    private static void escape(String s, StringBuilder sb) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
                        String str = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - str.length(); k++) {
                            sb.append('0');
                        }
                        sb.append(str.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        }
    }

    public static String toJson(Object value) {
        return toJson(value, convertDepth);
    }

    public static String toJson(Object value, int depth) {
        if (value == null || (depth--) <= 0) {
            return "null";
        }

        if (value instanceof String) {
            return "\"" + escape((String) value) + "\"";
        }

        if (value instanceof Double) {
            if (((Double) value).isInfinite() || ((Double) value).isNaN()) {
                return "null";
            } else {
                return value.toString();
            }
        }

        if (value instanceof Float) {
            if (((Float) value).isInfinite() || ((Float) value).isNaN()) {
                return "null";
            } else {
                return value.toString();
            }
        }

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof Map) {
            return map2Json((Map) value, depth);
        }

        if (value instanceof List) {
            return listToJson((List) value, depth);
        }

        String result = otherToJson(value, depth);
        if (result != null) {
            return result;
        }

        // 类型无法处理时当作字符串处理,否则ajax调用返回时js无法解析
        // return value.toString();
        return "\"" + escape(value.toString()) + "\"";
    }

    private static String otherToJson(Object value, int depth) {
        if (value instanceof java.util.Date || value instanceof Character) {
            return null;
        }


        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            List list = new ArrayList(arr.length);
            for (int i = 0; i < arr.length; i++) {
                list.add(arr[i]);
            }
            return listToJson(list, depth);
        }
        if (value instanceof Enum) {
            return "\"" + ((Enum) value).name() + "\"";
        }

        return beanToJson(value, depth);
    }

    private static String beanToJson(Object model, int depth) {
        Map map = new HashMap();
        Method[] methods = model.getClass().getMethods();
        for (Method m : methods) {
            String methodName = m.getName();
            int indexOfGet = methodName.indexOf("get");
            if (indexOfGet == 0 && methodName.length() > 3) {	// Only getter
                String attrName = methodName.substring(3);
                if (!attrName.equals("Class")) {				// Ignore Object.getClass()
                    Class<?>[] types = m.getParameterTypes();
                    if (types.length == 0) {
                        try {
                            Object value = m.invoke(model);
                            map.put(UtilString.upperFrist(attrName), value);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                }
            } else {
                int indexOfIs = methodName.indexOf("is");
                if (indexOfIs == 0 && methodName.length() > 2) {
                    String attrName = methodName.substring(2);
                    Class<?>[] types = m.getParameterTypes();
                    if (types.length == 0) {
                        try {
                            Object value = m.invoke(model);
                            map.put(UtilString.upperFrist(attrName), value);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                }
            }
        }
        return map2Json(map, depth);
    }
}