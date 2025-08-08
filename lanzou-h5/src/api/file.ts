import type { ParserResponse } from "@/types/common";
import type { Download, LanzouParseFile } from "@/types/file";
import { request } from "@/utils/request";
const LANZOU_URL = import.meta.env.VITE_LANZOU_URL;

export function getFile(id: string, pwd?: string) {
  return request<{ url: string; pwd?: string }, ParserResponse<Download>>(
    "/api/json/parser",
    "GET",
    {
      url: `${LANZOU_URL}/${id}`,
      pwd: pwd || "",
    }
  );
}

/**
 * 获取文件信息
 *
 * @param url 文件下载地址
 * @returns 文件信息
 */
export function getDownloadFile(url: string) {
  return request<{ url: string }, Download>("/get-file", "GET", { url });
}

/**
 * 获取分享文件夹页面的响应 html
 *
 * @param id 文件 id
 * @returns html 数据
 */
export function getShareFolderPageHtml(id: string) {
  return request<string>(`/${id}`);
}

/**
 * 获取分享文件夹列表数据
 *
 * @param data 请求所需的参数
 * @returns 文件夹列表
 */
export function getShareFolderList(id: string, pwd?: string) {
  return request<{ url: string; pwd?: string }, ParserResponse<LanzouParseFile[]>>(
    `/api/v2/getFileList`,
    "GET",
    { url: `${LANZOU_URL}/${id}`, pwd: pwd || "" }
  );
}
