/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format registry implementation
 * Default implementation of the FormatRegistry interface
 *
 *########################################################*/

package digital.vasic.yole.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default implementation of the FormatRegistry interface.
 * 
 * Provides a basic implementation of format registry functionality
 * that can be used by Android components.
 */
public class FormatRegistryImpl implements FormatRegistry {
    
    private final List<TextFormat> formats;
    
    /**
     * Constructor with default formats.
     */
    public FormatRegistryImpl() {
        this.formats = createDefaultFormats();
    }
    
    /**
     * Constructor with custom formats.
     * 
     * @param formats Custom format list
     */
    public FormatRegistryImpl(List<TextFormat> formats) {
        this.formats = new ArrayList<>(formats);
    }
    
    @Override
    public TextFormat getById(String id) {
        if (id == null) return null;
        return formats.stream()
                .filter(format -> id.equals(format.getId()))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public TextFormat getByExtension(String extension) {
        if (extension == null) return null;
        final String cleanExtension = extension.trim().toLowerCase();
        final String finalExtension = cleanExtension.startsWith(".") ? cleanExtension : "." + cleanExtension;
        
        return formats.stream()
                .filter(format -> format.getExtensions().contains(finalExtension))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public TextFormat detectByExtension(String extension) {
        TextFormat format = getByExtension(extension);
        return format != null ? format : getById("plaintext");
    }
    
    @Override
    public boolean isSupported(String formatId) {
        return getById(formatId) != null;
    }
    
    @Override
    public boolean isExtensionSupported(String extension) {
        return getByExtension(extension) != null;
    }
    
    @Override
    public List<TextFormat> getAllFormats() {
        return new ArrayList<>(formats);
    }
    
    @Override
    public List<String> getAllExtensions() {
        return formats.stream()
                .flatMap(format -> format.getExtensions().stream())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<String> getFormatNames() {
        return formats.stream()
                .map(TextFormat::getName)
                .sorted()
                .toList();
    }
    
    /**
     * Create default format list.
     * 
     * @return List of default formats
     */
    private List<TextFormat> createDefaultFormats() {
        return Arrays.asList(
                new TextFormat("plaintext", "Plain Text", ".txt", 
                        Arrays.asList(".txt", ".text", ".log"), Arrays.asList()),
                new TextFormat("markdown", "Markdown", ".md", 
                        Arrays.asList(".md", ".markdown", ".mkd"), Arrays.asList("^#+ ", "^\\[.*\\]\\(.*\\)")),
                new TextFormat("todotxt", "Todo.txt", ".txt", 
                        Arrays.asList(".txt"), Arrays.asList("^\\([A-Z]\\) ", "^x \\d{4}-\\d{2}-\\d{2}")),
                new TextFormat("csv", "CSV", ".csv", 
                        Arrays.asList(".csv"), Arrays.asList("^.*,.*,.*$")),
                new TextFormat("wikitext", "WikiText", ".wiki", 
                        Arrays.asList(".wiki", ".wikitext"), Arrays.asList("^==+ .* ==+$", "^\\[\\[.*\\]\\]")),
                new TextFormat("orgmode", "Org Mode", ".org", 
                        Arrays.asList(".org"), Arrays.asList("^\\* ", "^#\\+")),
                new TextFormat("latex", "LaTeX", ".tex", 
                        Arrays.asList(".tex", ".latex"), Arrays.asList("\\\\documentclass", "\\\\begin\\{document\\}")),
                new TextFormat("asciidoc", "AsciiDoc", ".adoc", 
                        Arrays.asList(".adoc", ".asciidoc"), Arrays.asList("^= ", "^== ")),
                new TextFormat("jupyter", "Jupyter Notebook", ".ipynb", 
                        Arrays.asList(".ipynb"), Arrays.asList("\"nbformat\":", "\"cell_type\":"))
        );
    }
}