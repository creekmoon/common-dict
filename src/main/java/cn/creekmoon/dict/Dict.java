package cn.creekmoon.dict;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public interface Dict {
    Logger log = LoggerFactory.getLogger(Dict.class);
    /*缓存的业务包名*/
    static List<String> businessPackagePathList = new ArrayList<>();

    Map<Class, DictFieldTranslator> translators = new ConcurrentHashMap<>();


    public static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();
    public static final Map<String, List<String>> EMPTY_MAP_REVERSE = new HashMap<String, List<String>>();



    /**DictMappingTest
     * 缓存字典
     * DictMapping注解会通过这个集合, 翻译对应的字典值
     *
     *  k1=字典类型code  k2=字典key  v=字典值
     */
    static Map<String, Map<String, String>> DICT_MAP = new ConcurrentHashMap<>(512);

    /**
     * 缓存字典
     * 用字典值获取字典key
     *
     *  k1=字典类型code  k2=字典值  v=字典key
     */
    static Map<String, Map<String, List<String>>> DICT_MAP_REVERSE = new ConcurrentHashMap<>(512);


    /**
     * 获取字典翻译
     * <p>
     * dict : {
     * "transportType": "空运",
     * "crenelType": "装卸"
     * }
     *
     * @return
     */
    default JSONObject getDict() {
        return getDict(this);
    }



    /**
     * 设置字典值
     * @param dictMap
     */
    public static void setDictMap(Map<String, Map<String, String>> dictMap) {
        DICT_MAP.putAll(dictMap);
        DICT_MAP.forEach((k,v)->{
            DICT_MAP_REVERSE.put(k, v.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList()))));
        });
    }

    public static void setGlobalBusinessPackageNames(String... basePackageNames) {
        businessPackagePathList.addAll(Arrays.asList(basePackageNames));
    }


    /**
     * 获取字典值
     *
     * @param object 当前对象
     * @return
     */
    public static JSONObject getDict(Object object) {
        JSONObject result = new JSONObject();
        if (!isTranslatable(object)) {
            return null;
        }
        try {
            Field[] fields = ReflectUtil.getFields(object.getClass());
            for (Field field : fields) {
                // 如果是集合类型
                if (object instanceof Collection collection) {
                    result.put(field.getName(), collection.stream().map(Dict::getDict).collect(Collectors.toList()));
                    continue;
                }
                // 如果是业务对象, 则递归进入
                if (isBusinessObjectType(field.getType())) {
                    result.put(field.getName(), getDict(ReflectUtil.getFieldValue(object, field)));
                    continue;
                }
                //如果没有DictMapping注解 则跳过
                if (!field.isAnnotationPresent(DictMapping.class)) {
                    continue;
                }
                // 如果已经翻译成功KEY,则跳过
                if (result.get(field.getName()) != null) {
                    continue;
                }
                result.put(field.getName(), searchDictValue(object, field));
            }
            return result;
        } catch (Exception e) {
            log.error("翻译字典失败！", e);
            return new JSONObject();
        }
    }


    default void validate(Map<String, String> dict) {

    }

    /**
     * 字典自填充, 填充到原始字段上
     *
     * @return
     */
    default void fillSelf() {
        fillSelf(this);
    }

    /**
     * 字典自填充, 填充到原始字段上
     *
     * @return
     */
    public static void fillSelf(Object object) {
        if (!isTranslatable(object)) {
            return;
        }
        if (object instanceof Collection collection) {
            collection.forEach(Dict::fillSelf);
            return;
        }
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 如果是集合类型, 则递归进入
            if (Collection.class.isAssignableFrom(field.getType())) {
                fillSelf(ReflectUtil.getFieldValue(object, field));
                continue;
            }
            // 如果是业务对象类型, 则递归进入
            if (isBusinessObjectType(field.getType())) {
                fillSelf(ReflectUtil.getFieldValue(object, field));
                continue;
            }
            // 跳过没有加入DictMapping注解的字段
            if (!field.isAnnotationPresent(DictMapping.class)) {
                continue;
            }
            //如果field不是String类型,则跳过
            if (!field.getType().equals(String.class)) {
                continue;
            }
            ReflectUtil.setFieldValue(object, field.getName(), searchDictValue(object, field));
        }
    }


    public static String findPackagePath(Class<?> clazz) {
        int num = 2;
        int target = clazz.getName().indexOf(".");
        while (num > 0) {
            target = clazz.getName().indexOf(".", target + 1);
            num--;
        }
        return clazz.getName().substring(0, target);
    }


    /**
     * 是否为业务对象类型
     *
     * @param clazz
     * @return
     */
    public static boolean isBusinessObjectType(Class<?> clazz) {

        for (String basePath : businessPackagePathList) {
            if (clazz.getName().startsWith(basePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是可以进行递归翻译的对象
     *
     * @param clazz
     * @return
     */
    public static boolean isTranslatable(Class<?> clazz) {
        return Dict.class.isAssignableFrom(clazz)
                || Collection.class.isAssignableFrom(clazz)
                || isBusinessObjectType(clazz);

    }

    /**
     * 此对象是否能进行翻译?
     *
     * @param
     * @return
     */
    public static boolean isTranslatable(Object o) {
        if (o == null) {
            return false;
        }
        return Dict.class.isAssignableFrom(o.getClass())
                || Collection.class.isAssignableFrom(o.getClass())
                || isBusinessObjectType(o.getClass());

    }

    /**
     * 获取字典值
     *
     * @param sourceObject 字典对象
     * @param fieldName  字段名
     * @param
     * @return
     */
    static  String searchDictValue(Object sourceObject, String fieldName) {
        Field field = ReflectUtil.getField(sourceObject.getClass(), fieldName);
        if (field == null) {
            log.error("翻译字典失败！dictObject={}, fieldName={}", sourceObject, fieldName, new RuntimeException("翻译字典失败！字段不存在！"));
            return null;
        }
        return searchDictValue(sourceObject, field);
    }

    /**
     * 获取字典值
     *
     * @param dictCode 字典类型编码
     * @param dictKey  字典key
     * @return
     */
    public static String searchDictValue(String dictCode, String dictKey) {
        if (dictKey == null) {
            return null;
        }
        return DICT_MAP.getOrDefault(dictCode, EMPTY_MAP).get(dictKey);
    }


    /**
     * 获取字典值  如果没有找到字典值,则返回原值
     *
     * @param dictCode 字典类型编码
     * @param dictKey  字典key
     * @return
     */
    public static String searchDictValueOrSelf(String dictCode, Object dictKey) {
        String result = searchDictValue(dictCode, String.valueOf(dictKey));
        if (dictKey == null) {
            return null;
        }
        if (result == null) {
            return StrUtil.toString(dictKey);
        }
        return result;
    }

    /**
     * 翻译字典值
     *
     * @param dictObject 字典对象
     * @param field
     * @param <T>
     * @return
     */
    public static <T> String searchDictValue(T dictObject, Field field) {

        Field[] sourceFields = ReflectUtil.getFields(dictObject.getClass());
        for (Field sourcefield : sourceFields) {
            try {
                if (!sourcefield.getName().equals(field.getName())) {
                    continue;
                }
                if (!field.isAnnotationPresent(DictMapping.class)) {
                    continue;
                }
                field.setAccessible(true);
                Object fieldValue = field.get(dictObject);
                field.setAccessible(false);

                // 尝试对byte类型进行校验,如果校验出异常值, 直接返回异常值
                DictMapping annotation = field.getAnnotation(DictMapping.class);
                if (!translators.containsKey(annotation.fieldTranslator())) {
                    try {
                        translators.put(annotation.fieldTranslator(), (DictFieldTranslator) annotation.fieldTranslator().getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        RuntimeException runtimeException = new RuntimeException("字典解析失败! 无法获取到翻译器");
                        log.error("字典解析失败! 无法实例化到字段翻译器! 检查已经实现了DictFieldTranslator接口? field=[{}] annotation=[{}]", field, annotation, runtimeException);
                        throw runtimeException;
                    }
                }

                //尝试查找字典值
                return translators.get(annotation.fieldTranslator()).searchDictValue(dictObject, sourcefield, fieldValue, annotation);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * 获取字典项值（key）
     *
     * @param dictCode  字典类型编码
     * @param dictValue 字典名称（value）
     * @return
     */
    public static String searchDictKey(String dictCode, String dictValue) throws RuntimeException {
        if (dictValue == null) {
            return null;
        }
        List<String> dictKeys = DICT_MAP_REVERSE.getOrDefault(dictCode, EMPTY_MAP_REVERSE).get(dictValue);
        if (dictKeys.size() > 1) {
            throw new RuntimeException(StrFormatter.format("字典项：{}，获取的结果不唯一，请检查数据", dictValue));
        }
        return dictKeys.get(0);
    }


    /**
     * 获取字典类型编码关联的所有字典值
     *
     * @param dictCode 字典类型编码
     * @return
     */
    public static Map<String, String> getKeysMap(String dictCode) {
        if (StrUtil.isBlank(dictCode)) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(DICT_MAP.get(dictCode));
    }

    /**
     * 获取所有字典值
     *
     * @return
     */
    public static Map<String, Map<String, String>> getAll() {
        return Collections.unmodifiableMap(DICT_MAP);
    }
}
