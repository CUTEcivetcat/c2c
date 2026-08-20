<template>
  <div class="page-container" style="max-width:500px">
    <page-back-bar title="评价" />
    <div style="margin:20px 0"><span style="color:#909399">评分：</span><el-rate v-model="score" /></div>
    <el-input v-model="comment" type="textarea" :rows="3" placeholder="写下你的评价..." />
    <el-button type="primary" style="margin-top:16px;width:100%" @click="submit" :loading="loading">提交评价</el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { submitRating } from '@/api/rating'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const score = ref(5)
const comment = ref('')
const loading = ref(false)

const submit = async () => {
  loading.value = true
  try {
    await submitRating({ orderId: Number(route.params.orderId), score: score.value, comment: comment.value, tags: '[]' })
    ElMessage.success('评价成功')
    router.push('/order/list')
  } catch(e) { /* */ }
  loading.value = false
}
</script>
