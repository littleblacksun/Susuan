package com.xingce.speedmath.ui.navigation

import android.os.Bundle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xingce.speedmath.AppContainer
import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.ui.components.BottomNavBar
import com.xingce.speedmath.ui.components.PrimaryTab
import com.xingce.speedmath.ui.export.ExportViewModel
import com.xingce.speedmath.ui.formula.FormulaRoute
import com.xingce.speedmath.ui.formula.FormulaViewModel
import com.xingce.speedmath.ui.history.HistoryRoute
import com.xingce.speedmath.ui.history.HistoryViewModel
import com.xingce.speedmath.ui.home.HomeRoute
import com.xingce.speedmath.ui.home.HomeViewModel
import com.xingce.speedmath.ui.home.PracticeLaunchConfig
import com.xingce.speedmath.ui.home.normalizedForHome
import com.xingce.speedmath.ui.practice.PracticeRoute
import com.xingce.speedmath.ui.practice.PracticeStart
import com.xingce.speedmath.ui.practice.PracticeViewModel
import com.xingce.speedmath.ui.result.ResultRoute
import com.xingce.speedmath.ui.result.ResultViewModel

@Composable
fun AppNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selectedTab = PrimaryTab.entries.firstOrNull { it.route == entry?.destination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            selectedTab?.let { current ->
                BottomNavBar(selectedTab = current, onSelect = { tab ->
                    if (tab != current) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                })
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = PrimaryTab.HOME.route,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        ) {
            composable(PrimaryTab.HOME.route) { homeEntry ->
                val factory = remember(container) {
                    HomeViewModel.factory(container.practiceRepository, container.preferencesStore)
                }
                val exportFactory = remember(container) {
                    ExportViewModel.factory(container.worksheetExporter)
                }
                val home: HomeViewModel = viewModel(viewModelStoreOwner = homeEntry, factory = factory)
                val exportVm: ExportViewModel = viewModel(viewModelStoreOwner = homeEntry, factory = exportFactory)
                HomeRoute(home, exportVm) { config ->
                    navController.navigate(PracticeDestination.route(config)) { launchSingleTop = true }
                }
            }
            composable(PrimaryTab.FORMULAS.route) { formulaEntry ->
                val factory = remember(container) { FormulaViewModel.factory(container.formulaRepository) }
                val formulas: FormulaViewModel = viewModel(viewModelStoreOwner = formulaEntry, factory = factory)
                FormulaRoute(formulas)
            }
            composable(PrimaryTab.HISTORY.route) { historyEntry ->
                val factory = remember(container) {
                    HistoryViewModel.factory(container.practiceRepository, container.preferencesStore)
                }
                val exportFactory = remember(container) {
                    ExportViewModel.factory(container.worksheetExporter)
                }
                val history: HistoryViewModel = viewModel(viewModelStoreOwner = historyEntry, factory = factory)
                val exportVm: ExportViewModel = viewModel(viewModelStoreOwner = historyEntry, factory = exportFactory)
                HistoryRoute(
                    viewModel = history,
                    exportViewModel = exportVm,
                    onOpenSession = { sessionId ->
                        if (navController.currentBackStackEntry == historyEntry) {
                            navController.navigate("result/$sessionId")
                        }
                    },
                    onHome = {
                        if (navController.currentBackStackEntry == historyEntry) navController.showHome()
                    },
                )
            }
            composable(PracticeDestination.pattern, arguments = PracticeDestination.arguments) { practiceEntry ->
                val config = remember(practiceEntry) {
                    PracticeDestination.read(requireNotNull(practiceEntry.arguments))
                }
                PracticeDestinationContent(container, navController, practiceEntry, PracticeStart.New(config))
            }
            composable("practice/retry/{sessionId}", arguments = sessionArguments) { practiceEntry ->
                val sessionId = requireNotNull(practiceEntry.arguments).getLong("sessionId")
                PracticeDestinationContent(container, navController, practiceEntry, PracticeStart.Retry(sessionId))
            }
            composable("result/{sessionId}", arguments = sessionArguments) { resultEntry ->
                val sessionId = requireNotNull(resultEntry.arguments).getLong("sessionId")
                val fromHistory = remember(resultEntry) {
                    navController.previousBackStackEntry?.destination?.route == PrimaryTab.HISTORY.route
                }
                val factory = remember(container, sessionId) {
                    ResultViewModel.factory(sessionId, container.practiceRepository, container.preferencesStore)
                }
                val result: ResultViewModel = viewModel(viewModelStoreOwner = resultEntry, factory = factory)
                fun startPractice(route: String) {
                    if (navController.currentBackStackEntry != resultEntry) return
                    navController.navigate(route) {
                        popUpTo(resultEntry.destination.id) { inclusive = true }
                    }
                }
                ResultRoute(
                    viewModel = result,
                    fromHistory = fromHistory,
                    onHome = {
                        if (navController.currentBackStackEntry == resultEntry &&
                            (navController.previousBackStackEntry == null || !navController.popBackStack())
                        ) {
                            navController.showHome()
                        }
                    },
                    onRetry = { startPractice("practice/retry/$it") },
                    onNewPractice = { startPractice(PracticeDestination.route(it)) },
                )
            }
        }
    }
}

private val sessionArguments = listOf(navArgument("sessionId") { type = NavType.LongType })

private fun NavHostController.showHome() {
    navigate(PrimaryTab.HOME.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PracticeDestinationContent(
    container: AppContainer,
    navController: NavHostController,
    entry: NavBackStackEntry,
    start: PracticeStart,
) {
    val factory = remember(container, start) {
        PracticeViewModel.factory(start, container.practiceRepository, container.preferencesStore)
    }
    val practice: PracticeViewModel = viewModel(viewModelStoreOwner = entry, factory = factory)
    PracticeRoute(
        viewModel = practice,
        onExit = {
            if (navController.currentBackStackEntry == entry) navController.popBackStack()
        },
        onCompleted = { sessionId ->
            if (navController.currentBackStackEntry == entry) {
                navController.navigate("result/$sessionId") {
                    // A completed practice returns home, including a practice launched from history.
                    popUpTo(PrimaryTab.HOME.route)
                }
            }
        },
    )
}

private object PracticeDestination {
    private val defaultCustom = CustomQuizConfig()

    val arguments = listOf(
        navArgument("moduleId") { type = NavType.StringType },
        optionalArgument("count", NavType.IntType, 10),
        optionalArgument("tolerance", NavType.StringType, ToleranceLevel.STANDARD.name),
        optionalArgument("nBack", NavType.IntType, NBackLevel.OFF.steps),
        optionalArgument("keypad", NavType.StringType, KeypadOrder.ASCENDING.name),
        optionalArgument("stylus", NavType.BoolType, true),
        optionalArgument("haptic", NavType.BoolType, true),
        optionalArgument("operation", NavType.StringType, defaultCustom.operation.name),
        optionalArgument("firstDigits", NavType.IntType, defaultCustom.firstDigits),
        optionalArgument("secondDigits", NavType.IntType, defaultCustom.secondDigits),
        optionalArgument("allowNegative", NavType.BoolType, defaultCustom.allowNegative),
        optionalArgument("percentAnswer", NavType.BoolType, defaultCustom.percentAnswer),
    )
    val pattern = arguments.drop(1).joinToString("&", prefix = "practice/{moduleId}?") { "${it.name}={${it.name}}" }

    private fun <T> optionalArgument(name: String, navType: NavType<T>, default: T) = navArgument(name) {
        type = navType
        defaultValue = default
    }

    fun route(config: PracticeLaunchConfig): String = buildString {
        append("practice/${config.module.id}?count=${config.count}&tolerance=${config.tolerance.name}")
        append("&nBack=${config.nBack.steps}&keypad=${config.keypad.name}&stylus=${config.stylus}")
        append("&haptic=${config.haptic}")
        config.custom?.let {
            append("&operation=${it.operation.name}&firstDigits=${it.firstDigits}&secondDigits=${it.secondDigits}")
            append("&allowNegative=${it.allowNegative}&percentAnswer=${it.percentAnswer}")
        }
    }

    fun read(args: Bundle): PracticeLaunchConfig {
        val module = MathModule.fromId(args.getString("moduleId").orEmpty())
        val tolerance = ToleranceLevel.fromName(args.getString("tolerance").orEmpty())
        val nBack = NBackLevel.entries.firstOrNull { it.steps == args.getInt("nBack") } ?: NBackLevel.OFF
        val keypad = KeypadOrder.entries.firstOrNull { it.name == args.getString("keypad") } ?: KeypadOrder.ASCENDING
        val custom = if (module == MathModule.CUSTOM) {
            CustomQuizConfig(
                operation = CustomOperation.entries.firstOrNull { it.name == args.getString("operation") }
                    ?: CustomOperation.DIV,
                firstDigits = args.getInt("firstDigits"),
                secondDigits = args.getInt("secondDigits"),
                allowNegative = args.getBoolean("allowNegative"),
                percentAnswer = args.getBoolean("percentAnswer"),
                tolerance = tolerance,
            ).normalizedForHome()
        } else {
            null
        }
        return PracticeLaunchConfig(
            module = module,
            count = args.getInt("count"),
            tolerance = tolerance,
            nBack = nBack,
            keypad = keypad,
            stylus = args.getBoolean("stylus"),
            haptic = args.getBoolean("haptic"),
            custom = custom,
        )
    }
}

