<script setup>
import { useId } from 'vue'

defineProps({
  groups: { type: Array, required: true },
  disabled: Boolean,
})
const choices = defineModel({ type: Array, required: true })
const inputGroup = useId()
</script>

<template>
  <section v-if="groups.length" class="shop-product-options" aria-label="选择口味">
    <h3>选择口味</h3>
    <fieldset v-for="(group,index) in groups" :key="group.name" class="shop-flavor-group">
      <legend>{{ group.name }}</legend>
      <div class="shop-flavor-choices">
        <label v-for="option in group.options" :key="option">
          <input v-model="choices[index]" type="radio" :name="inputGroup + '-' + index" :value="option" :disabled="disabled" />
          <span>{{ option }}</span>
        </label>
      </div>
    </fieldset>
  </section>
</template>
