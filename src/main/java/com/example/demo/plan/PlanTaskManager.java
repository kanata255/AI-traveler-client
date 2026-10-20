package com.example.demo.plan;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中维护规划任务的取消信号，供 DELETE 接口取消正在进行的生成过程。
 */
@Component
public class PlanTaskManager {

    private final ConcurrentHashMap<String, Sinks.Empty<Void>> tasks = new ConcurrentHashMap<>();

    public void register(String taskId, Sinks.Empty<Void> cancelSignal) {
        tasks.put(taskId, cancelSignal);
    }

    /** 触发取消；任务不存在时返回 false（幂等）。 */
    public boolean cancel(String taskId) {
        Sinks.Empty<Void> signal = tasks.get(taskId);
        if (signal == null) {
            return false;
        }
        signal.tryEmitEmpty();
        return true;
    }

    public void remove(String taskId) {
        tasks.remove(taskId);
    }
}