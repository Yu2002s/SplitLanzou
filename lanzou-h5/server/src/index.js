/**
 *
 *  By Yu2002s
 *
 */

import express from "express";

import cors from "cors";
import httpProxyMiddleware from "http-proxy-middleware";

const app = express();
app.use(cors());
app.use(express.json({ type: "application/json" }));
app.use(express.urlencoded({ extended: true }));

const proxy = httpProxyMiddleware.createProxyMiddleware;

const HEADERS = {
  "Content-Type": "*/*",
  Accept: "*/*",
  "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
  "User-Agent":
    "Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36 Edg/131.0.0.0",
};

app.get("/get-file", (req, res) => {
  let fileName = "";
  fetch(req.query.url, {
    method: "HEAD",
    headers: HEADERS,
  }).then((result) => {
    const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
    const contentDisposition = result.headers.get("content-disposition");
    const match = filenameRegex.exec(contentDisposition);
    if (match && match[1]) {
      fileName = decodeURIComponent(
        match[1].replace("UTF-8''", "").replace(/\"/g, "")
      );
    }
    let { fileName: realFileName, fileLength } =
      getSplitFileNameOrLength(fileName);
    let isSplitFile = false;
    if (fileLength === 0) {
      fileLength = result.headers.get("Content-Length") || 0;
      isSplitFile = false;
    } else {
      isSplitFile = true;
    }
    fileName = realFileName;
    res.send({
      fileName,
      fileLength,
      isSplitFile,
    });
  });
});

// 转发请求
app.use(
  "/api",
  proxy({
    target: "http://api.jdynb.xyz:6400",
    changeOrigin: true,
  })
);

// 代理到 lanzou
app.use(
  "/",
  proxy({
    target: "https://lanzoui.com",
    changeOrigin: true,
    headers: {
      Origin: "https://lanzoui.com",
      Accept: "application/json, text/javascript, */*",
      "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
      "Content-Type": "application/x-www-form-urlencoded",
      Referer: "https://lanzoui.com",
      "User-Agent":
        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1 Edg/138.0.0.0",
    },
  })
);

// 监听10001端口
app.listen(10001, () => {
  console.log("Server is running on port 10001");
});

/**
 * 获取分割文件（大于100M的文件）名称、大小
 * @param filename 文件名称
 * @returns 真实的文件名称、大小
 */
export function getSplitFileNameOrLength(filename) {
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
export function isSplitFile(fileName) {
  return /(.+)\.([a-zA-Z]+\d?)\[(\d+)]\.enc/.test(fileName);
}

/**
 * 获取文件名称
 * @param filename 文件名称
 * @returns 真实的文件名称
 */
export function getNormalFileName(filename) {
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
export function getFileName(filename) {
  if (filename.endsWith(".enc")) {
    return getSplitFileNameOrLength(filename).fileName;
  } else {
    return getNormalFileName(filename);
  }
}
