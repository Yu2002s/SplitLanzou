package com.lanzou.cloud.model

import com.lanzou.cloud.enums.FileSortField
import com.lanzou.cloud.enums.FileSortRule

data class FilterSortModel(
  val rule: FileSortRule = FileSortRule.ASC,
  val field: FileSortField = FileSortField.NAME,
  var showSystemApp: Boolean = false,
)