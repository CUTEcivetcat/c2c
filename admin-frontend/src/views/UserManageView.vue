<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">👥 用户管理</h2>
    <div style="display:flex;gap:12px;margin-bottom:20px">
      <el-input v-model="keyword" placeholder="搜索手机号/邮箱/昵称" clearable style="width:300px" @change="load" />
      <el-button type="primary" @click="load" :loading="loading">搜索</el-button>
    </div>
    <el-card style="border-radius:14px">
      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="phone" label="手机号" width="140"><template #default="{row}"><span v-if="row.phone">{{ row.phone.slice(0,3) }}****{{ row.phone.slice(7) }}</span></template></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="90"><template #default="{row}"><el-tag v-if="row.role===1" type="danger" size="small">管理员</el-tag><el-tag v-else type="info" size="small">普通用户</el-tag></template></el-table-column>
        <el-table-column prop="reputationScore" label="信誉" width="80" />
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'正常':'封禁' }}</el-tag></template></el-table-column>
        <el-table-column label="注册时间" width="170"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right"><template #default="{row}">
          <el-button v-if="row.status===1" type="danger" size="small" :disabled="row.role===1" @click="toggle(row)">封禁</el-button>
          <el-button v-else type="success" size="small" @click="toggle(row)">解封</el-button>
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

const users = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const loading = ref(false)
const toggling = ref(false)

const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 19) : ''

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.getUsers({ keyword: keyword.value, page: page.value, size: 20 })
    users.value = res.records; total.value = res.total
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

const toggle = async (row) => {
  const banning = row.status === 1
  try {
    await ElMessageBox.confirm(
      banning ? `确定封禁用户「${row.nickname || row.username}」吗？封禁后该用户将无法登录。` : `确定解封用户「${row.nickname || row.username}」吗？`,
      banning ? '封禁确认' : '解封确认',
      { type: 'warning', confirmButtonText: banning ? '确定封禁' : '确定解封', cancelButtonText: '取消' }
    )
  } catch (e) { return }
  toggling.value = true
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await adminApi.toggleUserStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已解封' : '已封禁')
  } catch (e) {
    // 失败不改 row.status，保持服务端真实状态
  } finally {
    toggling.value = false
  }
}
onMounted(load)
</script>
