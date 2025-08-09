import style from './index.module.scss'

const arr = Array.from({length: 8}, (_, i) => i + 1)

export default function Loading() {
  return (
    <div className={style.loading}>
      <div className={style.loadingWrap}>
        {arr.map((_item, index) => (
          <span
            key={index}
            className={style.loadingItem}
            style={{
              transform: `rotateZ(calc(${index + 1} * 45deg)) translateY(-10px)`,
            }}
          ></span>
        ))}
      </div>
      <p className={style.loadingText}>正在加载中...请等待</p>
    </div>
  )
}
