<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">💬 FAQ 知识库</h2>
      <el-button type="primary" @click="openDialog()">➕ 新增问答</el-button>
    </div>

    <el-card style="border-radius:14px">
      <div style="margin-bottom:16px;display:flex;gap:10px">
        <el-input v-model="keyword" placeholder="搜索标准问" clearable style="max-width:260px" @keyup.enter="load" />
        <el-button type="primary" plain @click="load">搜索</el-button>
      </div>
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="标准问" min-width="200"><template #default="{row}">
          <span style="font-weight:600">{{ row.question }}</span>
        </template></el-table-column>
        <el-table-column label="分类" width="110"><template #default="{row}">
          <el-tag v-if="row.category" size="small" type="info">{{ row.category }}</el-tag>
          <span v-else style="color:#b2bec3">—</span>
        </template></el-table-column>
        <el-table-column label="关键词" min-width="160" show-overflow-tooltip>
          <template #default="{row}">{{ row.keywords || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template></el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right"><template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next,total" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑问答' : '新增问答'" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标准问" required>
          <el-input v-model="form.question" maxlength="200" show-word-limit placeholder="用户可能问的标准问题" />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="form.answer" type="textarea" :rows="6" maxlength="2000" show-word-limit placeholder="助手命中后回复的答案" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类" clearable style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" maxlength="500" placeholder="检索关键词，逗号分隔，如：退款,退货,多久到账" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <span class="form-tip">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
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

const categories = ['交易规则', '账号', '支付', '发货', '退款', '违规', 'AI']

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return { id: null, question: '', answer: '', category: '', keywords: '', sortOrder: 0, status: 1 }
}

const load = async () => {
  try {
    const res = await adminApi.getFaqs({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) { /* 拦截器已提示 */ }
}

const openDialog = (row) => {
  form.value = row
    ? { id: row.id, question: row.question, answer: row.answer, category: row.category || '', keywords: row.keywords || '', sortOrder: row.sortOrder ?? 0, status: row.status }
    : emptyForm()
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.question.trim()) { ElMessage.warning('请输入标准问'); return }
  if (!form.value.answer.trim()) { ElMessage.warning('请输入答案'); return }
  saving.value = true
  try {
    if (form.value.id) {
      await adminApi.updateFaq(form.value.id, form.value)
      ElMessage.success('问答已更新')
    } else {
      await adminApi.createFaq(form.value)
      ElMessage.success('问答已新增')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* */ } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await adminApi.toggleFaqStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除问答「${row.question}」吗？`, '提示', { type: 'warning' })
  } catch { return }
  await adminApi.deleteFaq(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.form-tip { margin-left: 10px; font-size: 12px; color: #b2bec3; }
</style>
