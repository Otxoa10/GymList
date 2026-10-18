package com.gymlist.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymlist.app.data.repo.GymRepository
import com.gymlist.app.ui.about.AboutScreen
import com.gymlist.app.ui.calendar.CalendarScreen
import com.gymlist.app.ui.calendar.CalendarViewModel
import com.gymlist.app.ui.calendar.SessionDetailScreen
import com.gymlist.app.ui.calendar.SessionDetailViewModel
import com.gymlist.app.ui.home.HomeScreen
import com.gymlist.app.ui.home.HomeViewModel
import com.gymlist.app.ui.library.LibraryScreen
import com.gymlist.app.ui.library.LibraryViewModel
import com.gymlist.app.ui.plan.PlanWizardScreen
import com.gymlist.app.ui.plan.PlanWizardViewModel
import com.gymlist.app.ui.routines.RoutineDetailScreen
import com.gymlist.app.ui.routines.RoutineDetailViewModel
import com.gymlist.app.ui.routines.RoutinesScreen
import com.gymlist.app.ui.routines.RoutinesViewModel
import com.gymlist.app.ui.workout.RestTimerBar
import com.gymlist.app.ui.workout.WorkoutScreen
import com.gymlist.app.ui.workout.WorkoutViewModel

object Routes {
    const val HOME = "home"
    const val ROUTINES = "routines"
    const val CALENDAR = "calendar"
    const val LIBRARY = "library"
    const val WORKOUT = "workout"
    const val ABOUT = "about"
    const val PLAN = "plan"
    const val ROUTINE_DETAIL = "routines/{routineId}"
    const val SESSION_DETAIL = "session/{sessionId}"

    fun routineDetail(routineId: Long) = "routines/$routineId"
    fun sessionDetail(sessionId: Long) = "session/$sessionId"
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    Tab(Routes.HOME, "Hoy", Icons.Default.Home),
    Tab(Routes.ROUTINES, "Rutinas", Icons.AutoMirrored.Filled.ListAlt),
    Tab(Routes.CALENDAR, "Seguimiento", Icons.Default.CalendarMonth),
    Tab(Routes.LIBRARY, "Ejercicios", Icons.Default.FitnessCenter)
)

/**
 * Armazón de la app.
 *
 * El WorkoutViewModel se crea aquí, a nivel de Activity, y no dentro de la pantalla de
 * entrenamiento: así el temporizador de descanso sigue contando mientras miras el calendario o
 * consultas cómo se hace el siguiente ejercicio, y la barra del descanso se queda fija encima
 * de la barra de navegación.
 */
@Composable
fun GymListApp(repository: GymRepository) {
    val navController = rememberNavController()
    val workoutViewModel: WorkoutViewModel =
        viewModel(factory = WorkoutViewModel.factory(repository))
    val timer by workoutViewModel.timer.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showTabs = currentRoute in tabs.map { it.route } || currentRoute == Routes.WORKOUT

    Scaffold(
        topBar = {
            GymTopBar(
                route = currentRoute,
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        },
        bottomBar = {
            // La barra de pestañas ya deja sitio para la barra de gestos del sistema. Sin ella
            // hay que dejarlo aquí, o los botones de abajo de cada pantalla quedarían debajo.
            Column(if (showTabs) Modifier else Modifier.navigationBarsPadding()) {
                RestTimerBar(
                    state = timer,
                    onTogglePause = workoutViewModel::togglePause,
                    onAdjust = workoutViewModel::adjustRest,
                    onSkip = workoutViewModel::skipRest
                )
                if (showTabs) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { navController.switchTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel(factory = HomeViewModel.factory(repository)),
                    onContinueWorkout = { navController.navigate(Routes.WORKOUT) },
                    onStartRoutine = { routineId ->
                        workoutViewModel.startRoutine(routineId) {
                            navController.navigate(Routes.WORKOUT)
                        }
                    },
                    onOpenRoutines = { navController.switchTab(Routes.ROUTINES) },
                    onOpenSession = { navController.navigate(Routes.sessionDetail(it)) },
                    onCreatePlan = { navController.navigate(Routes.PLAN) }
                )
            }

            composable(Routes.WORKOUT) {
                WorkoutScreen(
                    viewModel = workoutViewModel,
                    onFinished = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            }

            composable(Routes.ROUTINES) {
                RoutinesScreen(
                    viewModel = viewModel(factory = RoutinesViewModel.factory(repository)),
                    onOpenRoutine = { navController.navigate(Routes.routineDetail(it)) },
                    onCreatePlan = { navController.navigate(Routes.PLAN) }
                )
            }

            composable(Routes.PLAN) {
                PlanWizardScreen(
                    viewModel = viewModel(factory = PlanWizardViewModel.factory(repository)),
                    onClose = { navController.popBackStack() },
                    onOpenRoutines = {
                        navController.popBackStack()
                        navController.switchTab(Routes.ROUTINES)
                    },
                    onStartRoutine = { routineId ->
                        workoutViewModel.startRoutine(routineId) {
                            navController.popBackStack()
                            navController.navigate(Routes.WORKOUT)
                        }
                    }
                )
            }

            composable(
                Routes.ROUTINE_DETAIL,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { entry ->
                val routineId = entry.arguments?.getLong("routineId") ?: 0L
                RoutineDetailScreen(
                    viewModel = viewModel(
                        factory = RoutineDetailViewModel.factory(repository, routineId)
                    ),
                    onBack = { navController.popBackStack() },
                    onOpenWorkout = { navController.navigate(Routes.WORKOUT) }
                )
            }

            composable(Routes.CALENDAR) {
                CalendarScreen(
                    viewModel = viewModel(factory = CalendarViewModel.factory(repository)),
                    onOpenSession = { navController.navigate(Routes.sessionDetail(it)) }
                )
            }

            composable(
                Routes.SESSION_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
                SessionDetailScreen(
                    viewModel = viewModel(
                        factory = SessionDetailViewModel.factory(repository, sessionId)
                    ),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.LIBRARY) {
                LibraryScreen(
                    viewModel = viewModel(factory = LibraryViewModel.factory(repository))
                )
            }

            composable(Routes.ABOUT) {
                val libraryViewModel: LibraryViewModel =
                    viewModel(factory = LibraryViewModel.factory(repository))
                val catalog by libraryViewModel.exercises.collectAsStateWithLifecycle()
                AboutScreen(
                    exerciseCount = catalog.size,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Cabecera de las pantallas principales. Las de detalle (rutina, entrenamiento guardado) traen
 * la suya propia, así que aquí no se pinta nada para ellas.
 */
@Composable
private fun GymTopBar(
    route: String?,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val title = when (route) {
        Routes.HOME -> "GymList"
        Routes.ROUTINES -> "Mis rutinas"
        Routes.CALENDAR -> "Seguimiento"
        Routes.LIBRARY -> "Ejercicios"
        Routes.WORKOUT -> "Entrenamiento"
        else -> null
    } ?: return

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (route == Routes.WORKOUT) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = {
            if (route != Routes.WORKOUT) {
                IconButton(onClick = onOpenAbout) {
                    Icon(Icons.Outlined.Info, contentDescription = "Acerca de GymList")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * Cambio de pestaña: una sola entrada por pestaña en la pila y se conserva el estado de cada
 * una, para que volver a "Seguimiento" no reinicie el mes que estabas mirando.
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Sin insets propios: el Scaffold de arriba ya ha descontado las barras del sistema. */
val NoInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
