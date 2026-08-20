<template>
  <div class="page-container" style="max-width:600px">
    <page-back-bar title="消息" />
    <!-- 未读消息提醒：汇总所有会话的未读数 -->
    <div v-if="totalUnread > 0" class="unread-banner">
      <el-badge :value="totalUnread" :max="99" />
      <span>你有 {{ totalUnread }} 条未读消息</span>
    </div>
    <el-card v-for="c in conversations" :key="c.id" class="conv-card">
      <div class="conv-row" @click="$router.push(`/chat/${c.id}`)">
        <!-- 对方头像：点击进入对方主页 -->
        <div class="conv-avatar-wrap" @click.stop="$router.push(`/user/${partnerId(c)}`)">
          <el-avatar :size="42" class="conv-avatar">{{ partnerName(c).charAt(0) }}</el-avatar>
        </div>
        <div class="conv-main">
          <div class="conv-title">
            <strong class="conv-name" @click.stop="$router.push(`/user/${partnerId(c)}`)">{{ partnerName(c) }}</strong>
            <span class="conv-time">{{ formatTime(c.lastMessageTime) }}</span>
          </div>
          <div class="conv-sub">
            <span class="conv-product">商品 #{{ c.productId }}</span>
            <span class="conv-preview">{{ c.lastMessage || '暂无消息' }}</span>
          </div>
        </div>
        <el-badge :value="myUnread(c)" :hidden="!myUnread(c)" :max="99" />
      </div>
    </el-card>
    <el-empty v-if="!conversations.length" description="暂无消息" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { getConversations } from '@/api/im'

const store = useUserStore()
const conversations = ref([])
let timer = null

// 会话中"对方"的 ID
const partnerId = (c) => {
  if (!store.userInfo?.id) return null
  return store.userInfo.id === c.user1Id ? c.user2Id : c.user1Id
}
// 对方名称（会话无昵称字段，先展示"用户+id"，点击可进主页）
const partnerName = (c) => '用户' + (partnerId(c) ?? '')

// 未读数只显示"发给我的"，不能把两个方向的未读加起来
const myUnread = (c) => {
  if (!store.userInfo?.id) return 0
  return store.userInfo.id === c.user1Id ? (c.user1Unread || 0) : (c.user2Unread || 0)
}
// 消息界面未读提醒：汇总所有会话未读数
const totalUnread = computed(() => (conversations.value || []).reduce((s, c) => s + myUnread(c), 0))

// 会话列表时间：今天的显示 HH:mm，更早的显示 M月d日
const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const load = async () => { try { conversations.value = await getConversations() } catch (e) { /* */ } }
onMounted(() => { load(); timer = setInterval(load, 5000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.unread-banner {
  display: flex; align-items: center; gap: 10px;
  background: linear-gradient(135deg, #fff3ee, #ffe8e0);
  border: 1px solid #ffd9cc; border-radius: 10px;
  padding: 10px 14px; margin-bottom: 10px;
  font-size: 14px; color: #e55a2b;
}
.conv-card { margin-bottom: 8px; cursor: pointer; }
.conv-row { display: flex; align-items: center; gap: 12px; }
.conv-avatar-wrap { flex-shrink: 0; cursor: pointer; }
.conv-avatar { background: linear-gradient(135deg, #ff6b35, #ff8c5a); font-size: 16px; }
.conv-main { flex: 1; min-width: 0; }
.conv-title { display: flex; justify-content: space-between; align-items: center; }
.conv-name { cursor: pointer; transition: color 0.2s; }
.conv-name:hover { color: #ff6b35; }
.conv-time { color: #b2bec3; font-size: 12px; margin-left: 12px; flex-shrink: 0; }
.conv-sub { display: flex; align-items: baseline; gap: 8px; margin-top: 4px; }
.conv-product { color: #909399; font-size: 12px; flex-shrink: 0; }
.conv-preview {
  color: #909399; font-size: 13px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
</style>
