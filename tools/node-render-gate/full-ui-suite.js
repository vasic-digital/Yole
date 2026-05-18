// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
//
// iter-85 phase 2: Web Wasm full-UI anti-bluff suite.
//
// Complements render-gate.js (which checks RENDERING quality) with
// FUNCTIONAL checks that the actual semantic UI elements are present
// and reachable. Compose Multiplatform's Wasm runtime exposes a
// proper accessibility tree, so we can assert that every expected
// toolbar button / sidebar region / editor surface / preview / status-
// bar element exists by NAME — not by pixel position. A missing button
// fails LOUDLY here in a way the render gate (which only knew "canvas
// has color variance") never could.
//
// What it does:
//   1. Navigate to TARGET_URL.
//   2. Wait for splash to hide.
//   3. Walk the accessibility tree.
//   4. Assert every expected element is present by role + name.
//   5. Capture full-page screenshot + accessibility-tree JSON dump
//      as evidence at qa-results/iter-85/full-ui-suite/.
//
// Exit codes:
//   0 = all assertions PASSed.
//   1 = environmental failure (browser launch, navigation timeout).
//   2 = assertion failed — at least one expected element missing.
//
// Usage:
//   node full-ui-suite.js http://localhost:18080
//   node full-ui-suite.js https://yole-app.web.app

const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const TARGET_URL = process.argv[2] || 'http://localhost:18080';
const OUT_DIR = path.resolve(__dirname, '../../qa-results/iter-85/full-ui-suite');
const VIEWPORT = { width: 1280, height: 800 };

// Expected UI inventory — every entry MUST be present in the a11y tree
// for the gate to PASS. Adding a new feature to Yole's web UI means
// adding a row here in the same commit.
const EXPECTED = [
    // Top toolbar (left cluster)
    { role: 'StaticText', name: 'YOLE', description: 'app logo' },
    { role: 'button', name: 'File button', description: 'File menu' },
    { role: 'button', name: 'Open button', description: 'Open file' },
    { role: 'button', name: 'Save button', description: 'Save current document' },
    { role: 'button', name: 'Save As button', description: 'Save As dialog' },
    { role: 'button', name: 'Find button', description: 'Find in document' },
    { role: 'button', name: 'Go To button', description: 'Go to line' },
    { role: 'button', name: 'Preview button', description: 'Toggle preview pane' },
    { role: 'button', name: 'Explorer button', description: 'Toggle explorer sidebar' },
    { role: 'button', name: 'Export button', description: 'Export document' },
    { role: 'button', name: 'Print button', description: 'Print document' },
    // Top toolbar (right cluster)
    { role: 'button', name: 'Toggle theme', description: 'Theme toggle' },
    { role: 'button', name: 'Settings button', description: 'Settings panel' },
    // Sidebar
    { role: 'StaticText', name: 'EXPLORER', description: 'explorer pane header' },
    { role: 'button', name: 'New document', description: 'create-new in sidebar' },
    { role: 'StaticText', name: 'OPEN EDITORS', description: 'open editors section' },
    // Editor + tab
    { role: 'textbox', name: 'Code editor', description: 'main editor textbox' },
    // Preview pane label
    { role: 'StaticText', name: 'PREVIEW', description: 'preview pane header' },
    // Status bar
    { role: 'StaticText', name: 'localStorage', description: 'storage indicator' },
];

// Negative assertions — content that MUST NOT appear in the a11y tree.
// Catches regressions where internal data leaks into the user-visible
// surface. Each entry is a substring match.
//
// iter-85 phase-1 forensic case: the literal CSS stylesheet body
// `.markdown { font-family: -apple-system ... }` was leaking from
// StyleSheets.MARKDOWN_STYLES through parser.toHtml() into the preview
// pane because parseHtmlBlocks() in Main.kt didn't recognize <style>
// blocks. Fixed by stripping <style>/<script>/<link>/<meta>/comments
// in parseHtmlBlocks before block-level extraction.
//
// iter-85 phase-3 forensic case: even with the CSS leak gone, all
// markdown blocks were collapsed into a single concatenated text node
// "Welcome to YoleA professional IDE-style text editor.Features
// Multi-tab editing..." because the outer <div class='markdown'>
// wrapper was being treated as a flat container that swallowed all
// nested structure. Fixed by peeling the outer div + recursing into
// div content + extracting each <li> as its own block. The two
// "concatenation" forbiddens below catch that regression.
const FORBIDDEN_TEXT = [
    { substring: '.markdown { font-family:', description: 'raw CSS leaking from <style> into preview pane' },
    { substring: '.markdown h1 { font-size:', description: 'raw CSS leaking from <style> into preview pane' },
    { substring: 'YoleA professional', description: 'preview blocks concatenated without separation (iter-85-phase-3 regression)' },
    { substring: 'FeaturesMulti-tab', description: 'preview list items concatenated without bullets/separation (iter-85-phase-3 regression)' },
];

// Preview-structure assertions — content that MUST appear as INDIVIDUAL
// a11y nodes (not a concatenated blob). Each entry is an exact-string
// match against the leaf text of a node. The default welcome.md content
// authored in EnhancedWebApp.kt drives these — if you change the seed
// document, update both at once.
const PREVIEW_REQUIRED = [
    { name: 'Welcome to Yole', description: 'h1 heading rendered as standalone node' },
    { name: 'A professional IDE-style text editor.', description: 'first paragraph standalone' },
    { name: 'Features', description: 'h2 heading standalone' },
    { name: '• Multi-tab editing', description: 'first list item with bullet' },
    { name: '• 17+ text format support', description: 'second list item with bullet' },
    { name: '• Live preview', description: 'third list item with bullet' },
    { name: '• Offline support (PWA)', description: 'fourth list item with bullet' },
    { name: 'Create a new document or start editing here.', description: 'closing paragraph standalone' },
];

function log(level, msg) {
    console.log(`[full-ui-suite][${level}] ${msg}`);
}

// Flatten the a11y tree into a list for matching. We use a tolerant
// match: role matches if equal OR if the actual role is a superset of
// the expected role (so 'button' matches a 'tab role="button"').
function flatten(node, out = []) {
    if (!node) return out;
    out.push({ role: node.role || '', name: (node.name || '').trim() });
    for (const child of node.children || []) flatten(child, out);
    return out;
}

async function main() {
    log('INFO', `Target URL: ${TARGET_URL}`);
    fs.mkdirSync(OUT_DIR, { recursive: true });

    let browser;
    try {
        browser = await puppeteer.launch({
            headless: true,
            args: [
                '--no-sandbox', '--disable-setuid-sandbox',
                '--enable-unsafe-swiftshader', '--use-gl=swiftshader',
            ],
            defaultViewport: VIEWPORT,
        });
    } catch (err) {
        log('ENV-FAIL', `browser launch failed: ${err.message}`);
        process.exit(1);
    }

    const consoleErrors = [];
    try {
        const page = await browser.newPage();
        page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); });
        page.on('pageerror', err => consoleErrors.push(`uncaught: ${err.message}`));

        try {
            await page.goto(TARGET_URL, { waitUntil: 'load', timeout: 30000 });
        } catch (err) {
            log('ENV-FAIL', `navigation failed: ${err.message}`);
            await browser.close();
            process.exit(1);
        }

        // Wait for splash to hide
        log('INFO', 'Waiting for splash to hide...');
        try {
            await page.waitForFunction(
                () => {
                    const l = document.getElementById('loading');
                    return !l || window.getComputedStyle(l).display === 'none';
                },
                { timeout: 30000, polling: 100 },
            );
            log('OK', 'splash hidden');
        } catch (e) {
            log('WARN', 'splash never hid — proceeding anyway');
        }
        // Settle a beat for Compose to finish painting after splash fade
        await new Promise(r => setTimeout(r, 1000));

        // Snapshot the accessibility tree
        log('INFO', 'Snapshotting accessibility tree...');
        const tree = await page.accessibility.snapshot();
        const flat = flatten(tree);
        fs.writeFileSync(
            path.join(OUT_DIR, 'a11y-tree.json'),
            JSON.stringify(tree, null, 2),
        );
        log('INFO', `flattened to ${flat.length} a11y nodes`);

        // Screenshot for evidence
        const ssPath = path.join(OUT_DIR, 'full-ui.png');
        await page.screenshot({ path: ssPath, fullPage: false });
        log('INFO', `screenshot: ${ssPath}`);

        // Assert each expected element
        log('INFO', `Asserting ${EXPECTED.length} expected UI elements...`);
        const missing = [];
        const found = [];
        for (const exp of EXPECTED) {
            const hit = flat.find(n =>
                n.role.toLowerCase() === exp.role.toLowerCase() &&
                n.name === exp.name
            );
            if (hit) {
                found.push(exp);
                log('OK', `  ✓ ${exp.role} "${exp.name}" (${exp.description})`);
            } else {
                missing.push(exp);
                log('FAIL', `  ✗ MISSING: ${exp.role} "${exp.name}" (${exp.description})`);
            }
        }

        // Negative assertion: scan a11y tree for forbidden substrings.
        log('INFO', `Asserting ${FORBIDDEN_TEXT.length} forbidden text patterns are absent...`);
        const leaks = [];
        for (const forbidden of FORBIDDEN_TEXT) {
            const hit = flat.find(n => n.name.includes(forbidden.substring));
            if (hit) {
                leaks.push({ ...forbidden, hitName: hit.name.substring(0, 120) });
                log('FAIL', `  ✗ LEAK DETECTED: substring "${forbidden.substring}" found in a11y node "${hit.name.substring(0, 80)}..." — ${forbidden.description}`);
            } else {
                log('OK', `  ✓ absent: "${forbidden.substring}" (${forbidden.description})`);
            }
        }

        // Preview-structure assertion: each required block MUST appear as its
        // OWN standalone a11y node, proving the markdown was rendered into
        // distinct Compose Text composables (not a concatenated blob).
        log('INFO', `Asserting ${PREVIEW_REQUIRED.length} preview-pane standalone nodes...`);
        const previewMissing = [];
        for (const req of PREVIEW_REQUIRED) {
            const hit = flat.find(n => n.name === req.name);
            if (hit) {
                log('OK', `  ✓ standalone: "${req.name}" (${req.description})`);
            } else {
                previewMissing.push(req);
                log('FAIL', `  ✗ NOT STANDALONE: "${req.name}" (${req.description})`);
            }
        }

        // Write a summary report
        const report = {
            target: TARGET_URL,
            timestamp: new Date().toISOString(),
            expected: EXPECTED.length,
            found: found.length,
            missing: missing.length,
            forbiddenChecked: FORBIDDEN_TEXT.length,
            leaks: leaks.length,
            previewRequired: PREVIEW_REQUIRED.length,
            previewMissing: previewMissing.length,
            consoleErrors: consoleErrors.slice(0, 20),
            missingItems: missing,
            leakedItems: leaks,
            previewMissingItems: previewMissing,
        };
        fs.writeFileSync(
            path.join(OUT_DIR, 'report.json'),
            JSON.stringify(report, null, 2),
        );

        if (consoleErrors.length > 0) {
            log('WARN', `${consoleErrors.length} console error(s) during page load:`);
            consoleErrors.slice(0, 5).forEach(e => log('WARN', `  ${e}`));
        }

        if (missing.length > 0 || leaks.length > 0 || previewMissing.length > 0) {
            if (missing.length > 0) log('FAIL', `${missing.length}/${EXPECTED.length} expected UI elements MISSING`);
            if (leaks.length > 0) log('FAIL', `${leaks.length}/${FORBIDDEN_TEXT.length} forbidden text patterns LEAKED into a11y tree`);
            if (previewMissing.length > 0) log('FAIL', `${previewMissing.length}/${PREVIEW_REQUIRED.length} preview-pane standalone nodes MISSING (blocks collapsed)`);
            log('FAIL', `see ${path.join(OUT_DIR, 'report.json')} for details`);
            await browser.close();
            process.exit(2);
        }

        log('OK', `ALL ${EXPECTED.length} expected UI elements present, 0 forbidden leaks, ${PREVIEW_REQUIRED.length}/${PREVIEW_REQUIRED.length} preview standalone nodes — full-UI suite PASS`);
    } finally {
        await browser.close();
    }
    process.exit(0);
}

main().catch(err => {
    log('ENV-FAIL', `unhandled error: ${err.message}`);
    console.error(err.stack);
    process.exit(1);
});
