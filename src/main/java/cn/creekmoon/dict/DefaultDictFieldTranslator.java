package cn.creekmoon.dict;


import cn.hutool.core.util.NumberUtil;

import java.lang.reflect.Field;

/**
 * 默认字段翻译器
 * <p>
 * 拿到注解的值, 然后去找字典缓存进行翻译.
 */
public class DefaultDictFieldTranslator implements DictFieldTranslator {


    @Override
    public String searchDictValue(Object dictObject, Field sourcefield, Object fieldValue, DictMapping annotationValue) {

        //如果定义了后缀
        String suffix = annotationValue.suffix();

        //找dictCode
        String dictCode = annotationValue.dictCode().equals(DictMapping.AUTO_DICT_CODE)
                ? sourcefield.getName()
                : annotationValue.dictCode();

        //根据dictCode  dictKey 找到 dictValue
        String result = Dict.searchDictValueOrSelf(dictCode, fieldValue);
        return result == null ? null : result + suffix;

    }


}
