<script setup>
import { computed } from 'vue'
import { formatPrice } from '@/utils/format'
import { orderStatusMap } from '@/utils/dict'

const props = defineProps({
  order: { type: Object, required: true },
})

const status = computed(() => orderStatusMap[props.order.status] || { label: props.order.status, color: '#6b7280' })
const count = computed(() => props.order.items.reduce((sum, item) => sum + item.quantity, 0))
</script>

<template>
  <article class="order-card">
    <header>
      <span>{{ order.orderNo }}</span>
      <strong :style="{ color: status.color }">{{ status.label }}</strong>
    </header>
    <div class="order-items">
      <div v-for="item in order.items.slice(0, 3)" :key="item.productId" class="order-item">
        <img :src="item.image" :alt="item.name" />
        <div>
          <p>{{ item.name }}</p>
          <span>{{ item.unit }} × {{ item.quantity }}</span>
        </div>
        <b>¥{{ formatPrice(item.price) }}</b>
      </div>
    </div>
    <footer>
      <span>共 {{ count }} 件</span>
      <span>实付 <b>¥{{ formatPrice(order.payAmount) }}</b></span>
    </footer>
    <div v-if="$slots.default" class="order-actions">
      <slot />
    </div>
  </article>
</template>

<style scoped>
.order-card {
  margin-bottom: 12px;
  padding: 14px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.06);
}

.order-card header,
.order-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-card header {
  padding-bottom: 10px;
  border-bottom: 1px solid #efeee8;
  color: #7b857e;
  font-size: 11px;
}

.order-card header strong {
  font-size: 12px;
}

.order-items {
  display: grid;
  gap: 9px;
  padding: 11px 0;
}

.order-item {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  align-items: center;
  gap: 10px;
}

.order-item img {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  object-fit: cover;
  background: #f3f1ea;
}

.order-item p {
  margin: 0 0 3px;
  font-size: 13px;
}

.order-item span {
  color: #7b857e;
  font-size: 11px;
}

.order-item b {
  font-size: 12px;
}

.order-card footer {
  color: #7b857e;
  font-size: 11px;
}

.order-card footer b {
  color: #1c2420;
  font-size: 16px;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
