<script setup>
import MenuWorkspace from '../menu/MenuWorkspace.vue'
import DishEditor from './DishEditor.vue'

defineEmits(['session-expired'])
</script>

<template>
  <MenuWorkspace
    entity-label="菜品"
    unit="道"
    summary="维护菜单、售价和口味，按需要调整售卖状态。"
    resource="/dishes"
    :category-type="1"
    :item-editor="DishEditor"
    @session-expired="$emit('session-expired')"
  />
</template>
