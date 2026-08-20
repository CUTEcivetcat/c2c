<template>
  <div class="chat-page-layout">
    <!-- 顶部栏 -->
    <div class="chat-topbar">
      <button class="back-btn" @click="$router.back()"><el-icon><ArrowLeft /></el-icon></button>
      <div class="topbar-partner" @click="goPartner">
        <el-avatar :size="36" style="background:linear-gradient(135deg,#ff6b35,#ff8c5a);font-size:14px">{{ partnerName?.charAt(0) || '?' }}</el-avatar>
        <strong>{{ partnerName || '聊天' }}</strong>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="msgBox">
      <template v-for="(item, i) in messageItems" :key="item.m.id || 'k' + i">
        <!-- 日期分隔条（今天/昨天/具体日期） -->
        <div v-if="item.showDate" class="date-divider"><span>{{ item.dateLabel }}</span></div>

        <!-- 系统通知（审核结果等），居中灰条展示 -->
        <div v-if="item.m.messageType === 4" class="sys-msg">
          <div class="sys-bubble">{{ item.m.content }}</div>
          <div class="sys-time">{{ item.timeLabel }}</div>
        </div>

        <!-- 普通消息：本人右侧 / 他人左侧，均带头像 -->
        <div v-else :class="['msg-row', item.m.senderId === myId ? 'msg-mine' : 'msg-other']">
          <el-avatar v-if="item.m.senderId !== myId" :size="34" class="msg-avatar"
            style="background:linear-gradient(135deg,#a29bfe,#6c5ce7);font-size:13px;flex-shrink:0">{{ item.avatarText }}</el-avatar>
          <div class="msg-bubble" :class="{ 'msg-mine-bubble': item.m.senderId === myId }">
            <div class="msg-text">{{ item.m.content }}</div>
            <div class="msg-meta">
              <span v-if="item.showTime" class="msg-time">{{ item.timeLabel }}</span>
              <span v-if="item.m.senderId === myId" class="msg-read" :class="{ 'ok': item.m.isRead === 1 }">
                {{ item.m.isRead === 1 ? '已读' : '未读' }}
              </span>
            </div>
          </div>
          <el-avatar v-if="item.m.senderId === myId" :size="34" class="msg-avatar"
            style="background:linear-gradient(135deg,#ff6b35,#ff8c5a);font-size:13px;flex-shrink:0">{{ item.avatarText }}</el-avatar>
        </div>
      </template>
      <div v-if="messages.length === 0" class="chat-empty">
        <el-icon :size="48" color="#dfe6e9"><ChatDotRound /></el-icon>
        <p>发送第一条消息开始聊天吧</p>
      </div>
    </div>

    <!-- 输入栏 -->
    <div class="chat-input-bar">
      <input v-model="text" type="text" placeholder="输入消息…" class="chat-input" @keyup.enter="send" />
      <button class="send-btn" @click="send" :disabled="!text.trim() || sending">
        <el-icon :size="20"><Promotion /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getMessages, sendMessage, markRead, getConversations } from '@/api/im'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const myId = store.userInfo?.id
const messages = ref([])
const text = ref('')
const msgBox = ref(null)
const partnerName = ref('')
const partnerId = ref(null)
const sending = ref(false)
let lastId = 0
let pollTimer = null

onMounted(async () => {
  // 会话里没有昵称字段，先展示"用户+id"（后续可扩展为昵称）
  try {
    const convs = await getConversations()
    const conv = (convs || []).find(c => String(c.id) === String(route.params.id))
    if (conv) {
      const pId = myId === conv.user1Id ? conv.user2Id : conv.user1Id
      partnerId.value = pId
      partnerName.value = '用户' + pId
    }
  } catch (e) { /* 忽略，只影响顶栏展示 */ }
  await load()
  doMarkRead()
  pollTimer = setInterval(pollNew, 4000)
})
onUnmounted(() => clearInterval(pollTimer))

const calcLastId = (list) => list.reduce((m, x) => Math.max(m, Number(x.id) || 0), 0)

const load = async () => {
  try {
    const res = await getMessages(route.params.id, {})
    messages.value = res.records || []
    lastId = calcLastId(messages.value)
    updatePartnerName()
    scrollBottom()
  } catch (e) { /* 拦截器已提示 */ }
}

const pollNew = async () => {
  if (sending.value) return
  try {
    const res = await getMessages(route.params.id, { size: 200 })
    const all = res.records || []
    const latestId = calcLastId(all)
    if (latestId > lastId) {
      const known = new Set(messages.value.map(m => m.id))
      const fresh = all.filter(m => !known.has(m.id))
      messages.value.push(...fresh)
      lastId = latestId
      updatePartnerName()
      scrollBottom()
      doMarkRead() // 收到新消息顺手标记已读，未读角标归零
    }
    // 同步已读状态：对方读了我的消息后 isRead 由 0 变 1
    const byId = {}
    for (const m of all) byId[m.id] = m.isRead
    for (const m of messages.value) {
      if (byId[m.id] !== undefined && Number(m.isRead) !== Number(byId[m.id])) {
        m.isRead = byId[m.id]
      }
    }
  } catch (e) { /* 网络抖动忽略，下一轮再试 */ }
}

const doMarkRead = async () => {
  try { await markRead(route.params.id) } catch (e) { /* */ }
}

const send = async () => {
  const content = text.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    const msg = await sendMessage(route.params.id, content)
    messages.value.push(msg)
    lastId = Math.max(lastId, Number(msg.id) || 0)
    text.value = ''
    nextTick(scrollBottom)
  } catch (e) { /* 拦截器已提示（如内容为空/无权发言） */ } finally {
    sending.value = false
  }
}

const scrollBottom = () => nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight })

// 点击顶栏头像/名称进入对方主页
const goPartner = () => { if (partnerId.value) router.push(`/user/${partnerId.value}`) }

// 从消息里取对方真实昵称（后端已填充 senderName），有则替换"用户X"
const updatePartnerName = () => {
  const fallback = '用户' + (partnerId.value ?? '')
  if (partnerName.value && partnerName.value !== fallback) return
  const m = messages.value.find(x => x.senderId !== myId && x.senderName)
  if (m) partnerName.value = m.senderName
}

// ---- 时间/日期显示优化 ----
const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
const formatDateLabel = (d) => {
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return '今天'
  const y = new Date(now); y.setDate(now.getDate() - 1)
  if (d.toDateString() === y.toDateString()) return '昨天'
  if (d.getFullYear() === now.getFullYear()) return `${d.getMonth() + 1}月${d.getDate()}日`
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

// 渲染元信息：日期分隔 + 时间合并（同发送者且间隔<5分钟不重复显示时间）
const messageItems = computed(() => {
  const items = []
  let prev = null
  for (const m of messages.value) {
    const time = m.createdAt ? new Date(m.createdAt) : null
    let showDate = false, dateLabel = ''
    if (time && (!prev || !prev.time || time.toDateString() !== prev.time.toDateString())) {
      showDate = true
      dateLabel = formatDateLabel(time)
    }
    let showTime = false
    if (time && (!prev || !prev.time || prev.senderId !== m.senderId || (time - prev.time) >= 5 * 60 * 1000)) {
      showTime = true
    }
    const mine = m.senderId === myId
    const avatarText = mine
      ? (store.userInfo?.nickname?.charAt(0) || '我')
      : ((m.senderName || '用户').charAt(0))
    items.push({
      m,
      time,
      showDate,
      dateLabel,
      showTime,
      timeLabel: time ? formatTime(m.createdAt) : '',
      avatarText
    })
    prev = { time, senderId: m.senderId }
  }
  return items
})
</script>

<style scoped>
.chat-page-layout { display: flex; flex-direction: column; height: 100vh; background: #f5f6f8; max-width: 700px; margin: 0 auto; }
.chat-topbar {
  display: flex; align-items: center; gap: 12px; padding: 0 16px;
  height: 56px; background: #fff; border-bottom: 1px solid #f0f2f5;
  position: sticky; top: 0; z-index: 10;
}
.back-btn { border: none; background: none; cursor: pointer; font-size: 20px; color: #636e72; padding: 4px; }
.topbar-partner { display: flex; align-items: center; gap: 10px; cursor: pointer; transition: opacity 0.2s; }
.topbar-partner:hover { opacity: 0.8; }

/* 消息区 */
.chat-messages {
  flex: 1; overflow-y: auto; padding: 16px 12px;
  display: flex; flex-direction: column; gap: 2px;
}
.date-divider { display: flex; justify-content: center; margin: 10px 0 6px; }
.date-divider span { background: #e8eaed; color: #909399; font-size: 11px; padding: 3px 12px; border-radius: 10px; }

.msg-row { display: flex; max-width: 80%; align-items: flex-end; gap: 8px; margin-top: 8px; }
.msg-mine { align-self: flex-end; }
.msg-other { align-self: flex-start; }
.msg-avatar { flex-shrink: 0; margin-bottom: 1px; }

.msg-bubble {
  padding: 9px 14px; border-radius: 16px; font-size: 14px; line-height: 1.55;
  background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.05); word-break: break-word;
}
.msg-other .msg-bubble { border-top-left-radius: 5px; }
.msg-mine .msg-bubble {
  background: linear-gradient(135deg, #ff6b35, #ff8c5a); color: #fff;
  border-top-right-radius: 5px;
}
.msg-text { white-space: pre-wrap; }
.msg-meta { display: flex; align-items: center; justify-content: flex-end; gap: 6px; margin-top: 3px; }
.msg-time { font-size: 10px; opacity: 0.55; }
.msg-read { font-size: 10px; color: #ffd9c4; }
.msg-read.ok { color: rgba(255,255,255,0.6); }
.msg-other .msg-meta { justify-content: flex-start; }

.chat-empty { text-align: center; padding: 60px 20px; color: #b2bec3; display: flex; flex-direction: column; align-items: center; gap: 8px; }

/* 系统通知 */
.sys-msg { display: flex; flex-direction: column; align-items: center; gap: 2px; margin: 12px 0; }
.sys-bubble {
  max-width: 90%; text-align: center;
  background: #eef1f5; color: #636e72;
  border-radius: 12px; padding: 8px 16px;
  font-size: 12px; line-height: 1.6; word-break: break-word;
}
.sys-time { font-size: 10px; color: #b2bec3; }

/* 输入栏 */
.chat-input-bar {
  display: flex; gap: 8px; padding: 12px 16px;
  background: #fff; border-top: 1px solid #f0f2f5;
}
.chat-input {
  flex: 1; border: 2px solid #f0f2f5; border-radius: 24px;
  padding: 10px 18px; font-size: 14px; outline: none;
  transition: border-color 0.2s;
}
.chat-input:focus { border-color: #ff6b35; }
.send-btn {
  width: 44px; height: 44px; border-radius: 50%; border: none;
  background: #ff6b35; color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.send-btn:hover { background: #e55a2b; transform: scale(1.05); }
.send-btn:disabled { background: #dfe6e9; cursor: not-allowed; transform: none; }
</style>
