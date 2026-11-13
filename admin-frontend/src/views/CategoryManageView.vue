<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">🗂️ 分类管理</h2>
      <el-button type="primary" @click="openDialog()">➕ 新增分类</el-button>
    </div>

    <el-card style="border-radius:14px">
      <el-table :data="treeData" row-key="id" :tree-props="{ children: 'children' }" default-expand-all stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="分类名称" min-width="200">
          <template #default="{row}">
            <span style="font-weight:600">{{ row.name }}</span>
            <el-tag v-if="row.level === 1" size="small" type="info" style="margin-left:8px">一级</el-tag>
            <el-tag v-else size="small" type="warning" style="margin-left:8px">二级</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="图标" min-width="160">
          <template #default="{row}">
            <span v-if="row.iconUrl" style="color:#636e72">{{ row.iconUrl }}</span>
            <span v-else style="color:#b2bec3">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑分类 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="480px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" maxlength="50" show-word-limit placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" clearable placeholder="不选则为一级分类" style="width:100%">
            <el-option :value="0" label="（顶级分类）" />
            <el-option v-for="c in topOptions" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <span class="form-tip">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="图标链接">
          <el-input v-model="form.iconUrl" placeholder="可空，分类图标 URL" />
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
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return { id: null, name: '', parentId: 0, sortOrder: 0, iconUrl: '' }
}

// 组装两级树（后端 listAll 为扁平列表）
const treeData = computed(() => {
  const map = new Map(list.value.map(c => [c.id, { ...c, children: [] }]))
  const roots = []
  map.forEach(node => {
    if (node.parentId && node.parentId !== 0 && map.has(node.parentId)) {
      map.get(node.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
})

// 上级分类下拉：仅一级分类可作为父级（保持两级）
const topOptions = computed(() => list.value.filter(c => !c.parentId || c.parentId === 0))

const load = async () => {
  try {
    list.value = await adminApi.getAdminCategories() || []
  } catch (e) { /* 拦截器已提示 */ }
}

const openDialog = (row) => {
  form.value = row
    ? { id: row.id, name: row.name, parentId: row.parentId || 0, sortOrder: row.sortOrder ?? 0, iconUrl: row.iconUrl || '' }
    : emptyForm()
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  saving.value = true
  try {
    if (form.value.id) {
      await adminApi.updateCategory(form.value.id, form.value)
      ElMessage.success('分类已更新')
    } else {
      await adminApi.createCategory(form.value)
      ElMessage.success('分类已新增')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？存在子分类或关联商品时无法删除。`, '提示', { type: 'warning' })
  } catch { return }
  await adminApi.deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.form-tip { margin-left: 10px; font-size: 12px; color: #b2bec3; }
</style>
