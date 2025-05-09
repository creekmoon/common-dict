package cn.creekmoon.dict;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@Slf4j
class DictMappingTest {
    public static String token;


    @Test
    void dictTest() throws Exception {

        Dict.setGlobalBusinessPackageNames("cn.creekmoon.dict");

        //加临时字典测试
        Map<String, Map<String, String>> dictMap = new HashMap<>();;
        dictMap.put("testDict", Map.of("1", "value"));
        Dict.setDictMap(dictMap);

        //翻译测试
        MyDictObject sourceObject = new MyDictObject();
        JSONObject dictResult = JSONObject.from(sourceObject.getDict());

        System.out.println("dictResult = " + dictResult);
        //验证结果
        assertEquals("1veryGood", dictResult.get("level"));
        assertEquals("1veryGood", dictResult.getJSONObject("simpleObject").get("level"));
        assertNull(dictResult.getString("simpleObjectNull"));
        assertEquals("value", dictResult.getString("testDict"));
        assertNull(dictResult.getString("stringNull"));
        assertNull(dictResult.getString("longNull"));
        assertEquals("valuelalala", dictResult.getString("long1"));
        assertEquals("2lalala", dictResult.getString("long2"));
        assertEquals("valueveryGood", dictResult.getString("baseInt1"));
        assertNull(dictResult.getString("noExistsMappingAnnotation"));
        assertEquals("123123", dictResult.getString("happy"));
        assertEquals("1", dictResult.getString("bad"));
        assertEquals("value", Dict.searchDictValue("testDict", "1"));


        //验证自填充翻译
        sourceObject.fillSelf();
        assertEquals("1veryGood", sourceObject.getLevel());
        assertEquals("1veryGood", sourceObject.getSimpleObject().getLevel());
        assertNull(sourceObject.getStringNull());
        assertNull(sourceObject.getLongNull());
        assertEquals(1L, sourceObject.getLong1());
        assertEquals(2L, sourceObject.getLong2());
        assertEquals(1, sourceObject.getBaseInt1());
        assertEquals("1", sourceObject.getNoExistsMappingAnnotation());
        assertEquals("1veryGood", sourceObject.getTestCollection().stream().findAny().orElse(null).getLevel());
        assertEquals(2, sourceObject.getBigDecimalList().size());
        assertEquals("123123", sourceObject.getHappy());
        assertEquals("1", sourceObject.getBad());
        //验证自填充翻译, 嵌套的情况
        assertEquals("value", sourceObject.getTestCollection()
                .stream()
                .flatMap(x -> x.getTestCollection().stream())
                .findAny()
                .orElse(null)
                .getLevel());
        assertEquals("1", sourceObject.getNoExistsMappingAnnotation());
        //验证LazyDict.getDict 方法不会获得翻译结果.
        SimpleObject simpleObject = new SimpleObject();

        //验证searchDict方法
        JSONObject lazyResult2 = Dict.getDict(simpleObject);
        assertEquals("1veryGood", lazyResult2.get("level"));
        assertEquals("1", simpleObject.getLevel());

        //验证fillSelf方法
        Dict.fillSelf(simpleObject);
        assertEquals("1veryGood", simpleObject.getLevel());
    }


    @Data
    public static class MyDictObject implements Dict {


        @DictMapping(suffix = "veryGood")
        String level = "1";


        @DictMapping
        String testDict = "1";

        // 不加字典值, 应该不翻译
        String noExistsMappingAnnotation = "1";


        @DictMapping(dictCode = "testDict")
        String stringNull = null;

        @DictMapping(dictCode = "testDict")
        Long longNull = null;

        // 如果有这个字典KEY, 则会使用字典值加后缀
        @DictMapping(dictCode = "testDict", suffix = "lalala")
        Long long1 = 1L;

        // 如果没有这个字典KEY, 则会使用原值并加后缀
        @DictMapping(dictCode = "testDict", suffix = "lalala")
        Long long2 = 2L;

        //基本数据类型的翻译
        @DictMapping(dictCode = "testDict", suffix = "veryGood")
        int baseInt1 = 1;

        @DictMapping(suffix = "123", fieldTranslator = HappyTranslator.class)
        String happy = "happy";

        @DictMapping(fieldTranslator = HappyTranslator.class)
        String bad = "1";

        SimpleObject simpleObject = new SimpleObject();

        SimpleObject simpleObjectNull = null;

        //集合形式
        List<SimpleObject> testCollection = List.of(new SimpleObject());

        //集合形式
        List<BigDecimal> bigDecimalList = List.of(BigDecimal.ZERO, BigDecimal.ONE);
    }


    @Data
    public static class SimpleObject implements Dict {

        @DictMapping(suffix = "veryGood")
        String level = "1";

        //集合形式
        List<EasyObject> testCollection = List.of(new EasyObject());
    }


    @Data
    public static class EasyObject implements Dict{

        @DictMapping(dictCode = "testDict")
        String level = "1";
    }


    /**
     * 测试翻译器类型, 自己实现一个翻译器
     *   1.如果填写了后缀, 则忽略所有的值, 只会输出双倍后缀.
     *   2.如果没有填写后缀, 则忽略所有的值, 只会输出本身.
     *
     */
    public static class HappyTranslator implements DictFieldTranslator {
        @Override
        public String searchDictValue(Object dictObject, Field sourcefield, Object fieldValue, DictMapping annotationValue) {
            return StrUtil.isNotBlank(annotationValue.suffix()) ? annotationValue.suffix() + annotationValue.suffix() : fieldValue.toString();
        }
    }
}