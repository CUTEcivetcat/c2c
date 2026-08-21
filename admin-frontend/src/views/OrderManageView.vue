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
        <el-table-column prop="productTitle" label="商品" min-width="160"><template #default="{row}"><span style="font-weight:600">{{ row.productTitle }}</span></template></el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="90"><template #default="{row}">¥{{ row.totalAmount }}</template></el-table-column>
        <el-table-column label="托管金" width="90"><template #default="{row}">
          <span v-if="row.escrow" style="color:#e67e22">¥{{ row.escrow }}</span><span v-else style="color:#b2bec3">-</span>
        </template></el-table-column>
        <el-table-column label="支付方式" width="90"><template #default="{row}">
          <el-tag v-if="row.paymentMethod" size="small">{{ payText(row.paymentMethod) }}</el-tag><span v-else style="color:#b2bec3">-</span>
        </template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="sType(row.status)" size="small">{{ sMap[row.status] }}</el-tag></template></el-table-column>
        <el-table-column label="创建时间" width="160"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="80" fixed="right"><template #default="{row}">
          <el-button type="primary" size="small" plain @click="openDetail(row)">详情</el-button>
        </template></el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next" :total="total" :page-size="20" v-model:current-page="page" @change="load" />
      </div>
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="订单详情" width="640px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ sMap[current.status] }}</el-descriptions-item>
          <el-descriptions-item label="商品">{{ current.productTitle }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ current.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="买家ID">{{ current.buyerId }}</el-descriptions-item>
          <el-descriptions-item label="卖家ID">{{ current.sellerId }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ current.paymentMethod ? payText(current.paymentMethod) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="托管金">{{ current.escrow ? '¥' + current.escrow : '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ fmtTime(current.paymentTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ fmtTime(current.completeTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" :span="2">{{ current.cancelReason || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:0 0 10px;font-size:14px">资金流水</h4>
        <el-table :data="walletLogs" size="small" stripe max-height="240">
          <el-table-column label="类型" width="80"><template #default="{row}">{{ walletType(row.type) }}</template></el-table-column>
          <el-table-column label="金额" width="90"><template #default="{row}">
            <span :style="{ color: row.amount > 0 ? '#2e7d32' : '#c62828', fontWeight: 600 }">{{ row.amount > 0 ? '+' : '' }}{{ row.amount }}</span>
          </template></el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          <el-table-column label="时间" width="150"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
        </el-table>
        <el-empty v-if="!walletLogs.length" description="该订单无资金流水（未通过余额支付）" :image-size="50" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/request'

const orders = ref([]); const total = ref(0); const page = ref(1); const statusFilter = ref(null)
const loading = ref(false)
const dialogVisible = ref(false)
const current = ref(null)
const walletLogs = ref([])
const sMap = {0:'待支付',1:'已支付',2:'已发货',3:'已收货',4:'已完成',5:'已取消'}
const sType = (s) => ({0:'warning',1:'',2:'',3:'success',4:'success',5:'danger'}[s]||'info')
const payText = (p) => ({ balance: '余额支付', mock: '模拟支付' }[p] || p)
const walletType = (t) => ({ recharge: '充值', pay: '支付', refund: '退款', receive: '收款' }[t] || t)
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

const openDetail = async (row) => {
  current.value = row
  walletLogs.value = []
  dialogVisible.value = true
  try { walletLogs.value = await adminApi.getOrderWallet(row.id) || [] } catch (e) { /* */ }
}

onMounted(load)
</script>