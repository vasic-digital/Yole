%% SPDX-FileCopyrightText: 2026 Milos Vasic
%% SPDX-License-Identifier: Apache-2.0
%% iter-58 F2 Phase 6 fixture: Erlang.

-module(example).
-export([greet/1, main/0]).

greet(Name) ->
    "Hello, " ++ Name ++ "!".

main() ->
    io:format("~s~n", [greet("Yole")]).
