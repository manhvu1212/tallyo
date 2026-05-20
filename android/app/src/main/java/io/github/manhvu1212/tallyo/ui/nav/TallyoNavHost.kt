package io.github.manhvu1212.tallyo.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.manhvu1212.tallyo.data.AppContainer
import io.github.manhvu1212.tallyo.ui.LocalAppContainer
import io.github.manhvu1212.tallyo.ui.addround.AddRoundScreen
import io.github.manhvu1212.tallyo.ui.detail.SessionDetailScreen
import io.github.manhvu1212.tallyo.ui.newsession.NewSessionScreen
import io.github.manhvu1212.tallyo.ui.sessions.SessionsListScreen
import io.github.manhvu1212.tallyo.ui.stats.StatsScreen

object Routes {
    const val SESSIONS = "sessions"
    const val NEW_SESSION = "new-session"
    const val SESSION_DETAIL = "sessions/{sessionId}"
    const val STATS = "sessions/{sessionId}/stats"
    const val ADD_ROUND = "sessions/{sessionId}/round?roundId={roundId}"

    fun sessionDetail(id: String) = "sessions/$id"
    fun stats(id: String) = "sessions/$id/stats"
    fun addRound(sessionId: String, roundId: String? = null): String =
        if (roundId == null) "sessions/$sessionId/round" else "sessions/$sessionId/round?roundId=$roundId"
}

@Composable
fun TallyoNavHost(container: AppContainer) {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalAppContainer provides container) {
        NavHost(navController = navController, startDestination = Routes.SESSIONS) {
            composable(Routes.SESSIONS) {
                SessionsListScreen(
                    onOpenSession = { id -> navController.navigate(Routes.sessionDetail(id)) },
                    onNewSession = { navController.navigate(Routes.NEW_SESSION) },
                )
            }
            composable(Routes.NEW_SESSION) {
                NewSessionScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.sessionDetail(id))
                    },
                )
            }
            composable(
                route = Routes.SESSION_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("sessionId").orEmpty()
                SessionDetailScreen(
                    sessionId = id,
                    onBack = { navController.popBackStack() },
                    onOpenStats = { navController.navigate(Routes.stats(id)) },
                    onAddRound = { roundId -> navController.navigate(Routes.addRound(id, roundId)) },
                )
            }
            composable(
                route = Routes.STATS,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("sessionId").orEmpty()
                StatsScreen(
                    sessionId = id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ADD_ROUND,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("roundId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val sessionId = entry.arguments?.getString("sessionId").orEmpty()
                val roundId = entry.arguments?.getString("roundId")
                AddRoundScreen(
                    sessionId = sessionId,
                    roundId = roundId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
