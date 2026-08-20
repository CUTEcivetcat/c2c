<template>
  <div class="page-container" style="max-width:600px">
    <page-back-bar title="收货地址" />
    <el-button type="primary" @click="showForm=true;editing=null;form={}" style="margin:12px 0">新增地址</el-button>
    <el-card v-for="a in addresses" :key="a.id" style="margin-bottom:8px">
      <p><strong>{{ a.receiverName }}</strong> {{ a.phone }} <el-tag v-if="a.isDefault" size="small" type="success">默认</el-tag></p>
      <p style="color:#909399;font-size:13px">{{ a.province }} {{ a.city }} {{ a.district }} {{ a.detail }}</p>
      <div style="margin-top:8px">
        <el-button size="small" @click="editing=a.id;form={...a};showForm=true">编辑</el-button>
        <el-button size="small" type="danger" @click="del(a.id)">删除</el-button>
        <el-button size="small" v-if="!a.isDefault" @click="setDef(a.id)">设为默认</el-button>
      </div>
    </el-card>
    <el-dialog v-model="showForm" :title="editing?'编辑地址':'新增地址'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收件人"><el-input v-model="form.receiverName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="省"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="区"><el-input v-model="form.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" /></el-form-item>
        <el-form-item><el-button type="primary" @click="save">保存</el-button></el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/user'
import { ElMessage } from 'element-plus'

const addresses = ref([])
const showForm = ref(false)
const editing = ref(null)
const form = ref({})

const load = async () => { addresses.value = await getAddresses() }
onMounted(load)
const save = async () => {
  if (editing.value) await updateAddress(editing.value, form.value)
  else await addAddress(form.value)
  showForm.value = false; load()
}
const del = async (id) => { await deleteAddress(id); load() }
const setDef = async (id) => { await setDefaultAddress(id); load() }
</script>
