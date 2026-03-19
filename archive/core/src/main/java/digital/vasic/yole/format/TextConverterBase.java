/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Base class for text format converters
 * Provides common functionality for converting text formats to HTML
 *
 *########################################################*/

package digital.vasic.yole.format;

import android.content.Context;
import java.io.File;

/**
 * Base class for text format converters.
 * 
 * Provides common functionality for converting different text formats to HTML
 * for display and editing purposes. Each format should extend this class
 * and implement the required methods.
 */
public abstract class TextConverterBase {
    
    /**
     * Check if a file is of this format.
     * 
     * @param file The file to check
     * @return true if the file is of this format, false otherwise
     */
    public abstract boolean isFileOutOfThisFormat(File file);
    
    /**
     * Convert markup text to HTML.
     * 
     * @param markup The markup text to convert
     * @param context Android context for resources
     * @param lightMode Whether to use light mode styling
     * @param lineNum Line number for the conversion
     * @param file The source file
     * @return HTML representation of the markup
     */
    public abstract String convertMarkupToHtml(String markup, Context context, boolean lightMode, int lineNum, File file);
    
    /**
     * Get the format ID for this converter.
     * 
     * @return The format identifier
     */
    public String getFormatId() {
        // Default implementation - subclasses should override
        return "unknown";
    }
    
    /**
     * Get the format name for this converter.
     * 
     * @return The human-readable format name
     */
    public String getFormatName() {
        // Default implementation - subclasses should override
        return "Unknown Format";
    }
    
    /**
     * Check if this converter supports the given file extension.
     * 
     * @param extension The file extension (with or without dot)
     * @return true if supported, false otherwise
     */
    public boolean supportsExtension(String extension) {
        // Default implementation - subclasses should override
        return false;
    }
}