package com.javaai.bolum04;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile("!multimodel")
@EnableAutoConfiguration
@RestController
@RequestMapping("/api/b41")
public class PromptTemplateDemo {
    private static final Logger  logger = LoggerFactory.getLogger(PromptTemplateDemo.class);
    private static final String DYNAMIC_TEMPLATE = """
            {konu} hakkında {dil} dilinde {uzunluk} paragraflik bir aciklama yaz.""";

    // System katmanı "nasıl konuş" kuralını tanımlar. user katmanı ise "neyi yap" görevini verir.
    private static final String SYSTEM_TEMPLATE = """
            Sen bir {rol} uzmanisin. Yanitlarini {format} formatinda ver. Dil: Turkce.""";
    private static final String USER_TEMPLATE = """
            Asagidaki soruyu yanitla:
            {soru}
            Cevapta en az 3 madda olsun.""";
    // System : Politika // rol, ton, format
    // User: Görev // Soru

    private final ChatClient chatClient;
    public  PromptTemplateDemo(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("Sen bir bilgisayar mühendisisin net kisa ve aciklayici ol.")
                .build();
    }

    // Temel Kullanım

    @GetMapping("/dynamic")
    public Map<String, Object> dynamicPrompt(
            @RequestParam(required = false) String konu,
            @RequestParam(required = false) String dil,
            @RequestParam(required = false) String uzunluk
    ) {
        String effectiveKonu = konu == null ? "Prompt Mühendisligi" : konu;
        String effectiveDil = dil == null ? "Turkce" : dil;
        String effectiveUzunluk = uzunluk == null ? "2" : uzunluk;

        validateText(effectiveKonu, "konu");
        validateText(effectiveDil, "dil");
        validateText(effectiveUzunluk, "uzunluk");

        PromptTemplate template = new PromptTemplate(DYNAMIC_TEMPLATE);
        Prompt prompt = template.create(Map.of(
                "konu", effectiveKonu,
                "dil" , effectiveDil,
                "uzunluk", effectiveUzunluk
        ));

        String response = chatClient.prompt(prompt).call().content();
        return Map.of(
                "konu", effectiveKonu,
                "dil" , effectiveDil,
                "uzunluk", effectiveUzunluk,
                "yanit", response);
    }

    private void validateText(String value,String paramName) {
        if(value == null || value.isBlank()) {
            throw new IllegalArgumentException(paramName + "parametresi bos olamaz");
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Bilinmeyen Hata";
        logger.error("Hata ", message);
        return Map.of("error", message);
    }

    public static void main(String[] args) {
        SpringApplication.run(PromptTemplateDemo.class, args);
    }

    // Gelişmiş Kullanım
    @GetMapping("/System-user")
    public Map<String, Object> systemUserPrompt(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String soru
    ){
        String effectiveRol = rol == null ? "Java" : rol;
        String effectiveFormat = format == null ? "madde madde" : format;
        String effectiveSoru = soru == null ? "Spring AI neden onemlidir ?" : soru;

        validateText(effectiveRol, "rol");
        validateText(effectiveFormat, "format");
        validateText(effectiveSoru, "soru");

        String response = chatClient.prompt().system(s->
                s.text(SYSTEM_TEMPLATE)
                        .param("rol", effectiveRol)
                        .param("format", effectiveFormat)
                ).user(u->u.text(USER_TEMPLATE).param("soru", effectiveSoru)).call().content();
        return Map.of(
                "rol", effectiveRol,
                "format", effectiveFormat,
                "soru", effectiveSoru,
                "yanit", response
        );
    }

}
