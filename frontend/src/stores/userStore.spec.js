import { createPinia, setActivePinia } from 'pinia'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useUserStore } from './userStore'

// 外部登录/资料/登出接口全部 mock，单测只验证弹窗回调状态机
vi.mock('@/api/user', () => ({
  login: vi.fn(),
  getProfile: vi.fn(),
  logout: vi.fn()
}))

describe('登录弹窗回调状态机', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('closeLoginDialog 只关弹窗，不清登录后回调；runAfterLogin 执行并只执行一次', () => {
    const store = useUserStore()
    const cb = vi.fn()
    store.openLoginDialog(cb)
    expect(store.loginDialogVisible).toBe(true)

    // 模拟登录态 watch 抢先关窗：回调不应被清掉
    store.closeLoginDialog()
    expect(store.loginDialogVisible).toBe(false)

    store.runAfterLogin()
    expect(cb).toHaveBeenCalledTimes(1)
    // 回调消费后清空，再次触发不再执行
    store.runAfterLogin()
    expect(cb).toHaveBeenCalledTimes(1)
  })

  it('手动取消（clearLoginCallback）后，登录后回调不再执行', () => {
    const store = useUserStore()
    const cb = vi.fn()
    store.openLoginDialog(cb)
    store.closeLoginDialog()
    store.clearLoginCallback()

    store.runAfterLogin()
    expect(cb).not.toHaveBeenCalled()
  })

  it('未登记回调时 runAfterLogin 不报错', () => {
    const store = useUserStore()
    store.openLoginDialog()
    expect(() => store.runAfterLogin()).not.toThrow()
  })
})
