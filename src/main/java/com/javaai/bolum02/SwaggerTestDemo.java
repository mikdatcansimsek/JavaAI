package com.javaai.bolum02;

import com.javaai.JavaAIApplication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import org.stringtemplate.v4.ST;

import java.util.Map;

@EnableAutoConfiguration
@RestController
@RequestMapping("/api/b27")
@Tag(name="B2.7 - Swagger UI Demo", description = "REST API test araclari")


public class SwaggerTestDemo {


    private final ChatClient chatClient;

    public SwaggerTestDemo(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("Sen yardimci bir asistansin. Kisa ve oz yanitlar ver. Turkce yanit ver.")
                .build();
    }

    @GetMapping("/merhaba")
    @Operation(
            summary = "Selamlama (GET)",
            description = "Tarayicidan dogrudan test edilebilir."
    )
    public String merhaba(@RequestParam(defaultValue = "Dunya") String isim){
        return "Merhaba " + isim + "! Swagger UI'a hoşgeldiniz.";
    }

    @PostMapping("/hesapla")
    @Operation(
            summary = "hesapla",
            description = "Tarayicidan test edilemez."
    )
    public Map<String, Object> hesapla(@RequestBody HesaplamaRequest request){
        int a = request.sayi1();
        int b = request.sayi2();

        return Map.of(
                "toplam", a+b,
                "fark", a-b,
                "carpim", a*b,
                "bolum", a/b
        );
    }

    @PostMapping("/chat")
    @Operation(
            summary = "AI Sohbet",
            description = "ChatClient ile LLM'e mesaj gonderin."
    )
    public Map<String, String> chat(@RequestBody ChatRequest request){
        String yanit = chatClient
                .prompt()
                .user(request.message())
                .call()
                .content();
        return Map.of(
                "soru", request.message(),
                "yanit", yanit
        );
    }


    public record HesaplamaRequest(int sayi1, int sayi2){

    }

    public record ChatRequest(String message){

    }

    public static void main(String[] args) {
        SpringApplication.run(SwaggerTestDemo.class, args);
    }
}
