<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">🔐 权限管理</h2>

    <div style="display:flex;gap:12px;margin-bottom:20px;flex-wrap:wrap">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称/手机号/邮箱" clearable style="width:260px" @change="load" />
      <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width:140px" @change="load">
        <el-option :value="0" label="普通用户" /><el-option :value="1" label="管理员" /><el-option :value="2" label="审核员" />
      </el-select>
      <el-button type="primary" @click="load" :loading="loading">搜索</el-button>
    </div>

    <el-card style="border-radius:14px">
      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="nickname" label="昵称" min-width="120"><template #default="{row}">
          <div style="font-weight:600">{{ row.nickname }}</div>
          <div style="font-size:12px;color:#b2bec3">{{ row.username }}</div>
        </template></el-table-column>
        <el-table-column label="手机号/邮箱" min-width="160"><template #default="{row}">
          <div v-if="row.phone">{{ row.phone }}</div>
          <div v-else-if="row.email" style="color:#636e72">{{ row.email }}</div>
          <span v-else style="color:#b2bec3">-</span>
        </template></el-table-column>
        <el-table-column label="角色" width="110"><template #default="{row}">
          <el-tag :type="roleType(row.role)" size="small">{{ roleText(row.role) }}</el-tag>
        </template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template></el-table-column>
        <el-table-column label="操作" min-width="180"><template #default="{row}">
          <template v-if="row.role === 0">
            <el-button type="warning" size="small" plain @click="setRole(row, 2)">设为审核员</el-button>
          </template>
          <template v-else-if="row.role === 2">
            <el-button type="info" size="small" plain @click="setRole(row, 0)">取消审核员</el-button>
          </template>
          <template v-else>
            <span style="color:#b2bec3;font-size:12px">管理员账号</span>
          </template>
        </template></el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next" :total="total" :page-size="20" v-model:current-page="page" @change="load" />
      </div>
    </el-card>

    <el-alert type="info" :closable="false" style="margin-top:16px;border-radius:10px"
      title="说明：设为「审核员」后，该账号使用普通端登录即可看到「审核模块」，进入审核工作台处理商品举报与整改申诉；取消后恢复普通用户身份（需重新登录生效）。" />
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
const roleFilter = ref(null)
const loading = ref(false)

const roleText = (r) => ({0:'普通用户',1:'管理员',2:'审核员'}[r] || '未知')
const roleType = (r) => ({0:'info',1:'danger',2:'warning'}[r] || '')

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.getUserRoles({ keyword: keyword.value, role: roleFilter.value, page: page.value, size: 20 })
    users.value = res.records; total.value = res.total
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

const setRole = async (row, role) => {
  const label = role === 2 ? '设为审核员' : '取消审核员'
  try {
    await ElMessageBox.confirm(
      `确定对账号「${row.nickname}」执行「${label}」吗？${role === 2 ? '该账号登录后将获得审核权限。' : '该账号的审核权限将被收回。'}`,
      '权限分配确认', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch (e) { return }
  try {
    await adminApi.setUserRole(row.id, role)
    ElMessage.success('操作成功，重新登录后生效')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(load)
</script>
