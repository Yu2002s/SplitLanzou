import { type LoaderFunctionArgs } from "react-router";
import { getDownloadFile, getFile, getShareFolderList } from "@/api/file";
import type { Download } from "@/types/file";
import { formatByte, getSplitFileNameOrLength } from "@/utils/file";

export interface IFileLoaderData {
  success: boolean;
  download?: Download;
  message?: string;
}

/**
 * 为 FileSharePage 提供数据
 * @param params 路由参数
 * @returns 数据
 */
export async function fileLoader({
  params,
}: LoaderFunctionArgs): Promise<IFileLoaderData> {
  if (!params.id) {
    return {
      success: true,
    }
  }
  try {
    const data = await getFile(params.id!, params.pwd)

    if (!data.success) {
      throw new Error("获取下载地址失败");
    }

    const directLink = data.data.directLink;

    let fileName = "";
    const splits = directLink.split("?");
    if (splits.length > 1) {
      const searchParams = new URLSearchParams(splits[1]);
      fileName = searchParams.get("fn") || searchParams.get("fileName") || "";
    }

    const result = await getDownloadFile(data.data.directLink);

    if (!result.fileName) {
      result.fileName = fileName;
    }

    data.data = {
      ...data.data,
      ...result,
    };
    return {
      success: true,
      download: data.data,
    };
  } catch (e) {
    return {
      success: false,
      message: (e as Error).message,
    };
  }
}

/**
 * 文件夹页面所需的数据
 *
 * @param param 包含 id、pwd
 * @returns 所需数据
 */
export async function folderLoader({ params }: LoaderFunctionArgs) {
  if (!params.id) {
    return {
      success: false,
    }
  }
  const result = await getShareFolderList(params.id!, params.pwd)

  if (result.success) {
    result.data.forEach(item => {
      const {fileName, fileLength} = getSplitFileNameOrLength(item.fileName)
      if (fileLength !== 0) {
        item.sizeStr = formatByte(fileLength)
      }
      item.fileName = fileName
    })
  }
  return result
}
