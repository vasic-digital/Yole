# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Crystal.

class Greeter
  getter name : String

  def initialize(@name : String)
  end

  def greet : String
    "Hello, #{@name}!"
  end
end

puts Greeter.new("Yole").greet
