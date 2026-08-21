<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">📢 公告管理</h2>
      <el-button type="primary" @click="openDialog()">➕ 发布公告</el-button>
    </div>

    <el-card style="border-radius:14px">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="标题" min-width="220"><template #default="{row}">
          <div style="display:flex;align-items:center;gap:6px;font-weight:600">
            <el-tag v-if="row.pinned === 1" type="danger" size="small">置顶</el-tag>
            <span>{{ row.title }}</span>
          </div>
        </template></el-table-column>
        <el-table-column label="类型" width="110"><template #default="{row}">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
        </template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '已发布' : '已下架' }}</el-tag>
        </template></el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right"><template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next,total" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </el-card>

    <!-- 发布/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">公告</el-radio>
            <el-radio :value="2">平台公约</el-radio>
            <el-radio :value="3">通知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="强制弹窗">
          <el-switch v-model="form.isForce" :active-value="1" :inactive-value="0" />
          <span class="form-tip">开启后用户登录时强制弹出阅读</span>
        </el-form-item>
        <el-form-item label="停留秒数" v-if="form.isForce === 1">
          <el-input-number v-model="form.minSeconds" :min="1" :max="60" />
          <span class="form-tip">最低停留秒数，期间不可关闭</span>
        </el-form-item>
        <el-form-item label="滚动显示">
          <el-switch v-model="form.scroll" :active-value="1" :inactive-value="0" />
          <span class="form-tip">首页横幅参与轮播（默认滚动）</span>
        </el-form-item>
        <el-form-item label="发布页展示">
          <el-switch v-model="form.showOnPublish" :active-value="1" :inactive-value="0" />
          <span class="form-tip">在发布商品页右侧展示公告内容</span>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="公告内容（可换行）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({ id: null, title: '', content: '', type: 1, pinned: 0, isForce: 0, minSeconds: 5, scroll: 1, showOnPublish: 0 })

const typeText = (t) => ({ 1: '公告', 2: '平台公约', 3: '通知' }[t] || '公告')
const typeTag = (t) => ({ 1: '', 2: 'warning', 3: 'info' }[t] || '')

const load = async () => {
  try {
    const res = await adminApi.getAnnouncements({ page: page.value, size: size.value })
    list.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) { /* 拦截器已提示 */ }
}

const openDialog = (row) => {
  form.value = row ? { id: row.id, title: row.title, content: row.content, type: row.type, pinned: row.pinned, isForce: row.isForce, minSeconds: row.minSeconds || 5, scroll: row.scroll ?? 1, showOnPublish: row.showOnPublish ?? 0 } : { id: null, title: '', content: '', type: 1, pinned: 0, isForce: 0, minSeconds: 5, scroll: 1, showOnPublish: 0 }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.value.content.trim()) { ElMessage.warning('请输入内容'); return }
  saving.value = true
  try {
    if (form.value.id) {
      await adminApi.updateAnnouncement(form.value.id, form.value)
      ElMessage.success('公告已更新')
    } else {
      await adminApi.createAnnouncement(form.value)
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* */ } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await adminApi.toggleAnnouncementStatus(row.id, next)
  ElMessage.success(next === 1 ? '公告已发布' : '公告已下架')
  load()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch { return }
  await adminApi.deleteAnnouncement(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.form-tip { margin-left: 10px; font-size: 12px; color: #b2bec3; }
</style>
