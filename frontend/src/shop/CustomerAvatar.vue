<script setup>
import { computed } from 'vue'

const props = defineProps({ customer: { type: Object, required: true } })
const initial = computed(() => [...(props.customer.nickname ?? '')][0] || props.customer.phone.slice(-2))
</script>

<template>
  <span class="shop-avatar" aria-hidden="true">{{ initial }}</span>
</template>
