<?php
// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: PHP.

namespace Yole\Fixtures;

class Greeter
{
    private string $name;

    public function __construct(string $name)
    {
        $this->name = $name;
    }

    public function greet(): string
    {
        return "Hello, {$this->name}!";
    }
}

echo (new Greeter("Yole"))->greet() . PHP_EOL;
