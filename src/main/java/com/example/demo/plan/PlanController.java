package com.example.demo.plan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.demo.common.Result;
import com.example.demo.plan.dto.CreatePlanTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "行程规划", description = "AI 生成旅行计划（SSE 流式）")
@RestController
@RequestMapping("/v1/plan/tasks")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final PlanService planService;
    private final PlanTaskManager taskManager;

    public PlanController(PlanService planService, PlanTaskManager taskManager) {
        this.planService = planService;
        this.taskManager = taskManager;
    }

    @Operation(summary = "创建规划任务（SSE 流式）", description = "直接以 SSE 流返回：meta → token* → final")
    @SaCheckLogin
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> create(@RequestBody @Valid CreatePlanTaskRequest req) {
        log.info("收到规划请求: {}", req);
        return planService.stream(req);
    }

    @Operation(summary = "取消规划任务", description = "终止生成过程，停止大模型调用")
    @SaCheckLogin
    @DeleteMapping("/{taskId}")
    public Result<Void> cancel(@PathVariable String taskId) {
        taskManager.cancel(taskId);
        return Result.success();
    }
}