# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Nix.

{ pkgs ? import <nixpkgs> {} }:

let
  name = "Yole";
  greet = who: "Hello, ${who}!";
in
pkgs.stdenv.mkDerivation {
  pname = "yole-greeter";
  version = "1.0.1";
  src = ./.;
  meta = {
    description = greet name;
    license = pkgs.lib.licenses.asl20;
  };
}
