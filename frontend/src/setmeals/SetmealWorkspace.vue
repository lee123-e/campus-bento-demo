<script setup>
import MenuWorkspace from '../menu/MenuWorkspace.vue'
import SetmealEditor from './SetmealEditor.vue'

defineEmits(['session-expired'])
</script>

<template>
  <MenuWorkspace
    entity-label="套餐"
    unit="个"
    summary="组合已有菜品，设置份数、套餐价和售卖状态。"
    resource="/setmeals"
    :category-type="2"
    :item-editor="SetmealEditor"
    @session-expired="$emit('session-expired')"
  />
</template>
