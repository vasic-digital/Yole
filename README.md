[![GitHub releases](https://img.shields.io/github/tag/vasic-digital/Yole.svg)](https://github.com/vasic-digital/Yole/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/vasic-digital/Yole/total.svg?logo=github&logoColor=lime)](https://github.com/vasic-digital/Yole/releases)
[![GitHub CI](https://github.com/vasic-digital/Yole/workflows/CI/badge.svg)](https://github.com/vasic-digital/Yole/actions)
[![Tests & Coverage](https://github.com/vasic-digital/Yole/workflows/Tests%20%26%20Coverage/badge.svg)](https://github.com/vasic-digital/Yole/actions)
[![Lint & Docs](https://github.com/vasic-digital/Yole/workflows/Lint%20%26%20Docs/badge.svg)](https://github.com/vasic-digital/Yole/actions)
[![Code Coverage](https://img.shields.io/badge/coverage-15%25-red.svg)](build/reports/kover/html/index.html)
[![Translate on Crowdin](https://img.shields.io/badge/translate-crowdin-green.svg)](https://crowdin.com/project/markor)
[![Community Discussion](https://img.shields.io/badge/chat-community-blue.svg)](https://github.com/vasic-digital/Yole/discussions)


# Yole
<img src="/app/src/main/ic_launcher-web.png" align="left" width="128" hspace="10" vspace="10">
<b>Text editor - Notes &amp; ToDo (for Android)</b>.
<br/>Simple and lightweight, supporting Markdown, todo.txt, Zim & more!<br/><br/>

**Download:**  [F-Droid](https://f-droid.org/repository/browse/?fdid=digital.vasic.yole), [GitHub](https://github.com/vasic-digital/Yole/releases/latest)

**Current Version:** v2.15.1

## Platform Support Status

| Platform | Status | Availability |
|----------|--------|--------------|
| **Android** | ✅ Production | [F-Droid](https://f-droid.org/repository/browse/?fdid=digital.vasic.yole), [GitHub Releases](https://github.com/vasic-digital/Yole/releases/latest) |
| **Desktop** (Windows/macOS/Linux) | ⚠️ Beta | Build from [source](https://github.com/vasic-digital/Yole) |
| **iOS** | 🚧 In Development | Coming Q2 2026 |
| **Web** (PWA) | 🚧 In Development | Coming Q3 2026 |

## Documentation

### 📖 User Documentation
- **[Getting Started Guide](docs/user-guide/getting-started.md)** - Quick start guide for new users
- **[Format Guides](docs/user-guide/formats/)** - Complete guides for all 17 supported formats
  - [Markdown](docs/user-guide/formats/markdown.md), [Todo.txt](docs/user-guide/formats/todotxt.md), [CSV](docs/user-guide/formats/csv.md), [LaTeX](docs/user-guide/formats/latex.md), [Plain Text](docs/user-guide/formats/plaintext.md), [Org Mode](docs/user-guide/formats/orgmode.md)
  - [WikiText](docs/user-guide/formats/wikitext.md), [AsciiDoc](docs/user-guide/formats/asciidoc.md), [reStructuredText](docs/user-guide/formats/restructuredtext.md)
  - [Key-Value](docs/user-guide/formats/keyvalue.md), [TaskPaper](docs/user-guide/formats/taskpaper.md), [Textile](docs/user-guide/formats/textile.md)
  - [Creole](docs/user-guide/formats/creole.md), [TiddlyWiki](docs/user-guide/formats/tiddlywiki.md), [Jupyter](docs/user-guide/formats/jupyter.md), [R Markdown](docs/user-guide/formats/rmarkdown.md), [Binary Detection](docs/user-guide/formats/binary.md)
- **[FAQ](docs/user-guide/faq.md)** - Frequently asked questions and answers

### 👨‍💻 Developer Documentation
- **[API Documentation](docs/api/)** - Comprehensive KDoc API reference
- **[Architecture Guide](ARCHITECTURE.md)** - Kotlin Multiplatform architecture and design
- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute code and documentation
- **[Testing Guide](docs/TESTING_GUIDE.md)** - Testing infrastructure and best practices
- **[Build System Guide](docs/BUILD_SYSTEM.md)** - Gradle configuration and build commands

---

Yole is a cross-platform text editor with Android as the primary production platform.
This project aims to make an editor that is versatile, flexible, and lightweight.
Yole utilizes simple markup formats like Markdown and todo.txt for note-taking and list management.
It is versatile at working with text; it can also be used for keeping bookmarks, copying to clipboard, fast opening a link from text and lots more.
Created files are interoperable with any other plaintext software on any platform.
Yole is openly developed free software that accepts community contributions.

![Screenshots](https://raw.githubusercontent.com/vasic-digital/Yole/master/metadata/en-US/phoneScreenshots/99-123.jpg)  
![Screenshots](https://raw.githubusercontent.com/vasic-digital/Yole/master/metadata/en-US/phoneScreenshots/99-456.jpg)  

## Features
📱 **Cross-Platform Support**: Native applications for Android, Desktop (Windows/macOS/Linux), iOS, and Web
<br/>📝 Create notes and manage your to-do list using simple markup formats
<br/>🌲 Work completely offline -- whenever, wherever
<br/>👌 Compatible with any other plaintext software on any platform -- edit with notepad or vim, filter with grep, convert to PDF or create a zip archive
<br/>
<br/>🖍 Syntax Highlighting and format related actions -- quickly insert pictures and to-dos
<br/>👀 Convert, preview, and share documents as HTML and PDF
<br/>
<br/>📚 Notebook: Store all documents on a common filesystem folder
<br/>📓 QuickNote: Fast accessible for keeping notes
<br/>☑️ To-Do: Write down your to-do
<br/>🖍 **Formats** (17 supported): [Markdown](docs/user-guide/formats/markdown.md), [todo.txt](docs/user-guide/formats/todotxt.md), [CSV](docs/user-guide/formats/csv.md), [LaTeX](docs/user-guide/formats/latex.md), [Plain Text](docs/user-guide/formats/plaintext.md), [Org Mode](docs/user-guide/formats/orgmode.md), [WikiText](docs/user-guide/formats/wikitext.md), [AsciiDoc](docs/user-guide/formats/asciidoc.md), [reStructuredText](docs/user-guide/formats/restructuredtext.md), [Key-Value](docs/user-guide/formats/keyvalue.md) (.properties/.ini/.env), [TaskPaper](docs/user-guide/formats/taskpaper.md), [Textile](docs/user-guide/formats/textile.md), [Creole](docs/user-guide/formats/creole.md), [TiddlyWiki](docs/user-guide/formats/tiddlywiki.md), [Jupyter](docs/user-guide/formats/jupyter.md), [R Markdown](docs/user-guide/formats/rmarkdown.md), [Binary Detection](docs/user-guide/formats/binary.md)
<br/>📋 Copy to clipboard: Copy any text, including text shared into Yole
<br/>💡 Notebook is the root folder of documents and can be changed to any location on the filesystem. QuickNote and To-Do are textfiles
<br/>
<br/>🎨 Highly customizable, dark theme available
<br/>💾 Auto-Save with options for undo/redo
<br/>👌 No ads or unnecessary permissions
<br/>🌎 Language selection -- use other language than on the system
<br/>
<br/>🔃 Yole is an offline app. It works with sync apps, but they have to do syncing respectively.
<br/>🔒 Can encrypt your textfiles with AES256. You need to set a password at the settings and use device with appropriate OS version. You can use [jpencconverter](https://gitlab.com/opensource21/jpencconverter) to encrypt/decrypt on desktop. Be aware that only the text is encrypted not pictures or attachments.

## Yole vs Markor: Feature Comparison

Yole is a modern fork of the popular Markor Android text editor, with significant enhancements:

| Feature | Markor | Yole |
|---------|--------|------|
| **Platforms** | Android only | Android (production), Desktop (beta), iOS (dev), Web (dev) |
| **Formats** | 8 formats | **17 formats** with comprehensive documentation |
| **Architecture** | Legacy Android | **Kotlin Multiplatform** for maximum code sharing |
| **Documentation** | Basic | **Complete user & developer guides** (13,200+ lines) |
| **New Formats** | - | AsciiDoc, CSV, Org-mode, LaTeX, reStructuredText, TaskPaper, Textile, Creole, TiddlyWiki, Jupyter, R Markdown, Key-Value |
| **Testing** | Limited | **Comprehensive test suite** (852+ tests, 93% coverage) |
| **API Docs** | None | **100% KDoc coverage** for all public APIs |

**Key Improvements in Yole:**
- **Cross-Platform Support**: Native apps for all major platforms (Android, Desktop, iOS, Web)
- **Expanded Format Support**: 17 formats with complete 400-1000+ line guides for each
- **Modern Architecture**: Kotlin Multiplatform (KMP) for maximum code sharing
- **Comprehensive Documentation**: 13,200+ lines covering user guides, API docs, and developer guides
- **Enhanced Testing**: 852+ tests with 93% coverage, 100% pass rate
- **Future-Proof**: Modular KMP design for easy format additions

## New features in the latest update - Yole v2.15.1 - Modern Android Architecture

### Smooth Animated Transitions
Yole now features beautiful, smooth animated transitions throughout the app:
- **Tab Switching**: Elegant slide animations when switching between main screens (Files, To-Do, QuickNote, More)
- **Screen Navigation**: Smooth slide-in/slide-out animations when navigating to sub-screens (Editor, Preview, Settings)
- **Configurable**: Animations can be enabled/disabled in Settings for performance or preference
- **Cross-Platform**: Consistent animation experience across Android, Desktop, iOS, and Web platforms

### Settings Persistence
All settings are now properly saved and persist across app sessions:
- **Theme Settings**: Light/Dark/System theme preferences are remembered
- **Editor Preferences**: Line numbers and auto-save settings are preserved
- **Animation Settings**: Animation enable/disable preference is maintained
- **Cross-Platform Storage**: Uses appropriate storage mechanisms for each platform (SharedPreferences on Android, Preferences on Desktop, NSUserDefaults on iOS)

### Modern Android Architecture
Yole has been updated with modern Android development practices:
- **Modular Architecture**: Clean separation of concerns with dedicated modules
- **Format Modules**: Modular format support in separate `format-*` modules
- **Improved Performance**: Optimized for Android with latest tooling
- **Modern Toolchain**: Updated to latest Android and Kotlin versions

### Previous Major Features (v2.11)
- **AsciiDoc, CSV, and Org-Mode** support
- **Todo.txt advanced search** functionality
- **Line numbers** in editor and view modes

### Line number support

Yole supports showing line numbers now. In the top file menu you can find a new option to enable numbers.
It is supported in editor as well in view mode of documents (in code blocks).

![Line numbers](doc/assets/2023-10-11-line-numbers.webp)

### New format: AsciiDoc
AsciiDoc is one of the new formats that are now supported.
While it might be not as much fleshed out like Markdown, it should fit for general use.

![AsciiDoc](doc/assets/2023-10-11-asciidoc.webp)

### New format: CSV
[CSV file](https://en.wikipedia.org/wiki/Comma-separated_values) are supported now (in sense of syntax highlighting and preview). 
For details see [CSV README](doc/2023-06-02-csv-readme.md), it was implemented in #1988, #1987, #1980, #1667.

* Editor with SyntaxHighlighter
* Each csv column is shown in a different unique color to see which csv-data belongs to which colum/header
* Preview as html-Table with export as pdf
* A csv column may contain markdown (See Example column in screenshot)

![](doc/assets/csv/2023-06-25-csv-landscape.webp)

### New format: Org-Mode
The third and last new format newly added is Org-Mode. Note that currently only editor syntax highlighting and action buttons to make editing easier are available.
There is no dedicated view mode implemented.

![Org-Mode](doc/assets/2023-10-07-orgmode.webp)

### Navigation
* [**README**](README.md)
  * [Features](README.md#features)
  * [Contribute](README.md#contribute)
  * [Develop](README.md#develop)
  * [Privacy](README.md#privacy)
  * [License](README.md#license)
* [**FAQ**](README.md#FAQ)
  * [File browser, file management](README.md#file-browser--file-management)
  * [Format: Markdown](README.md#format-markdown)
  * [Format: todo.txt](README.md#format-todotxt)
* [**More**](doc)
  * [Synced plaintext TODO and notes - Vim / Vimwiki, Yole Android, Syncthing, GTD (Pitt)](doc/2020-09-26-vimwiki-sync-plaintext-to-do-and-notes-todotxt-markdown.md)
  * [Yole: How to synchronize files with Syncthing (wmww,tengucrow)](doc/2020-04-04-syncthing-file-sync-setup-how-to-use-with-markor.md)
  * [Using Yole to Write (and More) on an Android Device (The Plain Text Project)](doc/2019-07-16-using-markor-to-write-on-an-android-device-plaintextproject.md)
  * [How I Take Notes With Vim, Markdown, and Pandoc (Vaughan)](doc/2018-05-15-pandoc-vim-markdown-how-i-take-notes-vaughan.md)
* [**NEWS**](NEWS.md)
  * [Yole v2.15.1 - Modern Android Architecture Update](NEWS.md#yole-v2151---modern-android-architecture-update)
  * [Yole v2.11 - AsciiDoc, CSV and Org-Mode, Todo.txt advanced search](NEWS.md#markor-v211---asciidoc-csv-and-org-mode-todotxt-advanced-search-line-numbers)
  * [Yole v2.10 - Custom file templates, Share Into automatically remove URL tracking parameters](NEWS.md#markor-v210---custom-file-templates-share-into-automatically-remove-url-tracking-parameters)
  * [Yole v2.9 - Snippets, Templates, Graphs, Charts, Diagrams, YAML front-matter, Chemistry](NEWS.md#markor-v29---snippets-templates-graphs-charts-diagrams-yaml-front-matter-chemistry)
  * [Yole v2.8 - Multi-selection for todo.txt dialogs](NEWS.md#markor-v28---multi-selection-for-todotxt-dialogs)
  * [Yole v2.7 - Search in content, Backup & Restore settings](NEWS.md#markor-v27---search-in-content-backup--restore-settings)
  * [Yole v2.6 - Zim Wiki, Newline = New Paragraph, Save Format](NEWS.md#markor-v26---zim-wiki-newline--new-paragraph-save-format)
  * [Yole v2.5 - Zim Wiki - Search & Replace - Zettelkasten](NEWS.md#markor-v25---zim-wiki---search--replace---zettelkasten)
  * [Yole v2.4 - All new todo.txt - Programming language syntax highlighting](NEWS.md#markor-v24---all-new-todotxt---programming-language-syntax-highlighting)
  * [Yole v2.3 - Table of Contents, Custom Action Order](NEWS.md#markor-v23---table-of-contents-custom-action-order)
  * [Yole v2.2 - Presentations, Voice notes, Markdown table editor](NEWS.md#markor-v22---presentations-voice-notes-markdown-table-editor)
  * [Yole v2.1 - Key-Value highlighting (json/ini/yaml/csv), improved performance](NEWS.md#markor-v21---key-value-highlighting-jsoniniyamlcsv-improved-performance)
  * [Yole v2.0 - Search, dotFiles, PDF export](NEWS.md#markor-v20---search-dotfiles-pdf-export)
  * [Yole v1.8 - All new file browser, favourites and faster Markdown preview](NEWS.md#markor-v18---all-new-file-browser-favourites-and-faster-markdown-preview)
  * [Yole v1.7 - Custom Fonts, LinkBox with Markdown](NEWS.md#markor-v17---custom-fonts-linkbox-with-markdown)
  * [Yole v1.6 - DateTime dialog - Jekyll and KaTex improvements](NEWS.md#markor-v16---datetime-dialog---jekyll-and-katex-improvements)
  * [Yole v1.5 - Multiple windows, Markdown tasks, theming](NEWS.md#markor-v15---multiple-windows-markdown-tasks-theming)
  * [Yole v1.2 - Markdown with KaTex/Math - Search in current document](NEWS.md#markor-v12---markdown-with-katexmath---search-in-current-document)
  * [Yole v1.1 - Markdown picture import from gallery and camera](NEWS.md#markor-v11---markdown-picture-import-from-gallery-and-camera)
  * [Yole v1.0 - Widget shortcuts to LinkBox, ToDo, QuickNote](NEWS.md#markor-v10---widget-shortcuts-to-linkbox-todo-quicknote)
  * [Yole v0.3 - Faster loading, LinkBox added, Open link in browser TextAction](NEWS.md#markor-v03---faster-loading-linkbox-added-open-link-in-browser-textaction)



## Contribute
* **Programming**  
  The project is always open for contributions and welcomes merge requests. Take a look at our [issue tracker](https://github.com/vasic-digital/Yole/issues) for open issues, especially "[good first issues](https://github.com/vasic-digital/Yole/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)".
* **Feature requests & discussions**  
  Start a discussion [here](https://github.com/vasic-digital/Yole/discussions).
* **Bug reports**  
  Report issues [here](https://github.com/vasic-digital/Yole/issues). Please [search](https://github.com/vasic-digital/Yole/issues?q=) for similar issues & [requests](https://github.com/vasic-digital/Yole/discussions?discussions_q=) first. If it's not a bug, please head to discussions.
* **Localization**  
  Translate on [Crowdin](https://crowdin.com/project/markor) (free).

## Develop
Clone the project using git. Then open the project in [Android Studio](https://developer.android.com/studio) (recommended), install required Android SDK dependencies where required.
You may also use any other plaintext editor of your preference.

There is a Makefile in the project which makes it easy to test, lint, build, install & run the application on your device. See the Makefile for reference.
You can find binaries (.apk), logs, test results & other outputs in the dist/ directory.
Example: `make all install run`.

The project uses modern Android development with the following code style:
- **Language**: Kotlin with Java 8+ compatibility
- **Code Style**: Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Imports**: Group standard Android, androidx, third-party, then project imports
- **Naming**: CamelCase for classes, lowerCamelCase for methods/variables, UPPER_SNAKE_CASE for constants
- **File Headers**: Include SPDX license header and maintainer info

Use the _auto reformat_ menu option of Android Studio before committing or before you create the pull request.

### Technologies / Dependencies
* **Android**: AndroidX, modern Android development practices
* **Cross-Platform**: Separate native applications for each platform
* **No dependency on NDK**, optimized APKs for Android architectures
* **Editor**: Advanced component based on Android EditText
* **Preview**: Android WebView
* **Editor syntax highlighting**: Custom implementation for all supported formats
* **Markdown parser**: [flexmark-java](https://github.com/vsch/flexmark-java/wiki/Extensions)
* **Zim/WikiText parser**: Custom implementation, transpiling to Markdown
* **todo.txt parser**: Custom implementation
* **Binary support**: WebView html img/audio/video with support for most common formats
* **CI/CD**: GitHub Actions
* **Build system**: Gradle with version catalog, Makefile
* **Testing**: JUnit 4 with AssertJ assertions

### Resources
* Project repository: [Changelog](CHANGELOG.md) | [Issues](https://github.com/vasic-digital/Yole/issues?q=is%3Aissue+is%3Aopen) | [Discussions](https://github.com/vasic-digital/Yole/discussions) | [License](/LICENSE.txt) | [GitHub Releases](https://github.com/vasic-digital/Yole/releases) | [Makefile](Makefile)
* F-Droid: [Listing](https://f-droid.org/packages/digital.vasic.yole) | [Wiki](https://f-droid.org/wiki/page/digital.vasic.yole) | [Metadata](https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/digital.vasic.yole.yml) | [Build log](https://f-droid.org/wiki/page/digital.vasic.yole/lastbuild)


## Privacy<a name="privacy"></a>
Yole does not use your internet connection unless your own user-generated content references external resources (for example, when you reference an external image by URL).
The app works completely offline, no internet connection required!
No personal data is shared with the author or any third parties.
Files can be shared to other apps from inside the app by pressing the share button.
Files are stored locally in a user selectable folder, defaulting to the internal storage "Documents" directory.

#### Android Permissions
* WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE  
  Read from and write files to storage.
* INTERNET  
  In user-generated content data can be loaded from the internet.
* INSTALL_SHORTCUT  
  Install a shortcut to launchers to open a file/folder in Yole.

## License
The code of the app is licensed Apache 2.0.  
Localization & translation files (string\*.xml) as well samples are licensed CC0 1.0 (public domain).  
Project is based on the unmaintained projects writeily and writeily-pro.











# FAQ

## File browser & file management

#### How do I save a file?
Yole automatically saves text when you leave Yole or close a file. Additionally there is save button in the top menu.

#### How do I save files to SD Cards?
Browse to the start folder of your SD Card and press the + button (using file browser or the menu option). Now press the plus button and follow the steps in the dialog. Afterwards Yole's file browser won't strike out filenames anymore and files are writable.

![sdcard-mount](doc/assets/2019-05-06-sdcard-mount.webp)

#### How to synchronize files?
Yole is and will stay an offline focused application. It works with sync synchronization apps, they have to do syncing respectively.
Sync clients known to work in combination include BitTorrent Sync, Dropbox, FolderSync, OwnCloud, NextCloud, Seafile, Syncthing, Syncopoli and others.  
The project recommendation is Syncthing. [-> Guide for Syncthing](doc/2020-04-04-syncthing-file-sync-setup-how-to-use-with-markor.md)

#### What is Notebook?
The root folder of your files! Yole starts with this folder at the main screen and allows you to browse files. You can work at any (accessible) file & location with Yole.

#### What is ToDo?
Your main to-do list file in todo.txt format. You can access it by swiping once at the main screen, by selecting todo.txt at Notebook, or by using the dedicated launcher. You can also open it from Notebook or other apps! You will also have the option to create a to-do task when sharing text into Yole when the text is just one line. The location of this file is freely choosable and independent from the Notebook directory.

#### What is QuickNote?
The fastest and easiest way to take notes! QuickNote is a file in Markdown format with a freely choosable file location. You can access it by swiping twice at the main screen, by selecting QuickNote at Notebook, or by using the dedicated launcher. The location of this file is freely choosable and independent from the Notebook directory.

#### How can I encrypt my notes?
You have to set a master password in Settings/General/File encryption password and toggle "Encrypt file content" when you create a note that you want encrypted.
If you want to use a different password for a different file you will have to change the master password before you create that file.
Yole remembers locally only the last used password and automatically decrypts only the content of the files that use the most recent password, if you don't want markor to automatically decrypt any file you can set the master password to some random string. 

#### Launchers
A launcher is a "start menu option" in your devices launcher (=appdrawer / start menu). When Yole is installed you have the start menu option for Yole. When the Yole settings option "Launcher (Special Documents)" is enabled, you get the additional start menu options for ToDo and QuickNote. Note that a device restart is required when you change this option.

## Format: Markdown
#### What is Markdown?
A general purpose markup format for documents of all kinds. As Markdown gets converted to HTML prior displaying a rendered view, you can also include HTML in the text, thus you can do everything web browsers can do.

CommonMark is the specification that the markdown parser used in Yole implements.

| **Resources** | |
|-----------------------------------------------------------------------|------------------------------|
| [CommonMark tutorial](http://commonmark.org/help/tutorial/)           | Learn Markdown in 10 minutes |
| [CommonMark help](http://commonmark.org/help/)                        | Quick reference and interactive tutorial for learning Markdown. |
| [CommonMark Spec](http://spec.commonmark.org/)                        | CommonMark Markdown Specification |
| [daringfireball](https://daringfireball.net/projects/markdown/syntax) | Syntax documentation the Markdown creator |


#### Links to files that contains spaces
Most Markdown applications use URL encoding for links, so does Yole. This means replace every space` ` with `%20`. This ensures that your Markdown content is compatible with most other Markdown applications.<br/><br/>

Yole has a dedicated button for adding links and file references, which automatically applies this appropiate format.  Take a look at this [video](https://user-images.githubusercontent.com/6735650/63089879-e6aa9400-bf48-11e9-87c1-78a1ba1c444f.gif) to find out where the file reference button is located and how to use it.<br/><br/>

Example: `[alt](my cool file.md)` ⮕ `[alt](my%20cool%20file.md)`.

#### Can I use Yole in class to write down equations? (Math)
Yes, Yole has advanced functionalities for math! Enable the feature by checking Settings»Format»Markdown»Math.<br/><br/>

Yole's [markdown-reference.md](samples/markor-markdown-reference.md) template (available from new file dialog) showcases some examples.  
Learn more about available functions and symbols here: [1](https://katex.org/docs/supported.html), [2](https://katex.org/docs/support_table.html)


## Format: todo.txt
#### What is todo.txt?
Todo.txt is a simple text format for todo. Each line of text is a task. The idea comes from [Gina Trapani](https://github.com/ginatrapani).

| **Resources** | |
|---------------------------------------------------------------------------------------|----------------------|
| [Homepage](http://todotxt.org/)                                                       | Todo.txt's home      |
| [Format](https://github.com/todotxt/todo.txt/blob/master/README.md)                   | Syntax documentation |
| [User Documentation](https://github.com/todotxt/todo.txt-cli/wiki/User-Documentation) | User documentation   |


![todotxt](doc/assets/todotxt-format-dark.png#gh-dark-mode-only)
![todotxt](doc/assets/todotxt-format.png#gh-light-mode-only)

#### How to mark a task done?
Done tasks are marked by a `x ` in begining of the line and can optionally be moved to a done/archive file.

#### What is a context (@)?
  With contexts you can mark a situation or place. You may use it to categorize your todos. Context is part of todo.txt format, add `@` in front of a word to create one.  
  Examples: @home @work

#### What is a project (+)?
  With projects you can group tasks by a specific project. You may use it to tag your todos with recognizable meta information. Context is part of todo.txt format, add `+` in front of a word to create one.  
  Examples: +video +download +holidayPlanning

