<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <div class="section-head">
    <h2>{{ title }}</h2>
    <slot>
      <span v-if="subtitle">{{ subtitle }}</span>
    </slot>
  </div>
</template>
