改造任务：为行程规划接口增加多轮连续对话能力
后端改动（真的很少）
1. CreatePlanTaskRequest 加两个可选字段

java
public record CreatePlanTaskRequest(
// ...原有字段不变...
String previousPlan,   // 上一版完整 trip JSON（首轮为 null）
String modifyRequest   // 用户的修改语句（首轮为 null）
) {}
或者更通用：把"上一版 + 修改语句"直接塞进一个 List<ChatMessage> history 字段，后端原样拼进 messages。但 previousPlan + modifyRequest 更简单、更安全（见下文"注入防护"）。

2. PlanService 只改一处：拼 user prompt 时带上上一版

java
private String buildUserPrompt(CreatePlanTaskRequest r) {
// 首轮：原有逻辑
if (r.previousPlan() == null || r.previousPlan().isBlank()) {
return buildFirstRoundPrompt(r);   // 原 buildUserPrompt 内容
}
// 后续轮：上一版 + 修改要求
return """
这是当前的行程计划 JSON：
%s

            请在**上述计划的基础上**做如下修改，未提及的部分保持不变，直接输出修改后的完整 JSON：
            %s
            """.formatted(r.previousPlan(), r.modifyRequest());
}
3. SYSTEM_PROMPT 不用改（因为修改逻辑已经在 user prompt 里说清楚了）。

4. Controller 完全不用动。

真的就这些。PlanService.stream() 的参数从"首轮请求"变成"当前请求"，逻辑不变。