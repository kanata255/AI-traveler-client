package com.example.demo.chat;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.demo.chat.dto.ChatReqDto;
import com.example.demo.common.SseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@Tag(name = "流式对话", description = "DeepSeek 流式对话接口")
@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "流式对话", description = "SSE 逐字返回模型输出")
    @SaCheckLogin
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
