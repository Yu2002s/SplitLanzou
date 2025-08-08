import { isRouteErrorResponse, useNavigate, useRouteError } from 'react-router'

import style from './index.module.scss'
import { Button } from '@/components/Button'

export default function Error() {
  const error = useRouteError()
  const navigate = useNavigate()

  if (isRouteErrorResponse(error)) {
    return (
      <div className={style.error}>
        <p>错误码: {error.status}</p>
        <p>路由加载错误: {error.statusText}</p>
        <Button onClick={() => navigate(-1)}>返回</Button>
      </div>
    )
  } else if (error instanceof Error) {
    return <h1>加载错误: {(error as Error).message}</h1>
  } else {
    return <h1>发生位置错误</h1>
  }
}
