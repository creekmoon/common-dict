package cn.creekmoon.dict;


import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class DictHttpRefreshConfigTest {

    // 请将此 URL 替换为你实际可用且稳定返回 JSON 的地址
    // 期望的响应 JSON 结构：{"testDict":{"1":"value"}}
    private static final String TEST_URL = "http://127.0.0.1:19093/open-resource/all-dict-direct";

    @Test
    void testEnableAndRefreshWithStableContent() throws Exception {
        // 前置清理，复位全局静态状态，避免跨用例污染
        try {
            System.out.println("[DictHttpRefreshConfigTest] 开始测试，TEST_URL=" + TEST_URL + ", 初始 lastMD5=" + DictHttpRefreshConfig.lastMD5);
            ScheduledExecutorService pool = DictHttpRefreshConfig.scheduledThreadPool;
            if (pool != null) {
                pool.shutdownNow();
            }
            DictHttpRefreshConfig.scheduledThreadPool = null;
            DictHttpRefreshConfig.lastMD5 = "md5";
            System.out.println("[DictHttpRefreshConfigTest] 已重置静态状态：scheduledThreadPool=null, lastMD5=md5");

            // 启用定时刷新（1 秒周期）
            System.out.println("[DictHttpRefreshConfigTest] 调用 enable(url, 1s) 启动定时刷新...");
            DictHttpRefreshConfig.enable(TEST_URL, 1L);

            // 轮询等待首次刷新完成（最多 5 秒）
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (System.nanoTime() < deadline) {
                if (DictHttpRefreshConfig.scheduledThreadPool != null
                        && !DictHttpRefreshConfig.scheduledThreadPool.isShutdown()
                        && !"md5".equals(DictHttpRefreshConfig.lastMD5)) {
                    break;
                }
                Thread.sleep(100);
            }
            if ("md5".equals(DictHttpRefreshConfig.lastMD5)) {
                System.err.println("[DictHttpRefreshConfigTest] 等待超时：lastMD5 仍为初始值，可能是 URL 不可达或返回数据不合法");
            } else {
                System.out.println("[DictHttpRefreshConfigTest] 首次刷新完成：lastMD5=" + DictHttpRefreshConfig.lastMD5);
            }
            boolean created = DictHttpRefreshConfig.scheduledThreadPool != null;
            boolean shutdown = created && DictHttpRefreshConfig.scheduledThreadPool.isShutdown();
            boolean terminated = created && DictHttpRefreshConfig.scheduledThreadPool.isTerminated();
            System.out.println("[DictHttpRefreshConfigTest] 线程池状态：created=" + created + ", shutdown=" + shutdown + ", terminated=" + terminated);

            assertNotNull(DictHttpRefreshConfig.scheduledThreadPool, "scheduledThreadPool 应被创建");
            assertFalse(DictHttpRefreshConfig.scheduledThreadPool.isShutdown(), "scheduledThreadPool 不应为关闭状态");
            assertNotEquals("md5", DictHttpRefreshConfig.lastMD5, "首次刷新应更新 lastMD5（且仅在解析成功后才更新）");

            String firstMd5 = DictHttpRefreshConfig.lastMD5;
            System.out.println("[DictHttpRefreshConfigTest] 记录 firstMd5=" + firstMd5);

            // 再等待 1-2 个周期，验证内容未变化时 MD5 保持不变
            System.out.println("[DictHttpRefreshConfigTest] 等待 1.8s 观察第二个周期...");
            Thread.sleep(1800);
            String secondMd5 = DictHttpRefreshConfig.lastMD5;
            System.out.println("[DictHttpRefreshConfigTest] secondMd5=" + secondMd5 + "（应与 firstMd5 一致）");
            assertEquals(firstMd5, secondMd5, "内容未变化时应跳过刷新（MD5 保持不变）");

            // 可选：打印当前字典信息（格式化展示，仅用于观察，不做断言）
            try {
                var allDict = Dict.getAll();
                int dictTypeCount = allDict.size();
                StringBuilder sb = new StringBuilder();
                sb.append("[DictHttpRefreshConfigTest] 当前字典内容（类型数=").append(dictTypeCount).append(")\n");
                if (dictTypeCount == 0) {
                    sb.append("  <空>\n");
                } else {
                    allDict.keySet().stream().sorted().forEach(code -> {
                        var inner = allDict.get(code);
                        int size = inner == null ? 0 : inner.size();
                        sb.append("  - ").append(code).append(" (size=").append(size).append(")\n");
                        if (size > 0) {
                            inner.entrySet().stream()
                                    .sorted(java.util.Map.Entry.comparingByKey())
                                    .forEach(e -> sb.append("      ")
                                            .append(e.getKey())
                                            .append(" -> ")
                                            .append(e.getValue())
                                            .append("\n"));
                        }
                    });
                }
                System.out.print(sb.toString());
            } catch (Throwable ignore) {
                // 避免环境差异导致非关键路径报错
            }
            System.out.println("[DictHttpRefreshConfigTest] 测试通过：首次刷新成功且内容未变时跳过刷新逻辑");
        } finally {
            // 结束清理，关闭线程池并复位静态字段
            try {
                if (DictHttpRefreshConfig.scheduledThreadPool != null) {
                    System.out.println("[DictHttpRefreshConfigTest] 正在关闭线程池并复位静态字段...");
                    DictHttpRefreshConfig.scheduledThreadPool.shutdownNow();
                }
            } finally {
                DictHttpRefreshConfig.scheduledThreadPool = null;
                DictHttpRefreshConfig.lastMD5 = "md5";
                System.out.println("[DictHttpRefreshConfigTest] 清理完成：scheduledThreadPool=null, lastMD5=md5");
            }
        }
    }
}


