-- SPDX-FileCopyrightText: 2026 Milos Vasic
-- SPDX-License-Identifier: Apache-2.0
-- iter-58 F2 Phase 6 fixture: Lua.

local Greeter = {}
Greeter.__index = Greeter

function Greeter.new(name)
  local self = setmetatable({}, Greeter)
  self.name = name
  return self
end

function Greeter:greet()
  return "Hello, " .. self.name .. "!"
end

local g = Greeter.new("Yole")
print(g:greet())
