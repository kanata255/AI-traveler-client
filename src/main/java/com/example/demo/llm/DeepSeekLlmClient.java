package com.example.demo.llm;

import com.example.demo.llm.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

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
                .filter(r -> r != null && r.getResults() != null && !r.getResults().isEmpty())
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

    private List<Message> toAiMessages(List<ChatMessage> messages) {
        return messages.stream().map(m -> switch (m.role()) {
            case "system" -> (Message) new SystemMessage(m.content());
            case "assistant" -> (Message) new AssistantMessage(m.content());
            default -> (Message) new UserMessage(m.content());
        }).toList();
    }
}
