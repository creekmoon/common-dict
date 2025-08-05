package cn.creekmoon.dict;


import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 默认字段翻译器
 * <p>
 * 拿到注解的值, 然后去找字典缓存进行翻译.
 * 支持单值和多值翻译：
 * - 单值：直接翻译
 * - 多值：检测到逗号分隔的值时，对每个值进行翻译后重新拼接
 */
public class DefaultDictFieldTranslator implements DictFieldTranslator {


    @Override
    public String searchDictValue(Object dictObject, Field sourcefield, Object fieldValue, DictMapping annotationValue) {

        // 如果字段值为空，直接返回null
        if (fieldValue == null) {
            return null;
        }
        
        // 转换为字符串
        String fieldValueStr = fieldValue.toString();
        
        // 如果字符串为空，直接返回空字符串加后缀
        if (StrUtil.isBlank(fieldValueStr)) {
            return fieldValueStr;
        }

        //如果定义了后缀
        String suffix = annotationValue.suffix();

        //找dictCode
        String dictCode = annotationValue.dictCode().equals(DictMapping.AUTO_DICT_CODE)
                ? sourcefield.getName()
                : annotationValue.dictCode();

        // 检测是否为多值（包含逗号）
        if (fieldValueStr.contains(",")) {
            // 按逗号分隔字段值
            String[] values = fieldValueStr.split(",");
            
            // 对每个值进行字典翻译，并添加后缀
            String translatedResult = Arrays.stream(values)
                    .map(String::trim)  // 去除空格
                    .map(value -> Dict.searchDictValueOrSelf(dictCode, value))  // 翻译每个值，如果没找到则返回原值
                    .map(translatedValue -> translatedValue + suffix)  // 给每个翻译后的值添加后缀
                    .collect(Collectors.joining(","));  // 用逗号重新拼接
            
            return translatedResult;
        } else {
            // 单值翻译
            //根据dictCode  dictKey 找到 dictValue
            String result = Dict.searchDictValueOrSelf(dictCode, fieldValue);
            return result == null ? null : result + suffix;
        }

    }


}
