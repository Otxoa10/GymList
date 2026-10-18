package com.gymlist.app.data.repo

/**
 * El catálogo trae las categorías en inglés y en snake_case ("upper_legs", "ez_bar"). Aquí se
 * traducen a lo que se ve en pantalla. Si apareciera un valor nuevo que no esté en estos mapas,
 * se muestra el original con guiones bajos cambiados por espacios, para no dejar huecos.
 */
object Taxonomy {

    /** Zonas del cuerpo, tal y como se agrupan en el listado de ejercicios. */
    val bodyParts: Map<String, String> = mapOf(
        "upper_legs" to "Pierna",
        "lower_legs" to "Gemelo",
        "back" to "Espalda",
        "chest" to "Pecho",
        "shoulders" to "Hombro",
        "upper_arms" to "Brazo",
        "lower_arms" to "Antebrazo",
        "core" to "Core",
        "full_body" to "Cuerpo completo"
    )

    /** Orden en el que se muestran las zonas: de los grupos grandes a los pequeños. */
    val bodyPartOrder: List<String> = listOf(
        "chest", "back", "shoulders", "upper_arms", "lower_arms",
        "upper_legs", "lower_legs", "core", "full_body"
    )

    val difficulties: Map<String, String> = mapOf(
        "beginner" to "Principiante",
        "intermediate" to "Intermedio",
        "advanced" to "Avanzado"
    )

    val categories: Map<String, String> = mapOf(
        "strength" to "Fuerza",
        "cardio" to "Cardio",
        "stretching" to "Estiramiento",
        "plyometrics" to "Pliometría",
        "olympic" to "Halterofilia"
    )

    val equipment: Map<String, String> = mapOf(
        "ab_crunch_machine" to "Máquina de abdominales",
        "ab_wheel" to "Rueda abdominal",
        "air_bike" to "Bicicleta de aire",
        "assisted_pullup_machine" to "Máquina de dominadas asistidas",
        "back_extension_machine" to "Banco de lumbares",
        "barbell" to "Barra",
        "battle_rope" to "Cuerda de batalla",
        "bicep_curl_machine" to "Máquina de curl de bíceps",
        "cable" to "Polea",
        "chest_fly_machine" to "Máquina de aperturas",
        "chest_press_machine" to "Máquina de press de pecho",
        "climbing_rope" to "Cuerda de trepar",
        "dip_machine" to "Máquina de fondos",
        "dip_station" to "Paralelas",
        "donkey_calf_raise_machine" to "Máquina de gemelo a la burra",
        "dumbbell" to "Mancuernas",
        "elliptical" to "Elíptica",
        "ez_bar" to "Barra Z",
        "flat_bench" to "Banco plano",
        "glute_ham_developer" to "Banco de glúteo-femoral",
        "hack_squat" to "Máquina hack squat",
        "hip_abduction_machine" to "Máquina de abductores",
        "hip_adduction_machine" to "Máquina de aductores",
        "hip_thrust_machine" to "Máquina de hip thrust",
        "jump_rope" to "Comba",
        "kettlebell" to "Pesa rusa",
        "lat_pulldown_machine" to "Máquina de jalón",
        "leg_curl" to "Máquina de femoral",
        "leg_extension" to "Máquina de cuádriceps",
        "leg_press" to "Prensa de piernas",
        "loop_band" to "Banda circular",
        "pec_deck" to "Contractor de pecho",
        "plate_loaded_dip_machine" to "Máquina de fondos de discos",
        "plate_loaded_lateral_raise_machine" to "Máquina de elevaciones laterales de discos",
        "plates" to "Discos",
        "plyo_box" to "Cajón pliométrico",
        "preacher_curl_machine" to "Banco Scott",
        "pull_up_bar" to "Barra de dominadas",
        "resistance_band" to "Banda elástica",
        "reverse_fly_machine" to "Máquina de deltoides posterior",
        "rings" to "Anillas",
        "rower" to "Remoergómetro",
        "seated_calf_raise_machine" to "Máquina de gemelo sentado",
        "seated_row_machine" to "Máquina de remo sentado",
        "shoulder_press_machine" to "Máquina de press de hombro",
        "shrug_machine" to "Máquina de encogimientos",
        "ski_erg" to "Ski erg",
        "slam_ball" to "Balón medicinal",
        "sled" to "Trineo",
        "smith_machine" to "Máquina Smith",
        "stability_ball" to "Fitball",
        "stair_climber" to "Escaladora",
        "standing_calf_raise_machine" to "Máquina de gemelo de pie",
        "stationary_bike" to "Bicicleta estática",
        "suspension_trainer" to "TRX",
        "trap_bar" to "Barra hexagonal",
        "treadmill" to "Cinta de correr",
        "tricep_extension_machine" to "Máquina de tríceps",
        "wrist_roller" to "Rodillo de muñeca"
    )

    /** 195 ejercicios del catálogo no usan material; el campo viene vacío. */
    const val BODYWEIGHT = "Peso corporal"

    val muscles: Map<String, String> = mapOf(
        "abductors" to "Abductores",
        "adductors" to "Aductores",
        "anterior_deltoid" to "Deltoides anterior",
        "biceps_brachii" to "Bíceps",
        "brachialis" to "Braquial",
        "brachioradialis" to "Braquiorradial",
        "erector_spinae" to "Erectores espinales",
        "forearm_extensors" to "Extensores del antebrazo",
        "forearm_flexors" to "Flexores del antebrazo",
        "forearms" to "Antebrazos",
        "gastrocnemius" to "Gemelos",
        "gluteus_maximus" to "Glúteo mayor",
        "gluteus_medius" to "Glúteo medio",
        "hamstrings" to "Isquiotibiales",
        "hip_flexors" to "Flexores de cadera",
        "lateral_deltoid" to "Deltoides lateral",
        "latissimus_dorsi" to "Dorsal ancho",
        "obliques" to "Oblicuos",
        "pectoralis_major" to "Pectoral mayor",
        "posterior_deltoid" to "Deltoides posterior",
        "quadratus_lumborum" to "Cuadrado lumbar",
        "quadriceps" to "Cuádriceps",
        "rectus_abdominis" to "Recto abdominal",
        "rhomboids" to "Romboides",
        "serratus_anterior" to "Serrato anterior",
        "soleus" to "Sóleo",
        "supraspinatus" to "Supraespinoso",
        "tibialis_anterior" to "Tibial anterior",
        "transverse_abdominis" to "Transverso abdominal",
        "trapezius" to "Trapecio",
        "triceps_brachii" to "Tríceps"
    )

    fun bodyPart(raw: String): String = bodyParts[raw] ?: raw.humanize()

    fun difficulty(raw: String): String = difficulties[raw] ?: raw.humanize()

    fun category(raw: String): String = categories[raw] ?: raw.humanize()

    fun equipment(raw: String): String =
        if (raw.isBlank()) BODYWEIGHT else equipment[raw] ?: raw.humanize()

    fun muscle(raw: String): String = muscles[raw] ?: raw.humanize()

    private fun String.humanize(): String =
        replace('_', ' ').replaceFirstChar { it.uppercase() }
}
