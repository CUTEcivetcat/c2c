<template>
  <div class="page-container">
    <page-back-bar title="我的订单" />
    <el-tabs v-model="tab" @tab-change="load"><el-tab-pane label="买入" name="buy"/><el-tab-pane label="卖出" name="sell"/></el-tabs>
    <el-card v-for="o in orders" :key="o.id" style="margin-bottom:12px;cursor:pointer" @click="$router.push(`/order/${o.id}`)">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div><strong>{{ o.productTitle }}</strong><br/><span style="color:#909399;font-size:13px">订单号: {{ o.orderNo }}</span></div>
        <div style="text-align:right"><span style="color:#f56c6c;font-size:18px;font-weight:700">¥{{ o.totalAmount }}</span><br/><el-tag size="small">{{ statusMap[o.status] }}</el-tag></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBuyerOrders, getSellerOrders } from '@/api/order'

const tab = ref('buy')
const orders = ref([])
const statusMap = {0:'待支付',1:'已支付',2:'已发货',3:'已收货',4:'已完成',5:'已取消'}

const load = async () => {
  const api = tab.value === 'buy' ? getBuyerOrders : getSellerOrders
  const res = await api({ page:1, size:50 })
  orders.value = res.records
}
onMounted(load)
</script>
