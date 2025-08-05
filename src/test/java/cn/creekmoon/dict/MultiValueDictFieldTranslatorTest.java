package cn.creekmoon.dict;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
class MultiValueDictFieldTranslatorTest {

    @Test
    void multiValueDictTest() {
        
        // 设置业务包路径
        Dict.setGlobalBusinessPackageNames("cn.creekmoon.dict");
        
        // 配置多值字典数据
        Map<String, Map<String, String>> dictMap = new HashMap<>();
        dictMap.put("taskStatus", Map.of(
            "1", "未开始",
            "2", "进行中", 
            "3", "已完成"
        ));
        dictMap.put("priority", Map.of(
            "HIGH", "高优先级",
            "MEDIUM", "中优先级",
            "LOW", "低优先级"
        ));
        Dict.setDictMap(dictMap);
        
        // 测试对象
        MultiValueTestObject testObject = new MultiValueTestObject();
        
        // 获取翻译结果
        JSONObject dictResult = testObject.getDict();
        System.out.println("翻译结果: " + dictResult);
        
        // 验证完全匹配的情况：1,2,3 -> 未开始,进行中,已完成
        assertEquals("未开始,进行中,已完成", dictResult.getString("taskStatus"));
        
        // 验证部分匹配的情况：1,2,3,4 -> 未开始,进行中,已完成,4
        assertEquals("未开始,进行中,已完成,4", dictResult.getString("mixedStatus"));
        
        // 验证带后缀的情况：HIGH,LOW -> 高优先级任务,低优先级任务
        assertEquals("高优先级任务,低优先级任务", dictResult.getString("priorityWithSuffix"));
        
        // 验证没有匹配的情况：X,Y,Z -> X,Y,Z
        assertEquals("X,Y,Z", dictResult.getString("noMatchStatus"));
        
        // 验证空值情况
        assertNull(dictResult.getString("nullStatus"));
        
        // 验证空字符串情况
        assertEquals("状态", dictResult.getString("emptyStatus"));
        
        // 验证单个值的情况：2 -> 进行中
        assertEquals("进行中", dictResult.getString("singleStatus"));
        
        // 测试 fillSelf 方法
        testObject.fillSelf();
        assertEquals("未开始,进行中,已完成", testObject.getTaskStatus());
        assertEquals("未开始,进行中,已完成,4", testObject.getMixedStatus());
        assertEquals("高优先级任务,低优先级任务", testObject.getPriorityWithSuffix());
        
        log.info("多值字典翻译器测试通过！");
    }
    
    @Data
    public static class MultiValueTestObject implements Dict {
        
        // 完全匹配的多值字段：1,2,3 -> 未开始,进行中,已完成
        @DictMapping
        private String taskStatus = "1,2,3";
        
        // 部分匹配的多值字段：1,2,3,4 -> 未开始,进行中,已完成,4
        @DictMapping(dictCode = "taskStatus")
        private String mixedStatus = "1,2,3,4";
        
        // 带后缀的多值字段：HIGH,LOW -> 高优先级任务,低优先级任务
        @DictMapping(dictCode = "priority", suffix = "任务")
        private String priorityWithSuffix = "HIGH,LOW";
        
        // 没有匹配的多值字段：X,Y,Z -> X,Y,Z
        @DictMapping(dictCode = "taskStatus")
        private String noMatchStatus = "X,Y,Z";
        
        // 空值测试
        @DictMapping
        private String nullStatus = null;
        
        // 空字符串测试
        @DictMapping(suffix = "状态")
        private String emptyStatus = "";
        
        // 单个值测试：2 -> 进行中
        @DictMapping(dictCode = "taskStatus")
        private String singleStatus = "2";
    }
}