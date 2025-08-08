import { useRef, useState } from 'react'
import styles from './file.module.scss'
import { Form, Link, useLoaderData, useParams, useSubmit } from 'react-router'
import reactIcon from '@/assets/react.svg'
import { Button } from '@/components/Button'
import ProgressButton from '@/components/ProgressButton'
import { type IFileLoaderData } from './loader'
import { downloadFile, downloadSplitFile, formatByte } from '@/utils/file.ts'
import Config from '@/config/app'
import { isUrl } from '@/utils/common'
import type { SplitFile } from '@/types/file'

export default function FileSharePage() {
  const params = useParams()
  const submit = useSubmit()
  const loaderData = useLoaderData<IFileLoaderData>()
  const [url, setUrl] = useState(`https://www.lanzoui.com/${params.id || ''}`)
  const [pwd, setPwd] = useState(params.pwd || '')
  const [downloadBtnDisabled, setDownloadBtnDisabled] = useState(false)
  const [progress, setProgress] = useState(100)
  const formRef = useRef(null)

  const index = url.lastIndexOf('/') + 1
  const id = index > 0 ? url.substring(index) : ''

  const isBtnDisabled = !isUrl(url)

  let downloadBtnContent

  if (downloadBtnDisabled) {
    downloadBtnContent = '正在加载中...'
  } else {
    if (params.id) {
      downloadBtnContent = (
        <span>开始下载{progress < 100 && `(${progress}%)`}</span>
      )
    } else {
      downloadBtnContent = '开始解析'
    }
  }

  function clearUrl() {
    setUrl('')
  }

  function openOriginUrl() {
    window.open(url)
  }

  function openSplitLanzou() {
    let shareUrl = url.replace('https', 'lanzou')
    if (pwd) {
      shareUrl += `?pwd=${pwd}`
    }
    window.open(shareUrl)
  }

  async function startDownload() {
    if (!url) {
      return
    }
    if (id != params.id) {
      await submit(formRef.current)
      return
    }

    if (!loaderData.download) {
      // 没有获取到下载信息，可能还没加载
      return
    }
    setDownloadBtnDisabled(true)

    const fileName = loaderData.download.fileName
    // 这里直接进行判断后缀了
    if (!loaderData.download.isSplitFile) {
      // 下载小于100M的文件
      downloadFile(loaderData.download.directLink, fileName, () => {
        setDownloadBtnDisabled(false)
      })
    } else {
      // 下载大于100M的文件
      parseFile(loaderData.download.directLink)
    }
  }

  // 解析大于100M的文件
  function parseFile(downloadUrl: string) {
    fetch(downloadUrl, {
      headers: Config.REQUEST_HEADER,
    })
      .then((res) => res.json())
      .then(async (data: SplitFile) => {
        setProgress(0)
        let isError = false
        await downloadSplitFile(data, (progress: number) => {
          setProgress(progress)
        }).catch(() => {
          // 下载失败，跨域了
          if (isError) {
            return
          }
          isError = true
          if (confirm('浏览器限制，该文件需要通过SplitLanzou进行下载')) {
            openSplitLanzou()
          }
        })
      })
      .catch((err) => {
        console.error(err)
        if (confirm('浏览器限制，该文件需要通过SplitLanzou进行下载')) {
          openSplitLanzou()
        }
        setDownloadBtnDisabled(false)
      })
      .finally(() => {
        setProgress(100)
        setDownloadBtnDisabled(false)
      })
  }

  return (
    <div className={styles.filePage}>
      <div className={styles.fileInfo}>
        <img alt="logo" className={styles.fileLogo} src={reactIcon} />
        {loaderData.download && (
          <>
            <p className={styles.filename}>{loaderData.download.fileName}</p>
            <p>{formatByte(loaderData.download.fileLength)}</p>
          </>
        )}
        <p style={{ color: 'red' }}>
          {!loaderData.success && '获取文件信息失败'}
        </p>
        <Form ref={formRef} action={`/share/file/${id}/${pwd}`} replace={true}>
          <label className={styles.formItem}>
            <span className={styles.labelName}>分享地址:</span>
            <input
              className={styles.formInput}
              type="text"
              placeholder="请输入分享地址"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
            <Button
              type="button"
              style={{ height: 48, marginLeft: 10 }}
              onClick={clearUrl}
            >
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
        </Form>
      </div>
      <div style={{ textAlign: 'center' }}>
        <p style={{ fontSize: 14, color: 'red' }}>
          建议在SplitLanzou中打开进行下载
        </p>
        <p>
          <Link to="/download">下载SplitLanzou</Link>
          <Link
            style={{ marginLeft: 10 }}
            to="https://github.com/Yu2002s/SplitLanzou"
            target="new"
          >
            Github
          </Link>
          <Link
            style={{ marginLeft: 10 }}
            to="https://www.jdynb.xyz"
            target="new"
          >
            个人主页
          </Link>
        </p>
      </div>
      <div className={styles.bottom}>
        <div className={styles.otherBtn}>
          <Button
            disabled={isBtnDisabled}
            onClick={openOriginUrl}
            style={{ flex: 1, height: 48 }}
          >
            打开原分享地址
          </Button>
          <Button
            disabled={isBtnDisabled}
            onClick={openSplitLanzou}
            style={{ flex: 1, height: 48, marginLeft: 10 }}
          >
            在SplitLanzou中下载
          </Button>
        </div>
        <ProgressButton
          progress={progress}
          onClick={startDownload}
          disabled={downloadBtnDisabled}
        >
          {downloadBtnContent}
        </ProgressButton>
      </div>
    </div>
  )
}
