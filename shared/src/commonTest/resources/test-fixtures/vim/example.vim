" SPDX-FileCopyrightText: 2026 Milos Vasic
" SPDX-License-Identifier: Apache-2.0
" iter-58 F2 Phase 6 fixture: Vimscript.

function! Greet(name) abort
  echo "Hello, " . a:name . "!"
endfunction

function! Main() abort
  for target in ['android', 'desktop', 'ios', 'web']
    call Greet(target)
  endfor
endfunction

call Main()
