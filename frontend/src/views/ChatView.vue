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
      <div v-for="(m, i) in messages" :key="m.id || i">
        <!-- 系统通知（审核结果等），居中灰条展示 -->
        <div v-if="m.messageType === 4" class="sys-msg">
          <div class="sys-bubble">{{ m.content }}</div>
          <div class="sys-time">{{ formatTime(m.createdAt) }}</div>
        </div>
        <div v-else :class="['msg-row', m.senderId === myId ? 'msg-mine' : 'msg-other']">
          <div class="msg-bubble" :class="{ 'msg-mine-bubble': m.senderId === myId }">
            <div class="msg-text">{{ m.content }}</div>
            <div class="msg-meta">
              <span class="msg-time">{{ formatTime(m.createdAt) }}</span>
              <span v-if="m.senderId === myId" class="msg-read" :class="{ 'ok': m.isRead === 1 }">
                {{ m.isRead === 1 ? '已读' : '未读' }}
              </span>
            </div>
          </div>
        </div>
      </div>
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
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

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.chat-page-layout { display: flex; flex-direction: column; height: 100vh; background: #f8f9fa; max-width: 700px; margin: 0 auto; }
.chat-topbar {
  display: flex; align-items: center; gap: 12px; padding: 0 16px;
  height: 56px; background: #fff; border-bottom: 1px solid #f0f2f5;
  position: sticky; top: 0; z-index: 10;
}
.back-btn { border: none; background: none; cursor: pointer; font-size: 20px; color: #636e72; padding: 4px; }
.topbar-partner { display: flex; align-items: center; gap: 10px; cursor: pointer; transition: opacity 0.2s; }
.topbar-partner:hover { opacity: 0.8; }
.chat-messages {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 8px;
}
.msg-row { display: flex; max-width: 80%; }
.msg-mine { align-self: flex-end; }
.msg-other { align-self: flex-start; }
.msg-bubble {
  padding: 10px 16px; border-radius: 18px; font-size: 14px; line-height: 1.5;
  background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.msg-mine-bubble { background: linear-gradient(135deg, #ff6b35, #ff8c5a); color: #fff; }
.msg-text { word-break: break-word; }
.msg-meta { display: flex; align-items: center; justify-content: flex-end; gap: 6px; margin-top: 4px; }
.msg-time { font-size: 10px; opacity: 0.6; text-align: right; }
.msg-read { font-size: 10px; color: #ffd9c4; }
.msg-read.ok { color: rgba(255,255,255,0.65); }
.chat-empty { text-align: center; padding: 60px 20px; color: #b2bec3; display: flex; flex-direction: column; align-items: center; gap: 8px; }
/* 系统通知 */
.sys-msg { display: flex; flex-direction: column; align-items: center; gap: 2px; margin: 10px 0; }
.sys-bubble {
  max-width: 90%; text-align: center;
  background: #eef1f5; color: #636e72;
  border-radius: 12px; padding: 8px 16px;
  font-size: 12px; line-height: 1.6; word-break: break-word;
}
.sys-time { font-size: 10px; color: #b2bec3; }
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
@media (max-width: 480px) { .chat-input { font-size: 14px; } .msg-bubble { max-width: 85%; font-size: 13px; } }
