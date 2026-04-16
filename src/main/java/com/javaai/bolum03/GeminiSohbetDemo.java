package com.javaai.bolum03;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile("gemini")
@EnableAutoConfiguration
@RestController
@RequestMapping("/api/b32")
public class GeminiSohbetDemo {

    private static final Logger log = LoggerFactory.getLogger(GeminiSohbetDemo.class);
    private final ChatClient chatClient;

    public  GeminiSohbetDemo(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("Sen yardimci bir asistansin. Acik ve anlasilir yanitler ver. Turkce yanit ver.")
                .build();
    }

    public record ChatRequest(String message){
        public ChatRequest{
            if(message == null || message.isEmpty()){
                throw new IllegalArgumentException("Mesaj bos olamaz");
            }
        }
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest chatRequest){
        return chatClient.prompt().user(chatRequest.message()).call().content();
    }

    @PostMapping("/chat/context")
    public String chatWithContext(@RequestBody Map<String, String> request){
        String context = request.getOrDefault("context", "");
        String message = request.getOrDefault("message", "");

        return chatClient.prompt()
                .system("Aşagidaki baglam bilgisini kullanarak soruyu yanitla\n\n" + "BAGLAM:\n" + context)
                .user(message)
                .call()
                .content();
    }

    @PostMapping("chat/options")
    public Map<String, Object> chatWithOptions
            (@RequestBody ChatRequest request, @RequestParam(defaultValue = "0.7") double temperature, @RequestParam(defaultValue = "1000") int maxTokens){

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .temperature(temperature)
                .maxOutputTokens(maxTokens)
                .build();
        String response = chatClient.prompt()
                .user(request.message())
                .options(options)
                .call()
                .content();

        return Map.of(
                "response:", response,
                "temparature:", temperature,
                "maxTokens:", maxTokens
        );
    }

    @ExceptionHandler(Exception.class)
    public Map<String, String> handleError(Exception ex){
        String message = ex.getMessage() != null ? ex.getMessage().replaceAll("sk-[a-zA-Z0-9]{20,}", "sk-****")
                : "Bilinmeyen Hata";
        log.error("Hata: {}", message);
        return Map.of("error", message);
    }

    public static void main(String[] args) {
        SpringApplication.run(GeminiSohbetDemo.class, args);
    }

}
