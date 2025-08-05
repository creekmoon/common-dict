package cn.creekmoon.dict;

import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 多值字典翻译器
 * <p>
 * 将多值的内容，以逗号分隔的字典值进行翻译。
 * 例如："1,2,3" 翻译为 "未开始,进行中,已完成"
 * 如果某个值没有命中字典，则使用其本身的值。
 * 例如："1,2,3,4" 翻译为 "未开始,进行中,已完成,4"
 */
public class MultiValueDictFieldTranslator implements DictFieldTranslator {

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
            return annotationValue.suffix();
        }
        
        // 获取字典类型编码
        String dictCode = annotationValue.dictCode().equals(DictMapping.AUTO_DICT_CODE)
                ? sourcefield.getName()
                : annotationValue.dictCode();
        
        // 按逗号分隔字段值
        String[] values = fieldValueStr.split(",");
        
        // 获取后缀
        String suffix = annotationValue.suffix();
        
        // 对每个值进行字典翻译，并添加后缀
        String translatedResult = Arrays.stream(values)
                .map(String::trim)  // 去除空格
                .map(value -> Dict.searchDictValueOrSelf(dictCode, value))  // 翻译每个值，如果没找到则返回原值
                .map(translatedValue -> translatedValue + suffix)  // 给每个翻译后的值添加后缀
                .collect(Collectors.joining(","));  // 用逗号重新拼接
        
        return translatedResult;
    }
}