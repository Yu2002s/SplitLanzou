export interface SplitFile {
  name: string
  length: number
  files: FilePart[]
}

export interface FilePart {
  start: number
  end: number
  index: number
  length: number
  fileId: number
  url: string
  pwd?: string
}

export interface IDownloadStatus {
  progress: number
}

export interface Download {
  directLink: string;
  fileName: string;
  fileLength: number;
  isSplitFile: boolean
}

/**
 * 获取分享文件夹请求参数
 */
export interface GetShareFolderParams {
  lx?: number
  fid: number
  uid: string
  pg: number
  rep?: string
  t: string
  k: string
  up?: number
  ls?: number
  pwd: string
}

/**
 * 蓝奏文件结构
 */
export interface LanzouFile {
  id: string
  name_all: string
  icon: string
  size: string
  time: string
  infos: string
}

export interface LanzouParseFile {
  fileName: string
  fileId: string
  size: number
  sizeStr: string
  fileType: string
  createTime: string
}