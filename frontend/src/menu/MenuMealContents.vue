<script setup>
defineProps({
  contents: { type: Array, default: () => [] },
  copies: { type: Number, default: 1 },
})

function describe(contents, multiplier = 1) {
  return contents.map(item => item.name + ' × ' + (item.quantity * multiplier)).join('、')
}
</script>

<template>
  <small v-if="contents.length" class="menu-meal-contents">每份含 {{ describe(contents) }}</small>
  <small v-if="contents.length && copies > 1" class="menu-meal-contents">本项共 {{ describe(contents, copies) }}</small>
</template>

<style scoped>
.menu-meal-contents { display: block; color: var(--shop-muted, var(--muted)); font-size: .84rem; }
</style>
