package com.aipdfchat.controller;

import com.aipdfchat.entity.Document;
import com.aipdfchat.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

// REST APIs for PDF files:
//   POST   /api/documents/upload   upload a PDF
//   GET    /api/documents          list uploaded PDFs
//   DELETE /api/documents/{id}     remove a PDF
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final PdfService pdfService;

    public DocumentController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    // The frontend sends the PDF as multipart/form-data with the field name "file"
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            Document document = pdfService.uploadPdf(file);
            return ResponseEntity.ok(document);

        } catch (IllegalArgumentException e) {
            // Problem with the file (not a PDF, empty, no text ...)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            // Something else failed, usually Ollama or ChromaDB is not running
            log.error("Upload failed", e);
            return ResponseEntity.status(500).body(Map.of("message",
                    "Could not process the PDF. Please check that Ollama and ChromaDB are running."));
        }
    }

    @GetMapping
    public List<Document> getAllDocuments() {
        return pdfService.getAllDocuments();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") String id) {
        try {
            pdfService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("message", "Document removed."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            log.error("Delete failed", e);
            return ResponseEntity.status(500).body(Map.of("message",
                    "Could not remove the document. Please check that ChromaDB is running."));
        }
    }
}
