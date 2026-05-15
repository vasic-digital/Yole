// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: Zig.

const std = @import("std");

const Greeter = struct {
    name: []const u8,

    pub fn greet(self: Greeter, writer: anytype) !void {
        try writer.print("Hello, {s}!\n", .{self.name});
    }
};

pub fn main() !void {
    const stdout = std.io.getStdOut().writer();
    const g = Greeter{ .name = "Yole" };
    try g.greet(stdout);
}
