import request from './request'

export const getAnnouncementList = (params) => request.get('/announcement/list', { params })
export const getAnnouncementLatest = (limit = 3) => request.get('/announcement/latest', { params: { limit } })
