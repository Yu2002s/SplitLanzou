package com.lanzou.cloud.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineBottomSheetDialogFragment
import com.drake.net.utils.withIO
import com.drake.tooltip.toast
import com.lanzou.cloud.R
import com.lanzou.cloud.databinding.DialogFileFavoriteBinding
import com.lanzou.cloud.model.FavoriteItem
import com.lanzou.cloud.model.FileFavoritesModel
import com.lanzou.cloud.model.LanzouResolveFileModel
import kotlinx.coroutines.launch
import org.litepal.LitePal
import org.litepal.extension.findAll

class FileFavoriteDialog(private val lanzouResolveFileModel: LanzouResolveFileModel = LanzouResolveFileModel()) :
  EngineBottomSheetDialogFragment<DialogFileFavoriteBinding>() {

  var onFavoriteClickListener: View.OnClickListener? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.dialog_file_favorite, container, false)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    /*setStyle(
      STYLE_NO_TITLE,
      com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog
    )*/
  }

  override fun initData() {
    binding.m = lanzouResolveFileModel
  }

  override fun initView() {
    // setMaxWidth(percent = 1f)

    val favoritesModels = mutableListOf<FileFavoritesModel>()

    viewLifecycleOwner.lifecycleScope.launch {
      val fileFavoritesModels = withIO {
        LitePal.findAll<FileFavoritesModel>().also {
          if (it.isEmpty()) {
            val model = FileFavoritesModel("默认")
            model.save()
            favoritesModels.add(model)
          }
        }
      }
      favoritesModels.addAll(fileFavoritesModels)
      binding.spinner.adapter = ArrayAdapter(
        requireContext(),
        R.layout.layout_spinner_item,
        fileFavoritesModels.map { it.name })
    }

    /*binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        favoritesModels[position].id
      }

      override fun onNothingSelected(parent: AdapterView<*>?) {

      }
    }*/

    binding.btnFavorite.setOnClickListener {
      val extension = lanzouResolveFileModel.fileName.substringAfterLast(".", "")
      FavoriteItem(
        name = lanzouResolveFileModel.fileName, url = lanzouResolveFileModel.url,
        fileId = lanzouResolveFileModel.url.substringAfterLast("/", lanzouResolveFileModel.url),
        isFile = lanzouResolveFileModel.isFile,
        pwd = lanzouResolveFileModel.pwd ?: "",
        size = lanzouResolveFileModel.fileSize,
        extension = extension.ifEmpty { null },
        time = lanzouResolveFileModel.shareTime,
        remark = lanzouResolveFileModel.remark,
        updateAt = System.currentTimeMillis(),
        favoritesModel = favoritesModels[binding.spinner.selectedItemPosition]
      ).saveOrUpdate("url = ?", lanzouResolveFileModel.url)
      toast("收藏成功")
      dismiss()
      onFavoriteClickListener?.onClick(it)
    }

    binding.btnClose.setOnClickListener {
      dismiss()
    }
  }

}