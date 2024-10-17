package com.example.myapplication
data class chatbot_data(
    val text: String,
    val isFromUser: Boolean,
    var isProcessed: Boolean = false
)