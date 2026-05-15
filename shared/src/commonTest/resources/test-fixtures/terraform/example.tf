# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Terraform (HCL).

variable "name" {
  type    = string
  default = "Yole"
}

resource "null_resource" "greeter" {
  triggers = {
    name = var.name
  }

  provisioner "local-exec" {
    command = "echo Hello, ${var.name}!"
  }
}

output "greeting" {
  value = "Hello, ${var.name}!"
}
