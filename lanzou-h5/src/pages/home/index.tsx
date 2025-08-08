import { Button } from '@/components/Button'
import style from './index.module.scss'
import { useNavigate } from 'react-router'
import Config from '@/config/app'

export default function HomePage() {
  const navigate = useNavigate()

  function goPage(path: string) {
    navigate(path)
  }

  return (
    <div className={style.home}>
      <Button onClick={() => goPage('/share/file')} size="large" width="80%">
        解析文件
      </Button>
      <Button
        onClick={() => goPage('/share/folder')}
        size="large"
        width="80%"
        style={{ marginTop: 10 }}
      >
        解析文件夹
      </Button>
      <Button
        onClick={() => window.open(Config.GITHUB_HOME)}
        size="large"
        width="80%"
        style={{ marginTop: 10 }}
      >
        Github
      </Button>
    </div>
  )
}
