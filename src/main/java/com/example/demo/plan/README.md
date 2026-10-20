前端 POST /v1/plan/tasks
│
▼
PlanController.create(req)                 ← 刚加了日志
│  planService.stream(req)
▼
PlanService.stream(req)                    ← 外层 Flux<ServerSentEvent<String>>                                                                                                                                             
│  构造 system prompt + user prompt
│  llmClient.streamChat(messages)                                                                                                                                                                                                         
▼                                                                                                                                                                                                                        
DeepSeekLlmClient.streamChat(messages)     ← 内层 Flux<String>
│  chatModel.stream(prompt)
▼
DeepSeekChatModel (Spring AI 实际调 LLM)

两个 Flux 的关系：

- DeepSeekLlmClient.streamChat 返回的是 Flux<String>，即 LLM 吐出的一个个 token 增量文本（最底层原始数据流）。
- PlanService.stream 返回的是 Flux<ServerSentEvent<String>>，它在 :84 处用 .map(delta -> ...) 把内层那个 Flux<String> 转换成 event: token 的 SSE 事件流。

具体代码在 PlanService.java:80-95：

Flux<ServerSentEvent<String>> body = llmClient.streamChat(...)   // 内层 Flux<String>
.map(delta -> {                                           // 转成 SSE 事件
fullText.append(delta);
return ServerSentEvent.<String>builder()
.event("token")
│  planService.stream(req)
▼
│  llmClient.streamChat(messages)
▼
│  chatModel.stream(prompt)
DeepSeekChatModel (Spring AI 实际调 LLM)

- DeepSeekLlmClient.streamChat 返回的是 Flux<String>，即 LLM 吐出的一个个 token 增量文本（最底层原始数据流）。

                    .event("token")
                    .data(SseUtil.json(Map.of("text", delta)))
                    .build();
        })
        .concatWith(...finalEvent...)   // 追加 final 事件
        .startWith(...meta事件...);     // 前置 meta 事件

所以 PlanService 里的 Flux 是外层包装，DeepSeekLlmClient 里的 Flux 是内层数据源，后者被前者 map/组合成最终的 SSE 流返回给前端。

另外 llmClient 是 LlmClient 接口类型（PlanService.java:66），Spring 注入的是唯一实现类 DeepSeekLlmClient（@Service，DeepSeekLlmClient.java:20）。