/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Document model
 * Represents a document with its content and metadata
 *
 *########################################################*/

package digital.vasic.yole.model;

import java.io.File;

/**
 * Document model.
 * 
 * Represents a document with its content, file information, and metadata.
 * This class serves as the central data model for documents in the application.
 */
public class Document {
    
    private String content;
    private File file;
    private String format;
    private String title;
    private boolean modified;
    private long lastModified;
    
    /**
     * Constructor for a new document.
     * 
     * @param content Document content
     * @param file Associated file
     * @param format Document format
     */
    public Document(String content, File file, String format) {
        this.content = content;
        this.file = file;
        this.format = format;
        this.title = file != null ? file.getName() : "Untitled";
        this.modified = false;
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Constructor for a new document without a file.
     * 
     * @param content Document content
     * @param format Document format
     */
    public Document(String content, String format) {
        this(content, null, format);
    }
    
    /**
     * Default constructor.
     */
    public Document() {
        this("", "plaintext");
    }
    
    /**
     * Get the document content.
     * 
     * @return Document content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Set the document content.
     * 
     * @param content New content
     */
    public void setContent(String content) {
        this.content = content;
        this.modified = true;
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Get the associated file.
     * 
     * @return Associated file or null if not saved
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Set the associated file.
     * 
     * @param file New file
     */
    public void setFile(File file) {
        this.file = file;
        if (file != null) {
            this.title = file.getName();
        }
    }
    
    /**
     * Get the document format.
     * 
     * @return Document format identifier
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Set the document format.
     * 
     * @param format New format identifier
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Get the document title.
     * 
     * @return Document title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set the document title.
     * 
     * @param title New title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Check if the document has been modified.
     * 
     * @return true if modified, false otherwise
     */
    public boolean isModified() {
        return modified;
    }
    
    /**
     * Set the modified state.
     * 
     * @param modified New modified state
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }
    
    /**
     * Mark the document as saved (not modified).
     */
    public void markAsSaved() {
        this.modified = false;
    }
    
    /**
     * Get the last modified timestamp.
     * 
     * @return Last modified timestamp
     */
    public long getLastModified() {
        return lastModified;
    }
    
    /**
     * Check if the document has an associated file.
     * 
     * @return true if has file, false otherwise
     */
    public boolean hasFile() {
        return file != null;
    }
    
    /**
     * Get the file extension.
     * 
     * @return File extension or empty string if no file
     */
    public String getFileExtension() {
        if (file != null) {
            String name = file.getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0 && lastDot < name.length() - 1) {
                return name.substring(lastDot + 1);
            }
        }
        return "";
    }
    
    /**
     * Get a string representation of the document.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                ", format='" + format + '\'' +
                ", modified=" + modified +
                ", hasFile=" + hasFile() +
                '}';
    }
}