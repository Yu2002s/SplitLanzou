import Config from '@/config/app'
import style from './index.module.scss'
import { Button } from '@/components/Button'
import logo from '@/assets/images/logo.png'
import { useNavigate, useNavigation } from 'react-router'

export default function DownloadPage() {

  const navigate = useNavigate()
  const navigation = useNavigation()

  function navigateHistory() {
    const fileId = Config.PAN_SHARE_URL.substring(Config.PAN_SHARE_URL.lastIndexOf('/') + 1)
    navigate(`/share/folder/${fileId}/${Config.PAN_SHARE_PWD}`)
  }

  return (
    <div className={style.downloadPage}>
      <img className={style.logo} src={logo} alt="logo" />
      <p className={style.appName}>SplitLanzou</p>

      <p className={style.tip}>
        {navigation.state === 'loading' ? '正在跳转...' : '请选择下载方式'}
      </p>

      <Button size="large" width="80%" onClick={navigateHistory}>
        历史版本
      </Button>
      <Button
        size="large"
        width="80%"
        style={{ marginTop: 10 }}
        onClick={() => window.open(Config.GITHUB_HOME)}
      >
        从Github下载
      </Button>
      <Button
        width="80%"
        size="large"
        style={{ marginTop: 10 }}
        onClick={() => window.open(Config.PAN_SHARE_URL)}
      >
        从网盘中下载（密码：2fgt）
      </Button>
    </div>
  )
}
