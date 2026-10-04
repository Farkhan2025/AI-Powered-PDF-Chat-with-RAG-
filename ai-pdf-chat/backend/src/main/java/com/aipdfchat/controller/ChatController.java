package com.aipdfchat.controller;

import com.aipdfchat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// REST API for asking questions:
//   POST /api/chat    body: { "documentId": "...", "question": "..." }
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String documentId = body.get("documentId");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please type a question."));
        }
        if (documentId == null || documentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No document selected."));
        }

        try {
            Map<String, Object> result = chatService.chat(question, documentId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            log.error("Chat failed", e);
            return ResponseEntity.status(500).body(Map.of("message",
                    "Could not get an answer. Please check that Ollama and ChromaDB are running."));
        }
    }
}
