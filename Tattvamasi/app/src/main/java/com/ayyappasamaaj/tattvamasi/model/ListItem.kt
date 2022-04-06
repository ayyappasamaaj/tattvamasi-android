package com.ayyappasamaaj.tattvamasi.model

data class ListItem(
    val fileUrl: String = "",
    var itemTitle: String = "",
    val language: String = "",
    var header: Boolean = false,
    val headerTitle: String = ""
)