package dev.danvega.workshop.doubao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class DoubaoController {

    private final String apiKey;
    private final String modelName;
    private ArkService service;

    public DoubaoController(
            @Value("${doubao.api.key:}") String apiKey,
            @Value("${doubao.model.name:doubao-1-5-pro-32k-250115}") String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        initializeService();
    }

    private void initializeService() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Doubao API key is required. Set DOUBAO_API_KEY environment variable or doubao.api.key in application.properties");
        }
        
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        
        this.service = ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();
    }

    @GetMapping("/doubao")
    public String chat(@RequestParam(value = "message", defaultValue = "天空为什么是蓝色的？") String message) throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(message)
                .build());

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(messages)
                .build();

        StringBuilder response = new StringBuilder();
        service.createChatCompletion(request)
                .getChoices()
                .forEach(choice -> response.append(choice.getMessage().getContent()));

        return response.toString();
    }

    @GetMapping("/doubao/story")
    public String generateStory(@RequestParam(value = "topic", defaultValue = "中国文化") String topic) throws JsonProcessingException {
        String prompt = "请为我讲述一个关于'" + topic + "'的故事。";
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(prompt)
                .build());

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(messages)
                .build();

        StringBuilder response = new StringBuilder();
        service.createChatCompletion(request)
                .getChoices()
                .forEach(choice -> response.append(choice.getMessage().getContent()));

        return response.toString();
    }

    public void shutdown() {
        if (service != null) {
            service.shutdownExecutor();
        }
    }
}
