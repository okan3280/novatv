package com.hp.novatv.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hp.novatv.ui.screens.CatchupScreen
import com.hp.novatv.ui.screens.HomeScreen
import com.hp.novatv.ui.screens.MultiviewScreen
import com.hp.novatv.ui.screens.PinScreen
import com.hp.novatv.ui.screens.PlaylistScreen
import com.hp.novatv.ui.screens.ProgramDetailScreen
import com.hp.novatv.ui.screens.RecordingsScreen
import com.hp.novatv.ui.screens.SearchScreen
import com.hp.novatv.ui.screens.SettingsScreen

/** Tum rotalar. */
object Routes {
    const val PIN = "pin"
    const val PLAYLISTS = "playlists"
    const val HOME = "home"
    const val EPG = "epg?playlistId={playlistId}"
    const val SEARCH = "search"
    const val CATCHUP = "catchup/{channelId}"
    const val PROGRAM_DETAIL = "program/{channelId}/{programId}"
    const val RECORDINGS = "recordings"
    const val MULTIVIEW = "multiview"
    const val SETTINGS = "settings"

    fun epg(playlistId: Long) = "epg?playlistId=$playlistId"
    fun catchup(channelId: Long) = "catchup/$channelId"
    fun programDetail(channelId: Long, programId: Long) = "program/$channelId/$programId"
}

@Composable
fun NovaNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.PIN) {
            PinScreen(
                onSuccess = {
                    navController.navigate(Routes.PLAYLISTS) {
                        popUpTo(Routes.PIN) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Routes.PLAYLISTS) {
                        popUpTo(Routes.PIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.PLAYLISTS) {
            PlaylistScreen(
                onPlaylistSelected = { playlistId ->
                    navController.navigate(Routes.epg(playlistId))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenEpg = { navController.navigate(Routes.EPG) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenCatchup = { navController.navigate(Routes.CATCHUP) },
                onOpenRecordings = { navController.navigate(Routes.RECORDINGS) },
                onOpenMultiview = { navController.navigate(Routes.MULTIVIEW) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.EPG,
            arguments = listOf(
                navArgument("playlistId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId") ?: -1L
            EpgRoute(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onOpenProgram = { channelId, programId ->
                    navController.navigate(Routes.programDetail(channelId, programId))
                },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.CATCHUP,
            arguments = listOf(navArgument("channelId") { type = NavType.LongType }),
        ) { entry ->
            val channelId = entry.arguments?.getLong("channelId") ?: 0L
            CatchupScreen(
                channelId = channelId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PROGRAM_DETAIL,
            arguments = listOf(
                navArgument("channelId") { type = NavType.LongType },
                navArgument("programId") { type = NavType.LongType },
            ),
        ) { entry ->
            ProgramDetailScreen(
                channelId = entry.arguments?.getLong("channelId") ?: 0L,
                programId = entry.arguments?.getLong("programId") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.RECORDINGS) {
            RecordingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MULTIVIEW) {
            MultiviewScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
