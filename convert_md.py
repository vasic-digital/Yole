import markdown
import os

def convert_md_to_html(md_file, html_file, title, nav_home, nav_guides, nav_readme, style_path):
    with open(md_file, 'r') as f:
        md_content = f.read()
    
    html_content = markdown.markdown(md_content)
    
    template = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Yole - {title}</title>
    <link rel="stylesheet" href="{style_path}">
</head>
<body>
    <nav>
        <a href="{nav_home}">Home</a> |
        <a href="{nav_guides}">Guides</a> |
        <a href="{nav_readme}">README</a>
    </nav>

    <main style="padding: 20px;">
        {html_content}
    </main>

    <footer>
        <p>&copy; 2025 Yole Project</p>
    </footer>
</body>
</html>"""
    
    with open(html_file, 'w') as f:
        f.write(template)
    
    with open(html_file, 'w') as f:
        f.write(template)

# List of MD files and titles with nav paths
files = [
    ('README.md', 'docs/README.html', 'README', 'index.html', 'QUICK_START.html', 'ARCHITECTURE.html', 'style.css'),
    ('ARCHITECTURE.md', 'docs/ARCHITECTURE.html', 'Architecture', 'index.html', 'README.html', 'QUICK_START.html', 'style.css'),
    ('doc/2023-06-02-csv-readme.md', 'docs/doc/2023-06-02-csv-readme.html', 'CSV Format Guide', '../index.html', 'README.html', '../README.html', '../style.css'),
    ('doc/2020-09-26-vimwiki-sync-plaintext-to-do-and-notes-todotxt-markdown.md', 'docs/doc/2020-09-26-vimwiki-sync-plaintext-to-do-and-notes-todotxt-markdown.html', 'VimWiki Sync Guide', '../index.html', 'README.html', '../README.html', '../style.css'),
    ('doc/2020-04-04-syncthing-file-sync-setup-how-to-use-with-markor.md', 'docs/doc/2020-04-04-syncthing-file-sync-setup-how-to-use-markor.html', 'Syncthing Setup', '../index.html', 'README.html', '../README.html', '../style.css'),
    ('doc/2019-07-16-using-markor-to-write-on-an-android-device-plaintextproject.md', 'docs/doc/2019-07-16-using-markor-to-write-on-an-android-device-plaintextproject.html', 'Android Writing Guide', '../index.html', 'README.html', '../README.html', '../style.css'),
    ('doc/2018-05-15-pandoc-vim-markdown-how-i-take-notes-vaughan.md', 'docs/doc/2018-05-15-pandoc-vim-markdown-how-i-take-notes-vaughan.html', 'Pandoc Integration', '../index.html', 'README.html', '../README.html', '../style.css'),
    ('FORMAT_DOCUMENTATION.md', 'docs/FORMAT_DOCUMENTATION.html', 'Format Documentation', 'index.html', 'doc/README.html', 'README.html', 'style.css'),
    ('QUICK_START.md', 'docs/QUICK_START.html', 'Quick Start', 'index.html', 'doc/README.html', 'README.html', 'style.css'),
    ('TESTING_STRATEGY.md', 'docs/TESTING_STRATEGY.html', 'Testing Strategy', 'index.html', 'doc/README.html', 'README.html', 'style.css'),
    ('CHANGELOG.md', 'docs/CHANGELOG.html', 'Changelog', 'index.html', 'doc/README.html', 'README.html', 'style.css')
]

for md, html, title, nav_home, nav_guides, nav_readme, style_path in files:
    convert_md_to_html(md, html, title, nav_home, nav_guides, nav_readme, style_path)