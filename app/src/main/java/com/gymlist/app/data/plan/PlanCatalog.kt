package com.gymlist.app.data.plan

import com.gymlist.app.data.plan.Experience.ADVANCED
import com.gymlist.app.data.plan.Experience.INTERMEDIATE
import com.gymlist.app.data.plan.Gear.BANDS
import com.gymlist.app.data.plan.Gear.BARBELL
import com.gymlist.app.data.plan.Gear.BENCH
import com.gymlist.app.data.plan.Gear.DUMBBELLS
import com.gymlist.app.data.plan.Gear.KETTLEBELL
import com.gymlist.app.data.plan.Gear.MACHINES
import com.gymlist.app.data.plan.Gear.PULL_UP_BAR
import com.gymlist.app.data.plan.Limitation.KNEE
import com.gymlist.app.data.plan.Limitation.LOWER_BACK
import com.gymlist.app.data.plan.Limitation.SHOULDER
import com.gymlist.app.data.plan.Limitation.WRIST
import com.gymlist.app.data.plan.RepUnit.HOLD
import com.gymlist.app.data.plan.RepUnit.HOLD_PER_SIDE
import com.gymlist.app.data.plan.RepUnit.INTERVALS
import com.gymlist.app.data.plan.RepUnit.MINUTES
import com.gymlist.app.data.plan.RepUnit.PER_ARM
import com.gymlist.app.data.plan.RepUnit.PER_LEG
import com.gymlist.app.data.plan.RepUnit.PER_SIDE

/** Cómo se programa un hueco: los básicos llevan más series y descanso que los de aislamiento. */
enum class Kind { COMPOUND, ISOLATION, CORE, CARDIO }

/**
 * Los huecos de una rutina, por patrón de movimiento. El catálogo de RepDB no trae este dato, y
 * es lo que hace falta para montar un día equilibrado: una sentadilla, un empuje, un tirón...
 */
enum class Pattern(val title: String, val kind: Kind) {
    SQUAT("Sentadilla", Kind.COMPOUND),
    HINGE("Bisagra de cadera", Kind.COMPOUND),
    LUNGE("Pierna a una pierna", Kind.COMPOUND),
    GLUTE("Glúteo", Kind.COMPOUND),
    H_PUSH("Pecho", Kind.COMPOUND),
    INCLINE_PUSH("Pecho superior", Kind.COMPOUND),
    V_PUSH("Hombro", Kind.COMPOUND),
    H_PULL("Espalda: remo", Kind.COMPOUND),
    V_PULL("Espalda: jalón", Kind.COMPOUND),
    HAM_CURL("Femoral", Kind.ISOLATION),
    QUAD_ISO("Cuádriceps", Kind.ISOLATION),
    CALVES("Gemelo", Kind.ISOLATION),
    ABDUCTION("Glúteo medio", Kind.ISOLATION),
    CHEST_FLY("Aperturas de pecho", Kind.ISOLATION),
    LATERAL("Hombro lateral", Kind.ISOLATION),
    REAR_DELT("Hombro posterior", Kind.ISOLATION),
    BICEPS("Bíceps", Kind.ISOLATION),
    TRICEPS("Tríceps", Kind.ISOLATION),
    CORE_ANTI("Core", Kind.CORE),
    CORE_FLEX("Abdomen", Kind.CORE),
    CARDIO("Cardio", Kind.CARDIO)
}

/** En qué se mide cada serie. Decide si el objetivo es "10-12", "10-12 por pierna" o "30 s". */
enum class RepUnit { REPS, PER_LEG, PER_ARM, PER_SIDE, HOLD, HOLD_PER_SIDE, MINUTES, INTERVALS }

/**
 * Un ejercicio candidato para un hueco.
 *
 * @property needs material imprescindible. [orNeeds] es la alternativa: la sentadilla goblet se
 *   hace igual con una mancuerna que con una pesa rusa.
 * @property avoid zonas que el ejercicio suele cargar. Si el usuario tiene molestias en alguna,
 *   se descarta.
 * @property maxLevel por encima de este nivel el ejercicio se queda corto (flexiones de rodillas).
 * @property promoteFrom desde este nivel pasa a ser la primera opción: los básicos con barra.
 * @property reps objetivo fijo cuando un rango normal no tiene sentido: casi nadie hace 12
 *   dominadas, así que "8-12" sería frustrante.
 */
data class PlanOption(
    val id: String,
    val needs: Set<Gear> = emptySet(),
    val orNeeds: Set<Gear>? = null,
    val minLevel: Experience = Experience.BEGINNER,
    val maxLevel: Experience = ADVANCED,
    val avoid: Set<Limitation> = emptySet(),
    val unit: RepUnit = RepUnit.REPS,
    val promoteFrom: Experience? = null,
    val reps: String? = null
) {
    fun isDoableWith(gear: Set<Gear>): Boolean =
        gear.containsAll(needs) || (orNeeds != null && gear.containsAll(orNeeds))

    val needsGear: Boolean get() = needs.isNotEmpty() || orNeeds != null
}

/**
 * Qué ejercicios valen para cada hueco, en orden de preferencia para alguien que empieza: primero
 * lo más fácil de aprender y de encontrar. El generador se queda con el primero que encaja con el
 * material, el nivel y las molestias del usuario, y el resto quedan como alternativas.
 *
 * El material está revisado contra las instrucciones de cada ejercicio, no solo contra el campo
 * `equipment`: el remo a una mano, por ejemplo, se hace apoyado en un banco. Los id tienen que
 * existir en `assets/exercises_es.json`; hay un test que lo comprueba.
 */
object PlanCatalog {

    val options: Map<Pattern, List<PlanOption>> = mapOf(
        Pattern.SQUAT to listOf(
            candidate("goblet-squat", DUMBBELLS, orNeeds = setOf(KETTLEBELL), avoid = setOf(KNEE)),
            candidate("leg-press", MACHINES),
            candidate("squat", BARBELL, minLevel = INTERMEDIATE, avoid = setOf(KNEE, LOWER_BACK), promoteFrom = INTERMEDIATE),
            candidate("smith-machine-squat", MACHINES, avoid = setOf(KNEE)),
            candidate("hack-squat", MACHINES, avoid = setOf(KNEE)),
            candidate("front-squat", BARBELL, minLevel = INTERMEDIATE, avoid = setOf(KNEE, WRIST)),
            candidate("db-squat", DUMBBELLS, avoid = setOf(KNEE)),
            candidate("kettlebell-squat", KETTLEBELL, avoid = setOf(KNEE)),
            candidate("bodyweight-squat", avoid = setOf(KNEE)),
            candidate("banded-squat", BANDS, avoid = setOf(KNEE)),
            // Sentarse en una caja o una silla limita la profundidad: es la que mejor toleran
            // unas rodillas con molestias.
            candidate("box-squat"),
            candidate("wall-sit", unit = HOLD)
        ),
        Pattern.HINGE to listOf(
            candidate("dumbbell-romanian-deadlift", DUMBBELLS, avoid = setOf(LOWER_BACK)),
            candidate("deadlift", BARBELL, minLevel = INTERMEDIATE, avoid = setOf(LOWER_BACK), promoteFrom = INTERMEDIATE),
            candidate("romanian-deadlift", BARBELL, avoid = setOf(LOWER_BACK), promoteFrom = INTERMEDIATE),
            candidate("hex-bar-deadlift", BARBELL, minLevel = INTERMEDIATE, avoid = setOf(LOWER_BACK)),
            candidate("kettlebell-deadlift", KETTLEBELL, avoid = setOf(LOWER_BACK)),
            candidate("smith-machine-rdl", MACHINES, avoid = setOf(LOWER_BACK)),
            candidate("single-leg-romanian-deadlift", DUMBBELLS, minLevel = INTERMEDIATE, avoid = setOf(LOWER_BACK), unit = PER_LEG),
            candidate("cable-pull-through", MACHINES),
            candidate("banded-romanian-deadlift", BANDS),
            candidate("bodyweight-good-morning", avoid = setOf(LOWER_BACK)),
            candidate("glute-bridge")
        ),
        Pattern.LUNGE to listOf(
            candidate("reverse-lunge", DUMBBELLS, avoid = setOf(KNEE), unit = PER_LEG),
            candidate("bulgarian-split-squat", DUMBBELLS, BENCH, minLevel = INTERMEDIATE, avoid = setOf(KNEE), unit = PER_LEG),
            candidate("db-lunge", DUMBBELLS, avoid = setOf(KNEE), unit = PER_LEG),
            candidate("kettlebell-reverse-lunge", KETTLEBELL, avoid = setOf(KNEE), unit = PER_LEG),
            candidate("kettlebell-goblet-lunge", KETTLEBELL, avoid = setOf(KNEE), unit = PER_LEG),
            candidate("smith-machine-split-squat", MACHINES, avoid = setOf(KNEE), unit = PER_LEG),
            candidate("barbell-reverse-lunge", BARBELL, minLevel = INTERMEDIATE, avoid = setOf(KNEE), unit = PER_LEG),
            candidate("bodyweight-reverse-lunge", avoid = setOf(KNEE), unit = PER_LEG),
            candidate("split-squat", avoid = setOf(KNEE), unit = PER_LEG),
            candidate("step-ups", avoid = setOf(KNEE), unit = PER_LEG),
            candidate("walking-lunge", minLevel = INTERMEDIATE, avoid = setOf(KNEE), unit = PER_LEG),
            // Sin zancadas, el trabajo a una pierna se hace con la cadera y sin cargar la rodilla.
            candidate("single-leg-glute-bridge", unit = PER_LEG)
        ),
        Pattern.GLUTE to listOf(
            candidate("hip-thrust", BARBELL, BENCH),
            candidate("dumbbell-hip-thrust", DUMBBELLS, BENCH),
            candidate("smith-machine-hip-thrust", MACHINES),
            candidate("plate-loaded-glute-drive", MACHINES),
            candidate("kettlebell-hip-thrust", KETTLEBELL, BENCH),
            candidate("banded-hip-thrust", BANDS),
            candidate("glute-bridge"),
            candidate("banded-glute-bridge", BANDS),
            candidate("single-leg-glute-bridge", unit = PER_LEG)
        ),
        Pattern.H_PUSH to listOf(
            candidate("db-bench-press", DUMBBELLS, BENCH),
            candidate("chest-press-machine", MACHINES),
            candidate("bench-press", BARBELL, BENCH, avoid = setOf(SHOULDER), promoteFrom = INTERMEDIATE),
            candidate("smith-machine-bench-press", MACHINES, avoid = setOf(SHOULDER)),
            candidate("cable-chest-press", MACHINES),
            candidate("dumbbell-floor-press", DUMBBELLS),
            candidate("kettlebell-floor-press", KETTLEBELL),
            candidate("knee-push-ups", maxLevel = Experience.BEGINNER, avoid = setOf(WRIST)),
            candidate("push-up", avoid = setOf(WRIST)),
            candidate("incline-push-ups"),
            candidate("wall-push-ups")
        ),
        Pattern.INCLINE_PUSH to listOf(
            candidate("incline-db-press", DUMBBELLS, BENCH),
            candidate("smith-machine-incline-bench-press", MACHINES, avoid = setOf(SHOULDER)),
            candidate("incline-bench-press", BARBELL, BENCH, avoid = setOf(SHOULDER), promoteFrom = INTERMEDIATE),
            candidate("dumbbell-floor-press", DUMBBELLS),
            candidate("decline-push-up", minLevel = INTERMEDIATE, avoid = setOf(WRIST, SHOULDER)),
            candidate("incline-push-ups"),
            candidate("push-up", avoid = setOf(WRIST))
        ),
        Pattern.V_PUSH to listOf(
            candidate("dumbbell-shoulder-press", DUMBBELLS, avoid = setOf(SHOULDER)),
            candidate("machine-shoulder-press", MACHINES, avoid = setOf(SHOULDER)),
            candidate("ohp", BARBELL, minLevel = INTERMEDIATE, avoid = setOf(SHOULDER, LOWER_BACK), promoteFrom = INTERMEDIATE),
            candidate("seated-db-press", DUMBBELLS, BENCH, avoid = setOf(SHOULDER)),
            candidate("arnold-press", DUMBBELLS, minLevel = INTERMEDIATE, avoid = setOf(SHOULDER)),
            candidate("one-arm-kettlebell-shoulder-press", KETTLEBELL, avoid = setOf(SHOULDER), unit = PER_ARM),
            // Se empuja en diagonal y no por encima de la cabeza: el más llevadero para el hombro.
            candidate("landmine-press", BARBELL),
            candidate("pike-push-ups", minLevel = INTERMEDIATE, avoid = setOf(SHOULDER, WRIST), reps = "6-10")
        ),
        Pattern.H_PULL to listOf(
            candidate("seated-cable-row", MACHINES),
            candidate("chest-supported-machine-row", MACHINES),
            candidate("single-arm-db-row", DUMBBELLS, BENCH, unit = PER_ARM),
            candidate("barbell-row", BARBELL, minLevel = INTERMEDIATE, avoid = setOf(LOWER_BACK), promoteFrom = INTERMEDIATE),
            candidate("chest-supported-db-row", DUMBBELLS, BENCH),
            candidate("one-arm-kettlebell-row", KETTLEBELL, BENCH, unit = PER_ARM),
            candidate("bent-over-db-row", DUMBBELLS, avoid = setOf(LOWER_BACK)),
            candidate("inverted-row", BARBELL, minLevel = INTERMEDIATE),
            // En casa sin barra ni mancuernas no hay remos de verdad. Estas dos son lo más
            // parecido; el plan avisa de que con una barra o unas bandas se haría mejor.
            candidate("band-pull-apart", BANDS),
            candidate("superman", avoid = setOf(LOWER_BACK))
        ),
        Pattern.V_PULL to listOf(
            candidate("lat-pulldown", MACHINES),
            candidate("assisted-pull-ups", MACHINES, maxLevel = INTERMEDIATE),
            candidate("band-assisted-pull-ups", PULL_UP_BAR, BANDS, maxLevel = Experience.BEGINNER, reps = "6-10"),
            candidate("negative-pull-ups", PULL_UP_BAR, maxLevel = Experience.BEGINNER, reps = "3-5"),
            candidate("pull-up", PULL_UP_BAR, minLevel = INTERMEDIATE, promoteFrom = ADVANCED, reps = "5-8"),
            candidate("chin-ups", PULL_UP_BAR, minLevel = INTERMEDIATE, reps = "5-8"),
            candidate("neutral-grip-pull-ups", PULL_UP_BAR, minLevel = INTERMEDIATE, reps = "5-8"),
            candidate("v-bar-lat-pulldown", MACHINES),
            candidate("close-grip-lat-pulldown", MACHINES),
            candidate("db-pullover", DUMBBELLS, BENCH, avoid = setOf(SHOULDER)),
            candidate("floor-kettlebell-pullover", KETTLEBELL, avoid = setOf(SHOULDER))
        ),
        Pattern.HAM_CURL to listOf(
            candidate("seated-leg-curl", MACHINES),
            candidate("leg-curl", MACHINES),
            candidate("banded-standing-curl", BANDS, unit = PER_LEG)
        ),
        Pattern.QUAD_ISO to listOf(
            candidate("leg-extension", MACHINES, avoid = setOf(KNEE)),
            candidate("banded-terminal-knee-extension", BANDS, unit = PER_LEG),
            candidate("wall-sit", unit = HOLD),
            candidate("reverse-nordic-curl", minLevel = INTERMEDIATE, avoid = setOf(KNEE))
        ),
        Pattern.CALVES to listOf(
            candidate("standing-calf-raise", MACHINES),
            candidate("seated-calf-raise", MACHINES),
            candidate("machine-calf-raise", MACHINES),
            candidate("smith-machine-calf-raise", MACHINES),
            candidate("dumbbell-calf-raise", DUMBBELLS),
            candidate("bodyweight-calf-raise"),
            candidate("single-leg-calf-raise", unit = PER_LEG)
        ),
        Pattern.ABDUCTION to listOf(
            candidate("hip-abduction", MACHINES),
            candidate("banded-lateral-walk", BANDS, unit = PER_SIDE),
            candidate("banded-clamshell", BANDS, unit = PER_SIDE),
            candidate("side-lying-hip-abduction", unit = PER_SIDE),
            candidate("clamshells", unit = PER_SIDE),
            candidate("fire-hydrant", avoid = setOf(WRIST), unit = PER_SIDE)
        ),
        Pattern.CHEST_FLY to listOf(
            candidate("machine-chest-fly", MACHINES),
            candidate("pec-deck", MACHINES, avoid = setOf(SHOULDER)),
            candidate("cable-fly", MACHINES, avoid = setOf(SHOULDER)),
            candidate("db-fly", DUMBBELLS, BENCH, avoid = setOf(SHOULDER)),
            candidate("incline-dumbbell-fly", DUMBBELLS, BENCH, avoid = setOf(SHOULDER))
        ),
        Pattern.LATERAL to listOf(
            candidate("lateral-raise", DUMBBELLS),
            candidate("cable-lateral-raise", MACHINES),
            candidate("plate-loaded-lateral-raise", MACHINES),
            candidate("seated-dumbbell-lateral-raise", DUMBBELLS, BENCH),
            candidate("side-lying-lateral-raise", DUMBBELLS)
        ),
        Pattern.REAR_DELT to listOf(
            candidate("face-pull", MACHINES),
            candidate("machine-reverse-fly", MACHINES),
            candidate("rear-delt-fly", DUMBBELLS),
            candidate("dumbbell-reverse-fly", DUMBBELLS),
            candidate("band-pull-apart", BANDS)
        ),
        Pattern.BICEPS to listOf(
            candidate("bicep-curl", DUMBBELLS),
            candidate("cable-curl", MACHINES),
            candidate("hammer-curl", DUMBBELLS),
            candidate("ez-bar-curl", BARBELL),
            candidate("machine-bicep-curl", MACHINES),
            candidate("barbell-curl", BARBELL, avoid = setOf(WRIST)),
            candidate("kettlebell-hammer-curl", KETTLEBELL),
            candidate("chin-ups", PULL_UP_BAR, minLevel = INTERMEDIATE, reps = "5-8")
        ),
        Pattern.TRICEPS to listOf(
            candidate("tricep-pushdown", MACHINES),
            candidate("overhead-tricep-extension", DUMBBELLS, avoid = setOf(SHOULDER)),
            candidate("tricep-kickback", DUMBBELLS),
            candidate("v-bar-tricep-pushdown", MACHINES),
            candidate("machine-triceps-extension", MACHINES),
            candidate("skull-crusher", BARBELL, BENCH, minLevel = INTERMEDIATE),
            candidate("cable-overhead-tricep-extension", MACHINES, avoid = setOf(SHOULDER)),
            candidate("assisted-dips", MACHINES, avoid = setOf(SHOULDER)),
            candidate("dumbbell-tricep-extension", DUMBBELLS, avoid = setOf(SHOULDER), unit = PER_ARM),
            // Con una silla firme vale igual que con un banco.
            candidate("bench-dips", avoid = setOf(SHOULDER, WRIST)),
            candidate("diamond-push-ups", minLevel = INTERMEDIATE, avoid = setOf(WRIST)),
            candidate("close-grip-push-ups", minLevel = INTERMEDIATE, avoid = setOf(WRIST))
        ),
        Pattern.CORE_ANTI to listOf(
            candidate("plank", unit = HOLD),
            candidate("dead-bug", unit = PER_SIDE),
            candidate("cable-pallof-press", MACHINES, unit = PER_SIDE),
            candidate("side-plank", unit = HOLD_PER_SIDE),
            candidate("bird-dog", avoid = setOf(WRIST), unit = PER_SIDE),
            candidate("bear-plank-hold", avoid = setOf(WRIST), unit = HOLD),
            candidate("hollow-body-hold", minLevel = INTERMEDIATE, avoid = setOf(LOWER_BACK), unit = HOLD)
        ),
        Pattern.CORE_FLEX to listOf(
            candidate("machine-seated-crunch", MACHINES),
            candidate("crunches"),
            candidate("captains-chair-knee-raise", MACHINES),
            candidate("hanging-knee-raise", PULL_UP_BAR),
            candidate("reverse-crunches"),
            candidate("cable-crunch", MACHINES, minLevel = INTERMEDIATE),
            candidate("bicycle-crunch"),
            candidate("lying-leg-raise", avoid = setOf(LOWER_BACK)),
            // Para quien tiene la zona lumbar delicada: abdomen sin flexionar la columna.
            candidate("dead-bug-hold", unit = HOLD)
        ),
        Pattern.CARDIO to listOf(
            candidate("incline-treadmill-walk", MACHINES, unit = MINUTES),
            candidate("stationary-bike", MACHINES, unit = MINUTES),
            candidate("elliptical-trainer", MACHINES, unit = MINUTES),
            candidate("rowing-machine", MACHINES, avoid = setOf(LOWER_BACK), unit = MINUTES),
            candidate("jumping-jacks", avoid = setOf(KNEE), unit = INTERVALS),
            candidate("mountain-climbers", avoid = setOf(WRIST), unit = INTERVALS),
            candidate("high-knees", avoid = setOf(KNEE), unit = INTERVALS),
            candidate("walking", unit = MINUTES)
        )
    )

    fun option(pattern: Pattern, exerciseId: String): PlanOption? =
        options[pattern]?.firstOrNull { it.id == exerciseId }

    /** Todos los id que usa el generador. */
    val allIds: Set<String> get() = options.values.flatten().mapTo(HashSet()) { it.id }

    private fun candidate(
        id: String,
        vararg needs: Gear,
        orNeeds: Set<Gear>? = null,
        minLevel: Experience = Experience.BEGINNER,
        maxLevel: Experience = ADVANCED,
        avoid: Set<Limitation> = emptySet(),
        unit: RepUnit = RepUnit.REPS,
        promoteFrom: Experience? = null,
        reps: String? = null
    ) = PlanOption(
        id = id,
        needs = needs.toSet(),
        orNeeds = orNeeds,
        minLevel = minLevel,
        maxLevel = maxLevel,
        avoid = avoid,
        unit = unit,
        promoteFrom = promoteFrom,
        reps = reps
    )
}
