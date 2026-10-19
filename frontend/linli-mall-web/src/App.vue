<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const cart = useCartStore()
const user = useUserStore()
const showTabbar = computed(() => Boolean(route.meta.tab))

onMounted(() => {
  if (user.token) cart.fetch().catch(() => {})
})
</script>

<template>
  <div class="app-shell">
    <router-view v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <van-tabbar
      v-if="showTabbar"
      route
      fixed
      placeholder
      active-color="#1f8a4c"
      inactive-color="#8a928a"
    >
      <van-tabbar-item replace to="/" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/category" icon="apps-o">分类</van-tabbar-item>
      <van-tabbar-item replace to="/cart" icon="shopping-cart-o" :badge="cart.count || ''">
        购物车
      </van-tabbar-item>
      <van-tabbar-item replace to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item replace to="/me" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>
