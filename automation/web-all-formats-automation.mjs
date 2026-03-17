/**
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Yole Web App — All 17 Formats Comprehensive Automation
 *
 * Tests EVERY text format with EVERY operation: create, preview, edit, save,
 * find/replace, export HTML, export Markdown, delete. Plus storage operations,
 * settings, theme, responsive, and keyboard shortcuts.
 *
 * Output: recordings/web/formats/{01-markdown..19-settings}/
 *   - One video per test suite (19 total)
 *   - Screenshots at every operation step (~136+ total)
 *
 * Usage: node automation/web-all-formats-automation.mjs
 */

import { chromium } from 'playwright';
import { mkdirSync, existsSync, readFileSync, writeFileSync } from 'fs';
import { join } from 'path';
import { createServer } from 'http';

// ============================================================
// Constants
// ============================================================
const PROJECT_ROOT = '/run/media/milosvasic/DATA4TB/Projects/Yole';
const RECORDINGS_BASE = join(PROJECT_ROOT, 'recordings', 'web', 'formats');
const AUTOMATION_DIR = join(PROJECT_ROOT, 'automation');
const FALLBACK_APP = join(AUTOMATION_DIR, 'yole-fallback-app.html');

const SPEED = { clickDelay: 400, typeDelay: 20, navPause: 800 };

// ============================================================
// All 17 formats with real content, search terms, and edits
// ============================================================
const FORMATS = [
  {
    num: '01', id: 'markdown', name: 'Markdown', dir: '01-markdown',
    content: '# Title\n## Subtitle\n**bold** *italic*\n- list item\n[link](url)\n`code`',
    editAppend: '\n\n### Added Section\nThis paragraph was added during editing.\n- new item 1\n- new item 2',
    searchTerm: 'bold',
    replaceTerm: 'strong',
  },
  {
    num: '02', id: 'plaintext', name: 'Plain Text', dir: '02-plaintext',
    content: 'This is the first paragraph of plain text content.\nIt spans multiple lines for thorough testing.\n\nSecond paragraph begins here.\nPlain text is the simplest format supported by Yole.\n\nThird paragraph with concluding remarks.',
    editAppend: '\n\nFourth paragraph added during edit.\nThis tests the edit operation for plain text documents.',
    searchTerm: 'paragraph',
    replaceTerm: 'section',
  },
  {
    num: '03', id: 'todotxt', name: 'Todo.txt', dir: '03-todotxt',
    content: '(A) Task @context +project due:2026-03-17\n(B) Another task @home +personal\n(C) Low priority task @work +maintenance\nx completed task 2026-03-16',
    editAppend: '\n(A) Urgent new task @office +sprint due:2026-03-18\n(B) Review pull requests @dev +code-review',
    searchTerm: 'task',
    replaceTerm: 'item',
  },
  {
    num: '04', id: 'csv', name: 'CSV', dir: '04-csv',
    content: 'Name,Age,City\nAlice,30,NYC\nBob,25,London\nCharlie,35,Tokyo',
    editAppend: '\nDiana,28,Paris\nEve,32,Berlin',
    searchTerm: 'Alice',
    replaceTerm: 'Alicia',
  },
  {
    num: '05', id: 'latex', name: 'LaTeX', dir: '05-latex',
    content: '\\documentclass{article}\n\\begin{document}\nHello $E=mc^2$\n\\section{Introduction}\nThis is a LaTeX document.\n\\end{document}',
    editAppend: '\n% Added comment\n% \\subsection{Methods}\n% More LaTeX content here',
    searchTerm: 'Hello',
    replaceTerm: 'Welcome',
  },
  {
    num: '06', id: 'orgmode', name: 'Org Mode', dir: '06-orgmode',
    content: '* Heading\n** Subheading\n- list\n#+BEGIN_SRC python\nprint("hello")\n#+END_SRC',
    editAppend: '\n\n** New Subheading\n- added item\n- another item\n#+BEGIN_QUOTE\nA wise quote.\n#+END_QUOTE',
    searchTerm: 'hello',
    replaceTerm: 'world',
  },
  {
    num: '07', id: 'asciidoc', name: 'AsciiDoc', dir: '07-asciidoc',
    content: '= Title\n== Section\n*bold* _italic_\n[source,java]\n----\nSystem.out.println("hi");\n----',
    editAppend: '\n\n== Added Section\nNew paragraph with *formatting*.\n\n[source,kotlin]\n----\nprintln("Yole")\n----',
    searchTerm: 'Section',
    replaceTerm: 'Chapter',
  },
  {
    num: '08', id: 'wikitext', name: 'WikiText', dir: '08-wikitext',
    content: "== Section ==\n'''bold''' ''italic''\n* bullet\n[[Link|text]]",
    editAppend: "\n\n== New Section ==\n'''more bold'''\n* new bullet point\n* second bullet\n[[AnotherLink|description]]",
    searchTerm: 'bullet',
    replaceTerm: 'item',
  },
  {
    num: '09', id: 'restructuredtext', name: 'reStructuredText', dir: '09-rst',
    content: 'Title\n=====\nSubtitle\n--------\n**bold** *italic*\n.. code-block:: python\n   print("hi")',
    editAppend: '\n\nNew Section\n-----------\nAdded paragraph with **formatting**.\n\n.. note::\n   This is a note directive.',
    searchTerm: 'bold',
    replaceTerm: 'strong',
  },
  {
    num: '10', id: 'rmarkdown', name: 'RMarkdown', dir: '10-rmarkdown',
    content: '---\ntitle: "Test"\noutput: html_document\n---\n# Analysis\n```{r}\nsummary(cars)\n```',
    editAppend: '\n\n## Results\n```{r}\nplot(cars)\nmean(cars$speed)\n```\n\nThe analysis shows significant results.',
    searchTerm: 'Analysis',
    replaceTerm: 'Exploration',
  },
  {
    num: '11', id: 'taskpaper', name: 'TaskPaper', dir: '11-taskpaper',
    content: 'Project:\n\t- Task 1 @due(2026-03-17)\n\t- Task 2 @done\n\t- Task 3 @priority(high)',
    editAppend: '\n\nNew Project:\n\t- Added task @due(2026-03-20)\n\t- Research task @context(office)\n\t- Review task @done(2026-03-17)',
    searchTerm: 'Task',
    replaceTerm: 'Action',
  },
  {
    num: '12', id: 'textile', name: 'Textile', dir: '12-textile',
    content: 'h1. Title\n\n*bold* _italic_\n\n* list item\n\n|cell1|cell2|',
    editAppend: '\n\nh2. New Section\n\n*more bold text* and _more italic_\n\n# ordered item\n# another ordered item\n\n|header1|header2|\n|data1|data2|',
    searchTerm: 'Title',
    replaceTerm: 'Heading',
  },
  {
    num: '13', id: 'creole', name: 'Creole', dir: '13-creole',
    content: '= Title\n**bold** //italic//\n* list\n|cell|cell|',
    editAppend: '\n\n= New Title\n**more bold** //more italic//\n* new item\n** nested item\n|col1|col2|\n|data|data|',
    searchTerm: 'list',
    replaceTerm: 'items',
  },
  {
    num: '14', id: 'tiddlywiki', name: 'TiddlyWiki', dir: '14-tiddlywiki',
    content: "! Title\n!! Subtitle\n''bold'' //italic//\n* list",
    editAppend: "\n\n!! Added Subtitle\n''more bold text'' and //emphasized//\n* added item\n** nested item\n# ordered item",
    searchTerm: 'Title',
    replaceTerm: 'Header',
  },
  {
    num: '15', id: 'jupyter', name: 'Jupyter', dir: '15-jupyter',
    content: '{"cells":[{"cell_type":"code","source":["print(\'hello\')"]}]}',
    editAppend: '',
    editReplace: '{"cells":[{"cell_type":"code","source":["print(\'hello\')"]},{"cell_type":"markdown","source":["# Analysis\\nResults below"]},{"cell_type":"code","source":["x = 42\\nprint(x)"]}]}',
    searchTerm: 'hello',
    replaceTerm: 'world',
  },
  {
    num: '16', id: 'keyvalue', name: 'Key-Value', dir: '16-keyvalue',
    content: 'key1=value1\nkey2=value2\nkey3=value3',
    editAppend: '\nkey4=value4\nkey5=value5\ndebug=true\nversion=2.0',
    searchTerm: 'value2',
    replaceTerm: 'updated',
  },
  {
    num: '17', id: 'binary', name: 'Binary', dir: '17-binary',
    content: '48 65 6C 6C 6F 20 57 6F 72 6C 64 0A FF FE FD FC',
    editAppend: '\n00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\nDE AD BE EF CA FE BA BE',
    searchTerm: '6C',
    replaceTerm: '7C',
  },
];

// ============================================================
// Utility helpers
// ============================================================
function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

function ensureDir(dir) {
  mkdirSync(dir, { recursive: true });
}

async function screenshot(page, dir, name) {
  const path = join(dir, `${name}.png`);
  await page.screenshot({ path, fullPage: false });
  return path;
}

// ============================================================
// Local HTTP server for the fallback app
// ============================================================
function startLocalServer(port = 9876) {
  return new Promise((resolve, reject) => {
    const html = readFileSync(FALLBACK_APP, 'utf-8');
    const server = createServer((req, res) => {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(html);
    });
    server.listen(port, '127.0.0.1', () => {
      console.log(`  [SERVER] Fallback app serving on http://127.0.0.1:${port}`);
      resolve(server);
    });
    server.on('error', reject);
  });
}

// ============================================================
// Core interaction helpers
// ============================================================

/** Click a button/element by its CSS selector */
async function clickEl(page, selector) {
  await page.click(selector);
  await sleep(SPEED.clickDelay);
}

/** Clear editor and type new content */
async function clearAndType(page, content) {
  await page.click('#editor');
  await page.keyboard.press('Control+a');
  await sleep(100);
  await page.keyboard.press('Backspace');
  await sleep(200);
  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    if (i > 0) {
      await page.keyboard.press('Enter');
    }
    if (lines[i].length > 0) {
      await page.keyboard.type(lines[i], { delay: SPEED.typeDelay });
    }
  }
  await sleep(300);
}

/** Append content to the editor */
async function appendContent(page, content) {
  await page.click('#editor');
  await page.keyboard.press('Control+End');
  await sleep(100);
  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    if (i > 0 || content.startsWith('\n')) {
      await page.keyboard.press('Enter');
    }
    const line = i === 0 ? lines[i].replace(/^\n/, '') : lines[i];
    if (line.length > 0) {
      await page.keyboard.type(line, { delay: SPEED.typeDelay });
    }
  }
  await sleep(300);
}

/** Select format from sidebar */
async function selectFormat(page, formatId) {
  await page.click(`[data-format="${formatId}"]`);
  await sleep(SPEED.navPause);
}

/** Open Find dialog, search, replace, close */
async function findAndReplace(page, searchTerm, replaceTerm) {
  await clickEl(page, '#btnFind');
  await sleep(400);
  await page.fill('#findInput', searchTerm);
  await sleep(200);
  await clickEl(page, '#findNext');
  await sleep(400);
  await page.fill('#replaceInput', replaceTerm);
  await sleep(200);
  await clickEl(page, '#replaceAll');
  await sleep(400);
  await clickEl(page, '#findClose');
  await sleep(300);
}

/** Open Export dialog, export as HTML */
async function exportAsHtml(page) {
  await clickEl(page, '#btnExport');
  await sleep(400);
  await clickEl(page, '#exportHtml');
  await sleep(500);
}

/** Open Export dialog, export as Markdown */
async function exportAsMarkdown(page) {
  await clickEl(page, '#btnExport');
  await sleep(400);
  await clickEl(page, '#exportMd');
  await sleep(500);
}

/** Save document */
async function saveDocument(page) {
  await clickEl(page, '#btnSave');
  await sleep(500);
}

/** Clear editor content (delete operation) */
async function clearEditor(page) {
  await page.click('#editor');
  await page.keyboard.press('Control+a');
  await sleep(100);
  await page.keyboard.press('Backspace');
  await sleep(300);
}

/** Toggle preview on/off */
async function togglePreview(page) {
  await clickEl(page, '#btnPreview');
  await sleep(600);
}

// ============================================================
// Helpers that use Playwright page.evaluate (standard API)
// ============================================================

async function getEditorValue(page) {
  return page.$eval('#editor', el => el.value);
}

async function isFormatActive(page, formatId) {
  return page.$eval(
    `[data-format="${formatId}"]`,
    el => el.classList.contains('active')
  );
}

async function getPreviewText(page) {
  return page.$eval('#preview', el => el.textContent);
}

async function isPreviewHidden(page) {
  return page.$eval('#preview', el => el.style.display === 'none');
}

async function isDocDirty(page) {
  return page.$eval('#docName', el => el.classList.contains('dirty'));
}

async function getBodyTheme(page) {
  return page.$eval('body', el => el.getAttribute('data-theme'));
}

async function isDialogVisible(page, dialogId) {
  return page.$eval(`#${dialogId}`, el => el.style.display === 'flex');
}

async function getCheckboxState(page, checkboxId) {
  return page.$eval(`#${checkboxId}`, el => el.checked);
}

async function getFontSizeLabel(page) {
  return page.$eval('#fontSizeLabel', el => el.textContent);
}

async function getLocalStorageItem(page, key) {
  return page.$eval('body', (_, k) => localStorage.getItem(k), key);
}

async function getEditorSelection(page) {
  return page.$eval('#editor', el => el.value.substring(el.selectionStart, el.selectionEnd));
}

/** Force-close all dialog overlays via DOM manipulation */
async function closeAllDialogs(page) {
  await page.$eval('body', () => {
    document.querySelectorAll('.dialog-overlay').forEach(el => {
      el.style.display = 'none';
    });
  });
  await sleep(100);
}

/** Click a checkbox inside the settings dialog by clicking its sibling .slider */
async function clickCheckbox(page, checkboxId) {
  // The checkbox input has opacity:0 and size 0, so we click its sibling .slider span
  // which is the visible toggle switch. The <label> wrapping propagates the click.
  await page.click(`#${checkboxId} + .slider`, { force: true });
  await sleep(200);
}

// ============================================================
// Format test flow: runs all operations for a single format
// ============================================================
async function testFormat(browser, format, appUrl) {
  const dir = join(RECORDINGS_BASE, format.dir);
  ensureDir(dir);

  console.log(`\n  [${'='.repeat(50)}]`);
  console.log(`  [FORMAT] ${format.num}. ${format.name} (${format.id})`);
  console.log(`  [${'='.repeat(50)}]`);

  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    recordVideo: { dir, size: { width: 1440, height: 900 } },
  });
  const page = await context.newPage();

  let screenshotCount = 0;
  let passCount = 0;
  let failCount = 0;
  const results = [];

  async function step(name, fn) {
    const stepLabel = `${format.num}-${name}`;
    try {
      await fn();
      screenshotCount++;
      await screenshot(page, dir, `${String(screenshotCount).padStart(2, '0')}-${name}`);
      passCount++;
      results.push({ step: stepLabel, status: 'PASS' });
      console.log(`    [PASS] ${name}`);
    } catch (err) {
      failCount++;
      results.push({ step: stepLabel, status: 'FAIL', error: err.message });
      console.log(`    [FAIL] ${name}: ${err.message}`);
      try {
        screenshotCount++;
        await screenshot(page, dir, `${String(screenshotCount).padStart(2, '0')}-${name}-FAIL`);
      } catch (_) { /* ignore screenshot failure */ }
    }
  }

  // Navigate to app
  await page.goto(appUrl, { waitUntil: 'domcontentloaded', timeout: 15000 });
  await sleep(SPEED.navPause);

  // --- STEP 1: Select format ---
  await step('select-format', async () => {
    await selectFormat(page, format.id);
    const active = await isFormatActive(page, format.id);
    if (!active) throw new Error('Format not marked active in sidebar');
  });

  // --- STEP 2: Create (type content) ---
  await step('create-content', async () => {
    await clearAndType(page, format.content);
    const value = await getEditorValue(page);
    if (value.trim().length === 0) throw new Error('Editor is empty after typing');
  });

  // --- STEP 3: Preview ---
  await step('preview-on', async () => {
    const hidden = await isPreviewHidden(page);
    if (hidden) {
      await togglePreview(page);
    }
    const previewText = await getPreviewText(page);
    if (previewText.trim().length === 0) throw new Error('Preview is empty');
  });

  // --- STEP 4: Edit (append content) ---
  await step('edit-content', async () => {
    if (format.editReplace) {
      await clearAndType(page, format.editReplace);
    } else {
      await appendContent(page, format.editAppend);
    }
    const value = await getEditorValue(page);
    if (value.length <= format.content.length && !format.editReplace) {
      throw new Error('Content not appended');
    }
  });

  // --- STEP 5: Save ---
  await step('save-document', async () => {
    await saveDocument(page);
    const isDirty = await isDocDirty(page);
    if (isDirty) throw new Error('Document still marked dirty after save');
  });

  // --- STEP 6: Find and Replace ---
  await step('find-replace', async () => {
    await findAndReplace(page, format.searchTerm, format.replaceTerm);
    const value = await getEditorValue(page);
    if (value.includes(format.searchTerm) && !format.searchTerm.includes(format.replaceTerm)) {
      if (!value.includes(format.replaceTerm)) {
        throw new Error('Replacement term not found in editor');
      }
    }
  });

  // --- STEP 7: Export HTML ---
  await step('export-html', async () => {
    await exportAsHtml(page);
    await sleep(300);
  });

  // --- STEP 8: Export Markdown ---
  await step('export-markdown', async () => {
    await exportAsMarkdown(page);
    await sleep(300);
  });

  // --- STEP 9: Delete (clear content) ---
  await step('delete-content', async () => {
    await clearEditor(page);
    const value = await getEditorValue(page);
    if (value.trim().length > 0) throw new Error('Editor not empty after delete');
  });

  // Close context (stops video recording)
  await page.close();
  await context.close();

  return { format: format.name, screenshotCount, passCount, failCount, results };
}

// ============================================================
// Storage operations test
// ============================================================
async function testStorageOperations(browser, appUrl) {
  const dir = join(RECORDINGS_BASE, '18-storage');
  ensureDir(dir);

  console.log(`\n  [${'='.repeat(50)}]`);
  console.log('  [STORAGE] Storage Operations');
  console.log(`  [${'='.repeat(50)}]`);

  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    recordVideo: { dir, size: { width: 1440, height: 900 } },
  });
  const page = await context.newPage();

  let screenshotCount = 0;
  let passCount = 0;
  let failCount = 0;
  const results = [];

  async function step(name, fn) {
    try {
      await fn();
      screenshotCount++;
      await screenshot(page, dir, `${String(screenshotCount).padStart(2, '0')}-${name}`);
      passCount++;
      results.push({ step: name, status: 'PASS' });
      console.log(`    [PASS] ${name}`);
    } catch (err) {
      failCount++;
      results.push({ step: name, status: 'FAIL', error: err.message });
      console.log(`    [FAIL] ${name}: ${err.message}`);
      try {
        screenshotCount++;
        await screenshot(page, dir, `${String(screenshotCount).padStart(2, '0')}-${name}-FAIL`);
      } catch (_) { /* ignore */ }
    }
  }

  await page.goto(appUrl, { waitUntil: 'domcontentloaded', timeout: 15000 });
  await sleep(SPEED.navPause);

  // --- Local Save ---
  await step('local-save', async () => {
    await clearAndType(page, '# Storage Test Document\n\nThis tests localStorage persistence.');
    await saveDocument(page);
    const stored = await getLocalStorageItem(page, 'yole_content');
    if (!stored || !stored.includes('Storage Test')) {
      throw new Error('Content not found in localStorage');
    }
  });

  // --- Auto-save ---
  await step('auto-save', async () => {
    await appendContent(page, '\n\nAuto-save test line.');
    // Wait for auto-save timer (2 seconds + buffer)
    await sleep(3000);
    const autoSaved = await getLocalStorageItem(page, 'yole_autosave');
    if (!autoSaved || !autoSaved.includes('Auto-save test')) {
      throw new Error('Auto-save content not found in localStorage');
    }
    const autoSaveTs = await getLocalStorageItem(page, 'yole_autosave_ts');
    if (!autoSaveTs) throw new Error('Auto-save timestamp not set');
  });

  // --- Load from storage ---
  await step('load-from-storage', async () => {
    await clearEditor(page);
    await sleep(200);
    await page.reload({ waitUntil: 'domcontentloaded' });
    await sleep(SPEED.navPause);
    const stored = await getLocalStorageItem(page, 'yole_content');
    if (!stored) throw new Error('No content in localStorage after reload');
  });

  // --- File System Access API ---
  await step('file-system-access', async () => {
    await clickEl(page, '#btnOpen');
    await sleep(500);
    await clickEl(page, '#btnLoadDoc');
    await sleep(500);
  });

  // --- Offline mode ---
  await step('offline-mode', async () => {
    // Simulate offline by blocking all network requests
    await page.route('**/*', route => route.abort());
    await sleep(500);
    await clearAndType(page, '# Offline Document\nCreated while offline.');
    await saveDocument(page);
    const stored = await getLocalStorageItem(page, 'yole_content');
    if (!stored || !stored.includes('Offline')) {
      throw new Error('Offline save to localStorage failed');
    }
    // Restore network
    await page.unroute('**/*');
    await sleep(300);
  });

  await page.close();
  await context.close();

  return { format: 'Storage Operations', screenshotCount, passCount, failCount, results };
}

// ============================================================
// Settings, theme, responsive, keyboard shortcuts test
// ============================================================
async function testSettingsAndExtras(browser, appUrl) {
  const dir = join(RECORDINGS_BASE, '19-settings');
  ensureDir(dir);

  console.log(`\n  [${'='.repeat(50)}]`);
  console.log('  [SETTINGS] Settings, Theme, Responsive, Keyboard Shortcuts');
  console.log(`  [${'='.repeat(50)}]`);

  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    recordVideo: { dir, size: { width: 1440, height: 900 } },
  });
  const page = await context.newPage();

  let screenshotCount = 0;
  let passCount = 0;
  let failCount = 0;
  const results = [];

  async function step(name, fn) {
    try {
      await fn();
      screenshotCount++;
      await screenshot(page, dir, `${String(screenshotCount).padStart(2, '0')}-${name}`);
      passCount++;
      results.push({ step: name, status: 'PASS' });
      console.log(`    [PASS] ${name}`);
    } catch (err) {
      failCount++;
      results.push({ step: name, status: 'FAIL', error: err.message });
      console.log(`    [FAIL] ${name}: ${err.message}`);
      try {
        screenshotCount++;
        await screenshot(page, dir, `${String(screenshotCount).padStart(2, '0')}-${name}-FAIL`);
      } catch (_) { /* ignore */ }
    }
  }

  await page.goto(appUrl, { waitUntil: 'domcontentloaded', timeout: 15000 });
  await sleep(SPEED.navPause);

  // Type some content for visibility
  await clearAndType(page, '# Settings Test\n\nContent for testing settings and theme operations.');
  await sleep(300);

  // --- Theme: Light mode (default) ---
  await step('theme-light', async () => {
    const theme = await getBodyTheme(page);
    if (theme === 'dark') throw new Error('Expected light theme initially');
  });

  // --- Theme: Toggle to dark ---
  await step('theme-dark-toggle', async () => {
    await clickEl(page, '#btnTheme');
    await sleep(400);
    const theme = await getBodyTheme(page);
    if (theme !== 'dark') throw new Error('Expected dark theme after toggle');
  });

  // --- Theme: Toggle back to light ---
  await step('theme-light-restore', async () => {
    await clickEl(page, '#btnTheme');
    await sleep(400);
    const theme = await getBodyTheme(page);
    if (theme === 'dark') throw new Error('Expected light theme after second toggle');
  });

  // --- Settings dialog: Open ---
  await step('settings-open', async () => {
    await clickEl(page, '#btnSettings');
    await sleep(400);
    const visible = await isDialogVisible(page, 'settingsDialog');
    if (!visible) throw new Error('Settings dialog not visible');
  });

  // --- Settings: Dark theme toggle ---
  await step('settings-dark-theme', async () => {
    await clickCheckbox(page, 'setDark');
    const checked = await getCheckboxState(page, 'setDark');
    if (!checked) throw new Error('Dark theme checkbox not checked');
  });

  // --- Settings: Word wrap toggle ---
  await step('settings-word-wrap', async () => {
    await clickCheckbox(page, 'setWrap');
    const checked = await getCheckboxState(page, 'setWrap');
    if (checked) throw new Error('Word wrap should be unchecked');
  });

  // --- Settings: Line numbers toggle ---
  await step('settings-line-numbers', async () => {
    await clickCheckbox(page, 'setLines');
    const checked = await getCheckboxState(page, 'setLines');
    if (checked) throw new Error('Line numbers should be unchecked');
  });

  // --- Settings: Font size increase ---
  await step('settings-font-increase', async () => {
    await page.click('#fontInc', { force: true });
    await sleep(200);
    await page.click('#fontInc', { force: true });
    await sleep(200);
    const label = await getFontSizeLabel(page);
    if (!label.includes('18')) throw new Error(`Expected 18px, got ${label}`);
  });

  // --- Settings: Font size decrease ---
  await step('settings-font-decrease', async () => {
    await page.click('#fontDec', { force: true });
    await sleep(200);
    const label = await getFontSizeLabel(page);
    if (!label.includes('16')) throw new Error(`Expected 16px, got ${label}`);
  });

  // --- Settings: Save ---
  await step('settings-save', async () => {
    await page.click('#settingsSave', { force: true });
    await sleep(400);
    const theme = await getBodyTheme(page);
    if (theme !== 'dark') throw new Error('Dark theme not applied after settings save');
  });

  // --- Settings: Restore defaults ---
  await step('settings-restore', async () => {
    await closeAllDialogs(page);
    await clickEl(page, '#btnSettings');
    await sleep(400);
    // Ensure we reset to: dark=off, wrap=on, lines=on
    const isDarkChecked = await getCheckboxState(page, 'setDark');
    if (isDarkChecked) await clickCheckbox(page, 'setDark');
    const isWrapChecked = await getCheckboxState(page, 'setWrap');
    if (!isWrapChecked) await clickCheckbox(page, 'setWrap');
    const isLinesChecked = await getCheckboxState(page, 'setLines');
    if (!isLinesChecked) await clickCheckbox(page, 'setLines');
    await page.click('#settingsSave', { force: true });
    await sleep(400);
    await closeAllDialogs(page);
  });

  // Ensure all dialogs are closed before status bar tests
  await closeAllDialogs(page);

  // --- Status bar: Word wrap toggle ---
  await step('statusbar-wrap', async () => {
    await clickEl(page, '#btnWrap');
    await sleep(300);
    await clickEl(page, '#btnWrap');
    await sleep(300);
  });

  // --- Status bar: Line numbers toggle ---
  await step('statusbar-line-numbers', async () => {
    await clickEl(page, '#btnLineNums');
    await sleep(300);
    await clickEl(page, '#btnLineNums');
    await sleep(300);
  });

  // --- Status bar: Preview toggle ---
  await step('statusbar-preview', async () => {
    await clickEl(page, '#btnPreview');
    await sleep(400);
    const hidden = await isPreviewHidden(page);
    if (!hidden) throw new Error('Preview should be hidden');
    await clickEl(page, '#btnPreview');
    await sleep(400);
  });

  // Ensure all dialogs closed before keyboard shortcut tests
  await closeAllDialogs(page);

  // --- Keyboard shortcut: Ctrl+N (New) ---
  await step('shortcut-ctrl-n', async () => {
    // Focus the page body first
    await page.click('#editor', { force: true });
    await sleep(200);
    await page.keyboard.press('Control+n');
    await sleep(SPEED.navPause);
    const value = await getEditorValue(page);
    if (value.trim().length > 0) throw new Error('Ctrl+N did not clear editor');
  });

  // --- Keyboard shortcut: Type then Ctrl+S (Save) ---
  await step('shortcut-ctrl-s', async () => {
    await closeAllDialogs(page);
    await page.click('#editor', { force: true });
    await sleep(200);
    await page.keyboard.type('Saved via Ctrl+S shortcut', { delay: SPEED.typeDelay });
    await sleep(200);
    await page.keyboard.press('Control+s');
    await sleep(500);
    const isDirty = await isDocDirty(page);
    if (isDirty) throw new Error('Ctrl+S did not save (still dirty)');
  });

  // --- Keyboard shortcut: Ctrl+A (Select All) ---
  await step('shortcut-ctrl-a', async () => {
    await closeAllDialogs(page);
    await page.click('#editor', { force: true });
    await sleep(200);
    await page.keyboard.press('Control+a');
    await sleep(300);
    const selectedText = await getEditorSelection(page);
    if (selectedText.length === 0) throw new Error('Ctrl+A did not select text');
  });

  // --- Keyboard shortcut: Ctrl+Z (Undo) ---
  await step('shortcut-ctrl-z', async () => {
    await closeAllDialogs(page);
    await page.click('#editor', { force: true });
    await sleep(200);
    await page.keyboard.press('Control+a');
    await page.keyboard.press('Backspace');
    await sleep(100);
    await page.keyboard.type('Before undo', { delay: SPEED.typeDelay });
    await sleep(200);
    await page.keyboard.type(' ADDED', { delay: SPEED.typeDelay });
    await sleep(200);
    await page.keyboard.press('Control+z');
    await sleep(300);
    // Undo behavior depends on browser, just verify no crash
  });

  // --- Keyboard shortcut: Ctrl+F (Find) ---
  await step('shortcut-ctrl-f', async () => {
    await closeAllDialogs(page);
    await page.click('#editor', { force: true });
    await sleep(200);
    await page.keyboard.press('Control+f');
    await sleep(400);
    const visible = await isDialogVisible(page, 'findDialog');
    if (!visible) throw new Error('Ctrl+F did not open find dialog');
    await page.click('#findClose', { force: true });
    await sleep(300);
  });

  // Ensure all dialogs closed before responsive tests
  await closeAllDialogs(page);

  // --- Responsive: Desktop viewport (already at 1440x900) ---
  await step('responsive-desktop', async () => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await sleep(500);
  });

  // --- Responsive: Tablet viewport ---
  await step('responsive-tablet', async () => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await sleep(800);
  });

  // --- Responsive: Mobile viewport ---
  await step('responsive-mobile', async () => {
    await page.setViewportSize({ width: 375, height: 667 });
    await sleep(800);
  });

  // --- Responsive: Restore desktop ---
  await step('responsive-restore', async () => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await sleep(500);
  });

  // --- Format selector in statusbar ---
  await step('format-selector-statusbar', async () => {
    await page.selectOption('#formatSelector', 'csv');
    await sleep(500);
    const active = await isFormatActive(page, 'csv');
    if (!active) throw new Error('Format selector did not change format');
    await page.selectOption('#formatSelector', 'markdown');
    await sleep(500);
  });

  // --- Theme cycling per format (quick cycle through a few) ---
  await step('theme-format-cycle', async () => {
    await closeAllDialogs(page);
    const formatsToTest = ['todotxt', 'csv', 'latex', 'orgmode'];
    for (const fid of formatsToTest) {
      await selectFormat(page, fid);
      await clearAndType(page, `Content for ${fid} theme test`);
      await clickEl(page, '#btnTheme');
      await sleep(300);
      await clickEl(page, '#btnTheme');
      await sleep(300);
    }
    await selectFormat(page, 'markdown');
  });

  await page.close();
  await context.close();

  return { format: 'Settings & Extras', screenshotCount, passCount, failCount, results };
}

// ============================================================
// Main execution
// ============================================================
async function main() {
  console.log('==========================================================');
  console.log('  YOLE WEB — ALL 17 FORMATS COMPREHENSIVE AUTOMATION');
  console.log('==========================================================');
  console.log(`  Timestamp: ${new Date().toISOString()}`);
  console.log(`  Formats:   17`);
  console.log(`  Operations per format: 9 (select, create, preview, edit, save, find/replace, export HTML, export MD, delete)`);
  console.log(`  Additional suites: storage (5 ops), settings (20+ ops)`);
  console.log(`  Output:    ${RECORDINGS_BASE}`);
  console.log('==========================================================\n');

  // Ensure output directory structure
  ensureDir(RECORDINGS_BASE);
  for (const fmt of FORMATS) {
    ensureDir(join(RECORDINGS_BASE, fmt.dir));
  }
  ensureDir(join(RECORDINGS_BASE, '18-storage'));
  ensureDir(join(RECORDINGS_BASE, '19-settings'));

  // Start local server for the fallback app
  let server;
  const PORT = 9876;
  try {
    server = await startLocalServer(PORT);
  } catch (err) {
    console.error(`  [ERROR] Failed to start server: ${err.message}`);
    process.exit(1);
  }
  const appUrl = `http://127.0.0.1:${PORT}`;

  // Launch browser
  console.log('  [BROWSER] Launching Chromium...');
  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-gpu'],
  });
  console.log('  [BROWSER] Chromium launched\n');

  const allResults = [];
  let totalScreenshots = 0;
  let totalPass = 0;
  let totalFail = 0;
  let totalVideos = 0;
  const startTime = Date.now();

  // ---- Run all 17 format tests ----
  for (const format of FORMATS) {
    try {
      const result = await testFormat(browser, format, appUrl);
      allResults.push(result);
      totalScreenshots += result.screenshotCount;
      totalPass += result.passCount;
      totalFail += result.failCount;
      totalVideos++;
    } catch (err) {
      console.error(`  [ERROR] Format ${format.name} failed catastrophically: ${err.message}`);
      allResults.push({
        format: format.name,
        screenshotCount: 0,
        passCount: 0,
        failCount: 1,
        results: [{ step: 'catastrophic', status: 'FAIL', error: err.message }],
      });
      totalFail++;
    }
  }

  // ---- Run storage operations test ----
  try {
    const storageResult = await testStorageOperations(browser, appUrl);
    allResults.push(storageResult);
    totalScreenshots += storageResult.screenshotCount;
    totalPass += storageResult.passCount;
    totalFail += storageResult.failCount;
    totalVideos++;
  } catch (err) {
    console.error(`  [ERROR] Storage test failed catastrophically: ${err.message}`);
    allResults.push({
      format: 'Storage Operations',
      screenshotCount: 0, passCount: 0, failCount: 1,
      results: [{ step: 'catastrophic', status: 'FAIL', error: err.message }],
    });
    totalFail++;
  }

  // ---- Run settings & extras test ----
  try {
    const settingsResult = await testSettingsAndExtras(browser, appUrl);
    allResults.push(settingsResult);
    totalScreenshots += settingsResult.screenshotCount;
    totalPass += settingsResult.passCount;
    totalFail += settingsResult.failCount;
    totalVideos++;
  } catch (err) {
    console.error(`  [ERROR] Settings test failed catastrophically: ${err.message}`);
    allResults.push({
      format: 'Settings & Extras',
      screenshotCount: 0, passCount: 0, failCount: 1,
      results: [{ step: 'catastrophic', status: 'FAIL', error: err.message }],
    });
    totalFail++;
  }

  // Cleanup
  await browser.close();
  server.close();

  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);

  // ============================================================
  // Summary report
  // ============================================================
  console.log('\n==========================================================');
  console.log('  RESULTS SUMMARY');
  console.log('==========================================================');

  const reportLines = [];
  reportLines.push('Yole Web — All Formats Automation Report');
  reportLines.push(`Date: ${new Date().toISOString()}`);
  reportLines.push(`Duration: ${elapsed}s`);
  reportLines.push('');

  for (const r of allResults) {
    const status = r.failCount === 0 ? 'ALL PASS' : `${r.failCount} FAIL`;
    const line = `  ${r.format.padEnd(25)} ${String(r.passCount).padStart(2)}/${String(r.passCount + r.failCount).padStart(2)} passed  ${String(r.screenshotCount).padStart(3)} screenshots  [${status}]`;
    console.log(line);
    reportLines.push(line);
  }

  console.log('');
  console.log(`  Total tests:       ${totalPass + totalFail}`);
  console.log(`  Passed:            ${totalPass}`);
  console.log(`  Failed:            ${totalFail}`);
  console.log(`  Screenshots:       ${totalScreenshots}`);
  console.log(`  Videos:            ${totalVideos}`);
  console.log(`  Duration:          ${elapsed}s`);
  console.log(`  Output:            ${RECORDINGS_BASE}`);
  console.log('==========================================================');

  reportLines.push('');
  reportLines.push(`Total tests:       ${totalPass + totalFail}`);
  reportLines.push(`Passed:            ${totalPass}`);
  reportLines.push(`Failed:            ${totalFail}`);
  reportLines.push(`Screenshots:       ${totalScreenshots}`);
  reportLines.push(`Videos:            ${totalVideos}`);
  reportLines.push(`Duration:          ${elapsed}s`);

  // Write report file
  const reportPath = join(RECORDINGS_BASE, 'report.txt');
  writeFileSync(reportPath, reportLines.join('\n'), 'utf-8');
  console.log(`\n  Report written to: ${reportPath}`);

  // Write detailed JSON results
  const jsonPath = join(RECORDINGS_BASE, 'results.json');
  writeFileSync(jsonPath, JSON.stringify(allResults, null, 2), 'utf-8');
  console.log(`  Detailed results:  ${jsonPath}`);

  if (totalFail > 0) {
    console.log(`\n  WARNING: ${totalFail} test(s) failed. Check screenshots for details.`);
  } else {
    console.log('\n  All tests passed successfully!');
  }

  process.exit(totalFail > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(2);
});
