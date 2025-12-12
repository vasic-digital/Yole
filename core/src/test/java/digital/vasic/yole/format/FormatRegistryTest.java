/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Test for FormatRegistry implementation
 *
 *########################################################*/

package digital.vasic.yole.format;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Test for FormatRegistry implementation.
 */
public class FormatRegistryTest {
    
    @Test
    public void testGetById() {
        FormatRegistry registry = new FormatRegistryImpl();
        TextFormat format = registry.getById("markdown");
        assertNotNull("Markdown format should be found", format);
        assertEquals("Markdown", format.getName());
        assertEquals("markdown", format.getId());
    }
    
    @Test
    public void testGetByExtension() {
        FormatRegistry registry = new FormatRegistryImpl();
        TextFormat format = registry.getByExtension(".md");
        assertNotNull("Markdown format should be found by extension", format);
        assertEquals("markdown", format.getId());
        
        format = registry.getByExtension("md");
        assertNotNull("Markdown format should be found by extension without dot", format);
        assertEquals("markdown", format.getId());
    }
    
    @Test
    public void testDetectByExtension() {
        FormatRegistry registry = new FormatRegistryImpl();
        TextFormat format = registry.detectByExtension("unknown");
        assertNotNull("Should return plaintext for unknown extension", format);
        assertEquals("plaintext", format.getId());
        
        format = registry.detectByExtension(".md");
        assertEquals("markdown", format.getId());
    }
    
    @Test
    public void testIsSupported() {
        FormatRegistry registry = new FormatRegistryImpl();
        assertTrue("Markdown should be supported", registry.isSupported("markdown"));
        assertFalse("Unknown format should not be supported", registry.isSupported("unknown"));
    }
    
    @Test
    public void testIsExtensionSupported() {
        FormatRegistry registry = new FormatRegistryImpl();
        assertTrue(".md extension should be supported", registry.isExtensionSupported(".md"));
        assertTrue("md extension should be supported", registry.isExtensionSupported("md"));
        assertFalse(".xyz extension should not be supported", registry.isExtensionSupported(".xyz"));
    }
    
    @Test
    public void testGetAllFormats() {
        FormatRegistry registry = new FormatRegistryImpl();
        List<TextFormat> formats = registry.getAllFormats();
        assertNotNull("Formats list should not be null", formats);
        assertTrue("Should have multiple formats", formats.size() > 0);
        
        // Check that some expected formats are present
        boolean hasMarkdown = formats.stream().anyMatch(f -> "markdown".equals(f.getId()));
        boolean hasPlaintext = formats.stream().anyMatch(f -> "plaintext".equals(f.getId()));
        assertTrue("Should have markdown format", hasMarkdown);
        assertTrue("Should have plaintext format", hasPlaintext);
    }
    
    @Test
    public void testGetAllExtensions() {
        FormatRegistry registry = new FormatRegistryImpl();
        List<String> extensions = registry.getAllExtensions();
        assertNotNull("Extensions list should not be null", extensions);
        assertTrue("Should have multiple extensions", extensions.size() > 0);
        
        assertTrue("Should have .md extension", extensions.contains(".md"));
        assertTrue("Should have .txt extension", extensions.contains(".txt"));
    }
    
    @Test
    public void testGetFormatNames() {
        FormatRegistry registry = new FormatRegistryImpl();
        List<String> names = registry.getFormatNames();
        assertNotNull("Names list should not be null", names);
        assertTrue("Should have multiple format names", names.size() > 0);
        
        assertTrue("Should have Markdown name", names.contains("Markdown"));
        assertTrue("Should have Plain Text name", names.contains("Plain Text"));
    }
}