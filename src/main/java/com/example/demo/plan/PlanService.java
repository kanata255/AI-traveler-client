package com.example.demo.plan;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.demo.common.SseUtil;
import com.example.demo.llm.LlmClient;
import com.example.demo.llm.model.ChatMessage;
import com.example.demo.plan.dto.CreatePlanTaskRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    private static final Map<String, String> PACE_LABEL = Map.of(
            "relaxed", "轻松",
            "moderate", "适中",
            "packed", "紧凑"
    );

    private static final String SYSTEM_PROMPT = """
            你是一名专业的旅行规划师。请根据用户需求生成一份结构化的旅行计划。

            要求：
            1. 只输出 JSON，不要输出任何解释、客套话或 Markdown 代码块标记。
            2. JSON 结构必须严格符合以下字段（字段名精确匹配）：
            {
              "title": "行程标题",
              "destination": "目的地",
              "startDate": "YYYY-MM-DD",
              "endDate": "YYYY-MM-DD",
              "summary": "行程概述",
              "budget": { "total": 0, "currency": "CNY", "breakdown": { "交通": 0, "住宿": 0, "餐饮": 0, "门票": 0 } },
              "days": [
                {
                  "date": "YYYY-MM-DD",
                  "theme": "当日主题",
                  "items": [
                    {
                      "startTime": "09:00", "endTime": "11:00", "type": "spot",
                      "name": "景点名", "poiId": "", "address": "", "lat": 0.0, "lng": 0.0,
                      "description": "", "tips": "", "cost": 0,
                      "transportToNext": { "mode": "步行", "duration": 15, "distance": "1.2km", "cost": 0 }
                    }
                  ]
                }
              ],
              "warnings": [],
              "sources": []
            }
            3. type 只能取 spot / food / hotel / transport / activity 之一。
            4. 按天数生成每天的具体安排，考虑景点开放时间、交通与用餐。
            5. budget.total 为该币种下的总预算估算，breakdown 按类别拆分。
            """;

    private final LlmClient llmClient;
    private final PlanTaskManager taskManager;

    public PlanService(LlmClient llmClient, PlanTaskManager taskManager) {
        this.llmClient = llmClient;
        this.taskManager = taskManager;
    }

    public Flux<ServerSentEvent<String>> stream(CreatePlanTaskRequest req) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder fullText = new StringBuilder();
        Sinks.Empty<Void> cancelSignal = Sinks.empty();
        taskManager.register(taskId, cancelSignal);

        Flux<ServerSentEvent<String>> body = llmClient.streamChat(List.of(
                        ChatMessage.system(SYSTEM_PROMPT),
                        ChatMessage.user(buildUserPrompt(req))
                ))
                .map(delta -> {
                    fullText.append(delta);
                    return ServerSentEvent.<String>builder()
                            .event("token")
                            .data(SseUtil.json(Map.of("text", delta)))
                            .build();
                })
                .concatWith(Flux.defer(() -> Flux.just(finalEvent(taskId, fullText.toString()))))
                .startWith(ServerSentEvent.<String>builder()
                        .event("meta")
                        .data(SseUtil.json(Map.of("taskId", taskId)))
                        .build());

        return body
                .takeUntilOther(cancelSignal.asMono())
                .onErrorResume(e -> {
                    log.warn("plan stream error, taskId={}", taskId, e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(SseUtil.json(Map.of("code", "PLAN_ERROR", "message", "生成失败，请重试")))
                            .build());
                })
                .doFinally(sig -> taskManager.remove(taskId));
    }

    private ServerSentEvent<String> finalEvent(String taskId, String text) {
        try {
            JSONObject trip = JSONUtil.parseObj(extractJson(text));
            JSONObject payload = new JSONObject();
            payload.set("tripId", taskId);
            payload.set("trip", trip);
            return ServerSentEvent.<String>builder()
                    .event("final")
                    .data(payload.toString())
                    .build();
        } catch (Exception e) {
            log.warn("parse trip json failed, taskId={}, text={}", taskId, text);
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data(SseUtil.json(Map.of("code", "PLAN_PARSE_ERROR", "message", "行程结果解析失败")))
                    .build();
        }
    }

    private String extractJson(String text) {
        String t = text == null ? "" : text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "");
            t = t.replaceFirst("\\s*```$", "");
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return t;
    }

    private String buildUserPrompt(CreatePlanTaskRequest r) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为我规划一次旅行：\n");
        sb.append("目的地：").append(r.destination()).append("\n");
        if (r.origin() != null && !r.origin().isBlank()) {
            sb.append("出发地：").append(r.origin()).append("\n");
        }
        sb.append("日期：").append(r.startDate()).append(" 至 ").append(r.endDate()).append("\n");
        sb.append("人数：").append(r.travelers()).append("人\n");
        if (r.budget() != null) {
            sb.append("预算：").append(r.budget()).append(" ").append(r.currency()).append("\n");
        }
        if (r.pace() != null && !r.pace().isBlank()) {
            sb.append("旅行节奏：").append(PACE_LABEL.getOrDefault(r.pace(), r.pace())).append("\n");
        }
        if (r.interests() != null && !r.interests().isEmpty()) {
            sb.append("兴趣偏好：").append(String.join("、", r.interests())).append("\n");
        }
        if (r.accommodation() != null && !r.accommodation().isBlank()) {
            sb.append("住宿偏好：").append(r.accommodation()).append("\n");
        }
        if (r.transport() != null && !r.transport().isBlank()) {
            sb.append("交通方式：").append(r.transport()).append("\n");
        }
        if (r.dietary() != null && !r.dietary().isEmpty()) {
            sb.append("饮食禁忌：").append(String.join("、", r.dietary())).append("\n");
        }
        if (r.mustVisit() != null && !r.mustVisit().isBlank()) {
            sb.append("必去地点：").append(r.mustVisit()).append("\n");
        }
        if (r.avoid() != null && !r.avoid().isBlank()) {
            sb.append("不想去：").append(r.avoid()).append("\n");
        }
        if (r.extraNote() != null && !r.extraNote().isBlank()) {
            sb.append("补充说明：").append(r.extraNote()).append("\n");
        }
        return sb.toString();
    }
}