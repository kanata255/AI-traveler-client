package com.example.demo.llm;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.demo.llm.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DeepSeekLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekLlmClient.class);

    private final WebClient deepSeekWebClient;
    private final String apiKey;
    private final String model;

    public DeepSeekLlmClient(@Qualifier("deepSeekWebClient") WebClient deepSeekWebClient,
                             @Value("${spring.ai.deepseek.api-key}") String apiKey,
                             @Value("${spring.ai.deepseek.chat.model:deepseek-v4-pro}") String model) {
        this.deepSeekWebClient = deepSeekWebClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Flux<String> streamChat(List<ChatMessage> messages) {
        long start = System.currentTimeMillis();
        log.info("LLM stream start, msgCount={}", messages.size());

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", true);
        body.put("messages", toApiMessages(messages));

        return deepSeekWebClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .doOnNext(data -> log.info("LLM raw sse: {}", data))
                .takeWhile(data -> !"[DONE]".equals(data))
                .concatMap(data -> {
                    try {
                        JSONObject root = JSONUtil.parseObj(data);
                        JSONObject delta = root.getJSONArray("choices").getJSONObject(0).getJSONObject("delta");
                        String content = delta.getStr("content");
                        String reasoning = delta.getStr("reasoning_content");
                        if (reasoning != null && !reasoning.isEmpty()) {
                            log.info("LLM reasoning delta: {}", reasoning);
                        }
                        if (content == null || content.isEmpty()) {
                            return Flux.empty();
                        }
                        return Flux.just(content);
                    } catch (Exception e) {
                        log.warn("parse sse payload failed: {}", data, e);
                        return Flux.empty();
                    }
                })
                .doOnComplete(() -> log.info("LLM stream done, cost={}ms", System.currentTimeMillis() - start))
                .onErrorMap(e -> {
                    Throwable root = rootCause(e);
                    log.error("LLM stream failed, msgCount={}, cause={}: {}",
                            messages.size(), root.getClass().getSimpleName(), root.getMessage());
                    return new LlmException("流式调用失败: " + root.getClass().getSimpleName()
                            + " - " + root.getMessage(), e);
                });
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        long start = System.currentTimeMillis();
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("messages", toApiMessages(messages));

        try {
            String resp = deepSeekWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JSONObject root = JSONUtil.parseObj(resp);
            String text = root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getStr("content");
            log.info("LLM call done, cost={}ms", System.currentTimeMillis() - start);
            return text == null ? "" : text;
        } catch (Exception e) {
            throw new LlmException("阻塞调用失败", e);
        }
    }

    private List<Map<String, String>> toApiMessages(List<ChatMessage> messages) {
        List<Map<String, String>> list = new ArrayList<>();
        for (ChatMessage m : messages) {
            Map<String, String> item = new HashMap<>();
            item.put("role", Objects.requireNonNullElse(m.role(), "user"));
            item.put("content", Objects.requireNonNullElse(m.content(), ""));
            list.add(item);
        }
        return list;
    }

    private static Throwable rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}