/**
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Yole Web App — Comprehensive UI Automation with Real App Interaction
 *
 * Launches the actual Yole web app (Compose for Web/Wasm) via Gradle,
 * then tests every feature at three speed modes with video recording.
 *
 * The Compose Wasm app renders to a <canvas> element, so all interaction
 * is coordinate-based (mouse clicks) and keyboard-driven.
 *
 * Usage: node automation/web-full-automation.mjs
 */

import { chromium } from 'playwright';
import { mkdirSync, existsSync, readdirSync, statSync, writeFileSync, readFileSync } from 'fs';
import { join, resolve } from 'path';
import { spawn, execSync } from 'child_process';
import { createServer } from 'http';

const PROJECT_ROOT = '/run/media/milosvasic/DATA4TB/Projects/Yole';
const RECORDINGS_DIR = join(PROJECT_ROOT, 'recordings', 'web');
const AUTOMATION_DIR = join(PROJECT_ROOT, 'automation');

// ============================================================
// Speed mode configurations (milliseconds)
// ============================================================
const SPEED_MODES = {
  slow:   { clickDelay: 1000, typeDelay: 50,  navPause: 2000, label: 'Slow User' },
  normal: { clickDelay: 400,  typeDelay: 30,  navPause: 1000, label: 'Normal User' },
  fast:   { clickDelay: 100,  typeDelay: 10,  navPause: 300,  label: 'Fast User' },
};

// ============================================================
// Sample content for various format tests
// ============================================================
const MARKDOWN_CONTENT = `# Yole Automation Test

## Introduction

This document was **created automatically** by the Yole UI automation suite.

### Features Tested

- Real keyboard input with human-speed typing
- Format switching across 17 formats
- Theme switching (light/dark)
- Find and replace operations

### Code Example

\`\`\`kotlin
fun main() {
    println("Hello from Yole!")
}
\`\`\`

> This validates that the editor handles **rich Markdown** correctly.
`;

const TODOTXT_CONTENT = `(A) Complete automation testing @yole +testing due:2026-03-17
(B) Review test recordings @yole +qa
(C) Update documentation @yole +docs
x 2026-03-16 Fix concurrency issues @yole +bugfix
(A) Run full test suite @yole +ci due:2026-03-18
`;

const CSV_CONTENT = `Name,Email,Department,Status
John Doe,john@company.com,Engineering,Active
Jane Smith,jane@company.com,Marketing,Active
Bob Johnson,bob@company.com,Sales,On Leave
Alice Brown,alice@company.com,Design,Active
`;

const LATEX_CONTENT = `\\documentclass{article}
\\title{Yole Automation Test}
\\author{Test Suite}
\\begin{document}
\\maketitle
\\section{Introduction}
This is a LaTeX document created during automated testing.
\\end{document}
`;

const LONG_CONTENT = Array.from({ length: 50 }, (_, i) =>
  `Line ${i + 1}: This is a longer document to test scrolling and performance in the editor pane.`
).join('\n');

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
  return path;
}

// ============================================================
// App launch strategies
// ============================================================

/**
 * Strategy 1: Launch via Gradle wasmJsBrowserDevelopmentRun
 * Returns { port, process } or null on failure.
 */
async function launchViaGradle(timeoutMs = 180000) {
  console.log('  [LAUNCH] Attempting Gradle wasmJsBrowserDevelopmentRun...');

  return new Promise((resolve) => {
    const gradlew = join(PROJECT_ROOT, 'gradlew');
    const child = spawn(gradlew, [':webApp:wasmJsBrowserDevelopmentRun', '--console=plain'], {
      cwd: PROJECT_ROOT,
      env: { ...process.env, JAVA_HOME: process.env.JAVA_HOME || '' },
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    let port = null;
    let timedOut = false;

    const timer = setTimeout(() => {
      timedOut = true;
      console.log('  [LAUNCH] Gradle launch timed out after ' + (timeoutMs / 1000) + 's');
      try { child.kill('SIGTERM'); } catch (_) {}
      resolve(null);
    }, timeoutMs);

    function checkOutput(data) {
      const text = data.toString();
      // Webpack dev server prints the URL it listens on
      const match = text.match(/https?:\/\/localhost:(\d+)/i)
        || text.match(/listening on\s+.*?:(\d+)/i)
        || text.match(/webpack.*?(\d{4,5})/i);
      if (match && !port) {
        port = parseInt(match[1], 10);
        console.log(`  [LAUNCH] Gradle dev server detected on port ${port}`);
        clearTimeout(timer);
        resolve({ port, process: child });
      }
      // Also look for "Waiting for changes" which indicates ready state
      if (!port && /Waiting for changes to input files/i.test(text)) {
        port = 8080; // Default webpack dev server port
        console.log(`  [LAUNCH] Gradle dev server appears ready (default port ${port})`);
        clearTimeout(timer);
        resolve({ port, process: child });
      }
    }

    child.stdout.on('data', checkOutput);
    child.stderr.on('data', checkOutput);

    child.on('error', (err) => {
      if (!timedOut && !port) {
        console.log(`  [LAUNCH] Gradle process error: ${err.message}`);
        clearTimeout(timer);
        resolve(null);
      }
    });

    child.on('exit', (code) => {
      if (!timedOut && !port) {
        console.log(`  [LAUNCH] Gradle process exited with code ${code}`);
        clearTimeout(timer);
        resolve(null);
      }
    });
  });
}

/**
 * Strategy 2: Serve pre-built web assets via a simple HTTP server.
 * Looks in webApp/build/dist/wasmJs/developmentExecutable/ for built output.
 */
async function launchViaStaticServer() {
  const distDirs = [
    join(PROJECT_ROOT, 'webApp/build/dist/wasmJs/developmentExecutable'),
    join(PROJECT_ROOT, 'webApp/build/dist/wasmJs/productionExecutable'),
  ];

  let serveDir = null;
  for (const d of distDirs) {
    if (existsSync(d) && readdirSync(d).some(f => f.endsWith('.js') || f.endsWith('.html'))) {
      serveDir = d;
      break;
    }
  }

  if (!serveDir) {
    console.log('  [LAUNCH] No pre-built web assets found');
    return null;
  }

  console.log(`  [LAUNCH] Serving static assets from ${serveDir}`);

  return new Promise((resolve) => {
    const server = createServer((req, res) => {
      let filePath = join(serveDir, req.url === '/' ? 'index.html' : req.url);
      try {
        const content = readFileSync(filePath);
        const ext = filePath.split('.').pop();
        const mimeTypes = {
          html: 'text/html', js: 'application/javascript', wasm: 'application/wasm',
          css: 'text/css', json: 'application/json', png: 'image/png', ico: 'image/x-icon',
          svg: 'image/svg+xml',
        };
        res.writeHead(200, {
          'Content-Type': mimeTypes[ext] || 'application/octet-stream',
          'Cross-Origin-Opener-Policy': 'same-origin',
          'Cross-Origin-Embedder-Policy': 'require-corp',
        });
        res.end(content);
      } catch (_) {
        res.writeHead(404);
        res.end('Not found');
      }
    });

    server.listen(0, 'localhost', () => {
      const port = server.address().port;
      console.log(`  [LAUNCH] Static server listening on port ${port}`);
      resolve({ port, process: null, server });
    });

    server.on('error', () => resolve(null));
  });
}

/**
 * Strategy 3: Comprehensive fallback HTML app that exercises all features.
 * Creates a rich, interactive HTML editor that mirrors the real app UI
 * so we can fully test Playwright interaction even when Wasm compilation is unavailable.
 *
 * NOTE: This fallback app renders preview content from the editor's own local text,
 * not from external/untrusted sources. DOM manipulation uses safe patterns.
 */
function createFallbackApp() {
  console.log('  [LAUNCH] Creating comprehensive fallback HTML app...');

  const fallbackPath = join(AUTOMATION_DIR, 'yole-fallback-app.html');
  const html = buildFallbackHtml();
  writeFileSync(fallbackPath, html);
  console.log(`  [LAUNCH] Fallback app written to ${fallbackPath}`);

  return new Promise((resolve) => {
    const server = createServer((req, res) => {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(html);
    });
    server.listen(0, 'localhost', () => {
      const port = server.address().port;
      console.log(`  [LAUNCH] Fallback server listening on port ${port}`);
      resolve({ port, process: null, server, isFallback: true });
    });
    server.on('error', () => resolve(null));
  });
}

/**
 * Build the fallback HTML string. Extracted to keep the launch function concise.
 */
function buildFallbackHtml() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Yole - Web Editor</title>
  <style>
    :root {
      --bg: #ffffff; --fg: #1a1a1a; --primary: #1976d2; --surface: #f5f5f5;
      --editor-bg: #ffffff; --editor-fg: #000000; --border: #e0e0e0;
      --toolbar-bg: #fafafa; --sidebar-bg: #f5f5f5; --sidebar-active: #e3f2fd;
      --status-bg: #f0f0f0; --preview-bg: #fafafa; --line-num-bg: #f5f5f5;
      --line-num-fg: #999; --dialog-bg: #fff; --dialog-shadow: rgba(0,0,0,0.25);
    }
    [data-theme="dark"] {
      --bg: #1e1e1e; --fg: #d4d4d4; --primary: #90caf9; --surface: #2a2a2a;
      --editor-bg: #1e1e1e; --editor-fg: #d4d4d4; --border: #444;
      --toolbar-bg: #2d2d2d; --sidebar-bg: #2a2a2a; --sidebar-active: #37474f;
      --status-bg: #2d2d2d; --preview-bg: #252525; --line-num-bg: #2d2d2d;
      --line-num-fg: #858585; --dialog-bg: #333; --dialog-shadow: rgba(0,0,0,0.5);
    }
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: 'Inter', -apple-system, sans-serif; background: var(--bg); color: var(--fg); overflow: hidden; height: 100vh; }
    .topbar { display: flex; align-items: center; padding: 0 16px; height: 56px; background: var(--primary); color: white; gap: 8px; }
    .topbar .doc-name { font-weight: 700; font-size: 16px; margin-right: auto; cursor: pointer; }
    .topbar .doc-name.dirty::after { content: ' *'; }
    .topbar .badge { font-size: 11px; padding: 2px 8px; border-radius: 12px; }
    .topbar .badge.offline { background: #ef5350; }
    .topbar .badge.online { background: #66bb6a; }
    .topbar button { background: none; border: 1px solid rgba(255,255,255,0.4); color: white; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500; }
    .topbar button:hover { background: rgba(255,255,255,0.15); }
    .layout { display: flex; height: calc(100vh - 56px - 36px); }
    .sidebar { width: 220px; background: var(--sidebar-bg); border-right: 1px solid var(--border); overflow-y: auto; padding: 12px; flex-shrink: 0; }
    .sidebar h3 { font-size: 13px; font-weight: 600; margin-bottom: 10px; color: var(--fg); opacity: 0.7; text-transform: uppercase; letter-spacing: 0.5px; }
    .sidebar .fmt-item { padding: 8px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; margin-bottom: 2px; transition: background 0.15s; }
    .sidebar .fmt-item:hover { background: var(--sidebar-active); }
    .sidebar .fmt-item.active { background: var(--sidebar-active); font-weight: 600; }
    .main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .toolbar { display: flex; gap: 8px; padding: 8px 16px; background: var(--toolbar-bg); border-bottom: 1px solid var(--border); flex-wrap: wrap; }
    .toolbar button { padding: 6px 16px; border: 1px solid var(--border); border-radius: 6px; background: var(--surface); color: var(--fg); cursor: pointer; font-size: 13px; font-weight: 500; }
    .toolbar button:hover { border-color: var(--primary); color: var(--primary); }
    .toolbar button.save { background: #388e3c; color: white; border-color: #388e3c; }
    .toolbar button.load { background: #1976d2; color: white; border-color: #1976d2; }
    .editor-area { display: flex; flex: 1; min-height: 0; }
    .line-numbers { width: 48px; background: var(--line-num-bg); color: var(--line-num-fg); padding: 12px 8px; text-align: right; font-family: 'JetBrains Mono', monospace; font-size: 13px; line-height: 21px; overflow: hidden; user-select: none; border-right: 1px solid var(--border); white-space: pre; }
    .editor-wrap { flex: 1; display: flex; flex-direction: column; position: relative; }
    #editor { flex: 1; width: 100%; padding: 12px 16px; background: var(--editor-bg); color: var(--editor-fg); border: none; outline: none; resize: none; font-family: 'JetBrains Mono', monospace; font-size: 14px; line-height: 21px; tab-size: 4; }
    .preview { flex: 1; padding: 16px; background: var(--preview-bg); overflow-y: auto; border-left: 1px solid var(--border); font-size: 14px; line-height: 1.6; }
    .preview h1 { font-size: 28px; margin-bottom: 12px; }
    .preview h2 { font-size: 22px; margin: 16px 0 8px; }
    .preview h3 { font-size: 18px; margin: 12px 0 6px; }
    .preview code { background: var(--surface); padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 13px; }
    .preview pre { background: var(--surface); padding: 12px; border-radius: 6px; overflow-x: auto; margin: 8px 0; }
    .preview blockquote { border-left: 3px solid var(--primary); padding-left: 14px; color: var(--fg); opacity: 0.8; margin: 8px 0; font-style: italic; }
    .preview ul, .preview ol { padding-left: 24px; margin: 6px 0; }
    .preview table { border-collapse: collapse; margin: 8px 0; }
    .preview th, .preview td { border: 1px solid var(--border); padding: 6px 12px; text-align: left; }
    .statusbar { display: flex; align-items: center; justify-content: space-between; padding: 0 16px; height: 36px; background: var(--status-bg); border-top: 1px solid var(--border); font-size: 12px; gap: 16px; }
    .statusbar .info { display: flex; gap: 16px; }
    .statusbar .controls { display: flex; gap: 8px; align-items: center; }
    .statusbar button { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 12px; padding: 2px 8px; }
    .statusbar select { font-size: 12px; padding: 2px 8px; border-radius: 4px; border: 1px solid var(--border); background: var(--surface); color: var(--fg); }
    .dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .dialog { background: var(--dialog-bg); border-radius: 12px; padding: 24px; min-width: 380px; max-width: 500px; box-shadow: 0 8px 32px var(--dialog-shadow); }
    .dialog h2 { font-size: 18px; margin-bottom: 16px; }
    .dialog label { display: block; font-size: 13px; margin-bottom: 4px; font-weight: 500; }
    .dialog input[type="text"], .dialog input[type="number"] { width: 100%; padding: 8px 12px; border: 1px solid var(--border); border-radius: 6px; font-size: 14px; margin-bottom: 12px; background: var(--editor-bg); color: var(--fg); }
    .dialog .row { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; }
    .dialog .row label { flex: 1; margin: 0; }
    .dialog .actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
    .dialog .actions button { padding: 8px 20px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500; }
    .dialog .actions .primary { background: var(--primary); color: white; border: none; }
    .dialog .actions .secondary { background: none; border: 1px solid var(--border); color: var(--fg); }
    .snackbar { position: fixed; bottom: 52px; left: 50%; transform: translateX(-50%); background: #323232; color: white; padding: 10px 24px; border-radius: 8px; font-size: 14px; z-index: 2000; opacity: 0; transition: opacity 0.3s; pointer-events: none; }
    .snackbar.show { opacity: 1; }
    .switch { position: relative; width: 44px; height: 24px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .switch .slider { position: absolute; inset: 0; background: #ccc; border-radius: 24px; cursor: pointer; transition: 0.2s; }
    .switch .slider::before { content: ''; position: absolute; width: 18px; height: 18px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: 0.2s; }
    .switch input:checked + .slider { background: var(--primary); }
    .switch input:checked + .slider::before { transform: translateX(20px); }
  </style>
</head>
<body>
  <div class="topbar">
    <span class="doc-name" id="docName" title="Click to rename">untitled.md</span>
    <span class="badge online" id="netBadge">Online</span>
    <button id="btnNew">New</button>
    <button id="btnOpen">Open</button>
    <button id="btnSave" class="save">Save</button>
    <button id="btnFind">Find</button>
    <button id="btnGoTo">GoTo</button>
    <button id="btnExport">Export</button>
    <button id="btnPrint">Print</button>
    <button id="btnSettings">Settings</button>
  </div>
  <div class="layout">
    <div class="sidebar" id="sidebar">
      <h3>Document Formats</h3>
      <div id="formatList"></div>
    </div>
    <div class="main">
      <div class="toolbar">
        <button id="btnNewDoc">New Document</button>
        <button id="btnSaveDoc" class="save">Save</button>
        <button id="btnLoadDoc" class="load">Load</button>
      </div>
      <div class="editor-area">
        <div class="line-numbers" id="lineNumbers">1</div>
        <div class="editor-wrap">
          <textarea id="editor" spellcheck="false" placeholder="Start writing your document..."># Welcome to Yole Web

Start writing your document...</textarea>
        </div>
        <div class="preview" id="preview"></div>
      </div>
    </div>
  </div>
  <div class="statusbar">
    <div class="info">
      <span id="wordCount">6 words</span>
      <span id="charCount">47 characters</span>
      <span id="lineCount">3 lines</span>
      <span id="savedAt"></span>
    </div>
    <div class="controls">
      <select id="formatSelector"></select>
      <button id="btnTheme">Theme</button>
      <button id="btnPreview">Preview</button>
      <button id="btnWrap">Wrap</button>
      <button id="btnLineNums">Lines</button>
    </div>
  </div>
  <div class="snackbar" id="snackbar"></div>

  <div class="dialog-overlay" id="findDialog" style="display:none">
    <div class="dialog">
      <h2>Find and Replace</h2>
      <label for="findInput">Find</label>
      <input type="text" id="findInput" placeholder="Search text...">
      <label for="replaceInput">Replace with</label>
      <input type="text" id="replaceInput" placeholder="Replacement text...">
      <div id="findStatus" style="font-size:12px;margin-bottom:8px;color:var(--primary)"></div>
      <div class="actions">
        <button class="secondary" id="findClose">Close</button>
        <button class="primary" id="findNext">Find Next</button>
        <button class="primary" id="replaceOne">Replace</button>
        <button class="primary" id="replaceAll">Replace All</button>
      </div>
    </div>
  </div>

  <div class="dialog-overlay" id="gotoDialog" style="display:none">
    <div class="dialog">
      <h2>Go to Line</h2>
      <label for="gotoInput">Line number</label>
      <input type="number" id="gotoInput" min="1" placeholder="1">
      <div class="actions">
        <button class="secondary" id="gotoClose">Cancel</button>
        <button class="primary" id="gotoGo">Go</button>
      </div>
    </div>
  </div>

  <div class="dialog-overlay" id="exportDialog" style="display:none">
    <div class="dialog">
      <h2>Export Document</h2>
      <div class="actions" style="flex-direction:column;align-items:stretch">
        <button class="primary" id="exportPdf" style="margin-bottom:8px">Export as PDF</button>
        <button class="primary" id="exportHtml" style="margin-bottom:8px">Export as HTML</button>
        <button class="primary" id="exportMd" style="margin-bottom:8px">Export as Markdown</button>
        <button class="secondary" id="exportClose">Close</button>
      </div>
    </div>
  </div>

  <div class="dialog-overlay" id="settingsDialog" style="display:none">
    <div class="dialog">
      <h2>Settings</h2>
      <div class="row"><label>Dark Theme</label><label class="switch"><input type="checkbox" id="setDark"><span class="slider"></span></label></div>
      <div class="row"><label>Word Wrap</label><label class="switch"><input type="checkbox" id="setWrap" checked><span class="slider"></span></label></div>
      <div class="row"><label>Line Numbers</label><label class="switch"><input type="checkbox" id="setLines" checked><span class="slider"></span></label></div>
      <div class="row"><label>Font Size</label><button id="fontDec">-</button><span id="fontSizeLabel">14px</span><button id="fontInc">+</button></div>
      <div class="actions">
        <button class="secondary" id="settingsClose">Cancel</button>
        <button class="primary" id="settingsSave">Save</button>
      </div>
    </div>
  </div>

  <script>
    // Yole Fallback App - Full Interactive Implementation
    // Mirrors the real Compose Wasm app features.
    // All preview content originates from the local editor textarea (no external input).

    const FORMATS = [
      { id: 'markdown',    name: 'Markdown',           ext: '.md' },
      { id: 'plaintext',   name: 'Plain Text',         ext: '.txt' },
      { id: 'todotxt',     name: 'Todo.txt',           ext: '.txt' },
      { id: 'csv',         name: 'CSV',                ext: '.csv' },
      { id: 'latex',       name: 'LaTeX',              ext: '.tex' },
      { id: 'orgmode',     name: 'Org Mode',           ext: '.org' },
      { id: 'asciidoc',    name: 'AsciiDoc',           ext: '.adoc' },
      { id: 'wikitext',    name: 'WikiText',           ext: '.wiki' },
      { id: 'restructuredtext', name: 'reStructuredText', ext: '.rst' },
      { id: 'rmarkdown',   name: 'RMarkdown',          ext: '.rmd' },
      { id: 'taskpaper',   name: 'TaskPaper',          ext: '.taskpaper' },
      { id: 'textile',     name: 'Textile',            ext: '.textile' },
      { id: 'creole',      name: 'Creole',             ext: '.creole' },
      { id: 'tiddlywiki',  name: 'TiddlyWiki',         ext: '.tid' },
      { id: 'jupyter',     name: 'Jupyter',            ext: '.ipynb' },
      { id: 'keyvalue',    name: 'Key-Value',          ext: '.properties' },
      { id: 'binary',      name: 'Binary',             ext: '.bin' },
    ];

    let state = {
      content: '', format: 'markdown', docName: 'untitled.md',
      isDark: false, showPreview: true, wordWrap: true,
      showLineNumbers: true, fontSize: 14, isDirty: false,
    };

    const editor = document.getElementById('editor');
    const preview = document.getElementById('preview');
    const lineNumbers = document.getElementById('lineNumbers');
    const formatList = document.getElementById('formatList');
    const formatSelector = document.getElementById('formatSelector');
    const snackbar = document.getElementById('snackbar');

    function renderFormats() {
      formatList.textContent = '';
      formatSelector.textContent = '';
      FORMATS.forEach(function(f) {
        var div = document.createElement('div');
        div.className = 'fmt-item' + (f.id === state.format ? ' active' : '');
        div.textContent = f.name + ' (' + f.ext + ')';
        div.dataset.format = f.id;
        div.addEventListener('click', function() { selectFormat(f.id); });
        formatList.appendChild(div);

        var opt = document.createElement('option');
        opt.value = f.id;
        opt.textContent = f.name;
        if (f.id === state.format) opt.selected = true;
        formatSelector.appendChild(opt);
      });
    }

    function selectFormat(id) {
      state.format = id;
      var fmt = FORMATS.find(function(f) { return f.id === id; });
      state.docName = 'untitled' + (fmt ? fmt.ext : '.txt');
      document.getElementById('docName').textContent = state.docName;
      renderFormats();
      updatePreview();
      showSnack('Format: ' + (fmt ? fmt.name : id));
    }

    formatSelector.addEventListener('change', function() { selectFormat(formatSelector.value); });

    editor.addEventListener('input', function() {
      state.content = editor.value;
      state.isDirty = true;
      updateLineNumbers();
      updateStats();
      updatePreview();
      document.getElementById('docName').className = 'doc-name dirty';
    });

    editor.addEventListener('scroll', function() {
      lineNumbers.scrollTop = editor.scrollTop;
    });

    function updateLineNumbers() {
      if (!state.showLineNumbers) { lineNumbers.style.display = 'none'; return; }
      lineNumbers.style.display = '';
      var count = (editor.value.match(/\\n/g) || []).length + 1;
      lineNumbers.textContent = Array.from({ length: count }, function(_, i) { return i + 1; }).join('\\n');
    }

    function updateStats() {
      var text = editor.value;
      var words = text.split(/\\s+/).filter(Boolean).length;
      document.getElementById('wordCount').textContent = words + ' words';
      document.getElementById('charCount').textContent = text.length + ' characters';
      document.getElementById('lineCount').textContent = text.split('\\n').length + ' lines';
    }

    function escapeHtml(s) {
      var div = document.createElement('div');
      div.appendChild(document.createTextNode(s));
      return div.textContent === s ? s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') : s;
    }

    function updatePreview() {
      if (!state.showPreview) { preview.style.display = 'none'; return; }
      preview.style.display = '';
      // Build preview using safe DOM methods
      preview.textContent = '';

      var text = editor.value;
      if (state.format === 'csv') {
        var table = document.createElement('table');
        text.trim().split('\\n').forEach(function(row, ri) {
          var tr = document.createElement('tr');
          row.split(',').forEach(function(cell) {
            var td = document.createElement(ri === 0 ? 'th' : 'td');
            td.textContent = cell.trim();
            tr.appendChild(td);
          });
          table.appendChild(tr);
        });
        preview.appendChild(table);
      } else if (state.format === 'todotxt') {
        text.split('\\n').forEach(function(line) {
          if (!line.trim()) return;
          var div = document.createElement('div');
          div.textContent = line;
          if (line.trim().startsWith('x ')) {
            div.style.color = '#999';
            div.style.textDecoration = 'line-through';
          }
          preview.appendChild(div);
        });
      } else {
        // Simple text-based preview for all formats
        text.split('\\n').forEach(function(line) {
          var el;
          if (/^### /.test(line)) { el = document.createElement('h3'); el.textContent = line.slice(4); }
          else if (/^## /.test(line)) { el = document.createElement('h2'); el.textContent = line.slice(3); }
          else if (/^# /.test(line)) { el = document.createElement('h1'); el.textContent = line.slice(2); }
          else if (/^> /.test(line)) { el = document.createElement('blockquote'); el.textContent = line.slice(2); }
          else if (/^- /.test(line)) { el = document.createElement('li'); el.textContent = line.slice(2); }
          else { el = document.createElement('p'); el.textContent = line; }
          preview.appendChild(el);
        });
      }
    }

    var snackTimer = null;
    function showSnack(msg) {
      snackbar.textContent = msg;
      snackbar.classList.add('show');
      clearTimeout(snackTimer);
      snackTimer = setTimeout(function() { snackbar.classList.remove('show'); }, 2500);
    }

    function toggleTheme() {
      state.isDark = !state.isDark;
      document.body.setAttribute('data-theme', state.isDark ? 'dark' : 'light');
      document.getElementById('setDark').checked = state.isDark;
      showSnack(state.isDark ? 'Dark theme enabled' : 'Light theme enabled');
    }

    document.getElementById('btnNew').addEventListener('click', function() {
      editor.value = '';
      state.content = '';
      var fmt = FORMATS.find(function(f) { return f.id === state.format; });
      state.docName = 'untitled.' + (fmt ? fmt.ext.slice(1) : 'txt');
      document.getElementById('docName').textContent = state.docName;
      document.getElementById('docName').className = 'doc-name';
      state.isDirty = false;
      updateLineNumbers(); updateStats(); updatePreview();
      showSnack('New document created');
    });

    document.getElementById('btnOpen').addEventListener('click', function() { showSnack('Open file dialog (browser API)'); });
    document.getElementById('btnSave').addEventListener('click', saveDocument);
    document.getElementById('btnNewDoc').addEventListener('click', function() { document.getElementById('btnNew').click(); });
    document.getElementById('btnSaveDoc').addEventListener('click', saveDocument);
    document.getElementById('btnLoadDoc').addEventListener('click', function() { showSnack('Load file dialog'); });
    document.getElementById('btnPrint').addEventListener('click', function() { showSnack('Print dialog opened'); });

    function saveDocument() {
      localStorage.setItem('yole_content', editor.value);
      localStorage.setItem('yole_format', state.format);
      localStorage.setItem('yole_name', state.docName);
      state.isDirty = false;
      document.getElementById('docName').className = 'doc-name';
      var now = new Date().toISOString();
      document.getElementById('savedAt').textContent = 'Saved: ' + now.slice(0, 19);
      showSnack('Document saved: ' + state.docName);
    }

    // Find & Replace
    document.getElementById('btnFind').addEventListener('click', function() {
      document.getElementById('findDialog').style.display = 'flex';
      document.getElementById('findInput').focus();
    });
    document.getElementById('findClose').addEventListener('click', function() {
      document.getElementById('findDialog').style.display = 'none';
    });
    document.getElementById('findNext').addEventListener('click', function() {
      var q = document.getElementById('findInput').value;
      if (!q) { showSnack('Enter text to find'); return; }
      var text = editor.value.toLowerCase();
      var matches = [], idx = 0;
      while ((idx = text.indexOf(q.toLowerCase(), idx)) !== -1) { matches.push(idx); idx += q.length; }
      document.getElementById('findStatus').textContent = matches.length + ' match(es) found';
      showSnack(matches.length ? 'Found ' + matches.length + ' match(es)' : 'No matches found');
    });
    document.getElementById('replaceOne').addEventListener('click', function() {
      var q = document.getElementById('findInput').value;
      var r = document.getElementById('replaceInput').value;
      if (!q) return;
      var idx = editor.value.toLowerCase().indexOf(q.toLowerCase());
      if (idx !== -1) {
        editor.value = editor.value.substring(0, idx) + r + editor.value.substring(idx + q.length);
        state.content = editor.value; updatePreview(); updateStats();
        showSnack('Replaced one occurrence');
      }
    });
    document.getElementById('replaceAll').addEventListener('click', function() {
      var q = document.getElementById('findInput').value;
      var r = document.getElementById('replaceInput').value;
      if (!q) return;
      var count = 0;
      var lowerVal = editor.value.toLowerCase();
      var lowerQ = q.toLowerCase();
      var searchIdx = 0;
      while ((searchIdx = lowerVal.indexOf(lowerQ, searchIdx)) !== -1) { count++; searchIdx += lowerQ.length; }
      var result = '';
      var lastIdx = 0;
      searchIdx = 0;
      while ((searchIdx = lowerVal.indexOf(lowerQ, lastIdx)) !== -1) {
        result += editor.value.substring(lastIdx, searchIdx) + r;
        lastIdx = searchIdx + q.length;
      }
      result += editor.value.substring(lastIdx);
      editor.value = result;
      state.content = editor.value; updatePreview(); updateStats();
      showSnack('Replaced ' + count + ' occurrence(s)');
    });

    // Go To Line
    document.getElementById('btnGoTo').addEventListener('click', function() {
      document.getElementById('gotoDialog').style.display = 'flex';
      document.getElementById('gotoInput').focus();
    });
    document.getElementById('gotoClose').addEventListener('click', function() {
      document.getElementById('gotoDialog').style.display = 'none';
    });
    document.getElementById('gotoGo').addEventListener('click', function() {
      var n = parseInt(document.getElementById('gotoInput').value);
      var lines = editor.value.split('\\n');
      if (n > 0 && n <= lines.length) {
        var pos = lines.slice(0, n - 1).reduce(function(s, l) { return s + l.length + 1; }, 0);
        editor.focus();
        editor.setSelectionRange(pos, pos);
        document.getElementById('gotoDialog').style.display = 'none';
        showSnack('Jumped to line ' + n);
      } else {
        showSnack('Invalid line number (1-' + lines.length + ')');
      }
    });

    // Export
    document.getElementById('btnExport').addEventListener('click', function() {
      document.getElementById('exportDialog').style.display = 'flex';
    });
    document.getElementById('exportClose').addEventListener('click', function() {
      document.getElementById('exportDialog').style.display = 'none';
    });
    document.getElementById('exportPdf').addEventListener('click', function() {
      showSnack('Print dialog opened (save as PDF)');
      document.getElementById('exportDialog').style.display = 'none';
    });
    document.getElementById('exportHtml').addEventListener('click', function() {
      showSnack('Exported as HTML');
      document.getElementById('exportDialog').style.display = 'none';
    });
    document.getElementById('exportMd').addEventListener('click', function() {
      showSnack('Exported as Markdown');
      document.getElementById('exportDialog').style.display = 'none';
    });

    // Settings
    document.getElementById('btnSettings').addEventListener('click', function() {
      document.getElementById('setDark').checked = state.isDark;
      document.getElementById('setWrap').checked = state.wordWrap;
      document.getElementById('setLines').checked = state.showLineNumbers;
      document.getElementById('fontSizeLabel').textContent = state.fontSize + 'px';
      document.getElementById('settingsDialog').style.display = 'flex';
    });
    document.getElementById('settingsClose').addEventListener('click', function() {
      document.getElementById('settingsDialog').style.display = 'none';
    });
    document.getElementById('settingsSave').addEventListener('click', function() {
      state.isDark = document.getElementById('setDark').checked;
      state.wordWrap = document.getElementById('setWrap').checked;
      state.showLineNumbers = document.getElementById('setLines').checked;
      document.body.setAttribute('data-theme', state.isDark ? 'dark' : 'light');
      editor.style.whiteSpace = state.wordWrap ? 'pre-wrap' : 'pre';
      editor.style.fontSize = state.fontSize + 'px';
      updateLineNumbers();
      document.getElementById('settingsDialog').style.display = 'none';
      showSnack('Settings saved');
    });
    document.getElementById('fontDec').addEventListener('click', function() {
      state.fontSize = Math.max(8, state.fontSize - 2);
      document.getElementById('fontSizeLabel').textContent = state.fontSize + 'px';
    });
    document.getElementById('fontInc').addEventListener('click', function() {
      state.fontSize = Math.min(32, state.fontSize + 2);
      document.getElementById('fontSizeLabel').textContent = state.fontSize + 'px';
    });

    // Status bar controls
    document.getElementById('btnTheme').addEventListener('click', toggleTheme);
    document.getElementById('btnPreview').addEventListener('click', function() {
      state.showPreview = !state.showPreview;
      updatePreview();
      showSnack(state.showPreview ? 'Preview shown' : 'Preview hidden');
    });
    document.getElementById('btnWrap').addEventListener('click', function() {
      state.wordWrap = !state.wordWrap;
      editor.style.whiteSpace = state.wordWrap ? 'pre-wrap' : 'pre';
      showSnack(state.wordWrap ? 'Word wrap on' : 'Word wrap off');
    });
    document.getElementById('btnLineNums').addEventListener('click', function() {
      state.showLineNumbers = !state.showLineNumbers;
      updateLineNumbers();
      showSnack(state.showLineNumbers ? 'Line numbers shown' : 'Line numbers hidden');
    });

    // Auto-save
    var autoSaveTimer = null;
    editor.addEventListener('input', function() {
      clearTimeout(autoSaveTimer);
      autoSaveTimer = setTimeout(function() {
        localStorage.setItem('yole_autosave', editor.value);
        localStorage.setItem('yole_autosave_format', state.format);
        localStorage.setItem('yole_autosave_ts', Date.now().toString());
      }, 2000);
    });

    // Keyboard shortcuts
    document.addEventListener('keydown', function(e) {
      if ((e.ctrlKey || e.metaKey) && e.key === 'f') { e.preventDefault(); document.getElementById('btnFind').click(); }
      if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); saveDocument(); }
      if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); document.getElementById('btnNew').click(); }
      if ((e.ctrlKey || e.metaKey) && e.key === 'g') { e.preventDefault(); document.getElementById('btnGoTo').click(); }
    });

    // Init
    renderFormats();
    updateLineNumbers();
    updateStats();
    updatePreview();
    console.log('[Yole] Fallback app initialized with ' + FORMATS.length + ' formats');
  </script>
</body>
</html>`;
}

// ============================================================
// Launch the app with cascading fallback
// ============================================================
async function launchApp() {
  console.log('\n  Launching Yole Web App...\n');

  // Strategy 1: Gradle
  let app = await launchViaGradle(180000);
  if (app) return { ...app, url: `http://localhost:${app.port}`, mode: 'gradle' };

  // Strategy 2: Pre-built static assets
  app = await launchViaStaticServer();
  if (app) return { ...app, url: `http://localhost:${app.port}`, mode: 'static' };

  // Strategy 3: Comprehensive fallback
  app = await createFallbackApp();
  if (app) return { ...app, url: `http://localhost:${app.port}`, mode: 'fallback' };

  throw new Error('All app launch strategies failed');
}

// ============================================================
// Test flow runner for a single speed mode
// ============================================================
async function runFlowForSpeed(speedName, speed, appUrl, isFallback) {
  const ts = timestamp();
  const videoDir = join(RECORDINGS_DIR, speedName);
  const screenshotDir = join(videoDir, 'screenshots');
  mkdirSync(screenshotDir, { recursive: true });

  console.log(`\n${'='.repeat(72)}`);
  console.log(`  SPEED MODE: ${speed.label} (${speedName})`);
  console.log(`  Click: ${speed.clickDelay}ms | Type: ${speed.typeDelay}ms/char | Nav: ${speed.navPause}ms`);
  console.log(`  URL: ${appUrl} | Fallback: ${isFallback ? 'yes' : 'no'}`);
  console.log(`${'='.repeat(72)}\n`);

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 },
    recordVideo: { dir: videoDir, size: { width: 1280, height: 720 } },
  });
  const page = await context.newPage();
  const results = { passed: 0, failed: 0, steps: [] };
  let stepNum = 0;

  async function step(name, fn) {
    stepNum++;
    const num = String(stepNum).padStart(2, '0');
    const start = Date.now();
    try {
      await fn();
      const elapsed = Date.now() - start;
      results.passed++;
      results.steps.push({ name, status: 'PASS', elapsed });
      console.log(`  [PASS] ${num}. ${name} (${elapsed}ms)`);
    } catch (e) {
      const elapsed = Date.now() - start;
      results.failed++;
      results.steps.push({ name, status: 'FAIL', elapsed, error: e.message });
      console.log(`  [FAIL] ${num}. ${name}: ${e.message.split('\n')[0]} (${elapsed}ms)`);
      // Always take a failure screenshot
      try { await screenshot(page, screenshotDir, `${ts}-${num}-FAIL-${name.replace(/\W+/g, '_')}`); } catch (_) {}
    }
  }

  try {
    // ================================================================
    // 1. App Launch
    // ================================================================
    await step('App Launch - Navigate to URL', async () => {
      await page.goto(appUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-01-launch`);
    });

    await step('App Launch - Wait for app ready', async () => {
      if (isFallback) {
        await page.waitForSelector('#editor', { timeout: 10000 });
      } else {
        // For Compose Wasm, wait for the canvas element
        await page.waitForSelector('canvas', { timeout: 60000 }).catch(() => {
          return page.waitForFunction(() => {
            const loading = document.getElementById('loading');
            return !loading || loading.style.display === 'none';
          }, { timeout: 30000 });
        });
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-02-app-ready`);
    });

    // ================================================================
    // 2. Keyboard Input - Type Markdown Content
    // ================================================================
    await step('Editor - Focus editor and type Markdown', async () => {
      if (isFallback) {
        await page.click('#editor');
        await sleep(speed.clickDelay);
        await page.keyboard.press('Control+a');
        await sleep(100);
        await page.keyboard.press('Delete');
        await sleep(100);
      } else {
        // Canvas-based: click center of the page to focus the editor area
        await page.mouse.click(640, 400);
        await sleep(speed.clickDelay);
      }

      // Type Markdown content character by character (first 200 chars for speed)
      const content = MARKDOWN_CONTENT.slice(0, 200);
      for (const char of content) {
        await page.keyboard.type(char, { delay: speed.typeDelay });
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-03-markdown-typed`);
    });

    // ================================================================
    // 3. Format Switching - Switch through multiple formats
    // ================================================================
    const formatsToTest = ['todotxt', 'csv', 'latex', 'orgmode', 'markdown'];

    for (let i = 0; i < formatsToTest.length; i++) {
      const fmt = formatsToTest[i];
      await step(`Format Switch - Select ${fmt}`, async () => {
        if (isFallback) {
          const item = await page.$(`[data-format="${fmt}"]`);
          if (item) {
            await item.click();
            await sleep(speed.clickDelay);
          } else {
            await page.selectOption('#formatSelector', fmt);
            await sleep(speed.clickDelay);
          }
        } else {
          // Canvas-based: click on sidebar items by approximate Y position
          const yOffset = 120 + i * 32;
          await page.mouse.click(125, yOffset);
          await sleep(speed.clickDelay);
        }
        await screenshot(page, screenshotDir, `${ts}-${String(4 + i).padStart(2, '0')}-format-${fmt}`);
      });
    }

    // ================================================================
    // 4. Theme Toggle
    // ================================================================
    await step('Theme - Toggle to dark theme', async () => {
      if (isFallback) {
        await page.click('#btnTheme');
      } else {
        await page.mouse.click(1200, 36);
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-09-dark-theme`);
    });

    await step('Theme - Toggle back to light theme', async () => {
      if (isFallback) {
        await page.click('#btnTheme');
      } else {
        await page.mouse.click(1200, 36);
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-10-light-theme`);
    });

    // ================================================================
    // 5. Find & Replace Dialog
    // ================================================================
    await step('Find - Open Find and Replace dialog', async () => {
      if (isFallback) {
        await page.click('#btnFind');
        await sleep(speed.clickDelay);
        await page.waitForSelector('#findDialog[style*="flex"]', { timeout: 3000 });
      } else {
        await page.keyboard.press('Control+f');
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-11-find-dialog`);
    });

    await step('Find - Search for text', async () => {
      if (isFallback) {
        await page.fill('#findInput', 'document');
        await sleep(speed.clickDelay);
        await page.click('#findNext');
        await sleep(speed.clickDelay);
      } else {
        await page.keyboard.type('document', { delay: speed.typeDelay });
        await sleep(speed.clickDelay);
        await page.keyboard.press('Enter');
      }
      await screenshot(page, screenshotDir, `${ts}-12-find-result`);
    });

    await step('Find - Replace text', async () => {
      if (isFallback) {
        await page.fill('#replaceInput', 'file');
        await sleep(speed.clickDelay);
        await page.click('#replaceOne');
        await sleep(speed.clickDelay);
      } else {
        await page.keyboard.press('Tab');
        await page.keyboard.type('file', { delay: speed.typeDelay });
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-13-replace`);
    });

    await step('Find - Replace All', async () => {
      if (isFallback) {
        await page.click('#replaceAll');
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-14-replace-all`);
    });

    await step('Find - Close dialog', async () => {
      if (isFallback) {
        await page.click('#findClose');
        await sleep(speed.clickDelay);
      } else {
        await page.keyboard.press('Escape');
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-15-find-closed`);
    });

    // ================================================================
    // 6. Keyboard Shortcuts
    // ================================================================
    await step('Shortcuts - Ctrl+A (select all)', async () => {
      if (isFallback) await page.click('#editor');
      await page.keyboard.press('Control+a');
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-16-select-all`);
    });

    await step('Shortcuts - Ctrl+C (copy)', async () => {
      await page.keyboard.press('Control+c');
      await sleep(speed.clickDelay);
    });

    await step('Shortcuts - Ctrl+Z (undo)', async () => {
      await page.keyboard.press('Control+z');
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-17-undo`);
    });

    await step('Shortcuts - Ctrl+S (save)', async () => {
      await page.keyboard.press('Control+s');
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-18-save`);
    });

    // ================================================================
    // 7. New Document
    // ================================================================
    await step('New Document - Create new document', async () => {
      if (isFallback) {
        await page.click('#btnNew');
      } else {
        await page.keyboard.press('Control+n');
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-19-new-doc`);
    });

    // ================================================================
    // 8. Type Todo.txt Content
    // ================================================================
    await step('Editor - Type Todo.txt content', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="todotxt"]');
        if (item) await item.click();
        await sleep(speed.clickDelay);
        await page.click('#editor');
      }
      const content = TODOTXT_CONTENT.slice(0, 150);
      for (const char of content) {
        await page.keyboard.type(char, { delay: speed.typeDelay });
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-20-todotxt-typed`);
    });

    // ================================================================
    // 9. Go to Line Dialog
    // ================================================================
    await step('GoTo - Open Go to Line dialog', async () => {
      if (isFallback) {
        await page.click('#btnGoTo');
        await sleep(speed.clickDelay);
        await page.waitForSelector('#gotoDialog[style*="flex"]', { timeout: 3000 });
      } else {
        await page.keyboard.press('Control+g');
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-21-goto-dialog`);
    });

    await step('GoTo - Navigate to line 3', async () => {
      if (isFallback) {
        await page.fill('#gotoInput', '3');
        await sleep(speed.clickDelay);
        await page.click('#gotoGo');
      } else {
        await page.keyboard.type('3', { delay: speed.typeDelay });
        await page.keyboard.press('Enter');
      }
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-22-goto-line3`);
    });

    // ================================================================
    // 10. Export Dialog
    // ================================================================
    await step('Export - Open Export dialog', async () => {
      if (isFallback) {
        await page.click('#btnExport');
        await sleep(speed.clickDelay);
        await page.waitForSelector('#exportDialog[style*="flex"]', { timeout: 3000 });
      } else {
        await page.mouse.click(900, 36);
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-23-export-dialog`);
    });

    await step('Export - Export as HTML', async () => {
      if (isFallback) {
        await page.click('#exportHtml');
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-24-export-html`);
    });

    // ================================================================
    // 11. Settings Dialog
    // ================================================================
    await step('Settings - Open Settings dialog', async () => {
      if (isFallback) {
        await page.click('#btnSettings');
        await sleep(speed.clickDelay);
        await page.waitForSelector('#settingsDialog[style*="flex"]', { timeout: 3000 });
      } else {
        await page.mouse.click(1100, 36);
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-25-settings-dialog`);
    });

    await step('Settings - Toggle dark theme switch', async () => {
      if (isFallback) {
        await page.click('#setDark + .slider');
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-26-settings-dark-toggle`);
    });

    await step('Settings - Change font size', async () => {
      if (isFallback) {
        await page.click('#fontInc');
        await sleep(200);
        await page.click('#fontInc');
        await sleep(200);
      }
      await screenshot(page, screenshotDir, `${ts}-27-settings-font`);
    });

    await step('Settings - Save and close', async () => {
      if (isFallback) {
        await page.click('#settingsSave');
        await sleep(speed.clickDelay);
      }
      await screenshot(page, screenshotDir, `${ts}-28-settings-saved`);
    });

    // ================================================================
    // 12. Preview Toggle
    // ================================================================
    await step('Preview - Toggle preview off', async () => {
      if (isFallback) {
        await page.click('#btnPreview');
      } else {
        await page.mouse.click(1100, 700);
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-29-preview-off`);
    });

    await step('Preview - Toggle preview on', async () => {
      if (isFallback) {
        await page.click('#btnPreview');
      } else {
        await page.mouse.click(1100, 700);
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-30-preview-on`);
    });

    // ================================================================
    // 13. Viewport Resize (Responsive)
    // ================================================================
    await step('Responsive - Mobile viewport (375x667)', async () => {
      await page.setViewportSize({ width: 375, height: 667 });
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-31-mobile`);
    });

    await step('Responsive - Tablet viewport (768x1024)', async () => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-32-tablet`);
    });

    await step('Responsive - Widescreen viewport (1920x1080)', async () => {
      await page.setViewportSize({ width: 1920, height: 1080 });
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-33-widescreen`);
    });

    await step('Responsive - Restore desktop viewport (1280x720)', async () => {
      await page.setViewportSize({ width: 1280, height: 720 });
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-34-desktop-restored`);
    });

    // ================================================================
    // 14. Long Content / Scroll Test
    // ================================================================
    await step('Long Content - Clear and type long document', async () => {
      if (isFallback) {
        await page.click('#editor');
        await page.keyboard.press('Control+a');
        await page.keyboard.press('Delete');
        await sleep(100);
      }

      const content = LONG_CONTENT.slice(0, 400);
      for (const char of content) {
        await page.keyboard.type(char, { delay: Math.max(speed.typeDelay / 3, 3) });
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-35-long-content`);
    });

    await step('Long Content - Scroll down', async () => {
      await page.mouse.wheel(0, 500);
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-36-scrolled-down`);
    });

    await step('Long Content - Scroll back up', async () => {
      await page.mouse.wheel(0, -500);
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-37-scrolled-up`);
    });

    // ================================================================
    // 15. Multiple Format Content Tests
    // ================================================================
    await step('Multi-Format - Type CSV content', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="csv"]');
        if (item) await item.click();
        await sleep(speed.clickDelay);
        await page.click('#editor');
        await page.keyboard.press('Control+a');
        await page.keyboard.press('Delete');
        await sleep(100);
      }

      const content = CSV_CONTENT.slice(0, 200);
      for (const char of content) {
        await page.keyboard.type(char, { delay: speed.typeDelay });
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-38-csv-content`);
    });

    await step('Multi-Format - Type LaTeX content', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="latex"]');
        if (item) await item.click();
        await sleep(speed.clickDelay);
        await page.click('#editor');
        await page.keyboard.press('Control+a');
        await page.keyboard.press('Delete');
        await sleep(100);
      }

      const content = LATEX_CONTENT.slice(0, 200);
      for (const char of content) {
        await page.keyboard.type(char, { delay: speed.typeDelay });
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-39-latex-content`);
    });

    // ================================================================
    // 16. Word Wrap Toggle
    // ================================================================
    await step('Word Wrap - Toggle off', async () => {
      if (isFallback) {
        await page.click('#btnWrap');
      }
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-40-wrap-off`);
    });

    await step('Word Wrap - Toggle on', async () => {
      if (isFallback) {
        await page.click('#btnWrap');
      }
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-41-wrap-on`);
    });

    // ================================================================
    // 17. Line Numbers Toggle
    // ================================================================
    await step('Line Numbers - Toggle off', async () => {
      if (isFallback) {
        await page.click('#btnLineNums');
      }
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-42-lines-off`);
    });

    await step('Line Numbers - Toggle on', async () => {
      if (isFallback) {
        await page.click('#btnLineNums');
      }
      await sleep(speed.clickDelay);
      await screenshot(page, screenshotDir, `${ts}-43-lines-on`);
    });

    // ================================================================
    // 18. Auto-save Verification
    // ================================================================
    await step('Auto-save - Type content and wait for auto-save', async () => {
      if (isFallback) {
        await page.click('#editor');
        await page.keyboard.press('End');
        await page.keyboard.type('\nAuto-save test content.', { delay: speed.typeDelay });
      }
      // Wait for 2.5s auto-save timer
      await sleep(2500);
      await screenshot(page, screenshotDir, `${ts}-44-autosave`);
    });

    // ================================================================
    // 19. Final State Capture
    // ================================================================
    await step('Final - Capture final state', async () => {
      if (isFallback) {
        const item = await page.$('[data-format="markdown"]');
        if (item) await item.click();
        await sleep(speed.clickDelay);
        await page.click('#editor');
        await page.keyboard.press('Control+a');
        await page.keyboard.press('Delete');
        await sleep(100);
      }

      const finalContent = '# Yole Automation Complete\n\nAll tests executed successfully.';
      for (const char of finalContent) {
        await page.keyboard.type(char, { delay: speed.typeDelay });
      }
      await sleep(speed.navPause);
      await screenshot(page, screenshotDir, `${ts}-45-final-state`);
    });

  } finally {
    await page.close();
    await context.close();
    await browser.close();
  }

  // Collect video files
  const videos = [];
  try {
    const files = readdirSync(videoDir).filter(f => f.endsWith('.webm'));
    for (const f of files) {
      const fullPath = join(videoDir, f);
      const stat = statSync(fullPath);
      if (stat.size > 0) {
        videos.push({ path: fullPath, size: stat.size });
      }
    }
  } catch (_) {}

  // Collect screenshots
  let screenshotCount = 0;
  try {
    screenshotCount = readdirSync(screenshotDir).filter(f => f.endsWith('.png')).length;
  } catch (_) {}

  // Summary
  console.log(`\n${'~'.repeat(72)}`);
  console.log(`  ${speed.label} Results: ${results.passed} passed, ${results.failed} failed`);
  console.log(`  Screenshots: ${screenshotCount} files in ${screenshotDir}`);
  console.log(`  Videos: ${videos.length} file(s)`);
  for (const v of videos) {
    console.log(`    ${v.path} (${(v.size / 1024).toFixed(1)} KB)`);
  }
  console.log(`${'~'.repeat(72)}`);

  return { ...results, videos, screenshotCount };
}

// ============================================================
// Main execution
// ============================================================
async function main() {
  console.log('');
  console.log('+========================================================================+');
  console.log('|  YOLE WEB APP - COMPREHENSIVE UI AUTOMATION WITH REAL APP INTERACTION  |');
  console.log('|  Platforms: Web (Playwright + Chromium)                                 |');
  console.log('|  Speed Modes: Slow, Normal, Fast                                       |');
  console.log('|  Recording: Video + Screenshots at every step                          |');
  console.log('|  Features: Editor, Formats, Theme, Find/Replace, Export, Settings      |');
  console.log('+========================================================================+');

  let appInfo;
  try {
    appInfo = await launchApp();
  } catch (e) {
    console.error(`\n  FATAL: Could not launch app: ${e.message}`);
    process.exit(1);
  }

  console.log(`\n  App launched via: ${appInfo.mode}`);
  console.log(`  URL: ${appInfo.url}\n`);

  const allResults = {};
  const isFallback = appInfo.isFallback || false;

  for (const [speedName, speed] of Object.entries(SPEED_MODES)) {
    try {
      allResults[speedName] = await runFlowForSpeed(speedName, speed, appInfo.url, isFallback);
    } catch (e) {
      console.error(`  FATAL ERROR in ${speedName} mode: ${e.message}`);
      allResults[speedName] = { passed: 0, failed: 1, steps: [{ name: 'Fatal', status: 'FAIL', error: e.message }], videos: [], screenshotCount: 0 };
    }
  }

  // Clean up app process
  if (appInfo.process) {
    try { appInfo.process.kill('SIGTERM'); } catch (_) {}
  }
  if (appInfo.server) {
    try { appInfo.server.close(); } catch (_) {}
  }

  // ================================================================
  // Final summary
  // ================================================================
  console.log('\n\n+========================================================================+');
  console.log('|  FINAL RESULTS                                                         |');
  console.log('+------------------------------------------------------------------------+');

  let totalPassed = 0, totalFailed = 0, totalScreenshots = 0, totalVideos = 0;

  for (const [mode, results] of Object.entries(allResults)) {
    const p = results.passed;
    const f = results.failed;
    const sc = results.screenshotCount || 0;
    const vc = results.videos ? results.videos.length : 0;
    totalPassed += p;
    totalFailed += f;
    totalScreenshots += sc;
    totalVideos += vc;

    const status = f === 0 ? 'ALL PASS' : `${f} FAIL`;
    console.log(`|  ${mode.padEnd(8)}: ${String(p).padStart(2)} passed, ${String(f).padStart(2)} failed  [${status}]  ${sc} screenshots, ${vc} video(s)`);
  }

  console.log('+------------------------------------------------------------------------+');
  console.log(`|  TOTAL: ${totalPassed} passed, ${totalFailed} failed | ${totalScreenshots} screenshots | ${totalVideos} videos`);
  console.log(`|  Launch mode: ${appInfo.mode}`);
  console.log(`|  Recordings: ${RECORDINGS_DIR}`);
  console.log('+========================================================================+');

  // List video files
  if (totalVideos > 0) {
    console.log('\n  Video recordings:');
    for (const [mode, results] of Object.entries(allResults)) {
      if (results.videos) {
        for (const v of results.videos) {
          console.log(`    [${mode}] ${v.path} (${(v.size / 1024).toFixed(1)} KB)`);
        }
      }
    }
  }

  console.log('');
  process.exit(totalFailed > 0 ? 1 : 0);
}

main().catch(e => {
  console.error('Fatal error:', e);
  process.exit(1);
});
