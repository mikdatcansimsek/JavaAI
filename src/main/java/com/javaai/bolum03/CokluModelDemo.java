package com.javaai.bolum03;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile("multimodel")
@EnableAutoConfiguration
@RestController
@RequestMapping("/api/b36")
public class CokluModelDemo {
    // Fallback -- Yedek sağlayıcıya otomatik geçiş
    // Retry -- Geçici hatalarda tekrar deneme
    // Health Check -- Sağlayıcı durumunu izleme

    private static final Logger log = LoggerFactory.getLogger(CokluModelDemo.class);
    private final ChatClient googleAIClient;
    private final ChatClient ollamaAIClient;

    public CokluModelDemo(@Autowired(required = false)GoogleGenAiChatModel googleGenAiModel, @Autowired(required = false) OllamaChatModel ollamaAIModel) {
        // Gemini modeli varsa client oluştur yoksa null bırak
        this.googleAIClient = googleGenAiModel != null
                ? ChatClient.builder(googleGenAiModel).defaultSystem("Sen yardimci bir asistansin. Turkce yanit ver.").build()
                : null;
        // Ollma modeli varsa client oluştur yoksa null bırak
        this.ollamaAIClient = ollamaAIModel != null
                ? ChatClient.builder(ollamaAIModel).defaultSystem("Sen yardimci bir asistansin. Turkce yanit ver.").build()
                : null;

        // Uygulama Başlarken hangi sağlayıcıların aktif oldugunu loglayalım.
        log.info("Coklu Model basladi -- GoogleGenaI: {} ve Ollama: {}",
                googleAIClient != null ? "AKTIF" : "YOK",
                ollamaAIClient != null ? "AKTIF" : "YOK");
    }

    // Fallback mekanizması

    @PostMapping("/chat")
    public Map<String, Object> fallBackChat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        // 1. Adım GOOGLEAI
        if(googleAIClient != null){
            try {
               log.info("GoogleAI deneniyor. ");
               String response = googleAIClient.prompt().user(message).call().content();
                log.info("GoogleAI başarılı. ");
                return Map.of("response", response, "provider", "googleAI");
            }
            catch(Exception e){
                log.warn("GoogleAI hatasi. Ollama'ya geçiliyor. ");
            }
        }

        // 2. Adım OLLAMA
        if(ollamaAIClient != null){
            try {
                log.info("Ollama deneniyor. ");
                String response = ollamaAIClient.prompt().user(message).call().content();
                log.info("Ollma başarılı. ");
                return Map.of("response", response, "provider", "ollama");
            }
            catch(Exception e){
                log.error("Ollama da hatalı.");
            }
        }
        return Map.of("error","Tum AI saglayicilari kullanilamiyor.");
    }

    // RETRY mekanizmasi

    @PostMapping("/chat/retry")
    public Map<String, Object> retryChat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        ChatClient client = googleAIClient != null ? googleAIClient : ollamaAIClient;
        if (client == null) return Map.of("error","Hic AI saglayicisi yapilandirilmamis");

        for (int attempt = 1 ; attempt <= 3 ; attempt++) {
            try {
                log.info("Deneme {} / 3 ...", attempt);
                String response = client.prompt().user(message).call().content();
                return Map.of("response", response, "attempt", attempt,"provide", googleAIClient != null ? "googleAI" : "ollama");
            }
            catch(Exception e) {
                log.warn("Deneme {} / 3 başarisiz", attempt);
                if (attempt < 3) {
                    try {
                        Thread.sleep(2000L * attempt);
                    }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return Map.of("error", "3 deneme de başarisiz. Lutfen birkaç dakika sonra tekrar deneyiniz.");
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "googleai", googleAIClient != null,
                "ollama", ollamaAIClient != null,
                "anyAvailable" , googleAIClient != null || ollamaAIClient != null
                );
    }

    // Hata Yönetimi
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleError(Exception e) {
        log.error(e.getMessage());
        return Map.of("error", e.getMessage() != null ? e.getMessage() : "Bilinmeyen hata");
    }

    public static void main(String[] args) {
        SpringApplication.run(CokluModelDemo.class, "--spring.profiles.active=multimodel");
    }
}
