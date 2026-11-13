<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <h2 style="font-size:22px;color:#2d3436">🎟️ 优惠券管理</h2>
      <el-button type="primary" @click="openDialog()">➕ 新增优惠券</el-button>
    </div>

    <el-card style="border-radius:14px">
      <div style="margin-bottom:16px;display:flex;gap:10px">
        <el-input v-model="keyword" placeholder="搜索券名称" clearable style="max-width:260px" @keyup.enter="load" />
        <el-button type="primary" plain @click="load">搜索</el-button>
      </div>
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="券名称" min-width="180"><template #default="{row}">
          <span style="font-weight:600">{{ row.name }}</span>
        </template></el-table-column>
        <el-table-column label="类型" width="90"><template #default="{row}">
          <el-tag :type="row.type === 2 ? 'warning' : 'success'" size="small">{{ typeText(row.type) }}</el-tag>
        </template></el-table-column>
        <el-table-column label="优惠力度" width="120"><template #default="{row}">
          <span style="font-weight:600;color:#ff6b35">{{ strengthText(row) }}</span>
        </template></el-table-column>
        <el-table-column label="使用门槛" width="110"><template #default="{row}">
          <span v-if="!row.minAmount || Number(row.minAmount) === 0">无门槛</span>
          <span v-else>满 ¥{{ row.minAmount }}</span>
        </template></el-table-column>
        <el-table-column label="适用分类" width="120"><template #default="{row}">
          <el-tag size="small" :type="row.categoryId ? 'warning' : 'info'">{{ categoryName(row.categoryId) }}</el-tag>
        </template></el-table-column>
        <el-table-column label="库存" width="110"><template #default="{row}">
          {{ row.claimedCount }}/{{ row.totalCount }}
        </template></el-table-column>
        <el-table-column label="有效期" min-width="180"><template #default="{row}">
          <span>{{ fmtDate(row.validStart) }} ~ {{ fmtDate(row.validEnd) }}</span>
        </template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template></el-table-column>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑优惠券' : '新增优惠券'" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-form-item label="券名称" required>
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="如：新人满减券" />
        </el-form-item>
        <el-form-item label="券类型" required>
          <el-select v-model="form.type" :disabled="!!form.id" style="width:100%" @change="onTypeChange">
            <el-option :value="1" label="满减券" />
            <el-option :value="2" label="折扣券" />
          </el-select>
          <span class="form-tip" v-if="form.id">类型创建后不可修改</span>
        </el-form-item>
        <el-form-item :label="form.type === 2 ? '折扣率' : '减免金额'" required>
          <el-input-number v-if="form.type === 2" v-model="form.discountAmount" :min="0.1" :max="0.99" :step="0.01" :precision="2" />
          <el-input-number v-else v-model="form.discountAmount" :min="0.01" :precision="2" />
          <span class="form-tip">{{ form.type === 2 ? '0.9 表示 9 折' : '满足门槛后减免的金额' }}</span>
        </el-form-item>
        <el-form-item label="封顶金额" v-if="form.type === 2">
          <el-input-number v-model="form.maxDiscountAmount" :min="0" :precision="2" />
          <span class="form-tip">折扣减免最多减多少，0 表示不封顶</span>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" />
          <span class="form-tip">0 表示无门槛</span>
        </el-form-item>
        <el-form-item label="适用分类">
          <el-select v-model="form.categoryId" style="width:100%">
            <el-option :value="0" label="全部分类（通用）" />
            <el-option v-for="c in categoryOptions" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
          <span class="form-tip">仅该分类商品可用；选全部分类则通用</span>
        </el-form-item>
        <el-form-item label="发行总量" required>
          <el-input-number v-model="form.totalCount" :min="1" :max="999999" />
        </el-form-item>
        <el-form-item label="有效期">
          <div style="display:flex;gap:10px;width:100%">
            <el-date-picker v-model="form.validStart" type="datetime" placeholder="开始时间（不限）" value-format="YYYY-MM-DDTHH:mm:ss" style="flex:1" />
            <el-date-picker v-model="form.validEnd" type="datetime" placeholder="截止时间（不限）" value-format="YYYY-MM-DDTHH:mm:ss" style="flex:1" />
          </div>
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

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref(emptyForm())
const categoryOptions = ref([])

function emptyForm() {
  return { id: null, name: '', type: 1, discountAmount: null, maxDiscountAmount: 0, minAmount: 0, categoryId: 0, totalCount: 100, validStart: null, validEnd: null, status: 1 }
}

// 分类树拍平为下拉选项（0 单列“全部分类”）
const flattenCategories = (list, acc = []) => {
  ;(list || []).forEach(c => {
    acc.push({ id: c.id, name: c.name })
    if (c.children?.length) flattenCategories(c.children, acc)
  })
  return acc
}
const categoryName = (id) => categoryOptions.value.find(c => Number(c.id) === Number(id))?.name || '全部分类'

const loadCategories = async () => {
  try {
    const tree = await adminApi.getCategoryTree()
    categoryOptions.value = flattenCategories(tree || [])
  } catch (e) { categoryOptions.value = [] }
}

const typeText = (t) => ({ 1: '满减', 2: '折扣' }[t] || '满减')
const strengthText = (row) => {
  if (row.type === 2) return `${(Number(row.discountAmount) * 10).toFixed(1)} 折`
  return `减 ¥${row.discountAmount}`
}
const fmtDate = (s) => s ? String(s).replace('T', ' ').slice(0, 16) : '不限'

const load = async () => {
  try {
    const res = await adminApi.getCoupons({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) { /* 拦截器已提示 */ }
}

const onTypeChange = () => {
  form.value.discountAmount = null
}

const openDialog = (row) => {
  form.value = row
    ? { id: row.id, name: row.name, type: row.type, discountAmount: Number(row.discountAmount), maxDiscountAmount: Number(row.maxDiscountAmount) || 0, minAmount: Number(row.minAmount) || 0, categoryId: row.categoryId || 0, totalCount: row.totalCount, validStart: row.validStart || null, validEnd: row.validEnd || null, status: row.status }
    : emptyForm()
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name.trim()) { ElMessage.warning('请输入券名称'); return }
  if (form.value.discountAmount == null) { ElMessage.warning(form.value.type === 2 ? '请填写折扣率' : '请填写减免金额'); return }
  if (form.value.type === 2 && (Number(form.value.discountAmount) <= 0 || Number(form.value.discountAmount) >= 1)) {
    ElMessage.warning('折扣率需在 0~1 之间（如 0.9 表示 9 折）'); return
  }
  if (form.value.type !== 2 && Number(form.value.discountAmount) <= 0) { ElMessage.warning('减免金额必须大于 0'); return }
  if (!form.value.totalCount || form.value.totalCount < 1) { ElMessage.warning('发行总量至少为 1'); return }
  if (form.value.validStart && form.value.validEnd && form.value.validStart > form.value.validEnd) {
    ElMessage.warning('有效期截止时间不能早于开始时间'); return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await adminApi.updateCoupon(form.value.id, form.value)
      ElMessage.success('优惠券已更新')
    } else {
      await adminApi.createCoupon(form.value)
      ElMessage.success('优惠券已新增')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* */ } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await adminApi.toggleCouponStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除优惠券「${row.name}」吗？已领取的用户券不受影响。`, '提示', { type: 'warning' })
  } catch { return }
  await adminApi.deleteCoupon(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => { load(); loadCategories() })
</script>

<style scoped>
.form-tip { margin-left: 10px; font-size: 12px; color: #b2bec3; }
</style>
