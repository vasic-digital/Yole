/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format registry interface
 * Provides access to format information and detection
 *
 *########################################################*/

package digital.vasic.yole.format;

import java.util.List;

/**
 * Format registry interface.
 * 
 * Provides access to format information and detection functionality.
 * This interface bridges the shared module's FormatRegistry with
 * the Android-specific core module implementations.
 */
public interface FormatRegistry {
    
    /**
     * Get format by unique identifier.
     * 
     * @param id The format identifier to look up
     * @return The TextFormat if found, null otherwise
     */
    TextFormat getById(String id);
    
    /**
     * Get format by file extension.
     * 
     * @param extension The file extension (with or without dot)
     * @return The TextFormat if found, null otherwise
     */
    TextFormat getByExtension(String extension);
    
    /**
     * Detect format by file extension with fallback to plain text.
     * 
     * @param extension The file extension (with or without dot)
     * @return A TextFormat (never null, falls back to plain text)
     */
    TextFormat detectByExtension(String extension);
    
    /**
     * Check if a format is supported by the registry.
     * 
     * @param formatId The format identifier to check
     * @return true if the format is supported, false otherwise
     */
    boolean isSupported(String formatId);
    
    /**
     * Check if a file extension is supported.
     * 
     * @param extension The file extension to check (with or without dot)
     * @return true if the extension is supported, false otherwise
     */
    boolean isExtensionSupported(String extension);
    
    /**
     * Get all supported formats.
     * 
     * @return List of all supported formats
     */
    List<TextFormat> getAllFormats();
    
    /**
     * Get all supported file extensions.
     * 
     * @return List of all unique file extensions supported by any format
     */
    List<String> getAllExtensions();
    
    /**
     * Get all format names.
     * 
     * @return List of human-readable format names
     */
    List<String> getFormatNames();
}