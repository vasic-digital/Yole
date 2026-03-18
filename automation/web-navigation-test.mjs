/**
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Yole Web App — Tab Navigation Automation Test
 *
 * Tests comprehensive tab/toolbar navigation across all major screens
 * (Editor, Formats sidebar, Settings, Preview, Theme) with Playwright.
 *
 * Works with both the fallback HTML app and the Compose Wasm canvas app.
 * Screenshots at every navigation step + video recording of the session.
 *
 * Usage: node automation/web-navigation-test.mjs
 */

import { chromium } from 'playwright';
import { mkdirSync, existsSync, readFileSync } from 'fs';
import { join } from 'path';
import { createServer } from 'http';

// ============================================================
// Constants
// ============================================================
const PROJECT_ROOT = '/run/media/milosvasic/DATA4TB/Projects/Yole';
const RECORDINGS_DIR = join(PROJECT_ROOT, 'recordings', 'web', 'navigation');
const AUTOMATION_DIR = join(PROJECT_ROOT, 'automation');
const FALLBACK_APP = join(AUTOMATION_DIR, 'yole-fallback-app.html');

const SPEED = { clickDelay: 300, typeDelay: 20, navPause: 600 };

// ============================================================
// Utility helpers
// ============================================================
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function timestamp() {
  return new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
}

async function screenshot(page, dir, name) {
  const path = join(dir, `${name}.png`);
  await page.screenshot({ path, fullPage: false });
  console.log(`    Screenshot: ${name}.png`);
  return path;
}

// ============================================================
// Local HTTP server for fallback app
// ============================================================
function startFallbackServer(htmlPath, port = 9876) {
  return new Promise((resolve, reject) => {
    const html = readFileSync(htmlPath, 'utf-8');
    const server = createServer((req, res) => {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(html);
    });
    server.listen(port, '127.0.0.1', () => {
      console.log(`  Fallback server on http://127.0.0.1:${port}`);
      resolve(server);
    });
    server.on('error', reject);
  });
}

// ============================================================
// Navigation test suite
// ============================================================
async function runNavigationTests() {
  const ts = timestamp();
  const screenshotDir = join(RECORDINGS_DIR, ts);
  mkdirSync(screenshotDir, { recursive: true });
  mkdirSync(RECORDINGS_DIR, { recursive: true });

  let server = null;
  let appUrl = null;
  let isFallback = false;

  // Check for pre-built Compose Wasm app
  const distDirs = [
    join(PROJECT_ROOT, 'webApp/build/dist/wasmJs/developmentExecutable'),
    join(PROJECT_ROOT, 'webApp/build/dist/wasmJs/productionExecutable'),
  ];
  for (const d of distDirs) {
    if (existsSync(d) && existsSync(join(d, 'index.html'))) {
      console.log(`  Found Compose Wasm build at ${d}`);
      // Serve it via local HTTP (Wasm needs HTTP, not file://)
      const wasmHtml = readFileSync(join(d, 'index.html'), 'utf-8');
      server = createServer((req, res) => {
        const filePath = join(d, req.url === '/' ? 'index.html' : req.url);
        try {
          const content = readFileSync(filePath);
          const ext = filePath.split('.').pop();
          const mimeTypes = {
            html: 'text/html', js: 'application/javascript',
            wasm: 'application/wasm', css: 'text/css', json: 'application/json',
          };
          res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' });
          res.end(content);
        } catch {
          res.writeHead(404);
          res.end('Not found');
        }
      });
      await new Promise((resolve) => server.listen(9877, '127.0.0.1', resolve));
      appUrl = 'http://127.0.0.1:9877';
      break;
    }
  }

  // Fallback to the HTML test app
  if (!appUrl) {
    if (!existsSync(FALLBACK_APP)) {
      console.error(`ERROR: No app available. Neither Compose Wasm build nor fallback HTML found.`);
      console.error(`  Expected: ${FALLBACK_APP}`);
      process.exit(1);
    }
    server = await startFallbackServer(FALLBACK_APP);
    appUrl = 'http://127.0.0.1:9876';
    isFallback = true;
    console.log('  Using fallback HTML app');
  }

  // Launch browser with video recording
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 800 },
    recordVideo: { dir: screenshotDir, size: { width: 1280, height: 800 } },
  });
  const page = await context.newPage();

  let stepCount = 0;
  let passCount = 0;
  let failCount = 0;

  async function step(name, fn) {
    stepCount++;
    const num = String(stepCount).padStart(2, '0');
    try {
      console.log(`  [${num}] ${name}`);
      await fn();
      passCount++;
      console.log(`  [${num}] PASS`);
    } catch (err) {
      failCount++;
      console.error(`  [${num}] FAIL: ${err.message}`);
      await screenshot(page, screenshotDir, `${num}-FAIL-${name.replace(/\s+/g, '-')}`);
    }
  }

  try {
    // ================================================================
    // 1. Launch the app
    // ================================================================
    await step('Launch app', async () => {
      await page.goto(appUrl, { waitUntil: 'networkidle', timeout: 30000 });
      if (isFallback) {
        await page.waitForSelector('#editor', { timeout: 10000 });
      } else {
        await page.waitForSelector('canvas', { timeout: 60000 }).catch(() =>
          page.waitForFunction(() => {
            const l = document.getElementById('loading');
            return !l || l.style.display === 'none';
          }, { timeout: 30000 })
        );
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-01-app-launched`);
    });

    // ================================================================
    // 2. Forward navigation: Editor -> Formats -> Settings -> Preview
    // ================================================================
    await step('Forward: Focus editor area', async () => {
      if (isFallback) {
        await page.click('#editor');
      } else {
        await page.mouse.click(640, 400);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-02-editor-focused`);
    });

    await step('Forward: Click format sidebar item 1 (Markdown)', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="markdown"]');
        if (item) {
          await item.click();
        } else {
          await page.selectOption('#formatSelector', 'markdown');
        }
      } else {
        await page.mouse.click(125, 120);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-03-format-markdown`);
    });

    await step('Forward: Click format sidebar item 2 (Todo.txt)', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="todotxt"]');
        if (item) {
          await item.click();
        } else {
          await page.selectOption('#formatSelector', 'todotxt');
        }
      } else {
        await page.mouse.click(125, 152);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-04-format-todotxt`);
    });

    await step('Forward: Click format sidebar item 3 (CSV)', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="csv"]');
        if (item) {
          await item.click();
        } else {
          await page.selectOption('#formatSelector', 'csv');
        }
      } else {
        await page.mouse.click(125, 184);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-05-format-csv`);
    });

    await step('Forward: Open Settings dialog', async () => {
      if (isFallback) {
        await page.click('#btnSettings');
        await sleep(SPEED.clickDelay);
        await page.waitForSelector('#settingsDialog[style*="flex"]', { timeout: 3000 }).catch(() => {});
      } else {
        await page.keyboard.press('Control+,');
        await sleep(SPEED.clickDelay);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-06-settings-open`);
    });

    await step('Forward: Close Settings dialog', async () => {
      if (isFallback) {
        await page.click('#settingsClose');
      } else {
        await page.keyboard.press('Escape');
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-07-settings-closed`);
    });

    await step('Forward: Open Preview', async () => {
      if (isFallback) {
        await page.click('#btnPreview');
      } else {
        await page.mouse.click(1100, 700);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-08-preview-open`);
    });

    await step('Forward: Close Preview (toggle back)', async () => {
      if (isFallback) {
        await page.click('#btnPreview');
      } else {
        await page.mouse.click(1100, 700);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-09-preview-closed`);
    });

    // ================================================================
    // 3. Backward navigation: walk formats in reverse
    // ================================================================
    await step('Backward: Switch to CSV', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="csv"]');
        if (item) await item.click();
        else await page.selectOption('#formatSelector', 'csv');
      } else {
        await page.mouse.click(125, 184);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-10-back-csv`);
    });

    await step('Backward: Switch to Todo.txt', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="todotxt"]');
        if (item) await item.click();
        else await page.selectOption('#formatSelector', 'todotxt');
      } else {
        await page.mouse.click(125, 152);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-11-back-todotxt`);
    });

    await step('Backward: Switch to Markdown', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="markdown"]');
        if (item) await item.click();
        else await page.selectOption('#formatSelector', 'markdown');
      } else {
        await page.mouse.click(125, 120);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-12-back-markdown`);
    });

    // ================================================================
    // 4. Random jumps: theme toggle, format, settings, preview
    // ================================================================
    await step('Random: Toggle theme to dark', async () => {
      if (isFallback) {
        await page.click('#btnTheme');
      } else {
        await page.mouse.click(1200, 36);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-13-dark-theme`);
    });

    await step('Random: Jump to LaTeX format', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="latex"]');
        if (item) await item.click();
        else await page.selectOption('#formatSelector', 'latex');
      } else {
        await page.mouse.click(125, 216);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-14-jump-latex`);
    });

    await step('Random: Open settings in dark theme', async () => {
      if (isFallback) {
        await page.click('#btnSettings');
        await sleep(SPEED.clickDelay);
        await page.waitForSelector('#settingsDialog[style*="flex"]', { timeout: 3000 }).catch(() => {});
      } else {
        await page.keyboard.press('Control+,');
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-15-settings-dark`);
    });

    await step('Random: Close settings', async () => {
      if (isFallback) {
        await page.click('#settingsClose');
      } else {
        await page.keyboard.press('Escape');
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-16-settings-closed-dark`);
    });

    await step('Random: Toggle theme back to light', async () => {
      if (isFallback) {
        await page.click('#btnTheme');
      } else {
        await page.mouse.click(1200, 36);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-17-light-theme-restored`);
    });

    await step('Random: Jump to Markdown', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="markdown"]');
        if (item) await item.click();
        else await page.selectOption('#formatSelector', 'markdown');
      } else {
        await page.mouse.click(125, 120);
      }
      await sleep(SPEED.navPause);
      await screenshot(page, screenshotDir, `${ts}-18-jump-back-markdown`);
    });

    // ================================================================
    // 5. Rapid switching: 20 format changes
    // ================================================================
    const rapidFormats = [
      { id: 'markdown', y: 120, label: 'Markdown' },
      { id: 'todotxt', y: 152, label: 'TodoTxt' },
      { id: 'csv', y: 184, label: 'CSV' },
      { id: 'latex', y: 216, label: 'LaTeX' },
      { id: 'orgmode', y: 248, label: 'OrgMode' },
    ];

    for (let i = 0; i < 20; i++) {
      const fmt = rapidFormats[i % rapidFormats.length];
      const num = String(i + 1).padStart(2, '0');
      await step(`Rapid ${num}/20: Switch to ${fmt.label}`, async () => {
        if (isFallback) {
          const item = await page.$(`[data-format="${fmt.id}"]`);
          if (item) await item.click();
          else await page.selectOption('#formatSelector', fmt.id);
        } else {
          await page.mouse.click(125, fmt.y);
        }
        await sleep(SPEED.clickDelay);
        await screenshot(page, screenshotDir, `${ts}-rapid-${num}-${fmt.label.toLowerCase()}`);
      });
    }

  } finally {
    // Close browser and stop video recording
    await page.close();
    await context.close();
    await browser.close();
    if (server) server.close();
  }

  // ================================================================
  // Report
  // ================================================================
  console.log('\n========================================');
  console.log(`  Navigation Test Results`);
  console.log(`  Total steps:  ${stepCount}`);
  console.log(`  Passed:       ${passCount}`);
  console.log(`  Failed:       ${failCount}`);
  console.log(`  Screenshots:  ${screenshotDir}/`);
  console.log('========================================\n');

  if (failCount > 0) {
    process.exit(1);
  }
}

// ============================================================
// Main
// ============================================================
console.log('=== Yole Web Navigation Test ===');
console.log(`  Project: ${PROJECT_ROOT}`);
console.log(`  Output:  ${RECORDINGS_DIR}`);
runNavigationTests().catch(err => {
  console.error('FATAL:', err);
  process.exit(1);
});
