package cn.creekmoon.dict;

import java.lang.reflect.Field;

/**
 * 字典翻译器. 支持自定义翻译字典的逻辑   注: 这是在Field级别做的处理.
 *  一个翻译器只会有一个全局实例, 您必须保证对象是线程安全的.
 *
 * @param
 */
public interface DictFieldTranslator {

    /**
     * 获取翻译结果
     * @param dictObject  来源翻译的整个对象
     * @param sourcefield 来源字段本身
     * @param fieldValue  来源字段本身的值
     * @param annotationValue 来源字段携带的注解值
     * @return
     */
    String searchDictValue(Object dictObject, Field sourcefield, Object fieldValue, DictMapping annotationValue);


}
