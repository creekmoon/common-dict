package cn.creekmoon.dict;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class DictHttpRefreshConfig {

    // 记录上一次的MD5, 不一致则刷新字典
    public static String lastMD5 = "md5";

    // 创建ScheduledThreadPool，参数为线程池的大小
    public static ScheduledExecutorService scheduledThreadPool = null;


    /**
     * 定时通过HTTP刷新字典
     *
     * @param url             仅支持GET类型的地址,获取字典数据  预期数据为<Map<String, Map<String, String>>>
     * @param refreshInterval 刷新间隔,单位秒
     */
    public static void enable(String url, Long refreshInterval) {
        if (scheduledThreadPool != null) {
            scheduledThreadPool.shutdownNow();
        }
        if (scheduledThreadPool == null) {
            scheduledThreadPool = Executors.newScheduledThreadPool(1);
        }
        scheduledThreadPool.scheduleWithFixedDelay(() -> refresh(url), 0, refreshInterval, TimeUnit.SECONDS);
    }

    private static void refresh(String url) {
        HttpRequest httpRequest = HttpRequest.get(url);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            String body = httpResponse.body();
            String currentMD5 = DigestUtil.md5Hex(body);
            if (lastMD5.equals(currentMD5)) {
                log.info("[定时刷新字典]======字典定时检查与远端一致无须更新====");
                return;
            }
            JSONObject jsonObject = JSONObject.parseObject(body);
            Map<String, Map<String, String>> dict = jsonObject.toJavaObject(Map.class);
            Dict.addDictMap(dict);
            lastMD5 = currentMD5;
            log.info("[定时刷新字典]======字典定时更新成功====");
        } catch (Exception e) {
            log.error("[定时刷新字典]=======字典更新失败====", e);
        }
    }


}
