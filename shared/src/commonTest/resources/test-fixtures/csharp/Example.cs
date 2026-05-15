// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: C#.

namespace Yole.Fixtures
{
    public class Greeter
    {
        public string Name { get; }

        public Greeter(string name)
        {
            Name = name;
        }

        public string Greet() => $"Hello, {Name}!";
    }

    public static class Program
    {
        public static void Main(string[] args)
        {
            System.Console.WriteLine(new Greeter("Yole").Greet());
        }
    }
}
