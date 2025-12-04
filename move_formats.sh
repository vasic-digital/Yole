#!/bin/bash

# List of formats to move
declare -A FORMATS=(
    ["format-todotxt"]="todotxt"
    ["format-csv"]="csv"
    ["format-wikitext"]="wikitext"
    ["format-keyvalue"]="keyvalue"
    ["format-asciidoc"]="asciidoc"
    ["format-orgmode"]="orgmode"
    ["format-plaintext"]="plaintext"
)

# Move existing formats
for module in "${!FORMATS[@]}"; do
    format=${FORMATS[$module]}
    echo "Moving $format to $module..."
    mkdir -p "$module/src/main/java/digital/vasic/yole/format/$format"
    cp -r "app/src/main/java/digital/vasic/yole/format/$format/"* "$module/src/main/java/digital/vasic/yole/format/$format/" 2>/dev/null || true
done

# Create new format implementations
NEW_FORMATS=(
    "latex"
    "restructuredtext"
    "taskpaper"
    "textile"
    "creole"
    "tiddlywiki"
    "jupyter"
    "rmarkdown"
)

for format in "${NEW_FORMATS[@]}"; do
    module="format-$format"
    echo "Creating skeleton for $format in $module..."
    mkdir -p "$module/src/main/java/digital/vasic/yole/format/$format"

    # Create basic format classes
    cat > "$module/src/main/java/digital/vasic/yole/format/$format/${format^}TextConverter.java" << EOF
package digital.vasic.yole.format.$format;

import digital.vasic.yole.format.TextConverterBase;
import android.content.Context;
import java.io.File;

public class ${format^}TextConverter extends TextConverterBase {
    @Override
    public boolean isFileOutOfThisFormat(File file) {
        // TODO: Implement file detection logic
        return false;
    }

    @Override
    public String convertMarkupToHtml(String markup, Context context, boolean lightMode, int lineNum, File file) {
        // TODO: Implement markup to HTML conversion
        return markup;
    }
}
EOF

    cat > "$module/src/main/java/digital/vasic/yole/format/$format/${format^}SyntaxHighlighter.java" << EOF
package digital.vasic.yole.format.$format;

import digital.vasic.yole.frontend.textview.SyntaxHighlighterBase;
import digital.vasic.yole.model.AppSettings;

public class ${format^}SyntaxHighlighter extends SyntaxHighlighterBase {
    public ${format^}SyntaxHighlighter(AppSettings appSettings) {
        super(appSettings);
    }
}
EOF

    cat > "$module/src/main/java/digital/vasic/yole/format/$format/${format^}ActionButtons.java" << EOF
package digital.vasic.yole.format.$format;

import digital.vasic.yole.format.ActionButtonBase;
import digital.vasic.yole.model.Document;
import android.content.Context;

public class ${format^}ActionButtons extends ActionButtonBase {
    public ${format^}ActionButtons(Context context, Document document) {
        super(context, document);
    }
}
EOF
done

echo "Format migration completed!"