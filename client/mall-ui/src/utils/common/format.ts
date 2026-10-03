export function formatPrice(priceInCents: number): string {
  return (priceInCents / 100).toFixed(2)
}

export function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/**
 * 格式化为「年-月-日 时:分」
 *
 * @param dateStr 后端返回的时间字符串
 * @returns 格式化结果；无法解析时原样返回，避免展示 Invalid Date
 */
export function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) {
    return dateStr
  }
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${formatDate(dateStr)} ${hh}:${mm}`
}
