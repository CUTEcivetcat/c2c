<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">📋 订单管理</h2>
    <div style="display:flex;gap:12px;margin-bottom:20px">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:130px" @change="load">
        <el-option :value="0" label="待支付" /><el-option :value="1" label="已支付" />
        <el-option :value="2" label="已发货" /><el-option :value="3" label="已收货" />
        <el-option :value="4" label="已完成" /><el-option :value="5" label="已取消" />
      </el-select>
      <el-button type="primary" @click="load" :loading="loading">筛选</el-button>
    </div>
    <el-card style="border-radius:14px">
      <el-table :data="orders" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="productTitle" label="商品" min-width="180"><template #default="{row}"><span style="font-weight:600">{{ row.productTitle }}</span></template></el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100"><template #default="{row}">¥{{ row.totalAmount }}</template></el-table-column>
        <el-table-column label="买家ID" width="80"><template #default="{row}">{{ row.buyerId }}</template></el-table-column>
        <el-table-column label="卖家ID" width="80"><template #default="{row}">{{ row.sellerId }}</template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="sType(row.status)" size="small">{{ sMap[row.status] }}</el-tag></template></el-table-column>
        <el-table-column label="创建时间" width="170"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next" :total="total" :page-size="20" v-model:current-page="page" @change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/request'

const orders = ref([]); const total = ref(0); const page = ref(1); const statusFilter = ref(null)
const loading = ref(false)
const sMap = {0:'待支付',1:'已支付',2:'已发货',3:'已收货',4:'已完成',5:'已取消'}
const sType = (s) => ({0:'warning',1:'',2:'',3:'success',4:'success',5:'danger'}[s]||'info')
const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 19) : ''

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.getOrders({ status: statusFilter.value, page: page.value, size: 20 })
    orders.value = res.records; total.value = res.total
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
