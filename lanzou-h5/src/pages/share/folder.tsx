import type { ParserResponse } from '@/types/common'
import type { LanzouParseFile } from '@/types/file'
import { Form, Link, useLoaderData, useParams } from 'react-router'
import styles from './folder.module.scss'
import { useState } from 'react'
import { Button } from '@/components/Button'

export default function FolderSharePage() {
  const loaderData = useLoaderData<ParserResponse<LanzouParseFile[]>>()
  const params = useParams()

  const [url, setUrl] = useState(`https://www.lanzoui.com/${params.id || ''}`)
  const [pwd, setPwd] = useState(params.pwd || '')
  const index = url.lastIndexOf('/') + 1
  const id = index > 0 ? url.substring(index) : ''

  function clearUrl() {
    setUrl('')
  }

  return (
    <div className={styles.folderContainer}>
      {loaderData.success ? (
        <FileList />
      ) : (
        <div className={styles.form}>
          <h3 style={{ textAlign: 'center' }}>{loaderData.msg}</h3>
          <Form action={`/share/folder/${id}/${pwd}`} replace={true}>
            <label className={styles.formItem}>
              <span className={styles.labelName}>分享地址:</span>
              <input
                className={styles.formInput}
                type="text"
                placeholder="请输入分享地址"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
              />
              <Button type="button" style={{ height: 48, marginLeft: 10 }} onClick={clearUrl}>
                清空
              </Button>
            </label>
            <label className={styles.formItem}>
              <span className={styles.labelName}>文件密码:</span>
              <input
                className={styles.formInput}
                type="text"
                placeholder="请输入文件密码(可留空)"
                value={pwd}
                onChange={(e) => setPwd(e.target.value)}
              />
            </label>
            <Button
              type="submit"
              style={{ width: '100%', height: 48, marginTop: 10 }}
            >
              获取文件列表
            </Button>
          </Form>
        </div>
      )}
    </div>
  )
}

function FileList() {
  const loaderData = useLoaderData<ParserResponse<LanzouParseFile[]>>()

  return (
    <ul className={styles.fileList}>
      {loaderData.data.map((item) => {
        return (
          <li className={styles.fileItem} key={item.fileId}>
            <Link
              className={styles.fileLink}
              to={`/share/file/${encodeURIComponent(item.fileId)}`}
            >
              {item.fileName}
            </Link>
            <p style={{ marginTop: 10 }}>
              <span>{item.sizeStr}</span>
              <span style={{ marginLeft: 10 }}>{item.createTime}</span>
            </p>
          </li>
        )
      })}
      <li style={{ padding: '10px', height: 40, textAlign: 'center' }}>
        by 冬日暖雨
      </li>
    </ul>
  )
}
