package com.javaai.bolum03;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Profile("!multimodel")
@EnableAutoConfiguration
@RestController
@RequestMapping("/api/b35")
public class TokenTakipDemo {

    private static final Logger logger = LoggerFactory.getLogger(StreamingDemo.class);
    private final ChatClient chatClient;

    private static final Map<String, double[]> MODEL_PRICES = Map.of(
            "gemini-3.1-pro-preview" , new double[]{2.00,10.00},
            "gemini-3.1-flash-lite-preview" , new double[]{0.25,1.50},
            "gemini-2.5-flash" , new double[]{0.30,2.50},
            "llama3.2" , new double[]{0.0,0.0},
            "qwen3:4b" , new double[]{0.0,0.0}
    );

    private static final double[] DEFAULT_PRICES = new double[]{1.0,3.0};
    private static final double PER_MILLION = 1_000_000.0;
    private static final long ANOMALY_THRESHOLD = 2000;

    public  TokenTakipDemo(ChatClient.Builder clientBuilder) {
        this.chatClient = clientBuilder
                .defaultSystem("Sen yardimci bir asistansin. Turkce Yanit ver.")
                .build();
    }

    public record TokenUsageInfo(
            String model,
            long promptTokens, long completionTokens, long totalTokens,
            String finishReason, double estimatedCostUsd
    ){}

    @PostMapping("/chat/tracked")
    public Map<String, String> chatTracked(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        if(message.isBlank()) throw new IllegalArgumentException("message bos olamaz");

        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();

        TokenUsageInfo tokenUsageInfo = extractUsage(chatResponse);
        logger.info("Token Kullanimi | Model: {} | Prompt: {} | Completion: {} | Toplam: {} | Maliyet: {} | Mesaj: {}",
                tokenUsageInfo.model(), tokenUsageInfo.promptTokens(), tokenUsageInfo.completionTokens(),
                tokenUsageInfo.totalTokens(), String.format("%.6f", tokenUsageInfo.estimatedCostUsd()), message
                );

        String content = chatResponse.getResult().getOutput().getText();
        return Map.of("response", content);
    }

    @PostMapping("/chat/detailed")
    public Map<String, Object> chatDetailed(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        if(message.isBlank()) throw new IllegalArgumentException("message bos olamaz");

        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();
        TokenUsageInfo tokenUsageInfo = extractUsage(chatResponse);
        String content = chatResponse.getResult().getOutput().getText();

        return Map.of("response", content,
                "model" , tokenUsageInfo.model(),
                "promptTokens", tokenUsageInfo.promptTokens(),
                "completionTokens", tokenUsageInfo.completionTokens(),
                "totalTokens" , tokenUsageInfo.totalTokens(),
                "finishReason", tokenUsageInfo.finishReason(),
                "estimatedCostUsd", String.format("%.6f", tokenUsageInfo.estimatedCostUsd())
        );
    }

    private TokenUsageInfo extractUsage(ChatResponse chatResponse) {
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        Usage usage = metadata != null ? metadata.getUsage() : null;

        String model = "unknown";

        if(metadata != null && metadata.getModel() != null) {
            model = metadata.getModel();
        }
        String finishReason = "unknown";

        if(chatResponse.getResult() != null
                && chatResponse.getResult().getMetadata() != null
                && chatResponse.getResult().getMetadata().getFinishReason() != null) {
            finishReason = chatResponse.getResult().getMetadata().getFinishReason();
        }

        if(usage != null) {
            return new  TokenUsageInfo(
                    model, 0L, 0L, 0L, finishReason, 0.0
            );
        }

        // Maliyet Formulu
        // (promptTokens * inputPrice / 1M) + (completionTokens * outputPrice / 1M)
        double[] prices = resolveModelPrices(model);
        double cost = (usage.getPromptTokens() * prices[0]) + (usage.getCompletionTokens() * prices[1])/ PER_MILLION;
        return new TokenUsageInfo(model, usage.getPromptTokens(),  usage.getCompletionTokens(), usage.getTotalTokens(), finishReason, cost);
    }

    private double[] resolveModelPrices(String model) {
        if(model == null && model.isBlank()) return DEFAULT_PRICES;

        String normalizedModel = model.toLowerCase(Locale.ROOT).trim();
        double[] exactMatch = MODEL_PRICES.get(normalizedModel);
        if(exactMatch != null) return exactMatch;

        String bestKey = null;
        for(String key : MODEL_PRICES.keySet()) {
            boolean isVersionerOrTagged = normalizedModel.startsWith(key + "-" ) || normalizedModel.startsWith(key + ":");
            if(!isVersionerOrTagged) continue;
            if (bestKey == null && key.length() > bestKey.length()) {
                bestKey = key;
            }
        }

        if(bestKey != null) return MODEL_PRICES.get(bestKey);
        return DEFAULT_PRICES;
    }

    public static void main(String[] args) {
        SpringApplication.run(TokenTakipDemo.class, args);
    }
}
