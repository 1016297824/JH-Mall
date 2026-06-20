// EdgeOne Edge Functions — API 代理模板
//
// 部署前操作：
//   1. 复制本目录为 edge-functions：cp -r edge-functions.example edge-functions
//   2. 修改下方 BACKEND_URL 为实际 API 域名
//
// 注意：edge-functions/ 已被 .gitignore 忽略，不入库（避免暴露 API 地址）

const BACKEND_URL = 'https://<your-api-domain>'; // ← 部署前替换为实际 API 域名
const FETCH_TIMEOUT_MS = 15000;

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, token, X-Requested-With',
  'Access-Control-Max-Age': '86400',
};

const SKIP_HEADERS = ['host', 'connection', 'content-length'];
const PASS_THROUGH_HEADER_PREFIXES = ['token', 'role', 'username'];
const BODY_METHODS = new Set(['POST', 'PUT', 'PATCH']);

function corsResponse(body, status = 200) {
  return new Response(typeof body === 'string' ? body : JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, 'Content-Type': 'application/json; charset=utf-8' },
  });
}

function buildForwardHeaders(request) {
  const forwardHeaders = new Headers();
  try {
    for (const [key, value] of request.headers) {
      if (!SKIP_HEADERS.includes(key.toLowerCase())) {
        forwardHeaders.set(key, value);
      }
    }
  } catch (e) {
    console.error('[API Proxy] Header copy error:', e.message);
  }
  return forwardHeaders;
}

function getRequestBody(request) {
  if (!BODY_METHODS.has(request.method)) {
    return undefined;
  }
  try {
    return request.body || undefined;
  } catch (e) {
    console.error('[API Proxy] Body read error:', e.message);
    return undefined;
  }
}

async function fetchWithTimeout(url, options, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...options, signal: controller.signal });
    clearTimeout(timer);
    return response;
  } catch (error) {
    clearTimeout(timer);
    if (error.name === 'AbortError') {
      throw new Error(`Backend request timed out after ${timeoutMs}ms`);
    }
    throw error;
  }
}

async function safeReadResponse(response) {
  try {
    const text = await response.text();
    return text || '';
  } catch (e) {
    console.error('[API Proxy] Response read error:', e.message);
    return '';
  }
}

function buildResponseHeaders(response) {
  const responseHeaders = {
    ...CORS_HEADERS,
    'Content-Type': 'application/json; charset=utf-8',
    'X-Proxy-By': 'EdgeOne-Functions-v2',
  };
  try {
    const contentType = response.headers.get('Content-Type');
    if (contentType) {
      responseHeaders['Content-Type'] = contentType;
    }
    for (const [key, value] of response.headers) {
      const lowerKey = key.toLowerCase();
      if (PASS_THROUGH_HEADER_PREFIXES.some(prefix => lowerKey.startsWith(prefix))) {
        responseHeaders[key] = value;
      }
    }
  } catch (e) {
    console.error('[API Proxy] Response header error:', e.message);
  }
  return responseHeaders;
}

export async function onRequest(context) {
  const { request } = context;

  let urlObj;
  try {
    urlObj = new URL(request.url);
  } catch (e) {
    return corsResponse({ error: 'Invalid request URL' }, 400);
  }

  const path = urlObj.pathname || '';
  const search = urlObj.search || '';
  const targetUrl = `${BACKEND_URL}${path}${search}`;

  console.log(`[API Proxy] ${request.method} ${path} -> ${targetUrl}`);

  if (request.method === 'OPTIONS') {
    return corsResponse(null, 204);
  }

  let response;
  try {
    const forwardHeaders = buildForwardHeaders(request);
    const body = getRequestBody(request);

    response = await fetchWithTimeout(targetUrl, {
      method: request.method,
      headers: forwardHeaders,
      body: body,
      redirect: 'follow',
    }, FETCH_TIMEOUT_MS);

    const responseData = await safeReadResponse(response);
    const responseHeaders = buildResponseHeaders(response);

    return new Response(responseData, {
      status: response.status,
      statusText: response.statusText || '',
      headers: responseHeaders,
    });
  } catch (error) {
    console.error('[API Proxy] Error:', error.message, { targetUrl, method: request.method });

    const statusCode = error.message && error.message.includes('timed out') ? 504 : 502;
    return corsResponse({
      error: 'Backend connection failed',
      message: error.message || 'Unknown error',
    }, statusCode);
  }
}
