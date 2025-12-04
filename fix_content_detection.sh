#!/bin/bash

# Fix AsciiDoc content detection
sed -i '' '/AsciidocParserTest/,/detect format by content/{ /Sample AsciiDoc/c\
            = Document Title
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/asciidoc/AsciidocParserTest.kt

# Fix Creole content detection
sed -i '' '/CreoleParserTest/,/detect format by content/{ /Sample Creole/c\
            = Heading
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/creole/CreoleParserTest.kt

# Fix Jupyter content detection
sed -i '' '/JupyterParserTest/,/detect format by content/{ /Sample Jupyter/c\
            {"nbformat": 4, "cell_type": "code"}
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/jupyter/JupyterParserTest.kt

# Fix KeyValue content detection
sed -i '' '/KeyValueParserTest/,/detect format by content/{ /Sample KeyValue/c\
            key1 = value1\
            key2 = value2
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/keyvalue/KeyValueParserTest.kt

# Fix LaTeX content detection
sed -i '' '/LatexParserTest/,/detect format by content/{ /Sample LaTeX/c\
            \\documentclass{article}\
            \\begin{document}
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/latex/LatexParserTest.kt

# Fix Org Mode content detection
sed -i '' '/OrgModeParserTest/,/detect format by content/{ /Sample Org Mode/c\
            * Heading\
            #+TITLE: Document
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/orgmode/OrgModeParserTest.kt

# Fix reStructuredText content detection
sed -i '' '/RestructuredtextParserTest/,/detect format by content/{ /Sample reStructuredText/c\
            Title\
            =====\
            .. note:: content
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/restructuredtext/RestructuredtextParserTest.kt

# Fix R Markdown content detection
sed -i '' '/RmarkdownParserTest/,/detect format by content/{ /Sample RMarkdown/c\
            ---\
            title: "Document"\
            ---\
            ```{r}\
            x <- 1\
            ```
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/rmarkdown/RmarkdownParserTest.kt

# Fix TaskPaper content detection
sed -i '' '/TaskpaperParserTest/,/detect format by content/{ /Sample TaskPaper/c\
            Project:\
            \t- Task item
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/taskpaper/TaskpaperParserTest.kt

# Fix Textile content detection
sed -i '' '/TextileParserTest/,/detect format by content/{ /Sample Textile/c\
            h1. Heading\
            * List item
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/textile/TextileParserTest.kt

# Fix TiddlyWiki content detection
sed -i '' '/TiddlyWikiParserTest/,/detect format by content/{ /Sample TiddlyWiki/c\
            ! Heading\
            title: Document
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/tiddlywiki/TiddlyWikiParserTest.kt

# Fix WikiText content detection
sed -i '' '/WikitextParserTest/,/detect format by content/{ /Sample WikiText/c\
            == Heading ==\
            [[Link text]]
}' shared/src/commonTest/kotlin/digital/vasic/yole/format/wikitext/WikitextParserTest.kt

echo "Content detection samples updated"
