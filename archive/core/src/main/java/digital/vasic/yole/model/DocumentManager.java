/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Document manager
 * Manages document lifecycle and operations
 *
 *########################################################*/

package digital.vasic.yole.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Document manager.
 * 
 * Manages document lifecycle, file operations, and document state.
 * Provides centralized access to document-related functionality.
 */
public class DocumentManager {
    
    private final List<Document> openDocuments;
    private Document currentDocument;
    
    /**
     * Constructor.
     */
    public DocumentManager() {
        this.openDocuments = new ArrayList<>();
        this.currentDocument = null;
    }
    
    /**
     * Create a new document.
     * 
     * @param content Initial content
     * @param format Document format
     * @return New document
     */
    public Document createDocument(String content, String format) {
        Document document = new Document(content, format);
        openDocuments.add(document);
        setCurrentDocument(document);
        return document;
    }
    
    /**
     * Create a new document with a file.
     * 
     * @param file Associated file
     * @param format Document format
     * @return New document
     */
    public Document createDocument(File file, String format) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            Document document = new Document(content, file, format);
            openDocuments.add(document);
            setCurrentDocument(document);
            return document;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getAbsolutePath(), e);
        }
    }
    
    /**
     * Open a document from a file.
     * 
     * @param file File to open
     * @return Document
     */
    public Document openDocument(File file) {
        // Try to find existing document
        for (Document doc : openDocuments) {
            if (doc.getFile() != null && doc.getFile().equals(file)) {
                setCurrentDocument(doc);
                return doc;
            }
        }
        
        // Create new document
        String format = detectFormat(file);
        return createDocument(file, format);
    }
    
    /**
     * Save a document.
     * 
     * @param document Document to save
     */
    public void saveDocument(Document document) {
        if (document.getFile() == null) {
            throw new IllegalStateException("Cannot save document without file");
        }
        
        try {
            Files.write(document.getFile().toPath(), document.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            document.markAsSaved();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save document: " + document.getFile().getAbsolutePath(), e);
        }
    }
    
    /**
     * Save a document to a specific file.
     * 
     * @param document Document to save
     * @param file Target file
     */
    public void saveDocumentAs(Document document, File file) {
        document.setFile(file);
        saveDocument(document);
    }
    
    /**
     * Close a document.
     * 
     * @param document Document to close
     */
    public void closeDocument(Document document) {
        openDocuments.remove(document);
        if (document.equals(currentDocument)) {
            currentDocument = openDocuments.isEmpty() ? null : openDocuments.get(0);
        }
    }
    
    /**
     * Get all open documents.
     * 
     * @return List of open documents
     */
    public List<Document> getOpenDocuments() {
        return new ArrayList<>(openDocuments);
    }
    
    /**
     * Get the current document.
     * 
     * @return Current document or null if none
     */
    public Document getCurrentDocument() {
        return currentDocument;
    }
    
    /**
     * Set the current document.
     * 
     * @param document Document to set as current
     */
    public void setCurrentDocument(Document document) {
        if (!openDocuments.contains(document)) {
            throw new IllegalArgumentException("Document is not open");
        }
        this.currentDocument = document;
    }
    
    /**
     * Check if a document is open.
     * 
     * @param file File to check
     * @return true if document is open, false otherwise
     */
    public boolean isDocumentOpen(File file) {
        return openDocuments.stream()
                .anyMatch(doc -> doc.getFile() != null && doc.getFile().equals(file));
    }
    
    /**
     * Detect format from file extension.
     * 
     * @param file File to analyze
     * @return Detected format
     */
    private String detectFormat(File file) {
        String extension = getFileExtension(file);
        if (extension.isEmpty()) {
            return "plaintext";
        }
        
        // Simple extension-based detection
        switch (extension.toLowerCase()) {
            case "md":
            case "markdown":
            case "mkd":
                return "markdown";
            case "txt":
                return "plaintext";
            case "csv":
                return "csv";
            case "tex":
            case "latex":
                return "latex";
            case "adoc":
            case "asciidoc":
                return "asciidoc";
            case "org":
                return "orgmode";
            case "wiki":
            case "wikitext":
                return "wikitext";
            case "ipynb":
                return "jupyter";
            default:
                return "plaintext";
        }
    }
    
    /**
     * Get file extension.
     * 
     * @param file File to analyze
     * @return File extension without dot
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1);
        }
        return "";
    }
}