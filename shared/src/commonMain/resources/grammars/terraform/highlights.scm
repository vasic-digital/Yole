; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/terraform/highlights.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; inherits: hcl

; Terraform specific references
;
;
; local/module/data/var/output
(expression
  (variable_expr
    (identifier) @variable.builtin
    (#any-of? @variable.builtin "data" "var" "local" "module" "output"))
  (get_attr
    (identifier) @variable.member))

; path.root/cwd/module
(expression
  (variable_expr
    (identifier) @type.builtin
    (#eq? @type.builtin "path"))
  (get_attr
    (identifier) @variable.builtin
    (#any-of? @variable.builtin "root" "cwd" "module")))

; terraform.workspace
(expression
  (variable_expr
    (identifier) @type.builtin
    (#eq? @type.builtin "terraform"))
  (get_attr
    (identifier) @variable.builtin
    (#any-of? @variable.builtin "workspace")))

; Terraform specific keywords
; FIXME: ideally only for identifiers under a `variable` block to minimize false positives
((identifier) @type.builtin
  (#any-of? @type.builtin "bool" "string" "number" "object" "tuple" "list" "map" "set" "any"))

(object_elem
  val: (expression
    (variable_expr
      (identifier) @type.builtin
      (#any-of? @type.builtin "bool" "string" "number" "object" "tuple" "list" "map" "set" "any"))))
