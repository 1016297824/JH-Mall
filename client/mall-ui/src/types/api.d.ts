export interface MallResult<T> {
  errorCode: string
  errorMessage: string
  data: T
}

export interface PageResult<T> {
  page: number
  size: number
  total: number
  rows: T[]
}
