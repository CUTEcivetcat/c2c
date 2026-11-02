<template>
  <div class="page-container" style="max-width:600px">
    <page-back-bar title="消息" />
    <!-- 平台助手固定入口 -->
    <div class="assistant-entry" @click="goAssistant">
      <div class="assistant-avatar">🤖</div>
      <div class="assistant-main">
        <div class="assistant-title"><strong>青禾助手</strong><span class="ai-tag">AI</span></div>
        <div class="assistant-sub">查订单 · 问规则 · 7×24 在线</div>
      </div>
      <span class="assistant-arrow">›</span>
    </div>
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
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getConversations, getOrCreateConversation } from '@/api/im'

const store = useUserStore()
const router = useRouter()
const conversations = ref([])
let timer = null

/** 平台助手账号（与后端一致） */
const ASSISTANT_ID = 90000001
// 进入/创建助手会话并跳转
const goAssistant = async () => {
  try {
    const conv = await getOrCreateConversation({ targetUserId: ASSISTANT_ID })
    if (conv && conv.id) router.push(`/chat/${conv.id}`)
  } catch (e) { /* 拦截器已提示 */ }
}

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
/* 平台助手固定入口 */
.assistant-entry {
  display: flex; align-items: center; gap: 12px;
  background: linear-gradient(135deg, #fff7f2, #ffffff);
  border: 1px solid var(--c2c-line-soft, #f0e6e0);
  border-radius: 16px; padding: 14px 16px; margin-bottom: 12px;
  cursor: pointer; transition: all 0.25s ease;
  box-shadow: 0 2px 10px rgba(255, 107, 53, 0.06);
}
.assistant-entry:hover {
  transform: translateY(-2px);
  border-color: var(--c2c-primary, #ff6b35);
  box-shadow: 0 8px 20px rgba(255, 107, 53, 0.14);
}
.assistant-avatar {
  width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, var(--c2c-primary, #ff6b35), var(--c2c-primary-light, #ff8c5a));
  display: flex; align-items: center; justify-content: center; font-size: 22px;
}
.assistant-main { flex: 1; min-width: 0; }
.assistant-title { display: flex; align-items: center; gap: 8px; font-size: 15px; color: var(--c2c-text, #243b53); }
.ai-tag {
  font-size: 10px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  padding: 1px 7px; border-radius: 999px;
}
.assistant-sub { font-size: 12px; color: #9aa5b1; margin-top: 3px; }
.assistant-arrow { color: #c0c8d2; font-size: 20px; }
.unread-banner {  display: flex; align-items: center; gap: 10px;
  background: linear-gradient(135deg, #fff3ee, #ffe8e0);
  border: 1px solid #ffd9cc; border-radius: 10px;
  padding: 10px 14px; margin-bottom: 10px;
  font-size: 14px; color: var(--c2c-primary-dark);
}
.conv-card { margin-bottom: 8px; cursor: pointer; }
.conv-row { display: flex; align-items: center; gap: 12px; }
.conv-avatar-wrap { flex-shrink: 0; cursor: pointer; }
.conv-avatar { background: linear-gradient(135deg, var(--c2c-primary), var(--c2c-primary-light)); font-size: 16px; }
.conv-main { flex: 1; min-width: 0; }
.conv-title { display: flex; justify-content: space-between; align-items: center; }
.conv-name { cursor: pointer; transition: color 0.2s; }
.conv-name:hover { color: var(--c2c-primary); }
.conv-time { color: var(--c2c-text-muted); font-size: 12px; margin-left: 12px; flex-shrink: 0; }
.conv-sub { display: flex; align-items: baseline; gap: 8px; margin-top: 4px; }
.conv-product { color: #909399; font-size: 12px; flex-shrink: 0; }
.conv-preview {
  color: #909399; font-size: 13px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
</style>
