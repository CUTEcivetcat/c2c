<template>
  <!-- 悬浮入口 -->
  <button v-show="!open" class="chat-widget-fab" @click="openPanel" aria-label="联系客服">
    <el-icon :size="24"><ChatDotRound /></el-icon>
    <span class="fab-tip">客服</span>
  </button>

  <!-- 悬浮面板 -->
  <div v-show="open" class="chat-widget-panel">
    <!-- 头部 -->
    <div class="cw-header">
      <div class="cw-partner">
        <el-avatar :size="34" class="cw-avatar">🤖</el-avatar>
        <div class="cw-partner-info">
          <strong>青禾助手</strong>
          <span class="cw-ai-tag">AI</span>
          <div class="cw-status">{{ humanMode ? '人工服务入口' : '在线 · 7×24 智能解答' }}</div>
        </div>
      </div>
      <div class="cw-actions">
        <button v-if="!humanMode" class="cw-link-btn" @click="enterHuman">转人工</button>
        <button v-else class="cw-link-btn" @click="humanMode = false">返回 AI</button>
        <button class="cw-close" @click="closePanel" aria-label="关闭">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </div>

    <!-- AI 对话区 -->
    <template v-if="!humanMode">
      <div ref="msgBox" class="cw-messages">
        <div v-if="loading" class="cw-empty">加载中…</div>
        <template v-for="(m, i) in messages" :key="m.id || 'k' + i">
          <!-- 系统灰条（AI 回复 messageType=4） -->
          <div v-if="m.messageType === 4" class="cw-sys-msg">
            <div class="cw-sys-bubble">{{ m.content }}</div>
          </div>
          <!-- 普通消息：本人右 / 助手左 -->
          <div v-else :class="['cw-msg-row', m.senderId === myId ? 'cw-mine' : 'cw-other']">
            <div v-if="m.senderId !== myId" class="cw-bubble">{{ m.content }}</div>
            <div v-if="m.senderId === myId" class="cw-bubble cw-mine-bubble">{{ m.content }}</div>
          </div>
        </template>
        <div v-if="!loading && messages.length === 0" class="cw-empty">
          <el-icon :size="34" color="var(--c2c-border)"><ChatDotRound /></el-icon>
          <p>你好，请问有什么可以帮你？</p>
        </div>
      </div>

      <!-- 常用语句 chips（输入框上方） -->
      <div v-if="faqChips.length" class="cw-chips">
        <span v-for="q in faqChips" :key="q" class="cw-chip" @click="sendChip(q)">{{ q }}</span>
      </div>

      <!-- 输入栏 -->
      <div class="cw-input-bar">
        <input v-model="text" type="text" placeholder="输入消息…" class="cw-input"
               @keyup.enter="send" />
        <button class="cw-send" @click="send" :disabled="!text.trim() || sending">
          <el-icon :size="18"><Promotion /></el-icon>
        </button>
      </div>
    </template>

    <!-- 转人工指引面板 -->
    <div v-else class="cw-human">
      <div class="cw-human-icon">👤</div>
      <p class="cw-human-tip">如需人工处理（投诉 / 纠纷 / 违规申诉），可通过以下渠道提交，管理员会在审核工作台跟进：</p>
      <div class="cw-human-links">
        <button class="cw-human-link" @click="goExternal('/my/appeal')">📋 我的整改申诉</button>
        <button class="cw-human-link" @click="goExternal('/my/report')">🚩 我的举报</button>
        <button class="cw-human-link" @click="goExternal('/announcement')">📢 平台公告</button>
      </div>
      <p class="cw-human-note">在线人工排队功能暂未开放，紧急问题请优先提交申诉或举报。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getOrCreateConversation, getMessages, markRead } from '@/api/im'
import { aiChat, getFaqSuggestions } from '@/api/ai'

/** 平台助手账号（与后端 AiChatService.ASSISTANT_ID 一致） */
const ASSISTANT_ID = 90000001

/** 命中即自动转接人工的关键词 */
const HANDOFF_KEYWORDS = ['转人工', '人工客服', '找人工', '联系人工', '人工处理', '人工服务', '真人', '投诉', '纠纷']

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const open = ref(false)
const loading = ref(false)
const sending = ref(false)
const humanMode = ref(false)
const convId = ref(null)
const messages = ref([])
const faqChips = ref([])
const text = ref('')
const msgBox = ref(null)
let pollTimer = null

const myId = computed(() => store.userInfo?.id ?? null)

const openPanel = async () => {
  if (!store.isLoggedIn()) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  open.value = true
  humanMode.value = false
  await ensureConversation()
}

const closePanel = () => {
  open.value = false
  humanMode.value = false
  clearTimer()
}

/** 获取/创建与助手的会话，再加载消息与常用语句 */
const ensureConversation = async () => {
  try {
    if (!convId.value) {
      const conv = await getOrCreateConversation({ targetUserId: ASSISTANT_ID })
      convId.value = conv?.id
    }
    await Promise.all([loadMessages(), loadChips()])
    startPolling()
  } catch (e) { /* 拦截器已提示 */ }
}

const loadMessages = async () => {
  if (!convId.value) return
  loading.value = true
  try {
    const res = await getMessages(convId.value, { size: 200 })
    messages.value = res.records || []
    scrollBottom()
    markRead(convId.value).catch(() => {})
  } catch (e) { /* */ } finally {
    loading.value = false
  }
}

const loadChips = async () => {
  try { faqChips.value = await getFaqSuggestions() || [] } catch (e) { faqChips.value = [] }
}

const send = async () => {
  const content = text.value.trim()
  if (!content || sending.value) return
  // 命中转人工关键词：不发给 AI，直接切人工指引面板
  if (isHandoff(content)) {
    text.value = ''
    humanMode.value = true
    return
  }
  if (!convId.value) return
  sending.value = true
  try {
    await aiChat(convId.value, content)
    text.value = ''
    await loadMessages()
  } catch (e) { /* 拦截器已提示 */ } finally {
    sending.value = false
  }
}

/** 是否为转人工诉求 */
const isHandoff = (content) => HANDOFF_KEYWORDS.some(k => content.includes(k))

const sendChip = (q) => { text.value = q; send() }

const enterHuman = () => { humanMode.value = true }

const goExternal = (path) => {
  closePanel()
  router.push(path)
}

const startPolling = () => {
  clearTimer()
  pollTimer = setInterval(() => { if (!sending.value) loadMessages() }, 4000)
}
const clearTimer = () => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

const scrollBottom = () => nextTick(() => {
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
})

onUnmounted(clearTimer)
</script>

<style scoped>
/* 悬浮按钮 */
.chat-widget-fab {
  position: fixed; right: 24px; bottom: 92px; z-index: 1000;
  width: 52px; height: 52px; border-radius: 50%; border: none; cursor: pointer;
  background: linear-gradient(135deg, var(--c2c-primary), var(--c2c-primary-light));
  color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center;
  box-shadow: 0 6px 18px rgba(255, 107, 53, 0.35); transition: all 0.25s ease;
}
.chat-widget-fab:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(255, 107, 53, 0.45); }
.fab-tip { font-size: 10px; margin-top: 1px; }

/* 面板 */
.chat-widget-panel {
  position: fixed; right: 24px; bottom: 92px; z-index: 1000;
  width: 360px; height: 500px; max-height: calc(100vh - 140px);
  background: #fff; border-radius: 18px; overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  display: flex; flex-direction: column;
}

.cw-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; background: linear-gradient(135deg, var(--c2c-primary), var(--c2c-primary-light));
  color: #fff;
}
.cw-partner { display: flex; align-items: center; gap: 10px; }
.cw-avatar { background: rgba(255, 255, 255, 0.25); }
.cw-partner-info { display: flex; flex-direction: column; }
.cw-partner-info strong { font-size: 15px; }
.cw-ai-tag {
  display: inline-block; font-size: 10px; font-weight: 700; background: rgba(255, 255, 255, 0.28);
  padding: 0 7px; border-radius: 999px; margin-left: 6px;
}
.cw-status { font-size: 11px; opacity: 0.85; margin-top: 2px; }
.cw-actions { display: flex; align-items: center; gap: 10px; }
.cw-link-btn {
  background: rgba(255, 255, 255, 0.18); border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff; font-size: 12px; padding: 5px 10px; border-radius: 999px; cursor: pointer;
}
.cw-link-btn:hover { background: rgba(255, 255, 255, 0.28); }
.cw-close {
  background: none; border: none; color: #fff; cursor: pointer; font-size: 18px;
  display: flex; align-items: center;
}

/* 消息区 */
.cw-messages { flex: 1; overflow-y: auto; padding: 14px 12px; display: flex; flex-direction: column; gap: 8px; background: var(--c2c-bg); }
.cw-empty { text-align: center; color: var(--c2c-text-muted); margin: auto; display: flex; flex-direction: column; align-items: center; gap: 6px; font-size: 13px; }

.cw-msg-row { display: flex; max-width: 80%; }
.cw-mine { align-self: flex-end; }
.cw-other { align-self: flex-start; }
.cw-bubble {
  padding: 8px 12px; border-radius: 14px; font-size: 13px; line-height: 1.5;
  background: #fff; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05); word-break: break-word; white-space: pre-wrap;
}
.cw-other .cw-bubble { border-top-left-radius: 5px; }
.cw-mine-bubble {
  background: linear-gradient(135deg, var(--c2c-primary), var(--c2c-primary-light)); color: #fff;
  border-top-right-radius: 5px;
}

.cw-sys-msg { display: flex; justify-content: center; }
.cw-sys-bubble {
  max-width: 90%; background: #eef1f5; color: var(--c2c-text-secondary);
  border-radius: 12px; padding: 7px 12px; font-size: 12px; line-height: 1.6; word-break: break-word;
}

/* chips */
.cw-chips { display: flex; gap: 8px; overflow-x: auto; padding: 8px 12px 4px; scrollbar-width: none; }
.cw-chips::-webkit-scrollbar { display: none; }
.cw-chip {
  flex-shrink: 0; padding: 6px 12px; font-size: 12px; background: #fff;
  border: 1px solid var(--c2c-line-soft); border-radius: 999px; color: var(--c2c-text-secondary);
  cursor: pointer; user-select: none; transition: all 0.2s;
}
.cw-chip:hover { border-color: var(--c2c-primary); color: var(--c2c-primary); background: #fff5f0; }

/* 输入栏 */
.cw-input-bar { display: flex; gap: 8px; padding: 10px 12px; background: #fff; border-top: 1px solid var(--c2c-line-soft); }
.cw-input {
  flex: 1; border: 2px solid var(--c2c-line-soft); border-radius: 22px; padding: 9px 16px;
  font-size: 14px; outline: none; transition: border-color 0.2s;
}
.cw-input:focus { border-color: var(--c2c-primary); }
.cw-send {
  width: 40px; height: 40px; border-radius: 50%; border: none; background: var(--c2c-primary);
  color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.cw-send:hover { background: var(--c2c-primary-dark); }
.cw-send:disabled { background: var(--c2c-border); cursor: not-allowed; }

/* 转人工面板 */
.cw-human { flex: 1; overflow-y: auto; padding: 20px 16px; display: flex; flex-direction: column; gap: 14px; background: var(--c2c-bg); }
.cw-human-icon { font-size: 34px; text-align: center; }
.cw-human-tip { font-size: 14px; line-height: 1.8; color: var(--c2c-text-secondary); text-align: center; }
.cw-human-links { display: flex; flex-direction: column; gap: 10px; }
.cw-human-link {
  padding: 13px 16px; border: 1px solid var(--c2c-line-soft); background: #fff; border-radius: 14px;
  font-size: 14px; text-align: left; cursor: pointer; transition: all 0.2s;
}
.cw-human-link:hover { border-color: var(--c2c-primary); color: var(--c2c-primary); }
.cw-human-note { font-size: 12px; color: var(--c2c-text-muted); text-align: center; }

@media (max-width: 480px) {
  .chat-widget-panel { right: 12px; left: 12px; width: auto; bottom: 84px; height: calc(100vh - 130px); }
  .chat-widget-fab { right: 16px; bottom: 80px; }
}
</style>
