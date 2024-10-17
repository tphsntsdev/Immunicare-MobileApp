package com.example.myapplication

data class fetchdeletedappointments(
    val adultvaccine : String ?= null,
    val childvaccine : String ?= null,
    val status : String ?= null,
    val pkIdentifier: String?= null,
    val adultCertificateURL : String?= null,
    val childCertificateURL : String?= null,
    val date :String?= null,
    val time : String?= null,
    val location : String?= null,
)