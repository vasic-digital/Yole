/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Base class for format action buttons
 * Provides common functionality for format-specific toolbar actions
 *
 *########################################################*/

package digital.vasic.yole.format;

import android.content.Context;
import digital.vasic.yole.model.Document;

/**
 * Base class for format action buttons.
 * 
 * Provides common functionality for format-specific toolbar actions.
 * Each format should extend this class and implement the required methods.
 */
public abstract class ActionButtonBase {
    
    protected final Context context;
    protected final Document document;
    
    /**
     * Constructor for action button base.
     * 
     * @param context Android context
     * @param document The current document
     */
    public ActionButtonBase(Context context, Document document) {
        this.context = context;
        this.document = document;
    }
    
    /**
     * Get the format ID for this action button.
     * 
     * @return The format identifier
     */
    public String getFormatId() {
        // Default implementation - subclasses should override
        return "unknown";
    }
    
    /**
     * Check if this action button should be visible for the current document.
     * 
     * @return true if visible, false otherwise
     */
    public boolean isVisible() {
        // Default implementation - subclasses should override
        return true;
    }
    
    /**
     * Check if this action button should be enabled for the current document.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        // Default implementation - subclasses should override
        return true;
    }
    
    /**
     * Perform the action associated with this button.
     * 
     * @return true if action was performed successfully, false otherwise
     */
    public boolean performAction() {
        // Default implementation - subclasses should override
        return false;
    }
    
    /**
     * Get the display name for this action.
     * 
     * @return The human-readable action name
     */
    public String getActionName() {
        // Default implementation - subclasses should override
        return "Unknown Action";
    }
}