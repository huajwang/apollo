package com.goodfeel.nightgrass.data

data class Video(
    val id: String,
    val name: String,
    val url: String, // Path to video file
    val uploadedAt: Long
)
