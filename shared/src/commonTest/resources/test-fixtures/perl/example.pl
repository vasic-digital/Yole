#!/usr/bin/env perl
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Perl.

use strict;
use warnings;

package Greeter;

sub new {
    my ($class, $name) = @_;
    my $self = { name => $name };
    bless $self, $class;
    return $self;
}

sub greet {
    my $self = shift;
    return "Hello, $self->{name}!";
}

package main;

my $g = Greeter->new("Yole");
print $g->greet(), "\n";
