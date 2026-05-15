# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Julia.

struct Greeter
    name::String
end

function greet(g::Greeter)
    return "Hello, $(g.name)!"
end

function main()
    g = Greeter("Yole")
    println(greet(g))
end

main()
