import request from './request'

// ==================== AI 智能发布 ====================
// 多模态识别商品图片 → 一键回填标题/描述/分类/成色 + 建议价区间
// imageUrl: 首图 URL（必填）；categoryId: 用户已选分类（可选，规则引擎兜底时回填分类名）
export const intelligentPublish = (imageUrl, categoryId) =>
  request.post('/ai/intelligent-publish', { imageUrl, categoryId })

// ==================== AI 智能客服 ====================
// 助手会话内发消息（替代 /im/message）；返回 { messageId, content, source }
export const aiChat = (conversationId, content) => request.post('/ai/chat', { conversationId, content })
// 快捷问题 Top8（聊天页 chips）
export const getFaqSuggestions = () => request.get('/ai/chat/faq-suggestions')
