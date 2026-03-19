/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Storage manager
 * Manages file storage operations
 *
 *########################################################*/

package digital.vasic.yole.model;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage manager.
 * 
 * Manages file storage operations, directory management, and file utilities.
 * Provides a centralized interface for file system operations.
 */
public class StorageManager {
    
    private final Context context;
    
    /**
     * Constructor.
     * 
     * @param context Android context
     */
    public StorageManager(Context context) {
        this.context = context;
    }
    
    /**
     * Get the default documents directory.
     * 
     * @return Default documents directory
     */
    public File getDefaultDocumentsDirectory() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }
        return documentsDir;
    }
    
    /**
     * Get the application-specific documents directory.
     * 
     * @return Application documents directory
     */
    public File getAppDocumentsDirectory() {
        File appDir = new File(context.getFilesDir(), "documents");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        return appDir;
    }
    
    /**
     * Create a new file with the given name and extension.
     * 
     * @param directory Parent directory
     * @param name File name without extension
     * @param extension File extension with dot
     * @return Created file
     */
    public File createFile(File directory, String name, String extension) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String fileName = name + extension;
        File file = new File(directory, fileName);
        
        // Handle duplicate names
        int counter = 1;
        while (file.exists()) {
            fileName = name + " (" + counter + ")" + extension;
            file = new File(directory, fileName);
            counter++;
        }
        
        file.createNewFile();
        return file;
    }
    
    /**
     * Check if a file exists.
     * 
     * @param path File path
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String path) {
        return new File(path).exists();
    }
    
    /**
     * Check if a directory exists.
     * 
     * @param path Directory path
     * @return true if directory exists, false otherwise
     */
    public boolean directoryExists(String path) {
        File dir = new File(path);
        return dir.exists() && dir.isDirectory();
    }
    
    /**
     * Create a directory if it doesn't exist.
     * 
     * @param path Directory path
     * @return true if directory was created or already exists, false otherwise
     */
    public boolean createDirectory(String path) {
        File dir = new File(path);
        if (dir.exists()) {
            return true;
        }
        return dir.mkdirs();
    }
    
    /**
     * Delete a file or directory.
     * 
     * @param path Path to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean delete(String path) {
        File file = new File(path);
        return deleteRecursive(file);
    }
    
    /**
     * Recursively delete a file or directory.
     * 
     * @param file File or directory to delete
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursive(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
    
    /**
     * Copy a file.
     * 
     * @param source Source file
     * @param destination Destination file
     * @return true if copy was successful, false otherwise
     */
    public boolean copyFile(File source, File destination) {
        try {
            Path sourcePath = source.toPath();
            Path destPath = destination.toPath();
            Files.copy(sourcePath, destPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Move a file.
     * 
     * @param source Source file
     * @param destination Destination file
     * @return true if move was successful, false otherwise
     */
    public boolean moveFile(File source, File destination) {
        try {
            Path sourcePath = source.toPath();
            Path destPath = destination.toPath();
            Files.move(sourcePath, destPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Read file content as string.
     * 
     * @param file File to read
     * @return File content as string
     */
    public String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Write string content to file.
     * 
     * @param file File to write
     * @param content Content to write
     */
    public void writeFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    /**
     * Get file size in bytes.
     * 
     * @param file File to check
     * @return File size in bytes
     */
    public long getFileSize(File file) {
        return file.length();
    }
    
    /**
     * Get file extension.
     * 
     * @param fileName File name
     * @return File extension without dot, or empty string if no extension
     */
    public String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }
    
    /**
     * Get file name without extension.
     * 
     * @param fileName File name
     * @return File name without extension
     */
    public String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }
    
    /**
     * List files in a directory with a specific extension.
     * 
     * @param directory Directory to search
     * @param extension File extension to filter by (with or without dot)
     * @return List of matching files
     */
    public List<File> listFilesByExtension(File directory, String extension) {
        List<File> result = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return result;
        }
        
        String ext = extension.startsWith(".") ? extension : "." + extension;
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(ext.toLowerCase()));
        
        if (files != null) {
            for (File file : files) {
                result.add(file);
            }
        }
        
        return result;
    }
    
    /**
     * Check if external storage is available.
     * 
     * @return true if external storage is available, false otherwise
     */
    public boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}