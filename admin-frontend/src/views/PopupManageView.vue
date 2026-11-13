<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">🎯 活动弹窗管理</h2>
      <el-button type="primary" @click="openDialog()">➕ 新增弹窗</el-button>
    </div>

    <el-card style="border-radius:14px">
      <div style="margin-bottom:16px;display:flex;gap:10px">
        <el-input v-model="keyword" placeholder="搜索弹窗标题" clearable style="max-width:260px" @keyup.enter="load" />
        <el-button type="primary" plain @click="load">搜索</el-button>
      </div>
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="标题" min-width="180"><template #default="{row}">
          <span style="font-weight:600">{{ row.title }}</span>
        </template></el-table-column>
        <el-table-column label="绑定券" width="130"><template #default="{row}">
          <el-tag v-if="row.couponTemplateId" size="small" type="warning">{{ couponName(row.couponTemplateId) }}</el-tag>
          <span v-else style="color:#b2bec3">未绑定</span>
        </template></el-table-column>
        <el-table-column label="跳转" width="110"><template #default="{row}">
          <el-tag size="small" :type="row.jumpType === 'none' ? 'info' : 'primary'">{{ jumpText(row.jumpType) }}</el-tag>
        </template></el-table-column>
        <el-table-column label="有效期" min-width="180"><template #default="{row}">
          <span>{{ fmtDate(row.validStart) }} ~ {{ fmtDate(row.validEnd) }}</span>
        </template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '展示' : '下架' }}</el-tag>
        </template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right"><template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:center">
        <el-pagination layout="prev,pager,next,total" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑活动弹窗' : '新增活动弹窗'" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="弹窗标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="弹窗活动内容" />
        </el-form-item>
        <el-form-item label="配图链接">
          <el-input v-model="form.imageUrl" placeholder="可空，图片 URL" />
        </el-form-item>
        <el-form-item label="绑定优惠券">
          <el-select v-model="form.couponTemplateId" clearable style="width:100%" @change="onCouponChange">
            <el-option :value="0" label="不绑定活动券" />
            <el-option v-for="c in couponOptions" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
          <span class="form-tip">绑定后弹窗有效期跟随券有效期</span>
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-radio-group v-model="form.jumpType">
            <el-radio value="none">不跳转</el-radio>
            <el-radio value="coupon">优惠券</el-radio>
            <el-radio value="product">商品</el-radio>
            <el-radio value="announcement">公告</el-radio>
            <el-radio value="url">外链</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="跳转目标" v-if="form.jumpType === 'product' || form.jumpType === 'url'">
          <el-input v-model="form.jumpTarget" :placeholder="form.jumpType === 'product' ? '商品 ID' : '外链 URL'" />
        </el-form-item>
        <el-form-item label="有效期" v-if="!form.couponTemplateId">
          <div style="display:flex;gap:10px;width:100%">
            <el-date-picker v-model="form.validStart" type="datetime" placeholder="开始时间（不限）" value-format="YYYY-MM-DDTHH:mm:ss" style="flex:1" />
            <el-date-picker v-model="form.validEnd" type="datetime" placeholder="截止时间（不限）" value-format="YYYY-MM-DDTHH:mm:ss" style="flex:1" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="展示" inactive-text="下架" />
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
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const couponOptions = ref([])
const form = ref(emptyForm())

function emptyForm() {
  return { id: null, title: '', content: '', imageUrl: '', couponTemplateId: 0, jumpType: 'none', jumpTarget: '', validStart: null, validEnd: null, status: 1 }
}

const jumpText = (t) => ({ none: '不跳转', coupon: '优惠券', product: '商品', announcement: '公告', url: '外链' }[t] || '不跳转')
const fmtDate = (s) => s ? String(s).replace('T', ' ').slice(0, 16) : '不限'
const couponName = (id) => couponOptions.value.find(c => Number(c.id) === Number(id))?.name || ('券 #' + id)

const load = async () => {
  try {
    const res = await adminApi.getPopups({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) { /* 拦截器已提示 */ }
}

const loadCoupons = async () => {
  try {
    const res = await adminApi.getCoupons({ page: 1, size: 100 })
    couponOptions.value = res.records || []
  } catch (e) { couponOptions.value = [] }
}

const onCouponChange = () => {
  // 绑券后有效期由后端跟随券；清空手填时间避免误导
  if (form.value.couponTemplateId) {
    form.value.validStart = null
    form.value.validEnd = null
  }
}

const openDialog = (row) => {
  form.value = row
    ? { id: row.id, title: row.title, content: row.content, imageUrl: row.imageUrl || '', couponTemplateId: row.couponTemplateId || 0, jumpType: row.jumpType || 'none', jumpTarget: row.jumpTarget || '', validStart: row.validStart || null, validEnd: row.validEnd || null, status: row.status }
    : emptyForm()
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.value.content.trim()) { ElMessage.warning('请输入内容'); return }
  saving.value = true
  try {
    if (form.value.id) {
      await adminApi.updatePopup(form.value.id, form.value)
      ElMessage.success('弹窗已更新')
    } else {
      await adminApi.createPopup(form.value)
      ElMessage.success('弹窗已新增')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* */ } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await adminApi.togglePopupStatus(row.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已下架')
  load()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除弹窗「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch { return }
  await adminApi.deletePopup(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => { load(); loadCoupons() })
</script>

<style scoped>
.form-tip { margin-left: 10px; font-size: 12px; color: #b2bec3; }
</style>
