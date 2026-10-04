package com.aipdfchat.repository;

import com.aipdfchat.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring creates the implementation for us.
// We get save(), findById(), findAll(), deleteById() ... without writing SQL.
public interface DocumentRepository extends JpaRepository<Document, String> {
}
