<template>
  <div class="page-container coupon-my fade-in-up" style="max-width:1000px">
    <page-back-bar title="我的优惠券" />

    <el-tabs v-model="tab" style="margin-top:8px" @tab-change="load">
      <el-tab-pane v-for="(label, val) in COUPON_STATUS" :key="val" :label="label" :name="String(val)" />
    </el-tabs>

    <div v-if="list.length" class="coupon-list">
      <div v-for="c in list" :key="c.userCouponId" class="coupon-card" :class="{ inactive: c.status !== 0 }">
        <div class="coupon-face">
          <div class="face-value">{{ couponFaceText(c) }}</div>
          <div class="face-sub">{{ couponTypeText(c) }}</div>
        </div>
        <div class="coupon-main">
          <div class="coupon-name">{{ c.name || ('优惠券 #' + c.couponId) }}</div>
          <div class="coupon-threshold">{{ couponThresholdText(c) }}</div>
          <div class="coupon-cat">{{ couponCategoryText(c) }}</div>
          <div class="coupon-valid">{{ couponValidText(c) }}</div>
          <div class="coupon-extra" v-if="c.status === 1 && c.orderId">已用于订单 #{{ c.orderId }}</div>
          <div class="coupon-extra" v-if="Number(c.type) === COUPON_TYPE_BALANCE">已到账余额，可在钱包查看流水</div>
        </div>
        <div class="coupon-action">
          <el-tag :type="statusType(c.status)">{{ COUPON_STATUS[c.status] }}</el-tag>
        </div>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="该状态下暂无优惠券" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyCoupons } from '@/api/coupon'
import { COUPON_TYPE_BALANCE, COUPON_STATUS, couponFaceText, couponTypeText, couponThresholdText, couponValidText, couponCategoryText } from '@/utils/coupon'

const tab = ref('0')
const list = ref([])
const loading = ref(false)

const statusType = (s) => ({ 0: 'success', 1: 'info', 2: 'warning', 3: 'danger' }[s] || 'info')

const load = async () => {
  loading.value = true
  try {
    const res = await getMyCoupons(Number(tab.value))
    list.value = res || []
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.coupon-list { display: flex; flex-direction: column; gap: 14px; }
.coupon-card {
  display: flex; align-items: stretch; background: #fff; border: 1px solid var(--c2c-line-soft); border-radius: 16px; overflow: hidden;
}
.coupon-card.inactive { opacity: 0.6; }
.coupon-face {
  width: 110px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--c2c-primary), var(--c2c-primary-light)); color: #fff; padding: 18px 10px;
}
.coupon-card.inactive .coupon-face { background: #c9d2db; }
.face-value { font-size: 24px; font-weight: 800; font-family: "SF Pro Display", sans-serif; }
.face-sub { font-size: 12px; opacity: 0.9; margin-top: 4px; }
.coupon-main { flex: 1; min-width: 0; padding: 16px; }
.coupon-name { font-size: 15px; font-weight: 600; color: var(--c2c-text); margin-bottom: 6px; }
.coupon-threshold { font-size: 13px; color: var(--c2c-primary-dark); font-weight: 600; }
.coupon-cat { font-size: 12px; color: var(--c2c-text-muted); margin-top: 2px; }
.coupon-valid { font-size: 12px; color: var(--c2c-text-muted); margin-top: 4px; }
.coupon-extra { font-size: 12px; color: var(--c2c-text-secondary); margin-top: 2px; }
.coupon-action { display: flex; align-items: center; padding: 0 16px; }
@media (max-width: 480px) {
  .coupon-face { width: 92px; }
  .face-value { font-size: 20px; }
  .coupon-main { padding: 12px; }
}
</style>
