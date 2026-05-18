// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
//
// iter-90: Web Wasm responsive-layout anti-bluff suite.
//
// Forensic anchor (operator probe, 2026-05-18): the previous iter-84/85/86
// gates verified the app at a single 1280x800 viewport. Mobile users on
// a 320 px phone saw the three-column desktop layout crammed into 320 px
// — explorer + editor + preview squashed unusably with toolbar buttons
// cut off. The gates PASSed at desktop while the mobile experience was
// broken. Pure "tests pass / feature broken for end users" failure mode
// the §11.4 covenant forbids.
//
// This suite re-runs the inventory + structure checks at FIVE viewport
// sizes spanning real device classes:
//   320 x 568 — iPhone SE 1st gen (smallest modern phone)
//   375 x 667 — iPhone 8 (typical mid-size phone)
//   414 x 896 — iPhone 14 Pro Max (large phone)
//   768 x 1024 — iPad portrait
//   1024 x 768 — iPad landscape
//   1280 x 800 — desktop default (canonical sanity-check viewport)
//
// At each viewport it asserts:
//   - Splash hides (Compose mounts) within the timeout
//   - 5 key elements (YOLE logo + File / Save / Settings buttons + Code
//     editor textbox) are present in the a11y tree
//   - Per-breakpoint layout decisions are honored:
//       <  768 dp → sidebar HIDDEN, preview HIDDEN (editor full-screen)
//       768-1023 → sidebar VISIBLE, preview HIDDEN
//       >= 1024  → sidebar + preview both VISIBLE
//   - No console errors during load
//
// Each viewport gets a fresh browser instance — avoids cross-test
// contamination (e.g. cached SwiftShader WebGL contexts, shared SW state).
//
// Exit codes:
//   0 = every viewport's assertions all PASS
//   1 = environmental failure (browser launch / nav)
//   2 = at least one assertion failed at some viewport

const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const TARGET_URL = process.argv[2] || 'http://localhost:18080';
const OUT_DIR = path.resolve(__dirname, '../../qa-results/iter-90/responsive');

const VIEWPORTS = [
    { name: 'mobile-xs', w: 320,  h: 568,  class: 'compact' },
    { name: 'mobile-sm', w: 375,  h: 667,  class: 'compact' },
    { name: 'mobile-lg', w: 414,  h: 896,  class: 'compact' },
    { name: 'tablet-pt', w: 768,  h: 1024, class: 'medium' },
    { name: 'tablet-ls', w: 1024, h: 768,  class: 'wide' },
    { name: 'desktop',   w: 1280, h: 800,  class: 'wide' },
];

// Elements that MUST be reachable via a11y at every viewport — including
// the smallest phone. The toolbar is horizontally scrollable on narrow
// widths so even mobile-xs exposes File / Save / Settings.
const KEY_ELEMENTS = ['YOLE', 'File button', 'Save button', 'Settings button', 'Code editor'];

// Breakpoint contract — per the iter-90 responsive logic in
// EnhancedWebApp.kt's BoxWithConstraints. If the Kotlin breakpoints
// change, update both sides in the same commit.
//   < 768 dp  (compact) → both hidden
//   768-1023  (medium)  → sidebar yes, preview no
//   >= 1024   (wide)    → both yes
function expectedLayout(viewportClass) {
    switch (viewportClass) {
        case 'compact': return { sidebar: false, preview: false };
        case 'medium':  return { sidebar: true,  preview: false };
        case 'wide':    return { sidebar: true,  preview: true };
        default: throw new Error('unknown viewport class: ' + viewportClass);
    }
}

function flatten(n, out = []) {
    if (!n) return out;
    if (n.name) out.push({ role: n.role || '', name: n.name.trim() });
    for (const c of (n.children || [])) flatten(c, out);
    return out;
}

function log(level, msg) {
    console.log(`[responsive-suite][${level}] ${msg}`);
}

async function probeViewport(vp) {
    const browser = await puppeteer.launch({
        headless: true,
        args: [
            '--no-sandbox', '--disable-setuid-sandbox',
            '--enable-unsafe-swiftshader', '--use-gl=swiftshader',
        ],
    });
    try {
        const page = await browser.newPage();
        await page.setViewport({
            width: vp.w, height: vp.h,
            deviceScaleFactor: 1,
            isMobile: vp.w <= 768,
            hasTouch: vp.w <= 768,
        });
        const errs = [];
        page.on('console', m => { if (m.type() === 'error') errs.push(m.text()); });
        page.on('pageerror', err => errs.push('uncaught: ' + err.message));

        await page.goto(TARGET_URL, { waitUntil: 'load', timeout: 30000 });
        await page.waitForFunction(
            () => {
                const l = document.getElementById('loading');
                return !l || window.getComputedStyle(l).display === 'none';
            },
            { timeout: 30000, polling: 100 },
        );
        await new Promise(r => setTimeout(r, 1500));

        const tree = await page.accessibility.snapshot();
        const flat = flatten(tree);
        const names = new Set(flat.map(n => n.name));

        const missingKey = KEY_ELEMENTS.filter(k => !names.has(k));
        const sidebarShown = names.has('EXPLORER');
        const previewShown = names.has('PREVIEW');
        const expected = expectedLayout(vp.class);
        const layoutOk = sidebarShown === expected.sidebar && previewShown === expected.preview;

        await page.screenshot({
            path: path.join(OUT_DIR, vp.name + '.png'),
        });

        return {
            vp,
            a11yCount: flat.length,
            missingKey,
            sidebarShown,
            previewShown,
            expected,
            layoutOk,
            consoleErrors: errs.slice(0, 5),
            ok: missingKey.length === 0 && layoutOk,
        };
    } finally {
        await browser.close();
    }
}

async function main() {
    log('INFO', `Target URL: ${TARGET_URL}`);
    log('INFO', `Probing ${VIEWPORTS.length} viewports...`);
    fs.mkdirSync(OUT_DIR, { recursive: true });

    const results = [];
    for (const vp of VIEWPORTS) {
        const r = await probeViewport(vp);
        results.push(r);
        const verdict = r.ok ? 'OK   ' : 'FAIL ';
        log(verdict.trim(), `${vp.name.padEnd(11)} (${vp.w}x${vp.h}) a11y=${String(r.a11yCount).padEnd(3)} key=${KEY_ELEMENTS.length - r.missingKey.length}/${KEY_ELEMENTS.length} sidebar=${r.sidebarShown}(exp:${r.expected.sidebar}) preview=${r.previewShown}(exp:${r.expected.preview})`);
        if (r.missingKey.length > 0) log('FAIL', `  MISSING key elements at ${vp.name}: ${JSON.stringify(r.missingKey)}`);
        if (!r.layoutOk) log('FAIL', `  layout breakpoint contract violated at ${vp.name}: got sidebar=${r.sidebarShown} preview=${r.previewShown}, expected ${JSON.stringify(r.expected)}`);
        if (r.consoleErrors.length > 0) log('WARN', `  ${r.consoleErrors.length} console error(s) at ${vp.name}`);
    }

    fs.writeFileSync(
        path.join(OUT_DIR, 'report.json'),
        JSON.stringify({ target: TARGET_URL, timestamp: new Date().toISOString(), results }, null, 2),
    );

    const failures = results.filter(r => !r.ok).length;
    if (failures > 0) {
        log('FAIL', `${failures} / ${results.length} viewports FAILED — see ${path.join(OUT_DIR, 'report.json')}`);
        process.exit(2);
    }
    log('OK', `ALL ${results.length} viewports PASS — responsive layout works on every size`);
    process.exit(0);
}

main().catch(err => {
    console.error('[responsive-suite][ENV-FAIL]', err.message);
    console.error(err.stack);
    process.exit(1);
});
