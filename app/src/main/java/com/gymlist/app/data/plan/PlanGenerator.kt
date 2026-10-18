package com.gymlist.app.data.plan

import com.gymlist.app.data.plan.Pattern.ABDUCTION
import com.gymlist.app.data.plan.Pattern.BICEPS
import com.gymlist.app.data.plan.Pattern.CALVES
import com.gymlist.app.data.plan.Pattern.CHEST_FLY
import com.gymlist.app.data.plan.Pattern.CORE_ANTI
import com.gymlist.app.data.plan.Pattern.CORE_FLEX
import com.gymlist.app.data.plan.Pattern.GLUTE
import com.gymlist.app.data.plan.Pattern.HAM_CURL
import com.gymlist.app.data.plan.Pattern.HINGE
import com.gymlist.app.data.plan.Pattern.H_PULL
import com.gymlist.app.data.plan.Pattern.H_PUSH
import com.gymlist.app.data.plan.Pattern.INCLINE_PUSH
import com.gymlist.app.data.plan.Pattern.LATERAL
import com.gymlist.app.data.plan.Pattern.LUNGE
import com.gymlist.app.data.plan.Pattern.QUAD_ISO
import com.gymlist.app.data.plan.Pattern.REAR_DELT
import com.gymlist.app.data.plan.Pattern.SQUAT
import com.gymlist.app.data.plan.Pattern.TRICEPS
import com.gymlist.app.data.plan.Pattern.V_PULL
import com.gymlist.app.data.plan.Pattern.V_PUSH

/**
 * Convierte las respuestas del cuestionario en un plan semanal: reparte los días, elige un
 * ejercicio para cada hueco y le pone series, repeticiones y descanso.
 *
 * No usa red ni IA, todo sale de [PlanCatalog] y de las reglas de aquí. Es determinista: las
 * mismas respuestas dan siempre el mismo plan.
 */
object PlanGenerator {

    /** Por debajo de esto un día no merece la pena, y se rellena con core, glúteo o gemelo. */
    private const val MIN_EXERCISES = 3

    /**
     * Cuánto se adelanta un hueco de la zona prioritaria. Con 25 entra antes que los accesorios
     * pero no desplaza a los tres básicos del día.
     */
    private const val FOCUS_BOOST = 25

    /** Prioridad de los huecos que la zona prioritaria añade a un día que no los tenía. */
    private const val FOCUS_PRIORITY = 45

    private const val FILLER_PRIORITY = 100

    /**
     * Para variar entre días solo se mira entre las primeras opciones: más abajo de la lista
     * están las de emergencia, y repetir un buen ejercicio es mejor que cambiarlo por uno peor.
     */
    private const val VARIETY_WINDOW = 3

    /** Los dos básicos con los que abre cada plantilla de día llevan orden 10 y 20. */
    private const val MAIN_LIFT_ORDER = 20

    fun generate(answers: PlanAnswers, available: Set<String>): GeneratedPlan {
        val split = splitFor(answers)
        val schedule = schedules.getValue(split.days.size)
        // Los ejercicios ya puestos en otro día: así el día B no repite el press del día A si hay
        // otra opción igual de válida.
        val used = HashSet<String>()

        val days = split.days.mapIndexed { index, template ->
            PlanDay(
                name = "Día ${index + 1} · ${template.name}",
                notes = "Plan de ${split.days.size} días ($schedule). Calienta 5 minutos antes " +
                    "de empezar y haz 1 o 2 series ligeras del primer ejercicio.",
                exercises = buildDay(template, answers, available, used)
            )
        }

        return GeneratedPlan(
            answers = answers,
            title = "Plan de ${split.days.size} días · ${split.title}",
            summary = split.summary,
            schedule = schedule,
            tips = tipsFor(answers),
            days = days
        )
    }

    /**
     * Cambia un ejercicio del plan por la siguiente alternativa de su hueco que no esté ya en ese
     * día. Las series y repeticiones se recalculan, porque la alternativa puede medirse distinto
     * (una plancha va por segundos).
     */
    fun swap(plan: GeneratedPlan, dayIndex: Int, exerciseIndex: Int): GeneratedPlan {
        val day = plan.days.getOrNull(dayIndex) ?: return plan
        val current = day.exercises.getOrNull(exerciseIndex) ?: return plan
        val nextId = nextAlternative(day, current) ?: return plan
        val option = PlanCatalog.option(current.pattern, nextId) ?: return plan

        val replacement = planned(plan.answers, current.pattern, option, current.options, current.heavySlot)
        val exercises = day.exercises.toMutableList().apply { set(exerciseIndex, replacement) }
        val days = plan.days.toMutableList().apply { set(dayIndex, day.copy(exercises = exercises)) }
        return plan.copy(days = days)
    }

    fun canSwap(day: PlanDay, exerciseIndex: Int): Boolean =
        day.exercises.getOrNull(exerciseIndex)?.let { nextAlternative(day, it) } != null

    /** Cómo quedaría la semana, para enseñarlo mientras se eligen los días. */
    fun splitPreview(daysPerWeek: Int): String = when (daysPerWeek) {
        2 -> "Cuerpo completo en cada sesión."
        3 -> "Cuerpo completo en cada sesión. Es lo más recomendable para empezar."
        4 -> "Torso un día y pierna al siguiente."
        5 -> "Torso y pierna, más un día de cuerpo completo."
        else -> "Empuje, tirón y pierna, dos veces por semana."
    }

    // --- Días ---

    private fun buildDay(
        template: DayTemplate,
        answers: PlanAnswers,
        available: Set<String>,
        used: MutableSet<String>
    ): List<PlannedExercise> {
        val target = exercisesPerDay(answers)
        val picks = mutableListOf<Pick>()
        val inDay = HashSet<String>()

        fun tryAdd(slot: Slot) {
            val options = eligible(slot.pattern, answers, available)
            val free = options.filter { it.id !in inDay }
            val best = free.firstOrNull() ?: return
            // Los accesorios se repiten de un día a otro, como en cualquier programa, y en fuerza
            // también los básicos principales: repetirlos es lo que deja subir el peso. El peso
            // muerto es la excepción, porque dos días pesados a la semana cansan demasiado.
            val repeatBest = slot.pattern.kind == Kind.ISOLATION ||
                (answers.goal == Goal.STRENGTH && slot.order <= MAIN_LIFT_ORDER && slot.pattern != HINGE)
            val choice = if (repeatBest) {
                best
            } else {
                // Variar sí, pero sin bajar a peso corporal a quien tiene material para cargar.
                free.take(VARIETY_WINDOW).firstOrNull { it.id !in used && (it.needsGear || !best.needsGear) }
                    ?: best
            }
            picks += Pick(slot, choice, options.map { it.id })
            inDay += choice.id
        }

        // Los huecos van por prioridad: si el tiempo no da para todos, caen los accesorios.
        for (slot in template.slotsFor(answers.focus)) {
            if (picks.size >= target) break
            tryAdd(slot)
        }
        // Con poco material o varias molestias pueden quedar huecos sin ningún ejercicio posible.
        for (pattern in fillers) {
            if (picks.size >= MIN_EXERCISES) break
            if (template.type.allows(pattern)) tryAdd(Slot(pattern, FILLER_PRIORITY, FILLER_PRIORITY))
        }
        used += inDay

        // Orden de ejecución: básicos primero, cuando hay más energía, y el core al final. Va por
        // el orden de la plantilla y no por la prioridad, que la zona prioritaria puede alterar.
        var heavyLeft = if (answers.goal == Goal.STRENGTH) 2 else 0
        val strength = picks
            .sortedWith(compareBy<Pick>({ it.slot.pattern.kind.ordinal }, { it.slot.order }))
            .map { pick ->
                val heavySlot = pick.slot.pattern.kind == Kind.COMPOUND && heavyLeft-- > 0
                planned(answers, pick.slot.pattern, pick.option, pick.optionIds, heavySlot)
            }

        val cardio = if (answers.goal == Goal.FAT_LOSS || answers.goal == Goal.HEALTH) {
            cardioFinisher(answers, available, used)
        } else {
            null
        }
        return strength + listOfNotNull(cardio)
    }

    private fun cardioFinisher(
        answers: PlanAnswers,
        available: Set<String>,
        used: MutableSet<String>
    ): PlannedExercise? {
        val options = eligible(Pattern.CARDIO, answers, available)
        val choice = options.firstOrNull { it.id !in used } ?: options.firstOrNull() ?: return null
        used += choice.id
        return planned(answers, Pattern.CARDIO, choice, options.map { it.id }, heavySlot = false)
    }

    private fun exercisesPerDay(answers: PlanAnswers): Int {
        val base = when (answers.length) {
            SessionLength.SHORT -> 4
            SessionLength.MEDIUM -> 5
            SessionLength.LONG -> 6
            SessionLength.EXTRA_LONG -> 7
        }
        // Fuerza descansa más entre series; perder grasa y salud guardan tiempo para el cardio.
        val adjustment = if (answers.goal == Goal.MUSCLE) 0 else 1
        return (base - adjustment).coerceAtLeast(MIN_EXERCISES)
    }

    /** Opciones válidas para el usuario, en orden de preferencia. */
    private fun eligible(pattern: Pattern, answers: PlanAnswers, available: Set<String>): List<PlanOption> {
        val level = answers.experience
        val gear = answers.gear
        val candidates = PlanCatalog.options[pattern].orEmpty().filter { option ->
            option.id in available &&
                option.isDoableWith(gear) &&
                level >= option.minLevel &&
                level <= option.maxLevel &&
                option.avoid.none { it in answers.limitations }
        }
        // A quien ya entrena le tocan primero los básicos con barra.
        val (promoted, rest) = candidates.partition { option ->
            option.promoteFrom?.let { level >= it } == true
        }
        return promoted + rest
    }

    private fun nextAlternative(day: PlanDay, current: PlannedExercise): String? {
        val taken = day.exercises.mapTo(HashSet()) { it.exerciseId }
        val start = current.options.indexOf(current.exerciseId)
        return (1 until current.options.size)
            .map { offset -> current.options[(start + offset).mod(current.options.size)] }
            .firstOrNull { it !in taken }
    }

    // --- Series, repeticiones y descanso ---

    private fun planned(
        answers: PlanAnswers,
        pattern: Pattern,
        option: PlanOption,
        optionIds: List<String>,
        heavySlot: Boolean
    ): PlannedExercise {
        val level = answers.experience
        val goal = answers.goal

        if (pattern.kind == Kind.CARDIO) {
            val minutes = option.unit == RepUnit.MINUTES
            return PlannedExercise(
                exerciseId = option.id,
                pattern = pattern,
                sets = if (minutes) 1 else if (goal == Goal.FAT_LOSS) level.pick(3, 4, 5) else 3,
                reps = when {
                    !minutes -> "30 s"
                    goal == Goal.FAT_LOSS && level != Experience.BEGINNER -> "15 min"
                    else -> "10 min"
                },
                restSeconds = if (minutes) 0 else 30,
                options = optionIds
            )
        }

        // Las series pesadas solo tienen sentido contando repeticiones con carga.
        val heavy = heavySlot && option.unit == RepUnit.REPS && option.reps == null

        val sets = when (pattern.kind) {
            Kind.COMPOUND -> when (goal) {
                Goal.MUSCLE -> level.pick(3, 3, 4)
                Goal.STRENGTH -> if (heavy) level.pick(3, 4, 5) else 3
                Goal.FAT_LOSS -> level.pick(3, 3, 4)
                Goal.HEALTH -> level.pick(2, 3, 3)
            }
            else -> if (goal == Goal.HEALTH) level.pick(2, 2, 3) else level.pick(2, 3, 3)
        }

        val range = option.reps ?: when (pattern.kind) {
            Kind.COMPOUND -> when (goal) {
                Goal.MUSCLE -> "8-12"
                Goal.STRENGTH -> if (heavy) level.pick("8-10", "6-8", "4-6") else "8-12"
                Goal.FAT_LOSS -> level.pick("12-15", "10-12", "10-12")
                Goal.HEALTH -> "10-12"
            }
            Kind.ISOLATION -> if (goal == Goal.STRENGTH) "10-12" else level.pick("12-15", "10-15", "10-15")
            else -> level.pick("10-12", "12-15", "15-20")
        }
        val hold = level.pick("20-30 s", "30-45 s", "45-60 s")
        val reps = when (option.unit) {
            RepUnit.PER_LEG -> "$range por pierna"
            RepUnit.PER_ARM -> "$range por brazo"
            RepUnit.PER_SIDE -> "$range por lado"
            RepUnit.HOLD -> hold
            RepUnit.HOLD_PER_SIDE -> "$hold por lado"
            else -> range
        }

        val rest = when (pattern.kind) {
            Kind.COMPOUND -> when (goal) {
                Goal.MUSCLE -> level.pick(90, 120, 120)
                Goal.STRENGTH -> if (heavy) level.pick(120, 150, 180) else 90
                Goal.FAT_LOSS -> 60
                Goal.HEALTH -> 75
            }
            Kind.ISOLATION -> when (goal) {
                Goal.STRENGTH -> 75
                Goal.FAT_LOSS -> 45
                else -> 60
            }
            else -> if (goal == Goal.FAT_LOSS) 30 else 45
        }

        return PlannedExercise(
            exerciseId = option.id,
            pattern = pattern,
            sets = sets,
            reps = reps,
            restSeconds = rest,
            options = optionIds,
            heavySlot = heavySlot
        )
    }

    private fun <T> Experience.pick(beginner: T, intermediate: T, advanced: T): T = when (this) {
        Experience.BEGINNER -> beginner
        Experience.INTERMEDIATE -> intermediate
        Experience.ADVANCED -> advanced
    }

    // --- Textos del plan ---

    private val schedules = mapOf(
        2 to "lunes y jueves",
        3 to "lunes, miércoles y viernes",
        4 to "lunes, martes, jueves y viernes",
        5 to "lunes, martes, miércoles, viernes y sábado",
        6 to "de lunes a sábado"
    )

    private fun tipsFor(answers: PlanAnswers): List<String> = buildList {
        // Sin material no hay peso que subir: se progresa con repeticiones y variantes.
        val bodyweight = answers.place == Place.HOME_BODYWEIGHT
        add(
            when (answers.goal) {
                Goal.MUSCLE -> if (bodyweight) {
                    "Acaba cada serie cuando solo te queden 1 o 2 repeticiones más. Cuando llegues al " +
                        "número más alto del rango en todas las series, pasa a una variante más difícil."
                } else {
                    "Elige un peso con el que las dos últimas repeticiones te cuesten. Cuando llegues al " +
                        "número más alto del rango en todas las series, sube un poco el peso."
                }
                Goal.STRENGTH -> if (bodyweight) {
                    "En los dos primeros ejercicios de cada día descansa lo indicado y cuida la técnica. " +
                        "Cuando te sobren repeticiones, pasa a una variante más difícil."
                } else {
                    "En los dos primeros ejercicios de cada día descansa lo indicado y cuida la técnica. " +
                        "Sube el peso poco a poco, semana a semana."
                }
                Goal.FAT_LOSS -> "Para perder grasa lo que más cuenta es la alimentación; las pesas ayudan " +
                    "a no perder músculo por el camino. Por eso los descansos son cortos y cada día acaba con cardio."
                Goal.HEALTH -> "Lo más importante es la constancia: mejor entrenar a menudo y sin dolor que " +
                    "mucho de golpe. Cada día acaba con un poco de cardio suave."
            }
        )
        if (answers.experience == Experience.BEGINNER) {
            add(
                if (bodyweight) {
                    "Las dos primeras semanas ve con calma y fíjate en la técnica más que en las " +
                        "repeticiones. Toca (i) en cada ejercicio para ver cómo se hace."
                } else {
                    "Las dos primeras semanas usa pesos ligeros para aprender bien la técnica. " +
                        "Toca (i) en cada ejercicio para ver cómo se hace."
                }
            )
            if (answers.daysPerWeek >= 5) {
                add(
                    "Para empezar, 3 o 4 días a la semana suelen ser suficientes. " +
                        "Si te notas muy cansado, descansa un día más."
                )
            }
        }
        if (answers.limitations.isNotEmpty()) {
            val zones = answers.limitations.sortedBy { it.ordinal }.map { it.phrase }
            add(
                "El plan evita los ejercicios que suelen cargar ${joinWithAnd(zones)}. " +
                    "Si algo te duele, para y consulta con un profesional."
            )
        }
        val gear = answers.gear
        when {
            answers.place == Place.HOME_BODYWEIGHT && Gear.PULL_UP_BAR !in gear && Gear.BANDS !in gear -> add(
                "Sin barra de dominadas ni bandas elásticas casi no hay ejercicios de espalda para casa. " +
                    "Una barra de puerta o unas bandas te darían muchas más opciones."
            )
            answers.place == Place.HOME_DUMBBELLS && Gear.PULL_UP_BAR !in gear -> add(
                "Con una barra de dominadas de puerta podrías añadir dominadas, el mejor ejercicio " +
                    "de espalda que se puede hacer en casa."
            )
        }
        if (bodyweight && (answers.goal == Goal.FAT_LOSS || answers.goal == Goal.HEALTH)) {
            add("Cuando un ejercicio se te quede fácil, haz más repeticiones o pásate a una variante más difícil.")
        }
    }

    private fun joinWithAnd(items: List<String>): String =
        if (items.size <= 1) items.joinToString() else items.dropLast(1).joinToString(", ") + " y " + items.last()

    // --- Plantillas de los días ---

    private enum class DayType {
        FULL, UPPER, LOWER, PUSH, PULL;

        /** Para no meter un remo en un día de empuje al rellenar o al dar prioridad a una zona. */
        fun allows(pattern: Pattern): Boolean = pattern.kind == Kind.CORE || when (this) {
            FULL -> true
            UPPER -> pattern in pushPatterns || pattern in pullPatterns
            LOWER -> pattern in legPatterns
            PUSH -> pattern in pushPatterns
            PULL -> pattern in pullPatterns
        }
    }

    /**
     * Un hueco de un día. [priority] decide qué entra si no hay tiempo para todo (menor, antes);
     * [order] decide en qué orden se hacen. Empiezan siendo iguales, pero la zona prioritaria
     * adelanta la prioridad sin cambiar el orden: con prioridad en glúteo el hip thrust entra
     * seguro, pero el día sigue abriendo con la sentadilla.
     */
    private data class Slot(val pattern: Pattern, val priority: Int, val order: Int)

    private data class DayTemplate(val name: String, val type: DayType, val slots: List<Slot>) {

        fun slotsFor(focus: Focus): List<Slot> {
            val boosted = focus.patterns.filter { type.allows(it) }
            val adjusted = slots.map { slot ->
                if (slot.pattern in boosted) slot.copy(priority = slot.priority - FOCUS_BOOST) else slot
            }
            val added = boosted
                .filter { pattern -> slots.none { it.pattern == pattern } }
                .map { Slot(it, FOCUS_PRIORITY, FOCUS_PRIORITY) }
            return (adjusted + added).sortedBy { it.priority }
        }
    }

    private class Pick(val slot: Slot, val option: PlanOption, val optionIds: List<String>)

    private class Split(val title: String, val summary: String, val days: List<DayTemplate>)

    private val pushPatterns = setOf(H_PUSH, INCLINE_PUSH, V_PUSH, CHEST_FLY, LATERAL, TRICEPS)
    private val pullPatterns = setOf(H_PULL, V_PULL, REAR_DELT, BICEPS)
    private val legPatterns = setOf(SQUAT, HINGE, LUNGE, GLUTE, HAM_CURL, QUAD_ISO, CALVES, ABDUCTION)

    /** Lo que se puede hacer casi siempre, con o sin material, para no dejar un día vacío. */
    private val fillers = listOf(CORE_ANTI, CORE_FLEX, GLUTE, CALVES, ABDUCTION, SQUAT, H_PUSH)

    private val Focus.patterns: Set<Pattern>
        get() = when (this) {
            Focus.BALANCED -> emptySet()
            Focus.LEGS_GLUTES -> setOf(GLUTE, LUNGE, ABDUCTION, HAM_CURL)
            Focus.CHEST_BACK -> setOf(INCLINE_PUSH, CHEST_FLY, H_PULL, V_PULL)
            Focus.ARMS_SHOULDERS -> setOf(BICEPS, TRICEPS, LATERAL)
            Focus.CORE -> setOf(CORE_ANTI, CORE_FLEX)
        }

    private fun day(name: String, type: DayType, vararg slots: Pair<Pattern, Int>) =
        DayTemplate(name, type, slots.map { (pattern, priority) -> Slot(pattern, priority, priority) })

    // Cada pareja de días (A y B) cambia el patrón principal: sentadilla y bisagra, press plano e
    // inclinado, remo y jalón. Así en dos días se toca todo sin repetir.
    private val fullA = day(
        "Cuerpo completo A", DayType.FULL,
        SQUAT to 10, H_PUSH to 20, H_PULL to 30, HINGE to 40,
        CORE_ANTI to 50, V_PUSH to 60, BICEPS to 70, CALVES to 80
    )
    private val fullB = day(
        "Cuerpo completo B", DayType.FULL,
        HINGE to 10, V_PULL to 20, INCLINE_PUSH to 30, LUNGE to 40,
        CORE_FLEX to 50, LATERAL to 60, TRICEPS to 70, REAR_DELT to 80
    )
    private val fullC = day(
        "Cuerpo completo C", DayType.FULL,
        SQUAT to 10, H_PULL to 20, V_PUSH to 30, GLUTE to 40,
        CORE_ANTI to 50, CHEST_FLY to 60, TRICEPS to 70, HAM_CURL to 80
    )
    private val upperA = day(
        "Torso A", DayType.UPPER,
        H_PUSH to 10, H_PULL to 20, V_PUSH to 30, V_PULL to 40,
        BICEPS to 50, LATERAL to 60, TRICEPS to 70, CORE_ANTI to 80
    )
    private val upperB = day(
        "Torso B", DayType.UPPER,
        V_PULL to 10, INCLINE_PUSH to 20, H_PULL to 30, LATERAL to 40,
        TRICEPS to 50, REAR_DELT to 60, BICEPS to 70, CORE_FLEX to 80
    )
    private val lowerA = day(
        "Pierna A", DayType.LOWER,
        SQUAT to 10, HINGE to 20, LUNGE to 30, CORE_ANTI to 40,
        HAM_CURL to 50, CALVES to 60, QUAD_ISO to 70, ABDUCTION to 80
    )
    private val lowerB = day(
        "Pierna B", DayType.LOWER,
        HINGE to 10, SQUAT to 20, GLUTE to 30, CORE_FLEX to 40,
        QUAD_ISO to 50, HAM_CURL to 60, CALVES to 70, ABDUCTION to 80
    )
    private val pushA = day(
        "Empuje A", DayType.PUSH,
        H_PUSH to 10, V_PUSH to 20, INCLINE_PUSH to 30, LATERAL to 40,
        TRICEPS to 50, CHEST_FLY to 60, CORE_ANTI to 70, TRICEPS to 80
    )
    private val pushB = day(
        "Empuje B", DayType.PUSH,
        V_PUSH to 10, INCLINE_PUSH to 20, H_PUSH to 30, LATERAL to 40,
        CHEST_FLY to 50, TRICEPS to 60, CORE_ANTI to 70, TRICEPS to 80
    )
    private val pullA = day(
        "Tirón A", DayType.PULL,
        V_PULL to 10, H_PULL to 20, REAR_DELT to 30, BICEPS to 40,
        H_PULL to 50, CORE_FLEX to 60, BICEPS to 70
    )
    private val pullB = day(
        "Tirón B", DayType.PULL,
        H_PULL to 10, V_PULL to 20, REAR_DELT to 30, BICEPS to 40,
        V_PULL to 50, CORE_FLEX to 60, BICEPS to 70
    )

    private fun splitFor(answers: PlanAnswers): Split = when (answers.daysPerWeek.coerceIn(2, 6)) {
        2 -> Split(
            "Cuerpo completo",
            "Con 2 días a la semana lo más eficaz es trabajar todo el cuerpo en cada sesión: así " +
                "cada músculo se entrena dos veces por semana.",
            listOf(fullA, fullB)
        )
        3 -> Split(
            "Cuerpo completo",
            "Con 3 días, entrenar todo el cuerpo en cada sesión es lo que mejor funciona, sobre " +
                "todo al empezar. Cada día cambia el énfasis para no repetir siempre lo mismo.",
            listOf(fullA, fullB, fullC)
        )
        4 -> Split(
            "Torso y pierna",
            "Con 4 días se alternan torso y pierna: cada zona se entrena dos veces por semana y " +
                "tiene tiempo para recuperarse.",
            listOf(upperA, lowerA, upperB, lowerB)
        )
        5 -> Split(
            "Torso, pierna y cuerpo completo",
            "Con 5 días se alternan torso y pierna, y el quinto día repasa todo el cuerpo.",
            listOf(upperA, lowerA, upperB, lowerB, fullC.copy(name = "Cuerpo completo"))
        )
        // Sin material no hay suficientes tirones para llenar dos días de espalda.
        else -> if (answers.place == Place.HOME_BODYWEIGHT) {
            Split(
                "Torso, pierna y cuerpo completo",
                "Con 6 días sin material se alternan torso, pierna y cuerpo completo, para no " +
                    "repetir los mismos ejercicios cada día.",
                listOf(upperA, lowerA, fullA, upperB, lowerB, fullB)
            )
        } else {
            Split(
                "Empuje, tirón y pierna",
                "Con 6 días se reparte en empuje (pecho, hombro y tríceps), tirón (espalda y " +
                    "bíceps) y pierna, y cada uno se repite dos veces por semana.",
                listOf(pushA, pullA, lowerA, pushB, pullB, lowerB)
            )
        }
    }
}
