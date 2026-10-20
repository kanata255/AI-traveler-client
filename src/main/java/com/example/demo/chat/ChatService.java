package com.example.demo.chat;

import com.example.demo.llm.LlmClient;
import com.example.demo.llm.model.ChatMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

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
