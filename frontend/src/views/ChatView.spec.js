import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import ChatView from './ChatView.vue'
import { useUserStore } from '@/stores/userStore'
import { getMessages, getConversations, markRead } from '@/api/im'
import { getProfile } from '@/api/user'

// ---- 外部依赖全部 mock ----
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' } }),
  useRouter: () => ({ push: vi.fn(), back: vi.fn() })
}))
vi.mock('@/api/im', () => ({
  getMessages: vi.fn(),
  sendMessage: vi.fn(),
  markRead: vi.fn(),
  getConversations: vi.fn()
}))
vi.mock('@/api/user', () => ({ getProfile: vi.fn() }))

// jsdom 无 scrollTo
Element.prototype.scrollTo = vi.fn()

const MINE_ID = 2
const CONV_ID = 7

function makeMsg(id, senderId, content, extras = {}) {
  return {
    id,
    conversationId: CONV_ID,
    senderId,
    content,
    messageType: extras.messageType ?? 1,
    isRead: extras.isRead ?? 1,
    createdAt: extras.createdAt || '2026-11-02T10:00:00',
    senderName: extras.senderName || null
  }
}

function mountChat() {
  return mount(ChatView, {
    global: {
      stubs: { 'el-avatar': true, 'el-icon': true }
    }
  })
}

async function stubConversation() {
  getConversations.mockResolvedValue([
    { id: CONV_ID, user1Id: 1, user2Id: MINE_ID }
  ])
}

describe('ChatView 消息方向', () => {
  let wrapper

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    wrapper = null
  })

  it('userInfo 已加载：我的消息在右(msg-mine)，对方在左(msg-other)', async () => {
    useUserStore().userInfo = { id: MINE_ID, nickname: '我' }
    await stubConversation()
    getMessages.mockResolvedValue({
      records: [
        makeMsg(1, 1, '你好（对方）'),
        makeMsg(2, MINE_ID, '你好（我）'),
        makeMsg(3, 1, '在吗（对方）')
      ]
    })

    wrapper = mountChat()
    await flushPromises()

    const mine = wrapper.findAll('.msg-mine')
    const other = wrapper.findAll('.msg-other')
    expect(mine.length).toBe(1)
    expect(mine[0].text()).toContain('你好（我）')
    expect(other.length).toBe(2)
  })

  it('回归：userInfo 异步晚到，方向也要从"全左"纠正为"左右分开"', async () => {
    // 模拟进页面时 userInfo 未加载，getProfile 延迟返回
    getProfile.mockImplementation(() =>
      new Promise(resolve => setTimeout(() => resolve({ id: MINE_ID, nickname: '我' }), 20))
    )
    await stubConversation()
    getMessages.mockResolvedValue({
      records: [makeMsg(1, 1, '对方消息'), makeMsg(2, MINE_ID, '我的消息')]
    })

    wrapper = mountChat()
    await flushPromises()

    // userInfo 未到前：我的消息被误判到左侧（旧 bug 现象）
    expect(wrapper.findAll('.msg-mine').length).toBe(0)

    // 等待 fetchProfile 返回 + computed 刷新
    await new Promise(r => setTimeout(r, 40))
    await flushPromises()

    const mine = wrapper.findAll('.msg-mine')
    const other = wrapper.findAll('.msg-other')
    expect(mine.length).toBe(1)
    expect(mine[0].text()).toContain('我的消息')
    expect(other.length).toBe(1)
    expect(other[0].text()).toContain('对方消息')
  })

  it('系统通知（messageType=4）渲染为居中灰条，不参与左右分边', async () => {
    useUserStore().userInfo = { id: MINE_ID, nickname: '我' }
    await stubConversation()
    getMessages.mockResolvedValue({
      records: [
        makeMsg(1, 1, '订单已支付，等待卖家发货 #20261102001', { messageType: 4 }),
        makeMsg(2, 1, '你好（对方普通消息）')
      ]
    })

    wrapper = mountChat()
    await flushPromises()

    const sys = wrapper.findAll('.sys-msg')
    expect(sys.length).toBe(1)
    expect(sys[0].text()).toContain('订单已支付')
    // 灰条不计入左右消息行
    expect(wrapper.findAll('.msg-mine').length + wrapper.findAll('.msg-other').length).toBe(1)
  })
})
