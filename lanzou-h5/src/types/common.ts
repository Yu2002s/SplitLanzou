export interface ParserResponse<T> {
  code: number
  success: boolean
  msg: string
  data: T
}