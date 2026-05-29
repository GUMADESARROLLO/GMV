package com.unimarksa.gmv.models

data class Doctor(
    val id: String,
    val nombre: String,
    val especialidad: String,
    val telefono: String,
    val email: String,
    val estado: DoctorStatus
)
