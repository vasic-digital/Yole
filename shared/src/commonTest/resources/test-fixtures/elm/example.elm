-- SPDX-FileCopyrightText: 2026 Milos Vasic
-- SPDX-License-Identifier: Apache-2.0
-- iter-58 F2 Phase 6 fixture: Elm.

module Main exposing (main)

import Html exposing (Html, div, h1, text)


type alias Greeter =
    { name : String }


greet : Greeter -> String
greet g =
    "Hello, " ++ g.name ++ "!"


main : Html msg
main =
    div []
        [ h1 [] [ text (greet { name = "Yole" }) ] ]
