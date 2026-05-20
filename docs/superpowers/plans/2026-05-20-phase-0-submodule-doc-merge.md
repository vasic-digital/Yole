# Phase 0 — Submodule Doc Merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve the 4 dirty-submodule rewrites while making the 3 inaccurate `ARCHITECTURE.md` docs factually correct, gated by a new executable doc-accuracy test, then commit + push all 4 submodules to their upstreams and bump pointers in the Yole repo.

**Architecture:** TDD per submodule — first add a generic Go doc-accuracy verifier (`internal/archdoc`) and a test that FAILS on the current (wrong) doc, then rewrite the doc until the test PASSES, then confirm the whole submodule suite is green. The verifier is consumer-agnostic infrastructure (CONST-038-safe).

**Tech Stack:** Go 1.24+ (`go test`), Node 25 (`node --check`, jsdom), git submodules.

---

## Shared Component — `internal/archdoc/archdoc.go`

This **identical** file is created in all three Go submodules (DocProcessor,
LLMOrchestrator, VisionEngine). It contains zero consumer-project-specific
content. Full source:

`````go
// SPDX-License-Identifier: Apache-2.0
// Package archdoc verifies that docs/ARCHITECTURE.md stays factually
// consistent with the module's actual source tree. It is generic
// infrastructure and contains no consumer-project-specific knowledge.
package archdoc

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
)

// ModuleRoot walks upward from start until it finds a directory with go.mod.
func ModuleRoot(start string) (string, error) {
	dir, err := filepath.Abs(start)
	if err != nil {
		return "", err
	}
	for {
		if _, statErr := os.Stat(filepath.Join(dir, "go.mod")); statErr == nil {
			return dir, nil
		}
		parent := filepath.Dir(dir)
		if parent == dir {
			return "", fmt.Errorf("archdoc: go.mod not found at or above %s", start)
		}
		dir = parent
	}
}

var (
	pkgPathRe  = regexp.MustCompile(`pkg/[a-zA-Z0-9_]+`)
	goFileRe   = regexp.MustCompile(`[a-zA-Z0-9_]+\.go`)
	typeDeclRe = regexp.MustCompile(`type ([A-Z][A-Za-z0-9]*) (?:struct|interface)`)
	goBlockRe  = regexp.MustCompile("(?s)```go\n(.*?)```")
	methodRe   = regexp.MustCompile(`(?m)^\s+([A-Z][A-Za-z0-9]*)\(`)
	codeTypeRe = regexp.MustCompile(`type ([A-Z][A-Za-z0-9]*) `)
	identRe    = regexp.MustCompile(`[A-Za-z_][A-Za-z0-9_]*`)
)

// Verify checks docs/ARCHITECTURE.md under root against the source tree.
// requiredMentions are substrings that MUST appear in the doc (a curated
// guard for significant exported symbols package-completeness cannot catch).
// Returns a sorted list of human-readable problems; empty means accurate.
func Verify(root string, requiredMentions []string) ([]string, error) {
	docBytes, err := os.ReadFile(filepath.Join(root, "docs", "ARCHITECTURE.md"))
	if err != nil {
		return nil, err
	}
	doc := string(docBytes)
	var problems []string

	// (1) Soundness — every pkg/<name> path named in the doc must exist.
	for _, p := range uniqueStrings(pkgPathRe.FindAllString(doc, -1)) {
		if !isDir(filepath.Join(root, p)) {
			problems = append(problems, "references missing package directory: "+p)
		}
	}

	// (2) Soundness — every *.go filename named in the doc must exist.
	goFiles := goFileBasenames(root)
	for _, f := range uniqueStrings(goFileRe.FindAllString(doc, -1)) {
		if !goFiles[f] {
			problems = append(problems, "references non-existent Go file: "+f)
		}
	}

	// (3+4) Soundness — types & interface methods declared in ```go blocks.
	codeTypes, codeIdents := codeSymbols(root)
	for _, block := range goBlocks(doc) {
		for _, m := range typeDeclRe.FindAllStringSubmatch(block, -1) {
			if !codeTypes[m[1]] {
				problems = append(problems, "doc declares type absent from code: "+m[1])
			}
		}
		for _, m := range methodRe.FindAllStringSubmatch(block, -1) {
			if !codeIdents[m[1]] {
				problems = append(problems, "doc declares method absent from code: "+m[1])
			}
		}
	}

	// (5) Completeness — every pkg/<name> directory must be mentioned.
	for _, d := range pkgDirs(root) {
		if !strings.Contains(doc, "pkg/"+d) {
			problems = append(problems, "code package undocumented: pkg/"+d)
		}
	}

	// (6) Curated required mentions.
	for _, want := range requiredMentions {
		if !strings.Contains(doc, want) {
			problems = append(problems, "required mention missing: "+want)
		}
	}

	sort.Strings(problems)
	return problems, nil
}

func goBlocks(doc string) []string {
	var out []string
	for _, m := range goBlockRe.FindAllStringSubmatch(doc, -1) {
		out = append(out, m[1])
	}
	return out
}

func isDir(p string) bool {
	info, err := os.Stat(p)
	return err == nil && info.IsDir()
}

func uniqueStrings(in []string) []string {
	seen := map[string]bool{}
	var out []string
	for _, s := range in {
		if !seen[s] {
			seen[s] = true
			out = append(out, s)
		}
	}
	return out
}

func pkgDirs(root string) []string {
	entries, err := os.ReadDir(filepath.Join(root, "pkg"))
	if err != nil {
		return nil
	}
	var out []string
	for _, e := range entries {
		if e.IsDir() {
			out = append(out, e.Name())
		}
	}
	return out
}

func goFileBasenames(root string) map[string]bool {
	out := map[string]bool{}
	_ = filepath.WalkDir(root, func(path string, d os.DirEntry, err error) error {
		if err != nil {
			return nil
		}
		if d.IsDir() && (d.Name() == ".git" || d.Name() == "vendor") {
			return filepath.SkipDir
		}
		if !d.IsDir() && strings.HasSuffix(d.Name(), ".go") {
			out[d.Name()] = true
		}
		return nil
	})
	return out
}

func codeSymbols(root string) (types, idents map[string]bool) {
	types = map[string]bool{}
	idents = map[string]bool{}
	_ = filepath.WalkDir(filepath.Join(root, "pkg"), func(path string, d os.DirEntry, err error) error {
		if err != nil {
			return nil
		}
		if !d.IsDir() && strings.HasSuffix(path, ".go") {
			b, readErr := os.ReadFile(path)
			if readErr != nil {
				return nil
			}
			src := string(b)
			for _, m := range codeTypeRe.FindAllStringSubmatch(src, -1) {
				types[m[1]] = true
			}
			for _, id := range identRe.FindAllString(src, -1) {
				idents[id] = true
			}
		}
		return nil
	})
	return types, idents
}
`````

---

## Task 1: DocProcessor — doc-accuracy test + corrected ARCHITECTURE.md

**Submodule path:** `Dependencies/HelixDevelopment/DocProcessor` (module `digital.vasic.docprocessor`, branch `master`)

**Files:**
- Create: `internal/archdoc/archdoc.go` (the Shared Component above, verbatim)
- Create: `internal/archdoc/archdoc_test.go`
- Modify: `docs/ARCHITECTURE.md`

**Confirmed defects in the current working-tree `docs/ARCHITECTURE.md`:**
1. Interface shown as `LLMAgent.Extract(ctx, prompt string)` — real code (`pkg/llm/agent.go:63`) is `ExtractFeatures(ctx context.Context, text string) ([]RawFeature, error)`.
2. Prose invents parsers `HTMLParser`, `AsciiDocParser`, `RSTParser` — `pkg/loader/` contains only `loader.go`, `markdown.go`, `scanner.go`, `yaml_parser.go`. Only Markdown + YAML are parsed.
3. Claims 5 doc formats (`md, yaml, html, adoc, rst`) — only md + yaml supported.
4. Category list given as "functional, UI, API, configuration, etc." — real `FeatureCategory` constants (`pkg/feature/feature.go:21-28`) are exactly: `format, ui, network, settings, storage, auth, editor, other` (8).

**Real package inventory (the doc MUST match this):**
`pkg/config`, `pkg/coverage`, `pkg/docgraph`, `pkg/feature`, `pkg/llm`, `pkg/loader`; `cmd/docprocessor`. Key exported types: `Loader` interface + `DefaultLoader`; `FeatureMapBuilder` interface + `DefaultBuilder`; `CoverageTracker` interface; `DocGraph` struct; `LLMAgent` interface (`ExtractFeatures`).

- [ ] **Step 1: Create the verifier** — write `internal/archdoc/archdoc.go` exactly as the Shared Component section above.

- [ ] **Step 2: Create the test** — write `internal/archdoc/archdoc_test.go`:

```go
// SPDX-License-Identifier: Apache-2.0
package archdoc

import (
	"strings"
	"testing"
)

// requiredMentions guards significant exported symbols that the
// package-completeness check alone cannot catch.
var requiredMentions = []string{
	"FeatureMapBuilder", "CoverageTracker", "DocGraph",
	"LLMAgent", "ExtractFeatures",
}

func TestArchitectureDocAccuracy(t *testing.T) {
	root, err := ModuleRoot(".")
	if err != nil {
		t.Fatal(err)
	}
	problems, err := Verify(root, requiredMentions)
	if err != nil {
		t.Fatal(err)
	}
	if len(problems) > 0 {
		t.Fatalf("docs/ARCHITECTURE.md is inaccurate (%d problems):\n  - %s",
			len(problems), strings.Join(problems, "\n  - "))
	}
}
```

- [ ] **Step 3: Run the test — confirm it FAILS (red)** — proves the test genuinely exercises the doc, anti-bluff.

Run: `cd Dependencies/HelixDevelopment/DocProcessor && go test ./internal/archdoc/ -run TestArchitectureDocAccuracy -v`
Expected: FAIL, listing problems such as the missing `ExtractFeatures` mention.

- [ ] **Step 4: Rewrite `docs/ARCHITECTURE.md` accurately** — Read the actual `pkg/` tree first. Keep every stylistic improvement of the current rewrite (the `**Module:**` header, the package-overview table, mermaid pipeline diagram, section structure). Correct all 4 defects above: fix the `LLMAgent` interface block to `ExtractFeatures(ctx context.Context, text string) ([]RawFeature, error)`; remove the invented HTML/AsciiDoc/RST parsers; state only Markdown + YAML are supported; list the real 8 categories. Do not mention any package, type, or `.go` file that does not exist.

- [ ] **Step 5: Run the test — confirm it PASSES (green)**

Run: `cd Dependencies/HelixDevelopment/DocProcessor && go test ./internal/archdoc/ -v`
Expected: PASS.

- [ ] **Step 6: Confirm the whole submodule is green**

Run: `cd Dependencies/HelixDevelopment/DocProcessor && go build ./... && go vet ./... && go test ./... -race -count=1`
Expected: build + vet clean, all tests PASS. If any pre-existing test fails, STOP and report (do not disable it).

- [ ] **Step 7: Commit (inside the submodule)**

```bash
cd Dependencies/HelixDevelopment/DocProcessor
git add internal/archdoc/ docs/ARCHITECTURE.md
git commit -m "$(cat <<'EOF'
docs(architecture): correct ARCHITECTURE.md to match code + add accuracy test

The prior working-tree rewrite improved structure but introduced factual
errors: wrong LLMAgent signature, invented HTML/AsciiDoc/RST parsers, wrong
doc-format list, wrong category list. Corrected against the real pkg/ tree.

internal/archdoc is a generic, consumer-agnostic verifier that parses
docs/ARCHITECTURE.md and asserts every referenced package/type/file/method
exists and every pkg/ package is documented. TestArchitectureDocAccuracy
fails the build on any future doc drift.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: LLMOrchestrator — doc-accuracy test + corrected ARCHITECTURE.md

**Submodule path:** `Dependencies/HelixDevelopment/LLMOrchestrator` (module `digital.vasic.llmorchestrator`, branch `master`)

**Files:**
- Create: `internal/archdoc/archdoc.go` (the Shared Component above, verbatim — byte-identical to Task 1)
- Create: `internal/archdoc/archdoc_test.go`
- Modify: `docs/ARCHITECTURE.md`

**Confirmed defects in the current working-tree `docs/ARCHITECTURE.md`:**
1. Drops `MultiProviderPool` entirely — real code: `pkg/agent/multi_pool.go` (`type MultiProviderPool struct`, plus `AgentSelector`, `RoundRobinSelector`, `PreferenceSelector`).
2. Drops `OpenCodeHeadlessAdapter` — real code: `pkg/adapter/opencode_headless.go`.
3. Says "5 CLI-specific adapters" — real `pkg/adapter/` has 6 concrete adapters: `claudecode.go`, `gemini.go`, `junie.go`, `opencode.go`, `opencode_headless.go`, `qwencode.go` (+ `base.go`).

**Real package inventory:** `pkg/adapter`, `pkg/agent`, `pkg/config`, `pkg/parser`, `pkg/protocol`; `cmd/orchestrator`. Key exported types: `Agent`/`AgentPool`/`AgentSelector`/`ResponseParser` interfaces; `MultiProviderPool`, `RoundRobinSelector`, `PreferenceSelector`, `HealthMonitor`, `CircuitBreaker`, `BaseAdapter`, `PipeTransport`, `FileTransport`.

- [ ] **Step 1: Create the verifier** — write `internal/archdoc/archdoc.go` byte-identical to the Shared Component section. (`cp` from the DocProcessor submodule created in Task 1 is acceptable — the file is identical.)

- [ ] **Step 2: Create the test** — write `internal/archdoc/archdoc_test.go` identical to Task 1's test EXCEPT `requiredMentions`:

```go
var requiredMentions = []string{
	"MultiProviderPool", "OpenCodeHeadlessAdapter", "AgentPool",
	"CircuitBreaker", "ResponseParser", "AgentSelector",
}
```

(Use the same package, imports, and `TestArchitectureDocAccuracy` body shown in Task 1 Step 2.)

- [ ] **Step 3: Run the test — confirm it FAILS (red)**

Run: `cd Dependencies/HelixDevelopment/LLMOrchestrator && go test ./internal/archdoc/ -run TestArchitectureDocAccuracy -v`
Expected: FAIL, listing the missing `MultiProviderPool` / `OpenCodeHeadlessAdapter` mentions.

- [ ] **Step 4: Rewrite `docs/ARCHITECTURE.md` accurately** — Read the actual `pkg/` tree. Keep all stylistic improvements (module header, package table, mermaid pool diagram, transport tables). Restore documentation of `MultiProviderPool` (and `AgentSelector`/`RoundRobinSelector`/`PreferenceSelector`) and `OpenCodeHeadlessAdapter`; correct the adapter count to 6.

- [ ] **Step 5: Run the test — confirm it PASSES (green)**

Run: `cd Dependencies/HelixDevelopment/LLMOrchestrator && go test ./internal/archdoc/ -v`
Expected: PASS.

- [ ] **Step 6: Confirm the whole submodule is green**

Run: `cd Dependencies/HelixDevelopment/LLMOrchestrator && go build ./... && go vet ./... && go test ./... -race -count=1`
Expected: build + vet clean, all tests PASS. If a pre-existing test fails, STOP and report.

- [ ] **Step 7: Commit (inside the submodule)**

```bash
cd Dependencies/HelixDevelopment/LLMOrchestrator
git add internal/archdoc/ docs/ARCHITECTURE.md
git commit -m "$(cat <<'EOF'
docs(architecture): correct ARCHITECTURE.md to match code + add accuracy test

The prior working-tree rewrite dropped MultiProviderPool and
OpenCodeHeadlessAdapter (both present in code) and undercounted adapters.
Restored and corrected against the real pkg/ tree.

internal/archdoc is a generic, consumer-agnostic verifier that fails the
build on any future ARCHITECTURE.md drift.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: VisionEngine — doc-accuracy test + corrected ARCHITECTURE.md

**Submodule path:** `Dependencies/HelixDevelopment/VisionEngine` (module `digital.vasic.visionengine`, branch `master`)

**Files:**
- Create: `internal/archdoc/archdoc.go` (the Shared Component above, verbatim — byte-identical to Task 1)
- Create: `internal/archdoc/archdoc_test.go`
- Modify: `docs/ARCHITECTURE.md`

**Confirmed defects in the current working-tree `docs/ARCHITECTURE.md`:**
1. Deletes the entire "Remote Deployment" section — real code: `pkg/remote/{deployer,distributed,remote,ssh}.go` with `LlamaCppDeployer`, `VisionSlot`, `VisionPool`, `SlotTarget`, `SSHConfig`, `DistributionConfig`. This package is actively developed (most recent submodule commit wired SSH into it).
2. Lists only 4 vision providers — real `pkg/llmvision/` has 8: `openai.go`, `anthropic.go`, `gemini.go`, `qwen.go`, `ollama.go`, `kimi.go`, `astica.go`, `stepgui.go` (+ `fallback.go`, `provider.go`).
3. OpenCV table cites filenames `opencv_stub.go` / `opencv_real.go` — real `pkg/opencv/` files are `stub.go`, `factory.go`, `interfaces.go`, and `*_vision.go` (`color_vision.go`, `detector_vision.go`, `differ_vision.go`, `factory_vision.go`, `video_vision.go`).

**Real package inventory:** `pkg/analyzer`, `pkg/config`, `pkg/graph`, `pkg/llmvision`, `pkg/opencv`, `pkg/remote`. Key exported types: `Analyzer`/`VisionProvider`/`LLMVisionProvider`/`NavigationGraph`/`Differ`/`ElementDetector`/`ColorAnalyzer`/`VideoProcessor` interfaces; `FallbackProvider`, `OpenAIProvider`, `AnthropicProvider`, `GeminiProvider`, `QwenProvider`, `OllamaProvider`, `KimiProvider`, `AsticaProvider`, `StepGUIProvider`, `LlamaCppDeployer`, `VisionSlot`, `VisionPool`.

- [ ] **Step 1: Create the verifier** — write `internal/archdoc/archdoc.go` byte-identical to the Shared Component section.

- [ ] **Step 2: Create the test** — `internal/archdoc/archdoc_test.go` identical to Task 1's test EXCEPT `requiredMentions`:

```go
var requiredMentions = []string{
	"VisionPool", "LlamaCppDeployer", "VisionSlot", "OllamaProvider",
	"KimiProvider", "AsticaProvider", "FallbackProvider",
	"NavigationGraph", "Analyzer",
}
```

- [ ] **Step 3: Run the test — confirm it FAILS (red)**

Run: `cd Dependencies/HelixDevelopment/VisionEngine && go test ./internal/archdoc/ -run TestArchitectureDocAccuracy -v`
Expected: FAIL — `pkg/remote` undocumented, `OllamaProvider`/`KimiProvider`/`AsticaProvider` missing, `opencv_stub.go`/`opencv_real.go` non-existent.

- [ ] **Step 4: Rewrite `docs/ARCHITECTURE.md` accurately** — Read the actual `pkg/` tree. Keep all stylistic improvements (module header, package table, mermaid diagrams, OpenCV table, sequence diagram). Restore the "Remote Deployment" section documenting `pkg/remote` (`LlamaCppDeployer`, `VisionSlot`, `VisionPool`, SSH-based deployment). List all 8 vision providers. Fix the OpenCV table to cite real filenames (`stub.go`, `*_vision.go`).

- [ ] **Step 5: Run the test — confirm it PASSES (green)**

Run: `cd Dependencies/HelixDevelopment/VisionEngine && go test ./internal/archdoc/ -v`
Expected: PASS.

- [ ] **Step 6: Confirm the whole submodule is green**

Run: `cd Dependencies/HelixDevelopment/VisionEngine && go build ./... && go vet ./... && go test ./... -race -count=1`
Expected: build + vet clean, all tests PASS. If a pre-existing test fails, STOP and report.

- [ ] **Step 7: Commit (inside the submodule)**

```bash
cd Dependencies/HelixDevelopment/VisionEngine
git add internal/archdoc/ docs/ARCHITECTURE.md
git commit -m "$(cat <<'EOF'
docs(architecture): correct ARCHITECTURE.md to match code + add accuracy test

The prior working-tree rewrite deleted the Remote Deployment section
(pkg/remote, present and actively developed), listed 4 of 8 vision
providers, and cited non-existent OpenCV filenames. Restored and corrected
against the real pkg/ tree.

internal/archdoc is a generic, consumer-agnostic verifier that fails the
build on any future ARCHITECTURE.md drift.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: LLMsVerifier — verify the website JS rewrite

**Submodule path:** `Dependencies/HelixDevelopment/LLMsVerifier` (branch `main`)

The `website/js/main.js` rewrite is a clean, self-consistent IIFE refactor (no
code-vs-doc drift). It still needs a real anti-bluff verification test.

**Files:**
- Modify: (none of `main.js` — it is already correct)
- Create: `website/js/main.test.js`
- Modify: `package.json` (add a test script + jsdom devDependency if absent)

- [ ] **Step 1: Syntax gate** — Run: `cd Dependencies/HelixDevelopment/LLMsVerifier && node --check website/js/main.js` — Expected: no output, exit 0.

- [ ] **Step 2: Inspect test infra** — Read `Dependencies/HelixDevelopment/LLMsVerifier/package.json`. Note the existing `test` script, `devDependencies`, and whether `jsdom` is present.

- [ ] **Step 3: Write the failing smoke test** — Create `website/js/main.test.js`. It MUST use jsdom to build a DOM containing a `.navbar` element and at least one `a[href^="#"]`, load `website/js/main.js` into that window by **injecting a `<script>` element** (no `eval`), dispatch `DOMContentLoaded`, and assert genuine behavior: (a) `window.trackEvent` is a function after load; (b) calling `window.trackEvent('cat','act','lbl')` does not throw; (c) scrolling past 50px adds the `scrolled` class to `.navbar`. Use the project's existing test runner if `package.json` declares one; otherwise use Node's built-in `node:test`. Example skeleton (adapt imports to the runner actually present):

```js
// SPDX-License-Identifier: Apache-2.0
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const { JSDOM } = require('jsdom');

test('main.js wires navigation and analytics for end users', () => {
  const dom = new JSDOM(
    `<!DOCTYPE html><nav class="navbar"></nav><a href="#x"></a><div id="x"></div>`,
    { runScripts: 'dangerously', pretendToBeVisual: true });
  const code = fs.readFileSync(__dirname + '/main.js', 'utf8');
  // Load the real script by DOM injection — jsdom executes appended <script>.
  const scriptEl = dom.window.document.createElement('script');
  scriptEl.textContent = code;
  dom.window.document.body.appendChild(scriptEl);
  dom.window.document.dispatchEvent(new dom.window.Event('DOMContentLoaded'));
  assert.strictEqual(typeof dom.window.trackEvent, 'function');
  assert.doesNotThrow(() => dom.window.trackEvent('cat', 'act', 'lbl'));
  Object.defineProperty(dom.window, 'scrollY', { value: 100, writable: true });
  dom.window.dispatchEvent(new dom.window.Event('scroll'));
  assert.ok(dom.window.document.querySelector('.navbar').classList.contains('scrolled'));
});
```

- [ ] **Step 4: Ensure jsdom is available** — If `jsdom` is not in `devDependencies`, add it (`npm install --save-dev jsdom` inside the submodule) and add/extend the `test` script so `node --test website/js/` (or the existing runner) executes the new test.

- [ ] **Step 5: Run the test — confirm it PASSES**

Run: `cd Dependencies/HelixDevelopment/LLMsVerifier && npm test` (or `node --test website/js/main.test.js`)
Expected: PASS, exercising the real rewritten code.

- [ ] **Step 6: Commit (inside the submodule)**

```bash
cd Dependencies/HelixDevelopment/LLMsVerifier
git add website/js/main.js website/js/main.test.js package.json package-lock.json
git commit -m "$(cat <<'EOF'
feat(website): modular main.js refactor + jsdom anti-bluff smoke test

main.js is refactored into an IIFE with navigation, smooth-scroll, and
analytics modules. main.test.js loads the real file in jsdom, dispatches
real DOM events, and asserts user-visible behavior (trackEvent wired,
navbar scroll state) — it fails if the script is stubbed.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Push submodules to upstreams + bump pointers in Yole

**Files:**
- Modify (Yole repo): the 4 submodule gitlink entries + `docs/CONTINUATION.md`

- [ ] **Step 1: Push each submodule to its upstream**

```bash
cd Dependencies/HelixDevelopment/DocProcessor   && git push origin master
cd ../LLMOrchestrator                           && git push origin master
cd ../VisionEngine                              && git push origin master
cd ../LLMsVerifier                              && git push origin main
```
Expected: each push succeeds (SSH remotes, fast-forward). If a push is rejected as non-fast-forward, STOP and report — do not force-push.

- [ ] **Step 2: Update `docs/CONTINUATION.md`** — Read the Yole `docs/CONTINUATION.md`, then update its current-state section to record Phase 0 complete: 3 ARCHITECTURE.md docs corrected, doc-accuracy tests added, 4 submodules pushed; next = Phase 1 (codegraph install).

- [ ] **Step 3: Stage the submodule pointer bumps + continuation in the Yole repo**

```bash
cd /Users/milosvasic/Projects/Yole
git add Dependencies/HelixDevelopment/DocProcessor \
        Dependencies/HelixDevelopment/LLMOrchestrator \
        Dependencies/HelixDevelopment/LLMsVerifier \
        Dependencies/HelixDevelopment/VisionEngine \
        docs/CONTINUATION.md
git status --short
```
Expected: the 4 submodule gitlinks show as modified (new commits) and `CONTINUATION.md` staged.

- [ ] **Step 4: Commit the pointer bump in Yole**

```bash
cd /Users/milosvasic/Projects/Yole
git commit -m "$(cat <<'EOF'
chore(submodules): bump HelixDevelopment deps — Phase 0 doc-accuracy merge

Picks up corrected ARCHITECTURE.md docs + new internal/archdoc accuracy
tests in DocProcessor, LLMOrchestrator, VisionEngine, and the jsdom smoke
test in LLMsVerifier. No Yole source affected.

Cross-platform impact:
- Android/Desktop/iOS/Web: N/A — submodule docs/tests only, no app code.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 5: Push the Yole repo**

Run: `cd /Users/milosvasic/Projects/Yole && git push origin master`
Expected: push succeeds. If rejected as non-fast-forward, STOP and report.

---

## Self-Review Notes

- **Spec coverage:** Covers Phase 0 §3 of the design spec (T0.1–T0.7) — doc fixes (Tasks 1-3), per-submodule doc-accuracy test (Tasks 1-3 archdoc), LLMsVerifier (Task 4), existing-suite verification (Step 6 of each Go task), commit+push+pointer-bump (Task 5).
- **Anti-bluff:** Every Go task runs the new test RED before the fix and GREEN after — proving the test exercises the doc. The jsdom test loads the real script and dispatches real events.
- **CONST-038:** `internal/archdoc` contains no consumer-project-specific content; it is generic infrastructure safe for any consumer.
- **No force-push:** Tasks explicitly STOP on non-fast-forward rather than overwrite upstream history.
