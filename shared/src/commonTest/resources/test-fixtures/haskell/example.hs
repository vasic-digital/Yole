-- SPDX-FileCopyrightText: 2026 Milos Vasic
-- SPDX-License-Identifier: Apache-2.0
-- iter-58 F2 Phase 6 fixture: Haskell.

module Main where

data Greeter = Greeter { name :: String }

greet :: Greeter -> String
greet g = "Hello, " ++ name g ++ "!"

main :: IO ()
main = do
  let g = Greeter { name = "Yole" }
  putStrLn (greet g)
