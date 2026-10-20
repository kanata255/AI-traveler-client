markdown
# 任务设计书：Spring Boot 4.0 流式对话全栈实现（DeepSeek）

> **文档用途**：供 Claude Code 直接执行。
> **目标**：在 Spring Boot 4.0 项目中，以模块化方式封装 DeepSeek 大模型调用，提供 SSE 流式对话接口，前端用 `fetch` + `ReadableStream` 接收逐字输出。
> **技术栈**：Spring Boot 4.0、Spring AI 2.0（`spring-ai-starter-model-deepseek`）、DeepSeek API、原生 fetch SSE。
> **核心约束**：大模型调用必须独立封装为 `llm` 模块，业务层只依赖接口，禁止直接引用 Spring AI 类。

---

## 目录

1. [总体架构与模块划分](#一总体架构与模块划分)
2. [依赖配置](#二依赖配置)
3. [大模型封装模块llm-包](#三大模型封装模块llm-包)
4. [后端业务接口设计](#四后端业务接口设计)
5. [前端实现](#五前端实现)
6. [兜底方案spring-ai-starter-不可用时](#六兜底方案spring-ai-starter-不可用时)
7. [验收清单](#七验收清单)
8. [实现顺序建议](#八实现顺序建议)

---

## 一、总体架构与模块划分

com.example.chat
├── config/ # 配置类
│ ├── LlmProperties.java # 自定义 llm.* 配置绑定
│ └── WebConfig.java # 跨域配置
├── llm/ # 大模型调用模块（独立封装，不依赖业务）
│ ├── LlmClient.java # 接口：统一调用入口
│ ├── DeepSeekLlmClient.java # 实现：基于 Spring AI
│ ├── LlmException.java # 统一异常类型
│ └── model/
│ ├── ChatMessage.java # 内部消息模型
│ └── ChatRequest.java # 内部请求模型（供业务层复用）
├── chat/ # 业务模块（只依赖 LlmClient 接口）
│ ├── ChatController.java # SSE 接口
│ ├── ChatService.java # 业务编排
│ └── dto/
│ └── ChatReqDto.java # 前端请求体
└── common/
└── SseUtil.java # SSE 格式化工具

**关键约束**：

- `chat` 包**只允许**依赖 `LlmClient` 接口，**禁止**直接 `import org.springframework.ai.*`。
- 将来换模型厂商，只需改 `llm` 包，业务代码零改动。
- 验证命令：`grep -r "springframework.ai" chat/` 结果必须为空。

---

## 二、依赖配置

### 2.1 `pom.xml` 新增依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-deepseek</artifactId>
</dependency>
```
版本由 Spring Boot 4.0 的 BOM 管理。若需显式指定，使用 Spring AI 2.0.x。若解析失败，见第六章兜底方案。

### 2.2 application.yml
yaml
spring:
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}          # 通过环境变量注入，禁止硬编码
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-v4-pro
          temperature: 0.7

server:
  port: 8080

llm:
  timeout-seconds: 120
安全要求：API Key 只能通过环境变量注入，仓库内不得出现明文密钥。

### 2.3 启动类
java
@SpringBootApplication
@EnableConfigurationProperties(LlmProperties.class)
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
## 三、大模型封装模块（llm 包）
### 3.1 LlmProperties.java
java
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private int timeoutSeconds = 120;
    // getter / setter
}
### 3.2 ChatMessage.java
java
public record ChatMessage(String role, String content) {
    public static ChatMessage user(String content)    { return new ChatMessage("user", content); }
    public static ChatMessage system(String content)  { return new ChatMessage("system", content); }
    public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }
}
### 3.3 LlmException.java
java
public class LlmException extends RuntimeException {
    public LlmException(String message, Throwable cause) { super(message, cause); }
    public LlmException(String message) { super(message); }
}
### 3.4 LlmClient.java（核心接口）
java
public interface LlmClient {
    /**
     * 流式对话：返回文本增量流，每个元素是一段新生成的文本（可能为空串，调用方可忽略）
     */
    Flux<String> streamChat(List<ChatMessage> messages);

    /**
     * 阻塞式对话：返回完整文本
     */
    String chat(List<ChatMessage> messages);
}
### 3.5 DeepSeekLlmClient.java
实现要点：

构造注入 DeepSeekChatModel。

将内部 ChatMessage 转换为 Spring AI 的 UserMessage / SystemMessage / AssistantMessage。

streamChat 调用 chatModel.stream(new Prompt(aiMessages))，用 .map(resp -> resp.getResult().getOutput().getText()) 提取文本增量，过滤 null 与空串。

chat 调用 chatModel.call(...) 并取 getResult().getOutput().getText()。

异常包装：捕获 Spring AI 异常，统一抛 LlmException，避免上层感知具体实现。

日志：记录请求消息条数、耗时、是否流式；不打印完整 prompt 内容（防泄露）。

java
@Service
public class DeepSeekLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekLlmClient.class);

    private final DeepSeekChatModel chatModel;

    public DeepSeekLlmClient(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Flux<String> streamChat(List<ChatMessage> messages) {
        long start = System.currentTimeMillis();
        Prompt prompt = new Prompt(toAiMessages(messages));
        log.info("LLM stream start, msgCount={}", messages.size());

        return chatModel.stream(prompt)
                .map(r -> r.getResult().getOutput().getText())
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .doOnComplete(() ->
                        log.info("LLM stream done, cost={}ms", System.currentTimeMillis() - start))
                .onErrorMap(e -> new LlmException("流式调用失败", e));
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        long start = System.currentTimeMillis();
        Prompt prompt = new Prompt(toAiMessages(messages));
        try {
            ChatResponse resp = chatModel.call(prompt);
            String text = resp.getResult().getOutput().getText();
            log.info("LLM call done, cost={}ms", System.currentTimeMillis() - start);
            return text;
        } catch (Exception e) {
            throw new LlmException("阻塞调用失败", e);
        }
    }

    private List<org.springframework.ai.chat.messages.Message> toAiMessages(List<ChatMessage> messages) {
        return messages.stream().map(m -> switch (m.role()) {
            case "system"    -> (org.springframework.ai.chat.messages.Message)
                                new SystemMessage(m.content());
            case "assistant" -> new AssistantMessage(m.content());
            default          -> new UserMessage(m.content());
        }).toList();
    }
}
包路径以 Spring AI 2.0 实际 API 为准，如类名有调整（如 AssistantMessage 位置），按编译报错修正。

## 四、后端业务接口设计
### 4.1 接口契约
POST /api/chat/stream

请求体（JSON）：

json
{
  "sessionId": "uuid-可选",
  "message": "用户输入内容"
}
响应：Content-Type: text/event-stream

正常事件格式（每个事件以空行结束）：

text
data: {"delta":"你好"}

data: {"delta":"，很高兴"}

data: {"delta":"见到你"}

data: [DONE]
错误事件（流内推送，HTTP 状态仍为 200）：

text
data: {"error":"模型调用失败，请重试"}
### 4.2 ChatReqDto.java
java
public record ChatReqDto(
        String sessionId,
        @NotBlank(message = "message 不能为空") String message
) {}
### 4.3 SseUtil.java
java
public final class SseUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SseUtil() {}

    public static String json(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new LlmException("JSON 序列化失败", e);
        }
    }
}
### 4.4 ChatService.java
java
@Service
public class ChatService {

    private final LlmClient llmClient;

    public ChatService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public Flux<String> stream(String userMessage) {
        List<ChatMessage> messages = List.of(
                ChatMessage.system("你是一个简洁友好的助手。"),
                ChatMessage.user(userMessage)
        );
        return llmClient.streamChat(messages);
    }
}
本任务不做多轮会话持久化，sessionId 字段预留但暂不使用。

### 4.5 ChatController.java
java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody @Valid ChatReqDto req) {
        return chatService.stream(req.message())
                .map(delta -> ServerSentEvent.<String>builder()
                        .data(SseUtil.json(Map.of("delta", delta)))
                        .build())
                .concatWithValues(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build())
                .onErrorResume(e -> {
                    log.warn("chat stream error", e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .data(SseUtil.json(Map.of("error", "模型调用失败，请重试")))
                            .build());
                });
    }
}
用 ServerSentEvent 而非手拼字符串，避免换行/编码问题。响应头会自动带 text/event-stream。

### 4.6 WebConfig.java（前后端分离时必需）
java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}