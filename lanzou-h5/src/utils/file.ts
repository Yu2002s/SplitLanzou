import type { FilePart, SplitFile } from "@/types/file";
import Config from "@/config/app";

/**
 * 下载普通文件
 * @param url 下载地址
 * @param fileName 文件名称
 * @param cb 完成回调
 */
export function downloadFile(
  url: string,
  fileName: string,
  cb: (success: boolean) => void
) {
  const a = document.createElement("a");
  a.href = url;
  // 指定下载文件名为页面显示的文件名
  a.download = fileName;
  // 使用 blob 来指定下载文件名
  fetch(a.href)
    .then((res) => res.blob())
    .then((blob) => {
      const url = URL.createObjectURL(blob);
      a.href = url;
      a.click();
      URL.revokeObjectURL(url);
      cb(true);
    })
    .catch((err) => {
      // 可能会报错，下载的文件名就是不正确的
      console.error(err);
      a.click();
      cb(false);
    });
}

/**
 * 格式化文件大小单位
 * @param size 具体大小
 * @returns 带单位的大小
 */
export function formatByte(size: number): string {
  if (!size) return "0 B";

  const units = ["B", "KB", "MB", "GB", "TB"];
  let index = 0;

  while (size >= 1024 && index < units.length - 1) {
    size /= 1024;
    index++;
  }

  return size.toFixed(2) + " " + units[index];
}

/**
 * 获取分割文件（大于100M的文件）名称、大小
 * @param filename 文件名称
 * @returns 真实的文件名称、大小
 */
export function getSplitFileNameOrLength(filename: string): {
  fileName: string;
  fileLength: number;
} {
  // 大于100M文件
  const regex = /(.+)\.([a-zA-Z]+\d?)\[(\d+)]\.enc/;
  const matches = regex.exec(filename);
  if (matches && matches.length > 2) {
    return {
      fileName: matches[1] + "." + matches[2],
      fileLength: Number(matches[3]),
    };
  }
  return {
    fileName: getNormalFileName(filename),
    fileLength: 0,
  };
}

/**
 * 判断是否是分割文件
 * @param fileName 文件名
 * @returns true：是，false：否
 */
export function isSplitFile(fileName: string): boolean {
  return /(.+)\.([a-zA-Z]+\d?)\[(\d+)]\.enc/.test(fileName);
}

/**
 * 获取文件名称
 * @param filename 文件名称
 * @returns 真实的文件名称
 */
export function getNormalFileName(filename: string): string {
  // 小于100M文件
  const regex = /(.+)\.([a-zA-Z]+\d?)\.apk/;
  const matches = regex.exec(filename);
  if (matches && matches.length > 2) {
    return matches[1] + "." + matches[2];
  }
  return filename;
}

/**
 * 根据后缀获取文件名称
 * @param filename 文件名称
 * @returns 真实的文件名称
 */
export function getFileName(filename: string): string {
  if (filename.endsWith(".enc")) {
    return getSplitFileNameOrLength(filename).fileName;
  } else {
    return getNormalFileName(filename);
  }
}

async function getDirectLink(url: string) {
  const data = await fetch(url).then((res) => res.json());
  return data.data.directLink;
}

/**
 * 下载分割文件
 * @param splitFile 分割文件
 * @param cb 进度回调
 */
export async function downloadSplitFile(
  splitFile: SplitFile,
  cb: (progress: number) => void
) {
  const files = splitFile.files;
  if (!files.length) {
    throw new Error("文件列表是空的");
  }

  let downloadTotal = 0;
  const timer = setInterval(() => {
    const progress = (downloadTotal * 100) / splitFile.length;
    cb(progress);
  }, 1000);

  const promises = files.map((file) =>
    getDownloadFile(file, (size) => {
      downloadTotal += size;
    })
  );

  const results = await Promise.all(promises).finally(() => {
    clearInterval(timer);
  });

  const resultBuffer = new Uint8Array(splitFile.length);
  results
    .sort((a, b) => a.index - b.index)
    .forEach((file) => {
      resultBuffer.set(file.buffer, file.start);
    });

  // 注意：如果文件很大的话，这里是会直接溢出崩溃的
  const blob = new Blob([resultBuffer], {
    type: "application/octet-stream",
  });
  cb(100) // 确保文件完成
  const blobUrl = URL.createObjectURL(blob)
  const a = document.createElement("a");
  a.href = blobUrl;
  a.download = splitFile.name;
  a.click();
  URL.revokeObjectURL(blobUrl);
}

async function getDownloadFile(file: FilePart, block: (size: number) => void) {
  const pwd = file.pwd || "";
  const url = await getDirectLink(Config.PARSE_API + file.url + "&pwd=" + pwd);
  const response = await fetch(url, {
    headers: {
      Connection: "keep-alive",
    },
    cache: "no-store",
    mode: "cors",
    credentials: "omit",
  });
  const reader = response.body!.getReader();
  // const contentLength = +response!.headers!.get("Content-Length")!;

  let receivedLength = 0;
  const chunks: Uint8Array<ArrayBuffer>[] = [];

  while (true) {
    const { done, value } = await reader.read();

    if (done) break;

    chunks.push(value);
    receivedLength += value.length;

    block(value.length);
    // 计算下载进度百分比
    // const progress = (receivedLength / contentLength) * 100
    // console.log(`文件 ${file.index + 1} 下载进度: ${progress.toFixed(2)}%`)
  }

  const buffer = new Uint8Array(receivedLength);
  let position = 0;
  for (const chunk of chunks) {
    buffer.set(chunk, position);
    position += chunk.length;
  }

  return {
    ...file,
    buffer,
  };
}
