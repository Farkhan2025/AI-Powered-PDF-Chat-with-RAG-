package com.aipdfchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Entry point of the application. Spring Boot starts the web server,
// finds our controllers/services/repositories and connects everything.
@SpringBootApplication
public class AiPdfChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPdfChatApplication.class, args);
    }
}
