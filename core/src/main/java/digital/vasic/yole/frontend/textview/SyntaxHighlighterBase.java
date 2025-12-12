/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Base class for syntax highlighters
 * Provides common functionality for syntax highlighting
 *
 *########################################################*/

package digital.vasic.yole.frontend.textview;

import digital.vasic.yole.model.AppSettings;

/**
 * Base class for syntax highlighters.
 * 
 * Provides common functionality for syntax highlighting in text editors.
 * Each format should extend this class and implement the required methods.
 */
public abstract class SyntaxHighlighterBase {
    
    protected final AppSettings appSettings;
    
    /**
     * Constructor for syntax highlighter.
     * 
     * @param appSettings Application settings
     */
    public SyntaxHighlighterBase(AppSettings appSettings) {
        this.appSettings = appSettings;
    }
    
    /**
     * Highlight syntax in the given text.
     * 
     * @param text The text to highlight
     * @return Text with syntax highlighting applied
     */
    public abstract CharSequence highlight(String text);
    
    /**
     * Get the format ID for this highlighter.
     * 
     * @return The format identifier
     */
    public String getFormatId() {
        // Default implementation - subclasses should override
        return "unknown";
    }
    
    /**
     * Check if this highlighter is enabled.
     * 
     * @return true if highlighting is enabled, false otherwise
     */
    public boolean isEnabled() {
        // Default implementation - subclasses should override based on settings
        return true;
    }
    
    /**
     * Update settings for the highlighter.
     * 
     * @param newSettings The new application settings
     */
    public void updateSettings(AppSettings newSettings) {
        // Default implementation - subclasses should override if needed
    }
}