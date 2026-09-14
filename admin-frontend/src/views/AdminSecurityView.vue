<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">🔐 安全设置</h2>

    <!-- 我的二级密码 -->
    <el-card style="border-radius:14px;margin-bottom:20px">
      <template #header><span style="font-weight:700">我的二级密码</span></template>
      <p style="font-size:13px;color:#909399;margin:0 0 16px">
        二级密码用于删除账户等高危操作的身份二次校验。设置后，执行删除账户时必须输入该密码。
      </p>
      <el-form :model="pwdForm" ref="pwdFormRef" :rules="pwdRules" label-position="top" style="max-width:560px">
        <el-form-item label="新二级密码（至少 6 位）" prop="pwd" style="margin-bottom:16px">
          <el-input v-model="pwdForm.pwd" type="password" show-password placeholder="请输入新二级密码"
                    style="width:300px" maxlength="32" @keyup.enter="savePwd" />
        </el-form-item>
        <el-form-item label="确认二级密码" prop="pwd2" style="margin-bottom:16px">
          <el-input v-model="pwdForm.pwd2" type="password" show-password placeholder="再次输入确认"
                    style="width:300px" maxlength="32" @keyup.enter="savePwd" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPwd" @click="savePwd">保存二级密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 管理员管理（仅总管理员 admin 可见） -->
    <el-card v-if="isTop" style="border-radius:14px">
      <template #header>
        <span style="font-weight:700">管理员管理</span>
        <el-tag type="danger" size="small" style="margin-left:10px">仅总管理员</el-tag>
      </template>
      <p style="font-size:13px;color:#909399;margin:0 0 16px">
        总管理员（admin）拥有全部权限。你可以在下方添加 <b>次级管理员</b>（可授予全部或部分模块权限）与
        <b>子级管理员</b>（仅授予勾选的少量模块权限），为其设置「账户名 + 登录密码 + 二级密码 + 模块权限」。
        审核员（role=2）为最低级别：仅可进入用户端审核工作台，<b>不能登录管理端口</b>。
      </p>

      <!-- 添加表单 -->
      <el-form label-width="90px" style="max-width:720px;margin-bottom:18px">
        <el-form-item label="账户名">
          <el-input v-model="form.username" placeholder="字母/数字/下划线，3~30位" style="width:240px" maxlength="30" />
        </el-form-item>
        <el-form-item label="登录密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" style="width:220px" maxlength="32" />
        </el-form-item>
        <el-form-item label="二级密码">
          <el-input v-model="form.secondaryPassword" type="password" show-password placeholder="至少 6 位" style="width:220px" maxlength="32" />
        </el-form-item>
        <el-form-item label="管理员级别">
          <el-radio-group v-model="form.level">
            <el-radio value="secondary">次级管理员（可授予全部模块权限）</el-radio>
            <el-radio value="sub">子级管理员（仅授予勾选的部分权限）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模块权限">
          <el-checkbox-group v-model="form.perms">
            <el-checkbox v-for="p in PERM_OPTIONS" :key="p.code" :value="p.code">{{ p.label }}</el-checkbox>
          </el-checkbox-group>
          <div style="font-size:12px;color:#b2bec3;margin-top:4px;width:100%">未勾选任何权限时，该管理员登录后仅可使用「安全设置」（设置自己的二级密码）。</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="adding" @click="addAdmin">添加管理员</el-button>
          <el-button @click="resetForm">清空</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="admins" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="账户名" min-width="110" />
        <el-table-column label="级别" width="100">
          <template #default="{row}"><el-tag type="warning" size="small">次级/子级</el-tag></template>
        </el-table-column>
        <el-table-column label="模块权限" min-width="220">
          <template #default="{row}">
            <el-tag v-for="p in permTexts(row.adminPerms)" :key="p" size="small" style="margin-right:4px">{{ p }}</el-tag>
            <span v-if="!row.adminPerms" style="color:#b2bec3;font-size:12px">仅安全设置</span>
          </template>
        </el-table-column>
        <el-table-column label="二级密码" width="96">
          <template #default="{row}">
            <el-tag :type="row.hasSecondaryPwd ? 'success' : 'info'" size="small">{{ row.hasSecondaryPwd ? '已设置' : '未设置' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{row}">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button type="primary" size="small" plain @click="openEdit(row)">编辑权限</el-button>
            <el-button type="danger" size="small" plain @click="removeAdmin(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!admins.length" style="text-align:center;color:#b2bec3;padding:24px 0;font-size:13px">
        暂无次级/子级管理员，可在上方添加
      </div>
    </el-card>

    <el-card v-else style="border-radius:14px">
      <p style="font-size:13px;color:#909399;margin:0">管理员列表仅总管理员（admin）可管理。你可以使用上方功能设置自己的二级密码。</p>
    </el-card>

    <!-- 编辑管理员权限弹窗 -->
    <el-dialog v-model="editVisible" title="编辑管理员权限" width="520px">
      <p style="font-size:13px;color:#909399;margin:0 0 12px">账户：<b>{{ editRow?.username }}</b></p>
      <div style="font-size:13px;color:#2d3436;margin-bottom:6px">模块权限</div>
      <el-checkbox-group v-model="editPerms">
        <el-checkbox v-for="p in PERM_OPTIONS" :key="p.code" :value="p.code">{{ p.label }}</el-checkbox>
      </el-checkbox-group>
      <div style="font-size:13px;color:#2d3436;margin:18px 0 6px">重置登录密码（留空不修改）</div>
      <el-input v-model="editPassword" type="password" show-password placeholder="至少 6 位" maxlength="32" />
      <div style="font-size:13px;color:#2d3436;margin:14px 0 6px">重置二级密码（留空不修改）</div>
      <el-input v-model="editSecondaryPwd" type="password" show-password placeholder="至少 6 位" maxlength="32" />
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const pwdFormRef = ref()
const pwdForm = ref({ pwd: '', pwd2: '' })
const savingPwd = ref(false)

const validatePwd2 = (rule, value, cb) => {
  if (!value) cb(new Error('请再次输入确认'))
  else if (value !== pwdForm.value.pwd) cb(new Error('两次输入的密码不一致'))
  else cb()
}
const pwdRules = {
  pwd: [
    { required: true, message: '请输入新二级密码', trigger: 'blur' },
    { min: 6, max: 32, message: '二级密码至少 6 位', trigger: 'blur' }
  ],
  pwd2: [{ validator: validatePwd2, trigger: 'blur' }]
}

const isTop = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem('admin_user') || '{}')
    return u.adminLevel === 2 || u.username === 'admin'
  } catch { return false }
})

const PERM_OPTIONS = [
  { code: 'dashboard', label: '数据大屏' },
  { code: 'users', label: '用户管理' },
  { code: 'products', label: '商品管理' },
  { code: 'orders', label: '订单管理' },
  { code: 'review', label: '审核管理' },
  { code: 'announcement', label: '公告管理' },
  { code: 'banner', label: '轮播图管理' },
  { code: 'permissions', label: '权限管理' }
]
const PERM_LABEL = Object.fromEntries(PERM_OPTIONS.map(p => [p.code, p.label]))
const permTexts = (str) => (str || '').split(',').filter(Boolean).map(c => PERM_LABEL[c] || c)

const savePwd = async () => {
  try { await pwdFormRef.value.validate() } catch { return }
  savingPwd.value = true
  try {
    await adminApi.setSecondaryPassword({ password: pwdForm.value.pwd })
    ElMessage.success('二级密码已保存')
    pwdForm.value = { pwd: '', pwd2: '' }
    pwdFormRef.value.resetFields()
  } catch (e) { /* 拦截器已提示 */ } finally {
    savingPwd.value = false
  }
}

// ---- 管理员管理 ----
const admins = ref([])
const adding = ref(false)
const form = ref({ username: '', password: '', secondaryPassword: '', level: 'secondary', perms: PERM_OPTIONS.map(p => p.code) })

const resetForm = () => { form.value = { username: '', password: '', secondaryPassword: '', level: 'secondary', perms: PERM_OPTIONS.map(p => p.code) } }

const loadAdmins = async () => {
  if (!isTop.value) return
  try { admins.value = await adminApi.getSecondaryAdmins() || [] } catch (e) { /* 拦截器已提示 */ }
}

const addAdmin = async () => {
  if (!form.value.username || form.value.username.length < 3) return ElMessage.warning('请输入 3~30 位账户名')
  if (!/^[a-zA-Z0-9_]+$/.test(form.value.username)) return ElMessage.warning('账户名仅支持字母、数字、下划线')
  if (!form.value.password || form.value.password.length < 6) return ElMessage.warning('登录密码至少 6 位')
  if (!form.value.secondaryPassword || form.value.secondaryPassword.length < 6) return ElMessage.warning('二级密码至少 6 位')
  adding.value = true
  try {
    await adminApi.addSecondaryAdmin({
      username: form.value.username,
      password: form.value.password,
      secondaryPassword: form.value.secondaryPassword,
      perms: form.value.perms
    })
    ElMessage.success('管理员已添加')
    resetForm()
    loadAdmins()
  } catch (e) { /* 拦截器已提示 */ } finally {
    adding.value = false
  }
}

// ---- 编辑权限 ----
const editVisible = ref(false)
const editRow = ref(null)
const editPerms = ref([])
const editPassword = ref('')
const editSecondaryPwd = ref('')
const editing = ref(false)

const openEdit = (row) => {
  editRow.value = row
  editPerms.value = (row.adminPerms || '').split(',').filter(Boolean)
  editPassword.value = ''
  editSecondaryPwd.value = ''
  editVisible.value = true
}

const saveEdit = async () => {
  editing.value = true
  try {
    const data = { perms: editPerms.value }
    if (editPassword.value) data.password = editPassword.value
    if (editSecondaryPwd.value) data.secondaryPassword = editSecondaryPwd.value
    await adminApi.updateSecondaryAdmin(editRow.value.id, data)
    ElMessage.success('已保存')
    editVisible.value = false
    loadAdmins()
  } catch (e) { /* 拦截器已提示 */ } finally {
    editing.value = false
  }
}

const removeAdmin = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除管理员「${row.username}」吗？删除后该账号将无法登录管理后台。`, '删除管理员', { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' })
  } catch (e) { return }
  try {
    await adminApi.deleteSecondaryAdmin(row.id)
    ElMessage.success('已删除')
    loadAdmins()
  } catch (e) { /* 拦截器已提示 */ }
}

const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 19) : ''
onMounted(loadAdmins)
</script>
