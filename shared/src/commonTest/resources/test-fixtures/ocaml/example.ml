(* SPDX-FileCopyrightText: 2026 Milos Vasic *)
(* SPDX-License-Identifier: Apache-2.0 *)
(* iter-58 F2 Phase 6 fixture: OCaml. *)

type greeter = { name : string }

let greet g = "Hello, " ^ g.name ^ "!"

let () =
  let g = { name = "Yole" } in
  print_endline (greet g)
