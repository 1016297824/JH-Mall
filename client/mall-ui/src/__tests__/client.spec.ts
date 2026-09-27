import axios, { AxiosError, type AxiosAdapter } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import request from '@/api/client'

// 只 mock axios 的 post（刷新令牌用），保留 create 等真实实现
vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>()
  return {
    ...actual,
    default: { ...actual.default, post: vi.fn<(url: string, body?: unknown) => Promise<unknown>>() },
  }
})

/** 构造固定响应体的适配器 */
function dataAdapter(body: unknown, status = 200): AxiosAdapter {
  return async (config) => ({
    data: body,
    status,
    statusText: 'OK',
    headers: {},
    config,
  })
}

describe('api/client 响应拦截', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(axios.post).mockReset()
  })

  it('业务成功时返回原始 response', async () => {
    request.defaults.adapter = dataAdapter({
      errorCode: '00000',
      errorMessage: '操作成功',
      data: { id: 1 },
    })

    const response = await request.get('/demo')

    expect(response.data.data).toEqual({ id: 1 })
  })

  it('业务失败时优先展示 userTip（面向用户的文案）', async () => {
    request.defaults.adapter = dataAdapter({
      errorCode: 'A0210',
      errorMessage: '密码不匹配',
      userTip: '密码错误',
    })

    await expect(request.get('/demo')).rejects.toThrow('密码错误')
  })

  it('HTTP 400 + MallResult 时展示 userTip，而非 axios 的英文文案', async () => {
    request.defaults.adapter = async (config) => {
      const response = {
        data: { errorCode: 'A0210', errorMessage: '密码不匹配', userTip: '密码错误' },
        status: 400,
        statusText: 'Bad Request',
        headers: {},
        config,
      }
      throw new AxiosError(
        'Request failed with status code 400',
        'ERR_BAD_REQUEST',
        config,
        null,
        response,
      )
    }

    await expect(request.get('/demo')).rejects.toThrow('密码错误')
  })

  it('网关以 HTTP 200 + code=401 返回时提示缺少登录态', async () => {
    request.defaults.adapter = dataAdapter({ code: 401, msg: 'C 端请求缺少 token' })

    await expect(request.get('/demo')).rejects.toThrow('C 端请求缺少 token')
  })

  it('网关非 401 错误体不能被当作成功（否则调用方静默渲染空数据）', async () => {
    request.defaults.adapter = dataAdapter({ code: 500, msg: '服务未找到' })

    await expect(request.get('/demo')).rejects.toThrow('服务未找到')
  })

  it('已重试过仍返回未授权时不再刷新，避免无限重放', async () => {
    localStorage.setItem('accessToken', 'access-token-abc')
    localStorage.setItem('refreshToken', 'refresh-token-xyz')
    request.defaults.adapter = async (config) => {
      ;(config as { _retry?: boolean })._retry = true
      return {
        data: { code: 401, msg: 'C 端 token 无效' },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }
    }

    await expect(request.get('/demo')).rejects.toThrow('C 端 token 无效')
  })

  it('未授权时用 refreshToken 刷新并带上新令牌重放原请求', async () => {
    localStorage.setItem('accessToken', 'old-access')
    localStorage.setItem('refreshToken', 'old-refresh')

    const authHeaders: string[] = []
    let attempt = 0
    request.defaults.adapter = async (config) => {
      authHeaders.push(String(config.headers?.Authorization ?? ''))
      attempt += 1
      return attempt === 1
        ? { data: { code: 401, msg: 'token 过期' }, status: 200, statusText: 'OK', headers: {}, config }
        : {
            data: { errorCode: '00000', data: { ok: true } },
            status: 200,
            statusText: 'OK',
            headers: {},
            config,
          }
    }
    vi.mocked(axios.post).mockResolvedValue({
      data: { data: { accessToken: 'new-access', refreshToken: 'new-refresh' } },
    } as never)

    const response = await request.get('/demo')

    expect(response.data.data).toEqual({ ok: true })
    expect(localStorage.getItem('accessToken')).toBe('new-access')
    expect(authHeaders[1]).toBe('Bearer new-access')
  })

  it('并发未授权时只发起一次刷新（refreshToken 一次性轮换）', async () => {
    localStorage.setItem('accessToken', 'old-access')
    localStorage.setItem('refreshToken', 'old-refresh')

    let refreshCalls = 0
    const attempts = new Map<string, number>()
    request.defaults.adapter = async (config) => {
      const url = String(config.url)
      const count = (attempts.get(url) ?? 0) + 1
      attempts.set(url, count)
      return count === 1
        ? { data: { code: 401, msg: 'token 过期' }, status: 200, statusText: 'OK', headers: {}, config }
        : {
            data: { errorCode: '00000', data: { url } },
            status: 200,
            statusText: 'OK',
            headers: {},
            config,
          }
    }
    vi.mocked(axios.post).mockImplementation((() => {
      refreshCalls += 1
      return Promise.resolve({
        data: { data: { accessToken: 'new-access', refreshToken: 'new-refresh' } },
      })
    }) as never)

    await Promise.all([request.get('/a'), request.get('/b')])

    expect(refreshCalls).toBe(1)
  })

  it('刷新同步失败后，下一次未授权仍会重新发起刷新（不缓存失败结果）', async () => {
    // 触发条件：本地没有 refreshToken，刷新在同步阶段即失败——原缺陷会把该失败 Promise 永久缓存
    localStorage.setItem('accessToken', 'old-access')
    localStorage.removeItem('refreshToken')
    request.defaults.adapter = async (config) => ({
      data: { code: 401, msg: 'token 过期' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    })

    await expect(request.get('/first')).rejects.toThrow('token 过期')
    expect(vi.mocked(axios.post)).not.toHaveBeenCalled()

    // 用户重新登录后再遇 401，必须能真正发起刷新
    localStorage.setItem('accessToken', 'old-access')
    localStorage.setItem('refreshToken', 'old-refresh')
    vi.mocked(axios.post).mockResolvedValueOnce({
      data: { data: { accessToken: 'new-access', refreshToken: 'new-refresh' } },
    } as never)

    await expect(request.get('/second')).rejects.toThrow('token 过期')
    expect(vi.mocked(axios.post)).toHaveBeenCalledTimes(1)
  })
})
