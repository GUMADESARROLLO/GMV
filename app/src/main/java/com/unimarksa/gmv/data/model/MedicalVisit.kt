package com.unimarksa.gmv.data.model

data class MedicalVisit(
    val id: String,
    val doctorId: String,
    val doctorNombre: String,
    val doctorEspecialidad: String,
    val fecha: String,
    val efectiva: Boolean,
    val result: VisitResult
)
