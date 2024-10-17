package com.example.myapplication
data class fetchdatafromFirebase(
    val childvaccine : String ?= null,
    val adultvaccine : String ?= null,
    val date: String ?= null,
    val location : String ?= null,
    val time : String ?= null,
    val appointmentKey : String ?= null,
    val status : String ?= null

)
