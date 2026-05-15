<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Auto-Complete — Snippet Coverage Matrix

> **Audience:** contributors and users checking language support.
> Re-generate after adding new bundles (see `docs/features/auto-complete/architecture.md` §4).

All 55 bundles live under `shared/src/commonMain/resources/snippets/<langId>/snippets.json` and follow the VS Code snippet schema (with v1 limitations: no `$VARIABLE` substitution, no `${N|a,b,c|}` choice-list UI).

| Language ID | Bundled snippet count | User snippets directory wired | ScopeAwareRanker boost rules applied |
|---|---|---|---|
| bash | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| bibtex | 4 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| c | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| clojure | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| cpp | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| crystal | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| csharp | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| css | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| dart | 5 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| dockerfile | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| elixir | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| elm | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| erlang | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| fortran | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| go | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| graphql | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| groovy | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| haskell | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| html | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| java | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| javascript | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| json | 4 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| jsx | 4 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| julia | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| kotlin | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| latex | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| less | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| lua | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| makefile | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| markdown | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| nim | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| nix | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| objc | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| ocaml | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| perl | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| php | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| proto | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| python | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| r | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| regex | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| ruby | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| rust | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| scala | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| scss | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| sql | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| swift | 8 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| terraform | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| toml | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| tsx | 4 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| typescript | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| vim | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| vue | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| xml | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| yaml | 6 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |
| zig | 7 | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |

**Totals:** 55 language bundles, 361 snippets total.

**Languages with fewer than 6 snippets (below the Phase 7 norm of 6-8):**

| Language ID | Count | Note |
|---|---|---|
| bibtex | 4 | BibTeX has fewer common authoring patterns; 4 covers the main entry types |
| dart | 5 | 5 covers the main patterns; planned to reach 6 in a follow-up bundle pass |
| json | 4 | JSON has little snippet surface beyond structural templates |
| jsx | 4 | JSX-specific patterns are a subset of JS; 4 covers the unique JSX templates |
| tsx | 4 | Same reasoning as jsx — TSX-unique patterns beyond TS are limited |

---

*Generated by iter-60 Phase 10 from the source tree on 2026-05-15. Re-generate after adding new bundles by running:*

```bash
python3 -c "
import json, os
d = 'shared/src/commonMain/resources/snippets'
for lang in sorted(os.listdir(d)):
    p = os.path.join(d, lang, 'snippets.json')
    if os.path.isfile(p):
        count = len(json.load(open(p)))
        print(f'| {lang} | {count} | No (v2) | table-driven; member_access + type_annotation + string_literal scopes |')
"
```
