package com.lanzou.cloud.config

import com.lanzou.cloud.LanzouApplication

/**
 * 接口管理
 */
object Api {

  const val BASE_URL = "https://pc.woozooo.com"

  /**
   * 网盘主页
   */
  const val DISK_PHP = "/mydisk.php"

  /**
   * 获取分享文件夹
   */
  const val GET_SHARE_FOLDERS = "${LanzouApplication.DOWNLOAD_API_URL}/v2/getFileList"

  /**
   * 操作文件相关
   */
  const val FILE_PHP = "/doupload.php"
}