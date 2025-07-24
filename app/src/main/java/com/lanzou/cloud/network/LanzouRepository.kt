package com.lanzou.cloud.network

import com.drake.net.Get
import com.drake.net.Post
import com.lanzou.cloud.config.Api
import com.lanzou.cloud.model.BaseLanzouResponse
import com.lanzou.cloud.model.FileInfoModel
import com.lanzou.cloud.model.LanzouShareFolderModel
import com.lanzou.cloud.model.ProfileModel
import com.lanzou.cloud.service.UploadService
import com.lanzou.cloud.utils.converter.SerializationConverter
import com.lanzou.cloud.utils.formatBytes
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup

object LanzouRepository {

  private val CLICKABLE_NAMES = arrayOf("个性域名", "密码修改", "手机号", "注销账户")

  private val FILE_REGEX = "(.+)\\.([a-zA-Z]+\\d?)\\.apk".toRegex()

  private val SPLIT_FILE_REGEX =
    ("(.+)\\.([a-zA-Z]+\\d?)\\[(\\d+)]\\." + UploadService.SPLIT_FILE_EXTENSION).toRegex()

  /**
   * 获取用户的资料信息
   */
  suspend fun getUserProfiles() = coroutineScope {
    val result = Get<String>(Api.DISK_PHP) {
      addQuery("item", "profile")
      addQuery("action", "mypower")
    }.await()

    val document = Jsoup.parse(result)
    val list = document.select("#info1 .mf")
    list.map {
      val name = (it.selectFirst(".mf1")?.text() ?: "undefined").removeSuffix(":")
      val mf2 = it.selectFirst(".mf2")
      val firstChild = mf2?.firstElementChild()
      var value = firstChild?.text()
      if (value.isNullOrBlank()) {
        value = firstChild?.nextElementSibling()?.text()
      }
      ProfileModel(name, value ?: "", CLICKABLE_NAMES.contains(name))
    }.filter { it.value.isNotEmpty() }
  }

  /**
   * 编辑密码
   */
  suspend fun editPassword(password: String, newPassword: String) = coroutineScope {
    val result = Get<String>(Api.DISK_PHP) {
      addQuery("item", "profile")
      addQuery("action", "password")
    }.await()

    val document = Jsoup.parse(result)
    val formEl = document.selectFirst("form") ?: return@coroutineScope false
    val inputEls = formEl.select("input[type=hidden]")

    val map = mutableMapOf<String, String>()
    inputEls.forEach { item ->
      map[item.attr("name")] = item.attr("value")
    }


    val result2 = Post<String>(Api.DISK_PHP) {
      addQuery("item", "profile")
      for ((key, value) in map.entries) {
        param(key, value)
      }
      param("old_pwd", password)
      param("new_pwd", newPassword)
      param("cfm_pwd", newPassword)
    }.await()

    val document2 = Jsoup.parse(result2)
    val tipsText =
      document2.selectFirst(".info_box .info_b2")?.text() ?: return@coroutineScope false
    return@coroutineScope tipsText.contains("密码修改成功")
  }

  suspend fun editDomain(domain: String) = coroutineScope {
    Post<BaseLanzouResponse>(Api.FILE_PHP) {
      param("task", 48)
      param("domain", domain)
    }.await().zt == 1
  }

  suspend fun sendEditPhoneSms(phone: String) = coroutineScope {
    Post<BaseLanzouResponse>(Api.FILE_PHP) {
      param("task", 43)
      param("type", 1)
      param("phoneold", phone)
    }.await().zt == 1
  }

  suspend fun editPhone(phone: String, newPhone: String, code: String) = coroutineScope {
    Post<BaseLanzouResponse>(Api.FILE_PHP) {
      param("task", 43)
      param("type", 2)
      param("phoneold", phone)
      param("phonenew", newPhone)
      param("phonecode", code)
    }.await().zt == 1
  }

  suspend fun deleteFile(fileId: String, isFile: Boolean = true) = coroutineScope {
    Post<BaseLanzouResponse>(Api.FILE_PHP) {
      param("task", if (isFile) 6 else 3)
      if (isFile) {
        param("file_id", fileId)
      } else {
        param("folder_id", fileId)
      }
    }.await().zt == 1
  }

  /**
   * 通过文件夹分享地址获取到文件列表（暂时不支持分页）
   *
   * @param url 原始分享地址
   * @param pwd 文件密码（没有留空）
   */
  suspend fun getShareFolders(url: String, pwd: String? = null) = coroutineScope {
    Get<List<LanzouShareFolderModel>>(Api.GET_SHARE_FOLDERS) {
      converter =
        SerializationConverter(
          success = arrayOf("200"),
          code = "code",
          data = "data",
          message = "msg"
        )
      addQuery("url", url)
      addQuery("pwd", pwd)
    }.await().onEach {
      getFileRealName(it)
    }
  }

  /**
   * 获取指定文件夹 id 下的所有文件
   *
   * @param folderId 文件夹 id
   * @param page 页码
   */
  suspend fun getFiles(folderId: String, page: Int): List<FileInfoModel> {
    val files = mutableListOf<FileInfoModel>()
    if (page == 1) {
      files.addAll(getFiles(47, folderId, page))
    }
    files.addAll(getFiles(5, folderId, page).onEach {
      it.folderId = folderId
      getFileRealName(it)
    })
    return files
  }

  /**
   * 通过 task、文件夹 id 获取文件文件列表
   *
   * @param task 指定任务 id
   * @param folderId 文件夹 id
   * @param page 页码
   */
  suspend fun getFiles(task: Int, folderId: String, page: Int) = coroutineScope {
    Post<List<FileInfoModel>?>(Api.FILE_PHP) {
      param("task", task)
      param("folder_id", folderId)
      param("pg", page)
    }.await()?.onEach {
      getFileRealName(it)
    } ?: emptyList()
  }

  /**
   * 创建文件夹
   *
   * @param parentId 父文件夹 id
   * @param name 文件夹名称
   * @return 创建后的文件夹 id
   */
  suspend fun mkdirFolder(parentId: String, name: String) = coroutineScope {
    Post<BaseLanzouResponse>(Api.FILE_PHP) {
      param("task", 2)
      param("parent_id", parentId)
      param("folder_name", name)
    }.await().text
  }

  suspend fun moveFile(fileId: String, folderId: String) = coroutineScope {
    Post<BaseLanzouResponse>(Api.FILE_PHP) {
      param("task", 20)
      param("file_id", fileId)
      param("folder_id", folderId)
    }.await().zt == 1
  }

  private fun getFileRealName(fileInfoModel: FileInfoModel) {
    val ext = fileInfoModel.extension
    val name = fileInfoModel.nameAll

    val isApk = "apk" == ext

    val regex = if (isApk) FILE_REGEX else SPLIT_FILE_REGEX

    val matchResult = regex.find(name) ?: return

    val isSplitFile = "enc" == ext
    val realExt = matchResult.destructured.component2()
    fileInfoModel.extension = realExt
    fileInfoModel.name =
      "${matchResult.destructured.component1()}.${realExt}"
    if (isSplitFile) {
      fileInfoModel.length = matchResult.destructured.component3().toLong()
      fileInfoModel.size = fileInfoModel.length.formatBytes()
    }
  }

  private fun getFileRealName(lanzouShareFolderModel: LanzouShareFolderModel) {
    val ext = lanzouShareFolderModel.fileType
    val name = lanzouShareFolderModel.fileName

    val isApk = "apk" == ext
    val regex = if (isApk) FILE_REGEX else SPLIT_FILE_REGEX

    val matchResult = regex.find(name) ?: return

    val isSplitFile = "enc" == ext
    lanzouShareFolderModel.fileName =
      "${matchResult.destructured.component1()}.${matchResult.destructured.component2()}"
    if (isSplitFile) {
      lanzouShareFolderModel.size = matchResult.destructured.component3().toLong()
      lanzouShareFolderModel.sizeStr = lanzouShareFolderModel.size.formatBytes()
    }
  }
}