---
schema_version: 1
constitution_rule: CONST-035
last_audit: 2026-05-01
---

# Behavior Anchor Manifest — Yole (main)

Every row is a user-facing capability and the single anchor test that
proves it works end-to-end. See CONST-035 in `CONSTITUTION.md`.

## Status legend

- `active` — anchor exists and is callable; capability is verified.
- `pending-anchor` — capability declared, anchor test does not yet
  exist. Listed in `challenges/baselines/bluff-baseline.txt` Section 3.
  Reducing this state is the work of campaign sub-project 4.
- `retired` — capability removed; row kept for history.

## Path format

For Kotlin tests: `<path>.kt::<methodName>`. The challenge verifier
greps for `fun <methodName>\b` in the file. Methods with backticked
names (Kotlin's `\`spaces in identifier\`` form) are not currently
resolvable; use a `pending-anchor` row until a non-backticked test
method is available.

## Capabilities

| id | layer | capability | anchor_test_path | verifies | status |
|----|-------|------------|------------------|----------|--------|
| CAP-001 | app | Render asciidoc file to HTML wrapped in asciidoc div | shared/src/commonTest/kotlin/digital/vasic/yole/format/asciidoc/AsciidocParserHtmlTest.kt::testWrappedInAsciidocDiv | parser.parse + parser.toHtml output contains `<div class='asciidoc'>` wrapper | active |
| CAP-002 | app | Detect and tag binary file (no parser, detection only) | shared/src/commonTest/kotlin/digital/vasic/yole/format/binary/BinaryDetectionTest.kt::testBinaryFormatRegistered | binary format registration produces ID_BINARY entry without parser | pending-anchor |
| CAP-003 | app | Render Creole wiki to HTML wrapped in creole div | shared/src/commonTest/kotlin/digital/vasic/yole/format/creole/CreoleParserHtmlTest.kt::testWrappedInCreoleDiv | parser output contains `<div class='creole'>` wrapper | active |
| CAP-004 | app | Render CSV table to HTML wrapped in csv div | shared/src/commonTest/kotlin/digital/vasic/yole/format/csv/CsvParserHtmlTest.kt::testTableWrappedInCsvDiv | parsed CSV renders as HTML table inside `<div class='csv'>` | active |
| CAP-005 | app | Render Jupyter notebook to HTML wrapped in jupyter div | shared/src/commonTest/kotlin/digital/vasic/yole/format/jupyter/JupyterParserHtmlTest.kt::testWrappedInJupyterDiv | notebook cells render to HTML inside `<div class='jupyter'>` | active |
| CAP-006 | app | Render key-value file to HTML wrapped in keyvalue div | shared/src/commonTest/kotlin/digital/vasic/yole/format/keyvalue/KeyValueParserHtmlTest.kt::testWrappedInKeyvalueDiv | key-value parse renders as definition list in `<div class='keyvalue'>` | active |
| CAP-007 | app | Render LaTeX document to HTML with header | shared/src/commonTest/kotlin/digital/vasic/yole/format/latex/LatexParserHtmlTest.kt::testDocumentClassGeneratesHeader | LaTeX `\documentclass` produces HTML header | active |
| CAP-008 | app | Render Markdown to HTML wrapped in markdown div | shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/MarkdownParserHtmlTest.kt::testOutputWrappedInMarkdownDiv | parser output contains `<div class='markdown'>` wrapper and `</div>` end tag | active |
| CAP-009 | app | Render OrgMode level-1 heading to HTML | shared/src/commonTest/kotlin/digital/vasic/yole/format/orgmode/OrgModeParserHtmlTest.kt::testLevel1Heading | `* Heading` renders as `<h1>` element | active |
| CAP-010 | app | Render plain text to HTML wrapped in pre block | shared/src/commonTest/kotlin/digital/vasic/yole/format/plaintext/PlaintextParserHtmlTest.kt::testPlainTextWrapsInPreBlock | plain text wrapped in `<pre>` for monospace rendering | active |
| CAP-011 | app | Render reStructuredText to HTML wrapped in rst document div | shared/src/commonTest/kotlin/digital/vasic/yole/format/restructuredtext/RstParserHtmlTest.kt::testWrappedInRstDocumentDiv | parser output contains rst document wrapper | active |
| CAP-012 | app | Render R Markdown to HTML wrapped in rmarkdown div | shared/src/commonTest/kotlin/digital/vasic/yole/format/rmarkdown/RMarkdownParserHtmlTest.kt::testToHtmlContainsRmarkdownDiv | parser output contains `<div class='rmarkdown'>` wrapper | active |
| CAP-013 | app | Render TaskPaper to HTML wrapped in taskpaper div | shared/src/commonTest/kotlin/digital/vasic/yole/format/taskpaper/TaskpaperParserHtmlTest.kt::testWrappedInTaskpaperDiv | tasks/projects/notes wrapped in `<div class='taskpaper'>` | active |
| CAP-014 | app | Render Textile to HTML wrapped in textile div | shared/src/commonTest/kotlin/digital/vasic/yole/format/textile/TextileParserHtmlTest.kt::testWrappedInTextileDiv | textile syntax renders inside `<div class='textile'>` | active |
| CAP-015 | app | Render TiddlyWiki to HTML wrapped in tiddlywiki div | shared/src/commonTest/kotlin/digital/vasic/yole/format/tiddlywiki/TiddlyWikiParserHtmlTest.kt::testWrappedInTiddlywikiDiv | tiddler syntax renders inside `<div class='tiddlywiki'>` | active |
| CAP-016 | app | Render Todo.txt to HTML wrapped in todotxt div | shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserHtmlTest.kt::testWrappedInTodotxtDiv | todo lines render with priority/context tags inside `<div class='todotxt'>` | active |
| CAP-017 | app | Render WikiText to HTML wrapped in wikitext div | shared/src/commonTest/kotlin/digital/vasic/yole/format/wikitext/WikitextParserHtmlTest.kt::testWrappedInWikitextDiv | MediaWiki-style markup renders inside `<div class='wikitext'>` | active |
| CAP-018 | app | FormatRegistry exposes ID constants for all 17 core formats | shared/src/commonTest/kotlin/digital/vasic/yole/format/FormatRegistryUnitTest.kt::idConstantsExistForAllCoreFormats | Every format has a stable string ID constant on TextFormat.Companion | active |
| CAP-019 | app | TextFormat ID constants are unique across all formats | shared/src/commonTest/kotlin/digital/vasic/yole/format/TextFormatComprehensiveTest.kt::allFormatIdConstantsAreUnique | No two formats share the same ID — detection unambiguous | active |
| CAP-020 | app | Connect to Dropbox cloud storage with valid OAuth token | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxMockHttpTest.kt::01 connect succeeds with valid token and account info | DropboxStorageService.connect() against mock HTTP returns success and populates account info | active |
| CAP-021 | app | Connect to FTP server | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpServiceTest.kt::testConnectSuccess | FtpService.connect() returns success and toggles isOnline | active |
| CAP-022 | app | Connect to Git remote with valid refs | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitMockHttpTest.kt::connect with valid git refs sets connected | GitService.connect() against mock HTTP returns success and refs are reachable | active |
| CAP-023 | app | Connect to Google Drive with valid OAuth token | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveMockHttpTest.kt::testConnectWithValidTokenSucceeds | GoogleDriveService.connect() returns success and authenticates | active |
| CAP-024 | app | Connect to OneDrive with valid OAuth token | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveMockHttpTest.kt::testConnectWithValidTokenSucceeds | OneDriveService.connect() returns success and authenticates | active |
| CAP-025 | app | Connect to SFTP server | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/sftp/SftpServiceTest.kt::testConnectSuccess | SftpService.connect() returns success and toggles isOnline | active |
| CAP-026 | app | Connect to SMB share | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/smb/SmbServiceTest.kt::testConnectSuccess | SmbService.connect() returns success and toggles isOnline | active |
| CAP-027 | app | Connect to WebDAV server | shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavMockHttpTest.kt::connect succeeds with 200 OPTIONS response | WebDavService.connect() against mock HTTP returns success after 200 OPTIONS | active |

(Format layer + network-protocol layer complete. Subsequent iterations
of sub-project 3 add: KMP module anchors (handled in their own repos
since modules consume themselves), app-screen anchors, document model,
monitoring, and submodule cross-recheck.)
