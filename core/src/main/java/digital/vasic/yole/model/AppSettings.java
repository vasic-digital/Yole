/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Application settings model
 * Provides centralized access to application configuration
 *
 *########################################################*/

package digital.vasic.yole.model;

/**
 * Application settings model.
 * 
 * Provides centralized access to application configuration and preferences.
 * This class serves as a bridge between the Android preferences system
 * and the format-specific components.
 */
public class AppSettings {
    
    // Editor settings
    private boolean syntaxHighlightingEnabled = true;
    private boolean lineNumbersEnabled = true;
    private boolean wordWrapEnabled = false;
    private int fontSize = 14;
    private String fontFamily = "monospace";
    
    // Display settings
    private boolean lightMode = true;
    private boolean showToolbar = true;
    private boolean showStatusBar = true;
    
    // Format-specific settings
    private boolean markdownAutoFormat = true;
    private boolean markdownLivePreview = false;
    
    /**
     * Default constructor.
     */
    public AppSettings() {
        // Default settings
    }
    
    /**
     * Check if syntax highlighting is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isSyntaxHighlightingEnabled() {
        return syntaxHighlightingEnabled;
    }
    
    /**
     * Set syntax highlighting enabled state.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setSyntaxHighlightingEnabled(boolean enabled) {
        this.syntaxHighlightingEnabled = enabled;
    }
    
    /**
     * Check if line numbers are enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isLineNumbersEnabled() {
        return lineNumbersEnabled;
    }
    
    /**
     * Set line numbers enabled state.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setLineNumbersEnabled(boolean enabled) {
        this.lineNumbersEnabled = enabled;
    }
    
    /**
     * Check if word wrap is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isWordWrapEnabled() {
        return wordWrapEnabled;
    }
    
    /**
     * Set word wrap enabled state.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setWordWrapEnabled(boolean enabled) {
        this.wordWrapEnabled = enabled;
    }
    
    /**
     * Get the font size.
     * 
     * @return Font size in pixels
     */
    public int getFontSize() {
        return fontSize;
    }
    
    /**
     * Set the font size.
     * 
     * @param size Font size in pixels
     */
    public void setFontSize(int size) {
        this.fontSize = size;
    }
    
    /**
     * Get the font family.
     * 
     * @return Font family name
     */
    public String getFontFamily() {
        return fontFamily;
    }
    
    /**
     * Set the font family.
     * 
     * @param family Font family name
     */
    public void setFontFamily(String family) {
        this.fontFamily = family;
    }
    
    /**
     * Check if light mode is enabled.
     * 
     * @return true for light mode, false for dark mode
     */
    public boolean isLightMode() {
        return lightMode;
    }
    
    /**
     * Set light mode state.
     * 
     * @param light true for light mode, false for dark mode
     */
    public void setLightMode(boolean light) {
        this.lightMode = light;
    }
    
    /**
     * Check if toolbar should be shown.
     * 
     * @return true if toolbar should be shown, false otherwise
     */
    public boolean isShowToolbar() {
        return showToolbar;
    }
    
    /**
     * Set toolbar visibility.
     * 
     * @param show true to show toolbar, false to hide
     */
    public void setShowToolbar(boolean show) {
        this.showToolbar = show;
    }
    
    /**
     * Check if status bar should be shown.
     * 
     * @return true if status bar should be shown, false otherwise
     */
    public boolean isShowStatusBar() {
        return showStatusBar;
    }
    
    /**
     * Set status bar visibility.
     * 
     * @param show true to show status bar, false to hide
     */
    public void setShowStatusBar(boolean show) {
        this.showStatusBar = show;
    }
    
    /**
     * Check if markdown auto-format is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isMarkdownAutoFormat() {
        return markdownAutoFormat;
    }
    
    /**
     * Set markdown auto-format state.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setMarkdownAutoFormat(boolean enabled) {
        this.markdownAutoFormat = enabled;
    }
    
    /**
     * Check if markdown live preview is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isMarkdownLivePreview() {
        return markdownLivePreview;
    }
    
    /**
     * Set markdown live preview state.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setMarkdownLivePreview(boolean enabled) {
        this.markdownLivePreview = enabled;
    }
}