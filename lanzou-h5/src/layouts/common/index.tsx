import { Outlet, useLocation, useNavigate, useNavigation } from 'react-router'
import styles from "./index.module.scss";
import Loading from '@/components/Loading'
import { Button } from '@/components/Button'

export default function CommonLayout() {
  const navigation = useNavigation();
  const isNavigation = Boolean(navigation.location);
  const navigate = useNavigate()
  const location = useLocation()
  const isShowHomeBtn = location.pathname !== '/'

  function goHome() {
    navigate('/')
  }

  return (
    <div className={styles.commonPage}>
      <header className={styles.header}>
        <h1 className={styles.title}>SplitLanzou</h1>
        {isShowHomeBtn && <Button onClick={goHome}>主页</Button>}
      </header>
      <main className={styles.mainContent}>
        {isNavigation ? <Loading /> : <Outlet />}
      </main>
    </div>
  );
}
