/* SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 F2 Phase 6 fixture: C. */

#include <stdio.h>
#include <string.h>

typedef struct {
    char name[64];
} Greeter;

void greet(const Greeter *g) {
    printf("Hello, %s!\n", g->name);
}

int main(void) {
    Greeter g;
    strcpy(g.name, "Yole");
    greet(&g);
    return 0;
}
