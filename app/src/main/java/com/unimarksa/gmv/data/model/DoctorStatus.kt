package com.unimarksa.gmv.data.model

enum class DoctorStatus(val label: String, val colorRes: Int) {
    ACTIVO("ACTIVO", android.R.color.holo_green_dark),
    PENDIENTE_ALTA("PENDIENTE", android.R.color.holo_orange_dark),
    PENDIENTE_BAJA("BAJA PENDIENTE", android.R.color.holo_red_dark)
}
