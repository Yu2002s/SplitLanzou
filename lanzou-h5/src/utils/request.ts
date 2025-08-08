const BASE_URL = import.meta.env.VITE_API_BASE;

/**
 * 通用返回响应数据结构
 */
export type BaseResponse<T> = {
  zt: 0 | 1;
  msg?: string;
  text?: string | T;
};

const HEADER = {
  "Content-Type": "application/x-www-form-urlencoded",
};

type RequestType = "GET" | "POST" | "DELETE" | "PUT"

/**
 * 发起简单请求
 *
 * @param url 请求地址
 */
export function request<R>(url: string): Promise<R>;

/**
 * 发起请求
 *
 * @param url 请求地址
 * @param method 请求方法
 * @param data 请求数据
 * @param headers 请求头
 * @returns 请求返回数据
 */
export function request<P, R>(
  url: string,
  method?: RequestType,
  data?: P,
  headers?: Record<string, string>
): Promise<R>;

export async function request<P, R>(
  url: string,
  method: RequestType = "GET",
  data: P | null = null,
  headers: Record<string, string> = HEADER
): Promise<R> {
  let query = "";
  if (method === "GET" && data) {
    const urlSearchParams = new URLSearchParams(data);
    query = "?" + urlSearchParams.toString();
  }
  let body: string | URLSearchParams | FormData
  const contentType = headers["Content-Type"].toLowerCase();
  if (contentType === "application/json" && data) {
    body = JSON.stringify(data);
  } else if (contentType === "application/x-www-form-urlencoded" && data) {
    const formData = new FormData()
    for (const [key, value] of Object.entries(data)) {
      formData.append(key, String(value))
    }
    body = formData
  }
  const isHttpUrl = url.startsWith("http")
  const requestUrl = isHttpUrl ? url : BASE_URL + url
  return new Promise<R>((resolve, reject) => {
    fetch(requestUrl + query, {
      method: method,
      body: method !== "GET" ? (body as XMLHttpRequestBodyInit) : null,
      headers,
    })
      .then((res: Response) => {
        const contentType = res.headers.get("Content-Type");
        if (
          contentType?.includes("application/json") ||
          contentType?.includes("text/json")
        ) {
          return res.json();
        }
        return res.text();
      })
      .then((res: BaseResponse<R> | string) => {
        if (isHttpUrl) {
          return res as R
        }
        if (typeof res === 'string' || !res.zt) {
          return resolve(res as R)
        }
        if (res.zt === 1) {
          resolve(res.text as R);
        } else {
          reject(res.msg || res.text);
        }
      })
      .catch((err) => {
        reject(err);
      });
  });
}
