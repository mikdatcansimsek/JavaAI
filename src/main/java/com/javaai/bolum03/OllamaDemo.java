package com.javaai.bolum03;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Profile("ollama")
@EnableAutoConfiguration
@RestController
@RequestMapping("/api/b33")
public class OllamaDemo {

    private final ChatClient chatClient;
    private static final Logger log = LoggerFactory.getLogger(OllamaDemo.class);

    public OllamaDemo(ChatClient.Builder clientBuilder) {
        this.chatClient = clientBuilder
                .defaultSystem("Sen yardimci bir asistansin. Acik ve net yanitlar ver. Turkce yanit ver.")
                .build();
    }

    @GetMapping("/chat")
    public Map<String, Object> generalChat(@RequestParam String question) {
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model("llama3.2")
                .temperature(0.7)
                .numPredict(2048)
                .build();
        long start = System.currentTimeMillis();
        String response = chatClient.prompt()
                .user(question)
                .options(options)
                .call()
                .content();
        long duration =  System.currentTimeMillis() - start;

        return Map.of("response" ,response , "model", "llama3.2", "duration", duration);
    }

    @PostMapping("/code")
    public Map<String, Object> generateCode(@RequestBody Map<String, String> request) {
        String codeRequest = request.getOrDefault("request","Hello World Java");
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model("deepseek-r1:8b")
                .temperature(0.1)
                .numPredict(4096)
                .numCtx(8192)
                .build();

        long start = System.currentTimeMillis();
        String response = chatClient.prompt()
                .system("Sen deneyimli bir yazilim gelistiricisin. Temiz, okunabilir, best practice'lere uygun kod yaz.")
                .user(codeRequest)
                .options(options)
                .call()
                .content();
        long duration =  System.currentTimeMillis() - start;

        return Map.of("code" ,response , "model", "deepseek-r1:8b", "duration", duration);
    }

    public static void main(String[] args) {
        SpringApplication.run(OllamaDemo.class, "--spring.profiles.active=ollama");
    }

}
