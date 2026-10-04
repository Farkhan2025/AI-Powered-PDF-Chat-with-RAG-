package com.aipdfchat.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// All communication with ChromaDB happens here, through Spring AI's VectorStore.
//
// Important: we never call the embedding model ourselves.
// When we call vectorStore.add(...) or vectorStore.similaritySearch(...), Spring AI
// sends the text to Ollama (nomic-embed-text) to get the embedding, and then talks to ChromaDB.
@Service
public class VectorService {

    private final VectorStore vectorStore;
    private final int topK;

    public VectorService(VectorStore vectorStore, @Value("${app.top-k}") int topK) {
        this.vectorStore = vectorStore;
        this.topK = topK;
    }

    // Saves every chunk in ChromaDB (text + embedding + metadata).
    public void saveChunks(String documentId, String fileName, List<String> chunks) {
        List<Document> documentsToSave = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            // We build the id ourselves, so we can delete the chunks later
            String chunkId = documentId + "-" + i;

            // Metadata is extra information stored next to each chunk.
            // "documentId" lets us search inside one PDF only.
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", documentId);
            metadata.put("fileName", fileName);
            metadata.put("chunkNumber", i + 1);

            documentsToSave.add(new Document(chunkId, chunks.get(i), metadata));
        }

        // Spring AI creates the embeddings with Ollama and stores everything in ChromaDB
        vectorStore.add(documentsToSave);
    }

    // Semantic search: finds the chunks of ONE pdf whose meaning is closest to the question.
    // Semantic search: finds the chunks of ONE pdf whose meaning is closest to the question.
    public List<String> search(String documentId, String question) {

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .filterExpression("documentId == '" + documentId + "'")
                .build();

        List<Document> found = vectorStore.similaritySearch(request);

        List<String> chunkTexts = new ArrayList<>();

        for (Document document : found) {
            chunkTexts.add(document.getText());
        }

        return chunkTexts;
    }

    // Removes all chunks of a PDF from ChromaDB.
    // We know the ids because saveChunks() used documentId-0, documentId-1, ...
    public void deleteChunks(String documentId, int totalChunks) {
        if (totalChunks <= 0) {
            return;
        }

        List<String> idsToDelete = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            idsToDelete.add(documentId + "-" + i);
        }

        vectorStore.delete(idsToDelete);
    }
}
