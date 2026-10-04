package com.aipdfchat.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

// This class is a database table (documents).
// It only stores details about the PDF. The chunks and embeddings live in ChromaDB.

@Data
@Entity
@Table(name = "documents")
public class Document {

    // Primary key. Hibernate generates a random UUID text for us.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String fileName;
    private long fileSizeBytes;
    private int totalChunks;

    // PROCESSING -> READY (or FAILED if something went wrong)
    private String status;

    private LocalDateTime uploadedAt;

    // JPA needs an empty constructor
    public Document() {
    }


}
