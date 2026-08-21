<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">🎠 轮播图管理</h2>
      <el-button type="primary" @click="openDialog()">➕ 新增轮播图</el-button>
    </div>

    <el-card style="border-radius:14px">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="140"><template #default="{row}">
          <product-cover :src="row.imageUrl" style="width:120px;height:60px;border-radius:8px" />
        </template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="90"><template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="暂无轮播图" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑轮播图' : '新增轮播图'" width="480px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" placeholder="选填" />
        </el-form-item>
        <el-form-item label="图片" required>
          <el-upload action="/api/v1/upload/image" :headers="uploadHeaders" :show-file-list="false" :on-success="onUpload" :on-change="onFileChange" :before-upload="beforeUpload" class="upload-btn">
            <el-button :loading="uploading">上传图片</el-button>
          </el-upload>
          <div v-if="previewUrl" style="margin-top:8px;position:relative">
            <el-image :src="previewUrl" style="width:200px;height:100px;border-radius:8px" fit="cover" />
            <div v-if="uploading" class="upload-overlay"><el-icon class="is-loading" :size="24"><Loading /></el-icon></div>
          </div>
          <div v-else style="margin-top:8px;background:#f0f2f5;border-radius:8px;width:200px;height:100px;display:flex;align-items:center;justify-content:center;font-size:12px;color:#b2bec3">暂无图片</div>
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="选填，点击轮播图跳转的地址" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="99" />
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
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({ id: null, title: '', imageUrl: '', linkUrl: '', sortOrder: 0, status: 1 })
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('admin_token')}` }
const previewUrl = ref('')
const uploading = ref(false)

const load = async () => {
  try { list.value = await adminApi.getBanners() || [] } catch (e) { /* */ }
}

const openDialog = (row) => {
  form.value = row ? { id: row.id, title: row.title, imageUrl: row.imageUrl, linkUrl: row.linkUrl, sortOrder: row.sortOrder, status: row.status } : { id: null, title: '', imageUrl: '', linkUrl: '', sortOrder: 0, status: 1 }
  previewUrl.value = row ? row.imageUrl : ''
  dialogVisible.value = true
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) { ElMessage.warning('请上传图片文件'); return false }
  uploading.value = true
  return true
}

const onFileChange = (file) => {
  if (file.status === 'ready') {
    previewUrl.value = URL.createObjectURL(file.raw)
  }
}

const onUpload = (res) => {
  uploading.value = false
  if (res.code === 200) {
    form.value.imageUrl = res.data.url
    previewUrl.value = res.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const save = async () => {
  if (!form.value.imageUrl) { ElMessage.warning('请上传图片'); return }
  saving.value = true
  try {
    if (form.value.id) { await adminApi.updateBanner(form.value.id, form.value); ElMessage.success('已更新') }
    else { await adminApi.createBanner(form.value); ElMessage.success('已新增') }
    dialogVisible.value = false
    load()
  } catch (e) { /* */ } finally { saving.value = false }
}

const toggle = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await adminApi.updateBanner(row.id, { ...row, status: next })
  row.status = next
  ElMessage.success(next === 1 ? '已启用' : '已停用')
}

const remove = async (row) => {
  try { await ElMessageBox.confirm('确定删除该轮播图吗？', '提示', { type: 'warning' }) } catch { return }
  await adminApi.deleteBanner(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.upload-btn { display: inline-block; }
.upload-overlay {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.7); border-radius: 8px;
}
</style>