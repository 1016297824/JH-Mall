import request from './client'
import type { UserProfile } from '@/types'

/** 查询当前登录用户资料 */
export function getProfile(): Promise<UserProfile> {
  return request.get('/user/profile').then((res) => res.data.data)
}
