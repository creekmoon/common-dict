package cn.creekmoon.dict;

import java.lang.annotation.*;

/**
 * Created by jiangyue on 2023-11-24 18:13:06
 * <p>
 * <p>
 * 字典解析流程   翻译字典 ---> 添加后缀
 *
 * @author JY
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DictMapping {

    public static final String AUTO_DICT_CODE = "AUTO";

    /**
     * 字典类型编码 如果为空，则使用字段名
     *
     * @return
     */
    String dictCode() default AUTO_DICT_CODE;

    /**
     * 固定后缀
     *
     * @return
     */
    String suffix() default "";


    Class<?> fieldTranslator() default DefaultDictFieldTranslator.class;

}
