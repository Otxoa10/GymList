package com.gymlist.app.data.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class PlanGeneratorTest {

    private val everything = PlanCatalog.allIds

    private val gymBeginner = PlanAnswers(
        goal = Goal.MUSCLE,
        experience = Experience.BEGINNER,
        daysPerWeek = 3,
        length = SessionLength.MEDIUM,
        place = Place.GYM
    )

    @Test
    fun `los ejercicios de la tabla existen en el catálogo`() {
        val file = File("src/main/assets/exercises_es.json")
        // El catálogo no está en el repositorio público por la licencia de RepDB.
        assumeTrue("No está $file, no hay nada con lo que comparar", file.exists())
        val ids = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
        val missing = PlanCatalog.allIds - ids
        assertTrue("No están en el catálogo: $missing", missing.isEmpty())
    }

    @Test
    fun `cualquier combinación de respuestas da un plan válido`() {
        var checked = 0
        for (answers in allAnswers()) {
            val plan = PlanGenerator.generate(answers, everything)
            assertEquals("Días: $answers", answers.daysPerWeek, plan.days.size)
            plan.days.forEach { day ->
                val strength = day.exercises.filter { it.pattern != Pattern.CARDIO }
                assertTrue("${day.name} con ${strength.size} ejercicios: $answers", strength.size in 3..7)
                val ids = day.exercises.map { it.exerciseId }
                assertEquals("Ejercicio repetido en ${day.name}: $answers", ids.size, ids.toSet().size)
                day.exercises.forEach { assertRespects(answers, it) }
            }
            checked++
        }
        assertTrue("Solo se han probado $checked combinaciones", checked > 50_000)
    }

    @Test
    fun `cambiar un ejercicio pone otro del mismo hueco sin repetir en el día`() {
        val plan = PlanGenerator.generate(gymBeginner, everything)
        val before = plan.days[0].exercises[0]
        assertTrue(PlanGenerator.canSwap(plan.days[0], 0))

        val swapped = PlanGenerator.swap(plan, 0, 0)
        val after = swapped.days[0].exercises[0]

        assertNotEquals(before.exerciseId, after.exerciseId)
        assertEquals(before.pattern, after.pattern)
        assertTrue(after.exerciseId in before.options)
        val ids = swapped.days[0].exercises.map { it.exerciseId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `cambiar una plancha por un ejercicio de repeticiones recalcula el objetivo`() {
        val plan = PlanGenerator.generate(gymBeginner, everything)
        val (dayIndex, exerciseIndex) = plan.days.withIndex().firstNotNullOf { (dayIndex, day) ->
            day.exercises.indexOfFirst { it.exerciseId == "plank" }
                .takeIf { it >= 0 }
                ?.let { dayIndex to it }
        }
        assertTrue(plan.days[dayIndex].exercises[exerciseIndex].reps.endsWith(" s"))

        val after = PlanGenerator.swap(plan, dayIndex, exerciseIndex).days[dayIndex].exercises[exerciseIndex]
        assertEquals("dead-bug", after.exerciseId)
        assertTrue(after.reps.endsWith("por lado"))
    }

    @Test
    fun `fuerza baja las repeticiones solo en los dos primeros básicos`() {
        val answers = gymBeginner.copy(goal = Goal.STRENGTH, experience = Experience.INTERMEDIATE)
        val day = PlanGenerator.generate(answers, everything).days[0]
        assertEquals("6-8", day.exercises[0].reps)
        assertEquals("6-8", day.exercises[1].reps)
        assertTrue(day.exercises.drop(2).none { it.reps == "6-8" })
    }

    @Test
    fun `a quien ya entrena le tocan los básicos con barra`() {
        val answers = gymBeginner.copy(experience = Experience.INTERMEDIATE)
        val first = PlanGenerator.generate(answers, everything).days[0].exercises[0]
        assertEquals("squat", first.exerciseId)
    }

    @Test
    fun `perder grasa acaba cada día con cardio y sin descanso`() {
        val plan = PlanGenerator.generate(gymBeginner.copy(goal = Goal.FAT_LOSS), everything)
        plan.days.forEach { day ->
            val last = day.exercises.last()
            assertEquals(Pattern.CARDIO, last.pattern)
            assertEquals(0, last.restSeconds)
        }
    }

    @Test
    fun `los días del plan no repiten la sentadilla si hay alternativas`() {
        val plan = PlanGenerator.generate(gymBeginner, everything)
        val squats = plan.days.flatMap { day -> day.exercises.filter { it.pattern == Pattern.SQUAT } }
        assertEquals(2, squats.size)
        assertNotEquals(squats[0].exerciseId, squats[1].exerciseId)
    }

    @Test
    fun `en casa sin barra ni bandas avisa de que falta material para la espalda`() {
        val plan = PlanGenerator.generate(gymBeginner.copy(place = Place.HOME_BODYWEIGHT), everything)
        assertTrue(plan.tips.any { "barra de dominadas" in it })
    }

    @Test
    fun `con molestias se avisa de qué zonas se han evitado`() {
        val answers = gymBeginner.copy(limitations = setOf(Limitation.KNEE, Limitation.SHOULDER))
        val tip = PlanGenerator.generate(answers, everything).tips.first { "evita" in it }
        assertTrue(tip, "los hombros y las rodillas" in tip)
    }

    @Test
    fun `las mismas respuestas dan el mismo plan`() {
        assertEquals(
            PlanGenerator.generate(gymBeginner, everything),
            PlanGenerator.generate(gymBeginner, everything)
        )
    }

    @Test
    fun `sin catálogo no falla y deja los días vacíos`() {
        val plan = PlanGenerator.generate(gymBeginner, emptySet())
        assertEquals(3, plan.days.size)
        assertTrue(plan.days.all { it.exercises.isEmpty() })
    }

    private fun assertRespects(answers: PlanAnswers, exercise: PlannedExercise) {
        val context = "${exercise.exerciseId} (${exercise.pattern}) en $answers"
        val option = PlanCatalog.option(exercise.pattern, exercise.exerciseId)
        assertNotNull("No está en su hueco: $context", option)
        option!!
        assertTrue("Falta material: $context", option.isDoableWith(answers.gear))
        assertTrue("Nivel: $context", answers.experience in option.minLevel..option.maxLevel)
        assertTrue("Molestias: $context", option.avoid.none { it in answers.limitations })
        assertTrue("Series: $context", exercise.sets > 0)
        assertTrue("Repeticiones: $context", exercise.reps.isNotBlank())
        assertTrue("Alternativas: $context", exercise.exerciseId in exercise.options)
    }

    /**
     * Todas las respuestas posibles, salvo que de las molestias y del material de casa se prueba
     * ninguno, cada uno por separado y todos a la vez: son los casos que cambian algo.
     */
    private fun allAnswers(): Sequence<PlanAnswers> = sequence {
        val limitationSets = listOf(emptySet<Limitation>()) +
            Limitation.entries.map { setOf(it) } +
            listOf(Limitation.entries.toSet())
        val homeGearSets = listOf(emptySet<Gear>(), Gear.homeExtras.toSet()) + Gear.homeExtras.map { setOf(it) }

        for (place in Place.entries) {
            val gearSets = if (place == Place.GYM) listOf(emptySet()) else homeGearSets
            for (extraGear in gearSets) for (goal in Goal.entries) for (experience in Experience.entries)
                for (days in 2..6) for (length in SessionLength.entries) for (limitations in limitationSets)
                    for (focus in Focus.entries) {
                        yield(PlanAnswers(goal, experience, days, length, place, extraGear, limitations, focus))
                    }
        }
    }
}
