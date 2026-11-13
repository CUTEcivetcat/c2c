import request from './request'

// ==================== 意见建议反馈（用户端，需登录） ====================
// 提交建议（body: content 必填, contact 联系方式可空）
export const createSuggestion = (data) => request.post('/suggestion', data)
// 我的建议列表（含管理回复与采纳状态）
export const getMySuggestions = (params) => request.get('/suggestion/my', { params })
