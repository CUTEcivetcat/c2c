<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">📦 商品管理</h2>
    <div style="display:flex;gap:12px;margin-bottom:20px;flex-wrap:wrap">
      <el-input v-model="keyword" placeholder="搜索商品标题" clearable style="width:240px" @change="load" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:130px" @change="load">
        <el-option :value="1" label="在售" /><el-option :value="2" label="已预定" />
        <el-option :value="3" label="已售" /><el-option :value="4" label="下架" /><el-option :value="5" label="违规下架" />
      </el-select>
      <el-button type="primary" @click="load" :loading="loading">搜索</el-button>
    </div>
    <el-card style="border-radius:14px">
      <el-table :data="products" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="封面" width="60"><template #default="{row}">
          <product-cover :src="row.images?.[0]?.url" style="width:44px;height:44px;border-radius:8px" />
        </template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="200"><template #default="{row}"><router-link :to="`/product/${row.id}`" target="_blank" style="color:#2d3436;font-weight:600">{{ row.title }}</router-link></template></el-table-column>
        <el-table-column prop="price" label="价格" width="100"><template #default="{row}">¥{{ row.price }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{row}">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template></el-table-column>
        <el-table-column label="违规原因" min-width="140"><template #default="{row}">
          <span v-if="row.status === 5 && row.reviewReason" :title="row.reviewReason" style="color:#e74c3c">{{ row.reviewReason }}</span>
          <span v-else>-</span>
        </template></el-table-column>
        <el-table-column prop="sellerId" label="卖家ID" width="80" />
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column label="发布时间" width="170"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="上架状态" width="130"><template #default="{row}">
          <el-switch :model-value="row.status === 1" active-text="在售" inactive-text="下架"
            :active-value="true" :inactive-value="false"
            :disabled="!canToggle(row.status)" :loading="toggling"
            @change="(val) => toggle(row, val)" />
        </template></el-table-column>
        <el-table-column label="审核操作" width="180"><template #default="{row}">
          <el-button v-if="row.status !== 5" type="danger" size="small" plain @click="ban(row)">违规下架</el-button>
          <el-button v-else type="success" size="small" plain @click="restore(row)">恢复上架</el-button>
        </template></el-table-column>
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
import { ElMessage, ElMessageBox } from 'element-plus'

const products = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const statusFilter = ref(null)
const loading = ref(false)
const toggling = ref(false)

const statusMap = {1:'在售',2:'已预定',3:'已售',4:'下架',5:'违规下架'}
const statusType = (s) => ({1:'success',2:'warning',3:'info',4:'danger',5:'danger'}[s]||'')
const statusText = (s) => statusMap[s]||'未知'
const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 19) : ''

// 只有「在售/下架」允许管理员切换；已预定、已售属于交易进行中，不允许改
const canToggle = (s) => s === 1 || s === 4

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.getProducts({ keyword: keyword.value, status: statusFilter.value, page: page.value, size: 20 })
    products.value = res.records; total.value = res.total
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

const toggle = async (row, val) => {
  const target = val ? 1 : 4
  if (!val) {
    // 下架是不可逆的对外操作，先确认
    try {
      await ElMessageBox.confirm(
        `确定将商品「${row.title}」下架吗？下架后买家将无法看到该商品。`,
        '下架确认',
        { type: 'warning', confirmButtonText: '确定下架', cancelButtonText: '取消' }
      )
    } catch (e) { return } // 用户取消
  }
  toggling.value = true
  try {
    await adminApi.toggleProductStatus(row.id, target)
    row.status = target
    ElMessage.success(val ? '已上架' : '已下架')
  } catch (e) {
    // 失败时不改 row.status，保持服务端真实状态；错误提示由拦截器统一处理
  } finally {
    toggling.value = false
  }
}
const ban = async (row) => {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写「${row.title}」的违规下架原因，卖家会看到该原因。`, '违规下架',
      { inputType: 'textarea', inputPlaceholder: '例如：涉嫌违禁品 / 假冒商品', confirmButtonText: '确认下架', cancelButtonText: '取消' }
    )
    reason = (value || '').trim()
  } catch (e) { return } // 用户取消
  if (!reason) { ElMessage.warning('请填写违规原因'); return }
  try {
    await adminApi.banProduct(row.id, reason)
    ElMessage.success('已违规下架')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

const restore = async (row) => {
  try {
    await ElMessageBox.confirm(`确定将「${row.title}」恢复上架吗？恢复后买家即可看到。`, '恢复上架', { type: 'warning', confirmButtonText: '恢复', cancelButtonText: '取消' })
  } catch (e) { return }
  try {
    await adminApi.restoreProduct(row.id)
    ElMessage.success('已恢复上架')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(load)
</script>
