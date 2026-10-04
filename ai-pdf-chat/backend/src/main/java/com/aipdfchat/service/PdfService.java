package com.aipdfchat.service;

import com.aipdfchat.entity.Document;
import com.aipdfchat.repository.DocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// Handles the PDF side of the application:
// upload (validate -> extract text -> chunk -> store in ChromaDB), list and delete.
@Service
public class PdfService {

    private final DocumentRepository documentRepository;
    private final ChunkService chunkService;
    private final VectorService vectorService;

    public PdfService(DocumentRepository documentRepository,
                      ChunkService chunkService,
                      VectorService vectorService) {
        this.documentRepository = documentRepository;
        this.chunkService = chunkService;
        this.vectorService = vectorService;
    }

    // The full upload flow
    public Document uploadPdf(MultipartFile file) {

        // Step 1: check the file
        validate(file);

        // Step 2: save the PDF details in the database (status PROCESSING)
        Document document = new Document();
        document.setFileName(file.getOriginalFilename());
        document.setFileSizeBytes(file.getSize());
        document.setStatus("PROCESSING");
        document.setUploadedAt(LocalDateTime.now());
        document = documentRepository.save(document);

        try {
            // Step 3: read the text from the PDF with PDFBox
            String text = extractText(file);

            // Step 4: split the text into chunks
            List<String> chunks = chunkService.splitIntoChunks(text);

            // Step 5: create embeddings and store the chunks in ChromaDB
            vectorService.saveChunks(document.getId(), document.getFileName(), chunks);

            // Step 6: everything worked, so the PDF is ready for questions
            document.setTotalChunks(chunks.size());
            document.setStatus("READY");
            return documentRepository.save(document);

        } catch (RuntimeException e) {
            document.setStatus("FAILED");
            documentRepository.save(document);
            // Throw the problem again so the controller can tell the user
            throw e;
        }
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // Removes the PDF: first its chunks from ChromaDB, then its row from the database
    public void deleteDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));

        vectorService.deleteChunks(document.getId(), document.getTotalChunks());
        documentRepository.delete(document);
    }

    // ---------- helper methods ----------

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a PDF file.");
        }

        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }
    }

    // PDFBox opens the PDF and the PDFTextStripper reads the text of all pages
    private String extractText(MultipartFile file) {
        try (PDDocument pdf = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(pdf);
            String text = cleanText(rawText);

            if (text.isEmpty()) {
                throw new IllegalArgumentException(
                        "No text found in this PDF. It may be a scanned document (images only).");
            }
            return text;

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read this PDF. The file may be damaged or password protected.");
        }
    }

    // Removes extra blank lines and spaces so the chunks are cleaner
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        text = text.replace("\r\n", "\n");
        text = text.replace("\r", "\n");
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }
}
