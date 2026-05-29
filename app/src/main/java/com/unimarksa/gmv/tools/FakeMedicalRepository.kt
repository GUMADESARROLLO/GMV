package com.unimarksa.gmv.tools

import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.models.DoctorStatus
import com.unimarksa.gmv.models.MedicalVisit
import com.unimarksa.gmv.models.Product
import com.unimarksa.gmv.models.VisitResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FakeMedicalRepository {

    val doctors = mutableListOf(
        Doctor("1", "Dr. Carlos Mejía", "Cardiología", "8888-0001", "cmejia@mail.com", DoctorStatus.ACTIVO),
        Doctor("2", "Dra. Ana Torres", "Neurología", "8888-0002", "atorres@mail.com", DoctorStatus.ACTIVO),
        Doctor("3", "Dr. Luis Ramos", "Pediatría", "8888-0003", "", DoctorStatus.PENDIENTE_ALTA),
        Doctor("4", "Dra. María López", "Ginecología", "8888-0004", "mlopez@mail.com", DoctorStatus.PENDIENTE_BAJA),
        Doctor("5", "Dr. Roberto Sánchez", "Medicina General", "8888-0005", "rsanchez@mail.com", DoctorStatus.ACTIVO),
        Doctor("6", "Dra. Patricia Vega", "Oncología", "8888-0006", "pvega@mail.com", DoctorStatus.ACTIVO)
    )

    val products = listOf(
        Product("1", "Cardiomax", "Atorvastatina 40mg", "Cardiovascular", "Medicamento para reducción de colesterol LDL.", "Indicado para hipercolesterolemia y prevención cardiovascular."),
        Product("2", "Neurofen Plus", "Ibuprofeno 600mg", "Neurológico", "Antiinflamatorio no esteroideo de alta potencia.", "Indicado para dolor neuropático y migrañas."),
        Product("3", "PediVit", "Vitamina D3", "Pediátrico", "Suplemento vitamínico para desarrollo óseo infantil.", "Indicado para prevención de raquitismo y deficiencia vitamínica."),
        Product("4", "Oncosol", "Metotrexato 2.5mg", "Oncológico", "Agente quimioterapéutico antimetabolito.", "Indicado para tratamientos oncológicos y enfermedades autoinmunes."),
        Product("5", "DermaPlus", "Betametasona 0.05%", "Dermatológico", "Corticosteroide tópico de potencia media.", "Indicado para dermatitis, eccemas y reacciones alérgicas cutáneas."),
        Product("6", "GastroProtect", "Omeprazol 20mg", "Gastrointestinal", "Inhibidor de bomba de protones.", "Indicado para reflujo gastroesofágico y úlceras gástricas."),
        Product("7", "DiaFormin", "Metformina 850mg", "Endocrinológico", "Antidiabético oral biguanida.", "Indicado para diabetes tipo 2 y resistencia a la insulina.")
    )

    val molecules = listOf(
        "Atorvastatina", "Ibuprofeno", "Vitamina D3", "Metotrexato",
        "Betametasona", "Omeprazol", "Metformina"
    )

    val specialties = listOf(
        "Cardiología", "Neurología", "Pediatría", "Medicina General",
        "Ginecología", "Oncología", "Ortopedia", "Dermatología",
        "Psiquiatría", "Gastroenterología", "Endocrinología", "Otra"
    )

    val visitMotives = listOf(
        "Médico no disponible",
        "Consultorio cerrado",
        "Médico en cirugía / procedimiento",
        "Médico rechazó la visita",
        "Zona de acceso restringido",
        "Reagendado por el médico",
        "Otro"
    )

    private val visits = mutableListOf<MedicalVisit>()

    init {
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val now = Date()
        visits.add(
            MedicalVisit(
                id = UUID.randomUUID().toString(),
                doctorId = "1", doctorNombre = "Dr. Carlos Mejía", doctorEspecialidad = "Cardiología",
                fecha = fmt.format(Date(now.time - 86400000)),
                efectiva = true,
                result = VisitResult.Effective(listOf("Atorvastatina", "Omeprazol"), "Doctor receptivo. Interesado en nuevos productos.")
            )
        )
        visits.add(
            MedicalVisit(
                id = UUID.randomUUID().toString(),
                doctorId = "2", doctorNombre = "Dra. Ana Torres", doctorEspecialidad = "Neurología",
                fecha = fmt.format(Date(now.time - 172800000)),
                efectiva = false,
                result = VisitResult.NotEffective("Consultorio cerrado")
            )
        )
        visits.add(
            MedicalVisit(
                id = UUID.randomUUID().toString(),
                doctorId = "5", doctorNombre = "Dr. Roberto Sánchez", doctorEspecialidad = "Medicina General",
                fecha = fmt.format(Date(now.time - 259200000)),
                efectiva = true,
                result = VisitResult.Effective(listOf("Metformina"), "Visita breve pero efectiva.")
            )
        )
    }

    fun getAllDoctors(): List<Doctor> = doctors.toList()

    fun getDoctorById(id: String): Doctor? = doctors.find { it.id == id }

    fun addDoctor(doctor: Doctor) {
        doctors.add(doctor)
    }

    fun requestDoctorDowngrade(doctorId: String) {
        val idx = doctors.indexOfFirst { it.id == doctorId }
        if (idx >= 0) {
            doctors[idx] = doctors[idx].copy(estado = DoctorStatus.PENDIENTE_BAJA)
        }
    }

    fun getAllProducts(): List<Product> = products.toList()

    fun getAllMolecules(): List<String> = molecules.toList()

    fun getAllVisits(): List<MedicalVisit> = visits.toList()

    fun addVisit(visit: MedicalVisit) {
        visits.add(0, visit)
    }

    fun getVisitsByEffectiveness(efectiva: Boolean?): List<MedicalVisit> {
        return if (efectiva == null) visits.toList()
        else visits.filter { it.efectiva == efectiva }
    }

    fun getTodayStats(): Map<String, Int> {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val todayVisits = visits.filter { it.fecha.startsWith(today.substring(0, 5)) }
        return mapOf(
            "total_doctors" to doctors.count { it.estado == DoctorStatus.ACTIVO },
            "visits_done" to todayVisits.size,
            "visits_effective" to todayVisits.count { it.efectiva },
            "visits_not_effective" to todayVisits.count { !it.efectiva }
        )
    }
}
