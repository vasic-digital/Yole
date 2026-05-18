// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
//
// iter-84/85: Web Wasm browser-render anti-bluff gate.
//
// History:
//   iter-84 (v2.0.1): closed the v2.0.0 splash-then-blank gap
//     (#yoleCanvas container ID + missing <script src="yole-web.js">).
//   iter-85 (this version): closed a META-BLUFF in the iter-84 gate
//     itself — the gate claimed PASS on a 1280x154 canvas (rendering
//     a strip across the top, blank 80% below) because its "99.5%
//     non-white bytes" heuristic counted PNG-compressed bytes, not
//     decoded pixels. PNG compresses solid white areas to near-zero
//     bytes, so a screenshot that is mostly white still scored
//     "non-white" on its compressed byte distribution. User reported
//     "still opens just blank white page" after the iter-84 PASS.
//
// Hard assertions added in iter-85 — each one would, on its own,
// have caught the iter-84 regression:
//   A. CANVAS DIMENSIONS — client rect MUST be ≥ 80% of viewport in
//      BOTH width and height. 154px on a 800px viewport (19%) FAILS.
//   B. VIEWPORT POINT PROBES — at 9 evenly-spaced points across the
//      viewport (corners + edges + center), document.elementFromPoint
//      MUST return the Compose canvas OR a child of #yoleCanvas. If
//      any probe lands on a plain <html>/<body>, the canvas isn't
//      covering that region → FAIL.
//   C. DEVTOOLS PIXEL READBACK — for the canvas's own client rect,
//      use page.evaluate() to snapshot the canvas via toDataURL()
//      where supported, OR fall back to checking that the screenshot's
//      bottom-half is not uniformly the body background color via a
//      proper PNG decode (we ship a tiny inlined PNG parser to avoid
//      a new npm dependency).
//
// What it does (full pipeline):
//   1. Navigate to TARGET_URL in headless Chromium with SwiftShader.
//   2. Wait up to 30s for Compose to inject a <canvas> into #yoleCanvas
//      (shadow-DOM aware — CMP 1.11.0+ uses a shadow root).
//   3. Assert canvas client dimensions cover ≥ 80% of viewport (A).
//   4. Wait up to 5s for splash #loading to hide.
//   5. Probe 9 viewport points — all must resolve to the canvas (B).
//   6. Capture a full-viewport screenshot to qa-results/iter-84/render-gate.png.
//   7. Decode the screenshot and check that the bottom half has
//      meaningful pixel variance (not a single solid color) (C).
//
// Exit codes:
//   0 = render verified — all assertions passed.
//   1 = environmental failure (browser launch, page navigation).
//   2 = render failure — any of A/B/C asserted FAIL.
//
// Usage:
//   node render-gate.js https://yole-app.web.app
//   node render-gate.js http://localhost:8080         # local container
//   node render-gate.js file:///path/to/index.html    # local bundle

const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const TARGET_URL = process.argv[2] || 'https://yole-app.web.app';
const TIMEOUT_MS = 30000;
const SCREENSHOT_DIR = path.resolve(__dirname, '../../qa-results/iter-84');
const SCREENSHOT_PATH = path.join(SCREENSHOT_DIR, 'render-gate.png');
const VIEWPORT_WIDTH = 1280;
const VIEWPORT_HEIGHT = 800;
// Anti-bluff threshold: the canvas MUST cover at least this fraction of
// the viewport in each dimension. Calibrated against the iter-85 forensic
// case (154/800 = 0.19 → fail) with comfortable headroom for browser
// chrome / scrollbars (~0.95 in clean browsers, ~0.8 in some).
const MIN_VIEWPORT_COVERAGE = 0.80;
// Minimum number of distinct non-background pixel values that must appear
// in the bottom half of the captured screenshot. 1 = whole bottom half is
// the body background → FAIL. ≥ 8 = meaningful content rendered.
const MIN_BOTTOM_HALF_COLORS = 8;

function log(level, msg) {
    console.log(`[render-gate][${level}] ${msg}`);
}

// --- Minimal PNG decoder for the assertion-C bottom-half color check.
// Decodes IHDR + concatenates IDAT streams + inflates + un-filters into
// raw RGBA. Handles only 8-bit RGB or RGBA — the formats Puppeteer's
// `page.screenshot()` produces. Throws on anything exotic; that's a
// signal to update the gate, not to silently bluff-PASS.
function decodePng(buffer) {
    if (buffer.length < 16 || buffer.readUInt32BE(0) !== 0x89504E47 ||
        buffer.readUInt32BE(4) !== 0x0D0A1A0A) {
        throw new Error('not a PNG');
    }
    let offset = 8;
    let ihdr = null;
    const idatChunks = [];
    while (offset < buffer.length) {
        const length = buffer.readUInt32BE(offset);
        const type = buffer.slice(offset + 4, offset + 8).toString('ascii');
        const data = buffer.slice(offset + 8, offset + 8 + length);
        if (type === 'IHDR') {
            ihdr = {
                width: data.readUInt32BE(0),
                height: data.readUInt32BE(4),
                bitDepth: data.readUInt8(8),
                colorType: data.readUInt8(9),
                interlace: data.readUInt8(12),
            };
        } else if (type === 'IDAT') {
            idatChunks.push(data);
        } else if (type === 'IEND') {
            break;
        }
        offset += 8 + length + 4; // 4 bytes CRC
    }
    if (!ihdr) throw new Error('PNG missing IHDR');
    if (ihdr.interlace !== 0) throw new Error('interlaced PNG not supported');
    if (ihdr.bitDepth !== 8) throw new Error(`PNG bit depth ${ihdr.bitDepth} not supported`);
    let channels;
    if (ihdr.colorType === 2) channels = 3;       // RGB
    else if (ihdr.colorType === 6) channels = 4;  // RGBA
    else throw new Error(`PNG color type ${ihdr.colorType} not supported`);

    const inflated = zlib.inflateSync(Buffer.concat(idatChunks));
    const stride = ihdr.width * channels;
    const out = Buffer.alloc(ihdr.height * stride);
    let inPos = 0;
    let prevRowOut = Buffer.alloc(stride); // zeroed
    for (let y = 0; y < ihdr.height; y++) {
        const filter = inflated.readUInt8(inPos++);
        const rowOut = Buffer.alloc(stride);
        for (let x = 0; x < stride; x++) {
            const raw = inflated.readUInt8(inPos++);
            const left = x >= channels ? rowOut.readUInt8(x - channels) : 0;
            const up = prevRowOut.readUInt8(x);
            const upLeft = x >= channels ? prevRowOut.readUInt8(x - channels) : 0;
            let value;
            switch (filter) {
                case 0: value = raw; break;                                  // None
                case 1: value = (raw + left) & 0xFF; break;                  // Sub
                case 2: value = (raw + up) & 0xFF; break;                    // Up
                case 3: value = (raw + ((left + up) >> 1)) & 0xFF; break;    // Average
                case 4: {                                                    // Paeth
                    const p = left + up - upLeft;
                    const pa = Math.abs(p - left), pb = Math.abs(p - up), pc = Math.abs(p - upLeft);
                    const pred = (pa <= pb && pa <= pc) ? left : (pb <= pc ? up : upLeft);
                    value = (raw + pred) & 0xFF;
                    break;
                }
                default: throw new Error(`unknown PNG filter ${filter}`);
            }
            rowOut.writeUInt8(value, x);
        }
        rowOut.copy(out, y * stride);
        prevRowOut = rowOut;
    }
    return { width: ihdr.width, height: ihdr.height, channels, data: out };
}

// Count distinct (R,G,B) values in a region of the decoded image.
// We bucket by 32-step to ignore JPEG-style noise that Puppeteer's PNG
// shouldn't have anyway. A region with ≥ MIN_BOTTOM_HALF_COLORS distinct
// buckets is "rendering meaningful content"; a region with 1-2 buckets
// is solid background.
function countDistinctColors(img, x0, y0, x1, y1) {
    const seen = new Set();
    const { width, channels, data } = img;
    for (let y = y0; y < y1; y += 4) {
        for (let x = x0; x < x1; x += 4) {
            const i = (y * width + x) * channels;
            const r = data[i] >> 5;
            const g = data[i + 1] >> 5;
            const b = data[i + 2] >> 5;
            seen.add((r << 6) | (g << 3) | b);
        }
    }
    return seen.size;
}

async function main() {
    log('INFO', `Target URL: ${TARGET_URL}`);
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

    let browser;
    try {
        browser = await puppeteer.launch({
            headless: true,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--enable-unsafe-swiftshader',
                '--use-gl=swiftshader',
            ],
            defaultViewport: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT },
        });
    } catch (err) {
        log('ENV-FAIL', `browser launch failed: ${err.message}`);
        process.exit(1);
    }

    let exitCode = 0;
    try {
        const page = await browser.newPage();

        const consoleErrors = [];
        page.on('console', m => {
            if (m.type() === 'error') consoleErrors.push(m.text());
        });
        page.on('pageerror', err => consoleErrors.push(`uncaught: ${err.message}`));

        log('INFO', `Navigating with ${TIMEOUT_MS}ms timeout...`);
        try {
            await page.goto(TARGET_URL, { waitUntil: 'load', timeout: TIMEOUT_MS });
        } catch (err) {
            log('ENV-FAIL', `navigation failed: ${err.message}`);
            await browser.close();
            process.exit(1);
        }

        // Step 1: wait for canvas to mount (shadow-DOM aware)
        log('INFO', 'Waiting for Compose canvas to mount in #yoleCanvas (shadow DOM aware)...');
        let canvasMountedMs = -1;
        try {
            const startWait = Date.now();
            await page.waitForFunction(
                `(function() {
                    const yc = document.getElementById('yoleCanvas');
                    if (!yc) return false;
                    function findCanvas(el) {
                        if (!el) return false;
                        if (el.tagName === 'CANVAS') return true;
                        if (el.shadowRoot) {
                            for (const c of el.shadowRoot.children) { if (findCanvas(c)) return true; }
                        }
                        for (const c of el.children) { if (findCanvas(c)) return true; }
                        return false;
                    }
                    return findCanvas(yc);
                })()`,
                { timeout: TIMEOUT_MS, polling: 100 },
            );
            canvasMountedMs = Date.now() - startWait;
            log('OK', `Compose canvas mounted in ${canvasMountedMs} ms`);
        } catch (err) {
            log('RENDER-FAIL', `Compose canvas did NOT mount within ${TIMEOUT_MS}ms`);
            await page.screenshot({ path: SCREENSHOT_PATH, fullPage: false }).catch(() => {});
            await browser.close();
            process.exit(2);
        }

        // Step 2: canvas dimensions + assertion A (coverage)
        const canvasMeta = await page.evaluate(() => {
            function findCanvas(el) {
                if (!el) return null;
                if (el.tagName === 'CANVAS') return el;
                if (el.shadowRoot) {
                    for (const c of el.shadowRoot.children) { const f = findCanvas(c); if (f) return f; }
                }
                for (const c of el.children) { const f = findCanvas(c); if (f) return f; }
                return null;
            }
            const canvas = findCanvas(document.getElementById('yoleCanvas'));
            if (!canvas) return null;
            const r = canvas.getBoundingClientRect();
            return {
                width: canvas.width,
                height: canvas.height,
                clientWidth: r.width,
                clientHeight: r.height,
                visible: r.width > 0 && r.height > 0,
                inShadowDom: canvas.getRootNode() instanceof ShadowRoot,
            };
        });
        if (!canvasMeta || !canvasMeta.visible) {
            log('RENDER-FAIL', `canvas exists but zero dimensions: ${JSON.stringify(canvasMeta)}`);
            await page.screenshot({ path: SCREENSHOT_PATH }).catch(() => {});
            await browser.close();
            process.exit(2);
        }
        log('OK', `canvas dimensions: ${canvasMeta.width}x${canvasMeta.height} (client ${Math.round(canvasMeta.clientWidth)}x${Math.round(canvasMeta.clientHeight)}, shadow=${canvasMeta.inShadowDom})`);

        // --- ASSERTION A: viewport coverage ---
        const wCov = canvasMeta.clientWidth / VIEWPORT_WIDTH;
        const hCov = canvasMeta.clientHeight / VIEWPORT_HEIGHT;
        if (wCov < MIN_VIEWPORT_COVERAGE || hCov < MIN_VIEWPORT_COVERAGE) {
            log('RENDER-FAIL', `canvas coverage too small: width ${(wCov*100).toFixed(1)}% / height ${(hCov*100).toFixed(1)}% — need ≥ ${(MIN_VIEWPORT_COVERAGE*100)}% in both`);
            log('RENDER-FAIL', `This is the iter-85 forensic case: Compose mounted but rendered into a thin strip because the host div had no CSS height. End-user sees a tiny UI at top + blank below.`);
            await page.screenshot({ path: SCREENSHOT_PATH }).catch(() => {});
            await browser.close();
            process.exit(2);
        }
        log('OK', `assertion A: canvas covers ${(wCov*100).toFixed(1)}% × ${(hCov*100).toFixed(1)}% of viewport`);

        // Step 3: wait for splash to be hidden
        log('INFO', 'Waiting for splash screen to hide...');
        try {
            await page.waitForFunction(
                () => {
                    const loading = document.getElementById('loading');
                    if (!loading) return true;
                    const style = window.getComputedStyle(loading);
                    return style.display === 'none' || parseFloat(style.opacity) === 0;
                },
                { timeout: 5000, polling: 100 },
            );
            log('OK', 'splash hidden');
        } catch (err) {
            log('WARN', 'splash did not hide within 5s — render is suspicious');
        }

        // --- ASSERTION B: 9-point viewport probe ---
        // At evenly-spaced points across the viewport, the topmost element
        // at that point MUST be either the canvas itself OR a descendant
        // of #yoleCanvas. If it's <html> or <body>, the canvas isn't
        // covering that region — the user sees blank there.
        log('INFO', 'Probing 9 viewport points (corners + edges + center)...');
        const probeResult = await page.evaluate((W, H) => {
            const points = [
                [0.1, 0.1], [0.5, 0.1], [0.9, 0.1],
                [0.1, 0.5], [0.5, 0.5], [0.9, 0.5],
                [0.1, 0.9], [0.5, 0.9], [0.9, 0.9],
            ];
            const yoleCanvas = document.getElementById('yoleCanvas');
            const results = [];
            for (const [fx, fy] of points) {
                const x = Math.round(W * fx);
                const y = Math.round(H * fy);
                const el = document.elementFromPoint(x, y);
                let covered = false;
                if (el === yoleCanvas) covered = true;
                else if (el && yoleCanvas && yoleCanvas.contains(el)) covered = true;
                else if (el && el.tagName === 'CANVAS') covered = true;
                else if (el) {
                    // shadow-DOM walk: is el inside a shadow root hosted by yoleCanvas subtree?
                    let root = el.getRootNode();
                    while (root && root !== document) {
                        const host = root.host;
                        if (host && (host === yoleCanvas || (yoleCanvas && yoleCanvas.contains(host)))) {
                            covered = true;
                            break;
                        }
                        root = host ? host.getRootNode() : document;
                    }
                }
                results.push({ x, y, tag: el ? el.tagName : 'null', id: el ? el.id : '', covered });
            }
            return results;
        }, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        const uncovered = probeResult.filter(p => !p.covered);
        if (uncovered.length > 0) {
            log('RENDER-FAIL', `${uncovered.length}/9 viewport points NOT covered by canvas:`);
            uncovered.forEach(p => log('RENDER-FAIL', `  (${p.x},${p.y}) → <${p.tag.toLowerCase()}${p.id ? ` id="${p.id}"` : ''}> — user sees blank/wrong content here`));
            await page.screenshot({ path: SCREENSHOT_PATH }).catch(() => {});
            await browser.close();
            process.exit(2);
        }
        log('OK', `assertion B: all 9 viewport probes resolve to canvas`);

        // Step 6: capture screenshot (evidence + assertion-C input)
        const screenshotBuffer = await page.screenshot({ path: SCREENSHOT_PATH, fullPage: false });
        log('INFO', `screenshot: ${SCREENSHOT_PATH} (${screenshotBuffer.length} bytes)`);

        // --- ASSERTION C: decoded-pixel bottom-half color count ---
        // Decode the PNG and count distinct color buckets in the bottom
        // half of the viewport. If the bottom half is uniformly one or two
        // colors, the canvas didn't actually paint anything there.
        try {
            // Puppeteer 23.x returns Uint8Array, not Node Buffer. Convert explicitly.
            const buf = Buffer.isBuffer(screenshotBuffer) ? screenshotBuffer : Buffer.from(screenshotBuffer);
            const img = decodePng(buf);
            const bottomColors = countDistinctColors(
                img,
                0,
                Math.floor(img.height / 2),
                img.width,
                img.height,
            );
            if (bottomColors < MIN_BOTTOM_HALF_COLORS) {
                log('RENDER-FAIL', `bottom half has only ${bottomColors} distinct color buckets (need ≥ ${MIN_BOTTOM_HALF_COLORS}) — likely uniform background`);
                await browser.close();
                process.exit(2);
            }
            log('OK', `assertion C: bottom half has ${bottomColors} distinct color buckets`);
        } catch (e) {
            log('RENDER-FAIL', `PNG decode failed: ${e.message} — cannot verify pixel content`);
            await browser.close();
            process.exit(2);
        }

        log('OK', `RENDER PASS — canvas mounted in ${canvasMountedMs}ms, covers viewport, 9/9 probes covered, bottom half has color variance`);

        if (consoleErrors.length > 0) {
            log('WARN', `${consoleErrors.length} console error(s) during page load:`);
            consoleErrors.slice(0, 5).forEach(e => log('WARN', `  ${e}`));
        }
    } finally {
        await browser.close();
    }
    process.exit(exitCode);
}

main().catch(err => {
    log('ENV-FAIL', `unhandled error: ${err.message}`);
    console.error(err.stack);
    process.exit(1);
});
