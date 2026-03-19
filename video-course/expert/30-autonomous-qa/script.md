<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 30: Autonomous QA (8 videos)

## Learning Objectives

- Understand the autonomous QA pipeline: DocProcessor, LLMOrchestrator, VisionEngine, and HelixQA
- Learn how SessionCoordinator orchestrates multi-stage quality analysis
- Master the issue detection and ticket generation workflow for AI-assisted fixes
- Understand PlatformWorker architecture for parallel platform-specific testing
- Apply evidence collection patterns for reproducible bug reports

---

## Video 30.1: The Autonomous QA Vision (16 min)

### Timestamps
- 0:00 Introduction: what "autonomous QA" means for a cross-platform text editor
- 2:00 The traditional approach: manual testing, human-written tests, reactive bug fixes
- 4:00 The autonomous approach: AI analyzes code, detects issues, generates evidence, files tickets
- 6:00 The pipeline overview: DocProcessor -> LLMOrchestrator -> VisionEngine -> HelixQA
- 8:00 Where each component lives: 3 sibling Go modules + 1 Git submodule
- 10:00 Integration with the 10,000+ test suite: autonomous QA complements, not replaces, manual tests
- 12:00 The feedback loop: detected issues feed back into test creation for permanent regression coverage
- 14:00 Current state: HelixQA 458+ tests, DocProcessor 219 tests, LLMOrchestrator 247 tests, VisionEngine 262 tests
- 15:30 Summary

### Code References
- `HelixQA/` -- Git submodule for autonomous QA orchestration
- `../DocProcessor/` -- Document processing and analysis module
- `../LLMOrchestrator/` -- LLM agent coordination module
- `../VisionEngine/` -- Visual analysis and screenshot comparison module

---

## Video 30.2: DocProcessor Deep Dive (18 min)

### Timestamps
- 0:00 DocProcessor purpose: loading, parsing, and analyzing source code and documentation
- 2:00 Package structure: loader, feature, coverage, docgraph, llm, config (6 packages, 219 tests)
- 4:00 Loader package: file system traversal, language detection, AST extraction
- 6:00 Feature package: extracting features from code -- functions, classes, imports, dependencies
- 8:00 Coverage package: mapping test files to source files, identifying untested code paths
- 10:00 Docgraph package: building a dependency graph of documentation cross-references
- 12:00 LLM package: preparing structured prompts for LLM analysis from extracted features
- 14:00 Config package: YAML-based configuration for analysis rules and thresholds
- 16:00 Testing patterns: table-driven tests, test fixtures, race-condition-safe test helpers
- 17:30 Summary

### Code References
- `../DocProcessor/pkg/loader/` -- Source file loading
- `../DocProcessor/pkg/feature/` -- Feature extraction
- `../DocProcessor/pkg/coverage/` -- Test coverage analysis
- `../DocProcessor/pkg/docgraph/` -- Documentation dependency graph

---

## Video 30.3: LLMOrchestrator Deep Dive (18 min)

### Timestamps
- 0:00 LLMOrchestrator purpose: coordinating LLM agents for multi-step analysis
- 2:00 Package structure: agent, adapter, protocol, parser, config (5 packages, 247 tests)
- 4:00 Agent package: defining analysis agents with specific roles (code reviewer, security auditor, performance analyst)
- 6:00 Adapter package: LLM provider adapters (OpenAI, Anthropic, local models)
- 8:00 Protocol package: structured communication protocol between agents
- 10:00 Parser package: parsing LLM responses into structured findings (issues, suggestions, ratings)
- 12:00 Config package: agent configuration, prompt templates, model selection
- 14:00 Multi-agent orchestration: how agents collaborate to analyze different aspects of a codebase
- 16:00 Rate limiting and cost control: managing API calls and token budgets
- 17:30 Summary

### Code References
- `../LLMOrchestrator/pkg/agent/` -- Agent definitions
- `../LLMOrchestrator/pkg/adapter/` -- LLM provider adapters
- `../LLMOrchestrator/pkg/protocol/` -- Inter-agent communication
- `../LLMOrchestrator/pkg/parser/` -- Response parsing

---

## Video 30.4: VisionEngine Deep Dive (18 min)

### Timestamps
- 0:00 VisionEngine purpose: visual analysis of rendered output and UI screenshots
- 2:00 Package structure: analyzer, graph, llmvision, opencv, config (5 packages, 262 tests)
- 4:00 Analyzer package: image comparison algorithms (pixel diff, structural similarity, perceptual hash)
- 6:00 Graph package: visual dependency graphs for UI component relationships
- 8:00 LLMVision package: using vision-capable LLMs to evaluate rendered document quality
- 10:00 OpenCV package: computer vision operations for layout analysis, text detection, alignment
- 12:00 Config package: threshold configuration for visual regression sensitivity
- 14:00 Screenshot comparison workflow: baseline capture, current capture, diff generation, report
- 16:00 Cross-platform visual testing: comparing renders across Android, Desktop, and Web
- 17:30 Summary

### Code References
- `../VisionEngine/pkg/analyzer/` -- Image comparison
- `../VisionEngine/pkg/graph/` -- Visual dependency graphs
- `../VisionEngine/pkg/llmvision/` -- LLM-based visual analysis
- `../VisionEngine/pkg/opencv/` -- Computer vision operations

---

## Video 30.5: HelixQA Pipeline (18 min)

### Timestamps
- 0:00 HelixQA as the orchestrator: coordinating DocProcessor, LLMOrchestrator, and VisionEngine
- 2:00 Package overview: 12 packages, 458+ tests, all race-safe
- 4:00 SessionCoordinator: managing a full analysis session from start to finish
- 6:00 Session lifecycle: init, configure, analyze, detect, report, cleanup
- 8:00 Navigator: traversing the codebase using DocProcessor-generated feature maps
- 10:00 IssueDetector: correlating findings from LLM analysis and visual analysis
- 12:00 Severity classification: critical, high, medium, low based on impact and confidence
- 14:00 Deduplication: merging duplicate findings from multiple analysis agents
- 16:00 The autonomous keyword: HelixQA runs without human intervention once configured
- 17:30 Summary

### Code References
- `HelixQA/pkg/session/` -- SessionCoordinator
- `HelixQA/pkg/navigator/` -- Codebase navigation
- `HelixQA/pkg/issuedetector/` -- Issue detection and classification

---

## Video 30.6: Issue Detection and Ticket Generation (16 min)

### Timestamps
- 0:00 Issue detection: from raw findings to classified issues
- 2:00 Finding types: code quality, security vulnerability, performance regression, visual defect
- 4:00 Confidence scoring: how the system rates certainty of each finding
- 6:00 Ticket generation: Markdown tickets for AI fix pipelines
- 8:00 Ticket structure: title, description, evidence, reproduction steps, suggested fix
- 10:00 Evidence attachment: screenshots, log excerpts, code snippets, diff views
- 12:00 Integration with project management: output compatible with GitHub Issues, JIRA, Linear
- 14:00 The fix pipeline: how generated tickets feed into LLM-assisted code fixes
- 15:30 Summary

### Code References
- `HelixQA/pkg/ticket/` -- Ticket generation
- `HelixQA/pkg/evidence/` -- Evidence collection
- `HelixQA/pkg/testbank/` -- Test bank management

---

## Video 30.7: PlatformWorker and Evidence Collection (16 min)

### Timestamps
- 0:00 PlatformWorker: running analysis tasks in parallel across platforms
- 2:00 Worker architecture: goroutine pool with platform-specific task queues
- 4:00 Android worker: ADB-based screenshot capture, logcat collection, APK analysis
- 6:00 Desktop worker: window capture, process monitoring, JVM metrics
- 8:00 Web worker: Playwright-based screenshot, console log capture, network monitoring
- 10:00 Evidence collection: centralized storage for all captured artifacts
- 12:00 Evidence types: screenshots, video recordings, log files, performance traces
- 14:00 Evidence lifecycle: capture, store, reference in tickets, archive after resolution
- 15:30 Summary

### Code References
- `HelixQA/pkg/evidence/` -- Evidence collection and storage
- `HelixQA/pkg/testbank/` -- Platform-targeted test banks

---

## Video 30.8: Running and Extending the Pipeline (14 min)

### Timestamps
- 0:00 Running HelixQA: CLI subcommands (run, list, report, version)
- 2:00 Configuration: YAML configuration files for analysis scope and thresholds
- 4:00 Running a full session: end-to-end demonstration on the Yole codebase
- 6:00 Interpreting results: reading the generated report and triaging findings
- 8:00 Adding a new analysis rule: step-by-step guide to extending IssueDetector
- 10:00 Adding a new platform: implementing PlatformWorker for a new target
- 12:00 Future directions: continuous autonomous QA, self-healing pipelines, learning from fixes
- 13:30 Summary

### Exercises
1. **Run HelixQA**: Execute a full HelixQA session on the Yole shared module and analyze the generated report. Identify the top 3 findings by severity.
2. **Custom analysis rule**: Write a new IssueDetector rule that flags any suspend function missing CancellationException rethrow in its catch blocks.
3. **Evidence collection**: Capture screenshots of Yole running on Desktop, then use VisionEngine to compare against a baseline. Document any visual differences found.
4. **Ticket generation**: Take a finding from the HelixQA report and manually create a Markdown ticket using the ticket package format. Verify it contains all required sections.
5. **Multi-platform analysis**: Run the same analysis on Android and Web targets in parallel using PlatformWorker. Compare the findings between platforms.
