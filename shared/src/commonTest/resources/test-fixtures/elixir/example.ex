# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Elixir.

defmodule Greeter do
  defstruct name: ""

  def greet(%Greeter{name: name}) do
    "Hello, #{name}!"
  end
end

g = %Greeter{name: "Yole"}
IO.puts(Greeter.greet(g))
