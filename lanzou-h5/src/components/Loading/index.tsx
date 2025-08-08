import style from './index.module.scss'

export default function Loading() {
  return (
    <div className={style.loading}>
      <span className={style.loadingText}>正在加载中，请稍后...</span>
    </div>
  )
}
