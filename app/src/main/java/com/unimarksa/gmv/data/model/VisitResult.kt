package com.unimarksa.gmv.data.model

sealed class VisitResult {
    data class Effective(
        val moleculas: List<String>,
        val observaciones: String
    ) : VisitResult()

    data class NotEffective(
        val motivo: String,
        val otro: String = ""
    ) : VisitResult()
}
