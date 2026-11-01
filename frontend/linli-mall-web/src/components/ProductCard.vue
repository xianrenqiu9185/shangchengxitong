<script setup>
import { formatPrice } from '@/utils/format'

defineProps({
  product: { type: Object, required: true },
})

defineEmits(['add'])
</script>

<template>
  <article class="product-card" @click="$router.push(`/products/${product.id}`)">
    <div class="product-media">
      <img :src="product.image" :alt="product.name" loading="lazy" />
      <span v-if="product.tags?.length" class="product-tag">{{ product.tags[0] }}</span>
    </div>
    <div class="product-body">
      <h3>{{ product.name }}</h3>
      <p>{{ product.subtitle }}</p>
      <div class="product-meta">
        <span class="price"><small>¥</small>{{ formatPrice(product.price) }}</span>
        <button class="add-button" aria-label="加入购物车" @click.stop="$emit('add', product)">＋</button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  overflow: hidden;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 62, 44, 0.07);
  cursor: pointer;
}

.product-media {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #f3f1ea;
}

.product-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.25s ease;
}

.product-card:active .product-media img {
  transform: scale(1.03);
}

.product-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 3px 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #1f8a4c;
  font-size: 10px;
  font-weight: 700;
}

.product-body {
  padding: 10px 11px 11px;
}

.product-body h3 {
  margin: 0;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-body p {
  margin: 4px 0 9px;
  overflow: hidden;
  color: #7b857e;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.price {
  font-size: 17px;
}

.add-button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: #1f8a4c;
  color: #fff;
  font-size: 19px;
  line-height: 1;
  cursor: pointer;
}
</style>
