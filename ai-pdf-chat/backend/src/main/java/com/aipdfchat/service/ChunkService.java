package com.aipdfchat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Splits a long text into smaller pieces called "chunks".
//
// Why? 1) An embedding model works best on short text.
//      2) The LLM can only read a limited amount of text at once, so we cannot
//         send the whole PDF. We search for the few chunks that matter and send only those.
//
// We count words (not tokens) because it is simple. The chunks overlap a little,
// so a sentence that sits on the border of two chunks is not lost.
@Service
public class ChunkService {

    private final int chunkSize;     // words in one chunk
    private final int chunkOverlap;  // words shared with the previous chunk

    // @Value reads the numbers from application.properties
    public ChunkService(@Value("${app.chunk-size}") int chunkSize,
                        @Value("${app.chunk-overlap}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        // 1. Break the text into words
        String[] words = text.trim().split("\\s+");

        // 2. Each new chunk starts (chunkSize - chunkOverlap) words after the previous one
        int step = chunkSize - chunkOverlap;
        if (step < 1) {
            step = 1;
        }

        // 3. Walk through the words and cut one chunk at a time
        int start = 0;
        while (start < words.length) {
            int end = start + chunkSize;
            if (end > words.length) {
                end = words.length;
            }

            String[] wordsOfThisChunk = Arrays.copyOfRange(words, start, end);
            chunks.add(String.join(" ", wordsOfThisChunk));

            // The last chunk reached the end of the text, so we are done
            if (end == words.length) {
                break;
            }
            start = start + step;
        }

        return chunks;
    }
}
