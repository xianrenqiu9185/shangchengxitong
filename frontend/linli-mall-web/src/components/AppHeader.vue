<script setup>
import { useRouter } from 'vue-router'

defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true },
})

const router = useRouter()
</script>

<template>
  <header class="page-header">
    <button v-if="back" class="icon-button" aria-label="返回" @click="router.back()">‹</button>
    <h1>{{ title }}</h1>
    <slot name="right" />
  </header>
</template>
