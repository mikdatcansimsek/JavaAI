package com.javaai.bolum05;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("!multimodel")
@EnableAutoConfiguration
@RestController
@RequestMapping("/api/b51")
public class BeanOutputConverterDemo {

    private static final Logger log = LoggerFactory.getLogger(BeanOutputConverterDemo.class);

    private static final int MIN_TITLE_LENGTH = 2;
    private static final int MAX_PREVIEW_LENGTH = 200;

    private final ChatClient chatClient;

    public BeanOutputConverterDemo(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("Sen dikkatli bir sinema asistanisin. Cevaplari Turkce acikla")
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(BeanOutputConverterDemo.class, args);
    }

    @GetMapping("/movie")
    public Map<String, Object> movie(@RequestParam(required = false) String title) {
        String effectiveTitle = title == null ? "The Matrix" : title;
        if (effectiveTitle.trim().length() < MIN_TITLE_LENGTH) {
            throw new IllegalArgumentException("title en az 2 karakter olmalidir.");
        }

        BeanOutputConverter<MovieInfo> converter = new BeanOutputConverter<>(
                MovieInfo.class); // Record tipine göre JSON schema üretir.
        String formatInstructions = converter.getFormat(); // Modele gönderilecek format talimati

        String rawResponse = chatClient
                .prompt()
                .user(u -> u
                        .text("""
                    Asagidaki film icin structured bilgi ver:
                    Film: {title}
                    
                    Yalnizca bu formatta cevapla:
                    {format}
                    """)
                        .param("title", effectiveTitle.trim())
                        .param("format", formatInstructions))
                .call()
                .content();

        MovieInfo movieInfo = converter.convert(rawResponse);
        log.info("title: {}, year: {}, rating: {}", movieInfo.title(), movieInfo.year(),
                movieInfo.imdbRating());

        return Map.of(
                "requestedTitle", effectiveTitle.trim(),
                "structured", movieInfo,
                "rawResponse", rawResponse,
                "schema", formatInstructions
        );
    }


    public record MovieInfo(String title,int year, String genre, double imdbRating, String director) {
        public MovieInfo{
            if(title == null || title.isBlank()) {
                throw new  IllegalArgumentException("title bos olamaz");
            }
            if(year < 1888){
                throw new  IllegalArgumentException("year gecersiz"); // ilk film 1888 - altındakiler olamaz
            }
            if(genre == null || genre.isBlank()) {
                throw new  IllegalArgumentException("genre bos olamaz");
            }
            if(imdbRating <0.0 ||  imdbRating > 10.0) {
                throw new  IllegalArgumentException("imdb raiting 0-10 araliginda olmali");
            }
            if(director == null || director.isBlank()) {
                throw new  IllegalArgumentException("director bos olamaz");
            }
        }
    }
}
