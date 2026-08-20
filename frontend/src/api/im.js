import request from './request'

export const getConversations = () => request.get('/im/conversation/list')
export const getOrCreateConversation = (data) => request.post('/im/conversation', data)
export const getMessages = (conversationId, params) => request.get(`/im/message/${conversationId}`, { params })
export const sendMessage = (conversationId, content) => request.post('/im/message', { conversationId, content })
export const markRead = (conversationId) => request.put(`/im/message/${conversationId}/read`)
export const getUnreadCount = () => request.get('/im/unread/count')
export const getUnreadLatest = () => request.get('/im/unread/latest')
