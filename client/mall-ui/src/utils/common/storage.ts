export function getItem<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    return null
  }
}

export function setItem(key: string, value: unknown): void {
  localStorage.setItem(key, JSON.stringify(value))
}

export function removeItem(key: string): void {
  localStorage.removeItem(key)
}
