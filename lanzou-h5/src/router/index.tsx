import { createBrowserRouter } from 'react-router'
import CommonLayout from '../layouts/common'
import { lazy } from 'react'
import { fileLoader, folderLoader } from '../pages/share/loader'
import Error from '@/components/Error'
import Loading from '@/components/Loading'

const router = createBrowserRouter([
  {
    errorElement: <Error />,
    hydrateFallbackElement: <Loading />,
    Component: CommonLayout,
    children: [
      {
        index: true,
        Component: lazy(() => import('../pages/home/index')),
      },
      {
        path: 'test',
        Component: lazy(() => import('../pages/test/index')),
      },
      {
        path: 'download',
        Component: lazy(() => import('../pages/download/index')),
      },
      {
        path: 'donate',
        Component: lazy(() => import('../pages/donate/index')),
      },
      {
        path: 'share',
        children: [
          {
            path: 'file/:id?/:pwd?',
            loader: fileLoader,
            Component: lazy(() => import('../pages/share/file')),
          },
          {
            path: 'folder/:id?/:pwd?',
            loader: folderLoader,
            Component: lazy(() => import('../pages/share/folder')),
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <h1>404 Not Found</h1>,
  },
])

export default router
