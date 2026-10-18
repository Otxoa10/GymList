package com.gymlist.app.data.plan

/*
 * Respuestas del cuestionario con el que se genera un plan, y el plan que sale de ellas.
 *
 * Los textos de cada opción viven junto a su valor para que la pantalla y el generador hablen
 * exactamente de lo mismo.
 */

enum class Goal(val title: String, val detail: String) {
    MUSCLE("Ganar músculo", "Más masa muscular y mejor figura"),
    STRENGTH("Ganar fuerza", "Mover más peso en los ejercicios básicos"),
    FAT_LOSS("Perder grasa", "Pesas con descansos cortos y algo de cardio"),
    HEALTH("Salud y forma", "Moverte mejor y tener más energía")
}

enum class Experience(val title: String, val detail: String) {
    BEGINNER("Estoy empezando", "Nunca he entrenado o llevo menos de 6 meses"),
    INTERMEDIATE("Ya entreno", "Entre 6 meses y 2 años; conozco los ejercicios básicos"),
    ADVANCED("Tengo experiencia", "Más de 2 años entrenando con constancia")
}

enum class SessionLength(val title: String, val detail: String) {
    SHORT("30 minutos", "3 o 4 ejercicios"),
    MEDIUM("45 minutos", "4 o 5 ejercicios"),
    LONG("1 hora", "5 o 6 ejercicios"),
    EXTRA_LONG("Más de 1 hora", "6 o 7 ejercicios")
}

enum class Place(val title: String, val detail: String) {
    GYM("En el gimnasio", "Con máquinas, poleas, barras y mancuernas"),
    HOME_DUMBBELLS("En casa con mancuernas", "O en un gimnasio pequeño"),
    HOME_BODYWEIGHT("En casa sin material", "Solo con tu peso corporal")
}

/**
 * Material, con el detalle que importa para elegir ejercicios. No es el campo `equipment` del
 * catálogo: allí el press de banca con mancuernas solo dice "mancuernas", pero sin banco no se
 * puede hacer.
 */
enum class Gear(val title: String) {
    MACHINES("Máquinas y poleas"),
    BARBELL("Barra y discos"),
    DUMBBELLS("Mancuernas"),
    BENCH("Banco"),
    PULL_UP_BAR("Barra de dominadas"),
    BANDS("Bandas elásticas"),
    KETTLEBELL("Pesa rusa");

    companion object {
        /** Lo que se pregunta a quien entrena en casa, además de las mancuernas. */
        val homeExtras: List<Gear> = listOf(BENCH, PULL_UP_BAR, BANDS, KETTLEBELL)
    }
}

/** Zonas con molestias. [phrase] es como se nombran dentro de una frase ("cargar los hombros"). */
enum class Limitation(val title: String, val phrase: String) {
    SHOULDER("Hombros", "los hombros"),
    LOWER_BACK("Zona lumbar", "la zona lumbar"),
    KNEE("Rodillas", "las rodillas"),
    WRIST("Muñecas", "las muñecas")
}

enum class Focus(val title: String, val detail: String) {
    BALANCED("Equilibrado", "Todo el cuerpo por igual"),
    LEGS_GLUTES("Piernas y glúteos", "Más trabajo de glúteo y pierna"),
    CHEST_BACK("Pecho y espalda", "Más ejercicios de torso"),
    ARMS_SHOULDERS("Brazos y hombros", "Más bíceps, tríceps y hombro"),
    CORE("Abdomen", "Más trabajo de core")
}

data class PlanAnswers(
    val goal: Goal,
    val experience: Experience,
    /** De 2 a 6. */
    val daysPerWeek: Int,
    val length: SessionLength,
    val place: Place,
    /** Material que hay además de lo propio del sitio. En el gimnasio no se tiene en cuenta. */
    val extraGear: Set<Gear> = emptySet(),
    val limitations: Set<Limitation> = emptySet(),
    val focus: Focus = Focus.BALANCED
) {
    val gear: Set<Gear>
        get() = when (place) {
            Place.GYM -> Gear.entries.toSet()
            Place.HOME_DUMBBELLS -> extraGear + Gear.DUMBBELLS
            Place.HOME_BODYWEIGHT -> extraGear
        }
}

data class GeneratedPlan(
    val answers: PlanAnswers,
    /** "Plan de 3 días · Cuerpo completo". */
    val title: String,
    /** Por qué se reparte así la semana. */
    val summary: String,
    /** Días de la semana sugeridos: "lunes, miércoles y viernes". */
    val schedule: String,
    /** Consejos y avisos según las respuestas: progresión, molestias, material que falta. */
    val tips: List<String>,
    val days: List<PlanDay>
)

data class PlanDay(
    /** Nombre con el que se guarda la rutina: "Día 1 · Cuerpo completo A". */
    val name: String,
    val notes: String,
    val exercises: List<PlannedExercise>
)

data class PlannedExercise(
    val exerciseId: String,
    /** El hueco que ocupa en el día. Es lo que se mantiene al cambiar el ejercicio por otro. */
    val pattern: Pattern,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    /**
     * Ejercicios que valen para este hueco con el material, el nivel y las molestias del usuario,
     * en orden de preferencia. Incluye el actual; el botón de cambiar va pasando por ellos.
     */
    val options: List<String>,
    /** Si el hueco se programa con repeticiones de fuerza (los dos primeros básicos del día). */
    val heavySlot: Boolean = false
)
