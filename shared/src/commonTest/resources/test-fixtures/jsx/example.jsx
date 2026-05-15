// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: JSX.

import React from "react";

export function Greeter({ name }) {
  return (
    <div className="greeter">
      <h1>Hello, {name}!</h1>
    </div>
  );
}
