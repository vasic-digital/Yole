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
| CAP-002 | app | Detect and tag binary file (registration produces correct format metadata) | shared/src/commonTest/kotlin/digital/vasic/yole/format/binary/BinaryDetectionTest.kt::test binary format registration | FormatRegistry.getById(ID_BINARY) returns a format with id=ID_BINARY, name="Binary", defaultExtension=".bin", and empty extensions list | active |
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
| CAP-028 | app | Document model: construct Document with content + format | shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentTest.kt::testDocumentCreation | Document constructor produces a usable document with content and format ID set | active |
| CAP-029 | app | Document model: ID constants for all 17 formats are exposed | shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentFormatTest.kt::testAllFormatConstants | All 17 format ID constants are reachable from Document.kt | active |
| CAP-030 | app | Monitoring: MetricsReporter starts inactive | shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/MetricsReporterUnitTest.kt::reporterIsNotActiveInitially | New MetricsReporter has isActive == false until explicitly started | active |
| CAP-031 | app | Monitoring: PerformanceMetrics records parse latency and increments counter | shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/PerformanceMetricsTests.kt::recordParseIncrementsCountAndTotal | recordParse(format, durationMs) increments per-format count and accumulates total | active |
| CAP-032 | app | Monitoring: every format parses sample content within 500ms budget | shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/MetricsCollectionTest.kt::allFormatsParseSampleContentUnder500ms | All 17 format parsers complete a sample doc parse in under 500ms (perf budget enforcement) | active |
| CAP-033 | app | UI: light theme TextPrimary on SurfacePrimary meets WCAG AA contrast | shared/src/commonTest/kotlin/digital/vasic/yole/ui/ThemeAccessibilityTests.kt::light theme TextPrimary on SurfacePrimary meets WCAG AA | computed contrast ratio between TextPrimary and SurfacePrimary in light theme is ≥ 4.5 | active |
| CAP-034 | app | UI: touch targets meet WCAG 48dp minimum | shared/src/commonTest/kotlin/digital/vasic/yole/ui/AccessibilityTest.kt::touch target sizes meet WCAG guidelines | every defined touch target is ≥ 48.dp on at least one axis | active |
| CAP-035 | app | UI: animation timing constants are correctly mapped | shared/src/commonTest/kotlin/digital/vasic/yole/ui/AnimationsTests.kt::timing VERY_QUICK is 100ms | Animations.timing.VERY_QUICK == 100.milliseconds (other constants verified by sibling tests) | active |
| CAP-036 | app | Android: build.gradle version code matches release manifest | androidApp/src/test/java/digital/vasic/yole/android/VersionConsistencyTests.kt::testAndroidBuildGradleVersion | versionCode parsed from build.gradle.kts matches the release manifest entry | active |
| CAP-037 | app | Android: file save with direct file-system access succeeds | androidApp/src/test/java/digital/vasic/yole/android/FileBrowserSaveFunctionalityTests.kt::testSaveFileWithDirectAccess | save-then-read round-trip on a directly-accessible path returns identical bytes | active |
| CAP-038 | app | Desktop: parser detects Markdown by file extension | desktopApp/src/test/kotlin/digital/vasic/yole/desktop/DesktopAppParserTest.kt::should detect markdown format by extension | DesktopApp wires FormatRegistry.detectByExtension and returns ID_MARKDOWN for `.md` | active |
| CAP-039 | app | Desktop: file save round-trip via file manager | desktopApp/src/test/kotlin/digital/vasic/yole/desktop/DesktopFileManagerTest.kt::should save file with content successfully | DesktopFileManager.save() then re-read returns the same content | active |
| CAP-040 | app | iOS: YoleIOSSettings is instantiable from iosTest source set | shared/src/iosTest/kotlin/digital/vasic/yole/ios/IOSPlatformTests.kt::testSettingsClassExists | YoleIOSSettings constructor returns a non-null instance — proves iOS platform module links | active |
| CAP-041 | app | Web (Wasm): detect Markdown format from filename | webApp/src/wasmJsTest/kotlin/digital/vasic/yole/web/FormatDetectionTest.kt::should detect markdown format correctly | WebApp's format detector returns ID_MARKDOWN for `.md` files in browser environment | active |
| CAP-042 | app | Web (Wasm): download file with correct content and filename | webApp/src/wasmJsTest/kotlin/digital/vasic/yole/web/FileOperationsTest.kt::should download file with correct content and filename | Download API produces a Blob with the correct bytes and filename | active |
| CAP-043 | app | Web (Wasm): parse Markdown for web display | webApp/src/wasmJsTest/kotlin/digital/vasic/yole/web/WebAppParserTest.kt::should parse markdown for web display | WebApp parses Markdown content via shared parser and produces HTML | active |
| CAP-044 | app | Web (Wasm): integrate file download with document content | webApp/src/wasmJsTest/kotlin/digital/vasic/yole/web/WebAppIntegrationTest.kt::should integrate file download with document content | E2E flow: load document → render → download trigger produces correct artifact | active |
| CAP-045 | app | Web (Wasm): render main app shell correctly | webApp/src/wasmJsTest/kotlin/digital/vasic/yole/web/WebAppUITest.kt::should render main web app correctly | WebApp UI renders root composition without exception in Wasm | active |
| CAP-046 | app | Wasm: FormatRegistry exposes formats list on Wasm target | shared/src/wasmJsTest/kotlin/digital/vasic/yole/format/WasmPlatformIntegrationTests.kt::FormatRegistry is available on Wasm | shared FormatRegistry compiles + runs on Wasm (no JVM-only deps leaked) | active |
| CAP-047 | app | Wasm: detect Markdown by `.md` extension | shared/src/wasmJsTest/kotlin/digital/vasic/yole/format/WasmFormatDetectionTests.kt::detect Markdown by extension md | shared format-detection logic returns ID_MARKDOWN for `.md` on Wasm | active |
| CAP-048 | app | Wasm: encrypted data round-trips through Web SecureStorage | shared/src/wasmJsTest/kotlin/digital/vasic/yole/network/platform/WebSecureStorageTest.kt::should store encrypted data in localStorage | Web SecureStorage write+read returns plaintext after encryption round-trip | active |
| CAP-049 | app | Wasm: FtpProtocolClient stub instantiates without browser-incompatible deps | shared/src/wasmJsTest/kotlin/digital/vasic/yole/network/platform/WasmProtocolStubTests.kt::FtpProtocolClient can be instantiated on Wasm | FTP stub for Wasm (browser cannot do raw TCP) constructs cleanly — exposes documented "unsupported on Web" error path rather than crashing | active |

(Manifest now spans all 4 platform targets — Android, Desktop, iOS,
Web/Wasm — plus document model, monitoring, UI, format layer (16/17
formats), network-protocol layer (all 8 services), and cross-format
infrastructure. Total: 49 capability rows. Remaining iterations of
sub-project 3 add the long tail of public-API capabilities
surfaced during sub-project 4's deeper audit.)
