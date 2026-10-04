package com.aipdfchat.service;

import com.aipdfchat.entity.Document;
import com.aipdfchat.repository.DocumentRepository;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The RAG flow for one question:
// question -> find relevant chunks (ChromaDB) -> build prompt -> Llama 3.2 (Ollama) -> answer
@Service
public class ChatService {

    // This tells the model how to behave. It is what keeps answers based on the PDF.
    private static final String SYSTEM_INSTRUCTION =
            "You are a document assistant. "
            + "Answer only using the provided document context. "
            + "If the answer is not available in the document, say you don't know.";

    private final ChatModel chatModel;
    private final VectorService vectorService;
    private final DocumentRepository documentRepository;

    public ChatService(ChatModel chatModel,
                       VectorService vectorService,
                       DocumentRepository documentRepository) {
        this.chatModel = chatModel;
        this.vectorService = vectorService;
        this.documentRepository = documentRepository;
    }

    public Map<String, Object> chat(String question, String documentId) {

        // Step 1: make sure the PDF exists and is ready
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));

        if (!"READY".equals(document.getStatus())) {
            throw new IllegalArgumentException("This document is not ready. Please upload it again.");
        }

        // Step 2: semantic search in ChromaDB (only inside this PDF)
        List<String> chunks = vectorService.search(documentId, question);

        if (chunks.isEmpty()) {
            return buildResult("I don't know. I couldn't find relevant information in this document.",
                    new ArrayList<>());
        }

        // Step 3: join the chunks into one block of text (the "context")
        String context = String.join("\n\n", chunks);

        // Step 4: build the prompt = context + question
        String userText = "Context:\n" + context + "\n\nQuestion:\n" + question;

        // Step 5: send system instruction + user text to Llama 3.2 through Spring AI
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_INSTRUCTION),
                new UserMessage(userText)));

        String answer = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

        // Step 6: also return short previews of the chunks that were used
        List<String> sources = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.length() > 150) {
                sources.add(chunk.substring(0, 150) + "...");
            } else {
                sources.add(chunk);
            }
        }

        return buildResult(answer, sources);
    }

    // The JSON sent to the frontend: { "answer": "...", "sources": ["...", "..."] }
    private Map<String, Object> buildResult(String answer, List<String> sources) {
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("sources", sources);
        return result;
    }
}
