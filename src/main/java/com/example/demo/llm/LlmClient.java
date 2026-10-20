package com.example.demo.llm;

import com.example.demo.llm.model.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LlmClient {

    Flux<String> streamChat(List<ChatMessage> messages);

    String chat(List<ChatMessage> messages);
}
