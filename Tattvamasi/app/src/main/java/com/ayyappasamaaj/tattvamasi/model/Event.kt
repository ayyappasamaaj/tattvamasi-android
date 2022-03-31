package com.ayyappasamaaj.tattvamasi.model

data class Event(
    val date: Long = 0,
    val desc: String? = "",
    val latitude: Long = 0,
    val longitude: Long = 0,
    val name: String? = "",
    val registrationLink: String? = "",
    val venue: String? = "",
    val month: String? = "",
    val day: String? = "",
    val time: String? = ""
)