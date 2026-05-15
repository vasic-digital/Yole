<!--
  SPDX-FileCopyrightText: 2026 Milos Vasic
  SPDX-License-Identifier: Apache-2.0
  iter-58 F2 Phase 6 fixture: Vue.
-->
<template>
  <div class="greeter">
    <h1>Hello, {{ name }}!</h1>
  </div>
</template>

<script setup lang="ts">
defineProps<{ name: string }>();
</script>

<style scoped>
.greeter {
  color: #336699;
  font-family: system-ui, sans-serif;
}
</style>
